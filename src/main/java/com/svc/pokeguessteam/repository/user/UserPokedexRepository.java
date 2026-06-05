package com.svc.pokeguessteam.repository.user;

import com.svc.pokeguessteam.model.user.UserPokedexModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserPokedexRepository extends JpaRepository<UserPokedexModel, String> {

    Optional<UserPokedexModel> findByProfile_IdAndPokemon_PokedexNumber(String profileId, Integer pokedexNumber);

    @Query("SELECT u.pokemon.pokedexNumber FROM UserPokedexModel u "
            + "WHERE u.profile.id = :profileId AND u.registered = true")
    Set<Integer> findRegisteredPokedexNumbersByProfile_Id(@Param("profileId") String profileId);

    List<UserPokedexModel> findByProfile_IdAndRegisteredTrueOrderByPokemon_PokedexNumberAsc(String profileId);

    void deleteByProfile_Id(String profileId);
}
