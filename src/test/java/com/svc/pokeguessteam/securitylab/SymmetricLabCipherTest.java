package com.svc.pokeguessteam.securitylab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymmetricLabCipherTest {

    private final SymmetricLabCipher cipher = new SymmetricLabCipher();

    @Test
    void encryptDecryptRoundTrip() {
        var enc = cipher.encrypt("hello lab", "seed-a");
        var km = cipher.restoreKey("seed-a", enc.keyMaterial().nonceBase64());
        assertEquals("hello lab", cipher.decrypt(enc.ciphertextBase64(), km));
    }

    @Test
    void dynamicNonceChangesCiphertext() {
        var a = cipher.encrypt("same", "seed");
        var b = cipher.encrypt("same", "seed");
        assertNotEquals(a.ciphertextBase64(), b.ciphertextBase64());
        assertNotEquals(a.keyMaterial().nonceBase64(), b.keyMaterial().nonceBase64());
    }

    @Test
    void differentialChangesManyBits() {
        var attack = new CryptoAttackLabService(cipher);
        var diff = attack.differential("Pikachu", "seed");
        assertTrue(diff.changedBits() > 0);
        assertTrue(diff.avalanchePercent() > 0);
    }
}
