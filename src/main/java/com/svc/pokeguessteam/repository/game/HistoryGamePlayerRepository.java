package com.svc.pokeguessteam.repository.game;

import com.svc.pokeguessteam.model.game.HistoryGamePlayerModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistoryGamePlayerRepository extends JpaRepository<HistoryGamePlayerModel, String> {

    @Modifying
    @Query("UPDATE HistoryGamePlayerModel p SET p.profile = null WHERE p.profile.id = :profileId")
    void clearProfileReferences(@Param("profileId") String profileId);
}
