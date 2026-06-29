package ru.iamdvz.divizionsc.def.loader;

import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.loader.dsl.DscBlock;
import ru.iamdvz.divizionsc.def.loader.dsl.DscCompiler;
import ru.iamdvz.divizionsc.def.loader.dsl.DscParseException;
import ru.iamdvz.divizionsc.def.loader.dsl.DscParser;
import ru.iamdvz.divizionsc.def.loader.dsl.DscScript;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.PassiveTriggerType;
import ru.iamdvz.divizionsc.def.service.DefRegistry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class DefLoader {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private Map<String, DscBlock> sharedModules = Map.of();

    public DefLoader(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
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
            String message = "No def files (defs-*.dsc) found in " + folder.getPath();
            plugin.getLogger().warning(message);
            report.addWarning(message);
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));
        sharedModules = collectGlobalModules(files, report);

        for (File file : files) {
            loadFile(file, registry, report);
        }
        DefAddonLoader addonLoader = new DefAddonLoader(plugin, this);
        addonLoader.loadFromEnabledPlugins(registry, report);
        sharedModules = Map.of();
    }

    private Map<String, DscBlock> collectGlobalModules(File[] files, DefLoadReport report) {
        Map<String, DscBlock> modules = new LinkedHashMap<>();
        DscParser parser = new DscParser();
        for (File file : files) {
            try {
                String source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                DscScript script = parser.parse(source, file.getName());
                for (DscBlock block : script.blocks()) {
                    if (block.helper()) {
                        DscBlock previous = modules.put(block.id(), block);
                        if (previous != null) {
                            report.addWarning("Module '" + block.id() + "' redefined in " + file.getName());
                        }
                    }
                }
            } catch (DscParseException e) {
                logLoadError("DSC parse error while indexing modules in " + file.getName() + ": " + e.getMessage(), e,
                        report);
            } catch (IOException e) {
                logLoadError("Failed to read DSC file " + file.getName(), e, report);
            }
        }
        return modules;
    }

    public int loadFile(File file, DefRegistry registry, DefLoadReport report) {
        if (!DefFileFilter.accepts(file.getName())) {
            String message = "Skipped non-DSC def file: " + file.getName();
            plugin.getLogger().warning(message);
            report.addWarning(message);
            return 0;
        }
        return loadDscFile(file, registry, report);
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

        List<DefDefinition> compiled;
        try {
            compiled = compiler.compile(script, sharedModules);
        } catch (DscParseException e) {
            logLoadError("DSC compile error in " + sourceLabel + ": " + e.getMessage(), e, report);
            return 0;
        } catch (RuntimeException e) {
            logLoadError("DSC compile error in " + sourceLabel + ": " + e.getMessage(), e, report);
            return 0;
        }

        int loaded = 0;
        for (DefDefinition def : compiled) {
            if (def.helper() && !def.passive()) {
                continue;
            }
            try {
                validatePassive(def, sourceLabel, report);
                registry.register(def, sourceLabel, report);
                loaded++;
                report.defLoaded();
            } catch (Exception e) {
                logLoadError("Failed to register def '" + def.id() + "' from " + sourceLabel, e, report);
            }
        }
        return loaded;
    }

    public int loadStream(InputStream stream, String sourceLabel, DefRegistry registry, DefLoadReport report)
            throws IOException {
        if (!DefFileFilter.accepts(extractFileName(sourceLabel))) {
            String message = "Skipped non-DSC def stream: " + sourceLabel;
            report.addWarning(message);
            return 0;
        }
        String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        return loadDscSource(source, sourceLabel, registry, report);
    }

    private static String extractFileName(String sourceLabel) {
        int colon = sourceLabel.lastIndexOf(':');
        String path = colon >= 0 ? sourceLabel.substring(colon + 1) : sourceLabel;
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private void copyDefaults(File folder) {
        copyDefaultResource(folder, "defs/defs-examples.dsc", "defs-examples.dsc");
        copyDefaultResource(folder, "defs/defs-advanced.dsc", "defs-advanced.dsc");
        copyDefaultResource(folder, "defs/defs-fx-examples.dsc", "defs-fx-examples.dsc");
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

    private void validatePassive(DefDefinition def, String sourceLabel, DefLoadReport report) {
        if (!def.passive()) {
            return;
        }
        if (!def.hasPassiveTrigger()) {
            report.addWarning("Passive def '" + def.id() + "' in " + sourceLabel
                    + " needs trigger: on damage / key shift / passive-trigger / passive-key");
        }
        if (def.passiveKeyTrigger() != null && def.passivePressCount() > 1 && def.passivePressWindowTicks() < 1) {
            report.addWarning("Passive def '" + def.id() + "' combo key needs press-window > 0");
        }
        if (def.helper()) {
            report.addWarning("Passive def '" + def.id() + "' in " + sourceLabel + " is also helper");
        }
        if (def.passiveTrigger() == PassiveTriggerType.INTERVAL && def.passiveIntervalTicks() < 1) {
            report.addWarning("Passive def '" + def.id() + "' interval trigger needs passive-interval > 0");
        }
    }

    private void logLoadError(String message, Throwable error, DefLoadReport report) {
        if (plugin != null) {
            plugin.getLogger().warning(message);
        }
        report.addError(message);
        if (config.debugLoadErrors() && error != null && plugin != null) {
            plugin.getLogger().log(Level.WARNING, message, error);
        }
    }
}
