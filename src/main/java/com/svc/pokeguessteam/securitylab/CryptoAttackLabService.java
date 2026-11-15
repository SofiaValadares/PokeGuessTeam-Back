package com.svc.pokeguessteam.securitylab;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CryptoAttackLabService {

    private static final int MAX_SEARCHABLE_BITS = 512;
    private static final int FULL_SEARCH_MAX_BITS = 16;
    private static final int MAX_DIFFERENTIAL_BIT_COUNT = 512;
    private static final long MAX_SEARCH_DURATION_MS = 60_000;
    private static final double POWERFUL_MACHINE_GUESSES_PER_SECOND = 1_000_000_000_000d;
    private static final double CHI_SQUARE_SIGNIFICANCE_LEVEL = 0.05;
    private static final double GAMMA_EPSILON = 1e-14;
    private static final double GAMMA_FPMIN = 1e-300;
    private static final int GAMMA_MAX_ITERATIONS = 10_000;
    private static final double[] LANCZOS_COEFFICIENTS = {
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7
    };
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SymmetricLabCipher cipher;

    public CryptoAttackLabService(SymmetricLabCipher cipher) {
        this.cipher = cipher;
    }

    public record BruteForceResult(
            long keySpaceBits,
            long attempted,
            boolean found,
            long elapsedMs,
            boolean timeLimited,
            double attemptsPerSecond,
            double expectedSecondsAtObservedRate,
            double worstCaseSecondsAtObservedRate,
            double expectedSecondsAtPowerfulRate,
            double worstCaseSecondsAtPowerfulRate,
            String powerfulMachineAssessment,
            String viability,
            String recoveredSeedHint
    ) {
    }

    public record DifferentialResult(
            String originalCipherBase64,
            String flippedCipherBase64,
            List<Integer> flippedBitPositions,
            int flippedBitCount,
            int changedBits,
            int totalBitsCompared,
            double avalanchePercent,
            boolean strongAvalanche
    ) {
    }

    private record DifferentialDistance(int changedBits, int totalBits) {
    }

    public record StatisticalResult(
            int sampleCount,
            int sampleBytes,
            Map<String, Integer> topFrequencies,
            double chiSquare,
            double uniformExpected,
            double pValue,
            boolean sufficientSample,
            boolean looksUniform,
            String analysis
    ) {
    }

    public BruteForceResult bruteForce(String plaintext, String ignoredSeed, int searchableBits) {
        return bruteForce(plaintext, ignoredSeed, searchableBits, null);
    }

    public BruteForceResult bruteForce(
            String plaintext,
            String ignoredSeed,
            int searchableBits,
            Long requestedDurationMs
    ) {
        int bits = Math.min(MAX_SEARCHABLE_BITS, Math.max(4, searchableBits));
        BigInteger keySpace = BigInteger.ONE.shiftLeft(bits);
        BigInteger secretIndex = new BigInteger(bits, SECURE_RANDOM);
        boolean boundedSearch = bits > FULL_SEARCH_MAX_BITS;
        long searchLimit = bits <= 62 ? keySpace.longValueExact() : Long.MAX_VALUE;
        long searchDurationMs = requestedDurationMs == null || requestedDurationMs <= 0
                ? MAX_SEARCH_DURATION_MS
                : Math.min(requestedDurationMs, MAX_SEARCH_DURATION_MS);
        String weakSeed = "k" + secretIndex;
        var encrypted = cipher.encrypt(plaintext, weakSeed);
        byte[] target = java.util.Base64.getDecoder().decode(encrypted.ciphertextBase64());
        String nonceB64 = encrypted.keyMaterial().nonceBase64();
        byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);

        long started = System.nanoTime();
        long deadline = started + searchDurationMs * 1_000_000L;
        boolean found = false;
        String hint = null;
        long attempted = 0;
        boolean timeLimited = false;
        for (long i = 0; i < searchLimit; i++) {
            attempted++;
            String guess = "k" + i;
            var km = cipher.restoreKey(guess, nonceB64);
            byte[] out = cipher.encryptBytes(plainBytes, km.key());
            if (Arrays.equals(out, target)) {
                found = true;
                hint = guess;
                break;
            }
            if (boundedSearch && System.nanoTime() >= deadline) {
                timeLimited = true;
                break;
            }
        }
        long elapsedNanos = Math.max(1, System.nanoTime() - started);
        long elapsedMs = elapsedNanos / 1_000_000L;
        boolean exhausted = BigInteger.valueOf(attempted).compareTo(keySpace) >= 0;
        double elapsedSeconds = elapsedNanos / 1_000_000_000d;
        double attemptsPerSecond = attempted / elapsedSeconds;
        double keySpaceSize = Math.pow(2, bits);
        double expectedSecondsAtObservedRate = keySpaceSize / 2d / attemptsPerSecond;
        double worstCaseSecondsAtObservedRate = keySpaceSize / attemptsPerSecond;
        double expectedSecondsAtPowerfulRate = keySpaceSize / 2d
                / POWERFUL_MACHINE_GUESSES_PER_SECOND;
        double worstCaseSecondsAtPowerfulRate = keySpaceSize
                / POWERFUL_MACHINE_GUESSES_PER_SECOND;
        String powerfulMachineAssessment = assessPowerfulMachineViability(expectedSecondsAtPowerfulRate);
        String viability;
        if (found) {
            viability = "Seed artificial de " + bits + " bits encontrada em " + attempted
                    + " tentativas no espaço de 2^" + bits + " candidatos.";
        } else if (exhausted) {
            viability = "Busca exaustiva: o espaço de " + bits + " bits foi percorrido sem encontrar a seed.";
        } else if (timeLimited) {
            viability = "Busca interrompida após " + elapsedMs + " ms e " + attempted
                    + " tentativas. A previsão usa a taxa observada nesta execução.";
        } else {
            viability = "Busca interrompida sem percorrer todo o espaço de " + bits + " bits.";
        }
        return new BruteForceResult(
                bits,
                attempted,
                found,
                elapsedMs,
                timeLimited,
                attemptsPerSecond,
                expectedSecondsAtObservedRate,
                worstCaseSecondsAtObservedRate,
                expectedSecondsAtPowerfulRate,
                worstCaseSecondsAtPowerfulRate,
                powerfulMachineAssessment,
                viability,
                hint
        );
    }

    private static String assessPowerfulMachineViability(double expectedSeconds) {
        if (expectedSeconds < 60) {
            return "Viável em segundos no cenário hipotético de 1 trilhão de tentativas por segundo.";
        }
        if (expectedSeconds < 3_600) {
            return "Viável em menos de uma hora apenas no cenário hipotético de 1 trilhão de tentativas por segundo.";
        }
        if (expectedSeconds < 86_400) {
            return "Pode ser viável em horas com hardware e paralelismo extremos; a taxa de 1 trilhão/s é apenas uma hipótese.";
        }
        if (expectedSeconds < 31_557_600) {
            return "Pode ser quebrado em dias ou meses com recursos extremos; 1 trilhão/s é uma hipótese, não uma medição.";
        }
        return "Não viável em prazo prático no cenário hipotético de 1 trilhão de tentativas por segundo.";
    }

    public DifferentialResult differential(String plaintext, String seed) {
        return differential(plaintext, seed, 1);
    }

    public DifferentialResult differential(String plaintext, String seed, int flippedBitPosition) {
        return differential(plaintext, seed, flippedBitPosition, 1);
    }

    public DifferentialResult differential(
            String plaintext,
            String seed,
            int flippedBitPosition,
            int flippedBitCount
    ) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext vazio");
        }
        byte[] p = plaintext.getBytes(StandardCharsets.UTF_8);
        int totalPlaintextBits = p.length * 8;
        int maximumFlippedBitCount = Math.min(MAX_DIFFERENTIAL_BIT_COUNT, totalPlaintextBits);
        if (flippedBitCount < 1 || flippedBitCount > maximumFlippedBitCount) {
            throw new IllegalArgumentException(
                    "A quantidade de bits deve estar entre 1 e " + maximumFlippedBitCount
            );
        }
        List<Integer> flippedBitPositions;
        if (flippedBitCount == 1) {
            if (flippedBitPosition < 1 || flippedBitPosition > totalPlaintextBits) {
                throw new IllegalArgumentException(
                        "A posição do bit deve estar entre 1 e " + totalPlaintextBits
                );
            }
            flippedBitPositions = List.of(flippedBitPosition);
        } else {
            flippedBitPositions = chooseRandomBitPositions(totalPlaintextBits, flippedBitCount);
        }

        byte[] p2 = Arrays.copyOf(p, p.length);
        for (int bitPosition : flippedBitPositions) {
            int zeroBasedBitPosition = bitPosition - 1;
            int byteIndex = zeroBasedBitPosition / 8;
            int bitMask = 1 << (7 - zeroBasedBitPosition % 8);
            p2[byteIndex] = (byte) (p2[byteIndex] ^ bitMask);
        }

        var km = cipher.deriveKey(seed);
        byte[] c1 = cipher.encryptBytes(p, km.key());
        byte[] c2 = cipher.encryptBytes(p2, km.key());
        DifferentialDistance distance = compareAffectedBlocks(c1, c2, flippedBitPositions);
        int changed = distance.changedBits();
        int total = distance.totalBits();
        double pct = total == 0 ? 0 : (100.0 * changed / total);
        return new DifferentialResult(
                java.util.Base64.getEncoder().encodeToString(c1),
                java.util.Base64.getEncoder().encodeToString(c2),
                flippedBitPositions,
                flippedBitCount,
                changed,
                total,
                Math.round(pct * 100.0) / 100.0,
                pct >= 40.0
        );
    }

    public List<DifferentialResult> differentialSamples(String plaintext, String seed, int sampleCount) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext vazio");
        }
        byte[] originalPlaintext = plaintext.getBytes(StandardCharsets.UTF_8);
        int totalPlaintextBits = originalPlaintext.length * 8;
        int maximumSampleCount = Math.min(MAX_DIFFERENTIAL_BIT_COUNT, totalPlaintextBits);
        if (sampleCount < 1 || sampleCount > maximumSampleCount) {
            throw new IllegalArgumentException(
                    "A quantidade de amostras deve estar entre 1 e " + maximumSampleCount
            );
        }

        List<Integer> bitPositions = chooseRandomBitPositions(totalPlaintextBits, sampleCount);
        var keyMaterial = cipher.deriveKey(seed);
        byte[] originalCipher = cipher.encryptBytes(originalPlaintext, keyMaterial.key());
        String originalCipherBase64 = java.util.Base64.getEncoder().encodeToString(originalCipher);
        List<DifferentialResult> results = new ArrayList<>(sampleCount);

        for (int bitPosition : bitPositions) {
            byte[] modifiedPlaintext = Arrays.copyOf(originalPlaintext, originalPlaintext.length);
            int zeroBasedBitPosition = bitPosition - 1;
            int byteIndex = zeroBasedBitPosition / 8;
            int bitMask = 1 << (7 - zeroBasedBitPosition % 8);
            modifiedPlaintext[byteIndex] = (byte) (modifiedPlaintext[byteIndex] ^ bitMask);

            byte[] modifiedCipher = cipher.encryptBytes(modifiedPlaintext, keyMaterial.key());
            DifferentialDistance distance = compareAffectedBlocks(
                    originalCipher,
                    modifiedCipher,
                    List.of(bitPosition)
            );
            int changedBits = distance.changedBits();
            int totalComparedBits = distance.totalBits();
            double avalanchePercent = totalComparedBits == 0
                    ? 0
                    : (100.0 * changedBits / totalComparedBits);
            results.add(new DifferentialResult(
                    originalCipherBase64,
                    java.util.Base64.getEncoder().encodeToString(modifiedCipher),
                    List.of(bitPosition),
                    1,
                    changedBits,
                    totalComparedBits,
                    Math.round(avalanchePercent * 100.0) / 100.0,
                    avalanchePercent >= 40.0
            ));
        }
        return List.copyOf(results);
    }

    /**
     * Avalanche is measured over the 16-byte block(s) containing changed input bits.
     * Other blocks are independent in this didactic mode and would dilute the percentage.
     */
    private static DifferentialDistance compareAffectedBlocks(
            byte[] originalCipher,
            byte[] modifiedCipher,
            List<Integer> flippedBitPositions
    ) {
        int blockCount = Math.max(originalCipher.length, modifiedCipher.length) / SymmetricLabCipher.BLOCK_SIZE;
        boolean[] affectedBlocks = new boolean[blockCount];
        for (int bitPosition : flippedBitPositions) {
            int bytePosition = (bitPosition - 1) / 8;
            int blockIndex = bytePosition / SymmetricLabCipher.BLOCK_SIZE;
            if (blockIndex >= 0 && blockIndex < affectedBlocks.length) {
                affectedBlocks[blockIndex] = true;
            }
        }

        int changedBits = 0;
        int totalBits = 0;
        for (int blockIndex = 0; blockIndex < affectedBlocks.length; blockIndex++) {
            if (!affectedBlocks[blockIndex]) continue;
            int start = blockIndex * SymmetricLabCipher.BLOCK_SIZE;
            int end = Math.min(start + SymmetricLabCipher.BLOCK_SIZE,
                    Math.min(originalCipher.length, modifiedCipher.length));
            if (end <= start) continue;
            changedBits += SymmetricLabCipher.hammingBits(
                    Arrays.copyOfRange(originalCipher, start, end),
                    Arrays.copyOfRange(modifiedCipher, start, end)
            );
            totalBits += (end - start) * 8;
        }
        return new DifferentialDistance(changedBits, totalBits);
    }

    private static List<Integer> chooseRandomBitPositions(int totalBits, int count) {
        int[] positions = new int[totalBits];
        for (int index = 0; index < totalBits; index++) {
            positions[index] = index + 1;
        }
        for (int index = 0; index < count; index++) {
            int selectedIndex = index + SECURE_RANDOM.nextInt(totalBits - index);
            int selectedPosition = positions[index];
            positions[index] = positions[selectedIndex];
            positions[selectedIndex] = selectedPosition;
        }
        List<Integer> selected = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            selected.add(positions[index]);
        }
        selected.sort(Integer::compareTo);
        return List.copyOf(selected);
    }

    public StatisticalResult statistical(String plaintext, String seed) {
        return statistical(plaintext, seed, 100);
    }

    public StatisticalResult statistical(String plaintext, String seed, int requestedSampleCount) {
        int sampleCount = Math.min(100, Math.max(1, requestedSampleCount));
        String samplePlaintext = plaintext == null ? "" : plaintext;
        ByteArrayOutputStream combinedCiphertexts = new ByteArrayOutputStream();
        for (int sample = 0; sample < sampleCount; sample++) {
            var encryptedSample = cipher.encrypt(samplePlaintext, seed);
            combinedCiphertexts.writeBytes(
                    java.util.Base64.getDecoder().decode(encryptedSample.ciphertextBase64())
            );
        }
        byte[] data = combinedCiphertexts.toByteArray();
        int[] freq = new int[256];
        for (byte b : data) {
            freq[b & 0xFF]++;
        }
        double expected = data.length / 256.0;
        double chi = 0;
        List<int[]> ranked = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            double diff = freq[i] - expected;
            chi += (diff * diff) / Math.max(expected, 1e-9);
            if (freq[i] > 0) {
                ranked.add(new int[]{i, freq[i]});
            }
        }
        ranked.sort((a, b) -> Integer.compare(b[1], a[1]));
        Map<String, Integer> top = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(12, ranked.size()); i++) {
            top.put(String.format("%02X", ranked.get(i)[0]), ranked.get(i)[1]);
        }
        boolean sufficientExpectedFrequency = expected >= 5;
        double pValue = chiSquareSurvivalProbability(chi, freq.length - 1);
        boolean uniform = sufficientExpectedFrequency && pValue >= CHI_SQUARE_SIGNIFICANCE_LEVEL;
        String analysis = !sufficientExpectedFrequency
                ? "Amostra curta: há menos de 5 bytes esperados por valor. O p-valor é apenas indicativo; aumenta a quantidade de amostras."
                : uniform
                        ? "Não foi detectado desvio significativo da distribuição uniforme (p ≥ 0,05). Isso não prova uniformidade nem segurança."
                        : "Foi detectado desvio significativo da distribuição uniforme (p < 0,05); isso sinaliza um padrão nesta amostra, não uma quebra do cipher.";
        return new StatisticalResult(
                sampleCount,
                data.length,
                top,
                Math.round(chi * 100.0) / 100.0,
                Math.round(expected * 1000.0) / 1000.0,
                pValue,
                sufficientExpectedFrequency,
                uniform,
                analysis
        );
    }

    /** Right-tail probability for a chi-square statistic with the given degrees of freedom. */
    private static double chiSquareSurvivalProbability(double chiSquare, int degreesOfFreedom) {
        if (chiSquare <= 0) return 1.0;
        return regularizedGammaQ(degreesOfFreedom / 2.0, chiSquare / 2.0);
    }

    /** Regularized upper incomplete gamma Q(a, x), used as the chi-square survival function. */
    private static double regularizedGammaQ(double a, double x) {
        if (a <= 0 || x < 0 || Double.isNaN(a) || Double.isNaN(x)) return Double.NaN;
        if (x == 0) return 1.0;
        if (x < a + 1.0) {
            return Math.max(0.0, Math.min(1.0, 1.0 - regularizedGammaPSeries(a, x)));
        }

        double b = x + 1.0 - a;
        double c = 1.0 / GAMMA_FPMIN;
        double d = 1.0 / b;
        double fraction = d;
        for (int iteration = 1; iteration <= GAMMA_MAX_ITERATIONS; iteration++) {
            double an = -iteration * (iteration - a);
            b += 2.0;
            d = an * d + b;
            if (Math.abs(d) < GAMMA_FPMIN) d = GAMMA_FPMIN;
            c = b + an / c;
            if (Math.abs(c) < GAMMA_FPMIN) c = GAMMA_FPMIN;
            d = 1.0 / d;
            double delta = d * c;
            fraction *= delta;
            if (Math.abs(delta - 1.0) < GAMMA_EPSILON) break;
        }

        double scale = Math.exp(-x + a * Math.log(x) - logGamma(a));
        return Math.max(0.0, Math.min(1.0, scale * fraction));
    }

    /** Regularized lower incomplete gamma P(a, x), evaluated by its convergent series. */
    private static double regularizedGammaPSeries(double a, double x) {
        double term = 1.0 / a;
        double sum = term;
        for (int iteration = 1; iteration <= GAMMA_MAX_ITERATIONS; iteration++) {
            term *= x / (a + iteration);
            sum += term;
            if (Math.abs(term) <= Math.abs(sum) * GAMMA_EPSILON) break;
        }

        double scale = Math.exp(-x + a * Math.log(x) - logGamma(a));
        return Math.max(0.0, Math.min(1.0, sum * scale));
    }

    /** Lanczos approximation for log(Gamma(z)); arguments here are positive. */
    private static double logGamma(double value) {
        if (value < 0.5) {
            return Math.log(Math.PI) - Math.log(Math.sin(Math.PI * value)) - logGamma(1.0 - value);
        }
        double z = value - 1.0;
        double series = 0.99999999999980993;
        for (int index = 0; index < LANCZOS_COEFFICIENTS.length; index++) {
            series += LANCZOS_COEFFICIENTS[index] / (z + index + 1.0);
        }
        double t = z + LANCZOS_COEFFICIENTS.length - 0.5;
        return 0.5 * Math.log(2.0 * Math.PI) + (z + 0.5) * Math.log(t) - t + Math.log(series);
    }
}
