package com.svc.pokeguessteam.repository;

import com.svc.pokeguessteam.model.MatchTeamSlotModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchTeamSlotRepository extends JpaRepository<MatchTeamSlotModel, String> {
    List<MatchTeamSlotModel> findByMatch_IdAndOwnerSideOrderBySlotIndexAsc(String matchId, String ownerSide);
    long countByMatch_IdAndOwnerSideAndDiscoveredByOpponentTrue(String matchId, String ownerSide);
}
