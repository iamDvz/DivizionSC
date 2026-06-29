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
import java.util.Locale;
import java.util.Map;

public final class SkillBarMenu implements org.bukkit.inventory.InventoryHolder {

    private static final int PREV_SLOT = 45;
    private static final int SEARCH_SLOT = 47;
    private static final int INFO_SLOT = 49;
    private static final int BROWSER_SLOT = 51;
    private static final int NEXT_SLOT = 53;

    private final PluginContext context;
    private final Player player;
    private final Inventory inventory;
    private final SkillBarConfig settings;
    private int page;
    private int activeBindSlot;
    private String filter;

    public SkillBarMenu(PluginContext context, Player player) {
        this(context, player, "");
    }

    public SkillBarMenu(PluginContext context, Player player, String filter) {
        this.context = context;
        this.player = player;
        this.settings = context.config().skillBar();
        this.page = 0;
        this.filter = filter == null ? "" : filter;
        this.activeBindSlot = player.getInventory().getHeldItemSlot();
        Component title = context.messages().format("skillbar-title", Map.of());
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

    public boolean isSearchSlot(int slot) {
        return slot == SEARCH_SLOT;
    }

    public boolean isBrowserSlot(int slot) {
        return slot == BROWSER_SLOT;
    }

    public String filter() {
        return filter;
    }

    public void clearFilter() {
        this.filter = "";
        this.page = 0;
        refresh();
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
        ItemStack glass = fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = settings.listStartSlot() + settings.listPageSize(); i < settings.bindRowStartSlot(); i++) {
            if (i == PREV_SLOT || i == INFO_SLOT || i == NEXT_SLOT
                    || i == SEARCH_SLOT || i == BROWSER_SLOT) {
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
                inventory.setItem(slot, fillerPane(Material.LIGHT_GRAY_STAINED_GLASS_PANE));
                continue;
            }
            DefDefinition def = defs.get(i);
            ItemStack icon = context.castItems().create(def);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                lore.add(Component.empty());
                lore.add(msg("skillbar-lmb-slot", Map.of("slot", String.valueOf(slotNumber))));
                lore.add(msg("skillbar-shift-lmb-free"));
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
                        "skillbar-slot-title",
                        Map.of("prefix", active ? "&e▶ " : "&a", "slot", String.valueOf(hotbar + 1)),
                        List.of(
                                active ? msg("skillbar-slot-empty-active") : msg("skillbar-slot-empty-hint"),
                                msg("skillbar-slot-key", Map.of("key", String.valueOf(hotbar + 1))),
                                msg("skillbar-shift-clear")
                        )
                ));
                continue;
            }
            var defOptional = context.defRegistry().find(defId);
            if (defOptional.isEmpty()) {
                inventory.setItem(slot, namedItem(
                        Material.BARRIER,
                        "skillbar-slot-title",
                        Map.of("prefix", active ? "&e▶ " : "&c", "slot", String.valueOf(hotbar + 1)),
                        List.of(
                                msg("skillbar-slot-broken", Map.of("def", defId)),
                                msg("skillbar-shift-clear")
                        )
                ));
                continue;
            }
            DefDefinition def = defOptional.get();
            ItemStack bound = context.castItems().create(def);
            ItemMeta meta = bound.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                lore.add(Component.empty());
                if (active) {
                    lore.add(msg("skillbar-slot-bound-active"));
                } else {
                    lore.add(msg("skillbar-slot-empty-hint"));
                }
                lore.add(msg("skillbar-slot-key", Map.of("key", String.valueOf(hotbar + 1))));
                lore.add(msg("skillbar-shift-clear"));
                meta.lore(lore);
                bound.setItemMeta(meta);
            }
            inventory.setItem(slot, bound);
        }
    }

    private void renderControls() {
        Map<String, String> pagePh = Map.of(
                "page", String.valueOf(page + 1),
                "pages", String.valueOf(maxPage() + 1)
        );
        inventory.setItem(PREV_SLOT, namedItem(Material.ARROW, "skillbar-prev",
                List.of(msg("skillbar-page-lore", pagePh))));
        boolean hasFilter = filter != null && !filter.isBlank();
        inventory.setItem(SEARCH_SLOT, namedItem(
                hasFilter ? Material.NAME_TAG : Material.OXEYE_DAISY,
                hasFilter ? "skillbar-search-filter" : "skillbar-search",
                hasFilter ? Map.of("filter", filter) : Map.of(),
                hasFilter
                        ? List.of(msg("skillbar-search-change"), msg("skillbar-search-reset"))
                        : List.of(msg("skillbar-search-hint"))
        ));
        inventory.setItem(BROWSER_SLOT, namedItem(Material.ENCHANTED_BOOK, "skillbar-browser-btn",
                List.of(msg("skillbar-browser-lore-1"), msg("skillbar-browser-lore-2"))));
        inventory.setItem(INFO_SLOT, namedItem(Material.BOOK, "skillbar-help-title",
                List.of(
                        msg("skillbar-help-1"),
                        msg("skillbar-help-2"),
                        Component.empty(),
                        ColorUtil.component("&7В игре: &e1-9 &7каст слота"),
                        ColorUtil.component("&7         &eПКМ &7каст текущего"),
                        ColorUtil.component("&7Открыть: &e/dsc skills &7или &eF")
                )));
        inventory.setItem(NEXT_SLOT, namedItem(Material.ARROW, "skillbar-next",
                List.of(msg("skillbar-page-lore", pagePh))));
    }

    private List<DefDefinition> filteredDefs() {
        List<DefDefinition> all = context.binds().availableDefs(player);
        if (filter == null || filter.isBlank()) {
            return all;
        }
        String needle = filter.toLowerCase(Locale.ROOT);
        List<DefDefinition> matches = new ArrayList<>();
        for (DefDefinition def : all) {
            if (def.id().toLowerCase(Locale.ROOT).contains(needle)
                    || def.name().toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(def);
            }
        }
        return matches;
    }

    private List<DefDefinition> pageDefs() {
        List<DefDefinition> all = filteredDefs();
        int from = page * settings.listPageSize();
        if (from >= all.size()) {
            return List.of();
        }
        int to = Math.min(from + settings.listPageSize(), all.size());
        return all.subList(from, to);
    }

    private int maxPage() {
        List<DefDefinition> all = filteredDefs();
        if (all.isEmpty()) {
            return 0;
        }
        return (all.size() - 1) / settings.listPageSize();
    }

    private ItemStack fillerPane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack namedItem(Material material, String messageKey, List<Component> loreLines) {
        return namedItem(material, messageKey, Map.of(), loreLines);
    }

    private ItemStack namedItem(Material material, String messageKey, Map<String, String> titlePh, List<Component> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(msg(messageKey, titlePh));
        if (!loreLines.isEmpty()) {
            meta.lore(loreLines);
        }
        meta.setHideTooltip(false);
        item.setItemMeta(meta);
        return item;
    }

    private Component msg(String key) {
        return context.messages().format(key, Map.of());
    }

    private Component msg(String key, Map<String, String> placeholders) {
        return context.messages().format(key, placeholders);
    }
}
