package ru.iamdvz.dscmeg.bridge;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.iamdvz.divizionsc.api.EffectHandler;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.dscmeg.MegContext;
import ru.iamdvz.dscmeg.model.VfxSpawnParams;
import ru.iamdvz.dscmeg.util.VfxEffectParser;
import ru.iamdvz.dscmeg.util.VfxSpawnFactory;

import java.util.Locale;

public final class VfxEffectHandler implements EffectHandler {

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
        VfxSpawnParams params = VfxSpawnFactory.fromEffect(
                VfxEffectParser.parse(effect, context.config()),
                resolveLocation(ctx, effect),
                resolveFollowEntity(ctx, effect),
                effect.bool("force-follow", false)
        );
        if (params == null) {
            return;
        }
        context.spawnService().spawn(params, null);
    }

    private Location resolveLocation(EffectContext ctx, EffectDefinition effect) {
        String at = effect.text("at", effect.text("location", "effect")).toLowerCase(Locale.ROOT);
        return switch (at) {
            case "caster", "self" -> ctx.caster().getLocation();
            case "eyes", "eye" -> ctx.caster().getEyeLocation();
            case "target" -> ctx.targetEntity() != null
                    ? ctx.targetEntity().getLocation()
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
        return switch (follow) {
            case "target" -> ctx.targetEntity();
            case "caster", "self" -> ctx.caster();
            default -> null;
        };
    }
}
