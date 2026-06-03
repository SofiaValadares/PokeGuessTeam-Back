package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.service.GameHistoryService;
import com.svc.pokeguessteam.service.PokedexService;
import com.svc.pokeguessteam.service.ProfileService;
import com.svc.pokeguessteam.util.GameConstants;
import com.svc.pokeguessteam.util.GameMatchRewards;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
public class GameMetaController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> meta() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "PokeTeamGuess");
        body.put("summary", "Dedução estratégica: duas equipas secretas de 6 Pokémon; vence quem descobrir o time adversário primeiro, "
                + "com pistas por tipagem, geração, cor, altura e peso.");
        body.put("pokedexDefaultPageSize", PokedexService.DEFAULT_PAGE_SIZE);
        body.put("pokedexMaxPageSize", PokedexService.MAX_PAGE_SIZE);
        body.put("pokemonInventoryDefaultPageSize", ProfileService.PokemonPcConstants.DEFAULT_PAGE_SIZE);
        body.put("pokemonInventoryMaxPageSize", ProfileService.PokemonPcConstants.MAX_PAGE_SIZE);
        body.put("pcPageSize", ProfileService.PokemonPcConstants.DEFAULT_PAGE_SIZE);
        body.put("pcMaxPageSize", ProfileService.PokemonPcConstants.MAX_PAGE_SIZE);
        body.put("fragmentsPerPokeBall", ProfileService.FRAGMENTS_PER_POKE_BALL);
        body.put("teamSize", GameConstants.TEAM_SIZE);
        body.put("maxCorrectGuesses", GameConstants.MAX_CORRECT_GUESSES);
        body.put("gameModes", Arrays.stream(GameModes.values()).map(Enum::name).toList());
        body.put("gameResults", Arrays.stream(com.svc.pokeguessteam.model.enums.GameResults.values()).map(Enum::name).toList());
        body.put("gameHistoryDefaultPageSize", GameHistoryService.DEFAULT_PAGE_SIZE);
        body.put("gameHistoryMaxPageSize", GameHistoryService.MAX_PAGE_SIZE);
        body.put("friendJoinCodeLength", GameConstants.FRIEND_JOIN_CODE_LENGTH);
        body.put("matchStatuses", Arrays.stream(com.svc.pokeguessteam.model.enums.MatchStatus.values()).map(Enum::name).toList());
        body.put("matchPlayerSides", Arrays.stream(com.svc.pokeguessteam.model.enums.MatchPlayerSide.values()).map(Enum::name).toList());
        body.put("guessOutcomes", Arrays.stream(com.svc.pokeguessteam.model.enums.GuessOutcome.values()).map(Enum::name).toList());
        body.put("matchRewards", Map.of(
                "winXp", GameMatchRewards.xpForResult(com.svc.pokeguessteam.model.enums.GameResults.WIN),
                "drawXp", GameMatchRewards.xpForResult(com.svc.pokeguessteam.model.enums.GameResults.DRAW),
                "loseXp", GameMatchRewards.xpForResult(com.svc.pokeguessteam.model.enums.GameResults.LOSE),
                "desistenceXp", GameMatchRewards.xpForResult(com.svc.pokeguessteam.model.enums.GameResults.DESISTENCE)
        ));
        body.put("activeMatchApi", Map.of(
                "bot", "/api/game/bot/match",
                "local", "/api/game/local/match",
                "friend", "/api/game/friend/match"
        ));
        return ResponseEntity.ok(body);
    }
}
