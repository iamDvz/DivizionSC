package ru.iamdvz.dscmeg.bridge;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import ru.iamdvz.divizionsc.api.EffectHandler;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.dscmeg.MegContext;
import ru.iamdvz.dscmeg.model.VfxSpawnParams;
import ru.iamdvz.dscmeg.util.VfxEffectParser;
import ru.iamdvz.dscmeg.util.VfxSpawnFactory;

import java.util.Locale;

public final class VfxEffectHandler implements EffectHandler {

    private static final String SPAWN_ONCE_META = "dscmeg_vfx_spawned";

    private final MegContext context;

    public VfxEffectHandler(MegContext context) {
        this.context = context;
    }

    @Override
    public String type() {
        return "vfx";
    }

    @Override
    public void execute(EffectContext ctx, EffectDefinition effect) {
        Entity anchor = ctx.anchorEntity();
        if (anchor != null && shouldSpawnOnce(effect) && anchor.hasMetadata(SPAWN_ONCE_META)) {
            return;
        }

        Entity followEntity = resolveFollowEntity(ctx, effect);
        VfxSpawnParams params = VfxSpawnFactory.fromEffect(
                VfxEffectParser.parse(effect, context.config()),
                resolveLocation(ctx, effect),
                followEntity,
                effect.bool("force-follow", false) || followEntity != null
        );
        if (params == null) {
            return;
        }
        context.spawnService().spawn(params, null);

        if (anchor != null && shouldSpawnOnce(effect)) {
            anchor.setMetadata(SPAWN_ONCE_META, new FixedMetadataValue(context.plugin(), true));
        }
    }

    private boolean shouldSpawnOnce(EffectDefinition effect) {
        if (effect.data().containsKey("spawn-once")) {
            return effect.bool("spawn-once", true);
        }
        return "projectile".equals(resolveAt(effect)) || "anchor".equals(resolveAt(effect));
    }

    private String resolveAt(EffectDefinition effect) {
        return effect.text("at", effect.text("position", effect.text("location", "effect"))).toLowerCase(Locale.ROOT);
    }

    private Location resolveLocation(EffectContext ctx, EffectDefinition effect) {
        String at = resolveAt(effect);
        return switch (at) {
            case "caster", "self" -> ctx.caster().getLocation();
            case "eyes", "eye" -> ctx.caster().getEyeLocation();
            case "target" -> ctx.targetEntity() != null
                    ? ctx.targetEntity().getLocation()
                    : ctx.effectLocation();
            case "projectile", "anchor" -> ctx.anchorEntity() != null
                    ? ctx.anchorEntity().getLocation()
                    : ctx.effectLocation();
            default -> ctx.effectLocation();
        };
    }

    private Entity resolveFollowEntity(EffectContext ctx, EffectDefinition effect) {
        if (effect.bool("follow-target", false) || effect.bool("mount-target", false)) {
            LivingEntity target = ctx.targetEntity();
            if (target != null) {
                return target;
            }
        }
        String follow = effect.text("follow", "").toLowerCase(Locale.ROOT);
        Entity resolved = switch (follow) {
            case "target" -> ctx.targetEntity();
            case "caster", "self" -> ctx.caster();
            case "projectile", "anchor" -> ctx.anchorEntity();
            default -> null;
        };
        if (resolved != null) {
            return resolved;
        }
        if (("projectile".equals(resolveAt(effect)) || "anchor".equals(resolveAt(effect))) && ctx.anchorEntity() != null) {
            return ctx.anchorEntity();
        }
        return null;
    }
}
