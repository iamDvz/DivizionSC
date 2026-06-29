package ru.iamdvz.divizionsc.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Абстракция планировщика, безопасная для Folia.
 * <p>
 * Все задачи, затрагивающие игровое состояние, должны планироваться через
 * региональные/сущностные методы, чтобы выполняться в правильном регион-потоке.
 * Тяжёлая работа (БД, I/O) — через {@link #async(Runnable)}.
 */
public interface Scheduler {

    /** Выполнить в глобальном регион-потоке (тики мира/глобальное состояние). */
    void global(Runnable task);

    TaskHandle globalLater(Runnable task, long delayTicks);

    TaskHandle globalTimer(Runnable task, long delayTicks, long periodTicks);

    /** Выполнить в регион-потоке сущности (перемещение/телепорт/инвентарь). */
    void entity(Entity entity, Runnable task);

    TaskHandle entityLater(Entity entity, Runnable task, long delayTicks);

    TaskHandle entityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks);

    /** Выполнить в регион-потоке локации (спавн/изменение блоков/партиклы). */
    void region(Location location, Runnable task);

    TaskHandle regionLater(Location location, Runnable task, long delayTicks);

    TaskHandle regionTimer(Location location, Runnable task, long delayTicks, long periodTicks);

    /** Асинхронная задача (БД, сеть, вычисления). Не трогать игровое состояние. */
    void async(Runnable task);

    TaskHandle asyncLater(Runnable task, long delayTicks);

    TaskHandle asyncTimer(Runnable task, long delayTicks, long periodTicks);

    /** Отменить все задачи, запланированные через этот планировщик. */
    void cancelAll();

    boolean isFolia();
}
