package ru.iamdvz.divizionsc.def.loader.dsl;

public sealed interface DscStatement permits DscStatement.EffectLine, DscStatement.ModuleCall, DscStatement.WaitBlock {

    record EffectLine(String line) implements DscStatement {
    }

    record ModuleCall(String moduleId, java.util.List<String> args) implements DscStatement {
    }

    record WaitBlock(String duration, DscSection body) implements DscStatement {
    }
}
