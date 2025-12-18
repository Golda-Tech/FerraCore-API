package com.goldatech.authservice.domain.model;

/**
 * Enum for user roles.
 */
public enum UserRoles {
    SUPER_ADMIN("Super Admin"),
    GA_ADMIN("Ga Admin"),
    BUSINESS_ADMIN("Business Admin"),
    BUSINESS_FINANCE("Business Finance"),
    BUSINESS_OPERATOR("Business Operator");


    private final String displayName;

    UserRoles(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}