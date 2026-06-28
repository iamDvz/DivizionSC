package ru.iamdvz.divizionsc;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.api.EffectHandlerRegistry;
import ru.iamdvz.divizionsc.api.event.DefLoadEvent;
import ru.iamdvz.divizionsc.bind.BindRepository;
import ru.iamdvz.divizionsc.bind.BindService;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.effect.EffectExecutor;
import ru.iamdvz.divizionsc.def.loader.DefAddonLoader;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.def.loader.DefLoader;
import ru.iamdvz.divizionsc.def.service.CastItemFactory;
import ru.iamdvz.divizionsc.def.service.ChainService;
import ru.iamdvz.divizionsc.def.service.CooldownService;
import ru.iamdvz.divizionsc.def.service.DefRegistry;
import ru.iamdvz.divizionsc.def.service.DefService;
import ru.iamdvz.divizionsc.def.service.TargetResolver;
import ru.iamdvz.divizionsc.effect.EffectLibService;
import ru.iamdvz.divizionsc.lang.MessageService;
import ru.iamdvz.divizionsc.listener.SkillBarListener;

public final class PluginContext {

    private final JavaPlugin plugin;
    private PluginConfig config;
    private final MessageService messages;
    private final EffectLibService effectLib;
    private final EffectHandlerRegistry effectHandlers;
    private final DefRegistry defRegistry;
    private final CooldownService cooldowns;
    private final CastItemFactory castItems;
    private final TargetResolver targetResolver;
    private final EffectExecutor effectExecutor;
    private final ChainService chainService;
    private final DefService defService;
    private final BindRepository bindRepository;
    private final BindService bindService;
    private final SkillBarListener skillBarListener;

    private int reloadGeneration;
    private DefLoadReport lastLoadReport = new DefLoadReport();

    private PluginContext(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.messages = new MessageService(plugin, config);
        this.effectLib = new EffectLibService(plugin);
        this.effectHandlers = new EffectHandlerRegistry();
        this.defRegistry = new DefRegistry();
        this.cooldowns = new CooldownService();
        this.castItems = new CastItemFactory(plugin, config);
        this.targetResolver = new TargetResolver(config);
        this.effectExecutor = new EffectExecutor(plugin, this);
        this.chainService = new ChainService(this);
        this.defService = new DefService(this);
        this.bindRepository = new BindRepository(plugin);
        this.bindService = new BindService(this, bindRepository);
        this.skillBarListener = new SkillBarListener(this);
        reloadDefs();
    }

    public static PluginContext create(JavaPlugin plugin, PluginConfig config) {
        return new PluginContext(plugin, config);
    }

    public DefLoadReport reload(PluginConfig newConfig) {
        config = newConfig;
        messages.reload(newConfig);
        castItems.updateConfig(newConfig);
        targetResolver.updateConfig(newConfig);
        return reloadDefs();
    }

    public DefLoadReport reloadDefs() {
        reloadGeneration++;
        effectLib.cancelAll();
        defRegistry.clear();
        DefLoadReport report = new DefLoadReport();
        DefLoader loader = new DefLoader(plugin, config);
        loader.loadAll(defRegistry, report);
        lastLoadReport = report;
        Bukkit.getPluginManager().callEvent(new DefLoadEvent(report));
        return report;
    }

    public void loadAddonDefs(org.bukkit.plugin.Plugin addon) {
        DefLoader loader = new DefLoader(plugin, config);
        DefAddonLoader addonLoader = new DefAddonLoader(plugin, loader);
        addonLoader.loadFromPlugin(addon, defRegistry, lastLoadReport);
    }

    public void shutdown() {
        cooldowns.clear();
        effectLib.dispose();
    }

    public int reloadGeneration() {
        return reloadGeneration;
    }

    public DefLoadReport lastLoadReport() {
        return lastLoadReport;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public PluginConfig config() {
        return config;
    }

    public MessageService messages() {
        return messages;
    }

    public DefRegistry defRegistry() {
        return defRegistry;
    }

    public CooldownService cooldowns() {
        return cooldowns;
    }

    public CastItemFactory castItems() {
        return castItems;
    }

    public TargetResolver targetResolver() {
        return targetResolver;
    }

    public EffectExecutor effectExecutor() {
        return effectExecutor;
    }

    public ChainService chainService() {
        return chainService;
    }

    public DefService defs() {
        return defService;
    }

    public BindService binds() {
        return bindService;
    }

    public SkillBarListener skillBarListener() {
        return skillBarListener;
    }

    public EffectLibService effectLib() {
        return effectLib;
    }

    public EffectHandlerRegistry effectHandlers() {
        return effectHandlers;
    }
}
