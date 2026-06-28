package ru.iamdvz.divizionsc.def.loader;

import ru.iamdvz.divizionsc.def.model.ChainEntry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ChainEntryParser {

    private ChainEntryParser() {
    }

    public static ChainEntry parse(Object raw) {
        if (raw instanceof String defId) {
            return parseString(defId);
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        String defId = readString(map, "def", readString(map, "id", readString(map, "call", "")));
        if (defId.isBlank()) {
            return null;
        }
        return new ChainEntry(defId.toLowerCase(Locale.ROOT), readArgs(map.get("args")));
    }

    public static ChainEntry parseString(String raw) {
        String defId = raw.trim();
        if (defId.startsWith("@")) {
            defId = defId.substring(1).trim();
        }
        int paren = defId.indexOf('(');
        if (paren >= 0 && defId.endsWith(")")) {
            String id = defId.substring(0, paren).trim().toLowerCase(Locale.ROOT);
            String argsRaw = defId.substring(paren + 1, defId.length() - 1).trim();
            return new ChainEntry(id, parseInlineArgs(argsRaw));
        }
        return new ChainEntry(defId.toLowerCase(Locale.ROOT), Map.of());
    }

    public static Map<String, Object> parseInlineArgs(String raw) {
        if (raw.isBlank()) {
            return Map.of();
        }
        Map<String, Object> args = new HashMap<>();
        if (raw.contains(":") || raw.contains("=")) {
            for (String part : raw.split(",")) {
                String[] kv = part.split("[=:]", 2);
                if (kv.length == 2) {
                    args.put(kv[0].trim(), parseScalar(kv[1].trim()));
                }
            }
            return args;
        }
        Object value = parseScalar(raw);
        args.put("amount", value);
        args.put("dmg", value);
        args.put("damage", value);
        return args;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readArgs(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> args = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            args.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return args;
    }

    private static Object parseScalar(String raw) {
        try {
            if (raw.contains(".")) {
                return Double.parseDouble(raw);
            }
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private static String readString(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}
