package ru.iamdvz.divizionsc.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.iamdvz.divizionsc.DivizionSC;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.service.DefRegistry;
import ru.iamdvz.divizionsc.def.service.DefService;

import java.util.Collection;
import java.util.Optional;

public final class DivizionSCApi {

    private static DivizionSC plugin;

    private DivizionSCApi() {
    }

    public static void bind(DivizionSC divizionSc) {
        plugin = divizionSc;
    }

    public static void bind(Plugin divizionSc) {
        if (divizionSc instanceof DivizionSC dsc) {
            plugin = dsc;
        }
    }

    public static void unbind(DivizionSC divizionSc) {
        if (plugin == divizionSc) {
            plugin = null;
        }
    }

    public static void unbind(Plugin divizionSc) {
        if (plugin == divizionSc) {
            plugin = null;
        }
    }

    public static boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }

    public static EffectHandlerRegistry effectHandlers() {
        return requirePlugin().context().effectHandlers();
    }

    public static DefRegistry defRegistry() {
        return requirePlugin().context().defRegistry();
    }

    public static Optional<DefDefinition> findDef(String id) {
        return defRegistry().find(id);
    }

    public static Collection<DefDefinition> allDefs() {
        return defRegistry().all();
    }

    public static DefService.CastResult cast(Player player, String defId) {
        return requirePlugin().context().defs().castFromCommand(player, defId);
    }

    public static DefLoadReport reloadDefs() {
        return requirePlugin().context().reloadDefs();
    }

    public static DefLoadReport lastLoadReport() {
        return requirePlugin().context().lastLoadReport();
    }

    public static Plugin plugin() {
        return requirePlugin();
    }

    private static DivizionSC requirePlugin() {
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalStateException("DivizionSC is not enabled");
        }
        return plugin;
    }
}
