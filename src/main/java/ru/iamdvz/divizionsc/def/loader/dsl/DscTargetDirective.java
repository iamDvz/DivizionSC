package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Разбирает маршрут {@code >> start >> end} у строк эффектов. */
public final class DscTargetDirective {

    private static final String TOKEN = "[\\p{L}\\p{N}_-]+";

    private static final Pattern DUAL_BLOCK = Pattern.compile(
            "^(.*?)\\s>>\\s+(" + TOKEN + ")\\s>>\\s+(" + TOKEN + ")\\s*\\{\\s*$"
    );
    private static final Pattern DUAL_LINE = Pattern.compile(
            "^(.*?)\\s>>\\s+(" + TOKEN + ")\\s>>\\s+(" + TOKEN + ")\\s*$"
    );
    /** {@code >> target >>} — указана только цель, не начало. */
    private static final Pattern END_ONLY_TRAILING = Pattern.compile(
            "^(.*?)\\s>>\\s+(" + TOKEN + ")\\s>>\\s*(\\{\\s*)?$"
    );
    /** {@code >> >> target} — указана только цель. */
    private static final Pattern END_ONLY_LEADING = Pattern.compile(
            "^(.*?)\\s>>\\s+\\s>>\\s+(" + TOKEN + ")\\s*(\\{\\s*)?$"
    );
    private static final Pattern SINGLE_BLOCK = Pattern.compile(
            "^(.*?)\\s>>\\s+(" + TOKEN + ")\\s*\\{\\s*$"
    );
    private static final Pattern SINGLE_LINE = Pattern.compile(
            "^(.*?)\\s>>\\s+(" + TOKEN + ")\\s*$"
    );

    public record Route(String from, String to) {
        public static final Route NONE = new Route(null, null);

        public boolean isEmpty() {
            return from == null && to == null;
        }

        /** Явно задана отправная точка (первый токен в {@code >> a >> b}). */
        public boolean hasExplicitFrom() {
            return from != null;
        }

        public Route mergeInherited(Route inherited) {
            if (inherited == null || inherited.isEmpty()) {
                return this;
            }
            return new Route(
                    from != null ? from : inherited.from,
                    to != null ? to : inherited.to
            );
        }

        /** Эффективный маршрут: если указана только цель — начало {@code self} по умолчанию. */
        public Route resolved() {
            if (to == null) {
                return this;
            }
            return new Route(from != null ? from : "self", to);
        }
    }

    public record Split(String body, Route route) {
    }

    private DscTargetDirective() {
    }

    public static Split splitBlock(String line) {
        String trimmed = line.trim();
        if (!trimmed.endsWith("{")) {
            return split(line);
        }
        Route route = parseRoute(trimmed);
        if (route != null) {
            return new Split(stripBlockBody(trimmed, route), route);
        }
        return new Split(trimmed.substring(0, trimmed.length() - 1).trim(), Route.NONE);
    }

    public static Split split(String line) {
        String trimmed = line.trim();
        Route route = parseRoute(trimmed);
        if (route != null) {
            return new Split(stripLineBody(trimmed, route), route);
        }
        return new Split(trimmed, Route.NONE);
    }

    private static Route parseRoute(String trimmed) {
        Matcher dual = DUAL_BLOCK.matcher(trimmed);
        if (dual.matches()) {
            return route(dual.group(2), dual.group(3));
        }
        dual = DUAL_LINE.matcher(trimmed);
        if (dual.matches()) {
            return route(dual.group(2), dual.group(3));
        }

        Matcher endTrailing = END_ONLY_TRAILING.matcher(trimmed);
        if (endTrailing.matches()) {
            return endOnly(endTrailing.group(2));
        }
        Matcher endLeading = END_ONLY_LEADING.matcher(trimmed);
        if (endLeading.matches()) {
            return endOnly(endLeading.group(2));
        }

        Matcher singleBlock = SINGLE_BLOCK.matcher(trimmed);
        if (singleBlock.matches()) {
            return endOnly(singleBlock.group(2));
        }
        Matcher singleLine = SINGLE_LINE.matcher(trimmed);
        if (singleLine.matches()) {
            return endOnly(singleLine.group(2));
        }
        return null;
    }

    private static String stripBlockBody(String trimmed, Route route) {
        String withoutBrace = trimmed.endsWith("{")
                ? trimmed.substring(0, trimmed.length() - 1).trim()
                : trimmed;
        return stripLineBody(withoutBrace, route);
    }

    private static String stripLineBody(String trimmed, Route route) {
        int first = trimmed.indexOf(">>");
        if (first < 0) {
            return trimmed;
        }
        return trimmed.substring(0, first).trim();
    }

    private static Route route(String fromRaw, String toRaw) {
        return new Route(normalizeFrom(fromRaw), normalizeTo(toRaw));
    }

    /** Один токен маршрута — всегда цель, не начало. */
    private static Route endOnly(String toRaw) {
        return new Route(null, normalizeTo(toRaw));
    }

    /** Начало / отправная точка. */
    public static String normalizeFrom(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "start", "начало", "отправная", "отправная_точка", "отпр", "caster", "self" -> "self";
            case "eyes", "eye", "глаза" -> "eyes";
            case "target", "entity", "ent", "цель", "таргет" -> "target";
            case "location", "block", "loc" -> "location";
            default -> raw.toLowerCase(Locale.ROOT);
        };
    }

    /** Конец / цель. */
    public static String normalizeTo(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "end", "конец", "target", "entity", "ent", "цель", "таргет", "tgt" -> "target";
            case "start", "начало", "отправная", "отправная_точка", "отпр", "caster", "self" -> "self";
            case "location", "block", "loc" -> "location";
            case "eyes", "eye", "глаза" -> "eyes";
            default -> raw.toLowerCase(Locale.ROOT);
        };
    }

    /** @deprecated используйте {@link #normalizeTo} */
    @Deprecated
    public static String normalize(String raw) {
        return normalizeTo(raw);
    }
}
