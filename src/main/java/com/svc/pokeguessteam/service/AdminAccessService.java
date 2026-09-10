package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccessService {

    private final UserRepository userRepository;

    public AdminAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserModel requireAdmin(String userId) {
        UserModel user = requireUser(userId);
        if (!user.getRole().isAdminOrAbove()) {
            throw forbidden();
        }
        return user;
    }

    @Transactional(readOnly = true)
    public UserModel requireMaster(String userId) {
        UserModel user = requireUser(userId);
        if (!user.getRole().isMaster()) {
            throw forbidden();
        }
        return user;
    }

    private UserModel requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.ADMIN_USER_NOT_FOUND,
                        MessageKeys.ADMIN_USER_NOT_FOUND
                ));
    }

    private static ApiBusinessException forbidden() {
        return new ApiBusinessException(
                HttpStatus.FORBIDDEN,
                ErrorCodes.ADMIN_FORBIDDEN,
                MessageKeys.ADMIN_FORBIDDEN
        );
    }
}
