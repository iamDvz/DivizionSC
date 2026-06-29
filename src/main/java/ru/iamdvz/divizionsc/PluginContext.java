package ru.iamdvz.divizionsc;

import java.util.Collection;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.api.DivizionSCService;
import ru.iamdvz.divizionsc.api.EffectHandlerRegistry;
import ru.iamdvz.divizionsc.api.IntegrationCastOptions;
import ru.iamdvz.divizionsc.api.event.DefLoadEvent;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.platform.Scheduler;
import ru.iamdvz.divizionsc.platform.SchedulerProvider;
import ru.iamdvz.divizionsc.bind.BindRepository;
import ru.iamdvz.divizionsc.bind.BindService;
import ru.iamdvz.divizionsc.passive.PassiveService;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.effect.EffectExecutor;
import ru.iamdvz.divizionsc.def.effect.StunService;
import ru.iamdvz.divizionsc.def.loader.DefAddonLoader;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.def.loader.DefLoader;
import ru.iamdvz.divizionsc.def.service.CastItemFactory;
import ru.iamdvz.divizionsc.def.service.ChainService;
import ru.iamdvz.divizionsc.def.service.CooldownService;
import ru.iamdvz.divizionsc.def.service.DefRegistry;
import ru.iamdvz.divizionsc.def.service.DefService;
import ru.iamdvz.divizionsc.def.service.ManaService;
import ru.iamdvz.divizionsc.def.service.TargetResolver;
import ru.iamdvz.divizionsc.effect.EffectLibBridge;
import ru.iamdvz.divizionsc.effect.EffectLibBridges;
import ru.iamdvz.divizionsc.lang.MessageService;
import ru.iamdvz.divizionsc.listener.SkillBarListener;

public final class PluginContext implements DivizionSCService {

    private final JavaPlugin plugin;
    private final Scheduler scheduler;
    private PluginConfig config;
    private final MessageService messages;
    private final ru.iamdvz.divizionsc.def.expr.PlaceholderResolver placeholders;
    private final ru.iamdvz.divizionsc.def.expr.ExpressionEvaluator expressions;
    private final ru.iamdvz.divizionsc.def.condition.ConditionEvaluator conditions;
    private final EffectLibBridge effectLib;
    private final StunService stunService;
    private final EffectHandlerRegistry effectHandlers;
    private volatile DefRegistry defRegistry;
    private final CooldownService cooldowns;
    private final ManaService mana;
    private final CastItemFactory castItems;
    private final TargetResolver targetResolver;
    private final EffectExecutor effectExecutor;
    private final ChainService chainService;
    private final DefService defService;
    private final ru.iamdvz.divizionsc.database.DatabaseManager databaseManager;
    private final ru.iamdvz.divizionsc.database.CooldownRepository cooldownRepository;
    private final BindRepository bindRepository;
    private final BindService bindService;
    private final PassiveService passiveService;
    private final SkillBarListener skillBarListener;
    private ru.iamdvz.divizionsc.integration.EconomyBridge economy;

    private int reloadGeneration;
    private DefLoadReport lastLoadReport = new DefLoadReport();

    private PluginContext(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.scheduler = SchedulerProvider.create(plugin);
        this.config = config;
        this.messages = new MessageService(plugin, config);
        this.placeholders = new ru.iamdvz.divizionsc.def.expr.PlaceholderResolver();
        this.expressions = new ru.iamdvz.divizionsc.def.expr.ExpressionEvaluator(placeholders);
        this.conditions = new ru.iamdvz.divizionsc.def.condition.ConditionEvaluator(placeholders, expressions);
        this.effectLib = EffectLibBridges.create(plugin);
        this.stunService = new StunService(scheduler);
        this.effectHandlers = new EffectHandlerRegistry();
        this.defRegistry = new DefRegistry();
        this.cooldowns = new CooldownService();
        this.mana = new ManaService(config);
        this.castItems = new CastItemFactory(plugin, config);
        this.targetResolver = new TargetResolver(config);
        this.effectExecutor = new EffectExecutor(plugin, this);
        this.chainService = new ChainService(this);
        this.defService = new DefService(this);
        this.databaseManager = new ru.iamdvz.divizionsc.database.DatabaseManager(plugin, config.database());
        this.cooldownRepository = new ru.iamdvz.divizionsc.database.CooldownRepository(databaseManager);
        this.cooldowns.setRepository(cooldownRepository);
        this.bindRepository = new BindRepository(plugin, databaseManager);
        this.bindService = new BindService(this, bindRepository);
        this.passiveService = new PassiveService(this);
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
        mana.updateConfig(newConfig);
        return reloadDefs();
    }

