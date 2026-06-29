package ru.iamdvz.divizionsc.bind;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.database.DatabaseManager;

/**
 * Привязки хотбара, хранящиеся в БД (кросс-сервер через общую базу).
 * Синхронное чтение — из кэша (наполняется async при входе игрока), запись — async.
 */
public final class BindRepository {

    private final JavaPlugin plugin;
    private final DatabaseManager database;
    private final Map<UUID, PlayerBinds> cache = new ConcurrentHashMap<>();

    public BindRepository(JavaPlugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    public PlayerBinds load(UUID playerId) {
        return cache.computeIfAbsent(playerId, PlayerBinds::new);
    }

    public CompletableFuture<PlayerBinds> preload(UUID playerId) {
        return database.supplyAsync(connection -> {
            String[] slots = new String[PlayerBinds.HOTBAR_SIZE];
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT slot, def_id FROM " + database.table("binds") + " WHERE player_uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        int slot = rs.getInt("slot");
                        if (slot >= 0 && slot < PlayerBinds.HOTBAR_SIZE) {
                            slots[slot] = rs.getString("def_id");
                        }
                    }
                }
            }
            return new PlayerBinds(playerId, slots);
        }, new PlayerBinds(playerId));
    }

    /** Вызывать только из global/entity region-потока. */
    public void applyPreload(UUID playerId, PlayerBinds fromDb) {
        cache.compute(playerId, (id, existing) -> {
            if (existing == null) {
                return fromDb;
            }
            existing.mergeFromDb(fromDb.slotsCopy());
            return existing;
        });
    }

    public void save(PlayerBinds binds) {
        cache.put(binds.playerId(), binds);
        String[] slots = binds.slotsCopy();
        UUID playerId = binds.playerId();
        database.runAsync(connection -> {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM " + database.table("binds") + " WHERE player_uuid = ?")) {
                delete.setString(1, playerId.toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + database.table("binds") + " (player_uuid, slot, def_id) VALUES (?, ?, ?)")) {
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] != null && !slots[i].isBlank()) {
                        insert.setString(1, playerId.toString());
                        insert.setInt(2, i);
                        insert.setString(3, slots[i]);
                        insert.addBatch();
                    }
                }
                insert.executeBatch();
            }
        });
    }

    public void unload(UUID playerId) {
        cache.remove(playerId);
    }

    /** Однократная миграция старых binds/&lt;uuid&gt;.yml в БД. */
    public CompletableFuture<Void> migrateLegacyYaml() {
        File folder = new File(plugin.getDataFolder(), "binds");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return CompletableFuture.completedFuture(null);
        }
        return database.runAsync(connection -> {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + database.table("binds") + " (player_uuid, slot, def_id) VALUES (?, ?, ?)")) {
                for (File file : files) {
                    UUID playerId = parseUuid(file.getName());
                    if (playerId == null) {
                        continue;
                    }
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    for (int i = 0; i < PlayerBinds.HOTBAR_SIZE; i++) {
                        String defId = yaml.getString("slot-" + (i + 1));
                        if (defId != null && !defId.isBlank()) {
                            insert.setString(1, playerId.toString());
                            insert.setInt(2, i);
                            insert.setString(3, defId);
                            insert.addBatch();
                        }
                    }
                }
                insert.executeBatch();
            }
        }).whenComplete((ignored, error) -> {
            if (error == null) {
                File migrated = new File(plugin.getDataFolder(), "binds_migrated");
                if (!migrated.exists() && !folder.renameTo(migrated)) {
                    plugin.getLogger().warning("Failed to rename legacy binds folder to binds_migrated");
                }
            }
        });
    }

    private UUID parseUuid(String fileName) {
        try {
            return UUID.fromString(fileName.substring(0, fileName.length() - 4));
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    public Map<UUID, PlayerBinds> cache() {
        return cache;
    }
}
