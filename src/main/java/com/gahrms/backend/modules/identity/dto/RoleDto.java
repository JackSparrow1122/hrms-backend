package com.gahrms.backend.modules.identity.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDto {
    private Long id;
    private String name;
    private String description;
    private String tier; // PLATFORM or ORGANIZATION
}
