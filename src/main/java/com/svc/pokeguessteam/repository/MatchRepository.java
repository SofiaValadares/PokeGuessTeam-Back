package com.svc.pokeguessteam.repository;

import com.svc.pokeguessteam.model.MatchModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<MatchModel, String> {
}
