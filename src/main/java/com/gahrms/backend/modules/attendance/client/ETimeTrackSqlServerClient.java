package com.gahrms.backend.modules.attendance.client;

import com.gahrms.backend.modules.attendance.config.EtimeTrackProperties;
import com.gahrms.backend.modules.attendance.dto.ConnectionDiagnosticResult;
import com.gahrms.backend.modules.attendance.dto.ETimeTrackRawPunchDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ETimeTrackSqlServerClient {

    private final EtimeTrackProperties properties;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public ETimeTrackSqlServerClient(
            EtimeTrackProperties properties,
            @Autowired(required = false) @Qualifier("etimeTrackNamedJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate
    ) {
        this.properties = properties;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    private static final RowMapper<ETimeTrackRawPunchDto> PUNCH_ROW_MAPPER = (rs, rowNum) -> {
        Timestamp punchTs = rs.getTimestamp("PunchDate");
        return ETimeTrackRawPunchDto.builder()
                .id(rs.getLong("ID"))
                .empCode(rs.getString("EMP_CODE") != null ? rs.getString("EMP_CODE").trim() : "")
                .punchDate(punchTs != null ? punchTs.toInstant() : null)
                .updateFlag(rs.getString("UpdateFlag") != null ? rs.getString("UpdateFlag").trim().toUpperCase() : "")
                .build();
    };

    /**
     * Read-only diagnostic operation: tests connectivity and verifies dbo.AttendanceAll accessibility
     * without writing or modifying anything in PostgreSQL or SQL Server.
     */
    public ConnectionDiagnosticResult testConnection() {
        if (!properties.isEnabled() || namedJdbcTemplate == null) {
            return ConnectionDiagnosticResult.builder()
                    .connected(false)
                    .status("DISABLED")
                    .message("eTimeTrack SQL Server integration is currently disabled in configuration (ETIMETRACK_ENABLED=false).")
                    .targetHost(properties.getHost())
                    .targetPort(properties.getPort())
                    .targetDatabase(properties.getDatabaseName())
                    .sourceTableAccessible(false)
                    .sampleRecordCount(0L)
                    .checkedAt(Instant.now())
                    .build();
        }

        try {
            log.info("Running read-only diagnostic against eTimeTrack SQL Server ({}:{})", properties.getHost(), properties.getPort());

            // 1. Check database connectivity without reading any attendance records
            String pingSql = "SELECT 1";
            Integer ping = namedJdbcTemplate.queryForObject(pingSql, new MapSqlParameterSource(), Integer.class);

            // 2. Verify table metadata existence in INFORMATION_SCHEMA without reading rows from dbo.AttendanceAll
            String tableCheckSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = 'AttendanceAll'";
            Integer tableCount = namedJdbcTemplate.queryForObject(tableCheckSql, new MapSqlParameterSource(), Integer.class);
            boolean tableExists = tableCount != null && tableCount > 0;

            return ConnectionDiagnosticResult.builder()
                    .connected(true)
                    .status("SUCCESS")
                    .message("Successfully connected to eTimeTrack SQL Server database '" + properties.getDatabaseName() + "'. Table dbo.AttendanceAll exists: " + tableExists)
                    .targetHost(properties.getHost())
                    .targetPort(properties.getPort())
                    .targetDatabase(properties.getDatabaseName())
                    .sourceTableAccessible(tableExists)
                    .sampleRecordCount(0L) // No attendance records read
                    .checkedAt(Instant.now())
                    .build();

        } catch (Exception ex) {
            String rawMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown connection error";
            String sanitizedStatus = "ERROR";
            String sanitizedMessage = "Failed to connect to eTimeTrack SQL Server: " + rawMessage;

            if (rawMessage.contains("Connection refused") || rawMessage.contains("The TCP/IP connection to the host") || rawMessage.contains("socket error")) {
                sanitizedStatus = "CONNECTION_REFUSED";
                sanitizedMessage = "Could not reach SQL Server at " + properties.getHost() + ":" + properties.getPort() + ". Server might be offline or port is closed.";
            } else if (rawMessage.contains("Login failed") || rawMessage.contains("authentication")) {
                sanitizedStatus = "AUTHENTICATION_FAILED";
                sanitizedMessage = "SQL Server authentication failed for user '" + properties.getUsername() + "'. Please verify SQL Server credentials.";
            } else if (rawMessage.contains("Invalid object name 'dbo.AttendanceAll'")) {
                sanitizedStatus = "TABLE_NOT_FOUND";
                sanitizedMessage = "Connected to database '" + properties.getDatabaseName() + "', but table dbo.AttendanceAll was not found.";
            } else if (rawMessage.contains("timed out") || rawMessage.contains("timeout")) {
                sanitizedStatus = "TIMEOUT";
                sanitizedMessage = "Connection timed out connecting to " + properties.getHost() + ":" + properties.getPort() + ". Check network routing or firewall.";
            }

            log.warn("eTimeTrack diagnostic failed: [{}] {}", sanitizedStatus, sanitizedMessage);

            return ConnectionDiagnosticResult.builder()
                    .connected(false)
                    .status(sanitizedStatus)
                    .message(sanitizedMessage)
                    .targetHost(properties.getHost())
                    .targetPort(properties.getPort())
                    .targetDatabase(properties.getDatabaseName())
                    .sourceTableAccessible(false)
                    .sampleRecordCount(0L)
                    .checkedAt(Instant.now())
                    .build();
        }
    }

    /**
     * Reads a small sample of recent raw punches without writing to PostgreSQL.
     */
    public List<ETimeTrackRawPunchDto> fetchSamplePunches(int limit) {
        if (!properties.isEnabled() || namedJdbcTemplate == null) {
            return Collections.emptyList();
        }

        String sql = "SELECT TOP (:limit) ID, EMP_CODE, PunchDate, UpdateFlag " +
                     "FROM dbo.AttendanceAll " +
                     "WHERE UpdateFlag IN ('IN', 'OUT') " +
                     "ORDER BY ID DESC";

        MapSqlParameterSource params = new MapSqlParameterSource("limit", Math.min(limit, 50));
        return namedJdbcTemplate.query(sql, params, PUNCH_ROW_MAPPER);
    }

    /**
     * Reads an incremental batch of new punches ordered strictly by source ID ASC.
     */
    public List<ETimeTrackRawPunchDto> fetchPunchesAfterId(long lastSyncedId, int batchSize) {
        if (!properties.isEnabled() || namedJdbcTemplate == null) {
            return Collections.emptyList();
        }

        String sql = "SELECT TOP (:batchSize) ID, EMP_CODE, PunchDate, UpdateFlag " +
                     "FROM dbo.AttendanceAll " +
                     "WHERE ID > :lastSyncedId " +
                     "  AND UpdateFlag IN ('IN', 'OUT') " +
                     "ORDER BY ID ASC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lastSyncedId", lastSyncedId)
                .addValue("batchSize", Math.max(1, Math.min(batchSize, 500)));

        return namedJdbcTemplate.query(sql, params, PUNCH_ROW_MAPPER);
    }
}
