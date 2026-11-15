package com.svc.pokeguessteam.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Converte colunas PostgreSQL OID (LOB antigo do {@code @Lob}) para TEXT/BYTEA.
 * Com {@code open-in-view=false}, ler OID fora de transação causa 500
 * ("Objetos Grandes não podem ser usados no modo de efetivação automática").
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PostgresOidColumnRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PostgresOidColumnRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public PostgresOidColumnRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateDetailJsonToText();
        migrateAvatarBytesToBytea();
    }

    private void migrateDetailJsonToText() {
        if (!columnIsOid("tb_security_lab_event", "detail_json")) {
            return;
        }
        log.warn("Migrating tb_security_lab_event.detail_json from oid to text");
        jdbcTemplate.execute("ALTER TABLE tb_security_lab_event ADD COLUMN IF NOT EXISTS detail_json_text text");
        jdbcTemplate.execute(
                """
                        UPDATE tb_security_lab_event
                        SET detail_json_text = CASE
                          WHEN detail_json IS NULL THEN NULL
                          ELSE convert_from(lo_get(detail_json), 'UTF8')
                        END
                        WHERE detail_json_text IS NULL
                        """
        );
        jdbcTemplate.execute("ALTER TABLE tb_security_lab_event DROP COLUMN detail_json");
        jdbcTemplate.execute("ALTER TABLE tb_security_lab_event RENAME COLUMN detail_json_text TO detail_json");
        log.info("Migrated tb_security_lab_event.detail_json to text");
    }

    private void migrateAvatarBytesToBytea() {
        if (!columnIsOid("tb_profiles", "avatar_custom_bytes")) {
            return;
        }
        log.warn("Migrating tb_profiles.avatar_custom_bytes from oid to bytea");
        jdbcTemplate.execute("ALTER TABLE tb_profiles ADD COLUMN IF NOT EXISTS avatar_custom_bytes_bin bytea");
        jdbcTemplate.execute(
                """
                        UPDATE tb_profiles
                        SET avatar_custom_bytes_bin = CASE
                          WHEN avatar_custom_bytes IS NULL THEN NULL
                          ELSE lo_get(avatar_custom_bytes)
                        END
                        WHERE avatar_custom_bytes_bin IS NULL
                        """
        );
        jdbcTemplate.execute("ALTER TABLE tb_profiles DROP COLUMN avatar_custom_bytes");
        jdbcTemplate.execute("ALTER TABLE tb_profiles RENAME COLUMN avatar_custom_bytes_bin TO avatar_custom_bytes");
        log.info("Migrated tb_profiles.avatar_custom_bytes to bytea");
    }

    private boolean columnIsOid(String table, String column) {
        Boolean isOid = jdbcTemplate.query(
                """
                        SELECT EXISTS (
                          SELECT 1
                          FROM information_schema.columns
                          WHERE table_schema = 'public'
                            AND table_name = ?
                            AND column_name = ?
                            AND udt_name = 'oid'
                        )
                        """,
                rs -> rs.next() && rs.getBoolean(1),
                table,
                column
        );
        return Boolean.TRUE.equals(isOid);
    }
}
