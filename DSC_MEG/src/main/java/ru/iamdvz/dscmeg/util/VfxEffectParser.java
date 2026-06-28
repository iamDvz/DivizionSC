package ru.iamdvz.dscmeg.util;

import com.ticxo.modelengine.api.ModelEngineAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.dscmeg.config.DscMegConfig;
import ru.iamdvz.dscmeg.model.AnimationEntry;
import ru.iamdvz.dscmeg.model.ChangePartEntry;
import ru.iamdvz.dscmeg.model.VfxEffectConfig;
import ru.iamdvz.dscmeg.vfx.BoneOverrides;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VfxEffectParser {

    private VfxEffectParser() {
    }

    public static VfxEffectConfig parse(EffectDefinition effect, DscMegConfig pluginConfig) {
        DscMegConfig.DefaultsSettings defaults = pluginConfig.defaults();
        String modelName = effect.text("model-name", effect.text("model", ""));

        List<AnimationEntry> animations = loadAnimations(effect, modelName, pluginConfig);
        List<ChangePartEntry> changeParts = loadChangeParts(effect, modelName);
        ConfigurationSection boneSection = resolveBoneSection(effect);

        long removeDelay = effect.data().containsKey("remove-delay")
                ? effect.integer("remove-delay", (int) defaults.removeDelay())
                : defaults.removeDelay();

        return new VfxEffectConfig(
                modelName,
                effect.text("model-color", defaults.modelColor()),
                effect.data().containsKey("glowing") ? effect.bool("glowing", false) : defaults.glowing(),
                effect.data().containsKey("model-scale") ? effect.number("model-scale", defaults.modelScale()) : defaults.modelScale(),
                effect.bool("lock-pitch", false),
                effect.bool("lock-yaw", false),
                effect.bool("lock-rotation", false),
                effect.bool("use-state-machine", false),
                effect.text("animation-state", null),
                effect.number("animation-state-speed", 1.0),
                effect.text("glow-color", null),
                effect.text("billboard", null),
                effect.bool("on-fire", false),
                effect.data().containsKey("shadow") ? effect.bool("shadow", true) : defaults.shadow(),
                effect.data().containsKey("remove-on-host-death")
                        ? effect.bool("remove-on-host-death", false)
                        : defaults.removeOnHostDeath(),
                effect.number("yaw-offset", 0.0),
                effect.number("pitch-offset", 0.0),
                effect.bool("follow-target", false),
                effect.bool("orient-yaw-when-teleport", false),
                effect.bool("mount-target", false),
                effect.text("mount-bone", "mount"),
                effect.text("mount-controller", "walking"),
                removeDelay,
                new BoneOverrides(boneSection),
                animations,
                changeParts
        );
    }

    private static ConfigurationSection resolveBoneSection(EffectDefinition effect) {
        Map<String, Object> overrides = effect.map("bone-overrides");
        if (!overrides.isEmpty()) {
            return toSection(overrides);
        }
        MemoryConfiguration section = new MemoryConfiguration();
        if (effect.data().containsKey("brightness")) {
            section.set("brightness", effect.data().get("brightness"));
        }
        if (effect.data().containsKey("bones")) {
            section.set("bones", effect.data().get("bones"));
        }
        return section;
    }

    private static List<AnimationEntry> loadAnimations(
            EffectDefinition effect,
            String modelName,
            DscMegConfig pluginConfig) {
        List<AnimationEntry> entries = new ArrayList<>();
        Object raw = effect.data().get("animations");
        if (!(raw instanceof List<?> animList) || animList.isEmpty()) {
            return entries;
        }

        MemoryConfiguration root = new MemoryConfiguration();
        for (int idx = 0; idx < animList.size(); idx++) {
            Object item = animList.get(idx);
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            try {
                ConfigurationSection entry = VfxUtils.mapToSection(root, rawMap);
                String rawName = entry.getString("name");
                if (rawName == null || rawName.isBlank()) {
                    Bukkit.getLogger().warning("[DSC_MEG] Animation entry #" + (idx + 1) + " is missing 'name'");
                    continue;
                }
                validateAnimation(modelName, rawName, pluginConfig);
                long defaultDuration = VfxUtils.blueprintDuration(modelName, rawName);
                long duration = entry.contains("duration")
                        ? entry.getLong("duration", defaultDuration)
                        : defaultDuration;
                entries.add(new AnimationEntry(
                        rawName,
                        entry.getInt("delay", 0),
                        duration,
                        entry.getDouble("lerp-in", 0.1),
                        entry.getDouble("lerp-out", 0.1),
                        entry.getDouble("speed", 1.0),
                        entry.getBoolean("overlap", false),
                        entry.getBoolean("loop", false)
                ));
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[DSC_MEG] Failed to parse animation entry #" + (idx + 1) + ": " + t.getMessage());
            }
        }
        return entries;
    }

    private static List<ChangePartEntry> loadChangeParts(EffectDefinition effect, String defaultModelId) {
        List<ChangePartEntry> entries = new ArrayList<>();
        Object raw = effect.data().get("change-parts");
        if (!(raw instanceof List<?> partList)) {
            return entries;
        }

        MemoryConfiguration root = new MemoryConfiguration();
        for (Object item : partList) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            ConfigurationSection entry = VfxUtils.mapToSection(root, rawMap);
            String modelId = entry.getString("model-id", defaultModelId);
            String newModelId = entry.getString("new-model-id", "");
            int delay = entry.getInt("delay", 0);
            List<String> parts = entry.getStringList("parts");
            List<String> newParts = entry.contains("new-parts") ? entry.getStringList("new-parts") : List.copyOf(parts);
            if (parts.isEmpty() || newModelId.isBlank()) {
                continue;
            }
            entries.add(new ChangePartEntry(modelId, parts, newModelId, newParts, delay));
        }
        return entries;
    }

    private static void validateAnimation(String modelName, String animName, DscMegConfig pluginConfig) {
        if (!pluginConfig.debug().validateAnimations() || modelName == null || modelName.isBlank()) {
            return;
        }
        var blueprint = ModelEngineAPI.getBlueprint(modelName);
        if (blueprint == null) {
            Bukkit.getLogger().warning("[DSC_MEG] ModelEngine blueprint '" + modelName + "' is not loaded");
            return;
        }
        if (!blueprint.getAnimations().containsKey(animName)) {
            Bukkit.getLogger().warning("[DSC_MEG] Animation '" + animName + "' is not registered for '" + modelName + "'");
        }
    }

    private static ConfigurationSection toSection(Map<String, Object> map) {
        MemoryConfiguration config = new MemoryConfiguration();
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
        }
        return config;
    }
}
