package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.user.UserSearchResultDto;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.user.UserRepository;
import com.svc.pokeguessteam.service.CryptoService;
import com.svc.pokeguessteam.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final int SEARCH_MIN_LENGTH = 2;
    private static final int SEARCH_MAX_RESULTS = 15;

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final CryptoService cryptoService;

    public UserController(
            UserRepository userRepository,
            CurrentUserService currentUserService,
            CryptoService cryptoService
    ) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.cryptoService = cryptoService;
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
                .map(this::toDto) // Método de instância para usar o cryptoService
                .toList();
        return ResponseEntity.ok(results);
    }

    /**
     * Criptografa um texto recebido no corpo da requisição (Requisito 9)
     * Exemplo de payload JSON: { "data": "texto plano" }
     */
    @PostMapping("/encrypt")
    public ResponseEntity<Map<String, String>> encrypt(@RequestBody Map<String, String> payload) {
        String data = payload.get("data");
        String encrypted = cryptoService.encrypt(data);
        return ResponseEntity.ok(Map.of("encrypted", encrypted));
    }

    /**
     * Descriptografa um texto cifrado recebido no corpo da requisição (Requisito 9)
     * Exemplo de payload JSON: { "data": "textoBase64Cifrado..." }
     */
    @PostMapping("/decrypt")
    public ResponseEntity<Map<String, String>> decrypt(@RequestBody Map<String, String> payload) {
        String encryptedData = payload.get("data");
        String decrypted = cryptoService.decrypt(encryptedData);
        return ResponseEntity.ok(Map.of("decrypted", decrypted));
    }

    /**
     * Transforma a entidade em DTO criptografando o ID do usuário apenas em memória para exibição.
     */
    private UserSearchResultDto toDto(UserModel user) {
        String idCriptografado = cryptoService.encrypt(user.getIdUser());
        return new UserSearchResultDto(idCriptografado, user.getUsername());
    }
}
