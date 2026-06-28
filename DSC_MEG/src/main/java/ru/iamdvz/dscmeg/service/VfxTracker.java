package ru.iamdvz.dscmeg.service;

import org.bukkit.World;
import ru.iamdvz.dscmeg.config.DscMegConfig;
import ru.iamdvz.dscmeg.model.VfxSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class VfxTracker {

    private final Map<UUID, VfxSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> perWorldCounts = new ConcurrentHashMap<>();
    private DscMegConfig.LimitsSettings limits;

    public VfxTracker(DscMegConfig.LimitsSettings limits) {
        this.limits = limits;
    }

    public void updateSettings(DscMegConfig.LimitsSettings limits) {
        this.limits = limits;
    }

    public SpawnCheckResult canSpawn(World world) {
        if (limits.maxActivePerWorld() > 0 && countInWorld(world) >= limits.maxActivePerWorld()) {
            return SpawnCheckResult.denied("world-limit");
        }
        return SpawnCheckResult.ok();
    }

    public void register(VfxSession session) {
        activeSessions.put(session.stand().getUniqueId(), session);
        if (session.stand().isValid()) {
            perWorldCounts.computeIfAbsent(session.stand().getWorld().getUID(), ignored -> new AtomicInteger())
                    .incrementAndGet();
        }
    }

    public void unregister(VfxSession session) {
        activeSessions.remove(session.stand().getUniqueId());
        AtomicInteger worldCount = perWorldCounts.get(session.worldUid());
        if (worldCount != null) {
            worldCount.updateAndGet(value -> Math.max(0, value - 1));
        }
    }

    public void reconcileWorldCounts() {
        perWorldCounts.clear();
        for (VfxSession session : activeSessions.values()) {
            if (session.stand().isValid()) {
                perWorldCounts.computeIfAbsent(session.worldUid(), ignored -> new AtomicInteger())
                        .incrementAndGet();
            }
        }
    }

    public int countInWorld(World world) {
        AtomicInteger counter = perWorldCounts.get(world.getUID());
        return counter == null ? 0 : counter.get();
    }

    public Map<UUID, VfxSession> activeSessions() {
        return activeSessions;
    }

    public record SpawnCheckResult(boolean permitted, String reason) {
        public static SpawnCheckResult ok() {
            return new SpawnCheckResult(true, null);
        }

        public static SpawnCheckResult denied(String reason) {
            return new SpawnCheckResult(false, reason);
        }
    }
}
