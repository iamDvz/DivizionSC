package ru.iamdvz.divizionsc.platform;

import org.bukkit.plugin.Plugin;

/**
 * Создаёт подходящий {@link Scheduler} и определяет, запущен ли сервер на Folia.
 */
public final class SchedulerProvider {

    private SchedulerProvider() {
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static Scheduler create(Plugin plugin) {
        return new FoliaScheduler(plugin, isFolia());
    }
}
