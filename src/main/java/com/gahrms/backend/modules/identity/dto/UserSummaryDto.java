package com.gahrms.backend.modules.identity.dto;

import java.util.Set;

public class UserSummaryDto {
    private Long id;
    private Long employeeId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Set<String> roles;

    public UserSummaryDto() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final UserSummaryDto dto = new UserSummaryDto();
        public Builder id(Long id) { dto.id = id; return this; }
        public Builder employeeId(Long employeeId) { dto.employeeId = employeeId; return this; }
        public Builder email(String email) { dto.email = email; return this; }
        public Builder firstName(String firstName) { dto.firstName = firstName; return this; }
        public Builder lastName(String lastName) { dto.lastName = lastName; return this; }
        public Builder fullName(String fullName) { dto.fullName = fullName; return this; }
        public Builder roles(Set<String> roles) { dto.roles = roles; return this; }
        public UserSummaryDto build() { return dto; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}
