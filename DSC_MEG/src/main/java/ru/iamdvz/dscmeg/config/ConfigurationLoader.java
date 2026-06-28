package ru.iamdvz.dscmeg.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ConfigurationLoader {

    private final JavaPlugin plugin;

    public ConfigurationLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public DscMegConfig load() {
        saveDefault();
        return new DscMegConfig(readConfig());
    }

    public DscMegConfig reload() {
        return new DscMegConfig(readConfig());
    }

    private FileConfiguration readConfig() {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
    }

    private void saveDefault() {
        plugin.saveDefaultConfig();
    }
}
