package ru.iamdvz.divizionsc.integration;

import java.util.logging.Level;
import org.bukkit.Bukkit;
import ru.iamdvz.divizionsc.PluginContext;

/**
 * Подключает soft-depend интеграции (PlaceholderAPI, Vault, WorldGuard) при наличии плагинов.
 * Каждый хук изолирован try/catch и грузится лениво, чтобы отсутствие плагина не ломало ядро.
 */
public final class IntegrationManager {

    private IntegrationManager() {
    }

    public static void setup(PluginContext context) {
        if (isPresent("PlaceholderAPI")) {
            safe(context, "PlaceholderAPI", () -> PlaceholderApiHook.register(context.placeholders()));
        }
        if (isPresent("WorldGuard")) {
            safe(context, "WorldGuard", () -> WorldGuardHook.register(context.conditions()));
        }
        if (isPresent("Vault")) {
            safe(context, "Vault", () -> {
                EconomyBridge economy = VaultHook.resolve();
                if (economy != null) {
                    context.setEconomy(economy);
                    context.conditions().setBalanceProvider(economy::balance);
                }
            });
        }
    }

    private static boolean isPresent(String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }

    private static void safe(PluginContext context, String name, Runnable action) {
        try {
            action.run();
            context.plugin().getLogger().info("Hooked into " + name + ".");
        } catch (Throwable error) {
            context.plugin().getLogger().log(Level.WARNING, "Failed to hook into " + name, error);
        }
    }
}
