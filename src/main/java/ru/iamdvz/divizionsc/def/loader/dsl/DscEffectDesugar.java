package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.iamdvz.divizionsc.def.effect.EffectVerbs;

/** Преобразует v2 function-call в legacy-строки для {@link ru.iamdvz.divizionsc.def.loader.simple.SimpleEffectParser}. */
public final class DscEffectDesugar {

    private static final Map<String, String> TARGET_ALIASES = Map.of(
            "entity", "entity",
            "target", "target",
            "self", "self",
            "caster", "caster"
    );

    public String desugar(DscCallParser.ParsedCall call) {
        String name = call.name().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "heal" -> "heal " + firstPos(call);
            case "damage", "dmg" -> joinPos(call, "damage");
            case "message", "msg" -> "msg " + messageBody(call);
            case "require" -> "require " + firstPos(call).replace('_', '-');
            case "lightning", "lit" -> boolNamed(call, "fx") ? "lit fx" : "lit";
            case "teleport", "tp" -> "tp " + firstPos(call);
            case "blink" -> "blink " + firstOrNamed(call, "distance", "6");
            case "dash" -> "dash " + firstOrNamed(call, "power", "1.4");
            case "pull" -> "pull " + firstOrNamed(call, "strength", "1");
            case "push" -> "push " + firstOrNamed(call, "strength", "1");
            case "shield" -> "shield " + firstOrNamed(call, "amount", "4");
            case "stun" -> "stun " + firstPos(call);
            case "velocity", "vel", "knockback" -> desugarVelocity(call);
            case "sound", "snd" -> desugarSound(call);
            case "particles", "particle", "ptl" -> desugarParticle(call);
            case "potion", "pot" -> desugarPotion(call);
            case "set", "setvar", "set_var" -> desugarSet(call);
            case "command", "cmd" -> "cmd " + commandBody(call);
            case "repeat" -> "repeat " + firstOrNamed(call, "times", "1");
            case "beam", "raycast" -> desugarRaycast(call);
            case "chain" -> desugarChain(call);
            case "ignite" -> "ignite " + firstOrNamed(call, "ticks", firstOrNamed(call, "duration", "60"));
            case "glow", "glowing" -> "glow " + firstOrNamed(call, "duration", "100");
            case "invis", "invisibility" -> "invis " + firstOrNamed(call, "duration", "100");
            case "root" -> "root " + firstOrNamed(call, "duration", "40");
            case "launch" -> "launch " + firstOrNamed(call, "power", firstOrNamed(call, "y", "1"));
            case "swap" -> "swap";
            case "cleanse", "purge" -> "cleanse";
            case "title" -> desugarTitle(call);
            case "fx", "effectlib" -> desugarFx(call);
            case "give" -> desugarGive(call);
            case "summon" -> desugarSummon(call);
            case "money", "give-money", "take-money" -> desugarMoney(name, call);
            default -> {
                if (EffectVerbs.isVerb(name)) {
                    yield rebuildLegacy(name, call);
                }
                throw new DscParseException("Unknown effect: " + name, call.lineNumber());
            }
        };
    }

    public boolean isBlockEffect(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "after", "delay", "wait", "area", "loop", "aura", "projectile", "chance", "if",
                    "raycast", "beam", "chain" -> true;
            default -> false;
        };
    }

    public String blockVerb(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "after", "delay", "wait" -> "delay";
            case "projectile" -> "projectile";
            default -> name.toLowerCase(Locale.ROOT);
        };
    }

    private String desugarSound(DscCallParser.ParsedCall call) {
        String sound = firstPosOrNamed(call, "sound", "entity_player_levelup");
        double volume = parseDouble(named(call, "volume"), 1.0);
        double pitch = parseDouble(named(call, "pitch"), 1.0);
        return "snd " + sound + " " + volume + " " + pitch;
    }

    private String desugarParticle(DscCallParser.ParsedCall call) {
        String particle = firstPosOrNamed(call, "particle", "FLAME");
        int count = (int) parseDouble(named(call, "count"), 10);
        if (named(call, "offset") != null) {
            return "ptl " + particle + " " + count + " " + named(call, "offset");
        }
        return "ptl " + particle + " " + count;
    }

    private String desugarPotion(DscCallParser.ParsedCall call) {
        String effect = firstPos(call);
        String duration = named(call, "duration") != null ? named(call, "duration") : "5s";
        String amplifier = named(call, "amplifier") != null ? named(call, "amplifier") : "0";
        String target = targetPos(call);
        if (target != null) {
            return "pot " + effect + " " + duration + " " + amplifier + " " + target;
        }
        return "pot " + effect + " " + duration + " " + amplifier;
    }

    private String desugarVelocity(DscCallParser.ParsedCall call) {
        if (named(call, "x") != null || named(call, "y") != null || named(call, "z") != null) {
            return "vel "
                    + orZero(named(call, "x")) + " "
                    + orZero(named(call, "y")) + " "
                    + orZero(named(call, "z"));
        }
        List<String> positional = positionalArgs(call);
        if (positional.size() >= 3) {
            return "vel " + positional.get(0) + " " + positional.get(1) + " " + positional.get(2);
        }
        return "vel " + firstOrNamed(call, "power", "1");
    }

    private String desugarSet(DscCallParser.ParsedCall call) {
        if (call.args().size() >= 2 && !call.args().get(1).named()) {
            return "set " + firstPos(call) + " " + call.args().get(1).value();
        }
        String var = named(call, "var") != null ? named(call, "var") : firstPos(call);
        String value = named(call, "value") != null ? named(call, "value") : "0";
        return "set " + var + " " + value;
    }

    private String desugarRaycast(DscCallParser.ParsedCall call) {
        String distance = firstOrNamed(call, "distance", "15");
        String radius = named(call, "hit_radius");
        String hits = named(call, "max_hits");
        if (hits == null) {
            hits = named(call, "hits");
        }
        if (hits == null) {
            hits = named(call, "targets");
        }
        StringBuilder sb = new StringBuilder("raycast ").append(distance);
        if (radius != null) {
            sb.append(' ').append(radius);
        }
        if (hits != null) {
            if (radius == null) {
                sb.append(" 1");
            }
            sb.append(' ').append(hits);
        }
        return sb.toString().trim();
    }

    private String desugarChain(DscCallParser.ParsedCall call) {
        String distance = firstOrNamed(call, "distance", "18");
        String hits = firstOrNamed(call, "hits", firstOrNamed(call, "times", firstOrNamed(call, "targets", "3")));
        String radius = named(call, "hit_radius");
        StringBuilder sb = new StringBuilder("chain ").append(distance).append(' ').append(hits);
        if (radius != null) {
            sb.append(' ').append(radius);
        }
        return sb.toString();
    }

    private String desugarExplosion(DscCallParser.ParsedCall call) {
        StringBuilder sb = new StringBuilder("explosion ");
        sb.append(firstOrNamed(call, "power", firstOrNamed(call, "yield", "0")));
        if (boolNamed(call, "fire")) {
            sb.append(" fire");
        }
        if (boolNamed(call, "break") || boolNamed(call, "break_blocks")) {
            sb.append(" break");
        }
        return sb.toString();
    }

    private String desugarTitle(DscCallParser.ParsedCall call) {
        String actionbar = named(call, "actionbar");
        if (actionbar != null && !actionbar.isBlank()) {
            return "title actionbar " + quoteIfNeeded(actionbar);
        }
        String title = named(call, "title");
        if (title == null) {
            for (DscCallParser.ParsedArg arg : call.args()) {
                if (!arg.named()) {
                    title = arg.value();
                    break;
                }
            }
        }
        if (title == null) {
            title = "";
        }
        if (title.isBlank()) {
            title = "\"\"";
        } else if (!title.startsWith("\"")) {
            title = quoteIfNeeded(title);
        }
        StringBuilder sb = new StringBuilder("title ").append(title);
        String subtitle = named(call, "subtitle");
        if (subtitle != null) {
            sb.append(' ').append(quoteIfNeeded(subtitle));
        }
        return sb.toString();
    }

    private String desugarFx(DscCallParser.ParsedCall call) {
        List<String> parts = new ArrayList<>();
        parts.add("fx");
        parts.add(firstPosOrNamed(call, "shape", "sphere"));
        if (named(call, "particle") != null) {
            parts.add(named(call, "particle"));
        } else if (call.args().size() > 1 && !call.args().get(1).named()) {
            parts.add(call.args().get(1).value());
        }
        for (String key : List.of("radius", "particles", "iterations", "period")) {
            if (named(call, key) != null) {
                parts.add(named(call, key));
            }
        }
        return String.join(" ", parts);
    }

    private String desugarGive(DscCallParser.ParsedCall call) {
        String material = firstPos(call);
        String amount = named(call, "amount") != null ? named(call, "amount") : "1";
        return "give " + material + " " + amount;
    }

    private String desugarSummon(DscCallParser.ParsedCall call) {
        String entity = firstPos(call);
        String count = named(call, "count") != null ? named(call, "count") : "1";
        return "summon " + entity + " " + count;
    }

    private String desugarMoney(String name, DscCallParser.ParsedCall call) {
        return name + " " + firstOrNamed(call, "amount", "0");
    }

    private String rebuildLegacy(String name, DscCallParser.ParsedCall call) {
        StringBuilder sb = new StringBuilder(name);
        for (DscCallParser.ParsedArg arg : call.args()) {
            sb.append(' ');
            if (arg.named()) {
                sb.append(arg.value());
            } else {
                sb.append(arg.value());
            }
        }
        return sb.toString().trim();
    }

    private String joinPos(DscCallParser.ParsedCall call, String verb) {
        String amount = firstPos(call);
        String target = targetPos(call);
        if (target != null) {
            return verb + " " + amount + " " + target;
        }
        return verb + " " + amount;
    }

    private String firstPos(DscCallParser.ParsedCall call) {
        for (DscCallParser.ParsedArg arg : call.args()) {
            if (!arg.named()) {
                return arg.value();
            }
        }
        throw new DscParseException("Missing positional argument for " + call.name(), call.lineNumber());
    }

    private String firstOrNamed(DscCallParser.ParsedCall call, String key, String fallback) {
        String named = named(call, key);
        if (named != null) {
            return named;
        }
        for (DscCallParser.ParsedArg arg : call.args()) {
            if (!arg.named()) {
                return arg.value();
            }
        }
        return fallback;
    }

    private String firstPosOrNamed(DscCallParser.ParsedCall call, String key, String fallback) {
        String named = named(call, key);
        if (named != null) {
            return named;
        }
        for (DscCallParser.ParsedArg arg : call.args()) {
            if (!arg.named()) {
                return arg.value();
            }
        }
        return fallback;
    }

    private String targetPos(DscCallParser.ParsedCall call) {
        String target = named(call, "target");
        if (target != null) {
            return TARGET_ALIASES.getOrDefault(target.toLowerCase(Locale.ROOT), target);
        }
        for (DscCallParser.ParsedArg arg : call.args()) {
            if (!arg.named() && TARGET_ALIASES.containsKey(arg.value().toLowerCase(Locale.ROOT))) {
                return arg.value().toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    private String named(DscCallParser.ParsedCall call, String key) {
        for (DscCallParser.ParsedArg arg : call.args()) {
            if (arg.named() && key.equalsIgnoreCase(arg.name())) {
                return arg.value();
            }
        }
        return null;
    }

    private boolean boolNamed(DscCallParser.ParsedCall call, String key) {
        String value = named(call, key);
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String orZero(String value) {
        return value == null ? "0" : value;
    }

    /** Полный текст команды — сохраняет запятые/пробелы (без разбиения по аргументам). */
    private String commandBody(DscCallParser.ParsedCall call) {
        String raw = call.rawArgs();
        return (raw == null || raw.isBlank()) ? firstPos(call) : raw.trim();
    }

    /** Текст сообщения — целиком, со снятой внешней парой кавычек. */
    private String messageBody(DscCallParser.ParsedCall call) {
        String raw = call.rawArgs();
        String body = (raw == null || raw.isBlank()) ? firstPos(call) : raw.trim();
        if (body.length() >= 2 && body.startsWith("\"") && body.endsWith("\"")) {
            return body.substring(1, body.length() - 1);
        }
        return body;
    }

    private String quoteIfNeeded(String value) {
        if (value.contains(" ")) {
            return "\"" + value + "\"";
        }
        return value;
    }

    public Map<String, String> namedArgs(DscCallParser.ParsedCall call) {
        Map<String, String> map = new HashMap<>();
        for (DscCallParser.ParsedArg arg : call.args()) {
            if (arg.named()) {
                map.put(arg.name(), arg.value());
            }
        }
        return map;
    }

    public List<String> positionalArgs(DscCallParser.ParsedCall call) {
        List<String> list = new ArrayList<>();
        for (DscCallParser.ParsedArg arg : call.args()) {
            if (!arg.named()) {
                list.add(arg.value());
            }
        }
        return list;
    }
}
