package com.svc.pokeguessteam.repository;

import com.svc.pokeguessteam.model.MatchGuessLogModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchGuessLogRepository extends JpaRepository<MatchGuessLogModel, String> {
    List<MatchGuessLogModel> findByMatch_IdOrderByCreatedAtAsc(String matchId);
}
