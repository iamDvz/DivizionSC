package ru.iamdvz.dscmm.bridge;

import ru.iamdvz.divizionsc.api.EffectHandler;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.dscmm.MmContext;

public final class AliasEffectHandler implements EffectHandler {

    private final String type;
    private final EffectHandler delegate;

    public AliasEffectHandler(String type, EffectHandler delegate) {
        this.type = type;
        this.delegate = delegate;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public void execute(EffectContext context, EffectDefinition effect) {
        delegate.execute(context, effect);
    }
}
