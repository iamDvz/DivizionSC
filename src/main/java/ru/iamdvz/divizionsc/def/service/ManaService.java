package ru.iamdvz.divizionsc.def.service;

import org.bukkit.entity.Player;
import ru.iamdvz.divizionsc.config.PluginConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public final class ManaService {

    private PluginConfig config;
    private final Map<UUID, Double> current = new ConcurrentHashMap<>();

    public ManaService(PluginConfig config) {
        this.config = config;
    }

    public void updateConfig(PluginConfig newConfig) {
        this.config = newConfig;
    }

    public void initPlayer(UUID playerId) {
        if (!config.manaEnabled()) {
            return;
        }
        current.put(playerId, clamp(config.manaDefaultAmount(), 0, config.manaDefaultMax()));
    }

    public void removePlayer(UUID playerId) {
        current.remove(playerId);
    }

    public double current(UUID playerId) {
        return current.getOrDefault(playerId, config.manaDefaultAmount());
    }

    public double max(UUID playerId) {
        return config.manaDefaultMax();
    }

    public boolean canAfford(Player player, double cost) {
        if (!applies(cost)) {
            return true;
        }
        if (config.opBypassesMana(player)) {
            return true;
        }
        return canAfford(player.getUniqueId(), cost);
    }

    public boolean canAfford(UUID playerId, double cost) {
        if (!config.manaEnabled() || cost <= 0) {
            return true;
        }
        ensurePlayer(playerId);
        return current(playerId) >= cost;
    }

    public boolean spend(Player player, double cost) {
        if (!applies(cost)) {
            return true;
        }
        if (config.opBypassesMana(player)) {
            return true;
        }
        return spend(player.getUniqueId(), cost);
    }

    public boolean spend(UUID playerId, double cost) {
        if (!config.manaEnabled() || cost <= 0) {
            return true;
        }
        ensurePlayer(playerId);
        double left = current(playerId) - cost;
        if (left < 0) {
            return false;
        }
        current.put(playerId, left);
        return true;
    }

    public void set(UUID playerId, double amount) {
        current.put(playerId, clamp(amount, 0, config.manaDefaultMax()));
    }

    private boolean applies(double cost) {
        return config.manaEnabled() && cost > 0;
    }

    private void ensurePlayer(UUID playerId) {
        current.computeIfAbsent(playerId, ignored -> clamp(
                config.manaDefaultAmount(),
                0,
                config.manaDefaultMax()
        ));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
