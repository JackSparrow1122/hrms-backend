package com.gahrms.backend.modules.identity.entity;

import com.gahrms.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity implements UserDetails {

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "company_id")
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private ScopeType scopeType = ScopeType.ORGANIZATION;

    @Column(name = "scope_branch_id")
    private Long scopeBranchId;

    @Column(name = "scope_department_id")
    private Long scopeDepartmentId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public boolean isSuperAdmin() {
        return roles.stream().anyMatch(r -> "ROLE_SUPER_ADMIN".equalsIgnoreCase(r.getName()));
    }

    public boolean isTenantAdmin() {
        return roles.stream().anyMatch(r -> "ROLE_TENANT_ADMIN".equalsIgnoreCase(r.getName()));
    }

    public boolean isHrAdmin() {
        return roles.stream().anyMatch(r -> "ROLE_HR_ADMIN".equalsIgnoreCase(r.getName()));
    }

    public boolean isManager() {
        return roles.stream().anyMatch(r -> "ROLE_MANAGER".equalsIgnoreCase(r.getName()));
    }

    public boolean isTeamLead() {
        return roles.stream().anyMatch(r -> "ROLE_TEAM_LEAD".equalsIgnoreCase(r.getName()));
    }

    public boolean isEmployee() {
        return roles.stream().anyMatch(r -> "ROLE_EMPLOYEE".equalsIgnoreCase(r.getName()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
