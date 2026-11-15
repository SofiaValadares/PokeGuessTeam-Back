package com.svc.pokeguessteam.repository.admin;

import com.svc.pokeguessteam.model.admin.AdminRoleAuditModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRoleAuditRepository
        extends JpaRepository<AdminRoleAuditModel, String> {
}