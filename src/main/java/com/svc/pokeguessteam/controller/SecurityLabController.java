package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.securitylab.SecurityLabDtos;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.SecurityLabService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Lab de criptografia / ataques / estego / anomalias (Unidade 2) — restrito a DEVELOPER.
 */
@RestController
@RequestMapping("/api/dev/security-lab")
public class SecurityLabController {

    private final SecurityLabService securityLabService;
    private final CurrentUserService currentUserService;

    public SecurityLabController(SecurityLabService securityLabService, CurrentUserService currentUserService) {
        this.securityLabService = securityLabService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/encrypt")
    public ResponseEntity<SecurityLabDtos.EncryptResponse> encrypt(
            HttpSession session,
            @RequestBody SecurityLabDtos.EncryptRequest body
    ) {
        return ResponseEntity.ok(securityLabService.encrypt(currentUserService.requireUserId(session), body));
    }

    @PostMapping("/decrypt")
    public ResponseEntity<SecurityLabDtos.DecryptResponse> decrypt(
            HttpSession session,
            @RequestBody SecurityLabDtos.DecryptRequest body
    ) {
        return ResponseEntity.ok(securityLabService.decrypt(currentUserService.requireUserId(session), body));
    }

    @PostMapping("/stego/embed")
    public ResponseEntity<SecurityLabDtos.StegoEmbedResponse> stegoEmbed(
            HttpSession session,
            @RequestBody SecurityLabDtos.StegoEmbedRequest body
    ) {
        return ResponseEntity.ok(securityLabService.stegoEmbed(currentUserService.requireUserId(session), body));
    }

    @PostMapping("/stego/extract")
    public ResponseEntity<SecurityLabDtos.StegoExtractResponse> stegoExtract(
            HttpSession session,
            @RequestBody SecurityLabDtos.StegoExtractRequest body
    ) {
        return ResponseEntity.ok(securityLabService.stegoExtract(currentUserService.requireUserId(session), body));
    }

    /** Ataque: tentar extrair mensagem oculta de uma imagem suspeita (ex.: avatar). */
    @PostMapping("/stego/attack")
    public ResponseEntity<SecurityLabDtos.StegoExtractResponse> stegoAttack(
            HttpSession session,
            @RequestBody SecurityLabDtos.StegoExtractRequest body
    ) {
        return ResponseEntity.ok(securityLabService.stegoAttack(currentUserService.requireUserId(session), body));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "enabled", securityLabService.isLabEnabled(),
                "temporaryDeployNotice", securityLabService.isTemporaryDeployNotice(),
                "access", "ROLE_DEVELOPER (perfil Lab)"
        ));
    }

    @GetMapping("/samples")
    public ResponseEntity<List<Map<String, String>>> listSamples() {
        securityLabService.assertLabEnabled();
        return ResponseEntity.ok(com.svc.pokeguessteam.securitylab.StegoSampleImages.catalog());
    }

    @GetMapping("/samples/{id}")
    public ResponseEntity<byte[]> downloadSample(@PathVariable String id) {
        securityLabService.assertLabEnabled();
        var sample = com.svc.pokeguessteam.securitylab.StegoSampleImages.load(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(sample.contentType());
        } catch (Exception e) {
            mediaType = MediaType.IMAGE_PNG;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sample.filename() + "\"")
                .contentType(mediaType)
                .body(sample.bytes());
    }

    @PostMapping("/attacks/brute-force")
    public ResponseEntity<SecurityLabDtos.BruteForceResponse> bruteForce(
            HttpSession session,
            @RequestBody SecurityLabDtos.AttackRequest body
    ) {
        return ResponseEntity.ok(securityLabService.bruteForce(currentUserService.requireUserId(session), body));
    }

    @PostMapping("/attacks/differential")
    public ResponseEntity<SecurityLabDtos.DifferentialResponse> differential(
            HttpSession session,
            @RequestBody SecurityLabDtos.AttackRequest body
    ) {
        return ResponseEntity.ok(securityLabService.differential(currentUserService.requireUserId(session), body));
    }

    @PostMapping("/attacks/differential/samples")
    public ResponseEntity<SecurityLabDtos.DifferentialSamplesResponse> differentialSamples(
            HttpSession session,
            @RequestBody SecurityLabDtos.DifferentialSamplesRequest body
    ) {
        return ResponseEntity.ok(
                securityLabService.differentialSamples(currentUserService.requireUserId(session), body)
        );
    }

    @PostMapping("/attacks/statistical")
    public ResponseEntity<SecurityLabDtos.StatisticalResponse> statistical(
            HttpSession session,
            @RequestBody SecurityLabDtos.AttackRequest body
    ) {
        return ResponseEntity.ok(securityLabService.statistical(currentUserService.requireUserId(session), body));
    }

    @PostMapping("/anomalies/scan")
    public ResponseEntity<SecurityLabDtos.AnomalyScanResponse> scanAnomalies(HttpSession session) {
        return ResponseEntity.ok(securityLabService.scanAnomalies(currentUserService.requireUserId(session)));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<SecurityLabDtos.DashboardResponse> dashboard() {
        return ResponseEntity.ok(securityLabService.dashboard());
    }
}
