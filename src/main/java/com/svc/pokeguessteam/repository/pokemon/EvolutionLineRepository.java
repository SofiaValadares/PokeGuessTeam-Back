package com.svc.pokeguessteam.repository.pokemon;

import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvolutionLineRepository extends JpaRepository<EvolutionLineModel, Integer> {

    /**
     * Linhas cuja cadeia inclui o número nacional (a mesma espécie pode integrar vários ramos).
     */
    @Query("SELECT DISTINCT l FROM EvolutionLineModel l JOIN l.memberPokedexNumbers m WHERE m = :pokedexNumber")
    List<EvolutionLineModel> findAllLinesContainingPokedexNumber(@Param("pokedexNumber") Integer pokedexNumber);
}
