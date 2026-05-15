package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.pokemon.PokeballDrawResponse;
import com.svc.pokeguessteam.dto.pokemon.PokemonDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.PokeballType;
import com.svc.pokeguessteam.model.enums.PokemonRarity;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileInventoryItemModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.repository.user.ProfileInventoryItemRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserPokemonInventoryRepository;
import com.svc.pokeguessteam.util.PokemonInventoryXp;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PokeballDrawService {

    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final PokemonRepository pokemonRepository;
    private final ProfileInventoryItemRepository profileInventoryItemRepository;
    private final UserPokemonInventoryRepository userPokemonInventoryRepository;

    public PokeballDrawService(
            ProfileRepository profileRepository,
            ProfileService profileService,
            PokemonRepository pokemonRepository,
            ProfileInventoryItemRepository profileInventoryItemRepository,
            UserPokemonInventoryRepository userPokemonInventoryRepository
    ) {
        this.profileRepository = profileRepository;
        this.profileService = profileService;
        this.pokemonRepository = pokemonRepository;
        this.profileInventoryItemRepository = profileInventoryItemRepository;
        this.userPokemonInventoryRepository = userPokemonInventoryRepository;
    }

    @Transactional
    public PokeballDrawResponse draw(String userId, PokeballType pokeballType) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
        profileService.ensureProfileWithStarters(userId);
        consumePokeball(profile, pokeballType);

        PokemonRarity rolledRarity = rollRarity(pokeballType);
        PokemonModel pokemon = pickRandomPokemon(rolledRarity);
        GrantResult grant = grantOrIncrementLine(profile, pokemon);

        return new PokeballDrawResponse(
                pokeballType,
                rolledRarity,
                PokemonDto.from(pokemon),
                grant.newLine(),
                grant.timesObtained()
        );
    }

    private void consumePokeball(ProfileModel profile, PokeballType type) {
        ProfileInventoryItemModel row = profileInventoryItemRepository
                .findByProfile_IdAndPokeballType(profile.getId(), type)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCodes.POKEBALL_INSUFFICIENT,
                        MessageKeys.POKEBALL_INSUFFICIENT
                ));
        int qty = row.getQuantity() != null ? row.getQuantity() : 0;
        if (qty < 1) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.POKEBALL_INSUFFICIENT,
                    MessageKeys.POKEBALL_INSUFFICIENT
            );
        }
        row.setQuantity(qty - 1);
        profileInventoryItemRepository.save(row);
    }

    /**
     * Probabilidades por tipo de bola (verificação do tier mais raro primeiro).
     */
    private PokemonRarity rollRarity(PokeballType type) {
        return switch (type) {
            case MASTER_BALL -> PokemonRarity.MYTHICAL;
            case ULTRA_BALL -> {
                if (rollOneIn(150)) {
                    yield PokemonRarity.MYTHICAL;
                }
                yield PokemonRarity.LEGENDARY;
            }
            case GREAT_BALL -> {
                if (rollOneIn(300)) {
                    yield PokemonRarity.MYTHICAL;
                }
                if (rollOneIn(50)) {
                    yield PokemonRarity.LEGENDARY;
                }
                yield PokemonRarity.RARE;
            }
            case POKE_BALL -> {
                if (rollOneIn(500)) {
                    yield PokemonRarity.MYTHICAL;
                }
                if (rollOneIn(100)) {
                    yield PokemonRarity.LEGENDARY;
                }
                if (rollOneIn(10)) {
                    yield PokemonRarity.RARE;
                }
                yield PokemonRarity.COMMON;
            }
        };
    }

    private static boolean rollOneIn(int denominator) {
        return ThreadLocalRandom.current().nextInt(denominator) == 0;
    }

    private PokemonModel pickRandomPokemon(PokemonRarity rarity) {
        List<PokemonModel> pool = pokemonRepository.findByEvolutionLine_Rarity(rarity);
        if (pool.isEmpty()) {
            throw new ApiBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.POKEMON_POOL_EMPTY_FOR_RARITY,
                    MessageKeys.POKEMON_POOL_EMPTY_FOR_RARITY,
                    rarity.name()
            );
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private GrantResult grantOrIncrementLine(ProfileModel profile, PokemonModel pokemon) {
        var line = pokemon.getEvolutionLine();
        var existing = userPokemonInventoryRepository.findByProfile_IdAndEvolutionLine_LineKey(
                profile.getId(),
                line.getLineKey()
        );
        if (existing.isPresent()) {
            UserPokemonInventoryModel row = existing.get();
            int times = row.getTimesObtained() != null ? row.getTimesObtained() : 0;
            row.setTimesObtained(times + 1);
            userPokemonInventoryRepository.save(row);
            return new GrantResult(false, row.getTimesObtained());
        }
        UserPokemonInventoryModel row = new UserPokemonInventoryModel();
        row.setProfile(profile);
        row.setEvolutionLine(line);
        row.setTotalXp(0);
        row.setTimesObtained(1);
        PokemonInventoryXp.syncLevelFromTotalXp(row);
        userPokemonInventoryRepository.save(row);
        return new GrantResult(true, 1);
    }

    private record GrantResult(boolean newLine, int timesObtained) {
    }
}
