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
import com.svc.pokeguessteam.service.UserPokedexService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final UserPokedexService userPokedexService;
    private final CurrentUserService currentUserService;
    private final AppDevToolsProperties devToolsProperties;

    public ProfileController(
            ProfileService profileService,
            UserPokedexService userPokedexService,
            CurrentUserService currentUserService,
            AppDevToolsProperties devToolsProperties
    ) {
        this.profileService = profileService;
        this.userPokedexService = userPokedexService;
        this.currentUserService = currentUserService;
        this.devToolsProperties = devToolsProperties;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
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
        body.put("avatarMode", profile.getAvatarMode().name());
        body.put("hasCustomAvatar", profile.getAvatarCustomBytes() != null && profile.getAvatarCustomBytes().length > 0);
        body.put("avatarUrl", "/api/profile/avatar/" + userId);
        body.put("registeredPokedexCount", userPokedexService.countRegistered(profile.getId()));
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            HttpSession session,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "hiddenMessage", required = false) String hiddenMessage
    ) throws java.io.IOException {
        String userId = currentUserService.requireUserId(session);
        profileService.uploadCustomAvatar(userId, file.getBytes(), file.getContentType(), hiddenMessage);
        return me(session);
    }

    /** Revela mensagem LSB escondida no avatar custom do utilizador autenticado. */
    @PostMapping("/avatar/stego/extract")
    public ResponseEntity<Map<String, String>> extractAvatarStego(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        String message = profileService.extractHiddenMessageFromAvatar(userId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/avatar/use-pokemon")
    public ResponseEntity<Map<String, Object>> usePokemonAvatar(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        profileService.usePokemonAvatar(userId);
        return me(session);
    }

    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String userId) {
        return profileService.findCustomAvatar(userId)
                .map(entry -> ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                        .contentType(org.springframework.http.MediaType.parseMediaType(entry.getKey()))
                        .body(entry.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
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
        // Converte fragmentos ≥10 → Poké Bola (também repara contas que ficaram “presas” a 10/10).
        ProfileModel profile = profileService.reconcilePokeballFragments(userId);
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
