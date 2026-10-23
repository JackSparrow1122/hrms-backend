package com.gahrms.backend.modules.attendance.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "etimetrack.datasource.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class EtimeTrackDataSourceConfig {

    private final EtimeTrackProperties properties;

    @Bean(name = "etimeTrackDataSource")
    public DataSource etimeTrackDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("HikariPool-eTimeTrack-SqlServer");
        config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        config.setJdbcUrl(properties.buildJdbcUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword() != null ? properties.getPassword() : "");

        // Safety Settings for Secondary Diagnostic / Ingestion DataSource:
        // 1. -1 ensures Spring Boot starts even if eTimeTrack SQL Server is unreachable
        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(Math.max(properties.getLoginTimeoutSeconds() * 1000L, 3000L));
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(0);
        config.setIdleTimeout(30000);
        config.setReadOnly(true);

        log.info("Initialized secondary eTimeTrack SQL Server DataSource configuration (Enabled: {}, Target: {})",
                properties.isEnabled(), properties.buildJdbcUrl().replaceAll(";password=[^;]*", ""));

        return new HikariDataSource(config);
    }

    @Bean(name = "etimeTrackJdbcTemplate")
    public JdbcTemplate etimeTrackJdbcTemplate(@Qualifier("etimeTrackDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "etimeTrackNamedJdbcTemplate")
    public NamedParameterJdbcTemplate etimeTrackNamedJdbcTemplate(@Qualifier("etimeTrackDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
