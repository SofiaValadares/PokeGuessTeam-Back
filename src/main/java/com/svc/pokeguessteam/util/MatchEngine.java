package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.GuessOutcome;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MatchEngine {

    private MatchEngine() {
    }

    public record ApplyGuessResult(
            ActiveMatchGuessModel guess,
            GuessOutcome outcome,
            String message
    ) {
    }

    public static boolean hasPlayerCompleted(ActiveMatchModel match, MatchPlayerSide side) {
        ActiveMatchPlayerModel player = match.getPlayer(side);
        ActiveMatchPlayerModel opponent = match.getOpponent(side);
        return !opponent.getTeam().isEmpty()
                && player.getHits().size() >= opponent.getTeam().size();
    }

    public static ApplyGuessResult applyGuess(
            ActiveMatchModel match,
            MatchPlayerSide playerSide,
            PokemonModel guessedPokemon,
            Map<Integer, PokemonModel> pokemonByDex
    ) {
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new IllegalStateException("A partida não está ativa.");
        }
        if (match.getCurrentTurn() != playerSide) {
            throw new IllegalStateException("Não é a vez deste jogador.");
        }

        ActiveMatchPlayerModel currentPlayer = match.getPlayer(playerSide);
        ActiveMatchPlayerModel opponentPlayer = match.getOpponent(playerSide);
        int guessedDex = guessedPokemon.getPokedexNumber();

        List<Integer> matchedDexNumbers = opponentPlayer.getTeam().stream()
                .filter(dex -> dex != null && dex == guessedDex)
                .distinct()
                .toList();

        ActiveMatchGuessModel guessRecord = new ActiveMatchGuessModel();
        guessRecord.setPlayerSide(playerSide);
        guessRecord.setGuessedPokedexNumber(guessedPokemon.getPokedexNumber());
        guessRecord.setExactMatch(!matchedDexNumbers.isEmpty());
        guessRecord.setMatchedPokedexNumbers(new ArrayList<>(matchedDexNumbers));
        match.addGuess(guessRecord);

        if (guessRecord.isExactMatch()) {
            currentPlayer.addHits(matchedDexNumbers);

            if (match.getFinalResponseFor() == playerSide) {
                if (hasPlayerCompleted(match, playerSide)) {
                    finishDraw(match);
                    return new ApplyGuessResult(guessRecord, GuessOutcome.DRAW, "A partida terminou empatada.");
                }
                return new ApplyGuessResult(guessRecord, GuessOutcome.KEEP_TURN, "Acerto na rodada extra; continua a jogar.");
            }

            if (hasPlayerCompleted(match, playerSide)) {
                match.setFinalResponseFor(match.getOpponentSide(playerSide));
                match.setLastCompletingPlayer(playerSide);
                match.setCurrentTurn(match.getFinalResponseFor());
                return new ApplyGuessResult(
                        guessRecord,
                        GuessOutcome.FINAL_RESPONSE,
                        "Adversário ganhou uma rodada extra para tentar o empate."
                );
            }

            return new ApplyGuessResult(guessRecord, GuessOutcome.KEEP_TURN, "Palpite correto; joga novamente.");
        }

        if (match.getFinalResponseFor() == playerSide) {
            match.setStatus(MatchStatus.FINISHED);
            match.setFinishedAt(LocalDateTime.now());
            match.setWinner(match.getLastCompletingPlayer());
            return new ApplyGuessResult(
                    guessRecord,
                    GuessOutcome.FINISHED_AFTER_FINAL_RESPONSE,
                    "Erro na rodada extra; partida terminada."
            );
        }

        currentPlayer.setSkipTurns(currentPlayer.getSkipTurns() + 1);
        advanceTurn(match);
        return new ApplyGuessResult(guessRecord, GuessOutcome.SWITCH_TURN, "Palpite errado; passa a vez.");
    }

    public static void finishBySurrender(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        match.setStatus(MatchStatus.FINISHED);
        match.setFinishedAt(LocalDateTime.now());
        match.setWinner(match.getOpponentSide(surrenderSide));
    }

    public static void finishDraw(ActiveMatchModel match) {
        match.setStatus(MatchStatus.FINISHED);
        match.setFinishedAt(LocalDateTime.now());
        match.setWinner(null);
    }

    public static void advanceTurn(ActiveMatchModel match) {
        MatchPlayerSide nextSide = match.getOpponentSide(match.getCurrentTurn());
        for (int attempts = 0; attempts < 2; attempts++) {
            ActiveMatchPlayerModel nextPlayer = match.getPlayer(nextSide);
            if (nextPlayer.getSkipTurns() > 0) {
                nextPlayer.setSkipTurns(nextPlayer.getSkipTurns() - 1);
                nextSide = match.getOpponentSide(nextSide);
                continue;
            }
            break;
        }
        match.setCurrentTurn(nextSide);
    }

    public static void startActiveMatch(ActiveMatchModel match) {
        MatchPlayerSide starter = Math.random() > 0.5 ? MatchPlayerSide.HOST : MatchPlayerSide.OPPONENT;
        match.setStatus(MatchStatus.ACTIVE);
        match.setStartingPlayer(starter);
        match.setCurrentTurn(starter);
        match.setStartedAt(LocalDateTime.now());
    }

    /**
     * Inicia a partida quando ambos os jogadores enviaram equipa completa (modo amigo).
     */
    public static boolean tryStartIfBothTeamsReady(ActiveMatchModel match, int teamSize) {
        if (match.getStatus() != MatchStatus.SETUP) {
            return false;
        }
        if (match.getHostPlayer().getTeam().size() != teamSize
                || match.getOpponentPlayer().getTeam().size() != teamSize) {
            return false;
        }
        startActiveMatch(match);
        return true;
    }
}
