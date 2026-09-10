package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.OpponentTeamKnowledgeResponse;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.util.OpponentKnowledgeBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MatchKnowledgeService {

    private final PokemonRepository pokemonRepository;

    public MatchKnowledgeService(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
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
        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        return new OpponentTeamKnowledgeResponse(
                viewerSide,
                OpponentKnowledgeBuilder.buildTeamKnowledge(match, viewerSide, pokemonByDex)
        );
    }

    public List<com.svc.pokeguessteam.dto.game.OpponentSlotKnowledgeDto> buildKnowledge(
            ActiveMatchModel match,
            MatchPlayerSide viewerSide
    ) {
        return OpponentKnowledgeBuilder.buildTeamKnowledge(match, viewerSide, loadPokemonByDex());
    }

    private Map<Integer, PokemonModel> loadPokemonByDex() {
        return pokemonRepository.findAllByOrderByPokedexNumberAsc().stream()
                .collect(Collectors.toMap(PokemonModel::getPokedexNumber, Function.identity(), (a, b) -> a));
    }
}
