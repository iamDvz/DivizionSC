package ru.iamdvz.divizionsc.def.service;

import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.def.model.DefDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class DefRegistry {

    private final Map<String, DefDefinition> defs = new LinkedHashMap<>();
    private final Map<String, String> sources = new HashMap<>();

    public void register(DefDefinition def, String source, DefLoadReport report) {
        String id = def.id().toLowerCase(Locale.ROOT);
        DefDefinition normalized = def.id().equals(id) ? def : copyWithId(def, id);
        DefDefinition previous = defs.get(id);
        if (previous != null) {
            String previousSource = sources.getOrDefault(id, "unknown");
            String warning = "Def '" + id + "' overwritten (" + previousSource + " -> " + source + ")";
            if (report != null) {
                report.addWarning(warning);
            }
        }
        defs.put(id, normalized);
        sources.put(id, source);
    }

    private static DefDefinition copyWithId(DefDefinition def, String id) {
        return new DefDefinition(
                id, def.name(), def.description(), def.permission(), def.cooldown(),
                def.trigger(), def.targetMode(), def.range(), def.helper(), def.castItem(),
                def.effects(), def.chain()
        );
    }

    public void register(DefDefinition def) {
        register(def, "unknown", null);
    }

    public Optional<DefDefinition> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(defs.get(id.toLowerCase()));
    }

    public Optional<String> sourceOf(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sources.get(id.toLowerCase()));
    }

    public Collection<DefDefinition> all() {
        return defs.values();
    }

    public int size() {
        return defs.size();
    }

    public void clear() {
        defs.clear();
        sources.clear();
    }
}
