package com.svc.pokeguessteam.repository.user;

import com.svc.pokeguessteam.model.user.FriendOnlinePenaltyModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FriendOnlinePenaltyRepository extends JpaRepository<FriendOnlinePenaltyModel, String> {

    @Query("""
            SELECT p FROM FriendOnlinePenaltyModel p
            WHERE p.profile.id = :profileId
              AND p.occurredAt >= :since
            ORDER BY p.occurredAt DESC
            """)
    List<FriendOnlinePenaltyModel> findRecentByProfileId(
            @Param("profileId") String profileId,
            @Param("since") LocalDateTime since
    );

    long countByProfile_IdAndOccurredAtAfter(String profileId, LocalDateTime since);

    void deleteByProfile_Id(String profileId);
}
