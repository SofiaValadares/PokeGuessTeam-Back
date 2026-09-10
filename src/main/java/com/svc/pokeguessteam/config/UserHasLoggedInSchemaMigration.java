package com.svc.pokeguessteam.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Garante {@code user_has_logged_in} em {@code TB_USERS} para o indicador de primeiro login.
 */
@Component
@Order(0)
public class UserHasLoggedInSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserHasLoggedInSchemaMigration.class);

    private final JdbcTemplate jdbc;

    public UserHasLoggedInSchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer columnExists = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'tb_users'
                  AND column_name = 'user_has_logged_in'
                """, Integer.class);

        if (columnExists == null || columnExists == 0) {
            log.info("Adding user_has_logged_in to TB_USERS");
            jdbc.execute("""
                    ALTER TABLE tb_users
                    ADD COLUMN user_has_logged_in BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            return;
        }

        jdbc.execute("""
                UPDATE tb_users
                SET user_has_logged_in = TRUE
                WHERE user_has_logged_in IS NULL
                """);
    }
}
