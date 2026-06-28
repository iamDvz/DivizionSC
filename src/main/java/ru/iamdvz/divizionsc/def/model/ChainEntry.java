package ru.iamdvz.divizionsc.def.model;

import java.util.Map;

public record ChainEntry(
        String defId,
        Map<String, Object> args
) {
    public ChainEntry {
        args = args == null ? Map.of() : Map.copyOf(args);
    }
}
