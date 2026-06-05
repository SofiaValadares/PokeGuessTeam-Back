package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.config.AppDevToolsProperties;
import com.svc.pokeguessteam.dto.dev.AdjustTrainingTeamXpRequest;
import com.svc.pokeguessteam.dto.pokemon.PcPageResponse;
import com.svc.pokeguessteam.dto.profile.TrainingTeamResponse;
import com.svc.pokeguessteam.dto.profile.UpdateFavoritePokemonRequest;
import com.svc.pokeguessteam.dto.profile.UpdateTrainingTeamRequest;
import com.svc.pokeguessteam.model.user.ProfileInventoryItemModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserService currentUserService;
    private final AppDevToolsProperties devToolsProperties;

    public ProfileController(
            ProfileService profileService,
            CurrentUserService currentUserService,
            AppDevToolsProperties devToolsProperties
    ) {
        this.profileService = profileService;
        this.currentUserService = currentUserService;
        this.devToolsProperties = devToolsProperties;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("profileId", profile.getId());
        body.put("userId", userId);
        if (profile.getFavoritePokemon() != null) {
            body.put("favoritePokemonId", String.valueOf(profile.getFavoritePokemon().getPokedexNumber()));
            body.put("favoritePokemonName", profile.getFavoritePokemon().getName());
        } else {
            body.put("favoritePokemonId", null);
            body.put("favoritePokemonName", null);
        }
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/favorite-pokemon")
    public ResponseEntity<Map<String, Object>> updateFavoritePokemon(
            HttpSession session,
            @Valid @RequestBody UpdateFavoritePokemonRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        profileService.updateFavoritePokemon(userId, request.pokedexNumber());
        return me(session);
    }

    /**
     * Time de treino (6 slots): cada um é uma linha evolutiva do inventário (PC).
     */
    @GetMapping("/training-team")
    public ResponseEntity<TrainingTeamResponse> trainingTeam(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(profileService.getTrainingTeam(userId));
    }

    @PutMapping("/training-team")
    public ResponseEntity<TrainingTeamResponse> updateTrainingTeamPut(
            HttpSession session,
            @Valid @RequestBody UpdateTrainingTeamRequest request
    ) {
        return updateTrainingTeam(session, request);
    }

    /** Alias para clientes que enviam POST em vez de PUT. */
    @PostMapping("/training-team")
    public ResponseEntity<TrainingTeamResponse> updateTrainingTeamPost(
            HttpSession session,
            @Valid @RequestBody UpdateTrainingTeamRequest request
    ) {
        return updateTrainingTeam(session, request);
    }

    private ResponseEntity<TrainingTeamResponse> updateTrainingTeam(
            HttpSession session,
            UpdateTrainingTeamRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(profileService.updateTrainingTeam(userId, request));
    }

    /**
     * Inventário de Pokémon do jogador (linhas evolutivas), paginado.
     */
    @GetMapping("/pokemon")
    public ResponseEntity<PcPageResponse> pokemonInventory(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + ProfileService.PokemonPcConstants.DEFAULT_PAGE_SIZE) int size
    ) {
        String userId = currentUserService.requireUserId(session);
        profileService.ensureProfileWithStarters(userId);
        int safeSize = Math.min(
                Math.max(size, 1),
                ProfileService.PokemonPcConstants.MAX_PAGE_SIZE
        );
        return ResponseEntity.ok(profileService.getPokemonPcPage(userId, page, safeSize));
    }

    /**
     * Inventário de Pokébolas e fragmentos.
     */
    @GetMapping("/collection")
    public ResponseEntity<Map<String, Object>> collection(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        List<ProfileInventoryItemModel> rows = profileService.getItemInventoryByProfileId(profile.getId());
        List<Map<String, Object>> items = rows.stream().map(entry -> {
            Map<String, Object> map = new HashMap<>();
            map.put("pokeballType", entry.getPokeballType().name());
            map.put("quantity", entry.getQuantity());
            return map;
        }).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("pokeballFragments", profile.getPokeballFragments() != null ? profile.getPokeballFragments() : 0);
        body.put("fragmentsPerPokeBall", ProfileService.FRAGMENTS_PER_POKE_BALL);
        return ResponseEntity.ok(body);
    }

    /**
     * Ferramenta de dev: ajusta XP do time de treino. Requer {@code app.dev-tools.enabled=true}.
     */
    @PostMapping("/dev/training-team/xp")
    public ResponseEntity<TrainingTeamResponse> adjustTrainingTeamXpDev(
            HttpSession session,
            @Valid @RequestBody AdjustTrainingTeamXpRequest request
    ) {
        if (!devToolsProperties.isEnabled()) {
            throw new ApiBusinessException(
                    HttpStatus.NOT_FOUND,
                    ErrorCodes.DEV_TOOLS_DISABLED,
                    MessageKeys.DEV_TOOLS_DISABLED
            );
        }
        String userId = currentUserService.requireUserId(session);
        profileService.ensureProfileWithStarters(userId);
        return ResponseEntity.ok(profileService.adjustTrainingTeamXp(userId, request.delta()));
    }
}
