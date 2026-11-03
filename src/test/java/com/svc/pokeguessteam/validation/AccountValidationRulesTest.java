package com.svc.pokeguessteam.validation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountValidationRulesTest {

    @Nested
    class Passwords {

        @Test
        void acceptsTenCharsWithUppercaseAndSpecial() {
            assertTrue(PasswordPolicy.matches("Abcdefgh!1"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"abcdefghi!", "Abcdefghij", "Short1!"})
        void rejectsShortMissingUppercaseOrSpecial(String password) {
            assertFalse(PasswordPolicy.matches(password));
        }
    }

    @Nested
    class Usernames {

        @Test
        void rejectsUsernameEqualToTheEmailOrContainingTheLocalPart() {
            assertTrue(UsernameEmailGuard.conflicts("ash.ketchum@pallete.com", "ash.ketchum@pallete.com"));
            assertTrue(UsernameEmailGuard.conflicts("ash.ketchum", "ash.ketchum@pallete.com"));
            assertTrue(UsernameEmailGuard.conflicts("ASHKETCHUM99", "ash.ketchum@pallete.com"));
            assertFalse(UsernameEmailGuard.conflicts("trainerRed", "ash.ketchum@pallete.com"));
            assertFalse(UsernameEmailGuard.conflicts("ab", "a@x.com"));
        }
    }
}
