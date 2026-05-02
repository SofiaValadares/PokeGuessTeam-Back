package com.svc.pokeguessteam.repository.user;

import com.svc.pokeguessteam.model.user.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserModel, String> {
    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByUsername(String username);
}