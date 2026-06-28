package ru.iamdvz.divizionsc.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.bind.PlayerBinds;
import ru.iamdvz.divizionsc.config.SkillBarConfig;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public final class SkillBarMenu implements org.bukkit.inventory.InventoryHolder {

    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final PluginContext context;
    private final Player player;
    private final Inventory inventory;
    private final SkillBarConfig settings;
    private int page;
    private int activeBindSlot;

    public SkillBarMenu(PluginContext context, Player player) {
        this.context = context;
        this.player = player;
        this.settings = context.config().skillBar();
        this.page = 0;
        this.activeBindSlot = player.getInventory().getHeldItemSlot();
        Component title = context.messages().format("skillbar-title", java.util.Map.of("page", "1"));
        this.inventory = Bukkit.createInventory(this, settings.guiSize(), title);
        refresh();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player player() {
        return player;
    }

    public int page() {
        return page;
    }

    public int activeBindSlot() {
        return activeBindSlot;
    }

    public void selectActiveSlot(int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot >= settings.bindSlotCount()) {
            return;
        }
        this.activeBindSlot = hotbarSlot;
        refresh();
    }

    public void nextPage() {
        int maxPage = maxPage();
        if (page < maxPage) {
            page++;
            refresh();
        }
    }

    public void prevPage() {
        if (page > 0) {
            page--;
            refresh();
        }
    }

    public boolean isBindSlot(int slot) {
        int start = settings.bindRowStartSlot();
        int end = start + settings.bindSlotCount();
        return slot >= start && slot < end;
    }

    public int hotbarSlotFor(int slot) {
        return slot - settings.bindRowStartSlot();
    }

    public boolean isListSlot(int slot) {
        int start = settings.listStartSlot();
        return slot >= start && slot < start + settings.listPageSize();
    }

    public int listIndexFor(int slot) {
        return slot - settings.listStartSlot();
    }

    public boolean isPrevSlot(int slot) {
        return slot == PREV_SLOT;
    }

    public boolean isNextSlot(int slot) {
        return slot == NEXT_SLOT;
    }

    public DefDefinition defAtListSlot(int slot) {
        List<DefDefinition> defs = pageDefs();
        int index = listIndexFor(slot);
        if (index < 0 || index >= defs.size()) {
            return null;
        }
        return defs.get(index);
    }

    public int firstEmptyBindSlot() {
        PlayerBinds binds = context.binds().binds(player);
        for (int hotbar = 0; hotbar < settings.bindSlotCount(); hotbar++) {
            String defId = binds.get(hotbar);
            if (defId == null || defId.isBlank()) {
                return hotbar;
            }
        }
        return -1;
    }

    public void refresh() {
        inventory.clear();
        fillGlass();
        renderDefList();
        renderBindRow();
        renderControls();
    }

    private void fillGlass() {
        ItemStack glass = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = settings.listStartSlot() + settings.listPageSize(); i < settings.bindRowStartSlot(); i++) {
            if (i == PREV_SLOT || i == INFO_SLOT || i == NEXT_SLOT) {
                continue;
            }
            inventory.setItem(i, glass);
        }
    }

    private void renderDefList() {
        List<DefDefinition> defs = pageDefs();
        int start = settings.listStartSlot();
        int slotNumber = activeBindSlot + 1;
        for (int i = 0; i < settings.listPageSize(); i++) {
            int slot = start + i;
            if (i >= defs.size()) {
                inventory.setItem(slot, namedItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
                continue;
            }
            DefDefinition def = defs.get(i);
            ItemStack icon = context.castItems().create(def);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                lore.add(ColorUtil.component("&7"));
                lore.add(ColorUtil.component("&eЛКМ &7→ слот &f" + slotNumber));
                lore.add(ColorUtil.component("&eShift+ЛКМ &7→ первый свободный"));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
        }
    }

    private void renderBindRow() {
        PlayerBinds binds = context.binds().binds(player);
        for (int hotbar = 0; hotbar < settings.bindSlotCount(); hotbar++) {
            int slot = settings.bindRowStartSlot() + hotbar;
            boolean active = hotbar == activeBindSlot;
            String defId = binds.get(hotbar);
            if (defId == null) {
                inventory.setItem(slot, namedItem(
                        active ? Material.YELLOW_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE,
                        (active ? "&e▶ " : "&a") + "Слот " + (hotbar + 1),
                        List.of(
                                active ? "&eВыбран для привязки" : "&7ЛКМ — выбрать слот",
                                "&7Клавиша &f" + (hotbar + 1) + " &7— каст",
                                "&cShift+ЛКМ &7— очистить"
                        )
                ));
                continue;
            }
            var defOptional = context.defRegistry().find(defId);
            if (defOptional.isEmpty()) {
                inventory.setItem(slot, namedItem(
                        Material.BARRIER,
                        (active ? "&e▶ " : "&c") + "Слот " + (hotbar + 1),
                        List.of("&7Битый бинд: &e" + defId, "&cShift+ЛКМ &7— очистить")
                ));
                continue;
            }
            DefDefinition def = defOptional.get();
            ItemStack bound = context.castItems().create(def);
            ItemMeta meta = bound.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                lore.add(ColorUtil.component("&7"));
                if (active) {
                    lore.add(ColorUtil.component("&e▶ Выбран для замены"));
                } else {
                    lore.add(ColorUtil.component("&7ЛКМ — выбрать слот"));
                }
                lore.add(ColorUtil.component("&7Клавиша &f" + (hotbar + 1) + " &7— каст"));
                lore.add(ColorUtil.component("&cShift+ЛКМ &7— очистить"));
                meta.lore(lore);
                bound.setItemMeta(meta);
            }
            inventory.setItem(slot, bound);
        }
    }

    private void renderControls() {
        inventory.setItem(PREV_SLOT, namedItem(
                Material.ARROW,
                "&e← Назад",
                List.of("&7Страница " + (page + 1) + "/" + (maxPage() + 1))
        ));
        inventory.setItem(INFO_SLOT, namedItem(
                Material.BOOK,
                "&6Панель скиллов",
                List.of(
                        "&71. Выберите слот &a1-9 &7ниже",
                        "&72. Кликните скилл сверху",
                        "&7",
                        "&7В игре: &e1-9 &7каст слота",
                        "&7         &eПКМ &7каст текущего",
                        "&7Открыть: &e/dsc skills &7или &eF"
                )
        ));
        inventory.setItem(NEXT_SLOT, namedItem(
                Material.ARROW,
                "&eВперёд →",
                List.of("&7Страница " + (page + 1) + "/" + (maxPage() + 1))
        ));
    }

    private List<DefDefinition> pageDefs() {
        List<DefDefinition> all = context.binds().availableDefs(player);
        int from = page * settings.listPageSize();
        if (from >= all.size()) {
            return List.of();
        }
        int to = Math.min(from + settings.listPageSize(), all.size());
        return all.subList(from, to);
    }

    private int maxPage() {
        List<DefDefinition> all = context.binds().availableDefs(player);
        if (all.isEmpty()) {
            return 0;
        }
        return (all.size() - 1) / settings.listPageSize();
    }

    private ItemStack namedItem(Material material, String name) {
        return namedItem(material, name, List.of());
    }

    private ItemStack namedItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(ColorUtil.component(name));
        if (!loreLines.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(ColorUtil.component(line));
            }
            meta.lore(lore);
        }
        meta.setHideTooltip(false);
        item.setItemMeta(meta);
        return item;
    }
}
