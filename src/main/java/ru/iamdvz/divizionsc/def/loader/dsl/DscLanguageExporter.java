package ru.iamdvz.divizionsc.def.loader.dsl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Экспорт каталога языка .dsc в JSON для VS Code extension.
 * Запуск: {@code ./gradlew exportDscLanguage}
 */
public final class DscLanguageExporter {

    private DscLanguageExporter() {
    }

    public static void main(String[] args) throws IOException {
        String version = args.length > 1 ? args[1] : "dev";
        Path output = Path.of(args.length > 0 ? args[0] : "../DivizionSC-vscode/generated/dsc-language.json");
        export(version, output);
        System.out.println("DSC language exported to " + output.toAbsolutePath());
    }

    public static void export(String pluginVersion, Path output) throws IOException {
        Map<String, Object> catalog = DscLanguageCatalog.toMap(pluginVersion);
        String json = toJson(catalog, 0);
        Files.createDirectories(output.getParent());
        Files.writeString(output, json + "\n", StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Object value, int indent) {
        String pad = "  ".repeat(indent);
        String padInner = "  ".repeat(indent + 1);
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return "\"" + escape(text) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                return "{}";
            }
            StringBuilder out = new StringBuilder("{\n");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(",\n");
                }
                first = false;
                out.append(padInner)
                        .append("\"")
                        .append(escape(String.valueOf(entry.getKey())))
                        .append("\": ")
                        .append(toJson(entry.getValue(), indent + 1));
            }
            out.append('\n').append(pad).append('}');
            return out.toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder out = new StringBuilder("[\n");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    out.append(",\n");
                }
                first = false;
                out.append(padInner).append(toJson(item, indent + 1));
            }
            out.append('\n').append(pad).append(']');
            return out.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private static String escape(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
