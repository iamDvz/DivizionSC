package ru.iamdvz.divizionsc.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class PluginConfig {

    private final FileConfiguration yaml;

    public PluginConfig(FileConfiguration yaml) {
        this.yaml = yaml;
    }

    public String locale() {
        return yaml.getString("locale", "ru");
    }

    public String defsFolder() {
        return yaml.getString("defs-folder", yaml.getString("abilities-folder", "defs"));
    }

    public double defaultRange() {
        return yaml.getDouble("default-range", 32.0);
    }

    public String adminPermission() {
        return yaml.getString("admin-permission", "divizionsc.admin");
    }

    public String castPermissionPrefix() {
        return yaml.getString("cast-permission-prefix", "divizionsc.def.");
    }

    public boolean cooldownMessages() {
        return yaml.getBoolean("cooldown-messages", true);
    }

    public boolean castMessages() {
        return yaml.getBoolean("cast-messages", false);
    }

    public boolean debugLoadErrors() {
        return yaml.getBoolean("debug-load-errors", true);
    }

    public int listPageSize() {
        return yaml.getInt("list-page-size", 15);
    }

    public SkillBarConfig skillBar() {
        return new SkillBarConfig(yaml);
    }
}
