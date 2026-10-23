package com.gahrms.backend.modules.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceDto {
    private Long id;
    private Long leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;
    private Integer year;
    private BigDecimal totalAllowance;
    private BigDecimal usedCount;
    private BigDecimal pendingCount;
    private BigDecimal remainingCount;
    private boolean isPaid;
}
