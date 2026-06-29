package ru.iamdvz.dscmm;

import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.dscmm.config.DscMmConfig;
import ru.iamdvz.dscmm.service.MythicMobService;

public final class MmContext {

    private final JavaPlugin plugin;
    private DscMmConfig config;
    private final MythicMobService mythicMobService;

    private MmContext(JavaPlugin plugin, DscMmConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.mythicMobService = new MythicMobService(plugin, this);
    }

    public static MmContext create(JavaPlugin plugin, DscMmConfig config) {
        return new MmContext(plugin, config);
    }

    public void reload(DscMmConfig newConfig) {
        config = newConfig;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public DscMmConfig config() {
        return config;
    }

    public MythicMobService mythicMobService() {
        return mythicMobService;
    }
}
