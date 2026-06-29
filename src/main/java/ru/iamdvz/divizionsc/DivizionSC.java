package ru.iamdvz.divizionsc;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.api.DivizionSCService;
import ru.iamdvz.divizionsc.command.DivizionSCCommand;
import ru.iamdvz.divizionsc.command.DivizionSCTabCompleter;
import ru.iamdvz.divizionsc.config.ConfigurationLoader;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.listener.CastTriggerListener;
import ru.iamdvz.divizionsc.listener.DefAddonListener;
import ru.iamdvz.divizionsc.listener.KeyInputListener;
import ru.iamdvz.divizionsc.listener.PassiveListener;
import ru.iamdvz.divizionsc.listener.StunListener;

public final class DivizionSC extends JavaPlugin {

    private ConfigurationLoader configurationLoader;
    private PluginContext context;

    @Override
    public void onEnable() {
        configurationLoader = new ConfigurationLoader(this);
        PluginConfig config = configurationLoader.load();

        var dscCommand = getCommand("dsc");
        if (dscCommand == null) {
            getLogger().severe("Command 'dsc' is missing from plugin.yml — disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        context = PluginContext.create(this, config);
        context.initDatabase();
        context.setupIntegrations();
        getServer().getServicesManager().register(DivizionSCService.class, context, this, ServicePriority.Normal);

        getServer().getPluginManager().registerEvents(new CastTriggerListener(context), this);
        getServer().getPluginManager().registerEvents(new PassiveListener(context), this);
        getServer().getPluginManager().registerEvents(new StunListener(context.stunService()), this);
        getServer().getPluginManager().registerEvents(new KeyInputListener(context), this);
        getServer().getPluginManager().registerEvents(new DefAddonListener(context, this), this);
        getServer().getPluginManager().registerEvents(context.skillBarListener(), this);

        DivizionSCCommand command = new DivizionSCCommand(context, this::reloadAll);
        dscCommand.setExecutor(command);
        dscCommand.setTabCompleter(new DivizionSCTabCompleter(context));

        context.scheduler().globalTimer(() -> context.cooldowns().pruneAllExpired(), 6000L, 6000L);

        DefLoadReport report = context.lastLoadReport();
        getLogger().info("Loaded " + report.loadedCount() + " defs.");
        logLoadIssues(report);
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
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
