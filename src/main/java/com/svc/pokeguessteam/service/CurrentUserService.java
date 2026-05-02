package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public String requireUserId(HttpSession session) {
        Object value = session.getAttribute("USER_ID");
        if (!(value instanceof String userId) || userId.isBlank()) {
            throw new ApiBusinessException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodes.SESSION_USER_ID_MISSING,
                    MessageKeys.SESSION_USER_ID_MISSING
            );
        }
        return userId;
    }
}
