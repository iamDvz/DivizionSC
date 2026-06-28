package ru.iamdvz.divizionsc.def.loader.verbose;

import ru.iamdvz.divizionsc.def.loader.ChainEntryParser;
import ru.iamdvz.divizionsc.def.model.ChainEntry;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VerboseEffectParser {

    public List<EffectDefinition> parseEffectsList(List<?> rawList) {
        List<EffectDefinition> result = new ArrayList<>();
        for (Object raw : rawList) {
            if (raw instanceof Map<?, ?> map) {
                result.add(parseEffect(map));
            }
        }
        return result;
    }

    public EffectDefinition parseEffect(Map<?, ?> raw) {
        Map<String, Object> data = new HashMap<>();
        String type = "message";
        List<EffectDefinition> nested = new ArrayList<>();

        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if ("type".equalsIgnoreCase(key)) {
                type = String.valueOf(value).toLowerCase(Locale.ROOT);
                continue;
            }
            if (value instanceof List<?> list && isEffectListKey(key)) {
                List<EffectDefinition> parsed = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        parsed.add(parseEffect(map));
                    } else {
                        ChainEntry chainEntry = parseChainEntry(item);
                        if (chainEntry != null) {
                            parsed.add(chainEntryToEffect(chainEntry));
                        }
                    }
                }
                data.put(key, parsed);
                continue;
            }
            data.put(key, value);
        }

        if ("def".equals(type) || "chain".equals(type) || "ability".equals(type)) {
            type = "def";
        } else if ("message".equals(type) && hasDefReference(data)) {
            type = "def";
            normalizeDefReference(data);
        }

        return new EffectDefinition(type, data, nested);
    }

    private boolean hasDefReference(Map<String, Object> data) {
        return data.containsKey("def")
                || data.containsKey("ability")
                || data.containsKey("chain")
                || data.containsKey("call");
    }

    private void normalizeDefReference(Map<String, Object> data) {
        if (!data.containsKey("def")) {
            for (String key : List.of("ability", "chain", "call")) {
                Object value = data.get(key);
                if (value != null && !String.valueOf(value).isBlank()) {
                    data.put("def", String.valueOf(value));
                    break;
                }
            }
        }
    }

    public ChainEntry parseChainEntry(Object raw) {
        return ChainEntryParser.parse(raw);
    }

    private EffectDefinition chainEntryToEffect(ChainEntry entry) {
        Map<String, Object> data = new HashMap<>();
        data.put("def", entry.defId());
        if (!entry.args().isEmpty()) {
            data.put("args", entry.args());
        }
        return new EffectDefinition("def", data, List.of());
    }

    private boolean isEffectListKey(String key) {
        return "on_hit".equalsIgnoreCase(key)
                || "on_land".equalsIgnoreCase(key)
                || "on_cast".equalsIgnoreCase(key)
                || "effects".equalsIgnoreCase(key);
    }
}
