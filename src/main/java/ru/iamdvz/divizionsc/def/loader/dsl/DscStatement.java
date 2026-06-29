package ru.iamdvz.divizionsc.def.loader.dsl;

public sealed interface DscStatement permits
        DscStatement.EffectCall,
        DscStatement.EffectLine,
        DscStatement.ModuleCall,
        DscStatement.WaitBlock,
        DscStatement.EffectBlock,
        DscStatement.VfxBlock,
        DscStatement.FxBlock,
        DscStatement.ProjBlock,
        DscStatement.IfBlock,
        DscStatement.ChanceBlock {

    record EffectCall(DscCallParser.ParsedCall call, DscTargetDirective.Route route) implements DscStatement {
        public EffectCall(DscCallParser.ParsedCall call) {
            this(call, DscTargetDirective.Route.NONE);
        }
    }

    record EffectLine(String line, DscTargetDirective.Route route) implements DscStatement {
        public EffectLine(String line) {
            this(line, DscTargetDirective.Route.NONE);
        }
    }

    record ModuleCall(
            String moduleId,
            java.util.List<String> positional,
            java.util.Map<String, String> named,
            DscTargetDirective.Route route
    ) implements DscStatement {
        public ModuleCall(String moduleId, java.util.List<String> args) {
            this(moduleId, args, java.util.Map.of(), DscTargetDirective.Route.NONE);
        }

        public ModuleCall(String moduleId, java.util.List<String> positional, java.util.Map<String, String> named) {
            this(moduleId, positional, named, DscTargetDirective.Route.NONE);
        }
    }

    record WaitBlock(String duration, DscSection body, DscTargetDirective.Route route) implements DscStatement {
        public WaitBlock(String duration, DscSection body) {
            this(duration, body, DscTargetDirective.Route.NONE);
        }
    }

    record EffectBlock(
            String verb,
            DscCallParser.ParsedCall call,
            DscSection body,
            DscTargetDirective.Route route
    ) implements DscStatement {
        public EffectBlock(String verb, DscCallParser.ParsedCall call, DscSection body) {
            this(verb, call, body, DscTargetDirective.Route.NONE);
        }
    }

    record VfxBlock(java.util.Map<String, Object> config, DscTargetDirective.Route route) implements DscStatement {
        public VfxBlock(java.util.Map<String, Object> config) {
            this(config, DscTargetDirective.Route.NONE);
        }
    }

    record FxBlock(java.util.Map<String, Object> config, DscTargetDirective.Route route) implements DscStatement {
        public FxBlock(java.util.Map<String, Object> config) {
            this(config, DscTargetDirective.Route.NONE);
        }
    }

    record ProjBlock(
            String projectile,
            double speed,
            DscSection tickBody,
            DscSection hitBody,
            DscTargetDirective.Route route
    ) implements DscStatement {
        public ProjBlock(String projectile, double speed, DscSection tickBody, DscSection hitBody) {
            this(projectile, speed, tickBody, hitBody, DscTargetDirective.Route.NONE);
        }

        public ProjBlock(String projectile, double speed, DscSection body) {
            this(projectile, speed, body, null, DscTargetDirective.Route.NONE);
        }
    }

    record IfBlock(
            String condition,
            DscSection thenBody,
            java.util.List<ElseIfBranch> elseIfBranches,
            DscSection elseBody,
            DscTargetDirective.Route route
    ) implements DscStatement {
        public IfBlock(String condition, DscSection thenBody, DscSection elseBody) {
            this(condition, thenBody, java.util.List.of(), elseBody, DscTargetDirective.Route.NONE);
        }

        public IfBlock(
                String condition,
                DscSection thenBody,
                java.util.List<ElseIfBranch> elseIfBranches,
                DscSection elseBody
        ) {
            this(condition, thenBody, elseIfBranches, elseBody, DscTargetDirective.Route.NONE);
        }
    }

    record ElseIfBranch(String condition, DscSection body) {
    }

    record ChanceBlock(String chance, DscSection body, DscTargetDirective.Route route) implements DscStatement {
        public ChanceBlock(String chance, DscSection body) {
            this(chance, body, DscTargetDirective.Route.NONE);
        }
    }
}
