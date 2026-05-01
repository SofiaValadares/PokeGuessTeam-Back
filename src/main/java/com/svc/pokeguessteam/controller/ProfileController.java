package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.model.PlayerPokemonModel;
import com.svc.pokeguessteam.model.ProfileModel;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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
        return ResponseEntity.ok(Map.of(
                "profileId", profile.getId(),
                "userId", userId,
                "rankTitle", profile.getRankTitle(),
                "pokeball", profile.getPokeballCount(),
                "greatBall", profile.getGreatBallCount(),
                "ultraBall", profile.getUltraBallCount(),
                "masterBall", profile.getMasterBallCount(),
                "shards", profile.getShardCount()
        ));
    }

    @GetMapping("/collection")
    public ResponseEntity<List<Map<String, Object>>> collection(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        List<PlayerPokemonModel> collection = profileService.getCollection(userId);
        List<Map<String, Object>> payload = collection.stream().map(entry -> {
            Map<String, Object> map = new HashMap<>();
            map.put("pokemonId", entry.getPokemon().getId());
            map.put("name", entry.getPokemon().getName());
            map.put("rarity", entry.getPokemon().getRarity().name());
            map.put("level", entry.getLevel());
            map.put("xp", entry.getXp());
            map.put("shiny", entry.getShiny());
            return map;
        }).toList();
        return ResponseEntity.ok(payload);
    }
}
