package com.svc.pokeguessteam.repository.user;

import com.svc.pokeguessteam.model.enums.UserRole;
import com.svc.pokeguessteam.model.user.UserModel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserModel, String> {

    Optional<UserModel> findByEmail(String email);

    Optional<UserModel> findByUsername(String username);

    List<UserModel> findByUsernameContainingIgnoreCaseOrderByUsernameAsc(
            String username,
            Pageable pageable
    );

    boolean existsByRole(UserRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from UserModel u
            where u.role = :role
            order by u.idUser
            """)
    List<UserModel> findAllByRoleForUpdate(@Param("role") UserRole role);
}