package com.gahrms.backend.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.ofNullable(authentication.getName());
    }

    public static boolean isCurrentUserSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN"));
    }

    public static boolean isCurrentUserTenantAdmin() {
        return hasAuthority("ROLE_TENANT_ADMIN");
    }

    public static boolean isCurrentUserHrAdmin() {
        return hasAuthority("ROLE_HR_ADMIN");
    }

    public static boolean hasAuthority(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(role));
    }
}
