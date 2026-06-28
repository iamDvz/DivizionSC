package ru.iamdvz.divizionsc;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.api.DivizionSCApi;
import ru.iamdvz.divizionsc.command.DivizionSCCommand;
import ru.iamdvz.divizionsc.command.DivizionSCTabCompleter;
import ru.iamdvz.divizionsc.config.ConfigurationLoader;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.listener.CastTriggerListener;
import ru.iamdvz.divizionsc.listener.DefAddonListener;
import ru.iamdvz.divizionsc.listener.KeyInputListener;

public final class DivizionSC extends JavaPlugin {

    private ConfigurationLoader configurationLoader;
    private PluginContext context;

    @Override
    public void onEnable() {
        configurationLoader = new ConfigurationLoader(this);
        PluginConfig config = configurationLoader.load();
        context = PluginContext.create(this, config);
        DivizionSCApi.bind(this);

        getServer().getPluginManager().registerEvents(new CastTriggerListener(context), this);
        getServer().getPluginManager().registerEvents(new KeyInputListener(context), this);
        getServer().getPluginManager().registerEvents(new DefAddonListener(context, this), this);
        getServer().getPluginManager().registerEvents(context.skillBarListener(), this);

        DivizionSCCommand command = new DivizionSCCommand(context, this::reloadAll);
        getCommand("dsc").setExecutor(command);
        getCommand("dsc").setTabCompleter(new DivizionSCTabCompleter(context));

        Bukkit.getScheduler().runTaskTimer(this, () -> context.cooldowns().pruneAllExpired(), 6000L, 6000L);

        DefLoadReport report = context.lastLoadReport();
        getLogger().info("Loaded " + report.loadedCount() + " defs.");
        logLoadIssues(report);
    }

    @Override
    public void onDisable() {
        DivizionSCApi.unbind(this);
        if (context != null) {
            context.shutdown();
        }
    }

    public DefLoadReport reloadAll() {
        PluginConfig config = configurationLoader.reload();
        DefLoadReport report = context.reload(config);
        getLogger().info("Reloaded. Defs: " + report.loadedCount());
        logLoadIssues(report);
        return report;
    }

    public PluginContext context() {
        return context;
    }

    private void logLoadIssues(DefLoadReport report) {
        for (String warning : report.warnings()) {
            getLogger().warning(warning);
        }
        for (String error : report.errors()) {
            getLogger().warning(error);
        }
    }
}
