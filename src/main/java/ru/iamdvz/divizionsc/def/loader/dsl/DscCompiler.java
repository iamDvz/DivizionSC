package ru.iamdvz.divizionsc.def.loader.dsl;

import org.bukkit.Material;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.loader.simple.SimpleEffectParser;
import ru.iamdvz.divizionsc.def.model.CastItemSpec;
import ru.iamdvz.divizionsc.def.model.ChainEntry;
import ru.iamdvz.divizionsc.def.model.ChainTrigger;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.divizionsc.def.model.TargetMode;
import ru.iamdvz.divizionsc.def.model.PassiveTriggerType;
import ru.iamdvz.divizionsc.def.model.TriggerType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DscCompiler {

    private final PluginConfig config;
    private final SimpleEffectParser effectParser = new SimpleEffectParser();
    private final DscEffectDesugar desugar = new DscEffectDesugar();
    private final Map<String, DscBlock> modules = new LinkedHashMap<>();
    private TargetMode defaultTarget = TargetMode.NONE;

    public DscCompiler(PluginConfig config) {
        this.config = config;
    }

    public List<DefDefinition> compile(DscScript script) {
        return compile(script, Map.of());
    }

    public List<DefDefinition> compile(DscScript script, Map<String, DscBlock> externalModules) {
        modules.clear();
        modules.putAll(externalModules);
        for (DscBlock block : script.blocks()) {
            if (block.helper()) {
                modules.put(block.id(), block);
            }
        }

        List<DefDefinition> result = new ArrayList<>();
        for (DscBlock block : script.blocks()) {
            result.add(compileBlock(block));
        }
        return result;
    }

    private DefDefinition compileBlock(DscBlock block) {
        Map<String, String> props = block.properties();
        String id = block.id();
        defaultTarget = TargetMode.parse(first(props, "none", "tgt", "to", "target"));

        CastItemSpec item = parseItem(first(props, null, "item", "cast-item", "cast_item"));
        Map<String, String> scope = defaultScope(block);
        DscSection mainBody = firstSection(block.sections(), "cast", "effects", "do");
        List<EffectDefinition> effects = compileSection(mainBody, scope);
        Map<ChainTrigger, List<ChainEntry>> chain = compileChain(block.sections());

        boolean passive = block.kind() == DscBlockKind.PASSIVE || parseBoolean(props.get("passive"), false);
        PassiveTriggers passiveTriggers = resolvePassiveTriggers(props, passive);
        int passiveInterval = parsePassiveInterval(props, passiveTriggers.event());
        int passivePressCount = parsePassivePressCount(props);
        int passivePressWindow = parsePassivePressWindow(props, passivePressCount);

        return new DefDefinition(
                id,
                props.getOrDefault("name", id),
                props.getOrDefault("desc", props.getOrDefault("description", "")),
                props.getOrDefault("perm", props.getOrDefault("permission", config.castPermissionPrefix() + id)),
                parseDouble(props.get("cd"), parseDouble(props.get("cooldown"), 0)),
                parseDouble(props.get("mana"), parseDouble(props.get("mp"), parseDouble(props.get("cost"), 0))),
                passive ? TriggerType.COMMAND : TriggerType.parse(first(props, "command", "key", "on", "trigger")),
                TargetMode.parse(first(props, "none", "tgt", "to", "target")),
                parseDouble(props.get("rng"), parseDouble(props.get("range"), config.defaultRange())),
                block.helper(),
                passive,
                passiveTriggers.event(),
                passiveTriggers.key(),
                Math.max(1, passiveInterval),
                Math.max(1, passivePressCount),
                Math.max(1, passivePressWindow),
                item,
                effects,
                chain
        );
    }

    private DscSection firstSection(Map<String, DscSection> sections, String... keys) {
        for (String key : keys) {
            DscSection section = sections.get(key);
            if (section != null) {
                return section;
            }
        }
        return null;
    }

    private record PassiveTriggers(PassiveTriggerType event, TriggerType key) {
    }

    private Map<ChainTrigger, List<ChainEntry>> compileChain(Map<String, DscSection> sections) {
        Map<ChainTrigger, List<ChainEntry>> chain = new EnumMap<>(ChainTrigger.class);
        putChainSection(chain, ChainTrigger.ON_CAST, sections.get("start"));
        putChainSection(chain, ChainTrigger.ON_HIT, sections.get("hit"));
        putChainSection(chain, ChainTrigger.ON_COMPLETE, firstSection(sections, "done", "complete", "end"));
        return chain;
    }

    private void putChainSection(Map<ChainTrigger, List<ChainEntry>> chain, ChainTrigger trigger, DscSection section) {
        if (section == null) {
            return;
        }
        List<ChainEntry> entries = new ArrayList<>();
        for (DscStatement statement : section.statements()) {
            if (statement instanceof DscStatement.ModuleCall call) {
                entries.add(moduleChainEntry(call));
                continue;
            }
            throw new DscParseException("Chain sections (start/hit/done) accept @module(...) only");
        }
        if (!entries.isEmpty()) {
            chain.put(trigger, entries);
        }
    }

    private ChainEntry moduleChainEntry(DscStatement.ModuleCall call) {
        DscBlock module = requireModule(call.moduleId());
        Map<String, Object> args = buildArgs(module.params(), call.positional(), call.named());
        return new ChainEntry(module.id(), args);
    }

    private List<EffectDefinition> compileSection(DscSection section, Map<String, String> scope) {
        return compileSection(section, scope, DscTargetDirective.Route.NONE);
    }

    private List<EffectDefinition> compileSection(DscSection section, Map<String, String> scope, DscTargetDirective.Route inheritedRoute) {
        if (section == null) {
            return List.of();
        }
        List<EffectDefinition> effects = new ArrayList<>();
        for (DscStatement statement : section.statements()) {
            effects.addAll(compileStatement(statement, scope, inheritedRoute));
        }
        return effects;
    }

    private List<EffectDefinition> compileStatement(DscStatement statement, Map<String, String> scope) {
        return compileStatement(statement, scope, DscTargetDirective.Route.NONE);
    }

    private List<EffectDefinition> compileStatement(
            DscStatement statement,
            Map<String, String> scope,
            DscTargetDirective.Route inheritedRoute
    ) {
        return switch (statement) {
            case DscStatement.EffectCall call -> List.of(applyRoute(
                    parseEffectCall(call, scope),
                    resolveRoute(call.route(), inheritedRoute, call.call().name())
            ));
            case DscStatement.EffectLine line -> List.of(applyRoute(
                    parseEffectLine(substitute(line.line(), scope)),
                    resolveRoute(line.route(), inheritedRoute, guessEffectName(line.line()))
            ));
            case DscStatement.ModuleCall call -> applyRouteList(
                    expandModule(call, scope, inheritedRoute),
                    resolveRoute(call.route(), inheritedRoute, call.moduleId())
            );
            case DscStatement.WaitBlock wait -> List.of(compileWait(wait, scope, inheritedRoute));
            case DscStatement.EffectBlock block -> List.of(compileEffectBlock(block, scope, inheritedRoute));
            case DscStatement.VfxBlock vfx -> List.of(compileVfx(vfx, inheritedRoute));
            case DscStatement.FxBlock fx -> List.of(compileFx(fx, inheritedRoute));
            case DscStatement.ProjBlock proj -> List.of(compileProjectile(proj, scope, inheritedRoute));
            case DscStatement.IfBlock conditional -> List.of(compileIf(conditional, scope, inheritedRoute));
            case DscStatement.ChanceBlock chance -> List.of(compileChance(chance, scope, inheritedRoute));
        };
    }

    private DscTargetDirective.Route resolveRoute(
            DscTargetDirective.Route own,
            DscTargetDirective.Route inherited,
            String effectName
    ) {
        DscTargetDirective.Route merged = mergeRoute(own, inherited);
        if (merged.to() != null) {
            return merged;
        }
        return DscRouteDefaults.forEffect(effectName, defaultTarget).mergeInherited(merged);
    }

    private String guessEffectName(String line) {
        String trimmed = line.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    private DscTargetDirective.Route mergeRoute(DscTargetDirective.Route own, DscTargetDirective.Route inherited) {
        if (own == null || own.isEmpty()) {
            return inherited == null ? DscTargetDirective.Route.NONE : inherited;
        }
        return own.mergeInherited(inherited);
    }

    private EffectDefinition parseEffectCall(DscStatement.EffectCall call, Map<String, String> scope) {
        String legacy = substitute(desugar.desugar(call.call()), scope);
        return parseEffectLine(legacy);
    }

    private EffectDefinition compileIf(DscStatement.IfBlock block, Map<String, String> scope, DscTargetDirective.Route inheritedRoute) {
        DscTargetDirective.Route blockRoute = resolveRoute(block.route(), inheritedRoute, "if");
        Map<String, Object> data = new HashMap<>();
        data.put("conditions", List.of(substitute(block.condition(), scope)));
        List<EffectDefinition> thenEffects = compileSection(block.thenBody(), scope, blockRoute);
        if (!thenEffects.isEmpty()) {
            data.put("effects", thenEffects);
        }
        DscSection elseSection = buildElseSection(block, scope);
        if (elseSection != null) {
            List<EffectDefinition> elseEffects = compileSection(elseSection, scope, blockRoute);
            if (!elseEffects.isEmpty()) {
                data.put("else", elseEffects);
            }
        }
        return new EffectDefinition("if", data, List.of());
    }

    private DscSection buildElseSection(DscStatement.IfBlock block, Map<String, String> scope) {
        DscSection elseSection = block.elseBody();
        List<DscStatement.ElseIfBranch> branches = block.elseIfBranches();
        for (int i = branches.size() - 1; i >= 0; i--) {
            DscStatement.ElseIfBranch branch = branches.get(i);
            List<DscStatement> nested = new ArrayList<>();
            nested.add(new DscStatement.IfBlock(
                    substitute(branch.condition(), scope),
                    branch.body(),
                    List.of(),
                    elseSection
            ));
            elseSection = new DscSection("else", nested);
        }
        return elseSection;
    }

    private EffectDefinition compileChance(DscStatement.ChanceBlock block, Map<String, String> scope, DscTargetDirective.Route inheritedRoute) {
        DscTargetDirective.Route blockRoute = resolveRoute(block.route(), inheritedRoute, "chance");
        Map<String, Object> data = new HashMap<>();
        data.put("chance", parseChance(substitute(block.chance(), scope)));
        List<EffectDefinition> body = compileSection(block.body(), scope, blockRoute);
        if (!body.isEmpty()) {
            data.put("effects", body);
        }
        return new EffectDefinition("chance", data, List.of());
    }

    private EffectDefinition compileEffectBlock(DscStatement.EffectBlock block, Map<String, String> scope, DscTargetDirective.Route inheritedRoute) {
        DscTargetDirective.Route blockRoute = resolveRoute(block.route(), inheritedRoute, block.verb());
        Map<String, Object> data = new HashMap<>();
        DscCallParser.ParsedCall call = block.call();
        String verb = block.verb();
        switch (verb) {
            case "area" -> data.put("radius", parseDouble(argValue(call, "radius", "5"), 5));
            case "loop" -> {
                data.put("iterations", (int) parseDouble(argValue(call, "times", argValue(call, "iterations", "1")), 1));
                String interval = argNamed(call, "interval");
                if (interval != null) {
                    data.put("interval", (int) parseDouble(interval, 1));
                }
            }
            case "aura" -> {
                data.put("radius", parseDouble(argValue(call, "radius", "4"), 4));
                String duration = argNamed(call, "duration");
                if (duration != null) {
                    data.put("duration", parseDurationToken(duration));
                }
                String interval = argNamed(call, "interval");
                if (interval != null) {
                    data.put("interval", (int) parseDouble(interval, 20));
                }
            }
            case "repeat" -> data.put("times", (int) parseDouble(argValue(call, "times", "1"), 1));
            case "raycast", "beam" -> {
                data.put("distance", parseDouble(argValue(call, "distance", "15"), 15));
                data.put("hit_radius", parseDouble(argNamed(call, "hit_radius") != null
                        ? argNamed(call, "hit_radius") : argValue(call, "radius", "1"), 1));
                String maxHits = argNamed(call, "max_hits");
                if (maxHits == null) {
                    maxHits = argNamed(call, "hits");
                }
                if (maxHits == null) {
                    maxHits = argNamed(call, "targets");
                }
                if (maxHits != null) {
                    data.put("max_hits", (int) parseDouble(maxHits, 1));
                }
            }
            case "chain" -> {
                data.put("distance", parseDouble(argValue(call, "distance", "18"), 18));
                data.put("hit_radius", parseDouble(argNamed(call, "hit_radius") != null
                        ? argNamed(call, "hit_radius") : "1.5", 1.5));
                data.put("max_hits", (int) parseDouble(argValue(call, "hits",
                        argValue(call, "times", argValue(call, "targets", "3"))), 3));
            }
            default -> throw new DscParseException("Unsupported effect block: " + verb, call.lineNumber());
        }
        List<EffectDefinition> nested = compileSection(block.body(), scope, blockRoute);
        if (!nested.isEmpty()) {
            data.put("effects", nested);
        }
        return new EffectDefinition(verb, data, List.of());
    }

    private String argValue(DscCallParser.ParsedCall call, String key, String fallback) {
        String named = argNamed(call, key);
        if (named != null) {
            return named;
        }
        List<String> positional = desugar.positionalArgs(call);
        if (!positional.isEmpty()) {
            return positional.getFirst();
        }
        return fallback;
    }

    private String argNamed(DscCallParser.ParsedCall call, String key) {
        return desugar.namedArgs(call).get(key.toLowerCase(Locale.ROOT));
    }

    private double parseChance(String raw) {
        String token = raw.trim();
        if (token.endsWith("%")) {
            return parseDouble(token.substring(0, token.length() - 1), 50) / 100.0;
        }
        return parseDouble(token, 0.5);
    }

    private EffectDefinition compileVfx(DscStatement.VfxBlock vfx, DscTargetDirective.Route inheritedRoute) {
        Map<String, Object> config = new HashMap<>(vfx.config());
        if (config.containsKey("position") || config.containsKey("at")) {
            if (!config.containsKey("at") && config.containsKey("position")) {
                config.put("at", String.valueOf(config.get("position")));
            }
            return new EffectDefinition("vfx", config, List.of());
        }
        DscTargetDirective.Route route = resolveRoute(vfx.route(), inheritedRoute, "vfx");
        if (!route.isEmpty() && route.to() != null) {
            config.put("position", mapVfxPosition(route.to()));
            if (route.hasExplicitFrom() && route.from() != null) {
                config.put("from", mapVfxPosition(route.from()));
            }
        }
        return applyRoute(new EffectDefinition("vfx", config, List.of()), route);
    }

    private EffectDefinition compileFx(DscStatement.FxBlock fx, DscTargetDirective.Route inheritedRoute) {
        Map<String, Object> config = new HashMap<>(DscFxBlockNormalizer.normalize(fx.config()));
        if (config.containsKey("at")) {
            config.put("at", mapFxPosition(String.valueOf(config.get("at"))));
            return new EffectDefinition("effectlib", config, List.of());
        }
        DscTargetDirective.Route route = resolveRoute(fx.route(), inheritedRoute, "fx");
        if (!route.isEmpty() && route.to() != null) {
            config.put("at", mapFxPosition(route.to()));
            if (route.hasExplicitFrom() && route.from() != null) {
                config.put("from", mapFxPosition(route.from()));
            }
        }
        return applyRoute(new EffectDefinition("effectlib", config, List.of()), route);
    }

    private String mapFxPosition(String target) {
        return switch (target) {
            case "self", "caster" -> "caster";
            case "target", "entity" -> "target";
            case "block", "location" -> "effect";
            case "eyes", "eye" -> "eyes";
            default -> target;
        };
    }

    private String mapVfxPosition(String target) {
        return switch (target) {
            case "self", "caster" -> "caster";
            case "target", "entity" -> "target";
            case "block", "location" -> "target";
            default -> target;
        };
    }

    private EffectDefinition compileProjectile(DscStatement.ProjBlock proj, Map<String, String> scope, DscTargetDirective.Route inheritedRoute) {
        DscTargetDirective.Route blockRoute = resolveRoute(proj.route(), inheritedRoute, "projectile");
        Map<String, Object> data = new HashMap<>();
        data.put("projectile", proj.projectile());
        data.put("speed", proj.speed());
        data.put("tick_interval", 1);
        if (proj.tickBody() != null) {
            List<EffectDefinition> onTick = compileSection(proj.tickBody(), scope, blockRoute);
            if (!onTick.isEmpty()) {
                data.put("on_tick", onTick);
            }
        }
        if (proj.hitBody() != null) {
            List<EffectDefinition> onHit = compileSection(proj.hitBody(), scope, blockRoute);
            if (!onHit.isEmpty()) {
                data.put("on_hit", onHit);
            }
        }
        return applyRoute(new EffectDefinition("projectile", data, List.of()), blockRoute);
    }

    private EffectDefinition compileWait(DscStatement.WaitBlock wait, Map<String, String> scope, DscTargetDirective.Route inheritedRoute) {
        DscTargetDirective.Route blockRoute = resolveRoute(wait.route(), inheritedRoute, "after");
        Map<String, Object> data = new HashMap<>();
        data.put("ticks", parseDurationToken(substitute(wait.duration(), scope)));
        List<EffectDefinition> nested = compileSection(wait.body(), scope, blockRoute);
        if (!nested.isEmpty()) {
            data.put("effects", nested);
        }
        return new EffectDefinition("delay", data, List.of());
    }

    private List<EffectDefinition> expandModule(
            DscStatement.ModuleCall call,
            Map<String, String> outerScope,
            DscTargetDirective.Route inheritedRoute
    ) {
        DscBlock module = requireModule(call.moduleId());
        Map<String, String> scope = mergeScope(outerScope, module.params(), call.positional(), call.named());
        DscSection body = firstSection(module.sections(), "effects", "cast", "do");
        if (body == null) {
            return List.of(new EffectDefinition("def", Map.of("def", module.id()), List.of()));
        }
        DscTargetDirective.Route route = resolveRoute(call.route(), inheritedRoute, call.moduleId());
        return compileSection(body, scope, route);
    }

    private Map<String, String> mergeScope(
            Map<String, String> outer,
            List<String> paramNames,
            List<String> positional,
            Map<String, String> named
    ) {
        Map<String, String> scope = new HashMap<>(outer);
        for (int i = 0; i < paramNames.size(); i++) {
            String param = paramNames.get(i);
            String namedValue = named.get(param.toLowerCase(Locale.ROOT));
            if (namedValue != null) {
                scope.put(param, namedValue);
            } else if (i < positional.size()) {
                scope.put(param, positional.get(i));
            }
        }
        for (Map.Entry<String, String> entry : named.entrySet()) {
            scope.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return scope;
    }

    private Map<String, Object> buildArgs(
            List<String> paramNames,
            List<String> positional,
            Map<String, String> named
    ) {
        Map<String, Object> args = new HashMap<>();
        for (int i = 0; i < paramNames.size(); i++) {
            String param = paramNames.get(i);
            String raw = named.get(param.toLowerCase(Locale.ROOT));
            if (raw == null && i < positional.size()) {
                raw = positional.get(i);
            }
            if (raw != null) {
                args.put(param, parseArgValue(raw));
                mapCommonArgAlias(args, param, raw);
            }
        }
        for (Map.Entry<String, String> entry : named.entrySet()) {
            args.putIfAbsent(entry.getKey(), parseArgValue(entry.getValue()));
            mapCommonArgAlias(args, entry.getKey(), entry.getValue());
        }
        return args;
    }

    private void mapCommonArgAlias(Map<String, Object> args, String param, String value) {
        if ("dmg".equalsIgnoreCase(param) || "damage".equalsIgnoreCase(param) || "amount".equalsIgnoreCase(param)) {
            args.put("amount", parseArgValue(value));
        }
    }

    private Object parseArgValue(String raw) {
        try {
            if (raw.contains(".")) {
                return Double.parseDouble(raw);
            }
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private EffectDefinition parseEffectLine(String line) {
        SimpleEffectParser.ParseResult parsed = effectParser.parse(line);
        if (parsed.effect() != null) {
            return parsed.effect();
        }
        throw new DscParseException("Unknown effect line: " + line);
    }

    private String substitute(String line, Map<String, String> scope) {
        String result = line;
        for (Map.Entry<String, String> entry : scope.entrySet()) {
            result = result.replace("$" + entry.getKey(), entry.getValue());
        }
        return result;
    }

    private DscBlock requireModule(String id) {
        DscBlock module = modules.get(id);
        if (module == null) {
            throw new DscParseException("Unknown module: " + id);
        }
        return module;
    }

    private CastItemSpec parseItem(String compact) {
        if (compact == null || compact.isBlank()) {
            return null;
        }
        String[] parts = compact.split("\\|", -1);
        Material material = Material.matchMaterial(parts[0].trim().toUpperCase(Locale.ROOT));
        if (material == null) {
            material = Material.STICK;
        }
        String name = parts.length > 1 ? parts[1].trim() : null;
        List<String> lore = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            lore.add(parts[i].trim());
        }
        return new CastItemSpec(material, name, lore, 0);
    }

    private int parseDurationToken(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.endsWith("s")) {
            return (int) Math.round(parseDouble(lower.substring(0, lower.length() - 1), 1) * 20);
        }
        if (lower.endsWith("t")) {
            return (int) Math.round(parseDouble(lower.substring(0, lower.length() - 1), 20));
        }
        return (int) Math.round(parseDouble(lower, 20));
    }

    private Map<String, String> defaultScope(DscBlock block) {
        if (!block.helper() || block.params().isEmpty()) {
            return Map.of();
        }
        Map<String, String> scope = new HashMap<>();
        for (String param : block.params()) {
            scope.put(param, "0");
        }
        return scope;
    }

    private PassiveTriggers resolvePassiveTriggers(Map<String, String> props, boolean passive) {
        if (!passive) {
            return new PassiveTriggers(null, null);
        }

        PassiveTriggerType explicitEvent = PassiveTriggerType.parse(
                first(props, null, "passive-trigger", "ptrigger", "passive_trigger", "event"));
        if (explicitEvent != null) {
            return new PassiveTriggers(explicitEvent, null);
        }

        TriggerType explicitKey = TriggerType.parsePassiveKey(
                first(props, null, "key", "passive-key", "pkey", "passive_key"));
        if (explicitKey != null) {
            return new PassiveTriggers(null, explicitKey);
        }

        String shared = first(props, null, "on", "trigger");
        if (shared != null) {
            PassiveTriggerType asEvent = PassiveTriggerType.parse(shared);
            if (asEvent != null) {
                return new PassiveTriggers(asEvent, null);
            }
            TriggerType asKey = TriggerType.parsePassiveKey(shared);
            if (asKey != null) {
                return new PassiveTriggers(null, asKey);
            }
        }

        return new PassiveTriggers(null, null);
    }

    private int parsePassivePressCount(Map<String, String> props) {
        String raw = first(props, null, "presses", "press-count", "press_count", "combo");
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        return Math.max(1, (int) Math.round(parseDouble(raw, 1)));
    }

    private int parsePassivePressWindow(Map<String, String> props, int pressCount) {
        String raw = first(props, null, "press-window", "press_window", "window", "press-interval");
        if (raw == null || raw.isBlank()) {
            return pressCount > 1 ? 40 : 1;
        }
        return Math.max(1, parseDurationToken(raw));
    }

    private int parsePassiveInterval(Map<String, String> props, PassiveTriggerType passiveTrigger) {
        String raw = first(props, null, "passive-interval", "interval");
        if (raw == null || raw.isBlank()) {
            return passiveTrigger == PassiveTriggerType.INTERVAL ? 20 : 1;
        }
        return Math.max(1, parseDurationToken(raw));
    }

    private boolean parseBoolean(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1", "on" -> true;
            case "false", "no", "0", "off" -> false;
            default -> fallback;
        };
    }

    private double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String first(Map<String, String> props, String defaultValue, String... keys) {
        for (String key : keys) {
            String value = props.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return defaultValue;
    }

    private List<EffectDefinition> applyRouteList(List<EffectDefinition> effects, DscTargetDirective.Route route) {
        if (route == null || route.isEmpty()) {
            return effects;
        }
        List<EffectDefinition> result = new ArrayList<>(effects.size());
        for (EffectDefinition effect : effects) {
            result.add(applyRoute(effect, route));
        }
        return result;
    }

    private EffectDefinition applyRoute(EffectDefinition effect, DscTargetDirective.Route route) {
        if (route == null || route.isEmpty() || route.to() == null) {
            return effect;
        }
        DscTargetDirective.Route effective = route.resolved();
        Map<String, Object> data = new HashMap<>(effect.data());
        stampRouteFields(data, effect.type(), route, effective);
        for (String key : List.of("effects", "else", "on_hit", "on_tick", "then")) {
            Object value = data.get(key);
            if (value instanceof List<?> list) {
                data.put(key, applyRouteChildren(list, route));
            }
        }
        List<EffectDefinition> nested = effect.nested();
        if (!nested.isEmpty()) {
            nested = applyRouteList(nested, route);
        }
        return new EffectDefinition(effect.type(), data, nested);
    }

    @SuppressWarnings("unchecked")
    private List<EffectDefinition> applyRouteChildren(List<?> list, DscTargetDirective.Route route) {
        List<EffectDefinition> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof EffectDefinition effectDefinition) {
                result.add(applyRoute(effectDefinition, route));
            }
        }
        return result;
    }

    private void stampRouteFields(
            Map<String, Object> data,
            String type,
            DscTargetDirective.Route route,
            DscTargetDirective.Route effective
    ) {
        if (effective.to() == null) {
            return;
        }
        if (usesAtField(type)) {
            data.put("at", toAtField(effective.to()));
            if (route.hasExplicitFrom() && !effective.from().equals(effective.to())) {
                data.put("from", toAtField(effective.from()));
            }
        }
        if (usesEntityTargetField(type)) {
            data.put("target", toEntityTargetField(effective.to()));
            if (route.hasExplicitFrom()) {
                data.put("from", toAtField(effective.from()));
            }
        }
        if ("raycast".equals(type) || "beam".equals(type) || "chain".equals(type)
                || "projectile".equals(type) || "particle_projectile".equals(type)) {
            data.put("from", toAtField(effective.from()));
            data.put("to", toAtField(effective.to()));
        }
    }

    private boolean usesAtField(String type) {
        return switch (type) {
            case "sound", "particle", "effectlib", "fx", "lightning", "shape", "shape_particle", "vfx" -> true;
            default -> false;
        };
    }

    private boolean usesEntityTargetField(String type) {
        return switch (type) {
            case "damage", "heal", "potion", "stun", "pull", "push", "shield", "dash", "blink", "velocity", "knockback",
                    "ignite", "glow", "invis", "invisibility", "root", "swap", "cleanse", "purge", "launch" -> true;
            default -> false;
        };
    }

    private String toAtField(String target) {
        return switch (target) {
            case "self", "caster" -> "caster";
            case "target", "entity" -> "target";
            case "block", "location" -> "effect";
            case "eyes", "eye" -> "eyes";
            default -> target;
        };
    }

    private String toEntityTargetField(String target) {
        return switch (target) {
            case "entity" -> "target";
            case "caster" -> "self";
            default -> target;
        };
    }
}
