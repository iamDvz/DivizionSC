package ru.iamdvz.divizionsc.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Персистентные кулдауны (кросс-сервер через общую БД).
 */
public final class CooldownRepository {

    private final DatabaseManager database;

    public CooldownRepository(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<Map<String, Long>> load(UUID playerId) {
        return database.supplyAsync(connection -> {
            Map<String, Long> result = new HashMap<>();
            long now = System.currentTimeMillis();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT def_id, expires_at FROM " + database.table("cooldowns") + " WHERE player_uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        long expiresAt = rs.getLong("expires_at");
                        if (expiresAt > now) {
                            result.put(rs.getString("def_id"), expiresAt);
                        }
                    }
                }
            }
            return result;
        }, new HashMap<>());
    }

    public void upsert(UUID playerId, String defId, long expiresAt) {
        database.runAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "REPLACE INTO " + database.table("cooldowns")
                            + " (player_uuid, def_id, expires_at) VALUES (?, ?, ?)")) {
                statement.setString(1, playerId.toString());
                statement.setString(2, defId);
                statement.setLong(3, expiresAt);
                statement.executeUpdate();
            }
        });
    }

    public void remove(UUID playerId, String defId) {
        database.runAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + database.table("cooldowns") + " WHERE player_uuid = ? AND def_id = ?")) {
                statement.setString(1, playerId.toString());
                statement.setString(2, defId);
                statement.executeUpdate();
            }
        });
    }
}
