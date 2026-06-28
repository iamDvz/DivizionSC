package ru.iamdvz.divizionsc.def.effect;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.iamdvz.divizionsc.def.model.DefDefinition;

public record EffectContext(
        Player caster,
        LivingEntity targetEntity,
        Location targetLocation,
        Block targetBlock,
        DefDefinition def,
        ItemStack castItem,
        int chainDepth
) {

    public Location effectLocation() {
        if (targetEntity != null) {
            return targetEntity.getLocation();
        }
        if (targetLocation != null) {
            return targetLocation;
        }
        return caster.getLocation();
    }

    public LivingEntity effectEntity() {
        if (targetEntity != null) {
            return targetEntity;
        }
        return caster;
    }
}
