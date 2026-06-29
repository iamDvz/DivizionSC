package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.Locale;
import java.util.List;
import java.util.Set;

/** Каноническая грамматика DSC v2 — нормализация алиасов. */
public final class DscSyntax {

    public static final String ROUTE = "effect >> target | effect >> self >> target | effect at target | effect to target";
    public static final String MODULE_CALL = "@id(args) >> route | @a, @b | @a + @b | @main with extra";
    public static final String STACK_BLOCK = "stack >> route { @a @b }";
    public static final String MODULE_EXTENDS = "module id extends parent | extends: parent";
    public static final String EFFECT_CALL = "verb(args) >> route";
    public static final String PROPERTY = "key: value";

    /** Ключевые слова блоков (канон + устаревшие алиасы). */
    public static List<String> blockKeywords() {
        return List.of("ability", "passive", "module");
    }

    /** Устаревшие алиасы блоков — парсятся, но не рекомендуются. */
    public static List<String> deprecatedBlockKeywords() {
        return List.of("def", "effect");
    }

    /** Канонические секции. */
    public static List<String> sectionKeywords() {
        return List.of("meta", "cast", "effects", "start", "hit", "done");
    }

    /** Алиасы секций. */
    public static List<String> sectionAliases() {
        return List.of("do", "on cast", "on hit", "on done", "complete", "end");
    }

    /** Маршрутные суффиксы (эквивалентны >>). */
    public static List<String> routeSuffixes() {
        return List.of(">>", "->", "at", "to");
    }

    private static final Set<String> BLOCK_KEYWORDS = Set.of(
            "ability", "passive", "module", "def", "effect"
    );
    private static final Set<String> SECTION_HEADERS = Set.of(
            "meta", "cast", "effects", "start", "hit", "done",
            "do", "on cast", "on hit", "on done", "complete", "end"
    );

    private DscSyntax() {
    }

    public static boolean isBlockKeyword(String word) {
        return BLOCK_KEYWORDS.contains(word.toLowerCase(Locale.ROOT));
    }

    public static String normalizeBlockKeyword(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "def" -> "ability";
            case "effect" -> "module";
            default -> raw.toLowerCase(Locale.ROOT);
        };
    }

    public static DscBlockKind blockKind(String normalizedKeyword) {
        return switch (normalizedKeyword) {
            case "passive" -> DscBlockKind.PASSIVE;
            case "module" -> DscBlockKind.MODULE;
            default -> DscBlockKind.ABILITY;
        };
    }

    /** Нормализует заголовок секции (без `{`). */
    public static String normalizeSection(String rawHeader) {
        String header = rawHeader.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        return switch (header) {
            case "do" -> "cast";
            case "on cast", "oncast" -> "start";
            case "on hit", "onhit" -> "hit";
            case "on done", "ondone", "on complete", "oncomplete", "complete", "end" -> "done";
            default -> header.replace(' ', '_');
        };
    }

    public static boolean isSectionHeader(String rawHeader) {
        String normalized = rawHeader.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        if (SECTION_HEADERS.contains(normalized)) {
            return true;
        }
        return SECTION_HEADERS.contains(normalizeSection(rawHeader));
    }

    public static String sectionPatternSource() {
        return "meta|cast|effects|start|hit|done|do|on cast|on hit|on done|complete|end";
    }
}
