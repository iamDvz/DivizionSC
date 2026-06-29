package ru.iamdvz.divizionsc.effect;

import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/** Фабрика моста к EffectLib (встроен в JAR через shadow). */
public final class EffectLibBridges {

    private EffectLibBridges() {
    }

    public static EffectLibBridge create(JavaPlugin plugin) {
        try {
            EffectLibBridge bridge = new EffectLibBridgeImpl(plugin);
            plugin.getLogger().info("EffectLib ready (embedded).");
            return bridge;
        } catch (Throwable error) {
            plugin.getLogger().log(Level.WARNING, "Embedded EffectLib failed to start", error);
            return new NoopEffectLibBridge();
        }
    }
}
