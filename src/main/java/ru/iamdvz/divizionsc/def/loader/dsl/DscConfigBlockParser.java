package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DscConfigBlockParser {

    private static final Pattern BLOCK_START = Pattern.compile("^([a-zA-Z][a-zA-Z0-9_-]*)\\s*\\{\\s*$");
    private static final Pattern PROPERTY = Pattern.compile("^([a-zA-Z][a-zA-Z0-9_-]*)\\s*[:=]\\s*(.+)$");
    private static final Pattern PROPERTY_PLAIN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9_-]*)\\s+(.+)$");

    Map<String, Object> parse(List<DscParser.Line> lines, int blockOpenIndex) {
        Map<String, Object> data = new LinkedHashMap<>();
        int cursor = blockOpenIndex + 1;
        int depth = 1;

        while (cursor < lines.size() && depth > 0) {
            DscParser.Line current = lines.get(cursor);
            if ("}".equals(current.text())) {
                depth--;
                if (depth == 0) {
                    break;
                }
                cursor++;
                continue;
            }

            Matcher nested = BLOCK_START.matcher(current.text());
            if (nested.matches() && depth == 1) {
                String blockName = nested.group(1).toLowerCase(Locale.ROOT);
                ParseResult nestedResult = parseNestedBlock(lines, cursor, blockName);
                mergeSubBlock(data, blockName, nestedResult.data());
                cursor = nestedResult.nextIndex();
                continue;
            }

            String property = parseProperty(current.text());
            if (property != null) {
                int split = property.indexOf('\0');
                String key = property.substring(0, split);
                String value = property.substring(split + 1);
                putProperty(data, key, value);
                cursor++;
                continue;
            }

            throw new DscParseException("Invalid line in config block: " + current.text(), current.number());
        }

        normalizeAliases(data);
        return data;
    }

    int endIndex(List<DscParser.Line> lines, int blockOpenIndex) {
        int cursor = blockOpenIndex + 1;
        int depth = 1;
        while (cursor < lines.size() && depth > 0) {
            if ("}".equals(lines.get(cursor).text())) {
                depth--;
                if (depth == 0) {
                    return cursor + 1;
                }
            } else if (BLOCK_START.matcher(lines.get(cursor).text()).matches()) {
                depth++;
            }
            cursor++;
        }
        throw new DscParseException("Unclosed config block", lines.get(blockOpenIndex).number());
    }

    private ParseResult parseNestedBlock(List<DscParser.Line> lines, int blockOpenIndex, String blockName) {
        Map<String, Object> data = new LinkedHashMap<>();
        int cursor = blockOpenIndex + 1;
        int depth = 1;

        while (cursor < lines.size() && depth > 0) {
            DscParser.Line current = lines.get(cursor);
            if ("}".equals(current.text())) {
                depth--;
                if (depth == 0) {
                    cursor++;
                    break;
                }
                cursor++;
                continue;
            }

            Matcher nested = BLOCK_START.matcher(current.text());
            if (nested.matches()) {
                String childName = nested.group(1).toLowerCase(Locale.ROOT);
                ParseResult child = parseNestedBlock(lines, cursor, childName);
                data.put(childName, child.data());
                cursor = child.nextIndex();
                continue;
            }

            String property = parseProperty(current.text());
            if (property != null) {
                int split = property.indexOf('\0');
                String key = property.substring(0, split);
                String value = property.substring(split + 1);
                putProperty(data, key, value);
                cursor++;
                continue;
            }

            throw new DscParseException("Invalid line in nested block '" + blockName + "': " + current.text(), current.number());
        }

        return new ParseResult(data, cursor);
    }

    private void mergeSubBlock(Map<String, Object> data, String name, Map<String, Object> block) {
        switch (name) {
            case "animations" -> data.put("animations", namedEntriesToList(block));
            case "change-parts", "change_parts" -> data.put("change-parts", namedEntriesToList(block));
            case "bones" -> data.put("bones", block);
            default -> data.put(name, block);
        }
    }

    private List<Map<String, Object>> namedEntriesToList(Map<String, Object> entries) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            if (entry.getValue() instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> props = (Map<String, Object>) map;
                item.putAll(props);
            }
            item.putIfAbsent("name", entry.getKey());
            list.add(item);
        }
        return list;
    }

    private void putProperty(Map<String, Object> data, String key, String rawValue) {
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        if ("parts".equals(normalizedKey) || "new-parts".equals(normalizedKey) || "new_parts".equals(normalizedKey)) {
            data.put(normalizedKey.replace('_', '-'), splitList(rawValue));
            return;
        }
        data.put(normalizedKey, coerceValue(rawValue));
    }

    private void normalizeAliases(Map<String, Object> data) {
        if (!data.containsKey("model") && data.containsKey("model-name")) {
            data.put("model", data.get("model-name"));
        }
        if (!data.containsKey("at") && data.containsKey("position")) {
            data.put("at", data.get("position"));
        }
        if (!data.containsKey("location") && data.containsKey("position")) {
            data.put("location", data.get("position"));
        }
    }

    private List<String> splitList(String raw) {
        if (raw.startsWith("[") && raw.endsWith("]")) {
            raw = raw.substring(1, raw.length() - 1);
        }
        List<String> items = new ArrayList<>();
        for (String part : raw.split("[,\\s]+")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }

    private Object coerceValue(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if ("true".equals(lower) || "false".equals(lower)) {
            return Boolean.parseBoolean(lower);
        }
        try {
            if (raw.contains(".")) {
                return Double.parseDouble(raw);
            }
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    private String parseProperty(String text) {
        Matcher explicit = PROPERTY.matcher(text);
        if (explicit.matches()) {
            return explicit.group(1).toLowerCase(Locale.ROOT) + '\0' + unquote(explicit.group(2).trim());
        }
        Matcher plain = PROPERTY_PLAIN.matcher(text);
        if (plain.matches()) {
            return plain.group(1).toLowerCase(Locale.ROOT) + '\0' + unquote(plain.group(2).trim());
        }
        return null;
    }

    private String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private record ParseResult(Map<String, Object> data, int nextIndex) {
    }
}
