package com.svc.pokeguessteam.repository;

import com.svc.pokeguessteam.model.PlayerPokemonModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerPokemonRepository extends JpaRepository<PlayerPokemonModel, String> {
    List<PlayerPokemonModel> findByProfile_Id(String profileId);
    Optional<PlayerPokemonModel> findByProfile_IdAndPokemon_Id(String profileId, String pokemonId);
}
