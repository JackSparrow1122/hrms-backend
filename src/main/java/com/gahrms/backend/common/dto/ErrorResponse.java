package com.gahrms.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String errorCode;
    private String message;
    private String correlationId;
    private Map<String, String> validationErrors;
    private List<String> details;
    private Instant timestamp = Instant.now();

    public ErrorResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ErrorResponse response = new ErrorResponse();

        public Builder status(int status) { response.status = status; return this; }
        public Builder errorCode(String errorCode) { response.errorCode = errorCode; return this; }
        public Builder message(String message) { response.message = message; return this; }
        public Builder correlationId(String correlationId) { response.correlationId = correlationId; return this; }
        public Builder validationErrors(Map<String, String> errors) { response.validationErrors = errors; return this; }
        public Builder details(List<String> details) { response.details = details; return this; }
        public Builder timestamp(Instant timestamp) { response.timestamp = timestamp; return this; }

        public ErrorResponse build() { return response; }
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Map<String, String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; }

    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
