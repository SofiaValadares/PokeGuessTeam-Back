package com.svc.pokeguessteam.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate {@code ddl-auto=update} não alarga CHECK enums existentes.
 * Garante que {@code FRIEND_BALL} é aceite em {@code tb_profile_inventory_items}.
 */
@Component
@Order(0)
public class FriendBallInventorySchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FriendBallInventorySchemaMigration.class);

    private static final String CONSTRAINT = "tb_profile_inventory_items_pokeball_type_check";
    private static final String ALLOWED =
            "CHECK (pokeball_type::text = ANY (ARRAY["
                    + "'POKE_BALL','GREAT_BALL','ULTRA_BALL','MASTER_BALL','FRIEND_BALL'"
                    + "]::text[]))";

    private final JdbcTemplate jdbc;

    public FriendBallInventorySchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer tableExists = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'tb_profile_inventory_items'
                """, Integer.class);
        if (tableExists == null || tableExists == 0) {
            return;
        }

        String definition = jdbc.query("""
                SELECT pg_get_constraintdef(oid)
                FROM pg_constraint
                WHERE conname = ?
                  AND conrelid = 'tb_profile_inventory_items'::regclass
                """, rs -> rs.next() ? rs.getString(1) : null, CONSTRAINT);

        if (definition != null && definition.contains("FRIEND_BALL")) {
            return;
        }

        log.info("Updating {} to allow FRIEND_BALL", CONSTRAINT);
        if (definition != null) {
            jdbc.execute("ALTER TABLE tb_profile_inventory_items DROP CONSTRAINT " + CONSTRAINT);
        }
        jdbc.execute("ALTER TABLE tb_profile_inventory_items ADD CONSTRAINT " + CONSTRAINT + " " + ALLOWED);
    }
}
