package ru.iamdvz.divizionsc.api;

import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

public interface EffectHandler {

    String type();

    void execute(EffectContext context, EffectDefinition effect);
}
