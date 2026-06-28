package ru.iamdvz.dscmeg.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class DscMegConfig {

    private final DebugSettings debug;
    private final CleanupSettings cleanup;
    private final DefaultsSettings defaults;
    private final LimitsSettings limits;

    public DscMegConfig(FileConfiguration config) {
        debug = new DebugSettings(
                config.getBoolean("debug.log-spawn-failures", true),
                config.getBoolean("debug.validate-animations", true)
        );
        cleanup = new CleanupSettings(config.getString("cleanup.mob-tag", "DSC_MEG_MOB"));
        defaults = new DefaultsSettings(
                config.getLong("defaults.remove-delay", -1L),
                config.getDouble("defaults.model-scale", 1.0),
                config.getBoolean("defaults.shadow", true),
                config.getBoolean("defaults.glowing", false),
                config.getString("defaults.model-color", "ffffff"),
                config.getBoolean("defaults.remove-on-host-death", false)
        );
        limits = new LimitsSettings(
                config.getInt("limits.max-active-per-world", 200),
                config.getDouble("limits.max-follow-distance", 64.0),
                config.getInt("limits.follow-interval-ticks", 2),
                config.getInt("limits.follow-far-interval-ticks", 5),
                config.getDouble("limits.follow-far-distance", 32.0),
                config.getBoolean("limits.notify-on-world-limit", false)
        );
    }

    public DebugSettings debug() {
        return debug;
    }

    public CleanupSettings cleanup() {
        return cleanup;
    }

    public DefaultsSettings defaults() {
        return defaults;
    }

    public LimitsSettings limits() {
        return limits;
    }

    public record DebugSettings(boolean logSpawnFailures, boolean validateAnimations) {
    }

    public record CleanupSettings(String mobTag) {
    }

    public record DefaultsSettings(
            long removeDelay,
            double modelScale,
            boolean shadow,
            boolean glowing,
            String modelColor,
            boolean removeOnHostDeath
    ) {
    }

    public record LimitsSettings(
            int maxActivePerWorld,
            double maxFollowDistance,
            int followIntervalTicks,
            int followFarIntervalTicks,
            double followFarDistance,
            boolean notifyOnWorldLimit
    ) {
    }
}
