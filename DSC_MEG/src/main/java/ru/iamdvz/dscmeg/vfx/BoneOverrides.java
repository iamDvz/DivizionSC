package ru.iamdvz.dscmeg.vfx;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import ru.iamdvz.dscmeg.util.VfxUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BoneOverrides {

    private final Map<String, Color> tints = new HashMap<>();
    private final Map<String, Integer> blockLights = new HashMap<>();
    private final Map<String, Integer> skyLights = new HashMap<>();
    private final Map<String, Integer> glowColors = new HashMap<>();
    private final Map<String, Display.Billboard> billboards = new HashMap<>();
    private final Set<String> hiddenBones = new HashSet<>();
    private final Set<String> enchanted = new HashSet<>();

    private final boolean hasGlobal;
    private final int globalBlock;
    private final int globalSky;

    public BoneOverrides(ConfigurationSection section) {
        ConfigurationSection brightSection = section == null ? null : section.getConfigurationSection("brightness");
        if (brightSection != null) {
            hasGlobal = true;
            globalBlock = brightSection.getInt("block", 15);
            globalSky = brightSection.getInt("sky", 15);
        } else {
            hasGlobal = false;
            globalBlock = 15;
            globalSky = 15;
        }

        ConfigurationSection bonesSection = section == null ? null : section.getConfigurationSection("bones");
        if (bonesSection != null) {
            loadBones(bonesSection);
        }
    }

    public int blockLight() {
        return globalBlock;
    }

    public int skyLight() {
        return globalSky;
    }

    public void apply(ActiveModel model, Color baseColor, boolean glowing, int blockLight, int skyLight) {
        for (ModelBone bone : model.getBones().values()) {
            applyRecursive(bone, baseColor, glowing, blockLight, skyLight);
        }
    }

    private void applyRecursive(ModelBone bone, Color baseColor, boolean glowing, int blockLight, int skyLight) {
        String name = bone.getBoneId();

        bone.setDefaultTint(tints.getOrDefault(name, baseColor));

        Integer glow = glowColors.get(name);
        bone.setGlowing(glowing || glow != null);
        if (glow != null) {
            bone.setGlowColor(glow);
        }

        Display.Billboard bb = billboards.get(name);
        if (bb != null) {
            bone.setBillboard(bb);
        }

        if (hiddenBones.contains(name)) {
            bone.setVisible(false);
        }
        if (enchanted.contains(name)) {
            bone.setEnchanted(true);
        }

        int bl = blockLights.getOrDefault(name, hasGlobal ? blockLight : -1);
        int sl = skyLights.getOrDefault(name, hasGlobal ? skyLight : -1);
        if (bl >= 0) {
            bone.setBlockLight(bl);
        }
        if (sl >= 0) {
            bone.setSkyLight(sl);
        }

        for (ModelBone child : bone.getChildren().values()) {
            applyRecursive(child, baseColor, glowing, blockLight, skyLight);
        }
    }

    private void loadBones(ConfigurationSection bones) {
        for (String bone : bones.getKeys(false)) {
            ConfigurationSection b = bones.getConfigurationSection(bone);
            if (b == null) {
                continue;
            }

            if (b.contains("tint")) {
                Color c = VfxUtils.parseBukkitColor(b.getString("tint"));
                if (c != null) {
                    tints.put(bone, c);
                }
            }
            if (b.contains("glow")) {
                Integer rgb = VfxUtils.parseHexColor(b.getString("glow"));
                if (rgb != null) {
                    glowColors.put(bone, rgb);
                }
            }
            if (b.contains("billboard")) {
                Display.Billboard bb = VfxUtils.parseBillboard(b.getString("billboard"));
                if (bb != null) {
                    billboards.put(bone, bb);
                }
            }
            if (b.contains("block-light")) {
                blockLights.put(bone, b.getInt("block-light", 15));
            }
            if (b.contains("sky-light")) {
                skyLights.put(bone, b.getInt("sky-light", 15));
            }
            if (b.getBoolean("hidden", false)) {
                hiddenBones.add(bone);
            }
            if (b.getBoolean("enchanted", false)) {
                enchanted.add(bone);
            }
        }
    }
}
