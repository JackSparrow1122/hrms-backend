package com.gahrms.backend.modules.identity.dto;

import com.gahrms.backend.modules.identity.entity.ScopeType;
import lombok.*;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private boolean active;
    private boolean emailVerified;
    private Long companyId;
    private String companyName;
    private ScopeType scopeType;
    private Long scopeBranchId;
    private String scopeBranchName;
    private Long scopeDepartmentId;
    private String scopeDepartmentName;
    private Set<String> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
