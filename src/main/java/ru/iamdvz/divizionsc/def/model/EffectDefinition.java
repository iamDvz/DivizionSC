package ru.iamdvz.divizionsc.def.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record EffectDefinition(
        String type,
        Map<String, Object> data,
        List<EffectDefinition> nested
) {

    public EffectDefinition {
        data = data == null ? Map.of() : Map.copyOf(data);
        nested = nested == null ? List.of() : List.copyOf(nested);
    }

    public EffectDefinition withArgs(Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new HashMap<>(data);
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            merged.put(entry.getKey(), entry.getValue());
        }
        List<EffectDefinition> mergedNested = nested.stream()
                .map(effect -> effect.withArgs(args))
                .toList();
        for (Map.Entry<String, Object> entry : merged.entrySet()) {
            if (entry.getValue() instanceof List<?> list) {
                List<EffectDefinition> effectList = list.stream()
                        .filter(EffectDefinition.class::isInstance)
                        .map(EffectDefinition.class::cast)
                        .map(effect -> effect.withArgs(args))
                        .toList();
                merged.put(entry.getKey(), effectList);
            }
        }
        return new EffectDefinition(type, merged, mergedNested);
    }

    public List<EffectDefinition> children(String key) {
        Object value = data.get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(EffectDefinition.class::isInstance)
                    .map(EffectDefinition.class::cast)
                    .toList();
        }
        return nested;
    }

    public double number(String key, double fallback) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public int integer(String key, int fallback) {
        return (int) Math.round(number(key, fallback));
    }

    public boolean bool(String key, boolean fallback) {
        Object value = data.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }

    public String text(String key, String fallback) {
        Object value = data.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> map(String key) {
        Object value = data.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }
}
