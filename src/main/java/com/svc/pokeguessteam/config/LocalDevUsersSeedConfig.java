package com.svc.pokeguessteam.config;

import com.svc.pokeguessteam.model.enums.UserRole;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.user.UserRepository;
import com.svc.pokeguessteam.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Contas locais de teste (só com {@code DEV_TOOLS_ENABLED=true}).
 * Idempotente: cria se faltar; sincroniza papel / DEV / e-mail verificado.
 */
@Component
@Order(1000)
@ConditionalOnProperty(prefix = "app.dev-tools", name = "enabled", havingValue = "true")
public class LocalDevUsersSeedConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDevUsersSeedConfig.class);

    /** Senha partilhada só para testes locais — documentada no README. */
    public static final String LOCAL_PASSWORD = "PokeTeam1!";

    private static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("masterdev", "masterdev@local.test", UserRole.MASTER_ADMIN, true),
            new SeedUser("adminonly", "admin@local.test", UserRole.ADMIN, false),
            new SeedUser("labdev", "dev@local.test", UserRole.USER, true),
            new SeedUser("player1", "player1@local.test", UserRole.USER, false),
            new SeedUser("player2", "player2@local.test", UserRole.USER, false)
    );

    private final AppDevToolsProperties devToolsProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;

    public LocalDevUsersSeedConfig(
            AppDevToolsProperties devToolsProperties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ProfileService profileService
    ) {
        this.devToolsProperties = devToolsProperties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.profileService = profileService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!devToolsProperties.isEnabled()) {
            return;
        }

        int created = 0;
        int updated = 0;
        for (SeedUser seed : SEED_USERS) {
            Result result = upsert(seed);
            if (result == Result.CREATED) {
                created++;
            } else if (result == Result.UPDATED) {
                updated++;
            }
        }

        log.info(
                "Local test users ready (created={}, updated={}, password={})",
                created,
                updated,
                LOCAL_PASSWORD
        );
    }

    private Result upsert(SeedUser seed) {
        UserModel user = userRepository.findByEmail(seed.email())
                .or(() -> userRepository.findByUsername(seed.username()))
                .orElse(null);

        if (user == null) {
            user = new UserModel();
            user.setUsername(seed.username());
            user.setEmail(seed.email());
            user.setPasswordHash(passwordEncoder.encode(LOCAL_PASSWORD));
            user.setRole(seed.role());
            user.setDeveloper(seed.developer());
            user.setEmailVerifyTrue();
            UserModel saved = userRepository.save(user);
            profileService.ensureProfileWithStarters(saved.getIdUser());
            log.info(
                    "Seeded local user username={} email={} role={} developer={}",
                    seed.username(),
                    seed.email(),
                    seed.role(),
                    seed.developer()
            );
            return Result.CREATED;
        }

        boolean changed = false;
        if (user.getRole() != seed.role()) {
            user.setRole(seed.role());
            changed = true;
        }
        if (user.isDeveloper() != seed.developer()) {
            user.setDeveloper(seed.developer());
            changed = true;
        }
        if (!Boolean.TRUE.equals(user.getEmailVerify())) {
            user.setEmailVerifyTrue();
            changed = true;
        }
        if (!passwordEncoder.matches(LOCAL_PASSWORD, user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(LOCAL_PASSWORD));
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
            log.info(
                    "Synced local user username={} role={} developer={}",
                    user.getUsername(),
                    seed.role(),
                    seed.developer()
            );
        }

        profileService.ensureProfileWithStarters(user.getIdUser());
        return changed ? Result.UPDATED : Result.UNCHANGED;
    }

    private enum Result {
        CREATED,
        UPDATED,
        UNCHANGED
    }

    private record SeedUser(
            String username,
            String email,
            UserRole role,
            boolean developer
    ) {
    }
}
