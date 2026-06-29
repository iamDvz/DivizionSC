package ru.iamdvz.dscmeg;

import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.api.DivizionSCApi;
import ru.iamdvz.dscmeg.bridge.VfxEffectHandler;
import ru.iamdvz.dscmeg.config.ConfigurationLoader;
import ru.iamdvz.dscmeg.config.DscMegConfig;

public final class DscMeg extends JavaPlugin {

    private MegContext context;

    @Override
    public void onEnable() {
        if (!DivizionSCApi.isAvailable()) {
            getLogger().severe("DivizionSC is required.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DscMegConfig config = new ConfigurationLoader(this).load();
        context = MegContext.create(this, config);
        context.spawnService().startMaintenance();
        DivizionSCApi.effectHandlers().register(new VfxEffectHandler(context));
        getLogger().info("ModelEngine VFX bridge enabled.");
    }

    @Override
    public void onDisable() {
        if (context != null) {
            try {
                if (DivizionSCApi.isAvailable()) {
                    DivizionSCApi.effectHandlers().unregister("vfx");
                }
            } catch (IllegalStateException ignored) {
            }
            context.shutdown();
            context = null;
        }
    }

    public void reloadAll() {
        DscMegConfig config = new ConfigurationLoader(this).reload();
        context.reload(config);
    }

    public MegContext context() {
        return context;
    }
}
