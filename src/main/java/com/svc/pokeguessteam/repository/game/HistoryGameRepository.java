package com.svc.pokeguessteam.repository.game;

import com.svc.pokeguessteam.model.game.HistoryGameModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistoryGameRepository extends JpaRepository<HistoryGameModel, String> {

    @EntityGraph(attributePaths = {"players", "players.profile", "players.profile.user"})
    @Query("""
            SELECT g FROM HistoryGameModel g
            WHERE EXISTS (
                SELECT 1 FROM HistoryGamePlayerModel p
                WHERE p.game = g AND p.profile.id = :profileId
            )
            """)
    Page<HistoryGameModel> findByProfileId(@Param("profileId") String profileId, Pageable pageable);
}
