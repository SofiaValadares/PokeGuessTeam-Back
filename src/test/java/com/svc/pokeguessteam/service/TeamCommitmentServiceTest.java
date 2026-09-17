package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.TeamOpeningDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamCommitmentServiceTest {

    private static final String KEY =
            "617e30427ecf9bf102367d03beb131336224bc1da1584ff3d04a50aa11422894";

    private TeamCommitmentService service;

    @BeforeEach
    void setUp() {
        service = new TeamCommitmentService(new CryptoService(KEY));
    }

    @Test
    void openingMatchesPublishedCommitment() {
        List<Integer> team = List.of(25, 6, 9, 3, 94, 130);
        TeamCommitmentService.IssuedTeamCommitment issued = service.issue(team);

        ActiveMatchPlayerModel player = bound(issued);

        TeamOpeningDto opened = service.openClaimedTeam(player, team, null);
        assertEquals(team, opened.team());
        assertEquals(issued.nonce(), opened.nonce());
        assertEquals(64, issued.commitment().length());
        assertEquals(issued.nonce(), service.nonceOf(player));
        service.verifyOpening(player, new TeamOpeningDto(team, issued.nonce()));
    }

    @Test
    void mutatedTeamFailsOpen() {
        List<Integer> team = List.of(1, 2, 3, 4, 5, 6);
        ActiveMatchPlayerModel player = bound(service.issue(team));

        assertThrows(
                ApiBusinessException.class,
                () -> service.openClaimedTeam(player, List.of(1, 2, 3, 4, 5, 7), null)
        );
    }

    @Test
    void differentNonceFailsOpen() {
        List<Integer> team = List.of(1, 2, 3, 4, 5, 6);
        TeamCommitmentService.IssuedTeamCommitment first = service.issue(team);
        TeamCommitmentService.IssuedTeamCommitment other = service.issue(team);
        assertNotEquals(first.nonce(), other.nonce());
        ActiveMatchPlayerModel player = bound(first);
        assertThrows(
                ApiBusinessException.class,
                () -> service.verifyOpening(player, new TeamOpeningDto(team, other.nonce()))
        );
    }

    @Test
    void hmacRejectsTamperedCommitment() {
        List<Integer> team = List.of(1, 2, 3, 4, 5, 6);
        TeamCommitmentService.IssuedTeamCommitment issued = service.issue(team);
        ActiveMatchPlayerModel player = bound(issued);
        player.setTeamCommitment("aa".repeat(32));

        assertThrows(ApiBusinessException.class, () -> service.openSealed(player));
    }

    @Test
    void canonicalPayloadIsStable() {
        String payload = TeamCommitmentService.canonicalPayload(List.of(1, 2, 3, 4, 5, 6), "ab".repeat(32));
        assertTrue(payload.startsWith("v1|1,2,3,4,5,6|"));
        CryptoService crypto = new CryptoService(KEY);
        String c = crypto.sha256Hex(payload);
        assertEquals(64, c.length());
    }

    @Test
    void aesRoundTripPreservesPayload() {
        CryptoService crypto = new CryptoService(KEY);
        String sealed = crypto.encrypt("v1|1,2,3,4,5,6|nonce");
        assertEquals("v1|1,2,3,4,5,6|nonce", crypto.decrypt(sealed));
    }

    private static ActiveMatchPlayerModel bound(TeamCommitmentService.IssuedTeamCommitment issued) {
        ActiveMatchPlayerModel player = new ActiveMatchPlayerModel();
        player.setTeamCommitment(issued.commitment());
        player.setTeamCommitmentMac(issued.mac());
        player.setSealedOpening(issued.sealedOpening());
        return player;
    }
}
