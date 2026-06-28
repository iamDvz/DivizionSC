package ru.iamdvz.dscmeg.model;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import ru.iamdvz.dscmeg.vfx.BoneOverrides;

import java.util.List;

public record VfxSpawnParams(
        String resolvedModel,
        Location spawnLoc,
        float spawnYaw,
        float spawnPitch,
        Entity followEntity,
        boolean orientFollowYaw,
        Entity mountEntity,
        boolean doMount,
        String mountBone,
        String mountController,
        boolean useStateMachine,
        String animationState,
        double animationStateSpeed,
        double modelScale,
        Integer glowRgb,
        Display.Billboard billboard,
        boolean onFire,
        Color baseColor,
        boolean isGlowing,
        int blockLight,
        int skyLight,
        boolean lockPitch,
        boolean lockYaw,
        boolean lockRotation,
        boolean shadow,
        boolean removeOnHostDeath,
        List<AnimationEntry> animationEntries,
        List<ChangePartEntry> changePartEntries,
        BoneOverrides boneOverrides,
        long removeDelay
) {
}
