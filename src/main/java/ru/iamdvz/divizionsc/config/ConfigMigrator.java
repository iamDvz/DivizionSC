package ru.iamdvz.divizionsc.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * Обновляет config.yml при смене {@link #CURRENT_VERSION}: удаляет устаревшие ключи,
 * затем Elytrium {@link ru.iamdvz.divizionsc.config.settings.Settings#save} дописывает новые.
 */
public final class ConfigMigrator {

    public static final int CURRENT_VERSION = 1;

    private static final List<String> REMOVED_AT_V1 = List.of(
            "abilities-folder"
    );

    private ConfigMigrator() {
    }

    public static void migrate(JavaPlugin plugin, Path configPath) {
        if (!Files.exists(configPath)) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configPath.toFile());
        int version = yaml.getInt("config-version", 0);
        if (version >= CURRENT_VERSION) {
            return;
        }

        boolean changed = false;
        if (version < 1) {
            for (String key : REMOVED_AT_V1) {
                if (yaml.contains(key)) {
                    yaml.set(key, null);
                    changed = true;
                }
            }
            ConfigurationSection skillBar = yaml.getConfigurationSection("skill-bar");
            if (skillBar != null && skillBar.contains("cast-mode")) {
                skillBar.set("cast-mode", null);
                changed = true;
            }
        }

        yaml.set("config-version", CURRENT_VERSION);
        try {
            yaml.save(configPath.toFile());
            if (plugin != null) {
                plugin.getLogger().info("config.yml updated to version " + CURRENT_VERSION
                        + (changed ? " (removed obsolete keys)" : ""));
            }
        } catch (IOException exception) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "Failed to migrate config.yml", exception);
            }
        }
    }
}
