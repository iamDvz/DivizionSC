package ru.iamdvz.divizionsc.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.iamdvz.divizionsc.def.effect.StunService;

public final class StunListener implements Listener {

    private final StunService stunService;

    public StunListener(StunService stunService) {
        this.stunService = stunService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!stunService.isStunned(player)) {
            return;
        }
        Location anchor = stunService.anchor(player);
        Location to = event.getTo();
        if (anchor == null || to == null) {
            return;
        }
        if (to.getX() != event.getFrom().getX()
                || to.getY() != event.getFrom().getY()
                || to.getZ() != event.getFrom().getZ()) {
            to.setX(anchor.getX());
            to.setY(anchor.getY());
            to.setZ(anchor.getZ());
            event.setTo(to);
        }
    }
}
