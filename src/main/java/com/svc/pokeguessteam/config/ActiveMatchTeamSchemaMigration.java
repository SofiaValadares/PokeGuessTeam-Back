package com.svc.pokeguessteam.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Garante {@code tb_active_match_player_team} — equipas persistidas via JDBC, fora do Hibernate.
 */
@Component
public class ActiveMatchTeamSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ActiveMatchTeamSchemaMigration.class);

    private final JdbcTemplate jdbc;

    public ActiveMatchTeamSchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tableExists()) {
            return;
        }

        log.info("Creating tb_active_match_player_team");
        jdbc.execute("""
                CREATE TABLE tb_active_match_player_team (
                    fk_active_match_player_id VARCHAR(36) NOT NULL,
                    slot_index INTEGER NOT NULL,
                    pokedex_number INTEGER NOT NULL,
                    CONSTRAINT tb_active_match_player_team_pkey
                        PRIMARY KEY (fk_active_match_player_id, slot_index),
                    CONSTRAINT fk_active_match_player_team_player
                        FOREIGN KEY (fk_active_match_player_id)
                        REFERENCES tb_active_match_players (pk_active_match_player_id)
                        ON DELETE CASCADE
                )
                """);
    }

    private boolean tableExists() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'tb_active_match_player_team'
                """, Integer.class);
        return count != null && count > 0;
    }
}
