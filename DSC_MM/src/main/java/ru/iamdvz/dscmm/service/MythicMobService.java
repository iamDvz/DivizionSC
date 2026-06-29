package ru.iamdvz.dscmm.service;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.dscmm.MmContext;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public final class MythicMobService {

    private final JavaPlugin plugin;
    private final MmContext context;

    public MythicMobService(JavaPlugin plugin, MmContext context) {
        this.plugin = plugin;
        this.context = context;
    }

    public ActiveMob spawnMob(String mobId, Location location, double level) {
        if (mobId == null || mobId.isBlank()) {
            logFailure("MythicMob id is empty");
            return null;
        }
        if (location == null || location.getWorld() == null) {
            logFailure("Cannot spawn MythicMob '" + mobId + "': invalid location");
            return null;
        }
        try {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().spawnMob(mobId, location, level);
            if (activeMob == null) {
                logFailure("MythicMob '" + mobId + "' was not spawned (unknown type or spawn blocked)");
            }
            return activeMob;
        } catch (Throwable t) {
            logFailure("Failed to spawn MythicMob '" + mobId + "': " + t.getMessage());
            return null;
        }
    }

    public boolean castSkill(
            Entity caster,
            String skillId,
            Location origin,
            Collection<Entity> entityTargets,
            float power
    ) {
        if (skillId == null || skillId.isBlank()) {
            logFailure("MythicMobs skill id is empty");
            return false;
        }
        if (caster == null) {
            logFailure("Cannot cast MythicMobs skill '" + skillId + "': caster is null");
            return false;
        }
        try {
            boolean success;
            if (origin != null && entityTargets != null && !entityTargets.isEmpty()) {
                success = MythicBukkit.inst().getAPIHelper().castSkill(
                        caster,
                        skillId,
                        origin,
                        entityTargets,
                        List.of(),
                        power
                );
            } else if (origin != null) {
                success = MythicBukkit.inst().getAPIHelper().castSkill(caster, skillId, origin, power);
            } else {
                success = MythicBukkit.inst().getAPIHelper().castSkill(caster, skillId, power);
            }
            if (!success) {
                logFailure("MythicMobs skill '" + skillId + "' was not cast (unknown skill or invalid caster)");
            }
            return success;
        } catch (Throwable t) {
            logFailure("Failed to cast MythicMobs skill '" + skillId + "': " + t.getMessage());
            return false;
        }
    }

    private void logFailure(String message) {
        if (!context.config().debug().logFailures()) {
            return;
        }
        Logger logger = plugin.getLogger();
        logger.warning("[DSC_MM] " + message);
    }
}