    public DefLoadReport reloadDefs() {
        reloadGeneration++;
        effectLib.cancelAll();
        stunService.clear();
        DefRegistry newRegistry = new DefRegistry();
        DefLoadReport report = new DefLoadReport();
        DefLoader loader = new DefLoader(plugin, config);
        loader.loadAll(newRegistry, report);
        defRegistry = newRegistry;
        lastLoadReport = report;
        passiveService.refreshAllOnline();
        Bukkit.getPluginManager().callEvent(new DefLoadEvent(report));
        return report;
    }

    /** Dry-run: загружает def-файлы во временный реестр, не затрагивая активное состояние. */
    public DefLoadReport validateDefs() {
        DefLoadReport report = new DefLoadReport();
        DefRegistry temp = new DefRegistry();
        DefLoader loader = new DefLoader(plugin, config);
        loader.loadAll(temp, report);
        return report;
    }

    public void loadAddonDefs(org.bukkit.plugin.Plugin addon) {
        DefLoader loader = new DefLoader(plugin, config);
        DefAddonLoader addonLoader = new DefAddonLoader(plugin, loader);
        addonLoader.loadFromPlugin(addon, defRegistry, lastLoadReport);
    }

    public void shutdown() {
        cooldowns.clear();
        scheduler.cancelAll();
        stunService.shutdown();
        effectLib.dispose();
        databaseManager.close();
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public void initDatabase() {
        databaseManager.init().thenRun(() -> bindRepository.migrateLegacyYaml());
    }

    public ru.iamdvz.divizionsc.database.DatabaseManager database() {
        return databaseManager;
    }

    public ru.iamdvz.divizionsc.database.CooldownRepository cooldownRepository() {
        return cooldownRepository;
    }

    public BindRepository bindRepository() {
        return bindRepository;
    }

    /** Подгрузка привязок и кулдаунов игрока из БД (кросс-сервер). */
    public void preloadPlayer(java.util.UUID playerId) {
        bindRepository.preload(playerId)
                .thenAccept(fromDb -> scheduler.global(() -> bindRepository.applyPreload(playerId, fromDb)));
        cooldownRepository.load(playerId)
                .thenAccept(map -> scheduler.global(() -> cooldowns.seed(playerId, map)));
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

    public ManaService mana() {
        return mana;
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

    public PassiveService passives() {
        return passiveService;
    }

    public SkillBarListener skillBarListener() {
        return skillBarListener;
    }

    public EffectLibBridge effectLib() {
        return effectLib;
    }

    public StunService stunService() {
        return stunService;
    }

    public ru.iamdvz.divizionsc.def.expr.PlaceholderResolver placeholders() {
        return placeholders;
    }

    public ru.iamdvz.divizionsc.def.expr.ExpressionEvaluator expressions() {
        return expressions;
    }

    public java.util.Optional<ru.iamdvz.divizionsc.integration.EconomyBridge> economy() {
        return java.util.Optional.ofNullable(economy);
    }

    public void setEconomy(ru.iamdvz.divizionsc.integration.EconomyBridge economy) {
        this.economy = economy;
    }

    /** Подключает soft-depend интеграции, если соответствующие плагины присутствуют. */
    public void setupIntegrations() {
        ru.iamdvz.divizionsc.integration.IntegrationManager.setup(this);
    }

    public ru.iamdvz.divizionsc.def.condition.ConditionEvaluator conditions() {
        return conditions;
    }

    @Override
    public EffectHandlerRegistry effectHandlers() {
        return effectHandlers;
    }

    @Override
    public Optional<DefDefinition> findDef(String id) {
        return defRegistry.find(id);
    }

    @Override
    public Collection<DefDefinition> allDefs() {
        return defRegistry.all();
    }

    @Override
    public DefService.CastResult cast(Player player, String defId) {
        return defService.castFromCommand(player, defId);
    }

    @Override
    public DefService.CastResult castIntegration(
            Player player,
            String defId,
            LivingEntity targetEntity,
            Location targetLocation,
            Block targetBlock,
            IntegrationCastOptions options) {
        return defService.castFromIntegration(player, defId, targetEntity, targetLocation, targetBlock, options);
    }
}
