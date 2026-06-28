package ru.iamdvz.divizionsc.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.gui.SkillBarMenu;

import java.util.Map;

public final class SkillBarListener implements Listener {

    private final PluginContext context;

    public SkillBarListener(PluginContext context) {
        this.context = context;
    }

    public void openSkillBar(Player player) {
        if (!context.config().skillBar().enabled()) {
            return;
        }
        if (!player.hasPermission(context.config().skillBar().permission())) {
            context.messages().send(player, "skillbar-no-permission");
            return;
        }
        SkillBarMenu menu = new SkillBarMenu(context, player);
        player.openInventory(menu.getInventory());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof SkillBarMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory() != top) {
            return;
        }

        int slot = event.getSlot();
        if (menu.isPrevSlot(slot)) {
            menu.prevPage();
            return;
        }
        if (menu.isNextSlot(slot)) {
            menu.nextPage();
            return;
        }

        if (menu.isBindSlot(slot)) {
            int hotbar = menu.hotbarSlotFor(slot);
            if (event.isShiftClick()) {
                context.binds().unbind(player, hotbar);
                player.sendMessage(context.messages().format(
                        "skillbar-unbound",
                        Map.of("slot", String.valueOf(hotbar + 1))
                ));
                menu.refresh();
                return;
            }
            menu.selectActiveSlot(hotbar);
            player.sendMessage(context.messages().format(
                    "skillbar-slot-selected",
                    Map.of("slot", String.valueOf(hotbar + 1))
            ));
            return;
        }

        if (menu.isListSlot(slot)) {
            DefDefinition def = menu.defAtListSlot(slot);
            if (def == null) {
                return;
            }
            int targetSlot = event.isShiftClick() ? menu.firstEmptyBindSlot() : menu.activeBindSlot();
            if (targetSlot < 0) {
                context.messages().send(player, "skillbar-no-empty-slot");
                return;
            }
            context.binds().bind(player, targetSlot, def.id());
            menu.selectActiveSlot(targetSlot);
            player.sendMessage(context.messages().format(
                    "skillbar-bound",
                    Map.of("def", def.name(), "slot", String.valueOf(targetSlot + 1))
            ));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof SkillBarMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        context.binds().unload(event.getPlayer().getUniqueId());
    }

    public static boolean isEmptyCastHand(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (!main.isEmpty()) {
            return false;
        }
        return player.getInventory().getItemInOffHand().isEmpty();
    }
}
