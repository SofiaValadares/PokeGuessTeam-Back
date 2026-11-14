package com.svc.pokeguessteam.dto.securitylab;

import java.util.List;
import java.util.Map;

public final class SecurityLabDtos {
    private SecurityLabDtos() {
    }

    public record EncryptRequest(String plaintext, String seed) {
    }

    public record EncryptResponse(
            String ciphertextBase64,
            String seedUsed,
            String nonceBase64,
            String keyFingerprint,
            int blockCount,
            String pipeline
    ) {
    }

    public record DecryptRequest(String ciphertextBase64, String seed, String nonceBase64) {
    }

    public record DecryptResponse(String plaintext, boolean success) {
    }

    public record StegoEmbedRequest(String imageBase64, String message) {
    }

    public record StegoEmbedResponse(String imageBase64Png, int capacityBits, int usedBits) {
    }

    public record StegoExtractRequest(String imageBase64) {
    }

    public record StegoExtractResponse(String message) {
    }

    public record AttackRequest(String plaintext, String seed, Integer searchableBits) {
    }

    public record BruteForceResponse(
            long keySpaceBits,
            long attempted,
            boolean found,
            long elapsedMs,
            String viability,
            String recoveredSeedHint
    ) {
    }

    public record DifferentialResponse(
            String originalCipherBase64,
            String flippedCipherBase64,
            int changedBits,
            int totalBitsCompared,
            double avalanchePercent,
            boolean strongAvalanche
    ) {
    }

    public record StatisticalResponse(
            int sampleBytes,
            Map<String, Integer> topFrequencies,
            double chiSquare,
            double uniformExpected,
            boolean looksUniform,
            String analysis
    ) {
    }

    public record AnomalyHitDto(String code, String summary, double score, String detailJson) {
    }

    public record AnomalyScanResponse(List<AnomalyHitDto> hits) {
    }

    public record DashboardBucket(String date, String eventType, long count) {
    }

    public record DashboardResponse(
            Map<String, Long> totalsByType,
            List<DashboardBucket> daily,
            List<AnomalyHitDto> recentAnomalies,
            String conclusions
    ) {
    }
}
