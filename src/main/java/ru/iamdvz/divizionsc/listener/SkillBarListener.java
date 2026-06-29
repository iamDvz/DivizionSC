package ru.iamdvz.divizionsc.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.gui.AbilityBrowserMenu;
import ru.iamdvz.divizionsc.gui.GuiInputService;
import ru.iamdvz.divizionsc.gui.SkillBarMenu;

import java.util.Map;

public final class SkillBarListener implements Listener {

    private final PluginContext context;
    private final GuiInputService input = new GuiInputService();

    public SkillBarListener(PluginContext context) {
        this.context = context;
    }

    public void openSkillBar(Player player) {
        openSkillBar(player, "");
    }

    public void openSkillBar(Player player, String filter) {
        if (!context.config().skillBar().enabled()) {
            return;
        }
        if (!player.hasPermission(context.config().skillBar().permission())) {
            context.messages().send(player, "skillbar-no-permission");
            return;
        }
        SkillBarMenu menu = new SkillBarMenu(context, player, filter);
        player.openInventory(menu.getInventory());
    }

    public void openBrowser(Player player, String filter) {
        AbilityBrowserMenu menu = new AbilityBrowserMenu(context, player, filter);
        player.openInventory(menu.getInventory());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        Object holder = top.getHolder(false);
        if (holder instanceof SkillBarMenu menu) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player
                    && event.getClickedInventory() == top) {
                handleSkillBarClick(menu, player, event);
            }
            return;
        }
        if (holder instanceof AbilityBrowserMenu browser) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player
                    && event.getClickedInventory() == top) {
                handleBrowserClick(browser, player, event);
            }
        }
    }

    private void handleSkillBarClick(SkillBarMenu menu, Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        if (menu.isPrevSlot(slot)) {
            menu.prevPage();
            return;
        }
        if (menu.isNextSlot(slot)) {
            menu.nextPage();
            return;
        }
        if (menu.isSearchSlot(slot)) {
            if (event.isRightClick()) {
                menu.clearFilter();
                return;
            }
            input.requestSearch(player.getUniqueId());
            player.closeInventory();
            context.messages().send(player, "skillbar-search-prompt");
            return;
        }
        if (menu.isBrowserSlot(slot)) {
            openBrowser(player, menu.filter());
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

    private void handleBrowserClick(AbilityBrowserMenu browser, Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        if (browser.isPrevSlot(slot)) {
            browser.prevPage();
            return;
        }
        if (browser.isNextSlot(slot)) {
            browser.nextPage();
            return;
        }
        DefDefinition def = browser.defAt(slot);
        if (def != null) {
            player.closeInventory();
            context.defs().castFromCommand(player, def.id());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Object holder = event.getView().getTopInventory().getHolder(false);
        if (holder instanceof SkillBarMenu || holder instanceof AbilityBrowserMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!input.isAwaitingSearch(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        input.clear(player.getUniqueId());
        String query = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        String filter = query.equalsIgnoreCase("cancel") ? "" : query;
        context.scheduler().entity(player, () -> openSkillBar(player, filter));
    }

    public static boolean isEmptyCastHand(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (!main.isEmpty()) {
            return false;
        }
        return player.getInventory().getItemInOffHand().isEmpty();
    }
}
