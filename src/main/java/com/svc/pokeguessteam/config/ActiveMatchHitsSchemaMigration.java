package com.svc.pokeguessteam.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Garante {@code tb_active_match_player_hits} — acertos persistidos via JDBC, fora do Hibernate.
 */
@Component
public class ActiveMatchHitsSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ActiveMatchHitsSchemaMigration.class);

    private final JdbcTemplate jdbc;

    public ActiveMatchHitsSchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists()) {
            log.info("Creating tb_active_match_player_hits");
            jdbc.execute("""
                    CREATE TABLE tb_active_match_player_hits (
                        fk_active_match_player_id VARCHAR(36) NOT NULL,
                        pokedex_number INTEGER NOT NULL,
                        CONSTRAINT tb_active_match_player_hits_pkey
                            PRIMARY KEY (fk_active_match_player_id, pokedex_number),
                        CONSTRAINT fk_active_match_player_hits_player
                            FOREIGN KEY (fk_active_match_player_id)
                            REFERENCES tb_active_match_players (pk_active_match_player_id)
                            ON DELETE CASCADE
                    )
                    """);
            return;
        }

        Integer hasHitOrder = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'tb_active_match_player_hits'
                  AND column_name = 'hit_order'
                """, Integer.class);

        if (hasHitOrder != null && hasHitOrder > 0) {
            log.info("Migrating tb_active_match_player_hits: removing hit_order column");
            jdbc.execute("ALTER TABLE tb_active_match_player_hits DROP CONSTRAINT IF EXISTS tb_active_match_player_hits_pkey");
            jdbc.execute("""
                    DELETE FROM tb_active_match_player_hits a
                    USING tb_active_match_player_hits b
                    WHERE a.ctid < b.ctid
                      AND a.fk_active_match_player_id = b.fk_active_match_player_id
                      AND a.pokedex_number = b.pokedex_number
                    """);
            jdbc.execute("ALTER TABLE tb_active_match_player_hits DROP COLUMN hit_order");
        }

        Integer pkOnPokedex = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.key_column_usage
                WHERE table_schema = 'public'
                  AND table_name = 'tb_active_match_player_hits'
                  AND constraint_name = 'tb_active_match_player_hits_pkey'
                  AND column_name = 'pokedex_number'
                """, Integer.class);

        if (pkOnPokedex == null || pkOnPokedex == 0) {
            log.info("Migrating tb_active_match_player_hits: ensuring PK (player_id, pokedex_number)");
            jdbc.execute("ALTER TABLE tb_active_match_player_hits DROP CONSTRAINT IF EXISTS tb_active_match_player_hits_pkey");
            jdbc.execute("""
                    DELETE FROM tb_active_match_player_hits a
                    USING tb_active_match_player_hits b
                    WHERE a.ctid < b.ctid
                      AND a.fk_active_match_player_id = b.fk_active_match_player_id
                      AND a.pokedex_number = b.pokedex_number
                    """);
            jdbc.execute("""
                    ALTER TABLE tb_active_match_player_hits
                    ADD CONSTRAINT tb_active_match_player_hits_pkey
                    PRIMARY KEY (fk_active_match_player_id, pokedex_number)
                    """);
        }
    }

    private boolean tableExists() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'tb_active_match_player_hits'
                """, Integer.class);
        return count != null && count > 0;
    }
}
