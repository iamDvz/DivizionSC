package ru.iamdvz.divizionsc.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.function.BiPredicate;
import org.bukkit.entity.Player;
import ru.iamdvz.divizionsc.def.condition.ConditionEvaluator;

/**
 * Интеграция с WorldGuard: проверка нахождения игрока в регионе для условия {@code region}.
 * Класс загружается только если WorldGuard присутствует на сервере.
 */
public final class WorldGuardHook {

    private WorldGuardHook() {
    }

    public static void register(ConditionEvaluator conditions) {
        conditions.setRegionCheck(regionPredicate());
    }

    private static BiPredicate<Player, String> regionPredicate() {
        return (player, regionId) -> {
            if (regionId == null || regionId.isBlank()) {
                return false;
            }
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            var applicable = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
            return applicable.getRegions().stream()
                    .anyMatch(region -> region.getId().equalsIgnoreCase(regionId));
        };
    }
}
