package com.svc.pokeguessteam.repository.game;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActiveMatchRepository extends JpaRepository<ActiveMatchModel, String> {

    @EntityGraph(attributePaths = {"userPlayer", "botPlayer", "guesses", "profile", "profile.user", "guestProfile", "guestProfile.user"})
    Optional<ActiveMatchModel> findByIdAndProfile_Id(String id, String profileId);

    @EntityGraph(attributePaths = {"userPlayer", "botPlayer", "guesses", "profile", "profile.user", "guestProfile", "guestProfile.user"})
    @Query("""
            SELECT m FROM ActiveMatchModel m
            WHERE m.profile.id = :profileId
              AND m.gameMode = :gameMode
              AND m.status <> :finished
            """)
    Optional<ActiveMatchModel> findActiveByProfileIdAndGameMode(
            @Param("profileId") String profileId,
            @Param("gameMode") GameModes gameMode,
            @Param("finished") MatchStatus finished
    );

    @EntityGraph(attributePaths = {"userPlayer", "botPlayer", "guesses", "profile", "profile.user", "guestProfile", "guestProfile.user"})
    @Query("""
            SELECT m FROM ActiveMatchModel m
            WHERE m.gameMode = :gameMode
              AND m.status <> :finished
              AND (m.profile.id = :profileId OR m.guestProfile.id = :profileId)
            """)
    Optional<ActiveMatchModel> findActiveFriendMatchForProfile(
            @Param("profileId") String profileId,
            @Param("gameMode") GameModes gameMode,
            @Param("finished") MatchStatus finished
    );

    @EntityGraph(attributePaths = {"userPlayer", "botPlayer", "guesses", "profile", "profile.user", "guestProfile", "guestProfile.user"})
    Optional<ActiveMatchModel> findByJoinCodeAndGameModeAndStatusNot(
            String joinCode,
            GameModes gameMode,
            MatchStatus status
    );

    List<ActiveMatchModel> findByProfile_IdAndStatusNot(String profileId, MatchStatus status);
}
