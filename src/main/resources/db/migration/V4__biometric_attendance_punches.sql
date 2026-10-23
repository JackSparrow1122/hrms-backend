-- =============================================================================
-- GA-HRMS Biometric Attendance Ingestion & Raw Punch Auditing - Migration V4
-- Supports eSSL / eTimeTrack MSSQL Ingestion with Provenance & Idempotency
-- =============================================================================

-- 1. Raw Biometric Attendance Punches Table
CREATE TABLE IF NOT EXISTS attendance_punches (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT REFERENCES employees(id) ON DELETE SET NULL,
    employee_code VARCHAR(50) NOT NULL,
    source_system VARCHAR(50) DEFAULT 'ESSL_ETIMETRACK' NOT NULL,
    source_record_id BIGINT NOT NULL,
    punch_time TIMESTAMP WITH TIME ZONE NOT NULL,
    punch_type VARCHAR(10) NOT NULL, -- 'IN' or 'OUT'
    mapping_status VARCHAR(20) DEFAULT 'MAPPED' NOT NULL, -- 'MAPPED', 'UNMAPPED'
    raw_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_punch_source UNIQUE (source_system, source_record_id)
);

CREATE INDEX IF NOT EXISTS idx_punches_emp_time ON attendance_punches(employee_id, punch_time);
CREATE INDEX IF NOT EXISTS idx_punches_emp_code ON attendance_punches(employee_code);
CREATE INDEX IF NOT EXISTS idx_punches_time ON attendance_punches(punch_time);
CREATE INDEX IF NOT EXISTS idx_punches_mapping ON attendance_punches(mapping_status);

-- 2. Biometric Ingestion Sync State / Cursor Tracker Table
CREATE TABLE IF NOT EXISTS attendance_sync_state (
    id BIGSERIAL PRIMARY KEY,
    source_system VARCHAR(50) NOT NULL UNIQUE,
    last_source_record_id BIGINT DEFAULT 0 NOT NULL,
    last_sync_timestamp TIMESTAMP WITH TIME ZONE,
    last_sync_status VARCHAR(30) DEFAULT 'IDLE' NOT NULL, -- 'IDLE', 'RUNNING', 'SUCCESS', 'FAILED'
    last_sync_message VARCHAR(500),
    total_records_synced BIGINT DEFAULT 0 NOT NULL,
    last_batch_count INTEGER DEFAULT 0 NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Seed Initial Sync Cursor Record for eSSL eTimeTrack
INSERT INTO attendance_sync_state (source_system, last_source_record_id, last_sync_status, last_sync_message)
VALUES ('ESSL_ETIMETRACK', 0, 'IDLE', 'Ready for initial diagnostic and backfill')
ON CONFLICT (source_system) DO NOTHING;

-- 3. Explicit Biometric Device to HRMS Employee Mapping Table
-- Allows mapping eTimeTrack codes (e.g. 'GA01', 'GA02', '101') explicitly to HRMS Employee records
CREATE TABLE IF NOT EXISTS biometric_employee_mappings (
    id BIGSERIAL PRIMARY KEY,
    source_system VARCHAR(50) DEFAULT 'ESSL_ETIMETRACK' NOT NULL,
    biometric_emp_code VARCHAR(50) NOT NULL,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    notes VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_biometric_emp_mapping UNIQUE (source_system, biometric_emp_code)
);

CREATE INDEX IF NOT EXISTS idx_biometric_map_code ON biometric_employee_mappings(source_system, biometric_emp_code);
CREATE INDEX IF NOT EXISTS idx_biometric_map_emp ON biometric_employee_mappings(employee_id);
