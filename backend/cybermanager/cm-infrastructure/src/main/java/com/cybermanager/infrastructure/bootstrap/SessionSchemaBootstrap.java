package com.cybermanager.infrastructure.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SessionSchemaBootstrap {
    @Bean
    CommandLineRunner bootstrapSessionSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("""
                    ALTER TABLE IF EXISTS cybermanager.cm_sessions
                    ADD COLUMN IF NOT EXISTS paused_at TIMESTAMP NULL
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE IF EXISTS cybermanager.cm_sessions
                    ADD COLUMN IF NOT EXISTS paused_minutes INTEGER
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE IF EXISTS cybermanager.cm_sessions
                    ADD COLUMN IF NOT EXISTS paid BOOLEAN
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE IF EXISTS cybermanager.cm_sessions
                    ADD COLUMN IF NOT EXISTS paused_seconds INTEGER
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE IF EXISTS cybermanager.cm_sessions
                    ADD COLUMN IF NOT EXISTS consumed_seconds INTEGER
                    """);
            jdbcTemplate.execute("""
                    UPDATE cybermanager.cm_sessions
                    SET paused_minutes = COALESCE(paused_minutes, 0),
                        paid = COALESCE(paid, FALSE),
                        paused_seconds = COALESCE(paused_seconds, COALESCE(paused_minutes, 0) * 60),
                        consumed_seconds = COALESCE(consumed_seconds, COALESCE(consumed_minutes, 0) * 60),
                        consumed_minutes = COALESCE(consumed_minutes, CASE
                            WHEN COALESCE(consumed_seconds, 0) = 0 THEN 0
                            ELSE ((COALESCE(consumed_seconds, 0) + 59) / 60)
                        END)
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE cybermanager.cm_sessions
                    ALTER COLUMN paused_minutes SET DEFAULT 0
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE cybermanager.cm_sessions
                    ALTER COLUMN paid SET DEFAULT FALSE
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE cybermanager.cm_sessions
                    ALTER COLUMN paused_seconds SET DEFAULT 0
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE cybermanager.cm_sessions
                    ALTER COLUMN consumed_seconds SET DEFAULT 0
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE cybermanager.cm_sessions
                    ALTER COLUMN paused_minutes SET NOT NULL
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE cybermanager.cm_sessions
                    ALTER COLUMN paid SET NOT NULL
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE cybermanager.cm_sessions
                    ALTER COLUMN paused_seconds SET NOT NULL
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE cybermanager.cm_sessions
                    ALTER COLUMN consumed_seconds SET NOT NULL
                    """);
        };
    }
}
