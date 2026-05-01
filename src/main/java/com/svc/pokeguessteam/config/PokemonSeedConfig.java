package com.svc.pokeguessteam.config;

import com.svc.pokeguessteam.model.PokemonModel;
import com.svc.pokeguessteam.model.enums.PokemonRarity;
import com.svc.pokeguessteam.repository.PokemonRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PokemonSeedConfig {

    @Bean
    public CommandLineRunner seedPokedex(PokemonRepository pokemonRepository) {
        return args -> {
            if (pokemonRepository.count() > 0) {
                return;
            }

            pokemonRepository.saveAll(List.of(
                    pokemon(1, "Bulbasaur", "Grass", "Poison", 1, "Green", 7, 69, PokemonRarity.COMMON),
                    pokemon(4, "Charmander", "Fire", null, 1, "Red", 6, 85, PokemonRarity.COMMON),
                    pokemon(7, "Squirtle", "Water", null, 1, "Blue", 5, 90, PokemonRarity.COMMON),
                    pokemon(25, "Pikachu", "Electric", null, 1, "Yellow", 4, 60, PokemonRarity.RARE),
                    pokemon(39, "Jigglypuff", "Normal", "Fairy", 1, "Pink", 5, 55, PokemonRarity.COMMON),
                    pokemon(94, "Gengar", "Ghost", "Poison", 1, "Purple", 15, 405, PokemonRarity.RARE),
                    pokemon(131, "Lapras", "Water", "Ice", 1, "Blue", 25, 2200, PokemonRarity.RARE),
                    pokemon(149, "Dragonite", "Dragon", "Flying", 1, "Brown", 22, 2100, PokemonRarity.LEGENDARY),
                    pokemon(150, "Mewtwo", "Psychic", null, 1, "Purple", 20, 1220, PokemonRarity.LEGENDARY),
                    pokemon(151, "Mew", "Psychic", null, 1, "Pink", 4, 40, PokemonRarity.MYTHICAL),
                    pokemon(245, "Suicune", "Water", null, 2, "Blue", 20, 1870, PokemonRarity.LEGENDARY),
                    pokemon(251, "Celebi", "Psychic", "Grass", 2, "Green", 6, 50, PokemonRarity.MYTHICAL)
            ));
        };
    }

    private PokemonModel pokemon(int number,
                                 String name,
                                 String primaryType,
                                 String secondaryType,
                                 int generation,
                                 String color,
                                 int heightDm,
                                 int weightHg,
                                 PokemonRarity rarity) {
        PokemonModel model = new PokemonModel();
        model.setPokedexNumber(number);
        model.setName(name);
        model.setPrimaryType(primaryType);
        model.setSecondaryType(secondaryType);
        model.setGeneration(generation);
        model.setColor(color);
        model.setHeightDm(heightDm);
        model.setWeightHg(weightHg);
        model.setRarity(rarity);
        return model;
    }
}
