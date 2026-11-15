package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.model.admin.AdminRoleAuditModel;
import com.svc.pokeguessteam.model.enums.UserRole;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.admin.AdminRoleAuditRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {

    private final AdminRoleAuditRepository auditRepository;

    public AdminAuditService(AdminRoleAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void recordRoleChange(
            UserModel actor,
            UserModel target,
            UserRole fromRole,
            UserRole toRole,
            String ipAddress
    ) {
        AdminRoleAuditModel audit = new AdminRoleAuditModel(
                actor.getIdUser(),
                actor.getUsername(),
                target.getIdUser(),
                target.getUsername(),
                fromRole,
                toRole,
                ipAddress
        );

        auditRepository.save(audit);
    }
}