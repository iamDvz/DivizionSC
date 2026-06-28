package ru.iamdvz.divizionsc.def.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.model.CastItemSpec;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CastItemFactory {

    private final NamespacedKey defKey;
    private final NamespacedKey legacyAbilityKey;

    public CastItemFactory(JavaPlugin plugin, PluginConfig config) {
        this.defKey = new NamespacedKey(plugin, "def_id");
        this.legacyAbilityKey = new NamespacedKey(plugin, "ability_id");
    }

    public void updateConfig(PluginConfig newConfig) {
    }

    public ItemStack create(DefDefinition def) {
        CastItemSpec spec = def.castItem();
        Material material = spec == null ? Material.BLAZE_ROD : spec.material();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String displayName = spec != null && spec.name() != null ? spec.name() : def.name();
        meta.displayName(ColorUtil.component(displayName));

        List<Component> lore = new ArrayList<>();
        if (spec != null && spec.lore() != null) {
            for (String line : spec.lore()) {
                lore.add(ColorUtil.component(line));
            }
        } else if (!def.description().isBlank()) {
            lore.add(ColorUtil.component("&7" + def.description()));
        }
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }

        if (spec != null && spec.customModelData() > 0) {
            meta.setCustomModelData(spec.customModelData());
        }

        meta.getPersistentDataContainer().set(defKey, PersistentDataType.STRING, def.id());
        item.setItemMeta(meta);
        return item;
    }

    public Optional<String> readDefId(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String defId = pdc.get(defKey, PersistentDataType.STRING);
        if (defId != null) {
            return Optional.of(defId);
        }
        return Optional.ofNullable(pdc.get(legacyAbilityKey, PersistentDataType.STRING));
    }
}
