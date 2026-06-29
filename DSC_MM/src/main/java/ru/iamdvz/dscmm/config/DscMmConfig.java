package ru.iamdvz.dscmm.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class DscMmConfig {

    private final DebugSettings debug;
    private final DefaultsSettings defaults;
    private final MechanicDefaultsSettings mechanicDefaults;

    public DscMmConfig(FileConfiguration config) {
        debug = new DebugSettings(config.getBoolean("debug.log-failures", true));
        defaults = new DefaultsSettings(
                config.getDouble("defaults.mob-level", 1.0),
                (float) config.getDouble("defaults.skill-power", 1.0)
        );
        mechanicDefaults = new MechanicDefaultsSettings(
                config.getBoolean("mechanic.allow-helper", true),
                config.getBoolean("mechanic.check-permission", false),
                config.getBoolean("mechanic.apply-cooldown", false),
                config.getBoolean("mechanic.require-target", false),
                config.getString("mechanic.caster", "auto")
        );
    }

    public DebugSettings debug() {
        return debug;
    }

    public DefaultsSettings defaults() {
        return defaults;
    }

    public MechanicDefaultsSettings mechanicDefaults() {
        return mechanicDefaults;
    }

    public record DebugSettings(boolean logFailures) {
    }

    public record DefaultsSettings(double mobLevel, float skillPower) {
    }

    public record MechanicDefaultsSettings(
            boolean allowHelper,
            boolean checkPermission,
            boolean applyCooldown,
            boolean requireTarget,
            String casterMode
    ) {
    }
}
