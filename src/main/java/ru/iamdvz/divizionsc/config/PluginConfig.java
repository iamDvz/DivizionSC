package ru.iamdvz.divizionsc.config;

import ru.iamdvz.divizionsc.config.settings.Settings;

/**
 * Типизированный доступ к настройкам плагина. Фасад над {@link Settings} (Elytrium),
 * чтобы остальной код не обращался к dotted-path конфигурации напрямую.
 */
public final class PluginConfig {

    private final Settings settings;
    private final SkillBarConfig skillBar;

    public PluginConfig(Settings settings) {
        this.settings = settings;
        this.skillBar = new SkillBarConfig(settings.skillBar);
    }

    public Settings raw() {
        return settings;
    }

    public String locale() {
        return settings.locale;
    }

    public String defsFolder() {
        return settings.defsFolder;
    }

    public double defaultRange() {
        return settings.defaultRange;
    }

    public String adminPermission() {
        return settings.adminPermission;
    }

    public String castPermissionPrefix() {
        return settings.castPermissionPrefix;
    }

    public boolean cooldownMessages() {
        return settings.cooldownMessages;
    }

    public boolean castMessages() {
        return settings.castMessages;
    }

    public boolean debugLoadErrors() {
        return settings.debugLoadErrors;
    }

    public int listPageSize() {
        return settings.listPageSize;
    }

    public SkillBarConfig skillBar() {
        return skillBar;
    }

    public Settings.Database database() {
        return settings.database;
    }

    public boolean opBypassesCooldown(org.bukkit.entity.Player player) {
        Settings.OpBypass bypass = settings.opBypass;
        return bypass.enabled && player.isOp() && bypass.ignoreCooldown;
    }

    public boolean opBypassesMana(org.bukkit.entity.Player player) {
        Settings.OpBypass bypass = settings.opBypass;
        return bypass.enabled && player.isOp() && bypass.ignoreMana;
    }

    public boolean manaEnabled() {
        return settings.mana.enabled;
    }

    public double manaDefaultMax() {
        return settings.mana.defaultMax;
    }

    public double manaDefaultAmount() {
        return settings.mana.defaultAmount;
    }

    public int configVersion() {
        return settings.configVersion;
    }
}
