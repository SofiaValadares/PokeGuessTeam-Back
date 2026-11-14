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
 * Converte {@code detail_json} de OID (LOB PostgreSQL) para TEXT.
 * O mapeamento antigo {@code @Lob} + {@code open-in-view=false} causava 500 em
 * {@code GET /api/admin/attack-reports}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityLabDetailJsonRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityLabDetailJsonRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public SecurityLabDetailJsonRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Boolean isOid = jdbcTemplate.query(
                """
                        SELECT EXISTS (
                          SELECT 1
                          FROM information_schema.columns
                          WHERE table_schema = 'public'
                            AND table_name = 'tb_security_lab_event'
                            AND column_name = 'detail_json'
                            AND udt_name = 'oid'
                        )
                        """,
                rs -> rs.next() && rs.getBoolean(1)
        );
        if (!Boolean.TRUE.equals(isOid)) {
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
}
