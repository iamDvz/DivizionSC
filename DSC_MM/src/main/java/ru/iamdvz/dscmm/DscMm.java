package ru.iamdvz.dscmm;

import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.api.DivizionSCApi;
import ru.iamdvz.divizionsc.api.EffectHandler;
import ru.iamdvz.dscmm.bridge.AliasEffectHandler;
import ru.iamdvz.dscmm.bridge.MythicMobEffectHandler;
import ru.iamdvz.dscmm.bridge.MythicSkillEffectHandler;
import ru.iamdvz.dscmm.config.ConfigurationLoader;
import ru.iamdvz.dscmm.config.DscMmConfig;
import ru.iamdvz.dscmm.listener.MythicMechanicListener;

import java.util.ArrayList;
import java.util.List;

public final class DscMm extends JavaPlugin {

    private MmContext context;
    private final List<String> registeredTypes = new ArrayList<>();

    @Override
    public void onEnable() {
        if (!DivizionSCApi.isAvailable()) {
            getLogger().severe("DivizionSC is required.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DscMmConfig config = new ConfigurationLoader(this).load();
        context = MmContext.create(this, config);
        registerHandlers();
        getServer().getPluginManager().registerEvents(new MythicMechanicListener(context), this);
        getLogger().info("MythicMobs bridge enabled.");
    }

    @Override
    public void onDisable() {
        if (!registeredTypes.isEmpty()) {
            try {
                if (DivizionSCApi.isAvailable()) {
                    for (String type : registeredTypes) {
                        DivizionSCApi.effectHandlers().unregister(type);
                    }
                }
            } catch (IllegalStateException ignored) {
            }
            registeredTypes.clear();
        }
        context = null;
    }

    public void reloadAll() {
        DscMmConfig config = new ConfigurationLoader(this).reload();
        context.reload(config);
    }

    public MmContext context() {
        return context;
    }

    private void registerHandlers() {
        MythicMobEffectHandler mobHandler = new MythicMobEffectHandler(context);
        MythicSkillEffectHandler skillHandler = new MythicSkillEffectHandler(context);
        register(mobHandler);
        register(new AliasEffectHandler("mmob", mobHandler));
        register(skillHandler);
        register(new AliasEffectHandler("mmskill", skillHandler));
    }

    private void register(EffectHandler handler) {
        DivizionSCApi.effectHandlers().register(handler);
        registeredTypes.add(handler.type());
    }
}
