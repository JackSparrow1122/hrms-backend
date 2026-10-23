package com.gahrms.backend.modules.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectedBiometricCodesDto {
    @Builder.Default
    private List<String> allCodes = new ArrayList<>();

    @Builder.Default
    private List<String> unmappedCodes = new ArrayList<>();
}
