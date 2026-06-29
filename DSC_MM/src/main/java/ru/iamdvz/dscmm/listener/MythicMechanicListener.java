package ru.iamdvz.dscmm.listener;

import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.iamdvz.dscmm.MmContext;
import ru.iamdvz.dscmm.mechanic.DscDefMechanic;

import java.util.Locale;
import java.util.Set;

public final class MythicMechanicListener implements Listener {

    private static final Set<String> MECHANIC_NAMES = Set.of(
            "dscdef",
            "dscskill",
            "dsc",
            "divizionsc"
    );

    private final MmContext context;

    public MythicMechanicListener(MmContext context) {
        this.context = context;
    }

    @EventHandler
    public void onMechanicLoad(MythicMechanicLoadEvent event) {
        if (!MECHANIC_NAMES.contains(event.getMechanicName().toLowerCase(Locale.ROOT))) {
            return;
        }
        event.register(new DscDefMechanic(context, event.getConfig()));
    }
}
