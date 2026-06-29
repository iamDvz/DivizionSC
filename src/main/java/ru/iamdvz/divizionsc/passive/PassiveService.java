package ru.iamdvz.divizionsc.passive;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.PassiveTriggerType;
import ru.iamdvz.divizionsc.def.model.TriggerType;
import ru.iamdvz.divizionsc.def.service.DefService;
import ru.iamdvz.divizionsc.platform.TaskHandle;

/** Пассивки: срабатывают по событию или клавише, если у игрока есть permission def. */
public final class PassiveService {

    private final PluginContext context;
    private final Set<UUID> firing = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private final Map<UUID, TaskHandle> intervalTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, PressCombo>> pressCombos = new ConcurrentHashMap<>();

    public PassiveService(PluginContext context) {
        this.context = context;
    }

    public boolean isActive(Player player, DefDefinition def) {
        if (!def.passive() || def.helper()) {
            return false;
        }
        return player.hasPermission(def.permission())
                || player.hasPermission("divizionsc.passive.*")
                || player.hasPermission("divizionsc.def.*");
    }

    public void fire(Player player, PassiveTriggerType trigger, PassiveEventContext event) {
        if (firing.contains(player.getUniqueId())) {
            return;
        }
        firing.add(player.getUniqueId());
        try {
            for (DefDefinition def : context.defRegistry().passivesByTrigger(trigger)) {
                if (!isActive(player, def)) {
                    continue;
                }
                context.defs().firePassive(player, def, event);
            }
        } finally {
            firing.remove(player.getUniqueId());
        }
    }

    public void handleKeyPress(Player player, TriggerType key) {
        for (DefDefinition def : context.defRegistry().passivesByKey(key)) {
            if (!isActive(player, def)) {
                continue;
            }
            if (!advanceCombo(player, def)) {
                continue;
            }
            DefService.CastResult result = context.defs().firePassive(
                    player, def, PassiveEventContext.at(player.getLocation()));
            if (result == DefService.CastResult.SUCCESS) {
                clearCombo(player.getUniqueId(), def.id());
            }
        }
    }

    private boolean advanceCombo(Player player, DefDefinition def) {
        int required = def.passivePressCount();
        if (required <= 1) {
            return true;
        }
        long now = Bukkit.getCurrentTick();
        int window = def.passivePressWindowTicks();
        Map<String, PressCombo> combos = pressCombos.computeIfAbsent(
                player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
        PressCombo combo = combos.computeIfAbsent(def.id(), ignored -> new PressCombo());
        synchronized (combo) {
            if (combo.count > 0 && now - combo.lastTick > window) {
                combo.count = 0;
            }
            combo.count++;
            combo.lastTick = now;
            if (combo.count < required) {
                return false;
            }
            combo.count = 0;
            return true;
        }
    }

    private void clearCombo(UUID playerId, String defId) {
        Map<String, PressCombo> combos = pressCombos.get(playerId);
        if (combos != null) {
            combos.remove(defId.toLowerCase(java.util.Locale.ROOT));
        }
    }

    public void refreshIntervalTask(Player player) {
        UUID playerId = player.getUniqueId();
        TaskHandle existing = intervalTasks.remove(playerId);
        if (existing != null) {
            existing.cancel();
        }
        boolean hasInterval = false;
        long minInterval = 20L;
        for (DefDefinition def : context.defRegistry().passivesByTrigger(PassiveTriggerType.INTERVAL)) {
            if (!isActive(player, def)) {
                continue;
            }
            hasInterval = true;
            minInterval = Math.min(minInterval, Math.max(1, def.passiveIntervalTicks()));
        }
        if (!hasInterval || !player.isOnline()) {
            return;
        }
        long period = minInterval;
        TaskHandle handle = context.scheduler().entityTimer(player, () -> {
            if (!player.isOnline()) {
                TaskHandle task = intervalTasks.remove(playerId);
                if (task != null) {
                    task.cancel();
                }
                return;
            }
            fire(player, PassiveTriggerType.INTERVAL, PassiveEventContext.at(player.getLocation()));
        }, period, period);
        intervalTasks.put(playerId, handle);
    }

    public void refreshAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshIntervalTask(player);
        }
    }

    public void unload(Player player) {
        UUID playerId = player.getUniqueId();
        TaskHandle handle = intervalTasks.remove(playerId);
        if (handle != null) {
            handle.cancel();
        }
        pressCombos.remove(playerId);
    }

    private static final class PressCombo {
        int count;
        long lastTick;
    }
}
