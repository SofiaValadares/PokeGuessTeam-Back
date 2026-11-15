package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.config.AppSecurityLabProperties;
import com.svc.pokeguessteam.dto.securitylab.SecurityLabDtos;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.SecurityLabEventType;
import com.svc.pokeguessteam.model.securitylab.SecurityLabEventModel;
import com.svc.pokeguessteam.repository.securitylab.SecurityLabEventRepository;
import com.svc.pokeguessteam.repository.user.UserRepository;
import com.svc.pokeguessteam.securitylab.AnomalyDetectionService;
import com.svc.pokeguessteam.securitylab.CryptoAttackLabService;
import com.svc.pokeguessteam.securitylab.SteganographyService;
import com.svc.pokeguessteam.securitylab.SymmetricLabCipher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class SecurityLabService {

    private final AppSecurityLabProperties securityLabProperties;
    private final SymmetricLabCipher cipher;
    private final SteganographyService steganographyService;
    private final CryptoAttackLabService attackLabService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final SecurityLabEventRepository eventRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public SecurityLabService(
            AppSecurityLabProperties securityLabProperties,
            SymmetricLabCipher cipher,
            SteganographyService steganographyService,
            CryptoAttackLabService attackLabService,
            AnomalyDetectionService anomalyDetectionService,
            SecurityLabEventRepository eventRepository,
            AuditLogService auditLogService,
            UserRepository userRepository
    ) {
        this.securityLabProperties = securityLabProperties;
        this.cipher = cipher;
        this.steganographyService = steganographyService;
        this.attackLabService = attackLabService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.eventRepository = eventRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    public void assertLabEnabled() {
        if (!securityLabProperties.isEnabled()) {
            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.SECURITY_LAB_DISABLED,
                    MessageKeys.SECURITY_LAB_DISABLED
            );
        }
    }

    public boolean isLabEnabled() {
        return securityLabProperties.isEnabled();
    }

    public boolean isTemporaryDeployNotice() {
        return securityLabProperties.isTemporaryDeployNotice();
    }

    @Transactional
    public SecurityLabDtos.EncryptResponse encrypt(String userId, SecurityLabDtos.EncryptRequest req) {
        assertLabEnabled();
        String plaintext = req.plaintext() == null ? "" : req.plaintext();
        String seed = req.seed();
        var result = cipher.encrypt(plaintext, seed);
        // Re-derive seed note: if seed blank, cipher used random — client must store fingerprint/nonce only with provided seed
        String seedUsed = (seed == null || seed.isBlank()) ? "(gerada dinamicamente no servidor — forneça seed nas próximas vezes)" : seed.trim();
        if (seed == null || seed.isBlank()) {
            // Force deterministic path for lab UX: invent a visible seed
            seedUsed = "dyn-" + result.keyMaterial().fingerprint();
            result = cipher.encrypt(plaintext, seedUsed);
        }
        persist(userId, SecurityLabEventType.ENCRYPT,
                "Cifragem lab (" + result.blockCount() + " blocos)",
                "{\"fingerprint\":\"" + result.keyMaterial().fingerprint() + "\",\"blocks\":" + result.blockCount() + "}",
                (double) result.blockCount());
        auditLogService.recordAdminAction(userId, "SEC_LAB_ENCRYPT", "fp=" + result.keyMaterial().fingerprint());
        return new SecurityLabDtos.EncryptResponse(
                result.ciphertextBase64(),
                seedUsed,
                result.keyMaterial().nonceBase64(),
                result.keyMaterial().fingerprint(),
                result.blockCount(),
                result.pipeline()
        );
    }

    @Transactional
    public SecurityLabDtos.DecryptResponse decrypt(String userId, SecurityLabDtos.DecryptRequest req) {
        assertLabEnabled();
        try {
            var km = cipher.restoreKey(req.seed(), req.nonceBase64());
            String plain = cipher.decrypt(req.ciphertextBase64(), km);
            persist(userId, SecurityLabEventType.DECRYPT, "Descriptografia lab OK", null, 1.0);
            return new SecurityLabDtos.DecryptResponse(plain, true);
        } catch (Exception e) {
            persist(userId, SecurityLabEventType.DECRYPT_FAIL,
                    "Falha de descriptografia lab",
                    "{\"error\":\"" + sanitize(e.getMessage()) + "\"}",
                    0.0);
            auditLogService.recordSecurity(userId, "SEC_LAB_DECRYPT_FAIL", e.getMessage());
            return new SecurityLabDtos.DecryptResponse(null, false);
        }
    }

    @Transactional
    public SecurityLabDtos.StegoEmbedResponse stegoEmbed(String userId, SecurityLabDtos.StegoEmbedRequest req) {
        assertLabEnabled();
        var result = steganographyService.embed(req.imageBase64(), req.message() == null ? "" : req.message());
        persist(userId, SecurityLabEventType.STEGO_EMBED,
                "Esteganografia embed (" + result.usedBits() + " bits)",
                "{\"usedBits\":" + result.usedBits() + ",\"capacityBits\":" + result.capacityBits() + "}",
                (double) result.usedBits());
        return new SecurityLabDtos.StegoEmbedResponse(result.imageBase64Png(), result.capacityBits(), result.usedBits());
    }

    @Transactional
    public SecurityLabDtos.StegoExtractResponse stegoExtract(String userId, SecurityLabDtos.StegoExtractRequest req) {
        assertLabEnabled();
        try {
            String msg = steganographyService.extract(req.imageBase64());
            persist(userId, SecurityLabEventType.STEGO_EXTRACT, "Esteganografia extract OK", null, 1.0);
            return new SecurityLabDtos.StegoExtractResponse(msg);
        } catch (Exception e) {
            persist(userId, SecurityLabEventType.STEGO_EXTRACT_FAIL,
                    "Falha extract esteganografia",
                    "{\"error\":\"" + sanitize(e.getMessage()) + "\"}",
                    0.0);
            throw e;
        }
    }

    @Transactional
    public SecurityLabDtos.BruteForceResponse bruteForce(String userId, SecurityLabDtos.AttackRequest req) {
        assertLabEnabled();
        var r = attackLabService.bruteForce(
                req.plaintext() == null ? "poke" : req.plaintext(),
                req.seed(),
                req.searchableBits() == null ? 12 : req.searchableBits(),
                req.maxSearchDurationMs()
        );
        persist(userId, SecurityLabEventType.ATTACK_BRUTE, "Ataque força bruta lab",
                "{\"attempted\":" + r.attempted() + ",\"found\":" + r.found() + "}",
                (double) r.attempted());
        return new SecurityLabDtos.BruteForceResponse(
                r.keySpaceBits(),
                r.attempted(),
                r.found(),
                r.elapsedMs(),
                r.timeLimited(),
                r.attemptsPerSecond(),
                r.expectedSecondsAtObservedRate(),
                r.worstCaseSecondsAtObservedRate(),
                r.expectedSecondsAtPowerfulRate(),
                r.worstCaseSecondsAtPowerfulRate(),
                r.powerfulMachineAssessment(),
                r.viability(),
                r.recoveredSeedHint()
        );
    }

    @Transactional
    public SecurityLabDtos.DifferentialResponse differential(String userId, SecurityLabDtos.AttackRequest req) {
        assertLabEnabled();
        var r = attackLabService.differential(
                req.plaintext() == null || req.plaintext().isBlank() ? "Pikachu" : req.plaintext(),
                req.seed(),
                req.differentialBitPosition() == null ? 1 : req.differentialBitPosition(),
                req.differentialBitCount() == null ? 1 : req.differentialBitCount()
        );
        persist(userId, SecurityLabEventType.ATTACK_DIFFERENTIAL,
                "Ataque diferencial — " + r.flippedBitCount() + " bit(s), avalanche "
                        + r.avalanchePercent() + "%",
                "{\"flippedBitPositions\":" + r.flippedBitPositions()
                        + ",\"flippedBitCount\":" + r.flippedBitCount()
                        + ",\"avalanchePercent\":" + r.avalanchePercent() + "}",
                r.avalanchePercent());
        return new SecurityLabDtos.DifferentialResponse(
                r.originalCipherBase64(), r.flippedCipherBase64(), r.flippedBitPositions(), r.flippedBitCount(),
                r.changedBits(),
                r.totalBitsCompared(), r.avalanchePercent(), r.strongAvalanche()
        );
    }

    @Transactional
    public SecurityLabDtos.DifferentialSamplesResponse differentialSamples(
            String userId,
            SecurityLabDtos.DifferentialSamplesRequest req
    ) {
        assertLabEnabled();
        List<CryptoAttackLabService.DifferentialResult> results = attackLabService.differentialSamples(
                req.plaintext() == null || req.plaintext().isBlank() ? "Pikachu" : req.plaintext(),
                req.seed(),
                req.sampleCount() == null ? 1 : req.sampleCount()
        );
        List<SecurityLabDtos.DifferentialResponse> samples = results.stream()
                .map(r -> new SecurityLabDtos.DifferentialResponse(
                        r.originalCipherBase64(), r.flippedCipherBase64(), r.flippedBitPositions(),
                        r.flippedBitCount(), r.changedBits(), r.totalBitsCompared(),
                        r.avalanchePercent(), r.strongAvalanche()
                ))
                .toList();
        double averageAvalanche = results.stream()
                .mapToDouble(CryptoAttackLabService.DifferentialResult::avalanchePercent)
                .average()
                .orElse(0);
        persist(userId, SecurityLabEventType.ATTACK_DIFFERENTIAL,
                "Ataque diferencial — " + samples.size() + " amostras bit-a-bit, avalanche média "
                        + Math.round(averageAvalanche * 100.0) / 100.0 + "%",
                "{\"sampleCount\":" + samples.size()
                        + ",\"averageAvalanchePercent\":" + averageAvalanche + "}",
                averageAvalanche);
        return new SecurityLabDtos.DifferentialSamplesResponse(samples);
    }

    @Transactional
    public SecurityLabDtos.StatisticalResponse statistical(String userId, SecurityLabDtos.AttackRequest req) {
        assertLabEnabled();
        var r = attackLabService.statistical(
                req.plaintext() == null || req.plaintext().isBlank()
                        ? "The quick brown fox jumps over the lazy pokemon trainer 0123456789"
                        : req.plaintext(),
                req.seed(),
                req.sampleCount() == null ? 100 : req.sampleCount()
        );
        persist(userId, SecurityLabEventType.ATTACK_STATISTICAL,
                "Ataque estatístico — " + r.sampleCount() + " amostras, chi²=" + r.chiSquare()
                        + ", p=" + r.pValue(),
                "{\"sampleCount\":" + r.sampleCount() + ",\"sampleBytes\":" + r.sampleBytes()
                        + ",\"chiSquare\":" + r.chiSquare() + ",\"pValue\":" + r.pValue()
                        + ",\"sufficientSample\":" + r.sufficientSample()
                        + ",\"noSignificantDeviation\":" + r.looksUniform() + "}",
                r.chiSquare());
        return new SecurityLabDtos.StatisticalResponse(
                r.sampleCount(), r.sampleBytes(), r.topFrequencies(), r.chiSquare(),
                r.uniformExpected(), r.pValue(), r.sufficientSample(), r.looksUniform(), r.analysis()
        );
    }

    @Transactional
    public SecurityLabDtos.AnomalyScanResponse scanAnomalies(String userId) {
        assertLabEnabled();
        var hits = anomalyDetectionService.scanAndPersist(userId);
        return new SecurityLabDtos.AnomalyScanResponse(
                hits.stream()
                        .map(h -> new SecurityLabDtos.AnomalyHitDto(h.code(), h.summary(), h.score(), h.detailJson()))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public SecurityLabDtos.DashboardResponse dashboard() {
        assertLabEnabled();
        LocalDateTime after = LocalDateTime.now().minusDays(14);
        Map<String, Long> totals = new LinkedHashMap<>();
        for (Object[] row : eventRepository.countByTypeSince(after)) {
            totals.put(String.valueOf(row[0]), (Long) row[1]);
        }

        Map<String, Long> dailyMap = new TreeMap<>();
        List<SecurityLabEventModel> recent = eventRepository.findByCreatedAtAfterOrderByCreatedAtDesc(
                after, PageRequest.of(0, 2000));
        for (SecurityLabEventModel e : recent) {
            String key = e.getCreatedAt().toLocalDate() + "|" + e.getEventType().name();
            dailyMap.merge(key, 1L, Long::sum);
        }
        List<SecurityLabDtos.DashboardBucket> daily = new ArrayList<>();
        for (Map.Entry<String, Long> entry : dailyMap.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            daily.add(new SecurityLabDtos.DashboardBucket(parts[0], parts[1], entry.getValue()));
        }

        List<SecurityLabDtos.AnomalyHitDto> anomalies = anomalyDetectionService.recentAnomalies(20).stream()
                .map(e -> new SecurityLabDtos.AnomalyHitDto(
                        "ANOMALY",
                        e.getSummary(),
                        e.getMetricValue() == null ? 0 : e.getMetricValue(),
                        e.getDetailJson()
                ))
                .toList();

        String conclusions = buildConclusions(totals);
        return new SecurityLabDtos.DashboardResponse(totals, daily, anomalies, conclusions);
    }

    private String buildConclusions(Map<String, Long> totals) {
        long diff = totals.getOrDefault(SecurityLabEventType.ATTACK_DIFFERENTIAL.name(), 0L);
        long brute = totals.getOrDefault(SecurityLabEventType.ATTACK_BRUTE.name(), 0L);
        long stat = totals.getOrDefault(SecurityLabEventType.ATTACK_STATISTICAL.name(), 0L);
        long anomalies = totals.getOrDefault(SecurityLabEventType.ANOMALY.name(), 0L);
        return "Cipher didático lab: chave dinâmica + XOR e 4 rodadas de S-box/ShiftRows/MixColumns/subchave. "
                + "Força bruta só é eficaz em espaço reduzido; chave de 256 bits é inviável. "
                + "Avalanche (diferencial) e qui-quadrado (estatístico) medem difusão/confusão. "
                + "AES-GCM do jogo permanece a camada forte. "
                + "Eventos 14d — diferencial:" + diff + " brute:" + brute + " estat:" + stat
                + " anomalias:" + anomalies + ".";
    }

    @Transactional
    public SecurityLabDtos.StegoExtractResponse stegoAttack(String userId, SecurityLabDtos.StegoExtractRequest req) {
        assertLabEnabled();
        try {
            String msg = steganographyService.extract(req.imageBase64());
            persist(userId, SecurityLabEventType.STEGO_ATTACK,
                    "Ataque estego: mensagem recuperada",
                    "{\"success\":true,\"length\":" + msg.length() + "}",
                    1.0);
            return new SecurityLabDtos.StegoExtractResponse(msg);
        } catch (Exception e) {
            persist(userId, SecurityLabEventType.STEGO_ATTACK,
                    "Ataque estego: falha ao extrair",
                    "{\"success\":false,\"error\":\"" + sanitize(e.getMessage()) + "\"}",
                    0.0);
            throw e;
        }
    }

    private void persist(String userId, SecurityLabEventType type, String summary, String detail, Double metric) {
        SecurityLabEventModel e = new SecurityLabEventModel();
        e.setEventType(type);
        e.setActorUserId(userId);
        userRepository.findById(userId).ifPresent(u -> {
            e.setActorUsername(u.getUsername());
            e.setActorWasDeveloper(u.isDeveloper());
        });
        e.setSummary(summary);
        e.setDetailJson(detail);
        e.setMetricValue(metric);
        eventRepository.save(e);
    }

    private static String sanitize(String msg) {
        if (msg == null) return "";
        return msg.replace("\"", "'").substring(0, Math.min(180, msg.length()));
    }
}
