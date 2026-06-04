package com.svc.pokeguessteam.model.game;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchPlayerSideConverter;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.user.ProfileModel;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "TB_ACTIVE_MATCHES")
public class ActiveMatchModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_ACTIVE_MATCH_ID", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_PROFILE_ID", nullable = false)
    private ProfileModel profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_GUEST_PROFILE_ID")
    private ProfileModel guestProfile;

    @Column(name = "JOIN_CODE", unique = true, length = 10)
    private String joinCode;

    @Column(name = "OPPONENT_NAME", length = 120)
    private String opponentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "GAME_MODE", nullable = false, length = 20)
    private GameModes gameMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "MATCH_STATUS", nullable = false, length = 20)
    private MatchStatus status;

    @Convert(converter = MatchPlayerSideConverter.class)
    @Column(name = "CURRENT_TURN", length = 10)
    private MatchPlayerSide currentTurn;

    @Convert(converter = MatchPlayerSideConverter.class)
    @Column(name = "STARTING_PLAYER", length = 10)
    private MatchPlayerSide startingPlayer;

    @Convert(converter = MatchPlayerSideConverter.class)
    @Column(name = "FINAL_RESPONSE_FOR", length = 10)
    private MatchPlayerSide finalResponseFor;

    @Convert(converter = MatchPlayerSideConverter.class)
    @Column(name = "LAST_COMPLETING_PLAYER", length = 10)
    private MatchPlayerSide lastCompletingPlayer;

    @Convert(converter = MatchPlayerSideConverter.class)
    @Column(name = "WINNER", length = 10)
    private MatchPlayerSide winner;

    @Column(name = "STARTED_AT")
    private LocalDateTime startedAt;

    @Column(name = "FINISHED_AT")
    private LocalDateTime finishedAt;

    @Column(name = "TURN_DEADLINE_AT")
    private LocalDateTime turnDeadlineAt;

    @Column(name = "TURN_SEQUENCE", nullable = false)
    private long turnSequence;

    /** Lado substituído por IA após 3 timeouts (modo amigo). */
    @Convert(converter = MatchPlayerSideConverter.class)
    @Column(name = "BOT_REPLACEMENT_SIDE", length = 10)
    private MatchPlayerSide botReplacementSide;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private ActiveMatchPlayerModel userPlayer;

    @OneToOne(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private ActiveMatchPlayerModel botPlayer;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActiveMatchGuessModel> guesses = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isSideControlledByBot(MatchPlayerSide side) {
        return botReplacementSide != null && botReplacementSide == side;
    }

    public boolean isHumanTurn(MatchPlayerSide side) {
        return !isSideControlledByBot(side);
    }

    public String getId() {
        return id;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public void setProfile(ProfileModel profile) {
        this.profile = profile;
    }

    public ProfileModel getGuestProfile() {
        return guestProfile;
    }

    public void setGuestProfile(ProfileModel guestProfile) {
        this.guestProfile = guestProfile;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public GameModes getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameModes gameMode) {
        this.gameMode = gameMode;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public MatchPlayerSide getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(MatchPlayerSide currentTurn) {
        this.currentTurn = currentTurn;
    }

    public MatchPlayerSide getStartingPlayer() {
        return startingPlayer;
    }

    public void setStartingPlayer(MatchPlayerSide startingPlayer) {
        this.startingPlayer = startingPlayer;
    }

    public MatchPlayerSide getFinalResponseFor() {
        return finalResponseFor;
    }

    public void setFinalResponseFor(MatchPlayerSide finalResponseFor) {
        this.finalResponseFor = finalResponseFor;
    }

    public MatchPlayerSide getLastCompletingPlayer() {
        return lastCompletingPlayer;
    }

    public void setLastCompletingPlayer(MatchPlayerSide lastCompletingPlayer) {
        this.lastCompletingPlayer = lastCompletingPlayer;
    }

    public MatchPlayerSide getWinner() {
        return winner;
    }

    public void setWinner(MatchPlayerSide winner) {
        this.winner = winner;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getTurnDeadlineAt() {
        return turnDeadlineAt;
    }

    public void setTurnDeadlineAt(LocalDateTime turnDeadlineAt) {
        this.turnDeadlineAt = turnDeadlineAt;
    }

    public long getTurnSequence() {
        return turnSequence;
    }

    public void setTurnSequence(long turnSequence) {
        this.turnSequence = turnSequence;
    }

    public MatchPlayerSide getBotReplacementSide() {
        return botReplacementSide;
    }

    public void setBotReplacementSide(MatchPlayerSide botReplacementSide) {
        this.botReplacementSide = botReplacementSide;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Jogador {@link MatchPlayerSide#HOST} (conta / anfitrião). */
    public ActiveMatchPlayerModel getHostPlayer() {
        return userPlayer;
    }

    public void setHostPlayer(ActiveMatchPlayerModel hostPlayer) {
        this.userPlayer = hostPlayer;
        if (hostPlayer != null) {
            hostPlayer.setMatch(this);
        }
    }

    /** Jogador {@link MatchPlayerSide#OPPONENT} (IA, 2.º humano local ou convidado). */
    public ActiveMatchPlayerModel getOpponentPlayer() {
        return botPlayer;
    }

    public void setOpponentPlayer(ActiveMatchPlayerModel opponentPlayer) {
        this.botPlayer = opponentPlayer;
        if (opponentPlayer != null) {
            opponentPlayer.setMatch(this);
        }
    }

    public List<ActiveMatchGuessModel> getGuesses() {
        return guesses;
    }

    public ActiveMatchPlayerModel getPlayer(MatchPlayerSide side) {
        return side == MatchPlayerSide.HOST ? userPlayer : botPlayer;
    }

    public ActiveMatchPlayerModel getOpponent(MatchPlayerSide side) {
        return side == MatchPlayerSide.HOST ? botPlayer : userPlayer;
    }

    public MatchPlayerSide getOpponentSide(MatchPlayerSide side) {
        return side == MatchPlayerSide.HOST ? MatchPlayerSide.OPPONENT : MatchPlayerSide.HOST;
    }

    public void addGuess(ActiveMatchGuessModel guess) {
        guess.setMatch(this);
        guesses.add(0, guess);
    }
}
