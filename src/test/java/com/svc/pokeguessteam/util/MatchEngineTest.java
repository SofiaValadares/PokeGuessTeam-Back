package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.GuessOutcome;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.enums.PokedexColor;
import com.svc.pokeguessteam.model.enums.PokemonType;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchEngineTest {

    private ActiveMatchModel match;
    private Map<Integer, PokemonModel> byDex;

    @BeforeEach
    void setUp() {
        match = new ActiveMatchModel();
        match.setStatus(MatchStatus.ACTIVE);
        match.setCurrentTurn(MatchPlayerSide.HOST);

        ActiveMatchPlayerModel user = new ActiveMatchPlayerModel();
        user.setSide(MatchPlayerSide.HOST);
        user.setTeam(List.of(1, 4, 7, 25, 133, 150));
        match.setHostPlayer(user);

        ActiveMatchPlayerModel bot = new ActiveMatchPlayerModel();
        bot.setSide(MatchPlayerSide.OPPONENT);
        bot.setTeam(List.of(2, 5, 8, 26, 134, 151));
        match.setOpponentPlayer(bot);

        byDex = Map.of(
                25, species(25, "Pikachu"),
                26, species(26, "Raichu"),
                2, species(2, "Ivysaur")
        );
    }

    @Test
    void correctGuessByPokedexNumberEvenWhenNameDiffers() {
        PokemonModel bulbasaurEn = species(1, "Bulbasaur");
        byDex = Map.of(1, bulbasaurEn, 26, species(26, "Raichu"));
        match.getOpponentPlayer().setTeam(List.of(1, 2, 3, 4, 5, 6));

        PokemonModel guessAlias = species(1, "Bulbasauro");
        var result = MatchEngine.applyGuess(match, MatchPlayerSide.HOST, guessAlias, byDex);

        assertTrue(result.guess().isExactMatch());
        assertEquals(GuessOutcome.KEEP_TURN, result.outcome());
        assertTrue(match.getHostPlayer().getHits().contains(1));
    }

    @Test
    void correctGuessKeepsTurn() {
        var result = MatchEngine.applyGuess(match, MatchPlayerSide.HOST, byDex.get(26), byDex);
        assertTrue(result.guess().isExactMatch());
        assertEquals(GuessOutcome.KEEP_TURN, result.outcome());
        assertEquals(MatchPlayerSide.HOST, match.getCurrentTurn());
        assertEquals(1, match.getHostPlayer().getHits().size());
    }

    @Test
    void wrongGuessSwitchesTurn() {
        var result = MatchEngine.applyGuess(match, MatchPlayerSide.HOST, byDex.get(25), byDex);
        assertEquals(GuessOutcome.SWITCH_TURN, result.outcome());
        assertEquals(MatchPlayerSide.OPPONENT, match.getCurrentTurn());
    }

    private static PokemonModel species(int dex, String name) {
        PokemonModel p = new PokemonModel();
        p.setPokedexNumber(dex);
        p.setName(name);
        p.setPrimaryType(PokemonType.NORMAL);
        p.setGeneration(1);
        p.setColor(PokedexColor.YELLOW);
        p.setHeightM(1.0);
        p.setWeightKg(10.0);
        return p;
    }
}
