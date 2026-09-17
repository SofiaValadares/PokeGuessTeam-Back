package com.svc.pokeguessteam.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = UsernameDistinctFromEmailValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsernameDistinctFromEmail {

    String message() default "{error.validation.register.username.from-email}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
