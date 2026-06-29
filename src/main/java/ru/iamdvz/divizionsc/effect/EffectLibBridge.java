package ru.iamdvz.divizionsc.effect;

import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

/** Опциональная интеграция с плагином EffectLib (soft-depend). */
public interface EffectLibBridge {

    boolean isAvailable();

    void play(EffectContext ctx, EffectDefinition effect);

    void cancelAll();

    void dispose();
}
