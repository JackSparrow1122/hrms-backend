package com.gahrms.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String errorCode;
    private String message;
    private String correlationId;
    private Map<String, String> validationErrors;
    private List<String> details;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
