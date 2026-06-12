package com.svc.pokeguessteam.model.game;

import com.svc.pokeguessteam.dto.game.GameHistoryOpponentSlotDto;
import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.model.user.ProfileModel;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Jogador num registo de histórico de partida (2 por partida).
 */
@Entity
@Table(
        name = "TB_GAME_HISTORY_PLAYERS",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_GAME_HISTORY_PLAYER_SLOT",
                columnNames = {"FK_GAME_HISTORY_ID", "PLAYER_SLOT"}
        )
)
public class HistoryGamePlayerModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_GAME_HISTORY_PLAYER_ID", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_GAME_HISTORY_ID", nullable = false)
    private HistoryGameModel game;

    /** Posição na partida: 1 ou 2. */
    @Column(name = "PLAYER_SLOT", nullable = false)
    private Integer slot;

    /**
     * Perfil do jogador registado. Pode ser {@code null} no modo local (convidado)
     * ou no modo bot (adversário artificial).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_PROFILE_ID")
    private ProfileModel profile;

    @Column(name = "CORRECT_GUESSES", nullable = false)
    private Integer correctGuesses;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESULT", nullable = false, length = 10)
    private GameResults result;

    @Column(name = "TURN_TIMEOUT_PENALTIES", nullable = false)
    private int turnTimeoutPenalties;

    /** Time adversário completo ao terminar a partida (perspetiva deste jogador). */
    @Convert(converter = OpponentTeamSnapshotConverter.class)
    @Column(name = "OPPONENT_TEAM_SNAPSHOT", columnDefinition = "TEXT")
    private List<GameHistoryOpponentSlotDto> opponentTeamSnapshot = new ArrayList<>();

    public String getId() {
        return id;
    }

    public HistoryGameModel getGame() {
        return game;
    }

    public void setGame(HistoryGameModel game) {
        this.game = game;
    }

    public Integer getSlot() {
        return slot;
    }

    public void setSlot(Integer slot) {
        this.slot = slot;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public void setProfile(ProfileModel profile) {
        this.profile = profile;
    }

    public Integer getCorrectGuesses() {
        return correctGuesses;
    }

    public void setCorrectGuesses(Integer correctGuesses) {
        this.correctGuesses = correctGuesses;
    }

    public GameResults getResult() {
        return result;
    }

    public void setResult(GameResults result) {
        this.result = result;
    }

    public int getTurnTimeoutPenalties() {
        return turnTimeoutPenalties;
    }

    public void setTurnTimeoutPenalties(int turnTimeoutPenalties) {
        this.turnTimeoutPenalties = turnTimeoutPenalties;
    }

    public List<GameHistoryOpponentSlotDto> getOpponentTeamSnapshot() {
        return opponentTeamSnapshot != null ? List.copyOf(opponentTeamSnapshot) : List.of();
    }

    public void setOpponentTeamSnapshot(List<GameHistoryOpponentSlotDto> opponentTeamSnapshot) {
        this.opponentTeamSnapshot = opponentTeamSnapshot != null
                ? new ArrayList<>(opponentTeamSnapshot)
                : new ArrayList<>();
    }
}
