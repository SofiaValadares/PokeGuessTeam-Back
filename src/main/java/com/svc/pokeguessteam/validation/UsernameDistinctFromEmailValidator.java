package com.svc.pokeguessteam.validation;

import com.svc.pokeguessteam.dto.auth.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UsernameDistinctFromEmailValidator
        implements ConstraintValidator<UsernameDistinctFromEmail, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !UsernameEmailGuard.conflicts(value.username(), value.email());
    }
}
