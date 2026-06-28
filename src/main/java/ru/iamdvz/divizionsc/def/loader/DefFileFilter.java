package ru.iamdvz.divizionsc.def.loader;

import java.util.Locale;
import java.util.regex.Pattern;

public final class DefFileFilter {

    private static final Pattern DEFS_PATTERN = Pattern.compile("defs-.*\\.(yml|yaml|dsc)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY = Pattern.compile("examples\\.ya?ml", Pattern.CASE_INSENSITIVE);

    private DefFileFilter() {
    }

    public static boolean accepts(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return DEFS_PATTERN.matcher(lower).matches()
                || LEGACY.matcher(lower).matches()
                || "advanced.yml".equalsIgnoreCase(lower);
    }

    public static boolean acceptsJarEntry(String entryName) {
        if (entryName == null || entryName.contains("..")) {
            return false;
        }
        if (!entryName.startsWith("defs/")) {
            return false;
        }
        int slash = entryName.lastIndexOf('/');
        String name = slash >= 0 ? entryName.substring(slash + 1) : entryName;
        return accepts(name) || "advanced.yml".equalsIgnoreCase(name);
    }
}
