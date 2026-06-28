package ru.iamdvz.divizionsc.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.bind.BindCastHelper;
import ru.iamdvz.divizionsc.def.model.TriggerType;
import ru.iamdvz.divizionsc.def.service.DefService;
import ru.iamdvz.divizionsc.input.PlayerInputTracker;

import java.util.Set;

public final class KeyInputListener implements Listener {

    private final PluginContext context;
    private final PlayerInputTracker inputTracker;

    public KeyInputListener(PluginContext context) {
        this.context = context;
        this.inputTracker = new PlayerInputTracker();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        Set<TriggerType> pressed = inputTracker.detectPressed(player, event.getInput());
        if (pressed.isEmpty()) {
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        for (TriggerType trigger : pressed) {
            if (tryCastFromHands(player, main, off, trigger)) {
                return;
            }
            if (BindCastHelper.tryCastCurrentSlot(context, player, trigger)) {
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        if (tryCastFromHands(player, main, player.getInventory().getItemInOffHand(), TriggerType.DROP)) {
            event.setCancelled(true);
            return;
        }
        if (BindCastHelper.tryCastCurrentSlot(context, player, TriggerType.DROP)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        if (tryCastFromHands(player, main, off, TriggerType.SWAP_HANDS)) {
            event.setCancelled(true);
            return;
        }
        if (BindCastHelper.tryCastCurrentSlot(context, player, TriggerType.SWAP_HANDS)) {
            event.setCancelled(true);
            return;
        }

        if (context.config().skillBar().enabled()
                && context.config().skillBar().openOnSwapHands()
                && player.hasPermission(context.config().skillBar().permission())) {
            event.setCancelled(true);
            context.skillBarListener().openSkillBar(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHotbarSelect(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int slot = event.getNewSlot();
        TriggerType keyTrigger = TriggerType.fromHotbarSlot(slot);
        ItemStack item = player.getInventory().getItem(slot);

        if (tryCastFromItem(player, item, keyTrigger)) {
            return;
        }
        if (tryCastFromItem(player, item, TriggerType.HOTBAR)) {
            return;
        }
        if (BindCastHelper.tryCastBoundSlot(context, player, slot, keyTrigger)) {
            return;
        }
        BindCastHelper.tryCastBoundSlot(context, player, slot, TriggerType.HOTBAR);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inputTracker.clear(event.getPlayer());
        context.cooldowns().removePlayer(event.getPlayer().getUniqueId());
    }

    private boolean tryCastFromHands(Player player, ItemStack main, ItemStack off, TriggerType trigger) {
        if (tryCastFromItem(player, main, trigger)) {
            return true;
        }
        return tryCastFromItem(player, off, trigger);
    }

    private boolean tryCastFromItem(Player player, ItemStack item, TriggerType trigger) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        return context.defs().castFromItem(player, item, trigger) == DefService.CastResult.SUCCESS;
    }
}
