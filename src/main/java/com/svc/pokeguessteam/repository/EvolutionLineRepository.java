package com.svc.pokeguessteam.repository;

import com.svc.pokeguessteam.model.EvolutionLineModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvolutionLineRepository extends JpaRepository<EvolutionLineModel, Integer> {
}
