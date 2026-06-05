package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.dto.pokemon.ClaimEvolutionRewardsResponse;
import com.svc.pokeguessteam.dto.pokemon.PcLineDto;
import com.svc.pokeguessteam.dto.pokemon.PcPageResponse;
import com.svc.pokeguessteam.dto.profile.TrainingTeamResponse;
import com.svc.pokeguessteam.dto.profile.UpdateTrainingTeamRequest;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.repository.pokemon.EvolutionLineRepository;
import com.svc.pokeguessteam.model.enums.PokeballType;
import com.svc.pokeguessteam.model.user.ProfileInventoryItemModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.TrainingTeamModel;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.repository.user.ProfileInventoryItemRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserPokemonInventoryRepository;
import com.svc.pokeguessteam.repository.user.UserRepository;
import com.svc.pokeguessteam.util.PokemonEvolutionRewards;
import com.svc.pokeguessteam.util.PokemonInventoryXp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProfileService {

    /** Fragmentos de Poké Bola necessários para ganhar 1 Poké Bola. */
    public static final int FRAGMENTS_PER_POKE_BALL = 10;

    /**
     * Uma entrada por linha evolutiva: iniciais principais de cada geração (Pokédex nacional).
     */
    private static final int[] STARTER_POKEDEX_NUMBERS = {
            1, 4, 7,
            152, 155, 158,
            252, 255, 258,
            387, 390, 393,
            495, 498, 501,
            650, 653, 656,
            722, 725, 728,
            810, 813, 816,
            906, 909, 912,
    };

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PokemonRepository pokemonRepository;
    private final UserPokemonInventoryRepository inventoryRepository;
    private final ProfileInventoryItemRepository profileInventoryItemRepository;
    private final UserPokedexService userPokedexService;
    private final EvolutionLineRepository evolutionLineRepository;

    public ProfileService(ProfileRepository profileRepository,
                          UserRepository userRepository,
                          PokemonRepository pokemonRepository,
                          UserPokemonInventoryRepository inventoryRepository,
                          ProfileInventoryItemRepository profileInventoryItemRepository,
                          UserPokedexService userPokedexService,
                          EvolutionLineRepository evolutionLineRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.pokemonRepository = pokemonRepository;
        this.inventoryRepository = inventoryRepository;
        this.profileInventoryItemRepository = profileInventoryItemRepository;
        this.userPokedexService = userPokedexService;
        this.evolutionLineRepository = evolutionLineRepository;
    }

    @Transactional
    public ProfileModel ensureProfileWithStarters(String userId) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId).orElseGet(() -> createProfile(userId));
        for (int dex : STARTER_POKEDEX_NUMBERS) {
            grantStarterLineIfMissing(profile, dex);
        }
        ensurePokeballInventoryIfMissing(profile);
        userPokedexService.registerStarterSpecies(profile, STARTER_POKEDEX_NUMBERS);
        userPokedexService.syncFromOwnership(profile);
        ensureTrainingTeamFromInventory(profile);
        return profileRepository.findById(profile.getId()).orElse(profile);
    }

    @Transactional
    public ProfileModel updateFavoritePokemon(String userId, int pokedexNumber) {
        ProfileModel profile = ensureProfileWithStarters(userId);
        PokemonModel pokemon = pokemonRepository.findByPokedexNumber(pokedexNumber)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.POKEMON_SPECIES_NOT_FOUND,
                        MessageKeys.POKEMON_SPECIES_NOT_FOUND
                ));
        profile.setFavoritePokemon(pokemon);
        return profileRepository.save(profile);
    }

    private ProfileModel createProfile(String userId) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_USER_NOT_FOUND,
                        MessageKeys.PROFILE_USER_NOT_FOUND
                ));
        ProfileModel profile = new ProfileModel();
        profile.setUser(user);
        profile.setPokeballFragments(0);
        ProfileModel saved = profileRepository.save(profile);
        pokemonRepository.findByPokedexNumber(1).ifPresent(saved::setFavoritePokemon);
        return profileRepository.save(saved);
    }

    /**
     * Garante time de treino só com linhas evolutivas do inventário; cria ou corrige slots inválidos.
     */
    private void ensureTrainingTeamFromInventory(ProfileModel profile) {
        TrainingTeamModel team = profile.getTrainingTeam();
        if (team == null) {
            team = new TrainingTeamModel();
            team.setProfile(profile);
            profile.setTrainingTeam(team);
            assignDefaultTrainingTeamFromInventory(profile, team);
            profileRepository.save(profile);
            return;
        }
        boolean changed = sanitizeTrainingTeamSlots(profile, team);
        if (changed) {
            profileRepository.save(profile);
        }
    }

    /**
     * Preenche até 6 slots com linhas evolutivas aleatórias do inventário.
     */
    private void assignDefaultTrainingTeamFromInventory(ProfileModel profile, TrainingTeamModel team) {
        List<UserPokemonInventoryModel> lines = inventoryRepository
                .findByProfile_IdOrderByEvolutionLine_LineKeyAsc(profile.getId());
        if (lines.isEmpty()) {
            for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
                team.setSlot(i, null);
            }
            return;
        }
        List<UserPokemonInventoryModel> shuffled = new ArrayList<>(lines);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            if (i < shuffled.size()) {
                team.setSlot(i, shuffled.get(i).getEvolutionLine());
            } else {
                team.setSlot(i, null);
            }
        }
    }

    /** Remove slots cuja linha não está mais no inventário do jogador. */
    private boolean sanitizeTrainingTeamSlots(ProfileModel profile, TrainingTeamModel team) {
        boolean changed = false;
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            EvolutionLineModel slot = team.getSlot(i);
            if (slot != null && !isLineInInventory(profile.getId(), slot.getLineKey())) {
                team.setSlot(i, null);
                changed = true;
            }
        }
        return changed;
    }

    private boolean isLineInInventory(String profileId, Integer lineKey) {
        if (lineKey == null) {
            return false;
        }
        return inventoryRepository.findByProfile_IdAndEvolutionLine_LineKey(profileId, lineKey).isPresent();
    }

    private void requireLineInInventory(ProfileModel profile, Integer lineKey) {
        if (!isLineInInventory(profile.getId(), lineKey)) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.TRAINING_TEAM_LINE_NOT_IN_INVENTORY,
                    MessageKeys.TRAINING_TEAM_LINE_NOT_IN_INVENTORY
            );
        }
    }

    private Map<Integer, UserPokemonInventoryModel> inventoryByLineKey(String profileId) {
        Map<Integer, UserPokemonInventoryModel> map = new HashMap<>();
        for (UserPokemonInventoryModel row : inventoryRepository.findByProfile_IdOrderByEvolutionLine_LineKeyAsc(profileId)) {
            migrateLegacyClaimedMilestonesIfNeeded(row);
            if (row.getEvolutionLine() != null) {
                map.put(row.getEvolutionLine().getLineKey(), row);
            }
        }
        return map;
    }

    private void migrateLegacyClaimedMilestonesIfNeeded(UserPokemonInventoryModel row) {
        if (row.getClaimedMilestones() != null) {
            return;
        }
        int level = row.getLevel() != null
                ? row.getLevel()
                : PokemonInventoryXp.levelFromTotalXp(row.getTotalXp() != null ? row.getTotalXp() : 0);
        row.setClaimedMilestones(PokemonEvolutionRewards.formatClaimed(PokemonEvolutionRewards.milestonesReached(level)));
        inventoryRepository.save(row);
    }

    /**
     * Resgata todas as recompensas de marco pendentes numa linha evolutiva do inventário.
     */
    @Transactional
    public ClaimEvolutionRewardsResponse claimEvolutionRewards(String userId, int lineKey) {
        ProfileModel profile = ensureProfileWithStarters(userId);
        UserPokemonInventoryModel row = inventoryRepository
                .findByProfile_IdAndEvolutionLine_LineKey(profile.getId(), lineKey)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.TRAINING_TEAM_LINE_NOT_IN_INVENTORY,
                        MessageKeys.TRAINING_TEAM_LINE_NOT_IN_INVENTORY
                ));
        migrateLegacyClaimedMilestonesIfNeeded(row);
        int level = row.getLevel() != null
                ? row.getLevel()
                : PokemonInventoryXp.levelFromTotalXp(row.getTotalXp() != null ? row.getTotalXp() : 0);
        List<Integer> claimed = new ArrayList<>(PokemonEvolutionRewards.parseClaimed(row.getClaimedMilestones()));
        List<Integer> pending = PokemonEvolutionRewards.pendingMilestones(level, claimed);
        if (pending.isEmpty()) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.EVOLUTION_REWARD_NOT_AVAILABLE,
                    MessageKeys.EVOLUTION_REWARD_NOT_AVAILABLE
            );
        }
        EnumMap<PokeballType, Integer> grants = new EnumMap<>(PokeballType.class);
        for (int milestone : pending) {
            for (var entry : PokemonEvolutionRewards.rewardsForMilestone(milestone).entrySet()) {
                grants.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            claimed.add(milestone);
        }
        row.setClaimedMilestones(PokemonEvolutionRewards.formatClaimed(claimed));
        inventoryRepository.save(row);
        for (var entry : grants.entrySet()) {
            addPokeballs(userId, entry.getKey(), entry.getValue());
        }
        Map<String, Integer> granted = new LinkedHashMap<>();
        for (var entry : grants.entrySet()) {
            granted.put(entry.getKey().name(), entry.getValue());
        }
        return new ClaimEvolutionRewardsResponse(PcLineDto.from(row), Map.copyOf(granted));
    }

    /**
     * Garante uma linha por tipo de Pokébola (quantidade inicial 0) quando o registo ainda não existe.
     */
    private void ensurePokeballInventoryIfMissing(ProfileModel profile) {
        for (PokeballType type : PokeballType.values()) {
            if (profileInventoryItemRepository.findByProfile_IdAndPokeballType(profile.getId(), type).isPresent()) {
                continue;
            }
            ProfileInventoryItemModel row = new ProfileInventoryItemModel();
            row.setProfile(profile);
            row.setPokeballType(type);
            row.setQuantity(0);
            profileInventoryItemRepository.save(row);
        }
    }

    /**
     * Acrescenta fragmentos de Poké Bola; de 10 em 10 converte automaticamente em 1 Poké Bola no inventário.
     */
    @Transactional
    public void addPokeballFragments(String userId, int amount) {
        if (amount <= 0) {
            return;
        }
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
        ensurePokeballInventoryIfMissing(profile);
        int current = profile.getPokeballFragments() != null ? profile.getPokeballFragments() : 0;
        profile.setPokeballFragments(current + amount);
        convertPokeballFragmentsToBalls(profile);
        profileRepository.save(profile);
    }

    private void convertPokeballFragmentsToBalls(ProfileModel profile) {
        int frags = profile.getPokeballFragments() != null ? profile.getPokeballFragments() : 0;
        if (frags < FRAGMENTS_PER_POKE_BALL) {
            return;
        }
        ProfileInventoryItemModel pokeRow = profileInventoryItemRepository
                .findByProfile_IdAndPokeballType(profile.getId(), PokeballType.POKE_BALL)
                .orElseThrow();
        int newBalls = frags / FRAGMENTS_PER_POKE_BALL;
        int remainder = frags % FRAGMENTS_PER_POKE_BALL;
        int qty = pokeRow.getQuantity() != null ? pokeRow.getQuantity() : 0;
        pokeRow.setQuantity(qty + newBalls);
        profile.setPokeballFragments(remainder);
        profileInventoryItemRepository.save(pokeRow);
    }

    private void grantStarterLineIfMissing(ProfileModel profile, int pokedexNumber) {
        PokemonModel pokemon = pokemonRepository.findByPokedexNumber(pokedexNumber).orElse(null);
        if (pokemon == null) {
            return;
        }
        EvolutionLineModel line = pokemon.getEvolutionLine();
        var existing = inventoryRepository.findByProfile_IdAndEvolutionLine_LineKey(
                profile.getId(),
                line.getLineKey()
        );
        if (existing.isPresent()) {
            userPokedexService.registerUnlockedSpeciesForInventoryLine(profile, existing.get());
            userPokedexService.registerSpeciesIfPresent(profile, pokedexNumber);
            return;
        }
        UserPokemonInventoryModel row = new UserPokemonInventoryModel();
        row.setProfile(profile);
        row.setEvolutionLine(line);
        row.setTotalXp(0);
        row.setTimesObtained(1);
        row.setClaimedMilestones("");
        PokemonInventoryXp.syncLevelFromTotalXp(row);
        inventoryRepository.save(row);
        userPokedexService.registerSpeciesIfPresent(profile, pokedexNumber);
        userPokedexService.registerUnlockedSpeciesForInventoryLine(profile, row);
    }

    @Transactional(readOnly = true)
    public List<ProfileInventoryItemModel> getItemInventory(String userId) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
        return listInventoryItemsOrdered(profile.getId());
    }

    @Transactional(readOnly = true)
    public List<ProfileInventoryItemModel> getItemInventoryByProfileId(String profileId) {
        return listInventoryItemsOrdered(profileId);
    }

    private List<ProfileInventoryItemModel> listInventoryItemsOrdered(String profileId) {
        return profileInventoryItemRepository.findByProfile_Id(profileId).stream()
                .sorted(Comparator.comparingInt(a -> a.getPokeballType().ordinal()))
                .toList();
    }

    /**
     * PC do jogador: linhas evolutivas do inventário, paginadas (ordenadas por linha evolutiva).
     */
    @Transactional
    public PcPageResponse getPokemonPcPage(String userId, int page, int size) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), PokemonPcConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "evolutionLine.lineKey")
        );
        Page<UserPokemonInventoryModel> result = inventoryRepository.findByProfile_Id(profile.getId(), pageable);
        result.getContent().forEach(this::migrateLegacyClaimedMilestonesIfNeeded);
        return PcPageResponse.from(result);
    }

    @Transactional
    public TrainingTeamResponse getTrainingTeam(String userId) {
        ProfileModel profile = ensureProfileWithStarters(userId);
        return TrainingTeamResponse.from(
                profile.getTrainingTeam(),
                inventoryByLineKey(profile.getId())
        );
    }

    /**
     * Atualiza o time de treino; cada slot é um {@code evolutionLineKey} do inventário (PC).
     */
    @Transactional
    public TrainingTeamResponse updateTrainingTeam(String userId, UpdateTrainingTeamRequest request) {
        ProfileModel profile = ensureProfileWithStarters(userId);
        TrainingTeamModel team = profile.getTrainingTeam();
        if (team == null) {
            team = new TrainingTeamModel();
            team.setProfile(profile);
            profile.setTrainingTeam(team);
        }

        List<Integer> slots = request.slots();
        Set<Integer> usedLineKeys = new HashSet<>();
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            Integer lineKey = slots.get(i);
            if (lineKey == null) {
                team.setSlot(i, null);
                continue;
            }
            if (!usedLineKeys.add(lineKey)) {
                throw new ApiBusinessException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCodes.TRAINING_TEAM_DUPLICATE,
                        MessageKeys.TRAINING_TEAM_DUPLICATE
                );
            }
            requireLineInInventory(profile, lineKey);
            EvolutionLineModel line = evolutionLineRepository.findById(lineKey)
                    .orElseThrow(() -> new ApiBusinessException(
                            HttpStatus.NOT_FOUND,
                            ErrorCodes.TRAINING_TEAM_LINE_NOT_FOUND,
                            MessageKeys.TRAINING_TEAM_LINE_NOT_FOUND
                    ));
            team.setSlot(i, line);
        }

        profileRepository.save(profile);
        userPokedexService.syncFromOwnership(profile);
        return TrainingTeamResponse.from(team, inventoryByLineKey(profile.getId()));
    }

    @Transactional
    public void addPokeballs(String userId, PokeballType type, int amount) {
        if (amount <= 0) {
            return;
        }
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
        ensurePokeballInventoryIfMissing(profile);
        ProfileInventoryItemModel row = profileInventoryItemRepository
                .findByProfile_IdAndPokeballType(profile.getId(), type)
                .orElseThrow();
        int qty = row.getQuantity() != null ? row.getQuantity() : 0;
        row.setQuantity(qty + amount);
        profileInventoryItemRepository.save(row);
    }

    /**
     * Distribui XP de partida pelas linhas evolutivas do time de treino.
     */
    @Transactional
    public void grantTrainingTeamMatchXp(String userId, int totalXp) {
        if (totalXp <= 0) {
            return;
        }
        ProfileModel profile = ensureProfileWithStarters(userId);
        TrainingTeamModel team = profile.getTrainingTeam();
        if (team == null) {
            return;
        }
        List<Integer> lineKeys = new ArrayList<>();
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            EvolutionLineModel slot = team.getSlot(i);
            if (slot != null) {
                lineKeys.add(slot.getLineKey());
            }
        }
        if (lineKeys.isEmpty()) {
            return;
        }
        int perSlot = totalXp / lineKeys.size();
        int remainder = totalXp % lineKeys.size();
        for (int i = 0; i < lineKeys.size(); i++) {
            int grant = perSlot + (i == 0 ? remainder : 0);
            addXpToInventoryLine(profile, lineKeys.get(i), grant);
        }
    }

    /**
     * Ajusta XP do time de treino (ferramenta de dev). Distribui o delta pelos slots ocupados.
     */
    @Transactional
    public TrainingTeamResponse adjustTrainingTeamXp(String userId, int totalDelta) {
        if (totalDelta == 0) {
            return getTrainingTeam(userId);
        }
        ProfileModel profile = ensureProfileWithStarters(userId);
        TrainingTeamModel team = profile.getTrainingTeam();
        if (team == null) {
            return TrainingTeamResponse.from(null, inventoryByLineKey(profile.getId()));
        }
        List<Integer> lineKeys = new ArrayList<>();
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            EvolutionLineModel slot = team.getSlot(i);
            if (slot != null) {
                lineKeys.add(slot.getLineKey());
            }
        }
        if (lineKeys.isEmpty()) {
            return getTrainingTeam(userId);
        }
        int absTotal = Math.abs(totalDelta);
        int perSlot = absTotal / lineKeys.size();
        int remainder = absTotal % lineKeys.size();
        int sign = totalDelta > 0 ? 1 : -1;
        for (int i = 0; i < lineKeys.size(); i++) {
            int magnitude = perSlot + (i == 0 ? remainder : 0);
            applyXpDeltaToInventoryLine(profile, lineKeys.get(i), sign * magnitude);
        }
        return getTrainingTeam(userId);
    }

    private void addXpToInventoryLine(ProfileModel profile, Integer lineKey, int xp) {
        if (xp <= 0 || lineKey == null) {
            return;
        }
        inventoryRepository.findByProfile_IdAndEvolutionLine_LineKey(profile.getId(), lineKey)
                .ifPresent(row -> {
                    PokemonInventoryXp.addXpAndSyncLevel(row, xp);
                    inventoryRepository.save(row);
                    userPokedexService.registerUnlockedSpeciesForInventoryLine(profile, row);
                });
    }

    private void applyXpDeltaToInventoryLine(ProfileModel profile, Integer lineKey, int xpDelta) {
        if (xpDelta == 0 || lineKey == null) {
            return;
        }
        inventoryRepository.findByProfile_IdAndEvolutionLine_LineKey(profile.getId(), lineKey)
                .ifPresent(row -> {
                    PokemonInventoryXp.applyXpDeltaAndSyncLevel(row, xpDelta);
                    inventoryRepository.save(row);
                    userPokedexService.registerUnlockedSpeciesForInventoryLine(profile, row);
                });
    }

    /** Constantes partilhadas com {@link com.svc.pokeguessteam.controller.PokemonController} (PC). */
    public static final class PokemonPcConstants {
        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE = 100;

        private PokemonPcConstants() {
        }
    }
}
  
