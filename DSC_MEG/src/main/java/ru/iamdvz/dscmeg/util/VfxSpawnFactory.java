package ru.iamdvz.dscmeg.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import ru.iamdvz.dscmeg.model.VfxEffectConfig;
import ru.iamdvz.dscmeg.model.VfxSpawnParams;

public final class VfxSpawnFactory {

    private VfxSpawnFactory() {
    }

    public static VfxSpawnParams fromEffect(
            VfxEffectConfig config,
            Location location,
            Entity targetEntity,
            boolean forceFollow) {
        if (config == null || config.modelName() == null || config.modelName().isBlank()) {
            return null;
        }

        Location spawnLoc = location.clone();
        spawnLoc.setYaw(spawnLoc.getYaw() + (float) config.yawOffset());
        float pitch = spawnLoc.getPitch() + (float) config.pitchOffset();
        spawnLoc.setPitch(Math.max(-90f, Math.min(90f, pitch)));

        boolean doFollow = targetEntity != null && (config.followTarget() || forceFollow);
        boolean doMount = targetEntity != null && config.mountTarget();

        return build(
                config,
                spawnLoc,
                spawnLoc.getYaw(),
                spawnLoc.getPitch(),
                doFollow ? targetEntity : null,
                doMount ? targetEntity : null,
                doMount
        );
    }

    private static VfxSpawnParams build(
            VfxEffectConfig config,
            Location spawnLoc,
            float spawnYaw,
            float spawnPitch,
            Entity followEntity,
            Entity mountEntity,
            boolean doMount) {
        BoneOverridesRef overrides = config.boneOverrides() == null
                ? new BoneOverridesRef(15, 15)
                : new BoneOverridesRef(config.boneOverrides().blockLight(), config.boneOverrides().skyLight());

        return new VfxSpawnParams(
                config.modelName(),
                spawnLoc,
                spawnYaw,
                spawnPitch,
                followEntity,
                config.orientYaw(),
                mountEntity,
                doMount,
                config.mountBone(),
                config.mountController(),
                config.useStateMachine(),
                config.animationState(),
                config.animationStateSpeed(),
                config.modelScale(),
                VfxUtils.parseHexColor(config.glowColor()),
                VfxUtils.parseBillboard(config.billboard()),
                config.onFire(),
                VfxUtils.parseBukkitColor(config.modelColor()),
                config.glowing(),
                overrides.blockLight,
                overrides.skyLight,
                config.lockPitch(),
                config.lockYaw(),
                config.lockRotation(),
                config.shadow(),
                config.removeOnHostDeath(),
                config.animationEntries(),
                config.changePartEntries(),
                config.boneOverrides(),
                config.removeDelay()
        );
    }

    private record BoneOverridesRef(int blockLight, int skyLight) {
    }
}
