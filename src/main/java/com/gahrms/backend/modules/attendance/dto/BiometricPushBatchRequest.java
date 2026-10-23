package com.gahrms.backend.modules.attendance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricPushBatchRequest {

    @JsonProperty("data")
    private List<BiometricPushItemDto> data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BiometricPushItemDto {

        @JsonProperty("emp")
        @JsonAlias({"empCode", "emp_code", "employeeCode"})
        private String emp;

        @JsonProperty("time")
        @JsonAlias({"punchDate", "punch_time", "dateTime"})
        private String time;

        @JsonProperty("status")
        @JsonAlias({"updateFlag", "punch_type", "type"})
        private String status; // "IN" or "OUT"

        @JsonProperty("serial")
        @JsonAlias({"id", "source_record_id", "punchId"})
        private Long serial;
    }
}
