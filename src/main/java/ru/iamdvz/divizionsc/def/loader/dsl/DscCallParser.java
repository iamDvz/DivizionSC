package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import ru.iamdvz.divizionsc.def.effect.EffectVerbs;

/** Парсит вызовы вида {@code heal(6)}, {@code heal 6}, {@code lightning}. */
public final class DscCallParser {

    private static final Set<String> ZERO_ARG_EFFECTS = Set.of(
            "lightning", "lit", "swap", "cleanse", "purge"
    );

    public record ParsedCall(String name, List<ParsedArg> args, int lineNumber, String rawArgs) {
        public ParsedCall(String name, List<ParsedArg> args, int lineNumber) {
            this(name, args, lineNumber, "");
        }
    }

    public record ParsedArg(String name, String value, boolean named) {
    }

    public ParsedCall parseLine(String line, int lineNumber) {
        return parseFlexible(line, lineNumber);
    }

    public ParsedCall parseFlexible(String line, int lineNumber) {
        String trimmed = line.trim();
        if (isParenCall(trimmed)) {
            return parseParenCall(trimmed, lineNumber);
        }
        if (isBareEffect(trimmed)) {
            return new ParsedCall(trimmed.toLowerCase(Locale.ROOT), List.of(), lineNumber);
        }
        ParsedCall shorthand = tryShorthand(trimmed, lineNumber);
        if (shorthand != null) {
            return shorthand;
        }
        throw new DscParseException("Expected effect call name(args): " + trimmed, lineNumber);
    }

    public boolean isCall(String line) {
        return isEffectLine(line);
    }

    public boolean isEffectLine(String line) {
        String trimmed = line.trim();
        return isParenCall(trimmed) || isBareEffect(trimmed) || tryShorthand(trimmed, -1) != null;
    }

    private boolean isParenCall(String trimmed) {
        int paren = trimmed.indexOf('(');
        return paren > 0 && trimmed.endsWith(")");
    }

    private boolean isBareEffect(String trimmed) {
        return ZERO_ARG_EFFECTS.contains(trimmed.toLowerCase(Locale.ROOT));
    }

    private ParsedCall tryShorthand(String trimmed, int lineNumber) {
        int space = trimmed.indexOf(' ');
        if (space <= 0) {
            return null;
        }
        String name = trimmed.substring(0, space).trim();
        if (name.isEmpty() || name.startsWith("@")) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (!EffectVerbs.isVerb(lower) && !isKnownShorthand(lower)) {
            return null;
        }
        String argsPart = trimmed.substring(space + 1).trim();
        if (argsPart.isEmpty()) {
            return null;
        }
        return parseParenCall(lower + "(" + argsPart + ")", lineNumber);
    }

    private boolean isKnownShorthand(String name) {
        return switch (name) {
            case "heal", "damage", "dmg", "message", "msg", "require", "blink", "dash",
                    "pull", "push", "shield", "stun", "sound", "snd", "particles", "particle",
                    "ptl", "potion", "pot", "set", "raycast", "beam", "command", "cmd", "after", "delay",
                    "wait", "chance", "if", "when", "area", "loop", "aura", "repeat",
                    "projectile", "proj", "velocity", "vel", "knockback", "teleport", "tp",
                    "give", "summon", "money", "fx", "effectlib",
                    "ignite", "glow", "glowing", "invis", "invisibility", "title", "swap",
                    "explosion", "explode", "cleanse", "purge", "launch", "root", "chain" -> true;
            default -> false;
        };
    }

    private ParsedCall parseParenCall(String trimmed, int lineNumber) {
        int paren = trimmed.indexOf('(');
        if (paren < 0 || !trimmed.endsWith(")")) {
            throw new DscParseException("Expected effect call name(args): " + trimmed, lineNumber);
        }
        String name = trimmed.substring(0, paren).trim();
        if (name.isEmpty()) {
            throw new DscParseException("Empty effect call name", lineNumber);
        }
        String argsRaw = trimmed.substring(paren + 1, trimmed.length() - 1).trim();
        return new ParsedCall(name, parseArgs(argsRaw), lineNumber, argsRaw);
    }

    private List<ParsedArg> parseArgs(String raw) {
        if (raw.isBlank()) {
            return List.of();
        }
        List<ParsedArg> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        int depth = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                current.append(c);
                continue;
            }
            if (!inQuote) {
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    args.add(parseArg(current.toString().trim()));
                    current.setLength(0);
                    continue;
                }
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            args.add(parseArg(current.toString().trim()));
        }
        if (inQuote) {
            throw new DscParseException("Unclosed quote in effect call arguments");
        }
        return args;
    }

    private ParsedArg parseArg(String token) {
        if (token.isEmpty()) {
            throw new DscParseException("Empty argument in effect call");
        }
        int eq = findNamedEquals(token);
        if (eq > 0) {
            String name = token.substring(0, eq).trim();
            String value = token.substring(eq + 1).trim();
            if (name.isEmpty()) {
                throw new DscParseException("Invalid named argument: " + token);
            }
            return new ParsedArg(name.toLowerCase(Locale.ROOT), unquote(value), true);
        }
        return new ParsedArg(null, unquote(token), false);
    }

    private int findNamedEquals(String token) {
        boolean inQuote = false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (!inQuote && c == '=') {
                return i;
            }
        }
        return -1;
    }

    private String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
