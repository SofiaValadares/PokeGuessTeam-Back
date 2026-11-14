package com.svc.pokeguessteam.repository.securitylab;

import com.svc.pokeguessteam.model.enums.SecurityLabEventType;
import com.svc.pokeguessteam.model.securitylab.SecurityLabEventModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SecurityLabEventRepository extends JpaRepository<SecurityLabEventModel, String> {

    long countByEventTypeAndCreatedAtAfter(SecurityLabEventType eventType, LocalDateTime after);

    List<SecurityLabEventModel> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after, Pageable pageable);

    List<SecurityLabEventModel> findByEventTypeInAndCreatedAtAfterOrderByCreatedAtDesc(
            Collection<SecurityLabEventType> types,
            LocalDateTime after,
            Pageable pageable
    );

    @Query("""
            SELECT e.eventType, COUNT(e)
            FROM SecurityLabEventModel e
            WHERE e.createdAt >= :after
            GROUP BY e.eventType
            """)
    List<Object[]> countByTypeSince(@Param("after") LocalDateTime after);

    @Query("""
            SELECT e FROM SecurityLabEventModel e
            WHERE (:type IS NULL OR e.eventType = :type)
              AND (:developerOnly = false OR e.actorWasDeveloper = true)
              AND (
                :q = ''
                OR LOWER(COALESCE(e.actorUsername, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.summary, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY e.createdAt DESC
            """)
    Page<SecurityLabEventModel> searchAttacks(
            @Param("type") SecurityLabEventType type,
            @Param("developerOnly") boolean developerOnly,
            @Param("q") String q,
            Pageable pageable
    );
}
