package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.iamdvz.divizionsc.def.loader.ChainEntryParser;
import ru.iamdvz.divizionsc.def.model.ChainEntry;

/** Разбирает строки с одним или несколькими {@code @module} и опциональным {@code with}. */
public final class DscModuleCallParser {

    private static final Pattern MODULE_TOKEN = Pattern.compile(
            "@([a-zA-Z][a-zA-Z0-9_]*)(?:\\(([^)]*)\\))?"
    );

    private DscModuleCallParser() {
    }

    public record ParsedLine(List<DscStatement.ModuleCall> calls) {
    }

    public static ParsedLine parse(String head, int lineNumber) {
        String body = head.trim();
        if (body.isEmpty()) {
            throw new DscParseException("Empty @module call", lineNumber);
        }

        String withClause = extractWithClause(body);
        String main = withClause == null ? body : body.substring(0, body.length() - withSuffixLength(body)).trim();

        List<DscStatement.ModuleCall> calls = new ArrayList<>();
        if (main.contains(",") || main.contains("+") || main.indexOf('@', 1) >= 0) {
            calls.addAll(parseTokens(main, lineNumber));
        } else {
            calls.add(fromChainEntry(ChainEntryParser.parseString(main), lineNumber));
        }
        if (calls.isEmpty()) {
            calls.addAll(parseTokens(main, lineNumber));
        }
        if (withClause != null && !withClause.isBlank()) {
            calls.addAll(parseWithModules(withClause, lineNumber));
        }
        if (calls.isEmpty()) {
            throw new DscParseException("Expected @module call: " + head, lineNumber);
        }
        return new ParsedLine(calls);
    }

    public static boolean looksLikeModuleLine(String head) {
        return head != null && head.trim().startsWith("@");
    }

    private static DscStatement.ModuleCall fromChainEntry(ChainEntry entry, int lineNumber) {
        if (entry.defId().isBlank()) {
            throw new DscParseException("Expected @module call", lineNumber);
        }
        Map<String, String> named = new LinkedHashMap<>();
        for (Map.Entry<String, Object> arg : entry.args().entrySet()) {
            named.put(arg.getKey().toLowerCase(Locale.ROOT), String.valueOf(arg.getValue()));
        }
        return new DscStatement.ModuleCall(entry.defId(), List.of(), named);
    }

    private static List<DscStatement.ModuleCall> parseTokens(String body, int lineNumber) {
        List<DscStatement.ModuleCall> calls = new ArrayList<>();
        Matcher matcher = MODULE_TOKEN.matcher(body);
        while (matcher.find()) {
            calls.add(toModuleCall(matcher.group(1), matcher.group(2), lineNumber));
        }
        return calls;
    }

    private static List<DscStatement.ModuleCall> parseWithModules(String clause, int lineNumber) {
        List<DscStatement.ModuleCall> calls = new ArrayList<>();
        if (clause.contains("@")) {
            calls.addAll(parseTokens(clause, lineNumber));
            return calls;
        }
        for (String part : clause.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int paren = trimmed.indexOf('(');
            if (paren >= 0 && trimmed.endsWith(")")) {
                calls.add(toModuleCall(
                        trimmed.substring(0, paren).trim(),
                        trimmed.substring(paren + 1, trimmed.length() - 1).trim(),
                        lineNumber
                ));
            } else {
                calls.add(toModuleCall(trimmed, null, lineNumber));
            }
        }
        return calls;
    }

    private static DscStatement.ModuleCall toModuleCall(String idRaw, String argsRaw, int lineNumber) {
        String id = idRaw.toLowerCase(Locale.ROOT);
        Map<String, String> named = new LinkedHashMap<>();
        if (argsRaw != null && !argsRaw.isBlank()) {
            for (Map.Entry<String, Object> entry : ChainEntryParser.parseInlineArgs(argsRaw).entrySet()) {
                named.put(entry.getKey().toLowerCase(Locale.ROOT), String.valueOf(entry.getValue()));
            }
        }
        return new DscStatement.ModuleCall(id, List.of(), named);
    }

    private static String extractWithClause(String body) {
        int depth = 0;
        for (int i = 0; i + 5 <= body.length(); i++) {
            char c = body.charAt(i);
            if (c == '(') {
                depth++;
                continue;
            }
            if (c == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth == 0 && body.regionMatches(true, i, " with ", 0, 6)) {
                return body.substring(i + 6).trim();
            }
        }
        return null;
    }

    private static int withSuffixLength(String body) {
        int depth = 0;
        for (int i = 0; i + 5 <= body.length(); i++) {
            char c = body.charAt(i);
            if (c == '(') {
                depth++;
                continue;
            }
            if (c == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth == 0 && body.regionMatches(true, i, " with ", 0, 6)) {
                return body.length() - i;
            }
        }
        return 0;
    }
}
