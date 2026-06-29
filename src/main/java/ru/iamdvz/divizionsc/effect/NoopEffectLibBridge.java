package ru.iamdvz.divizionsc.effect;

import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

final class NoopEffectLibBridge implements EffectLibBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void play(EffectContext ctx, EffectDefinition effect) {
        // EffectLib не установлен
    }

    @Override
    public void cancelAll() {
    }

    @Override
    public void dispose() {
    }
}
