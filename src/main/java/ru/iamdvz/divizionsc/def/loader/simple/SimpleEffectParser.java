package ru.iamdvz.divizionsc.def.loader.simple;

import ru.iamdvz.divizionsc.def.effect.EffectVerbs;
import ru.iamdvz.divizionsc.def.expr.ExpressionEvaluator;
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
        String type = EffectVerbs.canonicalType(head);
        if (type == null) {
            return ParseResult.useVerbose();
        }

        return switch (type) {
            case "heal" -> amountEffect("heal", parts, 4.0);
            case "damage" -> amountEffect("damage", parts, 1.0);
            case "teleport" -> amountEffect("teleport", parts, 5.0, "forward");
            case "velocity" -> velocity(parts);
            case "sound" -> sound(parts);
            case "particle" -> particle(parts);
            case "potion" -> potion(parts);
            case "lightning" -> lightning(parts);
            case "message" -> message(parts);
            case "def" -> callDef(joinTail(parts, 1));
            case "projectile" -> projectile(parts);
            case "effectlib" -> effectLib(parts);
            case "vfx" -> modelEngineVfx(parts);
            case "command" -> command(parts);
            case "set" -> setVar(parts);
            case "require" -> require(parts);
            case "dash" -> powerEffect("dash", parts, 1.4, "power");
            case "blink" -> powerEffect("blink", parts, 6.0, "distance");
            case "pull" -> powerEffect("pull", parts, 1.0, "strength");
            case "push" -> powerEffect("push", parts, 1.0, "strength");
            case "shield" -> shield(parts);
            case "give-money" -> moneyLine("give-money", parts);
            case "take-money" -> moneyLine("take-money", parts);
            case "money" -> moneyLine("money", parts);
            case "give" -> giveItem(parts);
            case "summon" -> summon(parts);
            case "stun" -> stun(parts);
            case "raycast" -> raycast(parts);
            case "chain" -> chain(parts);
            case "ignite" -> durationEffect("ignite", parts, "ticks", 60);
            case "glow", "glowing" -> durationEffect("glow", parts, "duration", 100);
            case "invis", "invisibility" -> durationEffect("invis", parts, "duration", 100);
            case "root" -> durationEffect("root", parts, "duration", 40);
            case "launch" -> powerEffect("launch", parts, 1.0, "power");
            case "swap" -> ParseResult.parsed(new EffectDefinition("swap", Map.of(), List.of()));
            case "explosion", "explode" -> explosion(parts);
            case "cleanse", "purge" -> ParseResult.parsed(new EffectDefinition("cleanse", Map.of(), List.of()));
            case "title" -> title(parts);
            case "particle_projectile" -> particleProjectile(parts);
            case "area" -> area(parts);
            case "loop" -> loop(parts);
            case "aura" -> aura(parts);
            case "shape" -> shape(parts);
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
        data.put(field, parts.length > 1 ? numberOrText(parts[1], defaultAmount) : defaultAmount);
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

    private ParseResult setVar(String[] parts) {
        if (parts.length < 3) {
            return ParseResult.useVerbose();
        }
        String name = parts[1];
        String rest = joinTail(parts, 2).trim();
        if (rest.startsWith("=")) {
            rest = rest.substring(1).trim();
        }
        Map<String, Object> data = new HashMap<>();
        data.put("var", name);
        if (rest.length() >= 2 && rest.startsWith("\"") && rest.endsWith("\"")) {
            data.put("string", rest.substring(1, rest.length() - 1));
        } else {
            data.put("value", rest);
        }
        return ParseResult.parsed(new EffectDefinition("set", data, List.of()));
    }

    private ParseResult require(String[] parts) {
        String condition = joinTail(parts, 1).trim();
        if (condition.isBlank()) {
            return ParseResult.useVerbose();
        }
        Map<String, Object> data = new HashMap<>();
        data.put("condition", condition);
        return ParseResult.parsed(new EffectDefinition("require", data, List.of()));
    }

    private ParseResult powerEffect(String type, String[] parts, double defaultValue, String field) {
        Map<String, Object> data = new HashMap<>();
        data.put(field, parts.length > 1 ? parseDouble(parts[1], defaultValue) : defaultValue);
        return ParseResult.parsed(new EffectDefinition(type, data, List.of()));
    }

    private ParseResult shield(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length > 1) {
            data.put("hearts", parseDouble(parts[1], 4));
        }
        if (parts.length > 2) {
            String raw = parts[2];
            if (raw.endsWith("s") || raw.endsWith("t")) {
                data.put("duration", parseDuration(raw, 200));
            } else {
                data.put("duration", (int) Math.round(parseDouble(raw, 10) * 20));
            }
        }
        return ParseResult.parsed(new EffectDefinition("shield", data, List.of()));
    }

    private ParseResult moneyLine(String type, String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if ("money".equals(type) && parts.length > 1) {
            String op = parts[1].toLowerCase(Locale.ROOT);
            if (op.startsWith("take") || op.startsWith("withdraw")) {
                data.put("op", "take");
                if (parts.length > 2) {
                    data.put("amount", parseDouble(parts[2], 0));
                }
            } else if (op.startsWith("give") || op.startsWith("deposit")) {
                data.put("op", "give");
                if (parts.length > 2) {
                    data.put("amount", parseDouble(parts[2], 0));
                }
            } else {
                data.put("amount", parseDouble(parts[1], 0));
            }
        } else if (parts.length > 1) {
            data.put("amount", parseDouble(parts[1], 0));
        }
        return ParseResult.parsed(new EffectDefinition(type, data, List.of()));
    }

    private ParseResult giveItem(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        data.put("material", (parts.length > 1 ? parts[1] : "STONE").toUpperCase(Locale.ROOT));
        if (parts.length > 2) {
            data.put("amount", (int) parseDouble(parts[2], 1));
        }
        return ParseResult.parsed(new EffectDefinition("give", data, List.of()));
    }

    private ParseResult summon(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        data.put("entity", (parts.length > 1 ? parts[1] : "ZOMBIE").toUpperCase(Locale.ROOT));
        if (parts.length > 2) {
            data.put("count", (int) parseDouble(parts[2], 1));
        }
        return ParseResult.parsed(new EffectDefinition("summon", data, List.of()));
    }

    private ParseResult stun(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length > 1) {
            data.put("duration", parseDuration(parts[1], 40));
        }
        return ParseResult.parsed(new EffectDefinition("stun", data, List.of()));
    }

    private ParseResult raycast(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length > 1) {
            data.put("distance", parseDouble(parts[1], 15));
        }
        if (parts.length > 2) {
            data.put("hit_radius", parseDouble(parts[2], 1));
        }
        if (parts.length > 3) {
            data.put("max_hits", (int) parseDouble(parts[3], 1));
        }
        return ParseResult.parsed(new EffectDefinition("raycast", data, List.of()));
    }

    private ParseResult chain(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length > 1) {
            data.put("distance", parseDouble(parts[1], 18));
        }
        if (parts.length > 2) {
            data.put("max_hits", (int) parseDouble(parts[2], 3));
        }
        if (parts.length > 3) {
            data.put("hit_radius", parseDouble(parts[3], 1.5));
        }
        return ParseResult.parsed(new EffectDefinition("chain", data, List.of()));
    }

    private ParseResult durationEffect(String type, String[] parts, String field, int defaultValue) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length > 1) {
            data.put(field, (int) parseDouble(parts[1], defaultValue));
        } else {
            data.put(field, defaultValue);
        }
        return ParseResult.parsed(new EffectDefinition(type, data, List.of()));
    }

    private ParseResult explosion(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String token = parts[i].toLowerCase(Locale.ROOT);
            if ("fire".equals(token)) {
                data.put("fire", true);
            } else if ("break".equals(token) || "break_blocks".equals(token)) {
                data.put("break_blocks", true);
            } else if (!data.containsKey("power")) {
                data.put("power", parseDouble(parts[i], 0));
            }
        }
        return ParseResult.parsed(new EffectDefinition("explosion", data, List.of()));
    }

    private ParseResult title(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length > 2 && "actionbar".equalsIgnoreCase(parts[1])) {
            data.put("actionbar", unquoteToken(parts[2]));
            return ParseResult.parsed(new EffectDefinition("title", data, List.of()));
        }
        if (parts.length > 1) {
            data.put("title", joinTail(parts, 1));
        }
        return ParseResult.parsed(new EffectDefinition("title", data, List.of()));
    }

    private ParseResult particleProjectile(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length > 1) {
            data.put("particle", EffectKeys.normalizeParticleName(parts[1]));
        }
        if (parts.length > 2) {
            data.put("speed", parseDouble(parts[2], 0.75));
        }
        if (parts.length > 3) {
            data.put("max_distance", parseDouble(parts[3], 15));
        }
        return ParseResult.parsed(new EffectDefinition("particle_projectile", data, List.of()));
    }

    private ParseResult area(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length > 1) {
            data.put("radius", parseDouble(parts[1], 5));
        }
        return ParseResult.parsed(new EffectDefinition("area", data, List.of()));
    }

    private ParseResult loop(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length > 1) {
            data.put("iterations", (int) parseDouble(parts[1], 1));
        }
        if (parts.length > 2) {
            data.put("interval", (int) parseDouble(parts[2], 1));
        }
        return ParseResult.parsed(new EffectDefinition("loop", data, List.of()));
    }

    private ParseResult aura(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        if (parts.length > 1) {
            data.put("radius", parseDouble(parts[1], 4));
        }
        if (parts.length > 2) {
            data.put("duration", parseDuration(parts[2], 100));
        }
        if (parts.length > 3) {
            data.put("interval", (int) parseDouble(parts[3], 20));
        }
        return ParseResult.parsed(new EffectDefinition("aura", data, List.of()));
    }

    private ParseResult shape(String[] parts) {
        Map<String, Object> data = new HashMap<>();
        data.put("shape", parts.length > 1 ? parts[1] : "circle");
        if (parts.length > 2) {
            data.put("particle", EffectKeys.normalizeParticleName(parts[2]));
        }
        if (parts.length > 3) {
            data.put("radius", parseDouble(parts[3], 2));
        }
        return ParseResult.parsed(new EffectDefinition("shape", data, List.of()));
    }

    private ParseResult modelEngineVfx(String[] parts) {
        if (parts.length < 3) {
            return ParseResult.useVerbose();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("model", parts[1]);

        Map<String, Object> animation = new HashMap<>();
        animation.put("name", parts[2]);
        data.put("animations", List.of(animation));

        int positional = 0;
        for (int i = 3; i < parts.length; i++) {
            String token = parts[i].toLowerCase(Locale.ROOT);
            switch (token) {
                case "follow", "follow-target" -> data.put("follow-target", true);
                case "follow-caster", "follow-self" -> data.put("follow", "caster");
                case "mount", "mount-target" -> data.put("mount-target", true);
                case "glow", "glowing" -> data.put("glowing", true);
                case "loop" -> animation.put("loop", true);
                case "self", "caster", "target", "eyes", "effect" -> data.put("at", token);
                default -> {
                    if (isNumeric(token)) {
                        if (positional == 0) {
                            data.put("model-scale", parseDouble(token, 1.0));
                            positional++;
                        } else if (positional == 1) {
                            data.put("remove-delay", (int) parseDouble(token, 40));
                        }
                    }
                }
            }
        }

        return ParseResult.parsed(new EffectDefinition("vfx", data, List.of()));
    }

    private boolean isNumeric(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(raw);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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

    /** Число — как Double; иначе сохраняем текст (переменная/формула, разрешается в рантайме). */
    private Object numberOrText(String token, double fallback) {
        Double parsed = ExpressionEvaluator.tryParse(token);
        return parsed != null ? parsed : token;
    }

    private String unquoteToken(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
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
