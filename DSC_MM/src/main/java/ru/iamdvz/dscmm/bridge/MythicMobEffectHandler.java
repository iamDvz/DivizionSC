package ru.iamdvz.dscmm.bridge;

import ru.iamdvz.divizionsc.api.EffectHandler;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.dscmm.MmContext;
import ru.iamdvz.dscmm.util.EffectLocations;

public final class MythicMobEffectHandler implements EffectHandler {

    private final MmContext context;

    public MythicMobEffectHandler(MmContext context) {
        this.context = context;
    }

    @Override
    public String type() {
        return "mythicmob";
    }

    @Override
    public void execute(EffectContext ctx, EffectDefinition effect) {
        String mobId = effect.text("mob", effect.text("type", effect.text("id", "")));
        if (mobId.isBlank()) {
            return;
        }
        double level = effect.data().containsKey("level")
                ? effect.number("level", context.config().defaults().mobLevel())
                : context.config().defaults().mobLevel();
        context.mythicMobService().spawnMob(mobId, EffectLocations.resolve(ctx, effect), level);
    }
}
