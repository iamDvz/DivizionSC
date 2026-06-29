package ru.iamdvz.divizionsc.effect;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.divizionsc.util.EffectKeys;

/** Реализация моста к встроенному EffectLib (shaded). */
public final class EffectLibBridgeImpl implements EffectLibBridge {

    private final JavaPlugin plugin;
    private final EffectManager manager;
    private final List<Effect> activeEffects = new ArrayList<>();

    public EffectLibBridgeImpl(JavaPlugin plugin) {
        this.plugin = plugin;
        this.manager = new EffectManager(plugin);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void play(EffectContext ctx, EffectDefinition effect) {
        Map<String, Object> params = resolveParams(effect);
        String className = readString(params, "class");
        if (className.isBlank()) {
            plugin.getLogger().warning("effectlib effect missing 'class' field");
            return;
        }

        ConfigurationSection section = toSection(params);
        Location location = resolveLocation(ctx, effect);
        Effect started = manager.start(className, section, location);
        if (started != null) {
            synchronized (activeEffects) {
                activeEffects.add(started);
            }
        }
    }

    @Override
    public void cancelAll() {
        List<Effect> snapshot;
        synchronized (activeEffects) {
            snapshot = new ArrayList<>(activeEffects);
            activeEffects.clear();
        }
        for (Effect effect : snapshot) {
            effect.cancel();
        }
    }

    @Override
    public void dispose() {
        cancelAll();
        manager.dispose();
    }

    private Map<String, Object> resolveParams(EffectDefinition effect) {
        Map<String, Object> data = new HashMap<>(effect.data());
        Object nested = data.remove("effectlib");
        if (nested instanceof Map<?, ?> map) {
            Map<String, Object> merged = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                merged.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            merged.putAll(data);
            return merged;
        }
        return data;
    }

    private Location resolveLocation(EffectContext ctx, EffectDefinition effect) {
        String at = effect.text("at", "effect").toLowerCase(Locale.ROOT);
        return switch (at) {
            case "caster", "self" -> ctx.caster().getLocation();
            case "target", "entity" -> ctx.targetEntity() != null
                    ? ctx.targetEntity().getLocation()
                    : ctx.effectLocation();
            case "eyes", "eye" -> ctx.caster().getEyeLocation();
            default -> ctx.effectLocation();
        };
    }

    private ConfigurationSection toSection(Map<String, Object> params) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if ("particle".equalsIgnoreCase(entry.getKey()) && value instanceof String particle) {
                value = EffectKeys.normalizeParticleName(particle);
            }
            yaml.set(entry.getKey(), normalizeValue(value));
        }
        return yaml;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
            }
            return normalized;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::normalizeValue).toList();
        }
        return value;
    }

    private String readString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
