package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.config.AppMasterAdminProperties;
import com.svc.pokeguessteam.model.enums.UserRole;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sincroniza {@link UserRole#MASTER_ADMIN} a partir de {@code app.master-admin.usernames}.
 * Promove quem está na lista; se o user já era MASTER mas saiu do env, deixa como está (mais seguro).
 */
@Service
public class UserRoleService {

    private final AppMasterAdminProperties masterAdminProperties;
    private final UserRepository userRepository;

    public UserRoleService(AppMasterAdminProperties masterAdminProperties, UserRepository userRepository) {
        this.masterAdminProperties = masterAdminProperties;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserModel syncMasterFromEnv(UserModel user) {
        if (user == null) {
            return null;
        }
        if (masterAdminProperties.isMasterUsername(user.getUsername())
                && user.getRole() != UserRole.MASTER_ADMIN) {
            user.setRole(UserRole.MASTER_ADMIN);
            return userRepository.save(user);
        }
        return user;
    }
}
