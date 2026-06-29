package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.Locale;

/** Параметр module-блока: {@code dmg} или {@code dmg=3}. */
public record DscModuleParam(String name, String defaultValue) {

    public DscModuleParam {
        name = name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    public DscModuleParam(String name) {
        this(name, null);
    }

    public static DscModuleParam parse(String raw) {
        String trimmed = raw.trim();
        int equals = trimmed.indexOf('=');
        if (equals > 0) {
            return new DscModuleParam(
                    trimmed.substring(0, equals).trim(),
                    trimmed.substring(equals + 1).trim()
            );
        }
        return new DscModuleParam(trimmed, null);
    }

    public boolean hasDefault() {
        return defaultValue != null && !defaultValue.isBlank();
    }
}
