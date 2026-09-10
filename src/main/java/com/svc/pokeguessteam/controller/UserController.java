package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.user.UserSearchResultDto;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.user.UserRepository;
import com.svc.pokeguessteam.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final int SEARCH_MIN_LENGTH = 2;
    private static final int SEARCH_MAX_RESULTS = 15;

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public UserController(UserRepository userRepository, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Pesquisa utilizadores por nome (ex.: convidar amigo). Exclui o utilizador autenticado.
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResultDto>> search(
            HttpSession session,
            @RequestParam("q") String query
    ) {
        String userId = currentUserService.requireUserId(session);
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.length() < SEARCH_MIN_LENGTH) {
            return ResponseEntity.ok(List.of());
        }
        List<UserSearchResultDto> results = userRepository
                .findByUsernameContainingIgnoreCaseOrderByUsernameAsc(
                        trimmed,
                        PageRequest.of(0, SEARCH_MAX_RESULTS)
                )
                .stream()
                .filter(user -> !user.getIdUser().equals(userId))
                .map(UserController::toDto)
                .toList();
        return ResponseEntity.ok(results);
    }

    private static UserSearchResultDto toDto(UserModel user) {
        return new UserSearchResultDto(user.getIdUser(), user.getUsername());
    }
}
