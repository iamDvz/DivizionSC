package ru.iamdvz.divizionsc.def.effect;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
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
        int chainDepth,
        Entity anchorEntity,
        VariableScope vars
) {

    public EffectContext(
            Player caster,
            LivingEntity targetEntity,
            Location targetLocation,
            Block targetBlock,
            DefDefinition def,
            ItemStack castItem,
            int chainDepth
    ) {
        this(caster, targetEntity, targetLocation, targetBlock, def, castItem, chainDepth, null, VariableScope.create());
    }

    public EffectContext(
            Player caster,
            LivingEntity targetEntity,
            Location targetLocation,
            Block targetBlock,
            DefDefinition def,
            ItemStack castItem,
            int chainDepth,
            Entity anchorEntity
    ) {
        this(caster, targetEntity, targetLocation, targetBlock, def, castItem, chainDepth, anchorEntity,
                VariableScope.create());
    }

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

    public EffectContext withTarget(LivingEntity entity) {
        Location location = entity != null ? entity.getLocation() : targetLocation;
        return new EffectContext(caster, entity, location, targetBlock, def, castItem, chainDepth, anchorEntity, vars);
    }

    public EffectContext withLocation(Location location) {
        return new EffectContext(caster, targetEntity, location, targetBlock, def, castItem, chainDepth, anchorEntity,
                vars);
    }

    public EffectContext withAnchor(Entity entity) {
        return new EffectContext(caster, targetEntity, targetLocation, targetBlock, def, castItem, chainDepth, entity,
                vars);
    }
}
