package com.gahrms.backend.modules.attendance;

import com.gahrms.backend.modules.attendance.client.ETimeTrackSqlServerClient;
import com.gahrms.backend.modules.attendance.config.EtimeTrackProperties;
import com.gahrms.backend.modules.attendance.dto.BiometricPushBatchRequest;
import com.gahrms.backend.modules.attendance.dto.BiometricPushBatchRequest.BiometricPushItemDto;
import com.gahrms.backend.modules.attendance.dto.ConnectionDiagnosticResult;
import com.gahrms.backend.modules.attendance.dto.ETimeTrackRawPunchDto;
import com.gahrms.backend.modules.attendance.dto.SyncSummaryDto;
import com.gahrms.backend.modules.attendance.entity.AttendancePunch;
import com.gahrms.backend.modules.attendance.entity.AttendanceSyncState;
import com.gahrms.backend.modules.attendance.entity.BiometricEmployeeMapping;
import com.gahrms.backend.modules.attendance.repository.AttendancePunchRepository;
import com.gahrms.backend.modules.attendance.repository.AttendanceSyncStateRepository;
import com.gahrms.backend.modules.attendance.repository.BiometricEmployeeMappingRepository;
import com.gahrms.backend.modules.attendance.service.AttendanceSyncService;
import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceSyncServiceTest {

    @Mock
    private ETimeTrackSqlServerClient sqlServerClient;

    @Mock
    private AttendancePunchRepository punchRepository;

    @Mock
    private AttendanceSyncStateRepository syncStateRepository;

    @Mock
    private BiometricEmployeeMappingRepository mappingRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EtimeTrackProperties etimeTrackProperties;

    @InjectMocks
    private AttendanceSyncService syncService;

    private Employee mockEmployee1;
    private AttendanceSyncState mockSyncState;

    @BeforeEach
    void setUp() {
        mockEmployee1 = Employee.builder()
                .employeeCode("EMP-0001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();
        mockEmployee1.setId(10L);

        mockSyncState = AttendanceSyncState.builder()
                .id(1L)
                .sourceSystem("ESSL_ETIMETRACK")
                .lastSourceRecordId(100L)
                .lastSyncStatus("SUCCESS")
                .totalRecordsSynced(50L)
                .build();
    }

    private void mockHealthyConnection() {
        when(sqlServerClient.testConnection()).thenReturn(
                ConnectionDiagnosticResult.builder()
                        .connected(true)
                        .status("SUCCESS")
                        .message("Connected successfully")
                        .build()
        );
        when(syncStateRepository.findBySourceSystem("ESSL_ETIMETRACK")).thenReturn(Optional.of(mockSyncState));
    }

    @Test
    @DisplayName("1. Direct Employee Code Matching: EMP-0001 matches existing employee")
    void testDirectEmployeeCodeMatching() {
        mockHealthyConnection();

        ETimeTrackRawPunchDto rawPunch = ETimeTrackRawPunchDto.builder()
                .id(101L)
                .empCode("EMP-0001")
                .punchDate(Instant.now())
                .updateFlag("IN")
                .build();

        when(sqlServerClient.fetchPunchesAfterId(100L, 100)).thenReturn(List.of(rawPunch));
        when(punchRepository.existsBySourceSystemAndSourceRecordId("ESSL_ETIMETRACK", 101L)).thenReturn(false);
        when(mappingRepository.findBySourceSystemAndBiometricEmpCode("ESSL_ETIMETRACK", "EMP-0001")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCode("EMP-0001")).thenReturn(Optional.of(mockEmployee1));

        SyncSummaryDto summary = syncService.syncIncrementalPunches(100);

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getInsertedCount()).isEqualTo(1);
        assertThat(summary.getUnmappedEmployeesCount()).isEqualTo(0);
        assertThat(summary.getNewCursorId()).isEqualTo(101L);

        ArgumentCaptor<AttendancePunch> punchCaptor = ArgumentCaptor.forClass(AttendancePunch.class);
        verify(punchRepository).save(punchCaptor.capture());
        AttendancePunch saved = punchCaptor.getValue();
        assertThat(saved.getEmployee()).isEqualTo(mockEmployee1);
        assertThat(saved.getMappingStatus()).isEqualTo("MAPPED");
        assertThat(saved.getSourceRecordId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("2. Explicit Biometric Mapping: GA01 mapped explicitly to EMP-0001")
    void testExplicitBiometricMapping_GA01_To_Employee() {
        mockHealthyConnection();

        ETimeTrackRawPunchDto rawPunch = ETimeTrackRawPunchDto.builder()
                .id(102L)
                .empCode("GA01")
                .punchDate(Instant.now())
                .updateFlag("OUT")
                .build();

        when(sqlServerClient.fetchPunchesAfterId(100L, 100)).thenReturn(List.of(rawPunch));
        when(punchRepository.existsBySourceSystemAndSourceRecordId("ESSL_ETIMETRACK", 102L)).thenReturn(false);

        BiometricEmployeeMapping explicitMapping = BiometricEmployeeMapping.builder()
                .id(1L)
                .sourceSystem("ESSL_ETIMETRACK")
                .biometricEmpCode("GA01")
                .employee(mockEmployee1)
                .build();
        when(mappingRepository.findBySourceSystemAndBiometricEmpCode("ESSL_ETIMETRACK", "GA01"))
                .thenReturn(Optional.of(explicitMapping));

        SyncSummaryDto summary = syncService.syncIncrementalPunches(100);

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getInsertedCount()).isEqualTo(1);
        assertThat(summary.getUnmappedEmployeesCount()).isEqualTo(0);

        ArgumentCaptor<AttendancePunch> punchCaptor = ArgumentCaptor.forClass(AttendancePunch.class);
        verify(punchRepository).save(punchCaptor.capture());
        AttendancePunch saved = punchCaptor.getValue();
        assertThat(saved.getEmployee()).isEqualTo(mockEmployee1);
        assertThat(saved.getMappingStatus()).isEqualTo("MAPPED");
        assertThat(saved.getPunchType()).isEqualTo("OUT");
    }

    @Test
    @DisplayName("3. Unmatched Employee: Unknown code GA99 is preserved as UNMAPPED, employee=null")
    void testUnmatchedEmployee_PreservedAsUnmapped() {
        mockHealthyConnection();

        ETimeTrackRawPunchDto rawPunch = ETimeTrackRawPunchDto.builder()
                .id(103L)
                .empCode("GA99")
                .punchDate(Instant.now())
                .updateFlag("IN")
                .build();

        when(sqlServerClient.fetchPunchesAfterId(100L, 100)).thenReturn(List.of(rawPunch));
        when(punchRepository.existsBySourceSystemAndSourceRecordId("ESSL_ETIMETRACK", 103L)).thenReturn(false);
        when(mappingRepository.findBySourceSystemAndBiometricEmpCode("ESSL_ETIMETRACK", "GA99")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCode("GA99")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCodeIgnoreCase("GA99")).thenReturn(Optional.empty());

        SyncSummaryDto summary = syncService.syncIncrementalPunches(100);

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getInsertedCount()).isEqualTo(1);
        assertThat(summary.getUnmappedEmployeesCount()).isEqualTo(1);

        ArgumentCaptor<AttendancePunch> punchCaptor = ArgumentCaptor.forClass(AttendancePunch.class);
        verify(punchRepository).save(punchCaptor.capture());
        AttendancePunch saved = punchCaptor.getValue();
        assertThat(saved.getEmployee()).isNull();
        assertThat(saved.getMappingStatus()).isEqualTo("UNMAPPED");
        assertThat(saved.getEmployeeCode()).isEqualTo("GA99");
    }

    @Test
    @DisplayName("4. Duplicate Punch Prevention: Pre-check skips already ingested sourceRecordId")
    void testDuplicatePunchPrevention_PreCheck() {
        mockHealthyConnection();

        ETimeTrackRawPunchDto rawPunch = ETimeTrackRawPunchDto.builder()
                .id(101L)
                .empCode("EMP-0001")
                .build();

        when(sqlServerClient.fetchPunchesAfterId(100L, 100)).thenReturn(List.of(rawPunch));
        when(punchRepository.existsBySourceSystemAndSourceRecordId("ESSL_ETIMETRACK", 101L)).thenReturn(true);

        SyncSummaryDto summary = syncService.syncIncrementalPunches(100);

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getInsertedCount()).isEqualTo(0);
        assertThat(summary.getSkippedDuplicatesCount()).isEqualTo(1);
        verify(punchRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Duplicate Punch Concurrency Resilience: Catches DataIntegrityViolationException")
    void testDuplicatePunch_ConcurrentDataIntegrityViolationResilience() {
        mockHealthyConnection();

        ETimeTrackRawPunchDto rawPunch = ETimeTrackRawPunchDto.builder()
                .id(104L)
                .empCode("EMP-0001")
                .build();

        when(sqlServerClient.fetchPunchesAfterId(100L, 100)).thenReturn(List.of(rawPunch));
        when(punchRepository.existsBySourceSystemAndSourceRecordId("ESSL_ETIMETRACK", 104L)).thenReturn(false);
        when(mappingRepository.findBySourceSystemAndBiometricEmpCode("ESSL_ETIMETRACK", "EMP-0001")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCode("EMP-0001")).thenReturn(Optional.of(mockEmployee1));

        // Simulate concurrent insert triggering database unique constraint
        when(punchRepository.save(any(AttendancePunch.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation uk_punch_source"));

        SyncSummaryDto summary = syncService.syncIncrementalPunches(100);

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getInsertedCount()).isEqualTo(0);
        assertThat(summary.getSkippedDuplicatesCount()).isEqualTo(1);
        assertThat(summary.getNewCursorId()).isEqualTo(104L);
    }

    @Test
    @DisplayName("6. Retry and Remapping: Previously unmapped punch becomes MAPPED when explicit mapping is configured")
    void testRetryAndRemappingBehavior() {
        AttendancePunch unmappedPunch = AttendancePunch.builder()
                .id(50L)
                .sourceSystem("ESSL_ETIMETRACK")
                .sourceRecordId(200L)
                .employeeCode("GA02")
                .mappingStatus("UNMAPPED")
                .employee(null)
                .build();

        when(punchRepository.findByMappingStatus("UNMAPPED")).thenReturn(List.of(unmappedPunch));

        // HR adds explicit mapping for GA02 -> mockEmployee1
        BiometricEmployeeMapping explicitMapping = BiometricEmployeeMapping.builder()
                .sourceSystem("ESSL_ETIMETRACK")
                .biometricEmpCode("GA02")
                .employee(mockEmployee1)
                .build();
        when(mappingRepository.findBySourceSystemAndBiometricEmpCode("ESSL_ETIMETRACK", "GA02"))
                .thenReturn(Optional.of(explicitMapping));

        SyncSummaryDto summary = syncService.remapUnmatchedPunches();

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getInsertedCount()).isEqualTo(1); // 1 remapped
        assertThat(summary.getUnmappedEmployeesCount()).isEqualTo(0); // 0 remaining

        ArgumentCaptor<AttendancePunch> captor = ArgumentCaptor.forClass(AttendancePunch.class);
        verify(punchRepository).save(captor.capture());
        assertThat(captor.getValue().getMappingStatus()).isEqualTo("MAPPED");
        assertThat(captor.getValue().getEmployee()).isEqualTo(mockEmployee1);
    }

    @Test
    @DisplayName("7. Ingest Push Batch: Deduplicates and resolves employee mappings from push webhook")
    void testIngestPushBatch_MappedAndDeduplicated() {
        when(syncStateRepository.findBySourceSystem("ESSL_ETIMETRACK")).thenReturn(Optional.of(mockSyncState));

        BiometricPushItemDto item1 = BiometricPushItemDto.builder()
                .serial(301L)
                .emp("EMP-0001")
                .time("2026-10-19 09:30:00")
                .status("IN")
                .build();

        BiometricPushItemDto item2 = BiometricPushItemDto.builder()
                .serial(302L)
                .emp("EMP-0001")
                .time("2026-10-19 18:30:00")
                .status("OUT")
                .build();

        BiometricPushBatchRequest request = BiometricPushBatchRequest.builder()
                .data(List.of(item1, item2))
                .build();

        // 301 is new, 302 is duplicate
        when(punchRepository.existsBySourceSystemAndSourceRecordId("ESSL_ETIMETRACK", 301L)).thenReturn(false);
        when(punchRepository.existsBySourceSystemAndSourceRecordId("ESSL_ETIMETRACK", 302L)).thenReturn(true);
        when(mappingRepository.findBySourceSystemAndBiometricEmpCode("ESSL_ETIMETRACK", "EMP-0001")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCode("EMP-0001")).thenReturn(Optional.of(mockEmployee1));

        SyncSummaryDto summary = syncService.ingestPushBatch(request);

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getFetchedCount()).isEqualTo(2);
        assertThat(summary.getInsertedCount()).isEqualTo(1);
        assertThat(summary.getSkippedDuplicatesCount()).isEqualTo(1);
        assertThat(summary.getNewCursorId()).isEqualTo(302L);

        ArgumentCaptor<AttendancePunch> captor = ArgumentCaptor.forClass(AttendancePunch.class);
        verify(punchRepository, times(1)).save(captor.capture());
        AttendancePunch saved = captor.getValue();
        assertThat(saved.getSourceRecordId()).isEqualTo(301L);
        assertThat(saved.getMappingStatus()).isEqualTo("MAPPED");
        assertThat(saved.getEmployee()).isEqualTo(mockEmployee1);
    }
}
