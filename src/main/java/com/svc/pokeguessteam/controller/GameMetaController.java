package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.service.GameHistoryService;
import com.svc.pokeguessteam.service.PokedexService;
import com.svc.pokeguessteam.service.ProfileService;
import com.svc.pokeguessteam.util.GameConstants;
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
        return ResponseEntity.ok(body);
    }
}
