package ru.iamdvz.divizionsc.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.iamdvz.divizionsc.config.settings.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigratorTest {

    @TempDir
    Path tempDir;

    @Test
    void removesObsoleteKeysAndBumpsVersion() throws IOException {
        Path configPath = tempDir.resolve("config.yml");
        Files.writeString(configPath, """
                locale: en
                abilities-folder: abilities
                skill-bar:
                  cast-mode: right_click
                  enabled: true
                """);

        ConfigMigrator.migrate(null, configPath);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configPath.toFile());
        assertEquals(ConfigMigrator.CURRENT_VERSION, yaml.getInt("config-version"));
        assertFalse(yaml.contains("abilities-folder"));
        assertFalse(yaml.getConfigurationSection("skill-bar").contains("cast-mode"));
        assertTrue(yaml.getBoolean("skill-bar.enabled"));
    }

    @Test
    void saveAddsMissingKeysFromSettings() throws IOException {
        Path configPath = tempDir.resolve("config.yml");
        Files.writeString(configPath, """
                config-version: 0
                locale: ru
                """);

        ConfigMigrator.migrate(null, configPath);

        Settings settings = new Settings();
        settings.reload(configPath);
        settings.configVersion = ConfigMigrator.CURRENT_VERSION;
        settings.save(configPath);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configPath.toFile());
        assertTrue(yaml.contains("op-bypass"));
        assertTrue(yaml.contains("mana"));
        assertTrue(yaml.getBoolean("op-bypass.ignore-cooldown"));
        assertTrue(yaml.getBoolean("mana.enabled"));
    }
}
