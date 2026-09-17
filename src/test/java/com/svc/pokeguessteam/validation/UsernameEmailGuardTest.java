package com.svc.pokeguessteam.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsernameEmailGuardTest {

    @Test
    void rejectsUsernameEqualToEmailOrContainingLocalPart() {
        assertTrue(UsernameEmailGuard.conflicts("ash.ketchum@pallete.com", "ash.ketchum@pallete.com"));
        assertTrue(UsernameEmailGuard.conflicts("ash.ketchum", "ash.ketchum@pallete.com"));
        assertTrue(UsernameEmailGuard.conflicts("ASHKETCHUM99", "ash.ketchum@pallete.com"));
    }

    @Test
    void allowsUnrelatedUsername() {
        assertFalse(UsernameEmailGuard.conflicts("trainerRed", "ash.ketchum@pallete.com"));
        assertFalse(UsernameEmailGuard.conflicts("ab", "a@x.com"));
    }
}
