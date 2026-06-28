package ru.iamdvz.divizionsc.def.loader;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.loader.dsl.DscCompiler;
import ru.iamdvz.divizionsc.def.loader.dsl.DscParseException;
import ru.iamdvz.divizionsc.def.loader.dsl.DscParser;
import ru.iamdvz.divizionsc.def.loader.dsl.DscScript;
import ru.iamdvz.divizionsc.def.loader.simple.SimpleDefCompiler;
import ru.iamdvz.divizionsc.def.loader.verbose.VerboseEffectParser;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.divizionsc.def.service.DefRegistry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class DefLoader {

    private final JavaPlugin plugin;
    private final PluginConfig config;

    private final SimpleDefCompiler simpleCompiler;
    private final VerboseEffectParser verboseParser;

    public DefLoader(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.simpleCompiler = new SimpleDefCompiler(config);
        this.verboseParser = new VerboseEffectParser();
    }

    public void loadAll(DefRegistry registry, DefLoadReport report) {
        File folder = new File(plugin.getDataFolder(), config.defsFolder());
        if (!folder.exists() && !folder.mkdirs()) {
            String message = "Failed to create defs folder: " + folder.getPath();
            plugin.getLogger().warning(message);
            report.addError(message);
        }
        copyDefaults(folder);

        File[] files = folder.listFiles((dir, name) -> DefFileFilter.accepts(name));
        if (files == null || files.length == 0) {
            String message = "No def files (defs-*.yml / defs-*.dsc) found in " + folder.getPath();
            plugin.getLogger().warning(message);
            report.addWarning(message);
            return;
        }

        for (File file : files) {
            loadFile(file, registry, report);
        }
        DefAddonLoader addonLoader = new DefAddonLoader(plugin, this);
        addonLoader.loadFromEnabledPlugins(registry, report);
    }

    public int loadFile(File file, DefRegistry registry, DefLoadReport report) {
        if (isDscFile(file.getName())) {
            return loadDscFile(file, registry, report);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return loadConfiguration(yaml, file.getName(), registry, report);
    }

    public int loadDscFile(File file, DefRegistry registry, DefLoadReport report) {
        try {
            String source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return loadDscSource(source, file.getName(), registry, report);
        } catch (IOException e) {
            logLoadError("Failed to read DSC file " + file.getName(), e, report);
            return 0;
        }
    }

    public int loadDscSource(String source, String sourceLabel, DefRegistry registry, DefLoadReport report) {
        DscParser parser = new DscParser();
        DscCompiler compiler = new DscCompiler(config);
        DscScript script;
        try {
            script = parser.parse(source, sourceLabel);
        } catch (DscParseException e) {
            logLoadError("DSC parse error in " + sourceLabel + ": " + e.getMessage(), e, report);
            return 0;
        }

        int loaded = 0;
        for (DefDefinition def : compiler.compile(script)) {
            try {
                registry.register(def, sourceLabel, report);
                loaded++;
                report.defLoaded();
            } catch (Exception e) {
                logLoadError("Failed to compile def '" + def.id() + "' from " + sourceLabel, e, report);
            }
        }
        return loaded;
    }

    private static boolean isDscFile(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".dsc");
    }

    private void copyDefaults(File folder) {
        copyDefaultResource(folder, "defs/defs-examples.yml", "defs-examples.yml");
        copyDefaultResource(folder, "defs/defs-advanced.yml", "defs-advanced.yml");
        copyDefaultResource(folder, "defs/defs-combat.yml", "defs-combat.yml");
        copyDefaultResource(folder, "defs/defs-magic.yml", "defs-magic.yml");
        copyDefaultResource(folder, "defs/defs-boss.dsc", "defs-boss.dsc");
        copyDefaultResource(folder, "defs/defs-script.dsc", "defs-script.dsc");
    }

    private void copyDefaultResource(File folder, String resourcePath, String targetName) {
        File target = new File(folder, targetName);
        if (target.exists()) {
            return;
        }
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return;
            }
            Files.copy(stream, target.toPath());
            plugin.getLogger().info("Created default defs file: " + target.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to copy default defs: " + targetName, e);
        }
    }

    public int loadStream(InputStream stream, String sourceLabel, DefRegistry registry, DefLoadReport report)
            throws IOException {
        if (sourceLabel.toLowerCase(Locale.ROOT).endsWith(".dsc")) {
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return loadDscSource(source, sourceLabel, registry, report);
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
            return loadConfiguration(yaml, sourceLabel, registry, report);
        }
    }

    private int loadConfiguration(YamlConfiguration yaml, String sourceLabel, DefRegistry registry, DefLoadReport report) {
        int loaded = 0;
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                DefDefinition def = parseDef(key.toLowerCase(Locale.ROOT), section);
                registry.register(def, sourceLabel, report);
                loaded++;
                report.defLoaded();
            } catch (Exception e) {
                logLoadError("Failed to load def '" + key + "' from " + sourceLabel, e, report);
            }
        }
        return loaded;
    }

    private DefDefinition parseDef(String id, ConfigurationSection section) {
        return simpleCompiler.compile(id, section, new SimpleDefCompiler.EffectFallback() {
            @Override
            public List<EffectDefinition> parseVerbose(List<?> rawList) {
                return verboseParser.parseEffectsList(rawList);
            }

            @Override
            public EffectDefinition parseVerboseMap(Map<?, ?> raw) {
                return verboseParser.parseEffect(raw);
            }
        });
    }

    private void logLoadError(String message, Throwable error, DefLoadReport report) {
        plugin.getLogger().warning(message);
        report.addError(message);
        if (config.debugLoadErrors() && error != null) {
            plugin.getLogger().log(Level.WARNING, message, error);
        }
    }
}
