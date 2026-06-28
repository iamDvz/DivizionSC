package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DscParser {

    private static final Pattern TOP_LEVEL = Pattern.compile(
            "^(def|module|effect)\\s+([a-zA-Z][a-zA-Z0-9_]*)\\s*(\\(([a-zA-Z0-9_,\\s]*)\\))?\\s*\\{\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SECTION = Pattern.compile(
            "^(do|on\\s+(hit|cast|done|complete|end))\\s*\\{\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WAIT_SECTION = Pattern.compile(
            "^(wait|delay)\\s+(.+?)\\s*\\{\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PROPERTY = Pattern.compile(
            "^([a-zA-Z][a-zA-Z0-9_-]*)\\s*[:=]\\s*(.+)$"
    );
    private static final Pattern PROPERTY_PLAIN = Pattern.compile(
            "^([a-zA-Z][a-zA-Z0-9_-]*)\\s+(.+)$"
    );
    private static final Pattern MODULE_CALL = Pattern.compile(
            "^@([a-zA-Z][a-zA-Z0-9_]*)\\s*(\\((.*)\\))?\\s*$"
    );
    private static final Pattern USE_CALL = Pattern.compile(
            "^(?:use|call)\\s+([a-zA-Z][a-zA-Z0-9_]*)\\s*(\\((.*)\\))?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    public DscScript parse(String source, String sourceName) {
        List<Line> lines = preprocess(source);
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
            throw new DscParseException("Expected def/module/effect block", line.number());
        }

        DscBlockKind kind = switch (top.group(1).toLowerCase(Locale.ROOT)) {
            case "module" -> DscBlockKind.MODULE;
            case "effect" -> DscBlockKind.EFFECT;
            default -> DscBlockKind.DEF;
        };
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
                String sectionName = normalizeSection(section.group(1), section.group(2));
                ParseSectionResult sectionResult = parseSection(lines, cursor + 1);
                sections.put(sectionName, sectionResult.section());
                cursor = sectionResult.nextIndex();
                continue;
            }

            Matcher wait = WAIT_SECTION.matcher(current.text());
            if (wait.matches() && depth == 1) {
                throw new DscParseException("wait { } blocks must be inside do/on sections", current.number());
            }

            if (depth == 1) {
                String property = parseProperty(current.text());
                if (property != null) {
                    int split = property.indexOf('\0');
                    properties.put(property.substring(0, split), property.substring(split + 1));
                    cursor++;
                    continue;
                }
            }

            throw new DscParseException("Unexpected line inside block '" + id + "': " + current.text(), current.number());
        }

        if (depth != 0) {
            throw new DscParseException("Unclosed block '" + id + "'", line.number());
        }

        return new ParseBlockResult(
                new DscBlock(kind, id, params, properties, sections),
                cursor
        );
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

            Matcher wait = WAIT_SECTION.matcher(current.text());
            if (wait.matches()) {
                ParseSectionResult nested = parseSection(lines, cursor + 1);
                statements.add(new DscStatement.WaitBlock(wait.group(2).trim(), nested.section()));
                cursor = nested.nextIndex();
                continue;
            }

            DscStatement statement = parseStatement(current);
            statements.add(statement);
            cursor++;
        }

        if (depth != 0) {
            throw new DscParseException("Unclosed section block", lines.get(index).number());
        }

        return new ParseSectionResult(new DscSection("body", statements), cursor);
    }

    private DscStatement parseStatement(Line line) {
        Matcher module = MODULE_CALL.matcher(line.text());
        if (module.matches()) {
            return new DscStatement.ModuleCall(
                    module.group(1).toLowerCase(Locale.ROOT),
                    parseCallArgs(module.group(3))
            );
        }
        Matcher use = USE_CALL.matcher(line.text());
        if (use.matches()) {
            return new DscStatement.ModuleCall(
                    use.group(1).toLowerCase(Locale.ROOT),
                    parseCallArgs(use.group(3))
            );
        }
        return new DscStatement.EffectLine(line.text());
    }

    private List<String> parseCallArgs(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                current.append(c);
                continue;
            }
            if (c == ',' && !inQuote) {
                args.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            args.add(current.toString().trim());
        }
        return args;
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

    private String parseProperty(String text) {
        Matcher explicit = PROPERTY.matcher(text);
        if (explicit.matches()) {
            return explicit.group(1).toLowerCase(Locale.ROOT) + '\0' + unquote(explicit.group(2).trim());
        }
        Matcher plain = PROPERTY_PLAIN.matcher(text);
        if (plain.matches()) {
            String key = plain.group(1).toLowerCase(Locale.ROOT);
            if (isReservedKey(key)) {
                return null;
            }
            return key + '\0' + unquote(plain.group(2).trim());
        }
        return null;
    }

    private boolean isReservedKey(String key) {
        return "do".equals(key) || key.startsWith("on");
    }

    private String normalizeSection(String head, String sub) {
        if ("do".equalsIgnoreCase(head)) {
            return "do";
        }
        return switch (sub.toLowerCase(Locale.ROOT)) {
            case "hit" -> "hit";
            case "cast" -> "cast";
            default -> "done";
        };
    }

    private String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private List<Line> preprocess(String source) {
        List<Line> lines = new ArrayList<>();
        String[] rawLines = source.split("\\R");
        for (int i = 0; i < rawLines.length; i++) {
            String trimmed = stripComment(rawLines[i]).trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            lines.add(new Line(i + 1, trimmed));
        }
        return lines;
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

    private int skipBlank(List<Line> lines, int index) {
        while (index < lines.size() && lines.get(index).text().isBlank()) {
            index++;
        }
        return index;
    }

    private record Line(int number, String text) {
    }

    private record ParseBlockResult(DscBlock block, int nextIndex) {
    }

    private record ParseSectionResult(DscSection section, int nextIndex) {
    }
}
