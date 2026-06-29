package ru.iamdvz.divizionsc.def.service;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.TargetMode;

import java.util.Optional;

public final class TargetResolver {

    private PluginConfig config;

    public TargetResolver(PluginConfig config) {
        this.config = config;
    }

    public void updateConfig(PluginConfig newConfig) {
        config = newConfig;
    }

    public ResolvedTarget resolve(Player caster, DefDefinition def) {
        double range = def.range() > 0 ? def.range() : config.defaultRange();
        return switch (def.targetMode()) {
            case SELF -> new ResolvedTarget(caster, caster.getLocation(), null);
            case ENTITY -> resolveEntity(caster, range);
            case BLOCK -> resolveBlock(caster, range);
            case NONE -> new ResolvedTarget(null, caster.getEyeLocation(), null);
        };
    }

    public ResolvedTarget resolveForChain(EffectContext parent, DefDefinition def) {
        return switch (def.targetMode()) {
            case SELF -> new ResolvedTarget(parent.caster(), parent.caster().getLocation(), null);
            case ENTITY -> {
                if (parent.targetEntity() != null) {
                    yield new ResolvedTarget(parent.targetEntity(), parent.targetEntity().getLocation(), null);
                }
                yield resolve(parent.caster(), def);
            }
            case BLOCK -> new ResolvedTarget(null, parent.targetLocation(), parent.targetBlock());
            case NONE -> new ResolvedTarget(null, parent.targetLocation(), parent.targetBlock());
        };
    }

    /** Прицел по сущности без обязательной цели на касте. */
    public LivingEntity raycastEntity(Player caster, double range) {
        return resolveEntity(caster, range).entity();
    }

    /** Точка прицела: сущность или точка взгляда на дистанции. */
    public Location raycastLocation(Player caster, double range) {
        ResolvedTarget resolved = resolveEntity(caster, range);
        if (resolved.entity() != null) {
            return resolved.entity().getLocation();
        }
        return resolved.location();
    }

    /** Блок или точка взгляда на дистанции. */
    public ResolvedTarget raycastBlock(Player caster, double range) {
        return resolveBlock(caster, range);
    }

    private ResolvedTarget resolveEntity(Player caster, double range) {
        RayTraceResult result = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                range,
                0.25,
                entity -> entity instanceof LivingEntity living
                        && living != caster
                        && living.isValid()
                        && !living.isDead()
        );
        if (result != null && result.getHitEntity() instanceof LivingEntity living) {
            return new ResolvedTarget(living, living.getLocation(), null);
        }
        Block fallbackBlock = caster.getTargetBlockExact((int) range);
        Location fallback = fallbackBlock != null
                ? fallbackBlock.getLocation().add(0.5, 0.5, 0.5)
                : caster.getEyeLocation().add(caster.getEyeLocation().getDirection().multiply(range));
        return new ResolvedTarget(null, fallback, null);
    }

    private ResolvedTarget resolveBlock(Player caster, double range) {
        RayTraceResult result = caster.getWorld().rayTraceBlocks(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                range,
                FluidCollisionMode.NEVER,
                true
        );
        Block block = result == null ? caster.getTargetBlockExact((int) range) : result.getHitBlock();
        Location location = block == null
                ? caster.getEyeLocation().add(caster.getEyeLocation().getDirection().multiply(range))
                : block.getLocation().add(0.5, 1.0, 0.5);
        return new ResolvedTarget(null, location, block);
    }

    public record ResolvedTarget(LivingEntity entity, Location location, Block block) {
        public Optional<LivingEntity> entityOptional() {
            return Optional.ofNullable(entity);
        }
    }
}
