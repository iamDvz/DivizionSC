package ru.iamdvz.divizionsc.bind;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class BindRepository {

    private final JavaPlugin plugin;
    private final File folder;
    private final Map<UUID, PlayerBinds> cache = new HashMap<>();

    public BindRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "binds");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public PlayerBinds load(UUID playerId) {
        return cache.computeIfAbsent(playerId, this::loadFromDisk);
    }

    public void save(PlayerBinds binds) {
        cache.put(binds.playerId(), binds);
        File file = fileFor(binds.playerId());
        FileConfiguration yaml = new YamlConfiguration();
        for (int i = 0; i < PlayerBinds.HOTBAR_SIZE; i++) {
            String defId = binds.get(i);
            if (defId != null && !defId.isBlank()) {
                yaml.set("slot-" + (i + 1), defId);
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save binds for " + binds.playerId(), e);
        }
    }

    public void unload(UUID playerId) {
        PlayerBinds binds = cache.remove(playerId);
        if (binds != null) {
            save(binds);
        }
    }

    private PlayerBinds loadFromDisk(UUID playerId) {
        File file = fileFor(playerId);
        if (!file.exists()) {
            return new PlayerBinds(playerId);
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String[] slots = new String[PlayerBinds.HOTBAR_SIZE];
        for (int i = 0; i < PlayerBinds.HOTBAR_SIZE; i++) {
            slots[i] = yaml.getString("slot-" + (i + 1));
        }
        return new PlayerBinds(playerId, slots);
    }

    private File fileFor(UUID playerId) {
        return new File(folder, playerId + ".yml");
    }
}
