package ru.iamdvz.divizionsc.def.service;

import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.ChainEntry;
import ru.iamdvz.divizionsc.def.model.ChainTrigger;

public final class ChainService {

    public static final int MAX_DEPTH = 8;

    private final PluginContext context;

    public ChainService(PluginContext context) {
        this.context = context;
    }

    public void fire(EffectContext parent, ChainTrigger trigger) {
        fire(parent, trigger, parent.chainDepth());
    }

    private void fire(EffectContext parent, ChainTrigger trigger, int depth) {
        if (depth >= MAX_DEPTH) {
            return;
        }
        for (ChainEntry entry : parent.def().chainEntries(trigger)) {
            context.defs().invokeChained(parent, entry, depth + 1);
        }
    }
}
