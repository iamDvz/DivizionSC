package ru.iamdvz.divizionsc.config;

import org.bukkit.configuration.file.FileConfiguration;
import ru.iamdvz.divizionsc.def.model.TriggerType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class SkillBarConfig {

    private static final List<String> DEFAULT_BIND_TRIGGERS = List.of(
            "right_click", "1", "2", "3", "4", "5", "6", "7", "8", "9"
    );

    private final FileConfiguration yaml;

    public SkillBarConfig(FileConfiguration yaml) {
        this.yaml = yaml;
    }

    public static List<String> defaultBindTriggers() {
        return DEFAULT_BIND_TRIGGERS;
    }

    public boolean enabled() {
        return yaml.getBoolean("skill-bar.enabled", true);
    }

    public String permission() {
        return yaml.getString("skill-bar.permission", "divizionsc.skills");
    }

    public int guiSize() {
        return yaml.getInt("skill-bar.gui-size", 54);
    }

    public int listStartSlot() {
        return yaml.getInt("skill-bar.list-start-slot", 0);
    }

    public int listPageSize() {
        return yaml.getInt("skill-bar.list-page-size", 27);
    }

    public int bindRowStartSlot() {
        return yaml.getInt("skill-bar.bind-row-start-slot", 36);
    }

    public int bindSlotCount() {
        return yaml.getInt("skill-bar.bind-slot-count", 9);
    }

    public boolean requireEmptyHand() {
        return yaml.getBoolean("skill-bar.require-empty-hand", true);
    }

    public boolean openOnSwapHands() {
        return yaml.getBoolean("skill-bar.open-on-swap-hands", true);
    }

    public boolean acceptsBindTrigger(TriggerType trigger) {
        return resolveBindTriggers().contains(trigger);
    }

    private Set<TriggerType> resolveBindTriggers() {
        List<String> configured = yaml.getStringList("skill-bar.bind-triggers");
        if (configured != null && !configured.isEmpty()) {
            Set<TriggerType> triggers = EnumSet.noneOf(TriggerType.class);
            for (String raw : configured) {
                triggers.add(TriggerType.parse(raw));
            }
            return triggers;
        }
        if (yaml.contains("skill-bar.cast-mode")) {
            return legacyBindTriggers();
        }
        return parseTriggerList(DEFAULT_BIND_TRIGGERS);
    }

    private Set<TriggerType> parseTriggerList(List<String> rawTriggers) {
        Set<TriggerType> triggers = EnumSet.noneOf(TriggerType.class);
        for (String raw : rawTriggers) {
            triggers.add(TriggerType.parse(raw));
        }
        return triggers;
    }

    private Set<TriggerType> legacyBindTriggers() {
        String mode = yaml.getString("skill-bar.cast-mode", "right_click");
        Set<TriggerType> triggers = EnumSet.noneOf(TriggerType.class);
        if ("right_click".equalsIgnoreCase(mode) || "both".equalsIgnoreCase(mode)) {
            triggers.add(TriggerType.RIGHT_CLICK);
        }
        if ("select".equalsIgnoreCase(mode) || "both".equalsIgnoreCase(mode)) {
            triggers.add(TriggerType.HOTBAR);
            triggers.addAll(EnumSet.range(TriggerType.KEY_1, TriggerType.KEY_9));
        }
        if (triggers.isEmpty()) {
            triggers.add(TriggerType.RIGHT_CLICK);
        }
        return triggers;
    }
}
