package com.svc.pokeguessteam.model.enums;

public enum UserRole {
    USER,
    ADMIN,
    MASTER_ADMIN;

    public boolean isAdminOrAbove() {
        return this == ADMIN || this == MASTER_ADMIN;
    }

    public boolean isMaster() {
        return this == MASTER_ADMIN;
    }
}
