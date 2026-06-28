package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.List;

public record DscScript(String sourceName, List<DscBlock> blocks) {
}
