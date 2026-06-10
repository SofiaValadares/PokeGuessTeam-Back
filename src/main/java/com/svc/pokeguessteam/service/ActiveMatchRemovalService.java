package com.svc.pokeguessteam.service;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remove partidas ativas na ordem correta — o Hibernate não garante cascade com
 * {@code @OneToOne(mappedBy)} + {@code @ManyToOne} no filho. Penalidades online
 * mantêm o registo histórico, mas a FK para a partida é anulada antes do DELETE.
 */
@Service
public class ActiveMatchRemovalService {

    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public ActiveMatchRemovalService(JdbcTemplate jdbc, EntityManager entityManager) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    @Transactional
    public void deleteByMatchId(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return;
        }

        entityManager.flush();
        jdbc.update("""
                DELETE FROM tb_active_match_player_hits
                WHERE fk_active_match_player_id IN (
                    SELECT pk_active_match_player_id
                    FROM tb_active_match_players
                    WHERE fk_active_match_id = ?
                )
                """, matchId);

        jdbc.update("""
                DELETE FROM tb_active_match_player_team
                WHERE fk_active_match_player_id IN (
                    SELECT pk_active_match_player_id
                    FROM tb_active_match_players
                    WHERE fk_active_match_id = ?
                )
                """, matchId);

        jdbc.update("""
                DELETE FROM tb_active_match_guess_matches
                WHERE fk_active_match_guess_id IN (
                    SELECT pk_active_match_guess_id
                    FROM tb_active_match_guesses
                    WHERE fk_active_match_id = ?
                )
                """, matchId);

        jdbc.update("DELETE FROM tb_active_match_guesses WHERE fk_active_match_id = ?", matchId);
        jdbc.update("DELETE FROM tb_active_match_players WHERE fk_active_match_id = ?", matchId);
        jdbc.update(
                "UPDATE tb_friend_online_penalties SET fk_active_match_id = NULL WHERE fk_active_match_id = ?",
                matchId
        );
        jdbc.update("DELETE FROM tb_active_matches WHERE pk_active_match_id = ?", matchId);

        entityManager.clear();
    }
}
