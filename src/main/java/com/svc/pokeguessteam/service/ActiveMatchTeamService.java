package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Equipa persistida via JDBC — evita inserts duplicados no {@code @ElementCollection}
 * quando a coleção em memória é substituída.
 */
@Service
public class ActiveMatchTeamService {

    private final JdbcTemplate jdbc;

    public ActiveMatchTeamService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void attachTeams(ActiveMatchModel match) {
        if (match == null) {
            return;
        }
        attachTeam(match.getHostPlayer());
        attachTeam(match.getOpponentPlayer());
    }

    public void attachTeam(ActiveMatchPlayerModel player) {
        if (player == null || player.getId() == null) {
            return;
        }
        player.loadTeam(findTeam(player.getId()));
    }

    public List<Integer> findTeam(String playerId) {
        return new ArrayList<>(jdbc.queryForList(
                """
                        SELECT pokedex_number
                        FROM tb_active_match_player_team
                        WHERE fk_active_match_player_id = ?
                        ORDER BY slot_index
                        """,
                Integer.class,
                playerId
        ));
    }

    public void persistTeams(ActiveMatchModel match) {
        if (match == null) {
            return;
        }
        persistTeam(match.getHostPlayer());
        persistTeam(match.getOpponentPlayer());
    }

    public void persistTeam(ActiveMatchPlayerModel player) {
        if (player == null || player.getId() == null) {
            return;
        }
        replaceTeam(player.getId(), player.getTeam());
    }

    public void replaceTeam(String playerId, List<Integer> team) {
        jdbc.update(
                "DELETE FROM tb_active_match_player_team WHERE fk_active_match_player_id = ?",
                playerId
        );
        if (team == null) {
            return;
        }
        for (int slot = 0; slot < team.size(); slot++) {
            jdbc.update(
                    """
                            INSERT INTO tb_active_match_player_team
                                (fk_active_match_player_id, slot_index, pokedex_number)
                            VALUES (?, ?, ?)
                            """,
                    playerId,
                    slot,
                    team.get(slot)
            );
        }
    }
}
