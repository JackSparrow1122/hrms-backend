package com.gahrms.backend.modules.identity.dto;

import com.gahrms.backend.modules.identity.entity.ScopeType;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRolesRequest {

    @NotEmpty(message = "At least one role must be provided")
    private Set<String> roles;

    private Long companyId;

    private ScopeType scopeType;

    private Long scopeBranchId;

    private Long scopeDepartmentId;
}
