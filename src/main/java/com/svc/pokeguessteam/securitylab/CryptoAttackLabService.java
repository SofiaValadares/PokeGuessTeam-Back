package com.svc.pokeguessteam.securitylab;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CryptoAttackLabService {

    private final SymmetricLabCipher cipher;

    public CryptoAttackLabService(SymmetricLabCipher cipher) {
        this.cipher = cipher;
    }

    public record BruteForceResult(
            long keySpaceBits,
            long attempted,
            boolean found,
            long elapsedMs,
            String viability,
            String recoveredSeedHint
    ) {
    }

    public record DifferentialResult(
            String originalCipherBase64,
            String flippedCipherBase64,
            int changedBits,
            int totalBitsCompared,
            double avalanchePercent,
            boolean strongAvalanche
    ) {
    }

    public record StatisticalResult(
            int sampleBytes,
            Map<String, Integer> topFrequencies,
            double chiSquare,
            double uniformExpected,
            boolean looksUniform,
            String analysis
    ) {
    }

    public BruteForceResult bruteForce(String plaintext, String ignoredSeed, int searchableBits) {
        int bits = Math.min(16, Math.max(4, searchableBits));
        long space = 1L << bits;
        // Lab pedagógico: esconde a seed num espaço reduzido "k{n}" para o ataque poder terminar.
        long secretIndex = Math.floorMod(System.nanoTime(), space);
        String weakSeed = "k" + secretIndex;
        var encrypted = cipher.encrypt(plaintext, weakSeed);
        byte[] target = java.util.Base64.getDecoder().decode(encrypted.ciphertextBase64());
        String nonceB64 = encrypted.keyMaterial().nonceBase64();
        byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);

        long started = System.nanoTime();
        boolean found = false;
        String hint = null;
        long attempted = 0;
        for (long i = 0; i < space; i++) {
            attempted++;
            String guess = "k" + i;
            var km = cipher.restoreKey(guess, nonceB64);
            byte[] out = cipher.encryptBytes(plainBytes, km.key());
            if (Arrays.equals(out, target)) {
                found = true;
                hint = guess;
                break;
            }
        }
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        String viability = "Com espaço artificial de " + bits + " bits o ataque é viável ("
                + attempted + " tentativas). Uma chave dinâmica SHA-256 (256 bits ≈ 1.16e77) é inviável.";
        return new BruteForceResult(256, attempted, found, elapsedMs, viability, hint);
    }

    public DifferentialResult differential(String plaintext, String seed) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext vazio");
        }
        byte[] p = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] p2 = Arrays.copyOf(p, p.length);
        p2[0] = (byte) (p2[0] ^ 0x01); // flip 1 bit

        var km = cipher.deriveKey(seed);
        byte[] c1 = cipher.encryptBytes(p, km.key());
        byte[] c2 = cipher.encryptBytes(p2, km.key());
        int changed = SymmetricLabCipher.hammingBits(c1, c2);
        int total = Math.max(c1.length, c2.length) * 8;
        double pct = total == 0 ? 0 : (100.0 * changed / total);
        return new DifferentialResult(
                java.util.Base64.getEncoder().encodeToString(c1),
                java.util.Base64.getEncoder().encodeToString(c2),
                changed,
                total,
                Math.round(pct * 100.0) / 100.0,
                pct >= 40.0
        );
    }

    public StatisticalResult statistical(String plaintext, String seed) {
        var enc = cipher.encrypt(plaintext == null ? "" : plaintext, seed);
        byte[] data = java.util.Base64.getDecoder().decode(enc.ciphertextBase64());
        int[] freq = new int[256];
        for (byte b : data) {
            freq[b & 0xFF]++;
        }
        double expected = data.length / 256.0;
        double chi = 0;
        List<int[]> ranked = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                double diff = freq[i] - expected;
                chi += (diff * diff) / Math.max(expected, 1e-9);
                ranked.add(new int[]{i, freq[i]});
            }
        }
        ranked.sort((a, b) -> Integer.compare(b[1], a[1]));
        Map<String, Integer> top = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(12, ranked.size()); i++) {
            top.put(String.format("%02X", ranked.get(i)[0]), ranked.get(i)[1]);
        }
        // Chi-square crítico ~294 para df=255 @0.05 — heurística simples
        boolean uniform = chi < 320 && data.length >= 64;
        String analysis = uniform
                ? "Distribuição próxima de uniforme — padrões fracos na ciphertext."
                : "Há desvios de frequência — ataque estatístico pode explorar vieses.";
        return new StatisticalResult(
                data.length,
                top,
                Math.round(chi * 100.0) / 100.0,
                Math.round(expected * 1000.0) / 1000.0,
                uniform,
                analysis
        );
    }
}
