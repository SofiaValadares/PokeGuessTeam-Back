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

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "userPlayer", "userPlayer.team",
            "botPlayer", "botPlayer.team",
            "guesses", "profile", "profile.user", "guestProfile", "guestProfile.user"
    })
    Optional<ActiveMatchModel> findByIdAndProfile_Id(String id, String profileId);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "userPlayer", "userPlayer.team",
            "botPlayer", "botPlayer.team",
            "guesses", "profile", "profile.user", "guestProfile", "guestProfile.user"
    })
    @Query("""
            SELECT m FROM ActiveMatchModel m
            WHERE m.profile.id = :profileId
              AND m.gameMode = :gameMode
              AND m.status <> :finished
            ORDER BY m.createdAt DESC
            """)
    List<ActiveMatchModel> findAllActiveByProfileIdAndGameModeOrderByCreatedAtDesc(
            @Param("profileId") String profileId,
            @Param("gameMode") GameModes gameMode,
            @Param("finished") MatchStatus finished
    );

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "userPlayer", "userPlayer.team",
            "botPlayer", "botPlayer.team",
            "guesses", "profile", "profile.user", "guestProfile", "guestProfile.user"
    })
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

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "userPlayer", "userPlayer.team",
            "botPlayer", "botPlayer.team",
            "guesses", "profile", "profile.user", "guestProfile", "guestProfile.user"
    })
    Optional<ActiveMatchModel> findByJoinCodeAndGameModeAndStatusNot(
            String joinCode,
            GameModes gameMode,
            MatchStatus status
    );

    List<ActiveMatchModel> findByProfile_IdAndStatusNot(String profileId, MatchStatus status);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "userPlayer", "userPlayer.team",
            "botPlayer", "botPlayer.team",
            "guesses", "profile", "profile.user", "guestProfile", "guestProfile.user"
    })
    @Query("SELECT m FROM ActiveMatchModel m WHERE m.id = :matchId")
    Optional<ActiveMatchModel> findDetailedById(@Param("matchId") String matchId);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "userPlayer", "userPlayer.team",
            "botPlayer", "botPlayer.team",
            "guesses", "profile", "profile.user", "guestProfile", "guestProfile.user"
    })
    @Query("""
            SELECT m FROM ActiveMatchModel m
            WHERE m.status <> :finished
              AND (m.profile.id = :profileId OR m.guestProfile.id = :profileId)
            ORDER BY m.createdAt DESC
            """)
    List<ActiveMatchModel> findAllUnfinishedForProfileOrderByCreatedAtDesc(
            @Param("profileId") String profileId,
            @Param("finished") MatchStatus finished
    );

    @Query("SELECT m FROM ActiveMatchModel m WHERE m.guestProfile.id = :profileId")
    java.util.List<ActiveMatchModel> findByGuestProfile_Id(@Param("profileId") String profileId);
}
