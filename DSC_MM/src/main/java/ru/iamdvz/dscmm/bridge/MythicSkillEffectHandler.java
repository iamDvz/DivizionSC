package ru.iamdvz.dscmm.bridge;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.iamdvz.divizionsc.api.EffectHandler;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.dscmm.MmContext;
import ru.iamdvz.dscmm.util.EffectLocations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MythicSkillEffectHandler implements EffectHandler {

    private final MmContext context;

    public MythicSkillEffectHandler(MmContext context) {
        this.context = context;
    }

    @Override
    public String type() {
        return "mythicskill";
    }

    @Override
    public void execute(EffectContext ctx, EffectDefinition effect) {
        String skillId = effect.text("skill", effect.text("name", effect.text("id", "")));
        if (skillId.isBlank()) {
            return;
        }
        LivingEntity caster = EffectLocations.resolveCaster(ctx, effect);
        float power = effect.data().containsKey("power")
                ? (float) effect.number("power", context.config().defaults().skillPower())
                : context.config().defaults().skillPower();
        Location origin = resolveOrigin(ctx, effect);
        List<Entity> targets = resolveEntityTargets(ctx, effect);
        context.mythicMobService().castSkill(caster, skillId, origin, targets, power);
    }

    private Location resolveOrigin(EffectContext ctx, EffectDefinition effect) {
        if (!effect.data().containsKey("at") && !effect.data().containsKey("location") && !effect.data().containsKey("origin")) {
            return null;
        }
        return EffectLocations.resolve(ctx, effect);
    }

    private List<Entity> resolveEntityTargets(EffectContext ctx, EffectDefinition effect) {
        String mode = effect.text("targets", effect.text("target", "none")).toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "target", "entity" -> {
                LivingEntity target = ctx.targetEntity();
                yield target == null ? List.of() : List.of(target);
            }
            case "caster", "self" -> List.of(ctx.caster());
            case "all" -> {
                List<Entity> entities = new ArrayList<>();
                entities.add(ctx.caster());
                if (ctx.targetEntity() != null) {
                    entities.add(ctx.targetEntity());
                }
                yield entities;
            }
            default -> List.of();
        };
    }
}
