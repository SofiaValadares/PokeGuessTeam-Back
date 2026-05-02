package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.model.user.ProfileInventoryItemModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
