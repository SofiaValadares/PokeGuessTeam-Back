package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.pokemon.PokeballDrawResponse;
import com.svc.pokeguessteam.dto.pokemon.PokeballMultiDrawResponse;
import com.svc.pokeguessteam.dto.pokemon.PokemonDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.admin.BonusEventModel;
import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.enums.PokeballType;
import com.svc.pokeguessteam.model.enums.PokemonRarity;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileInventoryItemModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import com.svc.pokeguessteam.repository.pokemon.EvolutionLineRepository;
import com.svc.pokeguessteam.repository.user.ProfileInventoryItemRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserPokemonInventoryRepository;
import com.svc.pokeguessteam.util.PokemonInventoryXp;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PokeballDrawService {

    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final EvolutionLineRepository evolutionLineRepository;
    private final NationalPokedexCatalog nationalPokedexCatalog;
    private final ProfileInventoryItemRepository profileInventoryItemRepository;
    private final UserPokemonInventoryRepository userPokemonInventoryRepository;
    private final UserPokedexService userPokedexService;
    private final BonusEventService bonusEventService;

    public PokeballDrawService(
            ProfileRepository profileRepository,
            ProfileService profileService,
            EvolutionLineRepository evolutionLineRepository,
            NationalPokedexCatalog nationalPokedexCatalog,
            ProfileInventoryItemRepository profileInventoryItemRepository,
            UserPokemonInventoryRepository userPokemonInventoryRepository,
            UserPokedexService userPokedexService,
            BonusEventService bonusEventService
    ) {
        this.profileRepository = profileRepository;
        this.profileService = profileService;
        this.evolutionLineRepository = evolutionLineRepository;
        this.nationalPokedexCatalog = nationalPokedexCatalog;
        this.profileInventoryItemRepository = profileInventoryItemRepository;
        this.userPokemonInventoryRepository = userPokemonInventoryRepository;
        this.userPokedexService = userPokedexService;
        this.bonusEventService = bonusEventService;
    }

    @Transactional
    public PokeballMultiDrawResponse draw(String userId, PokeballType pokeballType, int count) {
        int safeCount = Math.min(10, Math.max(1, count));
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
        profileService.ensureProfileWithStarters(userId);
        consumePokeballs(profile, pokeballType, safeCount);

        List<PokeballDrawResponse> draws = new ArrayList<>(safeCount);
        for (int i = 0; i < safeCount; i++) {
            draws.add(rollAndGrant(profile, pokeballType));
        }
        return new PokeballMultiDrawResponse(draws);
    }

    private PokeballDrawResponse rollAndGrant(ProfileModel profile, PokeballType pokeballType) {
        EvolutionLineModel line;
        PokemonRarity rolledRarity;
        if (pokeballType == PokeballType.FRIEND_BALL) {
            line = pickFriendBallLine();
            rolledRarity = line.getRarity() != null ? line.getRarity() : PokemonRarity.COMMON;
        } else {
            rolledRarity = rollRarity(pokeballType);
            line = pickRandomLine(rolledRarity);
        }

        PokemonModel pokemon = resolveBaseOfLine(line);
        GrantResult grant = grantOrIncrementLine(profile, line);
        userPokedexService.registerSpecies(profile, pokemon.getPokedexNumber());
        userPokemonInventoryRepository
                .findByProfile_IdAndEvolutionLine_LineKey(profile.getId(), line.getLineKey())
                .ifPresent(row -> userPokedexService.registerUnlockedSpeciesForInventoryLine(profile, row));

        return new PokeballDrawResponse(
                pokeballType,
                rolledRarity,
                PokemonDto.from(pokemon, line),
                grant.newLine(),
                grant.timesObtained()
        );
    }

    private void consumePokeballs(ProfileModel profile, PokeballType type, int amount) {
        ProfileInventoryItemModel row = profileInventoryItemRepository
                .findByProfile_IdAndPokeballType(profile.getId(), type)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCodes.POKEBALL_INSUFFICIENT,
                        MessageKeys.POKEBALL_INSUFFICIENT
                ));
        int qty = row.getQuantity() != null ? row.getQuantity() : 0;
        if (qty < amount) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.POKEBALL_INSUFFICIENT,
                    MessageKeys.POKEBALL_INSUFFICIENT
            );
        }
        row.setQuantity(qty - amount);
        profileInventoryItemRepository.save(row);
    }

    /**
     * Friend Ball: escolhe uniformemente uma linha que intersecta o evento.
     */
    private EvolutionLineModel pickFriendBallLine() {
        BonusEventModel event = bonusEventService.findActive()
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.CONFLICT,
                        ErrorCodes.FRIEND_BALL_NO_EVENT,
                        MessageKeys.FRIEND_BALL_NO_EVENT
                ));
        Set<Integer> eventDex = new HashSet<>(event.getPokedexNumbers());
        if (eventDex.isEmpty()) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.FRIEND_BALL_POOL_EMPTY,
                    MessageKeys.FRIEND_BALL_POOL_EMPTY
            );
        }

        List<EvolutionLineModel> lines = evolutionLineRepository.findAllLinesContainingPokedexNumbers(eventDex)
                .stream()
                .filter(line -> line.getMemberPokedexNumbers() != null && !line.getMemberPokedexNumbers().isEmpty())
                .toList();
        if (lines.isEmpty()) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.FRIEND_BALL_POOL_EMPTY,
                    MessageKeys.FRIEND_BALL_POOL_EMPTY
            );
        }
        return lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
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
            case FRIEND_BALL -> PokemonRarity.COMMON;
        };
    }

    private static boolean rollOneIn(int denominator) {
        return ThreadLocalRandom.current().nextInt(denominator) == 0;
    }

    /** Sorteia uma linha evolutiva da raridade (não a espécie). */
    private EvolutionLineModel pickRandomLine(PokemonRarity rarity) {
        List<EvolutionLineModel> pool = evolutionLineRepository.findByRarity(rarity);
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

    /** Forma BASE da linha (primeiro membro se não houver stage BASE explícito). */
    private PokemonModel resolveBaseOfLine(EvolutionLineModel line) {
        List<Integer> members = line.getMemberPokedexNumbers();
        if (members == null || members.isEmpty()) {
            throw new ApiBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.POKEMON_POOL_EMPTY_FOR_RARITY,
                    MessageKeys.POKEMON_POOL_EMPTY_FOR_RARITY,
                    line.getRarity() != null ? line.getRarity().name() : "UNKNOWN"
            );
        }
        Map<Integer, PokemonModel> byDex = nationalPokedexCatalog.speciesByDex();
        List<PokemonModel> species = new ArrayList<>();
        for (Integer member : members) {
            PokemonModel pokemon = byDex.get(member);
            if (pokemon != null) {
                species.add(pokemon);
            }
        }
        PokemonModel base = species.stream()
                .filter(p -> p.getEvolutionStage() == EvolutionStage.BASE)
                .min(Comparator.comparingInt(PokemonModel::getPokedexNumber))
                .orElseGet(() -> species.stream()
                        .min(Comparator.comparingInt(p -> {
                            int idx = members.indexOf(p.getPokedexNumber());
                            return idx < 0 ? Integer.MAX_VALUE : idx;
                        }))
                        .orElse(null));
        if (base == null) {
            throw new ApiBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.POKEMON_SPECIES_NOT_FOUND,
                    MessageKeys.POKEMON_SPECIES_NOT_FOUND
            );
        }
        return base;
    }

    private GrantResult grantOrIncrementLine(ProfileModel profile, EvolutionLineModel line) {
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
        row.setClaimedMilestones("");
        PokemonInventoryXp.syncLevelFromTotalXp(row);
        userPokemonInventoryRepository.save(row);
        return new GrantResult(true, 1);
    }

    private record GrantResult(boolean newLine, int timesObtained) {
    }
}
