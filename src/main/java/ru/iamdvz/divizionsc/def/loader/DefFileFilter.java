package ru.iamdvz.divizionsc.def.loader;

import java.util.Locale;
import java.util.regex.Pattern;

public final class DefFileFilter {

    private static final Pattern DEFS_PATTERN = Pattern.compile("defs-.*\\.dsc", Pattern.CASE_INSENSITIVE);

    private DefFileFilter() {
    }

    public static boolean accepts(String fileName) {
        if (fileName == null) {
            return false;
        }
        return DEFS_PATTERN.matcher(fileName.toLowerCase(Locale.ROOT)).matches();
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
        return accepts(name);
    }
}
