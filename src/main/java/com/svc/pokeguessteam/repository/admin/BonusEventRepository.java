package com.svc.pokeguessteam.repository.admin;

import com.svc.pokeguessteam.model.admin.BonusEventModel;
import com.svc.pokeguessteam.model.enums.BonusEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BonusEventRepository extends JpaRepository<BonusEventModel, String> {

    List<BonusEventModel> findByStatus(BonusEventStatus status);

    Optional<BonusEventModel> findFirstByStatus(BonusEventStatus status);

    boolean existsByStatus(BonusEventStatus status);
}
