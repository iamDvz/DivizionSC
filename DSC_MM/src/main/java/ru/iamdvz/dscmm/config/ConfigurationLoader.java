package ru.iamdvz.dscmm.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ConfigurationLoader {

    private final JavaPlugin plugin;

    public ConfigurationLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public DscMmConfig load() {
        saveDefault();
        return new DscMmConfig(readConfig());
    }

    public DscMmConfig reload() {
        return new DscMmConfig(readConfig());
    }

    private FileConfiguration readConfig() {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
    }

    private void saveDefault() {
        plugin.saveDefaultConfig();
    }
}
