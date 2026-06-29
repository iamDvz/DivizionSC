package ru.iamdvz.dscmeg;

import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.dscmeg.config.DscMegConfig;
import ru.iamdvz.dscmeg.service.VfxSpawnService;
import ru.iamdvz.dscmeg.service.VfxTracker;

public final class MegContext {

    private final JavaPlugin plugin;
    private DscMegConfig config;
    private final VfxTracker vfxTracker;
    private final VfxSpawnService spawnService;

    private MegContext(JavaPlugin plugin, DscMegConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.vfxTracker = new VfxTracker(config.limits());
        this.spawnService = new VfxSpawnService(plugin, this);
    }

    public static MegContext create(JavaPlugin plugin, DscMegConfig config) {
        return new MegContext(plugin, config);
    }

    public void reload(DscMegConfig newConfig) {
        config = newConfig;
        vfxTracker.updateSettings(newConfig.limits());
    }

    public void shutdown() {
        spawnService.stopMaintenance();
        for (var session : vfxTracker.activeSessions().values()) {
            spawnService.cleanup(session);
        }
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public DscMegConfig config() {
        return config;
    }

    public VfxTracker vfxTracker() {
        return vfxTracker;
    }

    public VfxSpawnService spawnService() {
        return spawnService;
    }
}
