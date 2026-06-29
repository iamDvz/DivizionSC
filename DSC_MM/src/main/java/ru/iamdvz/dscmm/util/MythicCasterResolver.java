package ru.iamdvz.dscmm.util;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class MythicCasterResolver {

    private MythicCasterResolver() {
    }

    public static Player resolve(SkillMetadata data, String mode, LivingEntity target) {
        String resolvedMode = mode == null || mode.isBlank() ? "auto" : mode.toLowerCase(Locale.ROOT);
        return switch (resolvedMode) {
            case "trigger" -> asPlayer(data.getTrigger());
            case "target" -> target instanceof Player player ? player : asPlayer(data.getTrigger());
            case "self", "caster" -> asPlayer(data.getCaster().getEntity());
            case "auto" -> resolveAuto(data, target);
            default -> resolveAuto(data, target);
        };
    }

    private static Player resolveAuto(SkillMetadata data, LivingEntity target) {
        Player self = asPlayer(data.getCaster().getEntity());
        if (self != null) {
            return self;
        }
        Player trigger = asPlayer(data.getTrigger());
        if (trigger != null) {
            return trigger;
        }
        if (target instanceof Player player) {
            return player;
        }
        return null;
    }

    private static Player asPlayer(AbstractEntity entity) {
        if (entity == null) {
            return null;
        }
        Entity bukkitEntity = BukkitAdapter.adapt(entity);
        return bukkitEntity instanceof Player player ? player : null;
    }
}
