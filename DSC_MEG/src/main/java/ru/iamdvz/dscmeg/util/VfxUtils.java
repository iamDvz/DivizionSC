package ru.iamdvz.dscmeg.util;

import com.ticxo.modelengine.api.ModelEngineAPI;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.inventory.EquipmentSlot;
import ru.iamdvz.dscmeg.config.DscMegConfig;

import java.util.Map;

public final class VfxUtils {

    private VfxUtils() {
    }

    public static long blueprintDuration(String modelName, String animName) {
        if (modelName == null || animName == null) {
            return 20L;
        }
        var bp = ModelEngineAPI.getBlueprint(modelName);
        if (bp == null) {
            return 20L;
        }
        var anim = bp.getAnimations().get(animName);
        return anim != null ? Math.round(anim.getLength() * 20) : 20L;
    }

    public static void configureStand(ArmorStand stand, DscMegConfig.CleanupSettings cleanup) {
        stand.setSilent(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setVisible(false);
        stand.addDisabledSlots(
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET,
                EquipmentSlot.HAND, EquipmentSlot.LEGS, EquipmentSlot.OFF_HAND);
        stand.addScoreboardTag(cleanup.mobTag());
    }

    public static ConfigurationSection mapToSection(ConfigurationSection parent, Map<?, ?> raw) {
        String key = "__tmp_" + System.nanoTime();
        ConfigurationSection section = parent.createSection(key);
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            section.set(String.valueOf(entry.getKey()), entry.getValue());
        }
        parent.set(key, null);
        return section;
    }

    public static Integer parseHexColor(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim().replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Color parseBukkitColor(String hex) {
        Integer rgb = parseHexColor(hex);
        if (rgb == null) {
            return Color.WHITE;
        }
        return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public static Display.Billboard parseBillboard(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Display.Billboard.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
