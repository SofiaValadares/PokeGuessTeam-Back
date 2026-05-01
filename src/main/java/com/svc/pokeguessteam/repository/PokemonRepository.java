package com.svc.pokeguessteam.repository;

import com.svc.pokeguessteam.model.PokemonModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PokemonRepository extends JpaRepository<PokemonModel, String> {
    Optional<PokemonModel> findByPokedexNumber(Integer pokedexNumber);
    Optional<PokemonModel> findByNameIgnoreCase(String name);
}
