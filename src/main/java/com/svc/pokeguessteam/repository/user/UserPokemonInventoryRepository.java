package com.svc.pokeguessteam.repository.user;

import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPokemonInventoryRepository extends JpaRepository<UserPokemonInventoryModel, String> {

    List<UserPokemonInventoryModel> findByProfile_IdOrderByEvolutionLine_LineKeyAsc(String profileId);

    Optional<UserPokemonInventoryModel> findByProfile_IdAndEvolutionLine_LineKey(String profileId, Integer lineKey);
}
