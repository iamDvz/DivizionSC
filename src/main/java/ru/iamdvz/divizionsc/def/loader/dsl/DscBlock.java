package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.List;
import java.util.Map;

public record DscBlock(
        DscBlockKind kind,
        String id,
        List<String> params,
        Map<String, String> properties,
        Map<String, DscSection> sections
) {
    public boolean helper() {
        return kind == DscBlockKind.MODULE;
    }
}
