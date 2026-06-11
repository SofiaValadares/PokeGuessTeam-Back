package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogo nacional em memória — espécies mudam raramente; evita reler toda a TB_POKEMON a cada pedido.
 */
@Service
public class NationalPokedexCatalog {

    private final PokemonRepository pokemonRepository;
    private volatile List<PokemonModel> cachedSpecies;

    public NationalPokedexCatalog(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    @Transactional(readOnly = true)
    public List<PokemonModel> allSpeciesOrdered() {
        List<PokemonModel> snapshot = cachedSpecies;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (this) {
            if (cachedSpecies == null) {
                cachedSpecies = List.copyOf(pokemonRepository.findAllByOrderByPokedexNumberAsc());
            }
            return cachedSpecies;
        }
    }
}
