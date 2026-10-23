package com.gahrms.backend.modules.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "attendance_sync_state")
public class AttendanceSyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_system", nullable = false, unique = true, length = 50)
    private String sourceSystem;

    @Builder.Default
    @Column(name = "last_source_record_id", nullable = false)
    private Long lastSourceRecordId = 0L;

    @Column(name = "last_sync_timestamp")
    private Instant lastSyncTimestamp;

    @Builder.Default
    @Column(name = "last_sync_status", nullable = false, length = 30)
    private String lastSyncStatus = "IDLE"; // "IDLE", "RUNNING", "SUCCESS", "FAILED"

    @Column(name = "last_sync_message", length = 500)
    private String lastSyncMessage;

    @Builder.Default
    @Column(name = "total_records_synced", nullable = false)
    private Long totalRecordsSynced = 0L;

    @Builder.Default
    @Column(name = "last_batch_count", nullable = false)
    private Integer lastBatchCount = 0;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
