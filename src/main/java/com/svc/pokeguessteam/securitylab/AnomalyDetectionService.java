package com.svc.pokeguessteam.securitylab;

import com.svc.pokeguessteam.model.enums.SecurityLabEventType;
import com.svc.pokeguessteam.model.securitylab.SecurityLabEventModel;
import com.svc.pokeguessteam.repository.audit.AuditLogRepository;
import com.svc.pokeguessteam.repository.securitylab.SecurityLabEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnomalyDetectionService {

    public static final int LOGIN_FAIL_WINDOW_MINUTES = 15;
    public static final long LOGIN_FAIL_THRESHOLD = 8;
    public static final long DECRYPT_FAIL_THRESHOLD = 5;

    private final AuditLogRepository auditLogRepository;
    private final SecurityLabEventRepository labEventRepository;

    public AnomalyDetectionService(
            AuditLogRepository auditLogRepository,
            SecurityLabEventRepository labEventRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.labEventRepository = labEventRepository;
    }

    public record AnomalyHit(String code, String summary, double score, String detailJson) {
    }

    @Transactional
    public List<AnomalyHit> scanAndPersist(String actorUserId) {
        List<AnomalyHit> hits = new ArrayList<>();
        LocalDateTime window = LocalDateTime.now().minusMinutes(LOGIN_FAIL_WINDOW_MINUTES);

        long loginFails = auditLogRepository.countByActionAndCreatedAtAfter("LOGIN_FAILED", window);
        if (loginFails >= LOGIN_FAIL_THRESHOLD) {
            AnomalyHit hit = new AnomalyHit(
                    "AUTH_BURST",
                    "Número incomum de falhas de autenticação (" + loginFails + " em "
                            + LOGIN_FAIL_WINDOW_MINUTES + " min)",
                    loginFails / (double) LOGIN_FAIL_THRESHOLD,
                    "{\"loginFails\":" + loginFails + ",\"windowMinutes\":" + LOGIN_FAIL_WINDOW_MINUTES + "}"
            );
            hits.add(hit);
            persist(actorUserId, hit);
        }

        long decryptFails = labEventRepository.countByEventTypeAndCreatedAtAfter(
                SecurityLabEventType.DECRYPT_FAIL, window);
        if (decryptFails >= DECRYPT_FAIL_THRESHOLD) {
            AnomalyHit hit = new AnomalyHit(
                    "DECRYPT_FAIL_BURST",
                    "Frequência elevada de falhas de descriptografia no lab (" + decryptFails + ")",
                    decryptFails / (double) DECRYPT_FAIL_THRESHOLD,
                    "{\"decryptFails\":" + decryptFails + "}"
            );
            hits.add(hit);
            persist(actorUserId, hit);
        }

        long stegoFails = labEventRepository.countByEventTypeAndCreatedAtAfter(
                SecurityLabEventType.STEGO_EXTRACT_FAIL, window);
        if (stegoFails >= 4) {
            AnomalyHit hit = new AnomalyHit(
                    "STEGO_EXTRACT_BURST",
                    "Padrão atípico de falhas ao extrair mensagens esteganográficas (" + stegoFails + ")",
                    stegoFails / 4.0,
                    "{\"stegoExtractFails\":" + stegoFails + "}"
            );
            hits.add(hit);
            persist(actorUserId, hit);
        }

        // Taxa de eventos de ataque no lab (comportamento incomum de geração)
        long attacks = labEventRepository.countByEventTypeAndCreatedAtAfter(SecurityLabEventType.ATTACK_BRUTE, window)
                + labEventRepository.countByEventTypeAndCreatedAtAfter(SecurityLabEventType.ATTACK_DIFFERENTIAL, window)
                + labEventRepository.countByEventTypeAndCreatedAtAfter(SecurityLabEventType.ATTACK_STATISTICAL, window);
        if (attacks >= 20) {
            AnomalyHit hit = new AnomalyHit(
                    "ATTACK_LAB_BURST",
                    "Geração intensa de eventos de ataque no lab (" + attacks + ")",
                    attacks / 20.0,
                    "{\"attackEvents\":" + attacks + "}"
            );
            hits.add(hit);
            persist(actorUserId, hit);
        }

        return hits;
    }

    public List<SecurityLabEventModel> recentAnomalies(int limit) {
        return labEventRepository.findByEventTypeInAndCreatedAtAfterOrderByCreatedAtDesc(
                List.of(SecurityLabEventType.ANOMALY),
                LocalDateTime.now().minusDays(7),
                PageRequest.of(0, Math.min(100, Math.max(1, limit)))
        );
    }

    private void persist(String actorUserId, AnomalyHit hit) {
        SecurityLabEventModel e = new SecurityLabEventModel();
        e.setEventType(SecurityLabEventType.ANOMALY);
        e.setActorUserId(actorUserId);
        e.setSummary(hit.summary());
        e.setDetailJson(hit.detailJson());
        e.setMetricValue(hit.score());
        labEventRepository.save(e);
    }
}
