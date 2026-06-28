package ru.iamdvz.divizionsc.def.loader.simple;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.model.CastItemSpec;
import ru.iamdvz.divizionsc.def.loader.ChainEntryParser;
import ru.iamdvz.divizionsc.def.model.ChainEntry;
import ru.iamdvz.divizionsc.def.model.ChainTrigger;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.divizionsc.def.model.TargetMode;
import ru.iamdvz.divizionsc.def.model.TriggerType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SimpleDefCompiler {

    private final PluginConfig config;
    private final SimpleEffectParser effectParser = new SimpleEffectParser();

    public SimpleDefCompiler(PluginConfig config) {
        this.config = config;
    }

    public DefDefinition compile(String id, ConfigurationSection section, EffectFallback fallback) {
        String permission = section.getString("perm",
                section.getString("permission", config.castPermissionPrefix() + id));
        TriggerType trigger = TriggerType.parse(firstString(section, "command", "key", "on", "trigger"));
        TargetMode target = TargetMode.parse(firstString(section, "none", "tgt", "to", "target"));
        double range = section.getDouble("rng", section.getDouble("range", config.defaultRange()));
        CastItemSpec castItem = parseItem(section.getString("item"), section.getConfigurationSection("cast-item"));
        List<EffectDefinition> effects = parseEffects(section, fallback);
        Map<ChainTrigger, List<ChainEntry>> chain = parseChain(section);

        return new DefDefinition(
                id,
                section.getString("name", id),
                firstString(section, "", "desc", "description"),
                permission,
                section.getDouble("cd", section.getDouble("cooldown", 0)),
                trigger,
                target,
                range,
                section.getBoolean("helper", false),
                castItem,
                effects,
                chain
        );
    }

    private List<EffectDefinition> parseEffects(ConfigurationSection section, EffectFallback fallback) {
        List<?> raw = section.getList("do");
        boolean simpleMode = raw != null;
        if (raw == null) {
            raw = section.getList("effects");
        }
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        if (!simpleMode && !hasStringEffects(raw)) {
            return fallback.parseVerbose(raw);
        }

        List<EffectDefinition> result = new ArrayList<>();
        for (Object item : raw) {
            SimpleEffectParser.ParseResult parsed = effectParser.parse(item);
            if (parsed.effect() != null) {
                result.add(parsed.effect());
            } else if (parsed.verboseMode() && item instanceof Map<?, ?> map) {
                result.add(fallback.parseVerboseMap(map));
            }
        }
        return result;
    }

    private boolean hasStringEffects(List<?> raw) {
        for (Object item : raw) {
            if (item instanceof String) {
                return true;
            }
            if (item instanceof Map<?, ?> map && (map.containsKey("wait") || map.containsKey("delay"))) {
                return true;
            }
        }
        return false;
    }

    private Map<ChainTrigger, List<ChainEntry>> parseChain(ConfigurationSection section) {
        Map<ChainTrigger, List<ChainEntry>> chain = new EnumMap<>(ChainTrigger.class);
        putChain(chain, ChainTrigger.ON_CAST, section, "on_cast", "cast");
        putChain(chain, ChainTrigger.ON_HIT, section, "on_hit", "hit");
        putChain(chain, ChainTrigger.ON_COMPLETE, section, "on_complete", "done", "after", "then");
        ConfigurationSection nested = section.getConfigurationSection("chain");
        if (nested != null) {
            for (String key : nested.getKeys(false)) {
                ChainTrigger trigger = ChainTrigger.parse(key);
                List<?> list = nested.getList(key);
                if (list != null) {
                    chain.put(trigger, parseChainEntries(list));
                }
            }
        }
        return chain;
    }

    private void putChain(Map<ChainTrigger, List<ChainEntry>> chain, ChainTrigger trigger,
                          ConfigurationSection section, String... keys) {
        for (String key : keys) {
            if (!section.contains(key)) {
                continue;
            }
            Object raw = section.get(key);
            if (raw instanceof String defId) {
                ChainEntry entry = ChainEntryParser.parseString(defId);
                if (entry != null) {
                    chain.put(trigger, List.of(entry));
                }
                return;
            }
            if (raw instanceof List<?> list) {
                chain.put(trigger, parseChainEntries(list));
                return;
            }
        }
    }

    private List<ChainEntry> parseChainEntries(List<?> rawList) {
        List<ChainEntry> entries = new ArrayList<>();
        for (Object raw : rawList) {
            ChainEntry entry = parseChainEntry(raw);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private ChainEntry parseChainEntry(Object raw) {
        return ChainEntryParser.parse(raw);
    }

    private CastItemSpec parseItem(String compact, ConfigurationSection verbose) {
        if (verbose != null) {
            Material material = Material.matchMaterial(verbose.getString("material", "STICK"));
            if (material == null) {
                material = Material.STICK;
            }
            return new CastItemSpec(
                    material,
                    verbose.getString("name"),
                    verbose.getStringList("lore"),
                    verbose.getInt("custom-model-data", 0)
            );
        }
        if (compact == null || compact.isBlank()) {
            return null;
        }
        String[] parts = compact.split("\\|", -1);
        Material material = Material.matchMaterial(parts[0].trim().toUpperCase(Locale.ROOT));
        if (material == null) {
            material = Material.STICK;
        }
        String name = parts.length > 1 ? parts[1].trim() : null;
        List<String> lore = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            lore.add(parts[i].trim());
        }
        return new CastItemSpec(material, name, lore, 0);
    }

    private String firstString(ConfigurationSection section, String defaultValue, String... keys) {
        for (String key : keys) {
            String value = section.getString(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return defaultValue;
    }

    public interface EffectFallback {
        List<EffectDefinition> parseVerbose(List<?> rawList);

        EffectDefinition parseVerboseMap(Map<?, ?> raw);
    }
}
