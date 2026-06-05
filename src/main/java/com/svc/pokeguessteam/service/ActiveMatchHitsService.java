package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Acertos persistidos fora do {@code @ElementCollection} do Hibernate — evita inserts duplicados
 * quando a coleção em memória fica dessincronizada da BD.
 */
@Service
public class ActiveMatchHitsService {

    private final JdbcTemplate jdbc;

    public ActiveMatchHitsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void attachHits(ActiveMatchModel match) {
        if (match == null) {
            return;
        }
        attachHits(match.getHostPlayer());
        attachHits(match.getOpponentPlayer());
    }

    public void attachHits(ActiveMatchPlayerModel player) {
        if (player == null || player.getId() == null) {
            return;
        }
        player.loadHits(findHits(player.getId()));
    }

    public Set<Integer> findHits(String playerId) {
        List<Integer> rows = jdbc.queryForList(
                """
                        SELECT pokedex_number
                        FROM tb_active_match_player_hits
                        WHERE fk_active_match_player_id = ?
                        """,
                Integer.class,
                playerId
        );
        return new HashSet<>(rows);
    }

    public void persistHits(ActiveMatchModel match) {
        if (match == null) {
            return;
        }
        persistHits(match.getHostPlayer());
        persistHits(match.getOpponentPlayer());
    }

    public void persistHits(ActiveMatchPlayerModel player) {
        if (player == null || player.getId() == null) {
            return;
        }
        for (int dex : player.getHits()) {
            insertIgnore(player.getId(), dex);
        }
    }

    private void insertIgnore(String playerId, int pokedexNumber) {
        jdbc.update(
                """
                        INSERT INTO tb_active_match_player_hits (fk_active_match_player_id, pokedex_number)
                        VALUES (?, ?)
                        ON CONFLICT (fk_active_match_player_id, pokedex_number) DO NOTHING
                        """,
                playerId,
                pokedexNumber
        );
    }
}
