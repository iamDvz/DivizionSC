package ru.iamdvz.divizionsc.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;

public final class DefLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final DefLoadReport report;

    public DefLoadEvent(@NotNull DefLoadReport report) {
        this.report = report;
    }

    public DefLoadReport report() {
        return report;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
