package ru.iamdvz.divizionsc.def.loader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.def.service.DefRegistry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public final class DefAddonLoader {

    private final JavaPlugin plugin;
    private final DefLoader defLoader;

    public DefAddonLoader(JavaPlugin plugin, DefLoader defLoader) {
        this.plugin = plugin;
        this.defLoader = defLoader;
    }

    public void loadFromEnabledPlugins(DefRegistry registry, DefLoadReport report) {
        for (Plugin enabled : Bukkit.getPluginManager().getPlugins()) {
            if (!enabled.isEnabled()) {
                continue;
            }
            loadFromPlugin(enabled, registry, report);
        }
    }

    public int loadFromPlugin(Plugin source, DefRegistry registry, DefLoadReport report) {
        if (source == plugin) {
            return 0;
        }

        File location = resolvePluginLocation(source);
        if (location == null) {
            return 0;
        }

        if (location.isDirectory()) {
            return loadFromDirectory(source, location, registry, report);
        }
        return loadFromJar(source, location, registry, report);
    }

    private int loadFromDirectory(Plugin source, File classesRoot, DefRegistry registry, DefLoadReport report) {
        File defsDir = new File(classesRoot, "defs");
        if (!defsDir.isDirectory()) {
            return 0;
        }

        File[] files = defsDir.listFiles((dir, name) -> DefFileFilter.accepts(name));
        if (files == null || files.length == 0) {
            return 0;
        }

        int loaded = 0;
        for (File file : files) {
            loaded += defLoader.loadFile(file, registry, report);
        }
        if (loaded > 0) {
            plugin.getLogger().info("Loaded " + loaded + " defs from addon " + source.getName()
                    + " (" + files.length + " files)");
        }
        return loaded;
    }

    private int loadFromJar(Plugin source, File jarFile, DefRegistry registry, DefLoadReport report) {
        List<String> entries = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            for (JarEntry entry : jar.stream().toList()) {
                String name = entry.getName();
                if (DefFileFilter.acceptsJarEntry(name)) {
                    entries.add(name);
                }
            }
            entries.sort(String::compareTo);

            if (entries.isEmpty()) {
                return 0;
            }

            int loaded = 0;
            for (String entryName : entries) {
                JarEntry entry = jar.getJarEntry(entryName);
                if (entry == null) {
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    loaded += defLoader.loadStream(
                            input,
                            source.getName() + ":" + entryName,
                            registry,
                            report
                    );
                }
            }

            plugin.getLogger().info("Loaded " + loaded + " defs from addon " + source.getName()
                    + " (" + entries.size() + " files)");
            return loaded;
        } catch (IOException e) {
            String message = "Failed to read def pack from " + source.getName();
            plugin.getLogger().log(Level.WARNING, message, e);
            report.addError(message);
            return 0;
        }
    }

    private File resolvePluginLocation(Plugin source) {
        try {
            if (source.getClass().getProtectionDomain().getCodeSource() == null) {
                return null;
            }
            return new File(source.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
