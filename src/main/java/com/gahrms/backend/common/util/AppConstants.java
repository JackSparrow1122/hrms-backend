package com.gahrms.backend.common.util;

public final class AppConstants {

    private AppConstants() {}

    public static final String API_V1_PREFIX = "/api/v1";

    // Core Multi-Tenant Enterprise Roles
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    public static final String ROLE_HR_ADMIN = "ROLE_HR_ADMIN";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";
    public static final String ROLE_TEAM_LEAD = "ROLE_TEAM_LEAD";
    public static final String ROLE_PAYROLL_ADMIN = "ROLE_PAYROLL_ADMIN";
    public static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";
    public static final String ROLE_AUDITOR_READONLY = "ROLE_AUDITOR_READONLY";
    public static final String ROLE_MANAGEMENT_VIEWER = "ROLE_MANAGEMENT_VIEWER";

    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "desc";
}
