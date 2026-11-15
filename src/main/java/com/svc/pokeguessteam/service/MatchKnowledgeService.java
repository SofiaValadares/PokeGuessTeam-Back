package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.OpponentTeamKnowledgeResponse;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.util.OpponentKnowledgeBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MatchKnowledgeService {

    private final NationalPokedexCatalog nationalPokedexCatalog;

    public MatchKnowledgeService(NationalPokedexCatalog nationalPokedexCatalog) {
        this.nationalPokedexCatalog = nationalPokedexCatalog;
    }

    /**
     * Conhecimento do jogador cuja vez está activa (início de turno).
     */
    public OpponentTeamKnowledgeResponse getOpponentKnowledgeForCurrentTurn(ActiveMatchModel match) {
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }
        MatchPlayerSide viewerSide = match.getCurrentTurn();
        if (viewerSide == null) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_INVALID_PHASE,
                    MessageKeys.GAME_MATCH_INVALID_PHASE
            );
        }
        return new OpponentTeamKnowledgeResponse(
                viewerSide,
                buildKnowledge(match, viewerSide)
        );
    }

    public List<com.svc.pokeguessteam.dto.game.OpponentSlotKnowledgeDto> buildKnowledge(
            ActiveMatchModel match,
            MatchPlayerSide viewerSide
    ) {
        return buildKnowledge(match, viewerSide, nationalPokedexCatalog.speciesByDex());
    }

    public List<com.svc.pokeguessteam.dto.game.OpponentSlotKnowledgeDto> buildKnowledge(
            ActiveMatchModel match,
            MatchPlayerSide viewerSide,
            Map<Integer, PokemonModel> pokemonByDex
    ) {
        return OpponentKnowledgeBuilder.buildTeamKnowledge(match, viewerSide, pokemonByDex);
    }
}
