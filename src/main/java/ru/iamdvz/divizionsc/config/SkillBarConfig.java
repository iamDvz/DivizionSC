package ru.iamdvz.divizionsc.config;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.model.TriggerType;

public final class SkillBarConfig {

    private static final List<String> DEFAULT_BIND_TRIGGERS = List.of(
            "right_click", "1", "2", "3", "4", "5", "6", "7", "8", "9");

    private final Settings.SkillBar settings;
    private final Set<TriggerType> bindTriggers;

    public SkillBarConfig(Settings.SkillBar settings) {
        this.settings = settings;
        this.bindTriggers = resolveBindTriggers(settings.bindTriggers);
    }

    public static List<String> defaultBindTriggers() {
        return DEFAULT_BIND_TRIGGERS;
    }

    public boolean enabled() {
        return settings.enabled;
    }

    public String permission() {
        return settings.permission;
    }

    public int guiSize() {
        return settings.guiSize;
    }

    public int listStartSlot() {
        return settings.listStartSlot;
    }

    public int listPageSize() {
        return settings.listPageSize;
    }

    public int bindRowStartSlot() {
        return settings.bindRowStartSlot;
    }

    public int bindSlotCount() {
        return settings.bindSlotCount;
    }

    public boolean requireEmptyHand() {
        return settings.requireEmptyHand;
    }

    public boolean openOnSwapHands() {
        return settings.openOnSwapHands;
    }

    public boolean acceptsBindTrigger(TriggerType trigger) {
        return bindTriggers.contains(trigger);
    }

    private static Set<TriggerType> resolveBindTriggers(List<String> configured) {
        List<String> source = (configured == null || configured.isEmpty())
                ? DEFAULT_BIND_TRIGGERS : configured;
        Set<TriggerType> triggers = EnumSet.noneOf(TriggerType.class);
        for (String raw : source) {
            triggers.add(TriggerType.parse(raw));
        }
        return triggers;
    }
}
