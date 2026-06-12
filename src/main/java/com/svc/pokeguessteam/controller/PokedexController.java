package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.pokemon.PokedexEntryDto;
import com.svc.pokeguessteam.dto.pokemon.PokedexEntryPageResponse;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.PokedexService;
import com.svc.pokeguessteam.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pokedex")
public class PokedexController {

    private final PokedexService pokedexService;
    private final CurrentUserService currentUserService;
    private final ProfileService profileService;

    public PokedexController(
            PokedexService pokedexService,
            CurrentUserService currentUserService,
            ProfileService profileService
    ) {
        this.pokedexService = pokedexService;
        this.currentUserService = currentUserService;
        this.profileService = profileService;
    }

    /**
     * Entradas da Pokédex pessoal (apenas espécies registadas).
     */
    @GetMapping("/registered")
    public ResponseEntity<List<PokedexEntryDto>> listRegistered(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        profileService.ensureProfileWithStarters(userId);
        return ResponseEntity.ok(pokedexService.listRegisteredForUser(userId));
    }

    /**
     * Lista completa da Pokédex nacional com flag de registo na Pokédex pessoal.
     */
    @GetMapping("/all")
    public ResponseEntity<List<PokedexEntryDto>> listAll(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        profileService.ensureProfileWithStarters(userId);
        return ResponseEntity.ok(pokedexService.listAllForUser(userId)); // sync incluído no serviço
    }

    /**
     * Pokédex paginada com flag de registo na Pokédex pessoal.
     *
     * @param page índice baseado em zero (primeira página = {@code 0})
     * @param size quantidade por página (padrão {@value com.svc.pokeguessteam.service.PokedexService#DEFAULT_PAGE_SIZE},
     *             máximo {@value com.svc.pokeguessteam.service.PokedexService#MAX_PAGE_SIZE})
     */
    @GetMapping
    public ResponseEntity<PokedexEntryPageResponse> listPage(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PokedexService.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) String q
    ) {
        String userId = currentUserService.requireUserId(session);
        profileService.ensureProfileWithStarters(userId);
        return ResponseEntity.ok(pokedexService.listPageForUser(userId, page, size, q));
    }
}
