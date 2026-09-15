package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.config.AppMasterAdminProperties;
import com.svc.pokeguessteam.model.enums.UserRole;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleService {

    private static final Logger log =
            LoggerFactory.getLogger(UserRoleService.class);

    private final AppMasterAdminProperties masterAdminProperties;
    private final UserRepository userRepository;

    public UserRoleService(
            AppMasterAdminProperties masterAdminProperties,
            UserRepository userRepository
    ) {
        this.masterAdminProperties = masterAdminProperties;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserModel bootstrapFirstMasterIfNeeded(UserModel user) {
        if (user == null) {
            return null;
        }

        // A partir do momento em que existe um master,
        // o ambiente deixa de ter poder para promover usuários.
        if (userRepository.existsByRole(UserRole.MASTER_ADMIN)) {
            return user;
        }

        // Só o username configurado como bootstrap pode ser o primeiro.
        if (!masterAdminProperties.isBootstrapUsername(user.getUsername())) {
            return user;
        }

        UserRole previousRole = user.getRole();

        user.setRole(UserRole.MASTER_ADMIN);
        UserModel saved = userRepository.save(user);

        log.warn(
                "BOOTSTRAP MASTER_ADMIN: userId={}, username={}, previousRole={}",
                saved.getIdUser(),
                saved.getUsername(),
                previousRole
        );

        return saved;
    }
}