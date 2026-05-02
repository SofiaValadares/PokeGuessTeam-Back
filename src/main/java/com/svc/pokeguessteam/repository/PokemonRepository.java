package com.svc.pokeguessteam.repository;

import com.svc.pokeguessteam.model.PokemonModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PokemonRepository extends JpaRepository<PokemonModel, String> {
    Optional<PokemonModel> findByPokedexNumber(int pokedexNumber);

    List<PokemonModel> findAllByOrderByPokedexNumberAsc();

    Optional<PokemonModel> findByNameIgnoreCase(String name);
}
