package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.TeamOpeningDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Protocolo commit → play → open do time secreto (anti-cheat).
 * <p>
 * Payload canónico {@code v1|&lt;dex,...&gt;|&lt;nonceHex&gt;}:
 * <ul>
 *   <li>{@code C} público = SHA-256(payload) — publicado no commit, verificável após a abertura</li>
 *   <li>MAC = HMAC-SHA256(chave do servidor, {@code pgt.team-commit.v1|C}) — autentica C</li>
 *   <li>abertura selada = AES-256-GCM(payload) — o nonce não é revelado até o open</li>
 * </ul>
 */
@Service
public class TeamCommitmentService {

    public static final int NONCE_BYTES = 32;
    public static final String PAYLOAD_VERSION = "v1";
    static final String MAC_PREFIX = "pgt.team-commit.v1|";

    private final CryptoService cryptoService;

    public TeamCommitmentService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public IssuedTeamCommitment issue(List<Integer> team) {
        String nonce = HexFormat.of().formatHex(cryptoService.randomBytes(NONCE_BYTES));
        String payload = canonicalPayload(team, nonce);
        String commitment = cryptoService.sha256Hex(payload);
        String mac = macOf(commitment);
        String sealed = cryptoService.encrypt(payload);
        return new IssuedTeamCommitment(commitment, mac, sealed, nonce, List.copyOf(team));
    }

    public IssuedTeamCommitment bindPlayer(ActiveMatchPlayerModel player, List<Integer> team) {
        IssuedTeamCommitment issued = issue(team);
        player.setTeam(issued.team());
        player.setTeamCommitment(issued.commitment());
        player.setTeamCommitmentMac(issued.mac());
        player.setSealedOpening(issued.sealedOpening());
        return issued;
    }

    public void verifyOpening(ActiveMatchPlayerModel player, TeamOpeningDto opening) {
        if (opening == null || opening.team() == null || opening.nonce() == null) {
            throw commitmentInvalid();
        }
        openClaimedTeam(player, opening.team(), opening.nonce());
    }

    /**
     * Abre o AES-GCM, verifica SHA-256 + HMAC, e exige que o time (e nonce, se enviado)
     * coincidam com o commitment publicado no início.
     */
    public TeamOpeningDto openClaimedTeam(
            ActiveMatchPlayerModel player,
            List<Integer> claimedTeam,
            String claimedNonceOrNull
    ) {
        requireBound(player);
        if (claimedTeam == null) {
            throw commitmentInvalid();
        }
        ParsedPayload parsed = decryptSealed(player.getSealedOpening());
        String computed = cryptoService.sha256Hex(parsed.payload());
        if (!CryptoService.hexEquals(computed, player.getTeamCommitment())) {
            throw commitmentInvalid();
        }
        if (!CryptoService.hexEquals(macOf(player.getTeamCommitment()), player.getTeamCommitmentMac())) {
            throw commitmentInvalid();
        }
        if (!parsed.team().equals(List.copyOf(claimedTeam))) {
            throw commitmentInvalid();
        }
        if (claimedNonceOrNull != null
                && !CryptoService.hexEquals(parsed.nonce(), claimedNonceOrNull.toLowerCase(Locale.ROOT))) {
            throw commitmentInvalid();
        }
        return new TeamOpeningDto(parsed.team(), parsed.nonce());
    }

    public TeamOpeningDto openSealed(ActiveMatchPlayerModel player) {
        requireBound(player);
        ParsedPayload parsed = decryptSealed(player.getSealedOpening());
        String computed = cryptoService.sha256Hex(parsed.payload());
        if (!CryptoService.hexEquals(computed, player.getTeamCommitment())) {
            throw commitmentInvalid();
        }
        if (!CryptoService.hexEquals(macOf(player.getTeamCommitment()), player.getTeamCommitmentMac())) {
            throw commitmentInvalid();
        }
        return new TeamOpeningDto(parsed.team(), parsed.nonce());
    }

    public String nonceOf(ActiveMatchPlayerModel player) {
        if (player == null || player.getSealedOpening() == null || player.getSealedOpening().isBlank()) {
            return null;
        }
        return decryptSealed(player.getSealedOpening()).nonce();
    }

    public static String canonicalPayload(List<Integer> team, String nonceHex) {
        if (team == null || nonceHex == null) {
            throw new IllegalArgumentException("team/nonce obrigatórios");
        }
        List<String> parts = new ArrayList<>(team.size());
        for (Integer dex : team) {
            if (dex == null) {
                throw new IllegalArgumentException("dex nulo no time");
            }
            parts.add(Integer.toString(dex));
        }
        return PAYLOAD_VERSION + "|" + String.join(",", parts) + "|" + nonceHex.toLowerCase(Locale.ROOT);
    }

    private String macOf(String commitment) {
        return cryptoService.hmacSha256Hex(MAC_PREFIX + commitment);
    }

    private void requireBound(ActiveMatchPlayerModel player) {
        if (player == null
                || player.getTeamCommitment() == null
                || player.getTeamCommitmentMac() == null
                || player.getSealedOpening() == null) {
            throw commitmentInvalid();
        }
    }

    private ParsedPayload decryptSealed(String sealed) {
        try {
            return parseCanonical(cryptoService.decrypt(sealed));
        } catch (RuntimeException ex) {
            throw commitmentInvalid();
        }
    }

    static ParsedPayload parseCanonical(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload vazio");
        }
        int first = payload.indexOf('|');
        int last = payload.lastIndexOf('|');
        if (first < 0 || last <= first) {
            throw new IllegalArgumentException("payload inválido");
        }
        String version = payload.substring(0, first);
        String teamCsv = payload.substring(first + 1, last);
        String nonce = payload.substring(last + 1).toLowerCase(Locale.ROOT);
        if (!PAYLOAD_VERSION.equals(version) || teamCsv.isBlank() || nonce.isBlank()) {
            throw new IllegalArgumentException("payload inválido");
        }
        List<Integer> team = new ArrayList<>();
        for (String part : teamCsv.split(",")) {
            team.add(Integer.parseInt(part.trim()));
        }
        return new ParsedPayload(List.copyOf(team), nonce, payload);
    }

    private static ApiBusinessException commitmentInvalid() {
        return new ApiBusinessException(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.GAME_TEAM_COMMITMENT_INVALID,
                MessageKeys.GAME_TEAM_COMMITMENT_INVALID
        );
    }

    public record IssuedTeamCommitment(
            String commitment,
            String mac,
            String sealedOpening,
            String nonce,
            List<Integer> team
    ) {
    }

    record ParsedPayload(List<Integer> team, String nonce, String payload) {
    }
}
