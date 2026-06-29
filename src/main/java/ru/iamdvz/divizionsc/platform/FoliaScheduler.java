package ru.iamdvz.divizionsc.platform;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Реализация {@link Scheduler} поверх унифицированного планировщика Paper
 * (global/region/entity/async). Этот API присутствует и на обычном Paper, и на Folia,
 * поэтому одна реализация безопасна для обеих платформ.
 */
public final class FoliaScheduler implements Scheduler {

    private final Plugin plugin;
    private final boolean folia;
    private final Set<ScheduledTask> tracked = ConcurrentHashMap.newKeySet();

    public FoliaScheduler(Plugin plugin, boolean folia) {
        this.plugin = plugin;
        this.folia = folia;
    }

    private static long safeDelay(long ticks) {
        return Math.max(1L, ticks);
    }

    private TaskHandle track(ScheduledTask task) {
        if (task == null) {
            return TaskHandle.CANCELLED;
        }
        tracked.add(task);
        return new FoliaTaskHandle(task, tracked);
    }

    private Consumer<ScheduledTask> oneShot(Runnable task) {
        return scheduled -> {
            try {
                task.run();
            } finally {
                tracked.remove(scheduled);
            }
        };
    }

    private Consumer<ScheduledTask> repeating(Runnable task) {
        return scheduled -> task.run();
    }

    @Override
    public void global(Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, oneShot(task));
    }

    @Override
    public TaskHandle globalLater(Runnable task, long delayTicks) {
        return track(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, oneShot(task), safeDelay(delayTicks)));
    }

    @Override
    public TaskHandle globalTimer(Runnable task, long delayTicks, long periodTicks) {
        return track(Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, repeating(task), safeDelay(delayTicks), safeDelay(periodTicks)));
    }

    @Override
    public void entity(Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, oneShot(task), null);
    }

    @Override
    public TaskHandle entityLater(Entity entity, Runnable task, long delayTicks) {
        ScheduledTask[] holder = new ScheduledTask[1];
        holder[0] = entity.getScheduler().runDelayed(plugin, oneShot(task), untrack(holder), safeDelay(delayTicks));
        return track(holder[0]);
    }

    @Override
    public TaskHandle entityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        ScheduledTask[] holder = new ScheduledTask[1];
        holder[0] = entity.getScheduler()
                .runAtFixedRate(plugin, repeating(task), untrack(holder), safeDelay(delayTicks), safeDelay(periodTicks));
        return track(holder[0]);
    }

    /** Retired-callback: сущность удалена до запуска задачи — снимаем её из tracked, чтобы не копить мусор. */
    private Runnable untrack(ScheduledTask[] holder) {
        return () -> {
            if (holder[0] != null) {
                tracked.remove(holder[0]);
            }
        };
    }

    @Override
    public void region(Location location, Runnable task) {
        Bukkit.getRegionScheduler().run(plugin, location, oneShot(task));
    }

    @Override
    public TaskHandle regionLater(Location location, Runnable task, long delayTicks) {
        return track(Bukkit.getRegionScheduler().runDelayed(plugin, location, oneShot(task), safeDelay(delayTicks)));
    }

    @Override
    public TaskHandle regionTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        return track(Bukkit.getRegionScheduler()
                .runAtFixedRate(plugin, location, repeating(task), safeDelay(delayTicks), safeDelay(periodTicks)));
    }

    @Override
    public void async(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, oneShot(task));
    }

    @Override
    public TaskHandle asyncLater(Runnable task, long delayTicks) {
        return track(Bukkit.getAsyncScheduler()
                .runDelayed(plugin, oneShot(task), safeDelay(delayTicks) * 50L, TimeUnit.MILLISECONDS));
    }

    @Override
    public TaskHandle asyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return track(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, repeating(task),
                safeDelay(delayTicks) * 50L, safeDelay(periodTicks) * 50L, TimeUnit.MILLISECONDS));
    }

    @Override
    public void cancelAll() {
        for (ScheduledTask task : tracked) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
                // задача уже завершена
            }
        }
        tracked.clear();
    }

    @Override
    public boolean isFolia() {
        return folia;
    }

    private record FoliaTaskHandle(ScheduledTask task, Set<ScheduledTask> tracked) implements TaskHandle {
        @Override
        public void cancel() {
            task.cancel();
            tracked.remove(task);
        }

        @Override
        public boolean isCancelled() {
            return task.getExecutionState() == ScheduledTask.ExecutionState.CANCELLED;
        }
    }
}
