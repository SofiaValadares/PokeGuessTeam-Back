package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.dto.game.DiscoveredPokemonHintsDto;
import com.svc.pokeguessteam.dto.game.OpponentSlotKnowledgeDto;
import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.enums.PokedexColor;
import com.svc.pokeguessteam.model.enums.PokemonType;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpponentKnowledgeBuilderTest {

    @Test
    void accumulatesHintsAcrossGuessesAndRevealsOnExactHit() {
        PokemonModel charmander = species(4, "Charmander", PokemonType.FIRE, null, 1, PokedexColor.RED,
                0.6, 8.5, EvolutionStage.BASE);
        PokemonModel bulbasaur = species(1, "Bulbasaur", PokemonType.GRASS, PokemonType.POISON, 1,
                PokedexColor.GREEN, 0.7, 6.9, EvolutionStage.BASE);
        PokemonModel squirtle = species(7, "Squirtle", PokemonType.WATER, null, 1, PokedexColor.BLUE,
                0.5, 9.0, EvolutionStage.BASE);
        PokemonModel pikachu = species(25, "Pikachu", PokemonType.ELECTRIC, null, 1, PokedexColor.YELLOW,
                0.4, 6.0, EvolutionStage.BASE);
        PokemonModel rattata = species(19, "Rattata", PokemonType.NORMAL, null, 1, PokedexColor.BROWN,
                0.3, 3.5, EvolutionStage.BASE);
        PokemonModel oddish = species(43, "Oddish", PokemonType.GRASS, PokemonType.POISON, 1,
                PokedexColor.BLUE, 0.5, 5.4, EvolutionStage.BASE);
        PokemonModel weedle = species(13, "Weedle", PokemonType.BUG, PokemonType.POISON, 1,
                PokedexColor.BROWN, 0.3, 3.2, EvolutionStage.BASE);

        Map<Integer, PokemonModel> byDex = Map.of(
                1, bulbasaur,
                4, charmander,
                7, squirtle,
                13, weedle,
                19, rattata,
                25, pikachu,
                43, oddish
        );

        ActiveMatchModel match = new ActiveMatchModel();
        match.setStatus(MatchStatus.ACTIVE);
        match.setCurrentTurn(MatchPlayerSide.HOST);

        ActiveMatchPlayerModel host = player(MatchPlayerSide.HOST);
        host.setTeam(List.of(25, 133, 150, 1, 4, 7));
        ActiveMatchPlayerModel opponent = player(MatchPlayerSide.OPPONENT);
        opponent.setTeam(List.of(1, 43, 13, 4, 25, 19)); // slot 4 = Charmander (fogo), 5–6 sem tipo secundário

        match.setHostPlayer(host);
        match.setOpponentPlayer(opponent);

        addGuess(match, MatchPlayerSide.HOST, charmander);

        List<OpponentSlotKnowledgeDto> afterCharmander = OpponentKnowledgeBuilder.buildTeamKnowledge(
                match, MatchPlayerSide.HOST, byDex
        );

        OpponentSlotKnowledgeDto slot1 = afterCharmander.get(0);
        assertFalse(slot1.adivinhado());
        assertEquals(1, slot1.informacoes().geracao());
        assertEquals(EvolutionStage.BASE.name(), slot1.informacoes().estagioEvolutivo());

        OpponentSlotKnowledgeDto slot3Fire = afterCharmander.get(3);
        assertEquals(PokemonType.FIRE.name(), slot3Fire.informacoes().tipoPrimario());

        OpponentSlotKnowledgeDto slot5 = afterCharmander.get(4);
        assertEquals(DiscoveredPokemonHintsDto.TIPO_SECUNDARIO_NENHUM, slot5.informacoes().tipoSecundario());
        OpponentSlotKnowledgeDto slot6 = afterCharmander.get(5);
        assertEquals(DiscoveredPokemonHintsDto.TIPO_SECUNDARIO_NENHUM, slot6.informacoes().tipoSecundario());

        addGuess(match, MatchPlayerSide.HOST, bulbasaur);
        host.getHits().add(1);

        List<OpponentSlotKnowledgeDto> afterBulbasaur = OpponentKnowledgeBuilder.buildTeamKnowledge(
                match, MatchPlayerSide.HOST, byDex
        );

        OpponentSlotKnowledgeDto revealed = afterBulbasaur.get(0);
        assertTrue(revealed.adivinhado());
        assertEquals("Bulbasaur", revealed.informacoes().nome());
        assertEquals(1, revealed.informacoes().numeroPokedex());
        assertEquals(PokemonType.GRASS.name(), revealed.informacoes().tipoPrimario());
        assertEquals(PokemonType.POISON.name(), revealed.informacoes().tipoSecundario());

        OpponentSlotKnowledgeDto slot2After = afterBulbasaur.get(1);
        assertEquals(PokemonType.GRASS.name(), slot2After.informacoes().tipoPrimario());
        assertNull(slot2After.informacoes().nome());

        OpponentSlotKnowledgeDto slot3Poison = afterBulbasaur.get(2);
        assertEquals(PokemonType.POISON.name(), slot3Poison.informacoes().tipoSecundario());

        OpponentSlotKnowledgeDto slot4Fire = afterBulbasaur.get(3);
        assertEquals(PokemonType.FIRE.name(), slot4Fire.informacoes().tipoPrimario());
        assertNull(slot4Fire.informacoes().nome());
    }

    private static void addGuess(ActiveMatchModel match, MatchPlayerSide side, PokemonModel guessed) {
        ActiveMatchGuessModel g = new ActiveMatchGuessModel();
        g.setPlayerSide(side);
        g.setGuessedPokedexNumber(guessed.getPokedexNumber());
        match.addGuess(g);
    }

    private static ActiveMatchPlayerModel player(MatchPlayerSide side) {
        ActiveMatchPlayerModel p = new ActiveMatchPlayerModel();
        p.setSide(side);
        return p;
    }

    private static PokemonModel species(
            int dex,
            String name,
            PokemonType primary,
            PokemonType secondary,
            int generation,
            PokedexColor color,
            double height,
            double weight,
            EvolutionStage stage
    ) {
        PokemonModel p = new PokemonModel();
        p.setPokedexNumber(dex);
        p.setName(name);
        p.setPrimaryType(primary);
        p.setSecondaryType(secondary);
        p.setGeneration(generation);
        p.setColor(color);
        p.setHeightM(height);
        p.setWeightKg(weight);
        p.setEvolutionStage(stage);
        return p;
    }
}
