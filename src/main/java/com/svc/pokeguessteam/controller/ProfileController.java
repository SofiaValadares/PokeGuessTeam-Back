package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.pokemon.PcPageResponse;
import com.svc.pokeguessteam.dto.profile.TrainingTeamResponse;
import com.svc.pokeguessteam.model.user.ProfileInventoryItemModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    public ProfileController(ProfileService profileService, CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.currentUserService = currentUserService;
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

    /**
     * Time de treino ativo (6 posições): foco de XP / evolução no menu; não restringe uso nas partidas.
     */
    @GetMapping("/training-team")
    public ResponseEntity<TrainingTeamResponse> trainingTeam(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        profileService.ensureProfileWithStarters(userId);
        return ResponseEntity.ok(profileService.getTrainingTeam(userId));
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
}
