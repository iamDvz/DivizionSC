package ru.iamdvz.divizionsc.def.service;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import ru.iamdvz.divizionsc.database.CooldownRepository;

public final class CooldownService {

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private CooldownRepository repository;

    public void setRepository(CooldownRepository repository) {
        this.repository = repository;
    }

    /** Подгрузить кулдауны из БД (кросс-сервер) в память. */
    public void seed(UUID playerId, Map<String, Long> persisted) {
        if (persisted == null || persisted.isEmpty()) {
            return;
        }
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
        for (Map.Entry<String, Long> entry : persisted.entrySet()) {
            playerCooldowns.merge(entry.getKey(), entry.getValue(), Math::max);
        }
    }

    public boolean isReady(UUID playerId, String abilityId, double cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return true;
        }
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return true;
        }
        pruneExpired(playerCooldowns);
        Long expiresAt = playerCooldowns.get(abilityId);
        return expiresAt == null || System.currentTimeMillis() >= expiresAt;
    }

    public long remainingMillis(UUID playerId, String abilityId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return 0L;
        }
        pruneExpired(playerCooldowns);
        Long expiresAt = playerCooldowns.get(abilityId);
        if (expiresAt == null) {
            return 0L;
        }
        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }

    public void apply(UUID playerId, String abilityId, double cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + (long) (cooldownSeconds * 1000.0);
        cooldowns.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(abilityId, expiresAt);
        if (repository != null) {
            repository.upsert(playerId, abilityId, expiresAt);
        }
    }

    public void removePlayer(UUID playerId) {
        cooldowns.remove(playerId);
    }

    public void pruneAllExpired() {
        Iterator<Map.Entry<UUID, Map<String, Long>>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map<String, Long> playerCooldowns = iterator.next().getValue();
            pruneExpired(playerCooldowns);
            if (playerCooldowns.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public void clear() {
        cooldowns.clear();
    }

    private void pruneExpired(Map<String, Long> playerCooldowns) {
        long now = System.currentTimeMillis();
        playerCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
