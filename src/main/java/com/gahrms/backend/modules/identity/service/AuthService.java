package com.gahrms.backend.modules.identity.service;

import com.gahrms.backend.common.exception.ResourceNotFoundException;
import com.gahrms.backend.common.exception.UnauthorizedException;
import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.repository.EmployeeRepository;
import com.gahrms.backend.modules.identity.dto.*;
import com.gahrms.backend.modules.identity.entity.RefreshToken;
import com.gahrms.backend.modules.identity.entity.Role;
import com.gahrms.backend.modules.identity.entity.User;
import com.gahrms.backend.modules.identity.repository.RefreshTokenRepository;
import com.gahrms.backend.modules.identity.repository.UserRepository;
import com.gahrms.backend.modules.organization.entity.Company;
import com.gahrms.backend.modules.organization.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmail() != null ? request.getEmail().trim() : "";
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
        );

        User user = userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> employeeRepository.findByEmployeeCodeIgnoreCase(identifier)
                        .flatMap(emp -> userRepository.findByEmployeeId(emp.getId())))
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account has been deactivated. Please contact HR/IT.");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshTokenStr = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiresAt(Instant.now().plusMillis(604800000L))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .user(mapToSummary(user))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        User user = token.getUser();
        String newAccessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token.getToken())
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .user(mapToSummary(user))
                .build();
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    public UserSummaryDto getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToSummary(user);
    }

    private UserSummaryDto mapToSummary(User user) {
        String employeeCode = null;
        String designationName = null;
        String departmentName = null;
        String companyName = null;

        if (user.getEmployeeId() != null) {
            Employee emp = employeeRepository.findById(user.getEmployeeId()).orElse(null);
            if (emp != null) {
                employeeCode = emp.getEmployeeCode();
                if (emp.getDesignation() != null) designationName = emp.getDesignation().getName();
                if (emp.getDepartment() != null) departmentName = emp.getDepartment().getName();
                if (emp.getCompany() != null) companyName = emp.getCompany().getName();
            }
        } else if (user.getCompanyId() != null) {
            companyName = companyRepository.findById(user.getCompanyId())
                    .map(Company::getName)
                    .orElse(null);
        }

        return UserSummaryDto.builder()
                .id(user.getId())
                .employeeId(user.getEmployeeId())
                .companyId(user.getCompanyId())
                .scopeType(user.getScopeType() != null ? user.getScopeType().name() : "ORGANIZATION")
                .scopeBranchId(user.getScopeBranchId())
                .scopeDepartmentId(user.getScopeDepartmentId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .employeeCode(employeeCode)
                .designationName(designationName)
                .departmentName(departmentName)
                .companyName(companyName)
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }
}
