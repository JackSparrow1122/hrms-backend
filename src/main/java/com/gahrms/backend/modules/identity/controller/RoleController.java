package com.gahrms.backend.modules.identity.controller;

import com.gahrms.backend.common.dto.ApiResponse;
import com.gahrms.backend.modules.identity.dto.RoleDto;
import com.gahrms.backend.modules.identity.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles & Permissions Directory", description = "Endpoints for discovering platform and organization role definitions")
@SecurityRequirement(name = "BearerAuth")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all system roles", description = "Retrieves all platform-level and organization-level roles with their descriptions and tiers")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getAllRoles()));
    }
}
