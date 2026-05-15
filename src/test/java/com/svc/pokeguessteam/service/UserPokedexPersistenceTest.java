package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.repository.user.UserPokedexRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UserPokedexPersistenceTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserPokedexRepository userPokedexRepository;

    @Autowired
    private com.svc.pokeguessteam.repository.user.UserRepository userRepository;

    @Test
    @Transactional
    void ensureProfilePersistsStarterPokedexEntries() {
        var user = userRepository.findAll().stream().findFirst().orElseThrow();
        profileService.ensureProfileWithStarters(user.getIdUser());

        long count = userPokedexRepository.findAll().stream()
                .filter(e -> e.isRegistered())
                .count();
        assertTrue(count >= 27, "expected starter species in TB_USER_POKEDEX, found " + count);
    }
}
