package com.svc.pokeguessteam.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    @Test
    void acceptsTenCharsWithUppercaseAndSpecial() {
        assertTrue(PasswordPolicy.matches("Abcdefgh!1"));
    }

    @Test
    void rejectsShortOrMissingUppercaseOrSpecial() {
        assertFalse(PasswordPolicy.matches("abcdefghi!"));
        assertFalse(PasswordPolicy.matches("Abcdefghij"));
        assertFalse(PasswordPolicy.matches("Short1!"));
        assertFalse(PasswordPolicy.matches(null));
    }
}
