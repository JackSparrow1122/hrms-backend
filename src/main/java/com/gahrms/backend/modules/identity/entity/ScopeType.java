package com.gahrms.backend.modules.identity.entity;

public enum ScopeType {
    PLATFORM,       // Super Admin: system-wide cross-tenant access
    ORGANIZATION,   // Tenant Admin / HR Admin: access to entire organization/company
    BRANCH,         // Access restricted to a specific branch location
    DEPARTMENT,     // Access restricted to a specific department (Manager, Team Lead)
    SELF            // Employee self-service scope
}
