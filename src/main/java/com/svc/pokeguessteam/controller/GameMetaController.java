package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.service.GameHistoryService;
import com.svc.pokeguessteam.service.PokedexService;
import com.svc.pokeguessteam.service.ProfileService;
import com.svc.pokeguessteam.util.GameConstants;
import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.util.GameMatchRewards;
import com.svc.pokeguessteam.util.PokeballGachaRules;
import com.svc.pokeguessteam.util.PokemonEvolutionRewards;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
public class GameMetaController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> meta() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "PokeTeamGuess");
        body.put("summary", "Dedução estratégica: duas equipes secretas de 6 Pokémon; vence quem descobrir o time adversário primeiro, "
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
        body.put("matchPlayerSidesLegacyAliases", List.of("USER", "BOT"));
        body.put("matchPlayerSidesByMode", Map.of(
                "BOT", Map.of(
                        "HOST", "Treinador (conta logada)",
                        "OPPONENT", "IA (PokéBot)"
                ),
                "LOCAL", Map.of(
                        "HOST", "Jogador 1 — conta logada (mesmo dispositivo)",
                        "OPPONENT", "Jogador 2 — pass-and-play (nome em localOpponentName)"
                ),
                "FRIEND", Map.of(
                        "HOST", "Anfitrião (criou a sala)",
                        "OPPONENT", "Convidado (entrou com código)"
                )
        ));
        body.put("guessOutcomes", Arrays.stream(com.svc.pokeguessteam.model.enums.GuessOutcome.values()).map(Enum::name).toList());
        body.put("matchRewards", Map.of(
                "friend", Map.of(
                        "win", payoutMeta(GameModes.FRIEND, GameResults.WIN),
                        "loseOrDrawOrDesistence", payoutMeta(
                                GameModes.FRIEND,
                                GameResults.LOSE
                        )
                ),
                "bot", Map.of(
                        "win", payoutMeta(GameModes.BOT, GameResults.WIN),
                        "loseOrDrawOrDesistence", payoutMeta(GameModes.BOT, GameResults.LOSE)
                ),
                "local", Map.of(
                        "win", payoutMeta(GameModes.LOCAL, GameResults.WIN),
                        "loseOrDrawOrDesistence", payoutMeta(GameModes.LOCAL, GameResults.LOSE)
                ),
                "note", "Derrota, empate e desistência usam a mesma tabela que loseOrDrawOrDesistence."
        ));
        body.put("evolutionRewards", PokemonEvolutionRewards.meta());
        body.put("gacha", PokeballGachaRules.meta());
        body.put("activeMatchApi", Map.of(
                "botValidateTeam", "/api/game/bot/match/team",
                "botFinish", "/api/game/bot/match/finish",
                "localSetup", "/api/game/local/match/setup",
                "localFinish", "/api/game/local/match/finish",
                "friend", "/api/game/friend/match"
        ));
        return ResponseEntity.ok(body);
    }

    private static Map<String, Integer> payoutMeta(GameModes mode, GameResults result) {
        GameMatchRewards.MatchRewardPayout p = GameMatchRewards.payout(mode, result);
        return Map.of(
                "trainingTeamXp", p.trainingTeamXp(),
                "pokeBalls", p.pokeBalls(),
                "pokeballFragments", p.pokeballFragments()
        );
    }
}
