package ru.iamdvz.divizionsc.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

/**
 * Проверяет, что DDL и REPLACE INTO совместимы с SQLite (драйвер в test-classpath).
 */
class DatabaseSchemaTest {

    @Test
    void schemaAndUpsertWorkOnSqlite() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS dsc_cooldowns ("
                        + "player_uuid VARCHAR(36) NOT NULL, def_id VARCHAR(64) NOT NULL, "
                        + "expires_at BIGINT NOT NULL, PRIMARY KEY (player_uuid, def_id))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS dsc_binds ("
                        + "player_uuid VARCHAR(36) NOT NULL, slot INT NOT NULL, def_id VARCHAR(64) NOT NULL, "
                        + "PRIMARY KEY (player_uuid, slot))");
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "REPLACE INTO dsc_cooldowns (player_uuid, def_id, expires_at) VALUES (?, ?, ?)")) {
                insert.setString(1, "p1");
                insert.setString(2, "fireball");
                insert.setLong(3, 1000L);
                insert.executeUpdate();
                insert.setLong(3, 2000L);
                insert.executeUpdate();
            }

            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT expires_at FROM dsc_cooldowns WHERE player_uuid = ? AND def_id = ?")) {
                query.setString(1, "p1");
                query.setString(2, "fireball");
                try (ResultSet rs = query.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(2000L, rs.getLong("expires_at"));
                }
            }
        }
    }
}
