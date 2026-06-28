package ru.iamdvz.divizionsc.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import ru.iamdvz.divizionsc.PluginContext;

public final class DefAddonListener implements Listener {

    private final PluginContext context;
    private final Plugin host;

    public DefAddonListener(PluginContext context, Plugin host) {
        this.context = context;
        this.host = host;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin() == host) {
            return;
        }
        context.loadAddonDefs(event.getPlugin());
    }
}
