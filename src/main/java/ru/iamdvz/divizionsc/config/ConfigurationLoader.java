package ru.iamdvz.divizionsc.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public final class ConfigurationLoader {

    private final JavaPlugin plugin;
    private final File configFile;

    public ConfigurationLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public PluginConfig load() {
        ensureDataFolder();
        FileConfiguration yaml;
        if (!configFile.exists()) {
            yaml = new YamlConfiguration();
            applyDefaults(yaml);
            save(yaml);
        } else {
            yaml = YamlConfiguration.loadConfiguration(configFile);
            if (mergeDefaults(yaml)) {
                save(yaml);
            }
        }
        return new PluginConfig(yaml);
    }

    public PluginConfig reload() {
        return load();
    }

    private void applyDefaults(FileConfiguration config) {
        config.options().header(
                "DivizionSC — глобальные настройки.\n"
                        + "Def задаются в defs/defs-*.yml (простой синтаксис, см. README)"
        );
        config.set("locale", "ru");
        config.set("defs-folder", "defs");
        config.set("default-range", 32.0);
        config.set("admin-permission", "divizionsc.admin");
        config.set("cast-permission-prefix", "divizionsc.def.");
        config.set("cooldown-messages", true);
        config.set("cast-messages", false);
        config.set("debug-load-errors", true);

        config.set("skill-bar.enabled", true);
        config.set("skill-bar.permission", "divizionsc.skills");
        config.set("skill-bar.gui-size", 54);
        config.set("skill-bar.list-start-slot", 0);
        config.set("skill-bar.list-page-size", 27);
        config.set("skill-bar.bind-row-start-slot", 36);
        config.set("skill-bar.bind-slot-count", 9);
        config.set("skill-bar.require-empty-hand", false);
        config.set("skill-bar.open-on-swap-hands", true);
        config.set("skill-bar.bind-triggers", SkillBarConfig.defaultBindTriggers());
    }

    private boolean mergeDefaults(FileConfiguration config) {
        FileConfiguration defaults = new YamlConfiguration();
        applyDefaults(defaults);
        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, defaults.get(key));
                changed = true;
            }
        }
        return changed;
    }

    private void ensureDataFolder() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Failed to create data folder.");
        }
    }

    private void save(FileConfiguration config) {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save config.yml", e);
        }
    }
}
