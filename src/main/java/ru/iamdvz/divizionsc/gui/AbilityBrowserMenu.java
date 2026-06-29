package ru.iamdvz.divizionsc.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.DefDefinition;

public final class AbilityBrowserMenu implements InventoryHolder {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final PluginContext context;
    private final Player player;
    private final Inventory inventory;
    private final String filter;
    private int page;

    public AbilityBrowserMenu(PluginContext context, Player player, String filter) {
        this.context = context;
        this.player = player;
        this.filter = filter == null ? "" : filter;
        this.page = 0;
        this.inventory = Bukkit.createInventory(this, SIZE, msg("browser-title"));
        refresh();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public boolean isPrevSlot(int slot) {
        return slot == PREV_SLOT;
    }

    public boolean isNextSlot(int slot) {
        return slot == NEXT_SLOT;
    }

    public DefDefinition defAt(int slot) {
        if (slot < 0 || slot >= PAGE_SIZE) {
            return null;
        }
        List<DefDefinition> defs = pageDefs();
        return slot < defs.size() ? defs.get(slot) : null;
    }

    public void nextPage() {
        if (page < maxPage()) {
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

    public void refresh() {
        inventory.clear();
        List<DefDefinition> defs = pageDefs();
        for (int i = 0; i < defs.size(); i++) {
            inventory.setItem(i, describe(defs.get(i)));
        }
        Map<String, String> pagePh = pagePlaceholders();
        inventory.setItem(PREV_SLOT, named(Material.ARROW, "browser-prev",
                List.of(msg("browser-page-lore", pagePh))));
        inventory.setItem(INFO_SLOT, named(Material.ENCHANTED_BOOK, "browser-info-title",
                List.of(msg("browser-info-lore-1"), msg("browser-info-lore-2"))));
        inventory.setItem(NEXT_SLOT, named(Material.ARROW, "browser-next",
                List.of(msg("browser-page-lore", pagePh))));
    }

    private ItemStack describe(DefDefinition def) {
        ItemStack icon = context.castItems().create(def);
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(msg("browser-def-id", Map.of("id", def.id())));
        lore.add(msg("browser-def-trigger", Map.of("trigger", def.trigger().name())));
        lore.add(msg("browser-def-target", Map.of("target", def.targetMode().name())));
        if (def.range() > 0) {
            lore.add(msg("browser-def-range", Map.of("range", String.valueOf(def.range()))));
        }
        if (def.cooldown() > 0) {
            lore.add(msg("browser-def-cd", Map.of("cooldown", String.valueOf(def.cooldown()))));
        }
        lore.add(msg("browser-lmb-cast"));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private List<DefDefinition> allDefs() {
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
        List<DefDefinition> all = allDefs();
        int from = page * PAGE_SIZE;
        if (from >= all.size()) {
            return List.of();
        }
        return all.subList(from, Math.min(from + PAGE_SIZE, all.size()));
    }

    private int maxPage() {
        List<DefDefinition> all = allDefs();
        return all.isEmpty() ? 0 : (all.size() - 1) / PAGE_SIZE;
    }

    private Map<String, String> pagePlaceholders() {
        return Map.of(
                "page", String.valueOf(page + 1),
                "pages", String.valueOf(maxPage() + 1)
        );
    }

    private ItemStack named(Material material, String messageKey, List<Component> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(msg(messageKey));
        meta.lore(loreLines);
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
