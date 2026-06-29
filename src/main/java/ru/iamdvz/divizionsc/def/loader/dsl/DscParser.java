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

public final class DscParser {

    private static final Pattern TOP_LEVEL = Pattern.compile(
            "^(ability|passive|module|def|effect)\\s+([a-zA-Z][a-zA-Z0-9_]*)\\s*(\\(([a-zA-Z0-9_,\\s]*)\\))?\\s*\\{\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SECTION = Pattern.compile(
            "^(" + DscSyntax.sectionPatternSource().replace(" ", "\\s+") + ")\\s*\\{\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PROP_COLON_START = Pattern.compile(
            "^[a-zA-Z][a-zA-Z0-9_-]*\\s*:.*"
    );
    private static final Pattern CALL_BLOCK = Pattern.compile(
            "^([a-zA-Z][a-zA-Z0-9_]*)\\s*\\((.*)\\)\\s*\\{\\s*$"
    );
    private static final Pattern IF_BLOCK = Pattern.compile(
            "^(if|when)\\s*\\((.+?)\\)\\s*\\{\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ELSE_IF_BODY = Pattern.compile(
            "^else\\s+if\\s*\\((.+?)\\)\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ELSE_BLOCK = Pattern.compile(
            "^else\\s*\\{\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CHANCE_BLOCK = Pattern.compile(
            "^chance\\s*\\(([^)]+)\\)\\s*\\{\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern VFX_HEAD = Pattern.compile(
            "^(vfx|meg|modelengine|model)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FX_HEAD = Pattern.compile(
            "^(fx|effectlib)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NESTED_SECTION = Pattern.compile(
            "^(hit|tick|on_hit|on_tick)\\s*\\{\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private final DscCallParser callParser = new DscCallParser();
    private final DscEffectDesugar desugar = new DscEffectDesugar();

    public DscScript parse(String source, String sourceName) {
        List<Line> lines = expandInlineSections(preprocess(source));
        List<DscBlock> blocks = new ArrayList<>();
        int index = 0;
        while (index < lines.size()) {
            index = skipBlank(lines, index);
            if (index >= lines.size()) {
                break;
            }
            ParseBlockResult result = parseBlock(lines, index);
            blocks.add(result.block());
            index = result.nextIndex();
        }
        return new DscScript(sourceName, blocks);
    }

    private ParseBlockResult parseBlock(List<Line> lines, int index) {
        Line line = lines.get(index);
        Matcher top = TOP_LEVEL.matcher(line.text());
        if (!top.matches()) {
            throw new DscParseException(
                    "Expected ability, passive, module, def, or effect block: " + line.text(),
                    line.number()
            );
        }

        String blockKeyword = DscSyntax.normalizeBlockKeyword(top.group(1));
        DscBlockKind kind = DscSyntax.blockKind(blockKeyword);
        String id = top.group(2).toLowerCase(Locale.ROOT);
        List<String> params = parseParams(top.group(4));

        Map<String, String> properties = new LinkedHashMap<>();
        Map<String, DscSection> sections = new LinkedHashMap<>();
        int cursor = index + 1;
        int depth = 1;

        while (cursor < lines.size() && depth > 0) {
            Line current = lines.get(cursor);
            if ("}".equals(current.text())) {
                depth--;
                if (depth == 0) {
                    cursor++;
                    break;
                }
                cursor++;
                continue;
            }

            Matcher section = SECTION.matcher(current.text());
            if (section.matches() && depth == 1) {
                String sectionName = DscSyntax.normalizeSection(section.group(1));
                if ("meta".equals(sectionName)) {
                    ParseMetaResult metaResult = parseMetaSection(lines, cursor + 1);
                    properties.putAll(metaResult.properties());
                    cursor = metaResult.nextIndex();
                    continue;
                }
                ParseSectionResult sectionResult = parseSection(lines, cursor + 1);
                sections.put(sectionName, sectionResult.section());
                cursor = sectionResult.nextIndex();
                continue;
            }

            if (depth == 1) {
                List<String[]> inlineProps = parseBlockProperties(current.text());
                if (inlineProps != null) {
                    for (String[] kv : inlineProps) {
                        properties.put(kv[0], kv[1]);
                    }
                    cursor++;
                    continue;
                }
            }

            throw new DscParseException("Unexpected line inside block '" + id + "': " + current.text(), current.number());
        }

        if (depth != 0) {
            throw new DscParseException("Unclosed block '" + id + "'", line.number());
        }

        if (kind == DscBlockKind.PASSIVE) {
            properties.putIfAbsent("passive", "true");
        }

        return new ParseBlockResult(new DscBlock(kind, id, params, properties, sections), cursor);
    }

    private ParseMetaResult parseMetaSection(List<Line> lines, int index) {
        Map<String, String> properties = new LinkedHashMap<>();
        int depth = 1;
        int cursor = index;

        while (cursor < lines.size() && depth > 0) {
            Line current = lines.get(cursor);
            if ("}".equals(current.text())) {
                depth--;
                if (depth == 0) {
                    cursor++;
                    break;
                }
                cursor++;
                continue;
            }

            List<String[]> metaProps = splitColonProperties(current.text());
            if (metaProps == null) {
                throw new DscParseException("meta expects key: value — got: " + current.text(), current.number());
            }
            for (String[] kv : metaProps) {
                properties.put(kv[0], kv[1]);
            }
            cursor++;
        }

        if (depth != 0) {
            throw new DscParseException("Unclosed meta section", lines.get(index).number());
        }

        return new ParseMetaResult(properties, cursor);
    }

    private ParseSectionResult parseSection(List<Line> lines, int index) {
        List<DscStatement> statements = new ArrayList<>();
        int depth = 1;
        int cursor = index;

        while (cursor < lines.size() && depth > 0) {
            Line current = lines.get(cursor);
            if ("}".equals(current.text())) {
                depth--;
                if (depth == 0) {
                    cursor++;
                    break;
                }
                cursor++;
                continue;
            }

            int next = parseStatementInto(statements, lines, cursor);
            if (next <= cursor) {
                throw new DscParseException("Failed to parse statement: " + current.text(), current.number());
            }
            cursor = next;
        }

        if (depth != 0) {
            throw new DscParseException("Unclosed section block", lines.get(index).number());
        }

        return new ParseSectionResult(new DscSection("body", statements), cursor);
    }

    private int parseStatementInto(List<DscStatement> statements, List<Line> lines, int index) {
        Line line = lines.get(index);
        String raw = line.text().trim();
        boolean opensBlock = raw.endsWith("{");
        DscTargetDirective.Split split = opensBlock
                ? DscTargetDirective.splitBlock(raw)
                : DscTargetDirective.split(raw);
        String head = split.body();
        DscTargetDirective.Route lineRoute = split.route();

        Matcher vfxBlock = VFX_HEAD.matcher(head);
        if (opensBlock && vfxBlock.matches()) {
            DscConfigBlockParser configParser = new DscConfigBlockParser();
            Map<String, Object> config = configParser.parse(lines, index);
            statements.add(new DscStatement.VfxBlock(config, lineRoute));
            return configParser.endIndex(lines, index);
        }

        Matcher fxBlock = FX_HEAD.matcher(head);
        if (opensBlock && fxBlock.matches()) {
            DscConfigBlockParser configParser = new DscConfigBlockParser();
            Map<String, Object> config = configParser.parse(lines, index);
            statements.add(new DscStatement.FxBlock(config, lineRoute));
            return configParser.endIndex(lines, index);
        }

        Matcher ifBlock = IF_BLOCK.matcher(opensBlock ? head + " {" : head);
        if (opensBlock && ifBlock.matches()) {
            return parseIfChain(statements, lines, index, ifBlock.group(2).trim(), lineRoute);
        }

        Matcher chanceBlock = CHANCE_BLOCK.matcher(opensBlock ? head + " {" : head);
        if (opensBlock && chanceBlock.matches()) {
            ParseSectionResult nested = parseSection(lines, index + 1);
            statements.add(new DscStatement.ChanceBlock(chanceBlock.group(1).trim(), nested.section(), lineRoute));
            return nested.nextIndex();
        }

        Matcher callBlock = CALL_BLOCK.matcher(opensBlock ? head + " {" : head);
        if (opensBlock && callBlock.matches()) {
            String name = callBlock.group(1);
            if (name.startsWith("@")) {
                throw new DscParseException("@module cannot have a body: " + raw, line.number());
            }
            String argsRaw = callBlock.group(2);
            DscCallParser.ParsedCall call = new DscCallParser.ParsedCall(
                    name,
                    callParser.parseLine(name + "(" + argsRaw + ")", line.number()).args(),
                    line.number()
            );
            return parseCallBlock(statements, lines, index, call, lineRoute);
        }

        if (head.startsWith("@")) {
            statements.add(withRoute(parseAtModuleCall(head, line.number()), lineRoute));
            return index + 1;
        }

        if (callParser.isEffectLine(head)) {
            DscCallParser.ParsedCall call = callParser.parseFlexible(head, line.number());
            if ("use".equalsIgnoreCase(call.name()) || "call".equalsIgnoreCase(call.name())) {
                throw new DscParseException(
                        "Use @module(args) instead of " + call.name() + "(): " + raw,
                        line.number()
                );
            }
            statements.add(new DscStatement.EffectCall(call, lineRoute));
            return index + 1;
        }

        throw new DscParseException("Expected effect call or block: " + raw, line.number());
    }

    private DscStatement.ModuleCall withRoute(DscStatement.ModuleCall call, DscTargetDirective.Route route) {
        if (route == null || route.isEmpty()) {
            return call;
        }
        return new DscStatement.ModuleCall(call.moduleId(), call.positional(), call.named(), route);
    }

    private int parseCallBlock(
            List<DscStatement> statements,
            List<Line> lines,
            int index,
            DscCallParser.ParsedCall call,
            DscTargetDirective.Route lineRoute
    ) {
        String name = call.name().toLowerCase(Locale.ROOT);
        if ("use".equals(name) || "call".equals(name)) {
            throw new DscParseException(
                    "Use @module(args) instead of " + name + "(): " + call.name(),
                    call.lineNumber()
            );
        }

        if (isDelayCall(name)) {
            ParseSectionResult nested = parseSection(lines, index + 1);
            String duration = firstPosOrNamed(call, "duration", firstPosOrEmpty(call));
            statements.add(new DscStatement.WaitBlock(duration, nested.section(), lineRoute));
            return nested.nextIndex();
        }

        if ("projectile".equals(name) || "proj".equals(name)) {
            return parseProjectileBlock(statements, lines, index, call, lineRoute);
        }

        if ("if".equals(name) || "when".equals(name)) {
            String condition = firstPosOrEmpty(call);
            return parseIfChain(statements, lines, index, condition, lineRoute);
        }

        if ("chance".equals(name)) {
            ParseSectionResult nested = parseSection(lines, index + 1);
            statements.add(new DscStatement.ChanceBlock(firstPosOrEmpty(call), nested.section(), lineRoute));
            return nested.nextIndex();
        }

        if (isContainerEffect(name)) {
            ParseSectionResult nested = parseSection(lines, index + 1);
            statements.add(new DscStatement.EffectBlock(desugar.blockVerb(name), call, nested.section(), lineRoute));
            return nested.nextIndex();
        }

        throw new DscParseException("Effect call cannot have a body: " + call.name(), call.lineNumber());
    }

    private int parseProjectileBlock(
            List<DscStatement> statements,
            List<Line> lines,
            int index,
            DscCallParser.ParsedCall call,
            DscTargetDirective.Route lineRoute
    ) {
        String projectile = firstPosOrNamed(call, "type", "FIREBALL");
        double speed = parseDouble(firstPosOrNamed(call, "speed", "1.2"), 1.2);
        DscSection tickBody = null;
        DscSection hitBody = null;
        int cursor = index + 1;
        int depth = 1;

        while (cursor < lines.size() && depth > 0) {
            Line current = lines.get(cursor);
            if ("}".equals(current.text())) {
                depth--;
                if (depth == 0) {
                    cursor++;
                    break;
                }
                cursor++;
                continue;
            }

            Matcher nested = NESTED_SECTION.matcher(current.text());
            if (nested.matches() && depth == 1) {
                String section = nested.group(1).toLowerCase(Locale.ROOT);
                ParseSectionResult sectionResult = parseSection(lines, cursor + 1);
                if ("hit".equals(section) || "on_hit".equals(section)) {
                    hitBody = sectionResult.section();
                } else {
                    tickBody = sectionResult.section();
                }
                cursor = sectionResult.nextIndex();
                continue;
            }

            throw new DscParseException("projectile body supports hit { } and tick { } only: " + current.text(), current.number());
        }

        if (depth != 0) {
            throw new DscParseException("Unclosed projectile block", lines.get(index).number());
        }

        statements.add(new DscStatement.ProjBlock(
                projectile.toUpperCase(Locale.ROOT),
                speed,
                tickBody,
                hitBody,
                lineRoute
        ));
        return cursor;
    }

    private int parseIfChain(
            List<DscStatement> statements,
            List<Line> lines,
            int index,
            String condition,
            DscTargetDirective.Route lineRoute
    ) {
        ParseSectionResult thenResult = parseSection(lines, index + 1);
        int next = thenResult.nextIndex();
        List<DscStatement.ElseIfBranch> elseIfs = new ArrayList<>();
        DscSection elseSection = null;

        while (true) {
            int peek = skipBlank(lines, next);
            if (peek >= lines.size()) {
                break;
            }
            Line peekLine = lines.get(peek);
            String peekRaw = peekLine.text().trim();
            if (!peekRaw.startsWith("else")) {
                break;
            }
            DscTargetDirective.Split elseSplit = peekRaw.endsWith("{")
                    ? DscTargetDirective.splitBlock(peekRaw)
                    : DscTargetDirective.split(peekRaw);
            Matcher elseIf = ELSE_IF_BODY.matcher(elseSplit.body());
            if (elseIf.matches()) {
                ParseSectionResult branch = parseSection(lines, peek + 1);
                elseIfs.add(new DscStatement.ElseIfBranch(elseIf.group(1).trim(), branch.section()));
                next = branch.nextIndex();
                continue;
            }
            if (ELSE_BLOCK.matcher(peekRaw).matches() || ELSE_BLOCK.matcher(elseSplit.body() + " {").matches()) {
                ParseSectionResult elseResult = parseSection(lines, peek + 1);
                elseSection = elseResult.section();
                next = elseResult.nextIndex();
                break;
            }
            break;
        }

        statements.add(new DscStatement.IfBlock(condition, thenResult.section(), elseIfs, elseSection, lineRoute));
        return next;
    }

    private DscStatement.ModuleCall parseAtModuleCall(String body, int lineNumber) {
        ChainEntry entry = ChainEntryParser.parseString(body.trim());
        if (entry.defId().isBlank()) {
            throw new DscParseException("@module requires module id", lineNumber);
        }
        Map<String, String> named = new LinkedHashMap<>();
        for (Map.Entry<String, Object> arg : entry.args().entrySet()) {
            named.put(arg.getKey().toLowerCase(Locale.ROOT), String.valueOf(arg.getValue()));
        }
        return new DscStatement.ModuleCall(entry.defId(), List.of(), named);
    }

    private boolean isDelayCall(String name) {
        return "after".equals(name) || "delay".equals(name) || "wait".equals(name);
    }

    private boolean isContainerEffect(String name) {
        return switch (name) {
            case "area", "loop", "aura", "repeat", "raycast", "beam", "chain" -> true;
            default -> false;
        };
    }

    private String firstPosOrNamed(DscCallParser.ParsedCall call, String key, String fallback) {
        for (DscCallParser.ParsedArg arg : call.args()) {
            if (arg.named() && key.equalsIgnoreCase(arg.name())) {
                return arg.value();
            }
        }
        for (DscCallParser.ParsedArg arg : call.args()) {
            if (!arg.named()) {
                return arg.value();
            }
        }
        return fallback;
    }

    private String firstPosOrEmpty(DscCallParser.ParsedCall call) {
        for (DscCallParser.ParsedArg arg : call.args()) {
            if (!arg.named()) {
                return arg.value();
            }
        }
        return "";
    }

    private List<String> parseParams(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> params = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                params.add(trimmed);
            }
        }
        return params;
    }

    private List<Line> expandInlineSections(List<Line> input) {
        List<Line> out = new ArrayList<>();
        Pattern singleLineSection = Pattern.compile(
                "^(" + DscSyntax.sectionPatternSource().replace(" ", "\\s+") + ")\\s*\\{\\s*(.+?)\\s*\\}\\s*$",
                Pattern.CASE_INSENSITIVE
        );
        for (Line line : input) {
            Matcher matcher = singleLineSection.matcher(line.text());
            if (matcher.matches()) {
                String name = DscSyntax.normalizeSection(matcher.group(1));
                String body = matcher.group(2).trim();
                out.add(new Line(line.number(), name + " {"));
                if (!body.isEmpty()) {
                    out.add(new Line(line.number(), body));
                }
                out.add(new Line(line.number(), "}"));
                continue;
            }
            out.add(line);
        }
        return out;
    }

    private List<Line> preprocess(String source) {
        List<Line> lines = new ArrayList<>();
        String[] rawLines = source.split("\\R");
        for (int i = 0; i < rawLines.length; i++) {
            String trimmed = normalizeRouteSyntax(stripComment(rawLines[i])).trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            while (trimmed.length() > 1 && trimmed.charAt(0) == '}') {
                lines.add(new Line(i + 1, "}"));
                trimmed = trimmed.substring(1).trim();
            }
            if (!trimmed.isEmpty()) {
                lines.add(new Line(i + 1, trimmed));
            }
        }
        return lines;
    }

    private String normalizeRouteSyntax(String line) {
        return line.replaceAll("\\s+->\\s+", " >> ").replaceAll("\\s+→\\s+", " >> ");
    }

    private String stripComment(String line) {
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (!inQuote && c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                return line.substring(0, i);
            }
            if (!inQuote && c == '#') {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Свойства блока: {@code cd: 8} (двоеточие, можно несколько на строке) либо
     * {@code cd 8} (короткая форма, одно на строку — README-стиль). {@code null} — строка не свойство.
     */
    private List<String[]> parseBlockProperties(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Ключевые слова секций (do/cast/hit/...) без `{` — это не свойство, а ошибочная секция.
        int leadEnd = 0;
        while (leadEnd < trimmed.length() && (Character.isLetterOrDigit(trimmed.charAt(leadEnd))
                || trimmed.charAt(leadEnd) == '_' || trimmed.charAt(leadEnd) == '-')) {
            leadEnd++;
        }
        if (leadEnd > 0 && DscSyntax.isSectionHeader(trimmed.substring(0, leadEnd))) {
            return null;
        }
        if (PROP_COLON_START.matcher(trimmed).matches()) {
            return splitColonProperties(trimmed);
        }
        int space = trimmed.indexOf(' ');
        if (space <= 0) {
            return null;
        }
        String key = trimmed.substring(0, space);
        if (!key.matches("[a-zA-Z][a-zA-Z0-9_-]*")) {
            return null;
        }
        return List.<String[]>of(new String[]{key.toLowerCase(Locale.ROOT), unquote(trimmed.substring(space + 1).trim())});
    }

    /**
     * Делит строку на пары {@code key: value}, уважая кавычки. Граница новой пары — пробел перед
     * {@code ident:} вне кавычек, когда текущий сегмент уже содержит своё двоеточие. {@code null},
     * если ни одного валидного {@code key: value} не найдено.
     */
    private List<String[]> splitColonProperties(String line) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                current.append(c);
                i++;
                continue;
            }
            if (!inQuote && Character.isWhitespace(c) && current.indexOf(":") >= 0) {
                int j = i;
                while (j < line.length() && Character.isWhitespace(line.charAt(j))) {
                    j++;
                }
                int k = j;
                while (k < line.length() && (Character.isLetterOrDigit(line.charAt(k))
                        || line.charAt(k) == '_' || line.charAt(k) == '-')) {
                    k++;
                }
                if (k > j && k < line.length() && line.charAt(k) == ':') {
                    segments.add(current.toString().trim());
                    current.setLength(0);
                    i = j;
                    continue;
                }
            }
            current.append(c);
            i++;
        }
        if (current.length() > 0) {
            segments.add(current.toString().trim());
        }

        List<String[]> properties = new ArrayList<>();
        for (String segment : segments) {
            int colon = segment.indexOf(':');
            if (colon <= 0) {
                return null;
            }
            properties.add(new String[]{
                    segment.substring(0, colon).trim().toLowerCase(Locale.ROOT),
                    unquote(segment.substring(colon + 1).trim())
            });
        }
        return properties.isEmpty() ? null : properties;
    }

    private int skipBlank(List<Line> lines, int index) {
        while (index < lines.size() && lines.get(index).text().isBlank()) {
            index++;
        }
        return index;
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

    record Line(int number, String text) {
    }

    private record ParseBlockResult(DscBlock block, int nextIndex) {
    }

    private record ParseSectionResult(DscSection section, int nextIndex) {
    }

    private record ParseMetaResult(Map<String, String> properties, int nextIndex) {
    }
}
