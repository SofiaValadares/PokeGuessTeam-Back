package com.svc.pokeguessteam.repository.user;

import com.svc.pokeguessteam.model.enums.PokeballType;
import com.svc.pokeguessteam.model.user.ProfileInventoryItemModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileInventoryItemRepository extends JpaRepository<ProfileInventoryItemModel, String> {

    List<ProfileInventoryItemModel> findByProfile_Id(String profileId);

    Optional<ProfileInventoryItemModel> findByProfile_IdAndPokeballType(String profileId, PokeballType pokeballType);
}
