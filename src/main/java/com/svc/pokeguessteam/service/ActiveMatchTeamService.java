package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ActiveMatchTeamService {

    private final JdbcTemplate jdbcTemplate;

    public ActiveMatchTeamService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveTeams(ActiveMatchModel match) {
        if (match == null || match.getId() == null) {
            return;
        }
        deleteByMatchId(match.getId());
        saveTeam(match.getHostPlayer().getId(), match.getHostPlayer().getTeam());
        saveTeam(match.getOpponentPlayer().getId(), match.getOpponentPlayer().getTeam());
    }

    public List<Integer> loadTeam(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT pokedex_number FROM tb_active_match_player_team WHERE fk_active_match_player_id = ? ORDER BY slot_index ASC",
                (rs, rowNum) -> rs.getInt("pokedex_number"),
                playerId
        );
    }

    public List<Integer> loadTeamFromMatchPlayer(String playerId) {
        return new ArrayList<>(loadTeam(playerId));
    }

    public void deleteByMatchId(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return;
        }
        jdbcTemplate.update("""
                DELETE FROM tb_active_match_player_team
                WHERE fk_active_match_player_id IN (
                    SELECT pk_active_match_player_id
                    FROM tb_active_match_players
                    WHERE fk_active_match_id = ?
                )
                """, matchId);
    }

    private void saveTeam(String playerId, List<Integer> team) {
        if (playerId == null || team == null || team.isEmpty()) {
            return;
        }
        int slotIndex = 0;
        for (Integer pokedexNumber : team) {
            if (pokedexNumber == null) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO tb_active_match_player_team (fk_active_match_player_id, slot_index, pokedex_number) VALUES (?, ?, ?)",
                    playerId,
                    slotIndex++,
                    pokedexNumber
            );
        }
    }
}