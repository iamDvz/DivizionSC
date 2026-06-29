package ru.iamdvz.divizionsc.listener;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.bind.BindCastHelper;
import ru.iamdvz.divizionsc.def.model.TriggerType;
import ru.iamdvz.divizionsc.def.service.DefService;

/** Ввод игрока (клавиши, хотбар) + preload/unload данных из БД. */
public final class KeyInputListener implements Listener {

    private final PluginContext context;
    private final Map<UUID, InputSnapshot> previous = new ConcurrentHashMap<>();

    public KeyInputListener(PluginContext context) {
        this.context = context;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        context.mana().initPlayer(playerId);
        context.preloadPlayer(playerId);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        previous.remove(playerId);
        context.binds().unload(playerId);
        context.passives().unload(event.getPlayer());
        context.cooldowns().removePlayer(playerId);
        context.mana().removePlayer(playerId);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        Set<TriggerType> pressed = detectPressed(player, event.getInput());
        if (pressed.isEmpty()) {
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        for (TriggerType trigger : pressed) {
            if (tryCastFromHands(player, main, off, trigger, false)) {
                return;
            }
            if (BindCastHelper.tryCastCurrentSlot(context, player, trigger)) {
                return;
            }
        }
        for (TriggerType trigger : pressed) {
            context.passives().handleKeyPress(player, trigger);
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
            return;
        }
        context.passives().handleKeyPress(player, TriggerType.DROP);
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
            return;
        }
        context.passives().handleKeyPress(player, TriggerType.SWAP_HANDS);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHotbarSelect(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int slot = event.getNewSlot();
        TriggerType keyTrigger = TriggerType.fromHotbarSlot(slot);
        ItemStack item = player.getInventory().getItem(slot);

        if (tryCastFromItem(player, item, keyTrigger, false)) {
            event.setCancelled(true);
            return;
        }
        if (tryCastFromItem(player, item, TriggerType.HOTBAR, false)) {
            event.setCancelled(true);
            return;
        }
        if (BindCastHelper.tryCastBoundSlot(context, player, slot, keyTrigger)) {
            event.setCancelled(true);
            return;
        }
        if (BindCastHelper.tryCastBoundSlot(context, player, slot, TriggerType.HOTBAR)) {
            event.setCancelled(true);
            return;
        }
        context.passives().handleKeyPress(player, keyTrigger);
    }

    private Set<TriggerType> detectPressed(Player player, Input current) {
        InputSnapshot now = InputSnapshot.from(current);
        InputSnapshot before = previous.getOrDefault(player.getUniqueId(), InputSnapshot.empty());
        previous.put(player.getUniqueId(), now);
        return risingEdges(before, now);
    }

    private Set<TriggerType> risingEdges(InputSnapshot before, InputSnapshot now) {
        Set<TriggerType> pressed = EnumSet.noneOf(TriggerType.class);
        if (!before.forward() && now.forward()) {
            pressed.add(TriggerType.FORWARD);
        }
        if (!before.backward() && now.backward()) {
            pressed.add(TriggerType.BACKWARD);
        }
        if (!before.left() && now.left()) {
            pressed.add(TriggerType.LEFT);
        }
        if (!before.right() && now.right()) {
            pressed.add(TriggerType.RIGHT);
        }
        if (!before.jump() && now.jump()) {
            pressed.add(TriggerType.JUMP);
        }
        if (!before.sneak() && now.sneak()) {
            pressed.add(TriggerType.SNEAK);
        }
        if (!before.sprint() && now.sprint()) {
            pressed.add(TriggerType.SPRINT);
        }
        return pressed;
    }

    private boolean tryCastFromHands(Player player, ItemStack main, ItemStack off, TriggerType trigger) {
        return tryCastFromHands(player, main, off, trigger, true);
    }

    private boolean tryCastFromHands(Player player, ItemStack main, ItemStack off, TriggerType trigger, boolean notify) {
        if (tryCastFromItem(player, main, trigger, notify)) {
            return true;
        }
        return tryCastFromItem(player, off, trigger, notify);
    }

    private boolean tryCastFromItem(Player player, ItemStack item, TriggerType trigger, boolean notify) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        return context.defs().castFromItem(player, item, trigger, notify) == DefService.CastResult.SUCCESS;
    }

    private record InputSnapshot(
            boolean forward,
            boolean backward,
            boolean left,
            boolean right,
            boolean jump,
            boolean sneak,
            boolean sprint
    ) {
        static InputSnapshot from(Input input) {
            return new InputSnapshot(
                    input.isForward(),
                    input.isBackward(),
                    input.isLeft(),
                    input.isRight(),
                    input.isJump(),
                    input.isSneak(),
                    input.isSprint()
            );
        }

        static InputSnapshot empty() {
            return new InputSnapshot(false, false, false, false, false, false, false);
        }
    }
}
