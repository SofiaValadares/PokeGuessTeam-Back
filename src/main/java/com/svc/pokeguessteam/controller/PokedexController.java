package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.pokemon.PokedexCatalogResponse;
import com.svc.pokeguessteam.dto.pokemon.PokedexEntryDto;
import com.svc.pokeguessteam.dto.pokemon.PokedexEntryPageResponse;
import com.svc.pokeguessteam.dto.pokemon.PokedexVersionResponse;
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

    /** Versão leve do catálogo nacional (sem payload completo). */
    @GetMapping("/version")
    public ResponseEntity<PokedexVersionResponse> version(HttpSession session) {
        currentUserService.requireUserId(session);
        return ResponseEntity.ok(pokedexService.version());
    }

    /** Catálogo nacional + linhas evolutivas (sem flags de utilizador). */
    @GetMapping("/catalog")
    public ResponseEntity<PokedexCatalogResponse> catalog(HttpSession session) {
        currentUserService.requireUserId(session);
        return ResponseEntity.ok(pokedexService.catalog());
    }

    /** Números da Pokédex pessoal registados pelo utilizador. */
    @GetMapping("/registered")
    public ResponseEntity<List<Integer>> registered(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        profileService.ensureProfileWithStarters(userId);
        return ResponseEntity.ok(pokedexService.registeredPokedexNumbers(userId));
    }

    /**
     * Lista completa da Pokédex nacional com flag de registo na Pokédex pessoal.
     */
    @GetMapping("/all")
    public ResponseEntity<List<PokedexEntryDto>> listAll(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        profileService.ensureProfileWithStarters(userId);
        return ResponseEntity.ok(pokedexService.listAllForUser(userId));
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
            @RequestParam(defaultValue = "" + PokedexService.DEFAULT_PAGE_SIZE) int size
    ) {
        String userId = currentUserService.requireUserId(session);
        profileService.ensureProfileWithStarters(userId);
        return ResponseEntity.ok(pokedexService.listPageForUser(userId, page, size));
    }
}
