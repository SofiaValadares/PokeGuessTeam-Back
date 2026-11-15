package com.svc.pokeguessteam.model.enums;

public enum AuditLogCategory {
    /** Pedidos HTTP comuns atrelados a uma conta. */
    USER_REQUEST,
    /** Ações administrativas explícitas. */
    ADMIN_ACTION,
    /** Eventos sensíveis de segurança. */
    SECURITY_CRITICAL
}
