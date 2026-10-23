package com.gahrms.backend.modules.attendance.service;

import com.gahrms.backend.common.dto.PageResponse;
import com.gahrms.backend.common.exception.ResourceNotFoundException;
import com.gahrms.backend.modules.attendance.client.ETimeTrackSqlServerClient;
import com.gahrms.backend.modules.attendance.config.EtimeTrackProperties;
import com.gahrms.backend.modules.attendance.dto.*;
import com.gahrms.backend.modules.attendance.entity.AttendancePunch;
import com.gahrms.backend.modules.attendance.entity.AttendanceSyncState;
import com.gahrms.backend.modules.attendance.entity.BiometricEmployeeMapping;
import com.gahrms.backend.modules.attendance.entity.Holiday;
import com.gahrms.backend.modules.attendance.entity.RegularizationRequest;
import com.gahrms.backend.modules.attendance.repository.AttendancePunchRepository;
import com.gahrms.backend.modules.attendance.repository.AttendanceSyncStateRepository;
import com.gahrms.backend.modules.attendance.repository.BiometricEmployeeMappingRepository;
import com.gahrms.backend.modules.attendance.repository.HolidayRepository;
import com.gahrms.backend.modules.attendance.repository.RegularizationRepository;
import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.gahrms.backend.common.util.SecurityUtils;
import com.gahrms.backend.modules.identity.entity.User;
import com.gahrms.backend.modules.identity.repository.UserRepository;
import com.gahrms.backend.modules.leave.entity.LeaveRequest;
import com.gahrms.backend.modules.leave.repository.LeaveRequestRepository;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceSyncService {

    public static final String SOURCE_SYSTEM = "ESSL_ETIMETRACK";
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final ETimeTrackSqlServerClient sqlServerClient;
    private final AttendancePunchRepository punchRepository;
    private final AttendanceSyncStateRepository syncStateRepository;
    private final BiometricEmployeeMappingRepository mappingRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EtimeTrackProperties etimeTrackProperties;
    private final HolidayRepository holidayRepository;
    private final RegularizationRepository regularizationRepository;

    @Value("${etimetrack.sync.enabled:false}")
    private boolean automaticSyncEnabled;

    @Value("${etimetrack.sync.batch-size:100}")
    private int configuredBatchSize;

    @Scheduled(cron = "${etimetrack.sync.cron:0 */5 * * * *}")
    public void runScheduledSync() {
        if (!automaticSyncEnabled || !etimeTrackProperties.isEnabled()) {
            return;
        }
        log.info("Triggering scheduled biometric attendance sync...");
        syncIncrementalPunches(configuredBatchSize);
    }

    @Transactional
    public SyncSummaryDto syncIncrementalPunches(int requestedBatchSize) {
        long startTime = System.currentTimeMillis();

        if (!syncInProgress.compareAndSet(false, true)) {
            log.warn("Biometric attendance sync is already running in another thread. Skipping overlapping run.");
            return SyncSummaryDto.builder()
                    .status("LOCKED")
                    .message("Sync operation already in progress.")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .completedAt(Instant.now())
                    .build();
        }

        try {
            ConnectionDiagnosticResult diagnostic = sqlServerClient.testConnection();
            if (!diagnostic.isConnected()) {
                log.warn("Cannot execute attendance sync: SQL Server is not reachable ({})", diagnostic.getMessage());
                return SyncSummaryDto.builder()
                        .status("SOURCE_OFFLINE")
                        .message(diagnostic.getMessage())
                        .durationMs(System.currentTimeMillis() - startTime)
                        .completedAt(Instant.now())
                        .build();
            }

            AttendanceSyncState syncState = syncStateRepository.findBySourceSystem(SOURCE_SYSTEM)
                    .orElseGet(() -> syncStateRepository.save(
                            AttendanceSyncState.builder()
                                    .sourceSystem(SOURCE_SYSTEM)
                                    .lastSourceRecordId(0L)
                                    .lastSyncStatus("IDLE")
                                    .build()
                    ));

            long previousCursorId = syncState.getLastSourceRecordId();
            int batchSize = requestedBatchSize > 0 ? requestedBatchSize : configuredBatchSize;

            List<ETimeTrackRawPunchDto> rawPunches = sqlServerClient.fetchPunchesAfterId(previousCursorId, batchSize);

            if (rawPunches.isEmpty()) {
                syncState.setLastSyncTimestamp(Instant.now());
                syncState.setLastSyncStatus("SUCCESS");
                syncState.setLastSyncMessage("No new punches found in eTimeTrack (Cursor: " + previousCursorId + ")");
                syncStateRepository.save(syncState);

                return SyncSummaryDto.builder()
                        .status("NO_NEW_RECORDS")
                        .fetchedCount(0)
                        .insertedCount(0)
                        .skippedDuplicatesCount(0)
                        .unmappedEmployeesCount(0)
                        .previousCursorId(previousCursorId)
                        .newCursorId(previousCursorId)
                        .message("No new punches found in source database.")
                        .durationMs(System.currentTimeMillis() - startTime)
                        .completedAt(Instant.now())
                        .build();
            }

            int insertedCount = 0;
            int skippedDuplicates = 0;
            int unmappedEmployees = 0;
            long lastSafelyProcessedId = previousCursorId;

            for (ETimeTrackRawPunchDto raw : rawPunches) {
                if (raw.getId() == null || raw.getId() <= 0) continue;

                if (punchRepository.existsBySourceSystemAndSourceRecordId(SOURCE_SYSTEM, raw.getId())) {
                    skippedDuplicates++;
                    lastSafelyProcessedId = Math.max(lastSafelyProcessedId, raw.getId());
                    continue;
                }

                String empCode = raw.getEmpCode() != null ? raw.getEmpCode().trim() : "";
                Instant rawPunchTime = raw.getPunchDate() != null ? raw.getPunchDate() : Instant.now();

                if (punchRepository.existsByEmployeeCodeAndPunchTime(empCode, rawPunchTime)) {
                    skippedDuplicates++;
                    lastSafelyProcessedId = Math.max(lastSafelyProcessedId, raw.getId());
                    continue;
                }
                Employee matchedEmployee = resolveEmployeeByCode(empCode);
                String mappingStatus = (matchedEmployee != null) ? "MAPPED" : "UNMAPPED";

                if (matchedEmployee == null) {
                    unmappedEmployees++;
                    log.warn("Biometric punch ID #{} with raw EMP_CODE '{}' could not be resolved to any HRMS employee. Stored as UNMAPPED for HR review.",
                            raw.getId(), empCode);
                }

                AttendancePunch punch = AttendancePunch.builder()
                        .sourceSystem(SOURCE_SYSTEM)
                        .sourceRecordId(raw.getId())
                        .employeeCode(empCode)
                        .employee(matchedEmployee)
                        .punchTime(raw.getPunchDate() != null ? raw.getPunchDate() : Instant.now())
                        .punchType(cleanPunchType(raw.getUpdateFlag()))
                        .mappingStatus(mappingStatus)
                        .rawPayload("{\"sourceId\":" + raw.getId() + ",\"empCode\":\"" + empCode + "\",\"updateFlag\":\"" + raw.getUpdateFlag() + "\"}")
                        .build();

                try {
                    punchRepository.save(punch);
                    insertedCount++;
                    lastSafelyProcessedId = Math.max(lastSafelyProcessedId, raw.getId());
                } catch (DataIntegrityViolationException dive) {
                    log.debug("Concurrent duplicate caught for punch ID #{}: {}", raw.getId(), dive.getMessage());
                    skippedDuplicates++;
                    lastSafelyProcessedId = Math.max(lastSafelyProcessedId, raw.getId());
                }
            }

            syncState.setLastSourceRecordId(lastSafelyProcessedId);
            syncState.setLastSyncTimestamp(Instant.now());
            syncState.setLastSyncStatus("SUCCESS");
            syncState.setLastBatchCount(insertedCount);
            syncState.setTotalRecordsSynced(syncState.getTotalRecordsSynced() + insertedCount);
            syncState.setLastSyncMessage("Successfully synced " + insertedCount + " punches (Unmapped: " + unmappedEmployees + "). New cursor: " + lastSafelyProcessedId);
            syncStateRepository.save(syncState);

            log.info("Attendance sync batch completed: Fetched={}, Inserted={}, SkippedDuplicates={}, Unmapped={}, NewCursor={}",
                    rawPunches.size(), insertedCount, skippedDuplicates, unmappedEmployees, lastSafelyProcessedId);

            return SyncSummaryDto.builder()
                    .status("SUCCESS")
                    .fetchedCount(rawPunches.size())
                    .insertedCount(insertedCount)
                    .skippedDuplicatesCount(skippedDuplicates)
                    .unmappedEmployeesCount(unmappedEmployees)
                    .previousCursorId(previousCursorId)
                    .newCursorId(lastSafelyProcessedId)
                    .message("Batch sync completed successfully.")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .completedAt(Instant.now())
                    .build();

        } catch (Exception ex) {
            log.error("Fatal unhandled error during biometric attendance sync batch", ex);
            return SyncSummaryDto.builder()
                    .status("ERROR")
                    .message("Sync execution error: " + ex.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .completedAt(Instant.now())
                    .build();
        } finally {
            syncInProgress.set(false);
        }
    }

    @Transactional
    public SyncSummaryDto ingestPushBatch(BiometricPushBatchRequest request) {
        long startTime = System.currentTimeMillis();
        if (request == null || request.getData() == null || request.getData().isEmpty()) {
            return SyncSummaryDto.builder()
                    .status("SUCCESS")
                    .fetchedCount(0)
                    .insertedCount(0)
                    .skippedDuplicatesCount(0)
                    .unmappedEmployeesCount(0)
                    .message("No records in push batch.")
                    .durationMs(0L)
                    .completedAt(Instant.now())
                    .build();
        }

        AttendanceSyncState syncState = syncStateRepository.findBySourceSystem(SOURCE_SYSTEM)
                .orElseGet(() -> syncStateRepository.save(
                        AttendanceSyncState.builder()
                                .sourceSystem(SOURCE_SYSTEM)
                                .lastSourceRecordId(0L)
                                .lastSyncStatus("IDLE")
                                .build()
                ));

        long previousCursorId = syncState.getLastSourceRecordId() != null ? syncState.getLastSourceRecordId() : 0L;
        long lastSafelyProcessedId = previousCursorId;

        int insertedCount = 0;
        int skippedDuplicates = 0;
        int unmappedEmployees = 0;

        for (BiometricPushBatchRequest.BiometricPushItemDto item : request.getData()) {
            if (item == null) continue;
            Long serial = item.getSerial();
            if (serial == null || serial <= 0) continue;

            if (punchRepository.existsBySourceSystemAndSourceRecordId(SOURCE_SYSTEM, serial)) {
                skippedDuplicates++;
                lastSafelyProcessedId = Math.max(lastSafelyProcessedId, serial);
                continue;
            }

            String empCode = item.getEmp() != null ? item.getEmp().trim() : "";
            Instant punchTime = parseInstantSafe(item.getTime());

            if (punchRepository.existsByEmployeeCodeAndPunchTime(empCode, punchTime)) {
                skippedDuplicates++;
                lastSafelyProcessedId = Math.max(lastSafelyProcessedId, serial);
                continue;
            }

            Employee matchedEmployee = resolveEmployeeByCode(empCode);
            String mappingStatus = (matchedEmployee != null) ? "MAPPED" : "UNMAPPED";

            if (matchedEmployee == null) {
                unmappedEmployees++;
                log.warn("Biometric push punch ID #{} with raw EMP_CODE '{}' could not be resolved to any HRMS employee. Stored as UNMAPPED for HR review.",
                        serial, empCode);
            }

            AttendancePunch punch = AttendancePunch.builder()
                    .sourceSystem(SOURCE_SYSTEM)
                    .sourceRecordId(serial)
                    .employeeCode(empCode)
                    .employee(matchedEmployee)
                    .punchTime(punchTime)
                    .punchType(cleanPunchType(item.getStatus()))
                    .mappingStatus(mappingStatus)
                    .rawPayload("{\"sourceId\":" + serial + ",\"empCode\":\"" + empCode + "\",\"updateFlag\":\"" + item.getStatus() + "\"}")
                    .build();

            try {
                punchRepository.save(punch);
                insertedCount++;
                lastSafelyProcessedId = Math.max(lastSafelyProcessedId, serial);
            } catch (DataIntegrityViolationException dive) {
                log.debug("Concurrent duplicate caught for punch ID #{}: {}", serial, dive.getMessage());
                skippedDuplicates++;
                lastSafelyProcessedId = Math.max(lastSafelyProcessedId, serial);
            }
        }

        syncState.setLastSourceRecordId(lastSafelyProcessedId);
        syncState.setLastSyncTimestamp(Instant.now());
        syncState.setLastSyncStatus("SUCCESS");
        syncState.setLastBatchCount(insertedCount);
        syncState.setTotalRecordsSynced(syncState.getTotalRecordsSynced() + insertedCount);
        syncState.setLastSyncMessage("Push batch ingested " + insertedCount + " punches (Unmapped: " + unmappedEmployees + "). Cursor: " + lastSafelyProcessedId);
        syncStateRepository.save(syncState);

        log.info("Attendance push batch ingested: Total={}, Inserted={}, SkippedDuplicates={}, Unmapped={}, NewCursor={}",
                request.getData().size(), insertedCount, skippedDuplicates, unmappedEmployees, lastSafelyProcessedId);

        return SyncSummaryDto.builder()
                .status("SUCCESS")
                .fetchedCount(request.getData().size())
                .insertedCount(insertedCount)
                .skippedDuplicatesCount(skippedDuplicates)
                .unmappedEmployeesCount(unmappedEmployees)
                .previousCursorId(previousCursorId)
                .newCursorId(lastSafelyProcessedId)
                .message("Push batch processed successfully.")
                .durationMs(System.currentTimeMillis() - startTime)
                .completedAt(Instant.now())
                .build();
    }

    private Instant parseInstantSafe(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return Instant.now();
        }
        try {
            return java.time.OffsetDateTime.parse(timeStr.trim()).toInstant();
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(timeStr.trim());
        } catch (Exception ignored) {
        }
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(timeStr.trim(), formatter);
            return ldt.atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant();
        } catch (Exception ignored) {
        }
        return Instant.now();
    }

    public Employee resolveEmployeeByCode(String rawEmpCode) {
        if (rawEmpCode == null || rawEmpCode.trim().isEmpty()) {
            return null;
        }
        String cleanCode = rawEmpCode.trim();

        Optional<BiometricEmployeeMapping> mapping = mappingRepository.findBySourceSystemAndBiometricEmpCode(SOURCE_SYSTEM, cleanCode);
        if (mapping.isPresent() && mapping.get().getEmployee() != null) {
            return mapping.get().getEmployee();
        }

        Optional<Employee> directMatch = employeeRepository.findByEmployeeCode(cleanCode);
        if (directMatch.isPresent()) {
            return directMatch.get();
        }

        Optional<Employee> caseInsensitiveMatch = employeeRepository.findByEmployeeCodeIgnoreCase(cleanCode);
        if (caseInsensitiveMatch.isPresent()) {
            return caseInsensitiveMatch.get();
        }

        return null;
    }

    @Transactional
    public SyncSummaryDto remapUnmatchedPunches() {
        long startTime = System.currentTimeMillis();
        List<AttendancePunch> unmappedPunches = punchRepository.findByMappingStatus("UNMAPPED");

        List<BiometricEmployeeMapping> allMappings = mappingRepository.findBySourceSystem(SOURCE_SYSTEM);
        Map<String, Employee> codeToEmp = allMappings.stream()
                .filter(m -> m.getEmployee() != null && m.getBiometricEmpCode() != null)
                .collect(Collectors.toMap(
                        m -> m.getBiometricEmpCode().trim().toUpperCase(),
                        BiometricEmployeeMapping::getEmployee,
                        (a, b) -> a
                ));

        List<Employee> allEmployees = employeeRepository.findAll();
        Map<String, Employee> empCodeToEmp = allEmployees.stream()
                .filter(e -> e.getEmployeeCode() != null)
                .collect(Collectors.toMap(
                        e -> e.getEmployeeCode().trim().toUpperCase(),
                        e -> e,
                        (a, b) -> a
                ));

        List<AttendancePunch> toUpdate = new ArrayList<>();
        int remappedCount = 0;
        int remainingUnmapped = 0;

        for (AttendancePunch punch : unmappedPunches) {
            String code = punch.getEmployeeCode() != null ? punch.getEmployeeCode().trim().toUpperCase() : "";
            Employee matched = codeToEmp.get(code);
            if (matched == null) {
                matched = empCodeToEmp.get(code);
            }

            if (matched != null) {
                punch.setEmployee(matched);
                punch.setMappingStatus("MAPPED");
                toUpdate.add(punch);
                remappedCount++;
            } else {
                remainingUnmapped++;
            }
        }

        if (!toUpdate.isEmpty()) {
            punchRepository.saveAll(toUpdate);
        }

        log.info("Remapping process completed: Remapped={}, RemainingUnmapped={}, DurationMs={}",
                remappedCount, remainingUnmapped, System.currentTimeMillis() - startTime);

        return SyncSummaryDto.builder()
                .status("SUCCESS")
                .fetchedCount(unmappedPunches.size())
                .insertedCount(remappedCount)
                .unmappedEmployeesCount(remainingUnmapped)
                .message("Remapping completed. " + remappedCount + " punches successfully linked to employees.")
                .durationMs(System.currentTimeMillis() - startTime)
                .completedAt(Instant.now())
                .build();
    }

    @Transactional
    public BiometricEmployeeMappingDto addOrUpdateMapping(BiometricEmployeeMappingDto request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        String cleanCode = request.getBiometricEmpCode().trim();
        BiometricEmployeeMapping mapping = mappingRepository
                .findBySourceSystemAndBiometricEmpCode(SOURCE_SYSTEM, cleanCode)
                .orElseGet(() -> BiometricEmployeeMapping.builder()
                        .sourceSystem(SOURCE_SYSTEM)
                        .biometricEmpCode(cleanCode)
                        .build());

        mapping.setEmployee(employee);
        mapping.setNotes(request.getNotes());
        BiometricEmployeeMapping saved = mappingRepository.save(mapping);

        return toMappingDto(saved);
    }

    @Transactional(readOnly = true)
    public List<BiometricEmployeeMappingDto> getAllMappings() {
        return mappingRepository.findBySourceSystem(SOURCE_SYSTEM).stream()
                .map(this::toMappingDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMapping(Long id) {
        if (!mappingRepository.existsById(id)) {
            throw new ResourceNotFoundException("BiometricEmployeeMapping", "id", id);
        }
        mappingRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendancePunchResponse> getUnmappedPunches(Pageable pageable) {
        Page<AttendancePunch> page = punchRepository.findByMappingStatusOrderByPunchTimeDesc("UNMAPPED", pageable);
        List<AttendancePunchResponse> content = page.getContent().stream()
                .map(this::toPunchResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendancePunchResponse> getAllPunches(String status, String search, LocalDate date, Pageable pageable) {
        Specification<AttendancePunch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(root.get("mappingStatus"), status.trim().toUpperCase()));
            }
            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Join<AttendancePunch, Employee> empJoin = root.join("employee", JoinType.LEFT);
                Predicate codeMatch = cb.like(cb.lower(root.get("employeeCode")), term);
                Predicate firstNameMatch = cb.like(cb.lower(empJoin.get("firstName")), term);
                Predicate lastNameMatch = cb.like(cb.lower(empJoin.get("lastName")), term);
                predicates.add(cb.or(codeMatch, firstNameMatch, lastNameMatch));
            }
            if (date != null) {
                ZoneId ist = ZoneId.of("Asia/Kolkata");
                Instant start = date.atStartOfDay(ist).toInstant();
                Instant end = date.plusDays(1).atStartOfDay(ist).toInstant();
                predicates.add(cb.between(root.get("punchTime"), start, end));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("punchTime")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AttendancePunch> page = punchRepository.findAll(spec, pageable);
        List<AttendancePunchResponse> content = page.getContent().stream()
                .map(this::toPunchResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @lombok.Getter
    @lombok.Builder
    public static class ProcessedAttendanceDay {
        private final Instant firstIn;
        private final Instant lastOut;
        private final int workedMinutes;
        private final String workingHoursFormatted;
        private final List<AttendancePunchResponse> punches;
        private final int punchCount;
    }

    public ProcessedAttendanceDay processDayPunches(List<AttendancePunch> rawPunches) {
        if (rawPunches == null || rawPunches.isEmpty()) {
            return ProcessedAttendanceDay.builder()
                    .firstIn(null)
                    .lastOut(null)
                    .workedMinutes(0)
                    .workingHoursFormatted("--")
                    .punches(new ArrayList<>())
                    .punchCount(0)
                    .build();
        }

        List<AttendancePunch> sorted = rawPunches.stream()
                .filter(p -> p != null && p.getPunchTime() != null)
                .sorted(java.util.Comparator.comparing(AttendancePunch::getPunchTime)
                        .thenComparing(p -> p.getSourceRecordId() != null ? p.getSourceRecordId() : 0L))
                .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            return ProcessedAttendanceDay.builder()
                    .firstIn(null)
                    .lastOut(null)
                    .workedMinutes(0)
                    .workingHoursFormatted("--")
                    .punches(new ArrayList<>())
                    .punchCount(0)
                    .build();
        }

        List<AttendancePunch> debounced = new ArrayList<>();
        for (AttendancePunch p : sorted) {
            if (debounced.isEmpty()) {
                debounced.add(p);
            } else {
                AttendancePunch prev = debounced.get(debounced.size() - 1);
                long diffSec = java.time.Duration.between(prev.getPunchTime(), p.getPunchTime()).abs().toSeconds();
                if (diffSec >= 60) {
                    debounced.add(p);
                }
            }
        }

        Instant firstIn = debounced.get(0).getPunchTime();
        Instant lastOut = null;
        int workedMinutes = 0;
        String workingHoursFormatted = "--";

        if (debounced.size() > 1) {
            Instant latestPunchTime = debounced.get(debounced.size() - 1).getPunchTime();
            long diffFromFirstIn = java.time.Duration.between(firstIn, latestPunchTime).toMinutes();
            if (diffFromFirstIn >= 5) {
                lastOut = latestPunchTime;
                long minutes = java.time.Duration.between(firstIn, lastOut).toMinutes();
                workedMinutes = (int) minutes;
                workingHoursFormatted = (minutes / 60) + "h " + (minutes % 60) + "m";
            }
        }

        List<AttendancePunchResponse> responses = new ArrayList<>();
        int total = debounced.size();
        for (int i = 0; i < total; i++) {
            AttendancePunch p = debounced.get(i);
            String pType;
            if (i == 0) {
                pType = "IN";
            } else if (i == total - 1 && lastOut != null) {
                pType = "OUT";
            } else {
                pType = (i % 2 == 0) ? "IN" : "OUT";
            }

            responses.add(AttendancePunchResponse.builder()
                    .id(p.getId())
                    .employeeId(p.getEmployee() != null ? p.getEmployee().getId() : null)
                    .employeeName(p.getEmployee() != null ? p.getEmployee().getFullName() : "UNRESOLVED")
                    .employeeCode(p.getEmployeeCode())
                    .sourceSystem(p.getSourceSystem())
                    .sourceRecordId(p.getSourceRecordId())
                    .punchTime(p.getPunchTime())
                    .punchType(pType)
                    .mappingStatus(p.getMappingStatus())
                    .rawPayload(p.getRawPayload())
                    .createdAt(p.getCreatedAt())
                    .build());
        }

        return ProcessedAttendanceDay.builder()
                .firstIn(firstIn)
                .lastOut(lastOut)
                .workedMinutes(workedMinutes)
                .workingHoursFormatted(workingHoursFormatted)
                .punches(responses)
                .punchCount(responses.size())
                .build();
    }

    @Transactional(readOnly = true)
    public DailyAttendanceRosterDto getDailyRoster(LocalDate targetDate) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now(IST_ZONE);
        ZoneId ist = ZoneId.of("Asia/Kolkata");
        Instant start = date.atStartOfDay(ist).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ist).toInstant();

        List<AttendancePunch> punchesOnDay = punchRepository.findByPunchTimeBetweenOrderByPunchTimeAsc(start, end);
        List<Employee> allEmployees = employeeRepository.findAll();
        List<BiometricEmployeeMapping> mappings = mappingRepository.findBySourceSystem(SOURCE_SYSTEM);
        Map<Long, String> employeeToBiometricCode = mappings.stream()
                .filter(m -> m.getEmployee() != null)
                .collect(Collectors.toMap(m -> m.getEmployee().getId(), BiometricEmployeeMapping::getBiometricEmpCode, (a, b) -> a));

        Map<Long, List<AttendancePunch>> mappedPunches = punchesOnDay.stream()
                .filter(p -> p.getEmployee() != null)
                .collect(Collectors.groupingBy(p -> p.getEmployee().getId()));

        Map<String, List<AttendancePunch>> unmappedPunches = punchesOnDay.stream()
                .filter(p -> p.getEmployee() == null && p.getEmployeeCode() != null)
                .collect(Collectors.groupingBy(AttendancePunch::getEmployeeCode));

        List<LeaveRequest> approvedLeavesOnDate = leaveRequestRepository.findAllApprovedLeavesInRange(date, date);
        Map<Long, LeaveRequest> empApprovedLeaves = approvedLeavesOnDate.stream()
                .filter(lr -> lr.getEmployee() != null)
                .collect(Collectors.toMap(lr -> lr.getEmployee().getId(), lr -> lr, (a, b) -> a));

        DayOfWeek dow = date.getDayOfWeek();
        boolean isSunday = (dow == DayOfWeek.SUNDAY);
        LocalDate lastSaturdayOfMonth = date.with(TemporalAdjusters.lastInMonth(DayOfWeek.SATURDAY));
        boolean isLastSaturday = date.equals(lastSaturdayOfMonth);

        List<Holiday> dbHolidays = holidayRepository.findByDateRange(date, date);
        String declaredHolidayName = dbHolidays.isEmpty() ? null : dbHolidays.get(0).getName();
        boolean isHoliday = (declaredHolidayName != null);
        boolean isWeekOffDay = isSunday || isLastSaturday || isHoliday;

        LocalTime standardStart = LocalTime.of(10, 0, 0);
        LocalTime lateThreshold = LocalTime.of(10, 5, 59);
        LocalTime halfDayThreshold = LocalTime.of(11, 0, 59);

        LocalTime expectedOutTime;
        String expectedOutStr;
        if (isHoliday) {
            expectedOutTime = null;
            expectedOutStr = declaredHolidayName;
        } else if (isWeekOffDay) {
            expectedOutTime = null;
            expectedOutStr = "Week Off";
        } else if (dow == DayOfWeek.MONDAY || dow == DayOfWeek.FRIDAY) {
            expectedOutTime = LocalTime.of(19, 30, 0);
            expectedOutStr = "07:30 PM";
        } else if (dow == DayOfWeek.SATURDAY) {
            expectedOutTime = LocalTime.of(18, 0, 0);
            expectedOutStr = "06:00 PM";
        } else {
            expectedOutTime = LocalTime.of(19, 0, 0);
            expectedOutStr = "07:00 PM";
        }

        List<DailyEmployeeAttendanceItemDto> records = new ArrayList<>();
        int presentCount = 0;
        int lateCount = 0;
        int halfDayCount = 0;
        int absentCount = 0;
        int onLeaveCount = 0;
        int weekOffCount = 0;

        for (Employee emp : allEmployees) {
            List<AttendancePunch> empPunches = mappedPunches.get(emp.getId());
            String bioCode = employeeToBiometricCode.get(emp.getId());

            if (empPunches != null && !empPunches.isEmpty()) {
                ProcessedAttendanceDay processed = processDayPunches(empPunches);
                Instant firstIn = processed.getFirstIn();
                Instant lastOut = processed.getLastOut();
                int workedMins = processed.getWorkedMinutes();
                String workingHoursStr = processed.getWorkingHoursFormatted();

                ZonedDateTime firstInZdt = firstIn.atZone(ist);
                LocalTime firstInTime = firstInZdt.toLocalTime();

                int lateMins = 0;
                String status;

                if (isWeekOffDay) {
                    status = "WEEK_OFF";
                    weekOffCount++;
                } else {
                    if (firstInTime.isAfter(halfDayThreshold)) {
                        status = "HALF_DAY";
                        lateMins = (int) java.time.Duration.between(standardStart, firstInTime).toMinutes();
                        halfDayCount++;
                    } else if (firstInTime.isAfter(lateThreshold)) {
                        status = "LATE";
                        lateMins = (int) java.time.Duration.between(standardStart, firstInTime).toMinutes();
                        lateCount++;
                    } else {
                        status = "PRESENT";
                        lateMins = 0;
                        presentCount++;
                    }
                }

                Integer earlyDepartureMins = null;
                if (!isWeekOffDay && expectedOutTime != null && lastOut != null) {
                    ZonedDateTime lastOutZdt = lastOut.atZone(ist);
                    LocalTime lastOutTime = lastOutZdt.toLocalTime();
                    if (lastOutTime.isBefore(expectedOutTime)) {
                        earlyDepartureMins = (int) java.time.Duration.between(lastOutTime, expectedOutTime).toMinutes();
                    }
                }

                records.add(DailyEmployeeAttendanceItemDto.builder()
                        .employeeId(emp.getId())
                        .employeeCode(emp.getEmployeeCode())
                        .employeeName(emp.getFullName())
                        .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : "General")
                        .branchName(emp.getBranch() != null ? emp.getBranch().getName() : "Headquarters")
                        .avatar(null)
                        .firstIn(firstIn)
                        .lastOut(lastOut)
                        .expectedIn(isHoliday ? ("Holiday: " + declaredHolidayName) : (isWeekOffDay ? "Week Off" : "10:00 AM"))
                        .expectedOut(expectedOutStr)
                        .isWeekOff(isWeekOffDay)
                        .isHoliday(isHoliday)
                        .holidayName(declaredHolidayName)
                        .earlyDepartureMinutes(earlyDepartureMins)
                        .workedMinutes(workedMins)
                        .workingHoursFormatted(workingHoursStr)
                        .lateMinutes(lateMins)
                        .status(status)
                        .punchCount(processed.getPunchCount())
                        .biometricMapped(bioCode != null)
                        .biometricCode(bioCode != null ? bioCode : emp.getEmployeeCode())
                        .punches(processed.getPunches())
                        .build());
            } else {
                if (isHoliday) {
                    weekOffCount++;
                    records.add(DailyEmployeeAttendanceItemDto.builder()
                            .employeeId(emp.getId())
                            .employeeCode(emp.getEmployeeCode())
                            .employeeName(emp.getFullName())
                            .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : "General")
                            .branchName(emp.getBranch() != null ? emp.getBranch().getName() : "Headquarters")
                            .avatar(null)
                            .firstIn(null)
                            .lastOut(null)
                            .expectedIn("Holiday Off")
                            .expectedOut(declaredHolidayName)
                            .isWeekOff(true)
                            .isHoliday(true)
                            .holidayName(declaredHolidayName)
                            .workedMinutes(0)
                            .workingHoursFormatted("Holiday: " + declaredHolidayName)
                            .lateMinutes(0)
                            .status("HOLIDAY")
                            .punchCount(0)
                            .biometricMapped(bioCode != null)
                            .biometricCode(bioCode)
                            .punches(new ArrayList<>())
                            .build());
                } else if (isWeekOffDay) {
                    weekOffCount++;
                    records.add(DailyEmployeeAttendanceItemDto.builder()
                            .employeeId(emp.getId())
                            .employeeCode(emp.getEmployeeCode())
                            .employeeName(emp.getFullName())
                            .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : "General")
                            .branchName(emp.getBranch() != null ? emp.getBranch().getName() : "Headquarters")
                            .avatar(null)
                            .firstIn(null)
                            .lastOut(null)
                            .expectedIn(isSunday ? "Sunday Off" : "Saturday Off")
                            .expectedOut("Week Off")
                            .isWeekOff(true)
                            .isHoliday(false)
                            .holidayName(null)
                            .workedMinutes(0)
                            .workingHoursFormatted("--")
                            .lateMinutes(0)
                            .status("WEEK_OFF")
                            .punchCount(0)
                            .biometricMapped(bioCode != null)
                            .biometricCode(bioCode)
                            .punches(new ArrayList<>())
                            .build());
                } else if (empApprovedLeaves.containsKey(emp.getId())) {
                    onLeaveCount++;
                    LeaveRequest lr = empApprovedLeaves.get(emp.getId());
                    String typeCode = lr.getLeaveType() != null ? lr.getLeaveType().getCode() : "LEAVE";
                    String typeName = lr.getLeaveType() != null ? lr.getLeaveType().getName() : "Approved Leave";
                    String leaveLabel = lr.isHalfDay() ? typeCode + " (Half Day)" : typeCode + " (Full Day)";

                    records.add(DailyEmployeeAttendanceItemDto.builder()
                            .employeeId(emp.getId())
                            .employeeCode(emp.getEmployeeCode())
                            .employeeName(emp.getFullName())
                            .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : "General")
                            .branchName(emp.getBranch() != null ? emp.getBranch().getName() : "Headquarters")
                            .avatar(null)
                            .firstIn(null)
                            .lastOut(null)
                            .expectedIn("Approved Leave")
                            .expectedOut(typeName)
                            .isWeekOff(false)
                            .isHoliday(false)
                            .holidayName(null)
                            .workedMinutes(0)
                            .workingHoursFormatted(leaveLabel)
                            .lateMinutes(0)
                            .status(lr.isHalfDay() ? "HALF_DAY_LEAVE" : "LEAVE")
                            .punchCount(0)
                            .biometricMapped(bioCode != null)
                            .biometricCode(bioCode)
                            .punches(new ArrayList<>())
                            .build());
                } else {
                    absentCount++;
                    records.add(DailyEmployeeAttendanceItemDto.builder()
                            .employeeId(emp.getId())
                            .employeeCode(emp.getEmployeeCode())
                            .employeeName(emp.getFullName())
                            .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : "General")
                            .branchName(emp.getBranch() != null ? emp.getBranch().getName() : "Headquarters")
                            .avatar(null)
                            .firstIn(null)
                            .lastOut(null)
                            .expectedIn("10:00 AM")
                            .expectedOut(expectedOutStr)
                            .isWeekOff(false)
                            .isHoliday(false)
                            .holidayName(null)
                            .workedMinutes(0)
                            .workingHoursFormatted("--")
                            .lateMinutes(0)
                            .status("ABSENT")
                            .punchCount(0)
                            .biometricMapped(bioCode != null)
                            .biometricCode(bioCode)
                            .punches(new ArrayList<>())
                            .build());
                }
            }
        }

        for (Map.Entry<String, List<AttendancePunch>> entry : unmappedPunches.entrySet()) {
            String code = entry.getKey();
            List<AttendancePunch> unmappedList = entry.getValue();
            ProcessedAttendanceDay processed = processDayPunches(unmappedList);

            records.add(DailyEmployeeAttendanceItemDto.builder()
                    .employeeId(null)
                    .employeeCode(code)
                    .employeeName("Unmapped Biometric User (" + code + ")")
                    .departmentName("Biometric Terminal")
                    .branchName("On-Premise Machine")
                    .avatar(null)
                    .firstIn(processed.getFirstIn())
                    .lastOut(processed.getLastOut())
                    .expectedIn(isHoliday ? ("Holiday: " + declaredHolidayName) : (isWeekOffDay ? "Week Off" : "10:00 AM"))
                    .expectedOut(expectedOutStr)
                    .isWeekOff(isWeekOffDay)
                    .isHoliday(isHoliday)
                    .holidayName(declaredHolidayName)
                    .workedMinutes(processed.getWorkedMinutes())
                    .workingHoursFormatted(processed.getWorkingHoursFormatted())
                    .lateMinutes(0)
                    .status("UNMAPPED_DEVICE")
                    .punchCount(processed.getPunchCount())
                    .biometricMapped(false)
                    .biometricCode(code)
                    .punches(processed.getPunches())
                    .build());
        }

        int unmappedTotal = unmappedPunches.values().stream().mapToInt(List::size).sum();

        return DailyAttendanceRosterDto.builder()
                .date(date)
                .dayOfWeek(dow.name())
                .isWeekOffDay(isWeekOffDay)
                .isHoliday(isHoliday)
                .holidayName(declaredHolidayName)
                .shiftExpectedIn(isHoliday ? ("Holiday: " + declaredHolidayName) : (isWeekOffDay ? (isSunday ? "Sunday Off" : "Saturday Off") : "10:00 AM (Grace: 10:05:59 AM)"))
                .shiftExpectedOut(expectedOutStr)
                .shiftPolicySummary("Mon & Fri: 7:30 PM | Tue-Thu: 7:00 PM | Sat: 6:00 PM | Last Sat & Sun: Week Off")
                .totalEmployees(allEmployees.size())
                .presentCount(presentCount)
                .lateCount(lateCount)
                .halfDayCount(halfDayCount)
                .absentCount(absentCount)
                .onLeaveCount(onLeaveCount)
                .weekOffCount(weekOffCount)
                .totalPunches(punchesOnDay.size())
                .unmappedDevicePunchesCount(unmappedTotal)
                .records(records)
                .build();
    }

    @Transactional(readOnly = true)
    public DetectedBiometricCodesDto getDetectedBiometricCodes() {
        List<String> allCodes = punchRepository.findDistinctEmployeeCodes();
        List<String> unmappedCodes = punchRepository.findDistinctUnmappedCodes();
        return DetectedBiometricCodesDto.builder()
                .allCodes(allCodes)
                .unmappedCodes(unmappedCodes)
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeMonthlyAttendanceDto getEmployeeMonthlyAttendance(Long employeeId, Integer yearParam, Integer monthParam) {
        LocalDate today = LocalDate.now(IST_ZONE);
        int year = (yearParam != null && yearParam > 2000) ? yearParam : today.getYear();
        int month = (monthParam != null && monthParam >= 1 && monthParam <= 12) ? monthParam : today.getMonthValue();

        Employee employee;
        String currentUserEmail = SecurityUtils.getCurrentUserEmail().orElse(null);
        User currentUser = currentUserEmail != null ? userRepository.findByEmailIgnoreCase(currentUserEmail).orElse(null) : null;
        boolean isHrOrAdmin = SecurityUtils.isCurrentUserSuperAdmin()
                || SecurityUtils.hasAuthority("ROLE_HR_ADMIN")
                || SecurityUtils.hasAuthority("ROLE_TENANT_ADMIN");
        boolean isSupervisor = SecurityUtils.hasAuthority("ROLE_TEAM_LEAD")
                || SecurityUtils.hasAuthority("ROLE_MANAGER");

        if (isHrOrAdmin) {
            if (employeeId != null) {
                employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
            } else if (currentUser != null && currentUser.getEmployeeId() != null) {
                employee = employeeRepository.findById(currentUser.getEmployeeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", currentUser.getEmployeeId()));
            } else {
                List<Employee> all = employeeRepository.findAll();
                if (all.isEmpty()) {
                    throw new ResourceNotFoundException("Employee", "all", "none found");
                }
                employee = all.get(0);
            }
        } else if (isSupervisor) {
            if (employeeId != null) {
                Employee target = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
                Employee supervisorEmp = (currentUser != null && currentUser.getEmployeeId() != null)
                        ? employeeRepository.findById(currentUser.getEmployeeId()).orElse(null)
                        : null;

                boolean isSelf = supervisorEmp != null && supervisorEmp.getId().equals(target.getId());
                boolean isSameDept = supervisorEmp != null && supervisorEmp.getDepartment() != null
                        && target.getDepartment() != null
                        && supervisorEmp.getDepartment().getId().equals(target.getDepartment().getId());
                boolean isDirectReport = supervisorEmp != null && target.getManager() != null
                        && target.getManager().getId().equals(supervisorEmp.getId());

                if (isSelf || isSameDept || isDirectReport) {
                    employee = target;
                } else if (supervisorEmp != null) {
                    employee = supervisorEmp;
                } else {
                    employee = target;
                }
            } else if (currentUser != null && currentUser.getEmployeeId() != null) {
                employee = employeeRepository.findById(currentUser.getEmployeeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", currentUser.getEmployeeId()));
            } else {
                employee = employeeRepository.findAll().get(0);
            }
        } else {
            if (currentUser != null && currentUser.getEmployeeId() != null) {
                employee = employeeRepository.findById(currentUser.getEmployeeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", currentUser.getEmployeeId()));
            } else if (employeeId != null) {
                employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
            } else {
                List<Employee> all = employeeRepository.findAll();
                if (all.isEmpty()) {
                    throw new ResourceNotFoundException("Employee", "all", "none found");
                }
                employee = all.get(0);
            }
        }

        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1);

        ZoneId ist = ZoneId.of("Asia/Kolkata");
        Instant monthStart = firstDayOfMonth.atStartOfDay(ist).toInstant();
        Instant monthEnd = lastDayOfMonth.plusDays(1).atStartOfDay(ist).toInstant();

        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedLeavesInRange(employee.getId(), firstDayOfMonth, lastDayOfMonth);
        Map<LocalDate, LeaveRequest> leavesByDate = new HashMap<>();
        for (LeaveRequest lr : approvedLeaves) {
            LocalDate cur = lr.getStartDate();
            while (!cur.isAfter(lr.getEndDate())) {
                leavesByDate.put(cur, lr);
                cur = cur.plusDays(1);
            }
        }

        List<AttendancePunch> monthlyPunches = punchRepository.findByPunchTimeBetweenOrderByPunchTimeAsc(monthStart, monthEnd)
                .stream()
                .filter(p -> p.getEmployee() != null && p.getEmployee().getId().equals(employee.getId()))
                .collect(Collectors.toList());

        Map<LocalDate, List<AttendancePunch>> punchesByDate = monthlyPunches.stream()
                .collect(Collectors.groupingBy(p -> p.getPunchTime().atZone(ist).toLocalDate()));

        LocalTime standardStart = LocalTime.of(10, 0, 0);
        LocalTime lateThreshold = LocalTime.of(10, 5, 59);
        LocalTime halfDayThreshold = LocalTime.of(11, 0, 59);

        List<MonthlyDayAttendanceItemDto> days = new ArrayList<>();
        int presentCount = 0;
        int lateCount = 0;
        int halfDayCount = 0;
        int absentCount = 0;
        int weekOffCount = 0;
        int lateLwpCount = 0;

        List<Holiday> dbHolidays = holidayRepository.findByDateRange(firstDayOfMonth, lastDayOfMonth);
        Map<LocalDate, String> holidayMap = dbHolidays.stream()
                .collect(Collectors.toMap(Holiday::getHolidayDate, Holiday::getName, (a, b) -> a));

        List<RegularizationRequest> approvedCorrections = regularizationRepository
                .findByStatusOrderByCreatedAtDesc("APPROVED")
                .stream()
                .filter(r -> r.getEmployee() != null && r.getEmployee().getId().equals(employee.getId()))
                .filter(r -> !r.getDate().isBefore(firstDayOfMonth) && !r.getDate().isAfter(lastDayOfMonth))
                .collect(Collectors.toList());

        Map<LocalDate, RegularizationRequest> correctionsByDate = approvedCorrections.stream()
                .collect(Collectors.toMap(
                        RegularizationRequest::getDate,
                        r -> r,
                        (a, b) -> a
                ));

        for (int day = 1; day <= lastDayOfMonth.getDayOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            DayOfWeek dow = date.getDayOfWeek();
            boolean isSunday = (dow == DayOfWeek.SUNDAY);
            LocalDate lastSaturdayOfMonth = date.with(TemporalAdjusters.lastInMonth(DayOfWeek.SATURDAY));
            boolean isLastSaturday = date.equals(lastSaturdayOfMonth);

            String declaredHolidayName = holidayMap.get(date);
            boolean isHoliday = (declaredHolidayName != null);
            boolean isWeekOff = isSunday || isLastSaturday || isHoliday;

            boolean isToday = date.equals(today);
            boolean isFuture = date.isAfter(today);

            String scheduledShift;
            if (isHoliday) {
                scheduledShift = "Holiday (" + declaredHolidayName + ")";
            } else if (isSunday) {
                scheduledShift = "Weekly Off (Sunday)";
            } else if (isLastSaturday) {
                scheduledShift = "Weekly Off (Last Saturday)";
            } else if (dow == DayOfWeek.MONDAY || dow == DayOfWeek.FRIDAY) {
                scheduledShift = "General Shift (10:00 - 19:30)";
            } else if (dow == DayOfWeek.SATURDAY) {
                scheduledShift = "General Shift (10:00 - 18:00)";
            } else {
                scheduledShift = "General Shift (10:00 - 19:00)";
            }

            List<AttendancePunch> dayPunches = punchesByDate.get(date);
            boolean hasPunches = (dayPunches != null && !dayPunches.isEmpty());
            ProcessedAttendanceDay processed = hasPunches ? processDayPunches(dayPunches) : null;

            String firstHalf = null;
            String secondHalf = null;
            String displayCode = null;
            boolean isLate = false;
            int lateMins = 0;
            Integer lateOccurrence = null;
            boolean latePenalty = false;
            boolean isLwpPenalty = false;

            Instant firstIn = processed != null ? processed.getFirstIn() : null;
            Instant lastOut = processed != null ? processed.getLastOut() : null;
            String workingHoursFormatted = processed != null ? processed.getWorkingHoursFormatted() : "--";

            RegularizationRequest correction = correctionsByDate.get(date);
            if (correction != null && correction.getProposedPunchIn() != null && correction.getProposedPunchOut() != null) {
                firstIn = correction.getProposedPunchIn();
                lastOut = correction.getProposedPunchOut();
                long mins = java.time.Duration.between(firstIn, lastOut).toMinutes();
                workingHoursFormatted = (mins / 60) + "h " + (mins % 60) + "m";
                hasPunches = true;
                processed = ProcessedAttendanceDay.builder()
                        .firstIn(firstIn)
                        .lastOut(lastOut)
                        .workedMinutes((int) mins)
                        .workingHoursFormatted(workingHoursFormatted)
                        .punches(processed != null ? processed.getPunches() : new ArrayList<>())
                        .punchCount(processed != null ? processed.getPunchCount() : 0)
                        .build();
            }

            LeaveRequest approvedLeave = leavesByDate.get(date);
            if (approvedLeave != null) {
                String leaveCode = approvedLeave.getLeaveType().getCode().toUpperCase();
                if (approvedLeave.isHalfDay()) {
                    boolean isFirstHalf = "FIRST_HALF".equalsIgnoreCase(approvedLeave.getHalfDaySession());
                    if (isFirstHalf) {
                        firstHalf = leaveCode;
                        secondHalf = hasPunches ? "P" : "A";
                    } else {
                        firstHalf = hasPunches ? "P" : "A";
                        secondHalf = leaveCode;
                    }
                    displayCode = firstHalf + " | " + secondHalf;
                    halfDayCount++;
                } else {
                    firstHalf = leaveCode;
                    secondHalf = leaveCode;
                    displayCode = leaveCode + " | " + leaveCode;
                }
            } else if (isHoliday) {
                weekOffCount++;
                if (hasPunches) {
                    firstHalf = "P";
                    secondHalf = "P";
                    displayCode = "P | P";
                    presentCount++;
                } else {
                    firstHalf = "O";
                    secondHalf = "O";
                    displayCode = "O | O";
                }
            } else if (isWeekOff) {
                weekOffCount++;
                if (hasPunches) {
                    firstHalf = "P";
                    secondHalf = "P";
                    displayCode = "P | P";
                } else {
                    firstHalf = "O";
                    secondHalf = "O";
                    displayCode = "O | O";
                }
            } else if (!hasPunches) {
                absentCount++;
                firstHalf = "A";
                secondHalf = "A";
                displayCode = "A | A";
            } else {
                LocalTime firstInTime = firstIn.atZone(ist).toLocalTime();
                LocalTime lastOutTime = (lastOut != null) ? lastOut.atZone(ist).toLocalTime() : null;

                if (firstInTime.isAfter(lateThreshold)) {
                    isLate = true;
                    lateMins = (int) java.time.Duration.between(standardStart, firstInTime).toMinutes();
                    lateCount++;
                    lateOccurrence = lateCount;
                }

                if (isLate && lateCount >= 3 && (lateCount % 2 == 1)) {
                    firstHalf = "LWP";
                    secondHalf = "LWP";
                    displayCode = "LWP | LWP";
                    latePenalty = true;
                    isLwpPenalty = true;
                    lateLwpCount++;
                    presentCount++;
                } else if (isLate && lateCount >= 4 && (lateCount % 2 == 0)) {
                    firstHalf = "AB";
                    secondHalf = "PR";
                    displayCode = "AB | PR";
                    latePenalty = true;
                    isLwpPenalty = false;
                    halfDayCount++;
                } else if (firstInTime.isAfter(halfDayThreshold) && !isLate) {
                    firstHalf = "AB";
                    secondHalf = "PR";
                    displayCode = "AB | PR";
                    isLwpPenalty = false;
                    halfDayCount++;
                } else if (date.isBefore(today) && lastOutTime != null && processed.getWorkedMinutes() < 240 && lastOutTime.isBefore(LocalTime.of(14, 0))) {
                    firstHalf = "PR";
                    secondHalf = "AB";
                    displayCode = "PR | AB";
                    isLwpPenalty = false;
                    halfDayCount++;
                } else {
                    firstHalf = "P";
                    secondHalf = "P";
                    displayCode = "P | P";
                    isLwpPenalty = false;
                    presentCount++;
                }
            }

            days.add(MonthlyDayAttendanceItemDto.builder()
                    .date(date)
                    .dayOfMonth(day)
                    .dayOfWeek(dow.name().substring(0, 3))
                    .isCurrentMonth(true)
                    .isToday(isToday)
                    .isFuture(isFuture)
                    .isWeekOff(isWeekOff)
                    .isHoliday(isHoliday)
                    .holidayName(declaredHolidayName)
                    .firstHalfStatus(firstHalf)
                    .secondHalfStatus(secondHalf)
                    .displayCode(displayCode)
                    .scheduledShift(scheduledShift)
                    .isLate(isLate)
                    .lateMinutes(lateMins)
                    .lateOccurrenceNumber(lateOccurrence)
                    .isLatePenaltyApplied(latePenalty)
                    .isLwpPenaltyApplied(isLwpPenalty)
                    .firstIn(firstIn)
                    .lastOut(lastOut)
                    .workingHoursFormatted(workingHoursFormatted)
                    .punchCount(processed != null ? processed.getPunchCount() : 0)
                    .punches(processed != null ? processed.getPunches() : new ArrayList<>())
                    .build());
        }

        String monthTitle = firstDayOfMonth.getMonth().name().charAt(0) +
                firstDayOfMonth.getMonth().name().substring(1).toLowerCase() + " " + year;

        return EmployeeMonthlyAttendanceDto.builder()
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getFullName())
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : "General")
                .branchName(employee.getBranch() != null ? employee.getBranch().getName() : "Headquarters")
                .year(year)
                .lateLwpDays(lateLwpCount)
                .month(month)
                .monthName(monthTitle)
                .totalDays(lastDayOfMonth.getDayOfMonth())
                .presentDays(presentCount)
                .lateDays(lateCount)
                .halfDays(halfDayCount)
                .absentDays(absentCount)
                .weekOffDays(weekOffCount)
                .days(days)
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlyEmployeeRegisterDto getMonthlyRegister(Integer yearParam, Integer monthParam) {
        LocalDate today = LocalDate.now(IST_ZONE);
        int year = (yearParam != null && yearParam > 2000) ? yearParam : today.getYear();
        int month = (monthParam != null && monthParam >= 1 && monthParam <= 12) ? monthParam : today.getMonthValue();

        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1);
        int totalDaysInMonth = lastDayOfMonth.getDayOfMonth();
        String monthTitle = firstDayOfMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + year;

        List<Employee> allEmployees = employeeRepository.findAll();
        List<MonthlyEmployeeRegisterDto.MonthlyEmployeeRegisterItemDto> records = new ArrayList<>();

        for (Employee emp : allEmployees) {
            try {
                EmployeeMonthlyAttendanceDto empMonthly = getEmployeeMonthlyAttendance(emp.getId(), year, month);

                double leaveDays = 0.0;
                List<String> leaveTypesSummary = new ArrayList<>();
                List<LeaveRequest> empLeaves = leaveRequestRepository.findApprovedLeavesInRange(emp.getId(), firstDayOfMonth, lastDayOfMonth);
                for (LeaveRequest lr : empLeaves) {
                    double d = lr.getTotalDays() != null ? lr.getTotalDays().doubleValue() : 1.0;
                    leaveDays += d;
                    String typeName = lr.getLeaveType() != null ? lr.getLeaveType().getCode() : "LV";
                    leaveTypesSummary.add(d + "d " + typeName);
                }

                int workingDays = totalDaysInMonth - empMonthly.getWeekOffDays();
                int extraLateMarks = Math.max(0, empMonthly.getLateDays() - 2);
                int lateLwpDays = (extraLateMarks + 1) / 2;
                int lateHalfDayPenalties = extraLateMarks / 2;

                double payableDays = Math.max(0,
                        empMonthly.getPresentDays()
                                + empMonthly.getWeekOffDays()
                                + leaveDays
                                - (empMonthly.getHalfDays() * 0.5)
                                - lateLwpDays
                );
                int attPercentage = workingDays > 0 ? (int) Math.round((empMonthly.getPresentDays() / (double) workingDays) * 100) : 0;

                records.add(MonthlyEmployeeRegisterDto.MonthlyEmployeeRegisterItemDto.builder()
                        .employeeId(emp.getId())
                        .employeeCode(emp.getEmployeeCode())
                        .employeeName(emp.getFullName())
                        .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : "General")
                        .branchName(emp.getBranch() != null ? emp.getBranch().getName() : "Headquarters")
                        .avatar(null)
                        .lateHalfDayPenalties(lateHalfDayPenalties)
                        .lateLwpDays(lateLwpDays)
                        .totalDays(totalDaysInMonth)
                        .workingDays(workingDays)
                        .presentDays(empMonthly.getPresentDays())
                        .leaveDays(leaveDays)
                        .leaveBreakdown(leaveTypesSummary.isEmpty() ? "None" : String.join(", ", leaveTypesSummary))
                        .halfDays(empMonthly.getHalfDays())
                        .lateDays(empMonthly.getLateDays())
                        .absentDays(empMonthly.getAbsentDays())
                        .weekOffDays(empMonthly.getWeekOffDays())
                        .payableDays(payableDays)
                        .attendancePercentage(attPercentage)
                        .build());
            } catch (Exception e) {
                log.warn("Could not compute monthly register for employee {}: {}", emp.getId(), e.getMessage());
            }
        }

        return MonthlyEmployeeRegisterDto.builder()
                .year(year)
                .month(month)
                .monthName(monthTitle)
                .totalDaysInMonth(totalDaysInMonth)
                .totalEmployees(records.size())
                .records(records)
                .build();
    }

    private BiometricEmployeeMappingDto toMappingDto(BiometricEmployeeMapping entity) {
        return BiometricEmployeeMappingDto.builder()
                .id(entity.getId())
                .sourceSystem(entity.getSourceSystem())
                .biometricEmpCode(entity.getBiometricEmpCode())
                .employeeId(entity.getEmployee().getId())
                .employeeCode(entity.getEmployee().getEmployeeCode())
                .employeeName(entity.getEmployee().getFullName())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AttendancePunchResponse toPunchResponse(AttendancePunch p) {
        return AttendancePunchResponse.builder()
                .id(p.getId())
                .employeeId(p.getEmployee() != null ? p.getEmployee().getId() : null)
                .employeeName(p.getEmployee() != null ? p.getEmployee().getFullName() : "UNRESOLVED")
                .employeeCode(p.getEmployeeCode())
                .sourceSystem(p.getSourceSystem())
                .sourceRecordId(p.getSourceRecordId())
                .punchTime(p.getPunchTime())
                .punchType(p.getPunchType())
                .mappingStatus(p.getMappingStatus())
                .rawPayload(p.getRawPayload())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private String cleanPunchType(String updateFlag) {
        if (updateFlag == null) return "IN";
        String flag = updateFlag.trim().toUpperCase();
        return ("OUT".equals(flag) || "0".equals(flag) || flag.contains("OUT")) ? "OUT" : "IN";
    }

    public AttendanceSyncState getSyncState() {
        return syncStateRepository.findBySourceSystem(SOURCE_SYSTEM)
                .orElse(AttendanceSyncState.builder()
                        .sourceSystem(SOURCE_SYSTEM)
                        .lastSourceRecordId(0L)
                        .lastSyncStatus("IDLE")
                        .lastSyncMessage("No sync has run yet")
                        .build());
    }
}