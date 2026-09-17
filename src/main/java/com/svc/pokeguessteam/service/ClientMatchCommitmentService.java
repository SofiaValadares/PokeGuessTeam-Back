package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.TeamOpeningDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientMatchCommitmentService {

    private final ActiveMatchRepository activeMatchRepository;
    private final ActiveMatchRemovalService activeMatchRemovalService;
    private final TeamCommitmentService teamCommitmentService;

    public ClientMatchCommitmentService(
            ActiveMatchRepository activeMatchRepository,
            ActiveMatchRemovalService activeMatchRemovalService,
            TeamCommitmentService teamCommitmentService
    ) {
        this.activeMatchRepository = activeMatchRepository;
        this.activeMatchRemovalService = activeMatchRemovalService;
        this.teamCommitmentService = teamCommitmentService;
    }

    @Transactional
    public CommittedClientMatch commit(
            ProfileModel profile,
            GameModes mode,
            String opponentName,
            List<Integer> hostTeam,
            List<Integer> opponentTeam
    ) {
        ActiveMatchModel match = new ActiveMatchModel();
        match.setProfile(profile);
        match.setGameMode(mode);
        match.setOpponentName(opponentName);
        match.setStatus(MatchStatus.ACTIVE);

        ActiveMatchPlayerModel host = new ActiveMatchPlayerModel();
        host.setSide(MatchPlayerSide.HOST);
        ActiveMatchPlayerModel opponent = new ActiveMatchPlayerModel();
        opponent.setSide(MatchPlayerSide.OPPONENT);
        match.setHostPlayer(host);
        match.setOpponentPlayer(opponent);

        TeamCommitmentService.IssuedTeamCommitment hostIssued = teamCommitmentService.bindPlayer(host, hostTeam);
        TeamCommitmentService.IssuedTeamCommitment opponentIssued =
                teamCommitmentService.bindPlayer(opponent, opponentTeam);

        ActiveMatchModel saved = activeMatchRepository.save(match);
        return new CommittedClientMatch(saved.getId(), hostIssued, opponentIssued);
    }

    /**
     * Fase open: o cliente apresenta os times usados; o servidor abre o AES,
     * verifica SHA-256(C) e HMAC, e só então consome a partida.
     */
    @Transactional
    public OpenedClientMatch verifyAndConsume(
            String profileId,
            GameModes mode,
            String matchId,
            List<Integer> claimedHostTeam,
            List<Integer> claimedOpponentTeam
    ) {
        ActiveMatchModel match = activeMatchRepository.findByIdAndProfile_Id(matchId, profileId)
                .filter(stored -> stored.getGameMode() == mode)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        mode == GameModes.LOCAL
                                ? ErrorCodes.GAME_LOCAL_MATCH_NOT_FOUND
                                : ErrorCodes.GAME_MATCH_NOT_FOUND,
                        mode == GameModes.LOCAL
                                ? MessageKeys.GAME_LOCAL_MATCH_NOT_FOUND
                                : MessageKeys.GAME_MATCH_NOT_FOUND
                ));
        String hostCommitment = match.getHostPlayer().getTeamCommitment();
        String opponentCommitment = match.getOpponentPlayer().getTeamCommitment();
        TeamOpeningDto hostOpening = teamCommitmentService.openClaimedTeam(
                match.getHostPlayer(),
                claimedHostTeam,
                null
        );
        TeamOpeningDto opponentOpening = teamCommitmentService.openClaimedTeam(
                match.getOpponentPlayer(),
                claimedOpponentTeam,
                null
        );
        activeMatchRemovalService.deleteByMatchId(match.getId());
        return new OpenedClientMatch(hostCommitment, opponentCommitment, hostOpening, opponentOpening);
    }

    public record CommittedClientMatch(
            String matchId,
            TeamCommitmentService.IssuedTeamCommitment host,
            TeamCommitmentService.IssuedTeamCommitment opponent
    ) {
    }

    public record OpenedClientMatch(
            String hostCommitment,
            String opponentCommitment,
            TeamOpeningDto hostOpening,
            TeamOpeningDto opponentOpening
    ) {
    }
}
