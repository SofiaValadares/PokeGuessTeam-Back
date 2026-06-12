package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.PokeballType;
import com.svc.pokeguessteam.model.enums.PokemonRarity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras do gacha (GDD: pisos de raridade + chances extras na Pokébola comum).
 * A rolagem efetiva está em {@link com.svc.pokeguessteam.service.PokeballDrawService}.
 */
public final class PokeballGachaRules {

    private PokeballGachaRules() {
    }

    public static Map<String, Object> meta() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("endpoint", "/api/pokemon/draw");
        body.put("requestBody", Map.of("pokeballType", "POKE_BALL | GREAT_BALL | ULTRA_BALL | MASTER_BALL"));
        body.put("consumesOneBallPerDraw", true);
        body.put("drawsBaseFormOnly", true);
        body.put(
                "drawPoolNote",
                "Cada sorteio devolve a primeira forma (estágio BASE) de uma linha evolutiva com a raridade rolada."
        );
        body.put("rarities", List.of("COMMON", "RARE", "LEGENDARY", "MYTHICAL"));
        body.put("balls", Map.of(
                PokeballType.POKE_BALL.name(), pokeBallRules(),
                PokeballType.GREAT_BALL.name(), greatBallRules(),
                PokeballType.ULTRA_BALL.name(), ultraBallRules(),
                PokeballType.MASTER_BALL.name(), masterBallRules()
        ));
        return body;
    }

    private static Map<String, Object> pokeBallRules() {
        return Map.of(
                "summary", "Sorteio aleatório; piso Comum.",
                "defaultRarity", PokemonRarity.COMMON.name(),
                "upgradeRolls", List.of(
                        roll(PokemonRarity.RARE, 10),
                        roll(PokemonRarity.LEGENDARY, 100),
                        roll(PokemonRarity.MYTHICAL, 500)
                )
        );
    }

    private static Map<String, Object> greatBallRules() {
        return Map.of(
                "summary", "Garante Raro ou superior (GDD).",
                "defaultRarity", PokemonRarity.RARE.name(),
                "upgradeRolls", List.of(
                        roll(PokemonRarity.LEGENDARY, 50),
                        roll(PokemonRarity.MYTHICAL, 300)
                )
        );
    }

    private static Map<String, Object> ultraBallRules() {
        return Map.of(
                "summary", "Garante Lendário ou superior (GDD).",
                "defaultRarity", PokemonRarity.LEGENDARY.name(),
                "upgradeRolls", List.of(
                        roll(PokemonRarity.MYTHICAL, 150)
                )
        );
    }

    private static Map<String, Object> masterBallRules() {
        return Map.of(
                "summary", "Mítico garantido (GDD).",
                "defaultRarity", PokemonRarity.MYTHICAL.name(),
                "upgradeRolls", List.of()
        );
    }

    private static Map<String, Object> roll(PokemonRarity rarity, int oneIn) {
        return Map.of(
                "rarity", rarity.name(),
                "chance", "1/" + oneIn
        );
    }
}
