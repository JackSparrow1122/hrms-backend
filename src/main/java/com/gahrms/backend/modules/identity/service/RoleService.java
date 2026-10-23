package com.gahrms.backend.modules.identity.service;

import com.gahrms.backend.modules.identity.dto.RoleDto;
import com.gahrms.backend.modules.identity.entity.Role;
import com.gahrms.backend.modules.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private RoleDto mapToDto(Role role) {
        String tier = "ROLE_SUPER_ADMIN".equalsIgnoreCase(role.getName()) ? "PLATFORM" : "ORGANIZATION";
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .tier(tier)
                .build();
    }
}
