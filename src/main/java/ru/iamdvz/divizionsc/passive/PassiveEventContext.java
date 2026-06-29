package ru.iamdvz.divizionsc.passive;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * Контекст события для срабатывания пассивки (цель, позиция, урон).
 */
public record PassiveEventContext(
        LivingEntity targetEntity,
        Location location,
        double damage
) {

    public static PassiveEventContext of(LivingEntity target, Location location) {
        return new PassiveEventContext(target, location, 0);
    }

    public static PassiveEventContext of(LivingEntity target, Location location, double damage) {
        return new PassiveEventContext(target, location, damage);
    }

    public static PassiveEventContext at(Location location) {
        return new PassiveEventContext(null, location, 0);
    }
}
