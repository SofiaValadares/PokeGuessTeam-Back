package com.svc.pokeguessteam.model.game;

import com.svc.pokeguessteam.model.enums.GameModes;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Histórico de partida concluída.
 * <p>
 * {@code players}: 2 jogadores por partida (utilizador + adversário/bot/convidado).
 * {@code opponentName}: opcional; preenchido apenas no modo {@link GameModes#LOCAL}.
 */
@Entity
@Table(name = "TB_GAME_HISTORY")
public class HistoryGameModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_GAME_HISTORY_ID", nullable = false, updatable = false, length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "GAME_MODE", nullable = false, length = 20)
    private GameModes gameMode;

    @Column(name = "PLAYED_AT", nullable = false, updatable = false)
    private LocalDateTime playedAt;

    /** Nome do adversário local; {@code null} fora do modo {@link GameModes#LOCAL}. */
    @Column(name = "OPPONENT_NAME", length = 120)
    private String opponentName;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoryGamePlayerModel> players = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        if (playedAt == null) {
            playedAt = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public GameModes getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameModes gameMode) {
        this.gameMode = gameMode;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(LocalDateTime playedAt) {
        this.playedAt = playedAt;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public List<HistoryGamePlayerModel> getPlayers() {
        return players;
    }

    public void setPlayers(List<HistoryGamePlayerModel> players) {
        this.players = players != null ? players : new ArrayList<>();
    }

    public void addPlayer(HistoryGamePlayerModel player) {
        if (player == null) {
            return;
        }
        player.setGame(this);
        if (!players.contains(player)) {
            players.add(player);
        }
    }
}
