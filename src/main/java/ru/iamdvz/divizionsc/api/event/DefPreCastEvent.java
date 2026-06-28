package ru.iamdvz.divizionsc.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.TriggerType;

public final class DefPreCastEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final DefDefinition def;
    private final TriggerType trigger;
    private boolean cancelled;

    public DefPreCastEvent(@NotNull Player player, @NotNull DefDefinition def, @NotNull TriggerType trigger) {
        super(player);
        this.def = def;
        this.trigger = trigger;
    }

    public DefDefinition def() {
        return def;
    }

    public TriggerType trigger() {
        return trigger;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
