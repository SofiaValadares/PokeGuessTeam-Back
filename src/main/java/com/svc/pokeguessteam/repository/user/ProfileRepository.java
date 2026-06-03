package com.svc.pokeguessteam.repository.user;

import com.svc.pokeguessteam.model.user.ProfileModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileModel, String> {
    Optional<ProfileModel> findByUser_IdUser(String userId);
}
