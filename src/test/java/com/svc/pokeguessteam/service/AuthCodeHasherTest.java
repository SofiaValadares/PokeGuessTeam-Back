package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.config.AppAuthProperties;
import com.svc.pokeguessteam.model.auth.AuthCodePurpose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCodeHasherTest {

    @Test
    void matches_sameCodeAndRejectsWrongCode() {
        AppAuthProperties props = new AppAuthProperties();
        props.setCodeSecret("test-secret");
        AuthCodeHasher hasher = new AuthCodeHasher(props);

        String hash = hasher.hash("user-1", AuthCodePurpose.EMAIL_VERIFICATION, "123456");
        assertTrue(hasher.matches("user-1", AuthCodePurpose.EMAIL_VERIFICATION, "123456", hash));
        assertFalse(hasher.matches("user-1", AuthCodePurpose.EMAIL_VERIFICATION, "654321", hash));
        assertFalse(hasher.matches("user-2", AuthCodePurpose.EMAIL_VERIFICATION, "123456", hash));
    }
}
