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
    private final Map<String, DscBlock> modules = new LinkedHashMap<>();

    public DscCompiler(PluginConfig config) {
        this.config = config;
    }

    public List<DefDefinition> compile(DscScript script) {
        modules.clear();
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

        CastItemSpec item = parseItem(props.get("item"));
        Map<String, String> scope = defaultScope(block);
        List<EffectDefinition> effects = compileSection(block.sections().get("do"), scope);
        Map<ChainTrigger, List<ChainEntry>> chain = compileChain(block.sections());

        return new DefDefinition(
                id,
                props.getOrDefault("name", id),
                props.getOrDefault("desc", props.getOrDefault("description", "")),
                props.getOrDefault("perm", props.getOrDefault("permission", config.castPermissionPrefix() + id)),
                parseDouble(props.get("cd"), parseDouble(props.get("cooldown"), 0)),
                TriggerType.parse(first(props, "command", "key", "on", "trigger")),
                TargetMode.parse(first(props, "none", "tgt", "to", "target")),
                parseDouble(props.get("rng"), parseDouble(props.get("range"), config.defaultRange())),
                block.helper(),
                item,
                effects,
                chain
        );
    }

    private Map<ChainTrigger, List<ChainEntry>> compileChain(Map<String, DscSection> sections) {
        Map<ChainTrigger, List<ChainEntry>> chain = new EnumMap<>(ChainTrigger.class);
        putChainSection(chain, ChainTrigger.ON_CAST, sections.get("cast"));
        putChainSection(chain, ChainTrigger.ON_HIT, sections.get("hit"));
        putChainSection(chain, ChainTrigger.ON_COMPLETE, sections.get("done"));
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
            if (statement instanceof DscStatement.EffectLine effectLine) {
                String line = effectLine.line().trim();
                if (line.startsWith("@")) {
                    entries.add(new ChainEntry(cleanModuleId(line), Map.of()));
                } else if (line.startsWith("call ") || line.startsWith("use ")) {
                    entries.add(parseCallChainEntry(line));
                } else {
                    throw new DscParseException("Chain sections support @module or call/use only: " + line);
                }
                continue;
            }
            throw new DscParseException("Nested wait blocks are not supported in chain sections");
        }
        if (!entries.isEmpty()) {
            chain.put(trigger, entries);
        }
    }

    private ChainEntry moduleChainEntry(DscStatement.ModuleCall call) {
        DscBlock module = requireModule(call.moduleId());
        Map<String, Object> args = buildArgs(module.params(), call.args());
        return new ChainEntry(module.id(), args);
    }

    private ChainEntry parseCallChainEntry(String line) {
        String trimmed = line.startsWith("call ") ? line.substring(5).trim() : line.substring(4).trim();
        int paren = trimmed.indexOf('(');
        if (paren < 0) {
            return new ChainEntry(trimmed.toLowerCase(Locale.ROOT), Map.of());
        }
        String id = trimmed.substring(0, paren).trim().toLowerCase(Locale.ROOT);
        String argsRaw = trimmed.substring(paren + 1, trimmed.lastIndexOf(')')).trim();
        DscBlock module = requireModule(id);
        List<String> argValues = splitArgs(argsRaw);
        return new ChainEntry(id, buildArgs(module.params(), argValues));
    }

    private List<EffectDefinition> compileSection(DscSection section, Map<String, String> scope) {
        if (section == null) {
            return List.of();
        }
        List<EffectDefinition> effects = new ArrayList<>();
        for (DscStatement statement : section.statements()) {
            effects.addAll(compileStatement(statement, scope));
        }
        return effects;
    }

    private List<EffectDefinition> compileStatement(DscStatement statement, Map<String, String> scope) {
        return switch (statement) {
            case DscStatement.EffectLine line -> List.of(parseEffectLine(substitute(line.line(), scope)));
            case DscStatement.ModuleCall call -> expandModule(call, scope);
            case DscStatement.WaitBlock wait -> List.of(compileWait(wait, scope));
        };
    }

    private EffectDefinition compileWait(DscStatement.WaitBlock wait, Map<String, String> scope) {
        Map<String, Object> data = new HashMap<>();
        data.put("ticks", parseDurationToken(wait.duration()));
        List<EffectDefinition> nested = compileSection(wait.body(), scope);
        if (!nested.isEmpty()) {
            data.put("effects", nested);
        }
        return new EffectDefinition("delay", data, List.of());
    }

    private List<EffectDefinition> expandModule(DscStatement.ModuleCall call, Map<String, String> outerScope) {
        DscBlock module = requireModule(call.moduleId());
        Map<String, String> scope = mergeScope(outerScope, module.params(), call.args());
        DscSection body = module.sections().get("do");
        if (body == null) {
            return List.of(new EffectDefinition("def", Map.of("def", module.id()), List.of()));
        }
        return compileSection(body, scope);
    }

    private Map<String, String> mergeScope(Map<String, String> outer, List<String> paramNames, List<String> values) {
        Map<String, String> scope = new HashMap<>(outer);
        for (int i = 0; i < paramNames.size(); i++) {
            if (i < values.size()) {
                scope.put(paramNames.get(i), values.get(i));
            }
        }
        return scope;
    }

    private Map<String, Object> buildArgs(List<String> paramNames, List<String> values) {
        Map<String, Object> args = new HashMap<>();
        for (int i = 0; i < paramNames.size() && i < values.size(); i++) {
            String param = paramNames.get(i);
            String value = values.get(i);
            args.put(param, parseArgValue(value));
            mapCommonArgAlias(args, param, value);
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
            throw new DscParseException("Unknown module/effect: " + id);
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

    private String cleanModuleId(String line) {
        String trimmed = line.startsWith("@") ? line.substring(1).trim() : line.trim();
        int paren = trimmed.indexOf('(');
        if (paren >= 0) {
            trimmed = trimmed.substring(0, paren).trim();
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private List<String> splitArgs(String raw) {
        if (raw.isBlank()) {
            return List.of();
        }
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                current.append(c);
                continue;
            }
            if (c == ',' && !inQuote) {
                args.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            args.add(current.toString().trim());
        }
        return args;
    }
}
