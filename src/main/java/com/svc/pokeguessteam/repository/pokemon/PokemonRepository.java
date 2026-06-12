package com.svc.pokeguessteam.repository.pokemon;

import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.enums.PokemonRarity;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PokemonRepository extends JpaRepository<PokemonModel, String> {
    Optional<PokemonModel> findByPokedexNumber(int pokedexNumber);

    List<PokemonModel> findByPokedexNumberIn(Iterable<Integer> pokedexNumbers);

    List<PokemonModel> findAllByOrderByPokedexNumberAsc();

    List<PokemonModel> findByEvolutionLine_Rarity(PokemonRarity rarity);

    /** Pool do gacha: primeira forma (BASE) de linhas com a raridade sorteada. */
    List<PokemonModel> findByEvolutionLine_RarityAndEvolutionStage(
            PokemonRarity rarity,
            EvolutionStage evolutionStage
    );

    List<PokemonModel> findByEvolutionLine_LineKey(Integer lineKey);

    Optional<PokemonModel> findByNameIgnoreCase(String name);

    List<PokemonModel> findByNameContainingIgnoreCaseOrderByPokedexNumberAsc(String name, Pageable pageable);

    Page<PokemonModel> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("""
            SELECT p FROM PokemonModel p
            WHERE CAST(p.pokedexNumber AS string) LIKE CONCAT(:prefix, '%')
            ORDER BY p.pokedexNumber ASC
            """)
    Page<PokemonModel> searchByPokedexNumberPrefix(@Param("prefix") String prefix, Pageable pageable);
}
