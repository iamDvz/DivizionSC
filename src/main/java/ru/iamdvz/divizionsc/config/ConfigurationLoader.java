package ru.iamdvz.divizionsc.config;

import java.io.File;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.config.settings.Settings;

/**
 * Загружает типизированный config.yml через Elytrium Serializer.
 * При загрузке: миграция по config-version → merge дефолтов → save (новые ключи + комментарии).
 */
public final class ConfigurationLoader {

    private final JavaPlugin plugin;
    private final Path configPath;

    public ConfigurationLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configPath = new File(plugin.getDataFolder(), "config.yml").toPath();
    }

    public PluginConfig load() {
        ensureDataFolder();
        ConfigMigrator.migrate(plugin, configPath);
        Settings settings = new Settings();
        settings.reload(configPath);
        settings.configVersion = ConfigMigrator.CURRENT_VERSION;
        settings.save(configPath);
        return new PluginConfig(settings);
    }

    public PluginConfig reload() {
        return load();
    }

    private void ensureDataFolder() {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Failed to create data folder.");
        }
    }
}
