package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.List;

public record DscSection(String name, List<DscStatement> statements) {
}
