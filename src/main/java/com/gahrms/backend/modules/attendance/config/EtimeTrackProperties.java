package com.gahrms.backend.modules.attendance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "etimetrack.datasource")
public class EtimeTrackProperties {

    private boolean enabled = false;
    private String host = "localhost";
    private int port = 1433;
    private String instanceName = "";
    private String databaseName = "etimetracklite1";
    private String username = "sa";
    private String password = "";
    private int loginTimeoutSeconds = 5;
    private boolean trustServerCertificate = true;

    /**
     * Builds safe, sanitized JDBC URL without exposing password or credentials.
     */
    public String buildJdbcUrl() {
        StringBuilder sb = new StringBuilder("jdbc:sqlserver://");
        sb.append(host);
        if (instanceName != null && !instanceName.trim().isEmpty()) {
            sb.append("\\").append(instanceName.trim());
        } else if (port > 0) {
            sb.append(":").append(port);
        }
        sb.append(";databaseName=").append(databaseName);
        sb.append(";trustServerCertificate=").append(trustServerCertificate);
        sb.append(";loginTimeout=").append(loginTimeoutSeconds);
        sb.append(";encrypt=false");
        return sb.toString();
    }
}
