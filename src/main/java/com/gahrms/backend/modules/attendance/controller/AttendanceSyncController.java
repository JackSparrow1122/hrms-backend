package com.gahrms.backend.modules.attendance.controller;

import com.gahrms.backend.common.dto.ApiResponse;
import com.gahrms.backend.common.dto.PageResponse;
import com.gahrms.backend.modules.attendance.client.ETimeTrackSqlServerClient;
import com.gahrms.backend.modules.attendance.dto.*;
import com.gahrms.backend.modules.attendance.entity.AttendanceSyncState;
import com.gahrms.backend.modules.attendance.entity.Holiday;
import com.gahrms.backend.modules.attendance.repository.HolidayRepository;
import com.gahrms.backend.modules.attendance.service.AttendanceSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance/sync")
@RequiredArgsConstructor
@Tag(name = "Biometric Attendance Sync", description = "Diagnostic, cursor tracking, explicit employee mappings, and manual ingestion endpoints for eSSL eTimeTrack SQL Server")
@SecurityRequirement(name = "BearerAuth")
public class AttendanceSyncController {

    private final AttendanceSyncService attendanceSyncService;
    private final ETimeTrackSqlServerClient sqlServerClient;
    private final HolidayRepository holidayRepository;

    @GetMapping("/diagnostic")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Test eTimeTrack SQL Server connection",
            description = "Performs read-only connection check against the secondary SQL Server datasource and verifies dbo.AttendanceAll table access. Returns sanitized status without exposing passwords."
    )
    public ResponseEntity<ApiResponse<ConnectionDiagnosticResult>> testConnection() {
        ConnectionDiagnosticResult result = sqlServerClient.testConnection();
        return ResponseEntity.ok(ApiResponse.ok("Connection diagnostic complete", result));
    }

    @GetMapping("/sample")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Preview sample punches from eTimeTrack",
            description = "Reads a small sample of recent raw punches directly from SQL Server dbo.AttendanceAll without writing to PostgreSQL."
    )
    public ResponseEntity<ApiResponse<List<ETimeTrackRawPunchDto>>> getSamplePunches(
            @Parameter(description = "Maximum records to fetch (max 50)") @RequestParam(defaultValue = "10") int limit
    ) {
        List<ETimeTrackRawPunchDto> sample = sqlServerClient.fetchSamplePunches(limit);
        return ResponseEntity.ok(ApiResponse.ok("Fetched sample punches from eTimeTrack SQL Server", sample));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Get current sync cursor and status",
            description = "Returns current cursor tracking state, last sync run outcome, and total records synced to date."
    )
    public ResponseEntity<ApiResponse<AttendanceSyncState>> getSyncStatus() {
        AttendanceSyncState state = attendanceSyncService.getSyncState();
        return ResponseEntity.ok(ApiResponse.ok("Current attendance sync cursor state", state));
    }

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Execute manual incremental sync batch",
            description = "Triggers non-overlapping incremental sync fetching punches where ID > lastSourceRecordId, deduping, and mapping explicitly to employees."
    )
    public ResponseEntity<ApiResponse<SyncSummaryDto>> triggerManualSync(
            @Parameter(description = "Batch size for ingestion (default 100)") @RequestParam(defaultValue = "100") int batchSize
    ) {
        SyncSummaryDto summary = attendanceSyncService.syncIncrementalPunches(batchSize);
        return ResponseEntity.ok(ApiResponse.ok("Manual attendance sync execution completed", summary));
    }

    @PostMapping("/push")
    @Operation(
            summary = "Push biometric attendance punches",
            description = "Receives a batch of raw punches pushed from on-premise sync agent. Deduplicates, maps to HRMS employees, and updates sync cursor."
    )
    public ResponseEntity<ApiResponse<SyncSummaryDto>> ingestPunchesPush(
            @RequestBody BiometricPushBatchRequest request
    ) {
        SyncSummaryDto summary = attendanceSyncService.ingestPushBatch(request);
        return ResponseEntity.ok(ApiResponse.ok("Biometric punches ingested successfully", summary));
    }

    @GetMapping("/unmapped")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Review unmapped biometric punches",
            description = "Returns paginated list of punches whose employee code could not be mapped to any HRMS employee, for HR audit and resolution."
    )
    public ResponseEntity<ApiResponse<PageResponse<AttendancePunchResponse>>> getUnmappedPunches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<AttendancePunchResponse> response = attendanceSyncService.getUnmappedPunches(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok("Unmapped biometric punches retrieved for review", response));
    }

    @GetMapping("/punches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(
            summary = "Query all biometric punches with filtering and pagination",
            description = "Returns paginated punches filterable by status (ALL, MAPPED, UNMAPPED), search query, and date."
    )
    public ResponseEntity<ApiResponse<PageResponse<AttendancePunchResponse>>> getAllPunches(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<AttendancePunchResponse> response = attendanceSyncService.getAllPunches(status, search, date, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok("Biometric punches retrieved successfully", response));
    }

    @GetMapping("/daily-roster")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN', 'MANAGER', 'TEAM_LEAD', 'EMPLOYEE')")
    @Operation(
            summary = "Get daily attendance roster aggregated from punches",
            description = "Computes daily roster for all employees and unmapped devices on the specified date with first in, last out, hours worked, late buffer, and status."
    )
    public ResponseEntity<ApiResponse<DailyAttendanceRosterDto>> getDailyRoster(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date
    ) {
        DailyAttendanceRosterDto roster = attendanceSyncService.getDailyRoster(date);
        return ResponseEntity.ok(ApiResponse.ok("Daily attendance roster calculated successfully", roster));
    }

    @GetMapping("/employee-monthly")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN', 'MANAGER', 'TEAM_LEAD', 'EMPLOYEE')")
    @Operation(
            summary = "Get employee 2-half monthly attendance matrix",
            description = "Returns calendar matrix with First Half / Second Half statuses (P|P, O|O, A|A, PR|AB), shift info, and punch timelines."
    )
    public ResponseEntity<ApiResponse<EmployeeMonthlyAttendanceDto>> getEmployeeMonthlyAttendance(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        EmployeeMonthlyAttendanceDto dto = attendanceSyncService.getEmployeeMonthlyAttendance(employeeId, year, month);
        return ResponseEntity.ok(ApiResponse.ok("Employee monthly attendance matrix retrieved", dto));
    }

    @GetMapping("/monthly-register")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN', 'MANAGER', 'TEAM_LEAD', 'EMPLOYEE')")
    @Operation(
            summary = "Get organization monthly attendance & leaves register for all employees",
            description = "Returns aggregated monthly totals (working days, present, leaves, half-days, late penalties, payable days) for all employees."
    )
    public ResponseEntity<ApiResponse<MonthlyEmployeeRegisterDto>> getMonthlyRegister(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        MonthlyEmployeeRegisterDto register = attendanceSyncService.getMonthlyRegister(year, month);
        return ResponseEntity.ok(ApiResponse.ok("Monthly employee register retrieved successfully", register));
    }

    @GetMapping("/detected-codes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "List distinct detected and unmapped biometric machine codes",
            description = "Returns distinct employee codes captured from biometric punches for mapping dropdowns."
    )
    public ResponseEntity<ApiResponse<DetectedBiometricCodesDto>> getDetectedCodes() {
        DetectedBiometricCodesDto codes = attendanceSyncService.getDetectedBiometricCodes();
        return ResponseEntity.ok(ApiResponse.ok("Detected biometric codes retrieved", codes));
    }

    @GetMapping("/mappings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "List explicit biometric employee mappings",
            description = "Returns all configured device code to HRMS employee associations (e.g. GA01 -> Employee #1)."
    )
    public ResponseEntity<ApiResponse<List<BiometricEmployeeMappingDto>>> getMappings() {
        List<BiometricEmployeeMappingDto> mappings = attendanceSyncService.getAllMappings();
        return ResponseEntity.ok(ApiResponse.ok("Configured biometric employee mappings", mappings));
    }

    @PostMapping("/mappings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Add or update explicit biometric employee mapping",
            description = "Maps a biometric machine employee code (e.g., GA01) explicitly to an HRMS Employee ID."
    )
    public ResponseEntity<ApiResponse<BiometricEmployeeMappingDto>> saveMapping(
            @Valid @RequestBody BiometricEmployeeMappingDto request
    ) {
        BiometricEmployeeMappingDto saved = attendanceSyncService.addOrUpdateMapping(request);
        return ResponseEntity.ok(ApiResponse.ok("Biometric employee mapping saved successfully", saved));
    }

    @DeleteMapping("/mappings/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Delete explicit biometric employee mapping",
            description = "Removes an explicit biometric code mapping by ID."
    )
    public ResponseEntity<ApiResponse<Void>> deleteMapping(@PathVariable Long id) {
        attendanceSyncService.deleteMapping(id);
        return ResponseEntity.ok(ApiResponse.ok("Biometric mapping deleted successfully", null));
    }

    @PostMapping("/remap")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Retry and remap unmatched punches",
            description = "Re-evaluates existing UNMAPPED punches against newly configured explicit mappings and links them to employees."
    )
    public ResponseEntity<ApiResponse<SyncSummaryDto>> remapUnmatchedPunches() {
        SyncSummaryDto summary = attendanceSyncService.remapUnmatchedPunches();
        return ResponseEntity.ok(ApiResponse.ok("Remapping execution completed", summary));
    }

    // 👇 HOLIDAY ENDPOINTS

    @GetMapping("/holidays")
    @Operation(summary = "Get official organization holidays", description = "Returns official holidays declared by HR Admin.")
    public ResponseEntity<ApiResponse<List<Holiday>>> getOfficialHolidays() {
        List<Holiday> holidays = holidayRepository.findAllByOrderByHolidayDateAsc();
        return ResponseEntity.ok(ApiResponse.ok("Official organization holidays", holidays));
    }
    @PostMapping("/holidays")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Declare a new holiday")
    public ResponseEntity<ApiResponse<Holiday>> addHoliday(@RequestBody Holiday holiday) {
        try {
            Holiday existing = holidayRepository.findByHolidayDate(holiday.getHolidayDate()).orElse(null);
            
            if (existing != null) {
                existing.setName(holiday.getName());
                return ResponseEntity.ok(ApiResponse.ok("Holiday updated", holidayRepository.save(existing)));
            } else {
                Holiday newHoliday = Holiday.builder()
                        .holidayDate(holiday.getHolidayDate())
                        .name(holiday.getName())
                        .build();
                return ResponseEntity.ok(ApiResponse.created("Holiday declared", holidayRepository.save(newHoliday)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build(); // 👈 simple 500 return
        }
    }

    @DeleteMapping("/holidays/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Remove a declared holiday")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable Long id) {
        holidayRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Holiday removed successfully", null));
    }
}