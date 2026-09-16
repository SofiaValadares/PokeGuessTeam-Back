package com.svc.pokeguessteam.repository.audit;

import com.svc.pokeguessteam.model.audit.AuditLogModel;
import com.svc.pokeguessteam.model.enums.AuditLogCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogModel, String> {

    Page<AuditLogModel> findByCategoryInOrderByCreatedAtDesc(
            Collection<AuditLogCategory> categories,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM AuditLogModel a
            WHERE a.category IN :categories
              AND (
                :q = ''
                OR LOWER(COALESCE(a.actorUsername, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.actorEmail, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.actorUserId, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.path, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.action, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.detail, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLogModel> searchSystemLogs(
            @Param("categories") Collection<AuditLogCategory> categories,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM AuditLogModel a
            WHERE a.actorUserId = :userId
              AND (
                :q = ''
                OR LOWER(COALESCE(a.path, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.action, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.detail, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.httpMethod, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLogModel> searchByActorUserId(
            @Param("userId") String userId,
            @Param("q") String q,
            Pageable pageable
    );

    long countByActorUserId(String actorUserId);

    @Query("""
            SELECT a.actorUserId, COUNT(a)
            FROM AuditLogModel a
            WHERE a.actorUserId IS NOT NULL
            GROUP BY a.actorUserId
            """)
    List<Object[]> countGroupedByActorUserId();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AuditLogModel a WHERE a.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
