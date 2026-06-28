package ru.iamdvz.dscmeg.model;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import ru.iamdvz.dscmeg.vfx.BoneOverrides;

import java.util.List;

public record VfxEffectConfig(
        String modelName,
        String modelColor,
        boolean glowing,
        double modelScale,
        boolean lockPitch,
        boolean lockYaw,
        boolean lockRotation,
        boolean useStateMachine,
        String animationState,
        double animationStateSpeed,
        String glowColor,
        String billboard,
        boolean onFire,
        boolean shadow,
        boolean removeOnHostDeath,
        double yawOffset,
        double pitchOffset,
        boolean followTarget,
        boolean orientYaw,
        boolean mountTarget,
        String mountBone,
        String mountController,
        long removeDelay,
        BoneOverrides boneOverrides,
        List<AnimationEntry> animationEntries,
        List<ChangePartEntry> changePartEntries
) {
}
