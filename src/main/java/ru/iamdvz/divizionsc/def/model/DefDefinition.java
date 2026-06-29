package ru.iamdvz.divizionsc.def.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record DefDefinition(
        String id,
        String name,
        String description,
        String permission,
        double cooldown,
        double mana,
        TriggerType trigger,
        TargetMode targetMode,
        double range,
        boolean helper,
        boolean passive,
        PassiveTriggerType passiveTrigger,
        TriggerType passiveKeyTrigger,
        int passiveIntervalTicks,
        int passivePressCount,
        int passivePressWindowTicks,
        CastItemSpec castItem,
        List<EffectDefinition> effects,
        Map<ChainTrigger, List<ChainEntry>> chain
) {

    public DefDefinition {
        effects = effects == null ? List.of() : List.copyOf(effects);
        chain = chain == null ? Map.of() : Map.copyOf(chain);
    }

    public DefDefinition withArgs(Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return this;
        }
        List<EffectDefinition> mergedEffects = effects.stream()
                .map(effect -> effect.withArgs(args))
                .toList();
        return new DefDefinition(
                id, name, description, permission, cooldown, mana,
                trigger, targetMode, range, helper, passive, passiveTrigger, passiveKeyTrigger,
                passiveIntervalTicks, passivePressCount, passivePressWindowTicks,
                castItem, mergedEffects, chain
        );
    }

    public boolean isPassive() {
        return passive;
    }

    public boolean hasPassiveTrigger() {
        return passiveTrigger != null || passiveKeyTrigger != null;
    }

    public List<ChainEntry> chainEntries(ChainTrigger trigger) {
        return chain.getOrDefault(trigger, List.of());
    }

    public static Map<ChainTrigger, List<ChainEntry>> emptyChain() {
        return new EnumMap<>(ChainTrigger.class);
    }
}
