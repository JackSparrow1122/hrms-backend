package com.gahrms.backend.config;

import com.gahrms.backend.modules.employee.repository.EmployeeRepository;
import com.gahrms.backend.modules.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationSecurityConfig {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return identifier -> userRepository.findByEmailIgnoreCase(identifier.trim())
                .or(() -> employeeRepository.findByEmployeeCodeIgnoreCase(identifier.trim())
                        .flatMap(emp -> userRepository.findByEmployeeId(emp.getId())))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email or employee code: " + identifier));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
