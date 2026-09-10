package com.svc.pokeguessteam.repository.auth;

import com.svc.pokeguessteam.model.auth.AuthCodeModel;
import com.svc.pokeguessteam.model.auth.AuthCodePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthCodeRepository extends JpaRepository<AuthCodeModel, String> {

    List<AuthCodeModel> findByUser_IdUser(String userId);

    Optional<AuthCodeModel> findFirstByUser_IdUserAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String userId,
            AuthCodePurpose purpose
    );

    @Modifying
    @Query("""
            update AuthCodeModel c
            set c.consumedAt = :now
            where c.user.idUser = :userId
              and c.purpose = :purpose
              and c.consumedAt is null
            """)
    void consumeActiveCodes(
            @Param("userId") String userId,
            @Param("purpose") AuthCodePurpose purpose,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AuthCodeModel c where c.user.idUser = :userId")
    void deleteAllByUserId(@Param("userId") String userId);
}
