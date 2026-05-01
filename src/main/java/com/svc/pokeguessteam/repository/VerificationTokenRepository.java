package com.svc.pokeguessteam.repository;

import com.svc.pokeguessteam.model.VerificationTokenModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationTokenModel, String> {
    Optional<VerificationTokenModel> findByToken(String token);
}