package com.svc.pokeguessteam.repository.pokemon;

import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.enums.PokemonRarity;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PokemonRepository extends JpaRepository<PokemonModel, String> {
    Optional<PokemonModel> findByPokedexNumber(int pokedexNumber);

    List<PokemonModel> findByPokedexNumberIn(Iterable<Integer> pokedexNumbers);

    List<PokemonModel> findAllByOrderByPokedexNumberAsc();

    @Query("SELECT DISTINCT p FROM PokemonModel p JOIN FETCH p.evolutionLine ORDER BY p.pokedexNumber ASC")
    List<PokemonModel> findAllWithEvolutionLineOrderByPokedexNumberAsc();

    List<PokemonModel> findByEvolutionLine_Rarity(PokemonRarity rarity);

    List<PokemonModel> findByEvolutionLine_RarityAndEvolutionStage(PokemonRarity rarity, EvolutionStage evolutionStage);

    List<PokemonModel> findByEvolutionLine_LineKey(Integer lineKey);

    Optional<PokemonModel> findByNameIgnoreCase(String name);

    List<PokemonModel> findByNameContainingIgnoreCaseOrderByPokedexNumberAsc(String name, Pageable pageable);
}
