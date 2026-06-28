package ru.iamdvz.divizionsc.def.loader.simple;

import ru.iamdvz.divizionsc.def.loader.ChainEntryParser;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.divizionsc.util.EffectKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SimpleEffectParser {

    private static final Pattern DELAY_INLINE = Pattern.compile("^(?:wait|delay)\\s+(\\d+(?:\\.\\d+)?)([st]?)(?:\\s*:\\s*(.+))?$", Pattern.CASE_INSENSITIVE);

    public ParseResult parse(Object raw) {
        if (raw instanceof String line) {
            return parseLine(line.trim());
        }
        if (raw instanceof Map<?, ?> map) {
            return parseMap(map);
        }
        return ParseResult.useVerbose();
    }

    private ParseResult parseMap(Map<?, ?> map) {
        Object wait = map.containsKey("wait") ? map.get("wait") : map.get("delay");
        if (wait != null) {
            int ticks = parseDuration(String.valueOf(wait), 20);
            List<EffectDefinition> nested = parseList(map.get("do"));
            if (nested.isEmpty()) {
                nested = parseList(map.get("effects"));
            }
            Map<String, Object> data = new HashMap<>();
            data.put("ticks", ticks);
            if (!nested.isEmpty()) {
                data.put("effects", nested);
            }
            return ParseResult.parsed(new EffectDefinition("delay", data, List.of()));
        }
        ParseResult defCall = parseDefMap(map);
        if (defCall != null) {
            return defCall;
        }
        if (map.containsKey("type")) {
            return ParseResult.useVerbose();
        }
        return ParseResult.useVerbose();
    }

    private ParseResult parseDefMap(Map<?, ?> map) {
        String defId = null;
        for (String key : List.of("def", "call", "chain", "ability")) {
            Object value = map.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                defId = String.valueOf(value);
                break;
            }
        }
        if (defId == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("def", defId.toLowerCase(Locale.ROOT));
        Object args = map.get("args");
        if (args instanceof Map<?, ?> argsMap) {
            Map<String, Object> parsedArgs = new HashMap<>();
            for (Map.Entry<?, ?> entry : argsMap.entrySet()) {
                parsedArgs.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            data.put("args", parsedArgs);
        }
        return ParseResult.parsed(new EffectDefinition("def", data, List.of()));
    }

    private ParseResult parseLine(String line) {
        if (line.isEmpty()) {
            return ParseResult.ignored();
        }
        if (line.startsWith("@")) {
            return callDef(line.substring(1).trim());
        }

        Matcher delayMatcher = DELAY_INLINE.matcher(line);
        if (delayMatcher.matches()) {
            int ticks = parseDuration(delayMatcher.group(1) + delayMatcher.group(2), 20);
            Map<String, Object> data = new HashMap<>();
            data.put("ticks", ticks);
            String tail = delayMatcher.group(3);
            if (tail != null && !tail.isBlank()) {
                List<EffectDefinition> nested = new ArrayList<>();
                for (String part : tail.split(",")) {
                    ParseResult parsed = parseLine(part.trim());
                    if (parsed.effect() != null) {
                        nested.add(parsed.effect());
                    }
                }
                data.put("effects", nested);
            }
            return ParseResult.parsed(new EffectDefinition("delay", data, List.of()));
        }

        String[] parts = line.split("\\s+");
        String head = parts[0].toLowerCase(Locale.ROOT);

        return switch (head) {
            case "heal" -> amountEffect("heal", parts, 4.0);
            case "dmg", "damage" -> amountEffect("damage", parts, 1.0);
            case "tp", "teleport" -> amountEffect("teleport", parts, 5.0, "forward");
            case "vel", "velocity" -> velocity(parts);
            case "snd", "sound" -> sound(parts);
            case "ptl", "particle" -> particle(parts);
            case "pot", "potion" -> potion(parts);
            case "lit", "lightning" -> lightning(parts);
            case "msg", "message" -> message(parts);
            case "call", "def", "chain" -> callDef(joinTail(parts, 1));
            case "proj", "projectile" -> projectile(parts);
            case "fx", "effectlib" -> effectLib(parts);
            case "cmd", "command" -> command(parts);
            default -> ParseResult.useVerbose();
        };
    }

    private ParseResult callDef(String raw) {
        if (raw.isBlank()) {
            return ParseResult.ignored();
        }
        var entry = ChainEntryParser.parseString(raw);
        Map<String, Object> data = new HashMap<>();
        data.put("def", entry.defId());
        if (!entry.args().isEmpty()) {
            data.put("args", entry.args());
        }
        return ParseResult.parsed(new EffectDefinition("def", data, List.of()));
    }

    private ParseResult amountEffect(String type, String[] parts, double defaultAmount) {
        return amountEffect(type, parts, defaultAmount, "amount");
    }

    private ParseResult amountEffect(String type, String[] parts, double defaultAmount, String field) {
        Map<String, Object> data = new HashMap<>();
        data.put(field, parts.length > 1 ? parseDouble(parts[1], defaultAmount) : defaultAmount);
        if ("heal".equals(type)) {
            data.put("target", "self");
        }
        if (parts.length > 2 && "damage".equals(type)) {
            data.put("target", parts[2]);
        }
        return ParseResult.parsed(new EffectDefinition(type, data, List.of()));
    }

    private ParseResult velocity(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length >= 4) {
            data.put("x", parseDouble(parts[1], 0));
            data.put("y", parseDouble(parts[2], 0));
            data.put("z", parseDouble(parts[3], 0));
        } else {
            data.put("power", parts.length > 1 ? parseDouble(parts[1], 1.0) : 1.0);
            if (parts.length > 2) {
                data.put("y", parseDouble(parts[2], 0.3));
            }
        }
        return ParseResult.parsed(new EffectDefinition("velocity", data, List.of()));
    }

    private ParseResult sound(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        data.put("sound", normalizeSound(parts.length > 1 ? parts[1] : "entity.experience_orb.pickup"));
        if (parts.length > 2) {
            data.put("volume", parseDouble(parts[2], 1.0));
        }
        if (parts.length > 3) {
            data.put("pitch", parseDouble(parts[3], 1.0));
        }
        return ParseResult.parsed(new EffectDefinition("sound", data, List.of()));
    }

    private ParseResult particle(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        String particle = parts.length > 1 ? parts[1] : "FLAME";
        data.put("particle", EffectKeys.normalizeParticleName(particle));
        data.put("count", parts.length > 2 ? (int) parseDouble(parts[2], 10) : 10);
        if (parts.length > 3) {
            data.put("offset", parseDouble(parts[3], 0.3));
        }
        return ParseResult.parsed(new EffectDefinition("particle", data, List.of()));
    }

    private ParseResult potion(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        data.put("effect", (parts.length > 1 ? parts[1] : "SPEED").toUpperCase(Locale.ROOT));
        if (parts.length > 2) {
            data.put("duration", parseDuration(parts[2], 100));
        }
        if (parts.length > 3) {
            data.put("amplifier", (int) parseDouble(parts[3], 0));
        }
        return ParseResult.parsed(new EffectDefinition("potion", data, List.of()));
    }

    private ParseResult lightning(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        boolean damage = parts.length <= 1 || !"fx".equalsIgnoreCase(parts[1]) && !"false".equalsIgnoreCase(parts[1]);
        data.put("damage", damage);
        return ParseResult.parsed(new EffectDefinition("lightning", data, List.of()));
    }

    private ParseResult message(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        data.put("text", joinTail(parts, 1));
        return ParseResult.parsed(new EffectDefinition("message", data, List.of()));
    }

    private ParseResult projectile(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        data.put("projectile", (parts.length > 1 ? parts[1] : "SNOWBALL").toUpperCase(Locale.ROOT));
        if (parts.length > 2) {
            data.put("speed", parseDouble(parts[2], 1.2));
        }
        return ParseResult.parsed(new EffectDefinition("projectile", data, List.of()));
    }

    private ParseResult effectLib(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        String shape = parts.length > 1 ? parts[1] : "sphere";
        data.put("class", shapeClass(shape));
        if (parts.length > 2) {
            data.put("particle", EffectKeys.normalizeParticleName(parts[2]));
        }
        if (parts.length > 3) {
            data.put("radius", parseDouble(parts[3], 1.0));
        }
        if (parts.length > 4) {
            data.put("particles", (int) parseDouble(parts[4], 20));
        }
        if (parts.length > 5) {
            data.put("iterations", (int) parseDouble(parts[5], 15));
        }
        if (parts.length > 6) {
            data.put("period", (int) parseDouble(parts[6], 2));
        }
        data.putIfAbsent("at", "self");
        return ParseResult.parsed(new EffectDefinition("effectlib", data, List.of()));
    }

    private ParseResult command(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        data.put("command", joinTail(parts, 1));
        return ParseResult.parsed(new EffectDefinition("command", data, List.of()));
    }

    private String shapeClass(String shape) {
        return switch (shape.toLowerCase(Locale.ROOT)) {
            case "helix" -> "HelixEffect";
            case "circle" -> "CircleEffect";
            case "line" -> "LineEffect";
            default -> "SphereEffect";
        };
    }

    private String normalizeSound(String raw) {
        String normalized = raw.toUpperCase(Locale.ROOT).replace('.', '_');
        if (normalized.startsWith("ENTITY_") || normalized.startsWith("BLOCK_") || normalized.startsWith("ITEM_")) {
            return normalized;
        }
        return "ENTITY_" + normalized;
    }

    private List<EffectDefinition> parseList(Object raw) {
        List<EffectDefinition> result = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return result;
        }
        for (Object item : list) {
            ParseResult parsed = parse(item);
            if (parsed.effect() != null) {
                result.add(parsed.effect());
            }
        }
        return result;
    }

    private int parseDuration(String raw, int defaultTicks) {
        if (raw == null || raw.isBlank()) {
            return defaultTicks;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.endsWith("s")) {
            return (int) Math.round(parseDouble(lower.substring(0, lower.length() - 1), 1) * 20);
        }
        if (lower.endsWith("t")) {
            return (int) Math.round(parseDouble(lower.substring(0, lower.length() - 1), defaultTicks));
        }
        return (int) Math.round(parseDouble(lower, defaultTicks));
    }

    private double parseDouble(String raw, double fallback) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String joinTail(String[] parts, int from) {
        if (from >= parts.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder(parts[from]);
        for (int i = from + 1; i < parts.length; i++) {
            builder.append(' ').append(parts[i]);
        }
        return builder.toString();
    }

    public record ParseResult(EffectDefinition effect, boolean verboseMode, boolean skip) {
        static ParseResult parsed(EffectDefinition effect) {
            return new ParseResult(effect, false, false);
        }

        static ParseResult useVerbose() {
            return new ParseResult(null, true, false);
        }

        static ParseResult ignored() {
            return new ParseResult(null, false, true);
        }
    }
}
