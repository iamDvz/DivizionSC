package ru.iamdvz.dscmeg.service;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.mount.controller.MountControllerSupplier;
import com.ticxo.modelengine.api.mount.controller.MountControllerTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.dscmeg.MegContext;
import ru.iamdvz.dscmeg.config.DscMegConfig;
import ru.iamdvz.dscmeg.model.VfxSession;
import ru.iamdvz.dscmeg.model.VfxSpawnParams;
import ru.iamdvz.dscmeg.util.StateMachineSupport;
import ru.iamdvz.dscmeg.util.VfxUtils;
import ru.iamdvz.dscmeg.vfx.BoneOverrides;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class VfxSpawnService {

    private static final long BONE_SETUP_DELAY_TICKS = 1L;

    private final JavaPlugin plugin;
    private final MegContext context;
    private final VfxScheduler scheduler;
    private final Logger log;

    public VfxSpawnService(JavaPlugin plugin, MegContext context) {
        this.plugin = plugin;
        this.context = context;
        this.scheduler = new VfxScheduler(plugin);
        this.log = plugin.getLogger();
    }

    public Runnable spawn(VfxSpawnParams params, ArmorStand[] standOut) {
        if (params.resolvedModel() == null || ModelEngineAPI.getBlueprint(params.resolvedModel()) == null) {
            logFailure("blueprint not found: " + params.resolvedModel());
            return null;
        }

        var spawnCheck = context.vfxTracker().canSpawn(params.spawnLoc().getWorld());
        if (!spawnCheck.permitted()) {
            if ("world-limit".equals(spawnCheck.reason()) && context.config().limits().notifyOnWorldLimit()) {
                log.warning("world VFX limit reached in " + params.spawnLoc().getWorld().getName());
            }
            return null;
        }

        DscMegConfig.CleanupSettings cleanupSettings = context.config().cleanup();
        ArmorStand stand = params.spawnLoc().getWorld().spawn(
                params.spawnLoc(), ArmorStand.class, entity -> VfxUtils.configureStand(entity, cleanupSettings));
        stand.addScoreboardTag(params.resolvedModel());
        if (standOut != null) {
            standOut[0] = stand;
        }

        ActiveModel model = ModelEngineAPI.createActiveModel(params.resolvedModel());
        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(stand);
        modeledEntity.addModel(model, params.useStateMachine());

        applyModelProperties(model, params);

        AtomicBoolean cleaned = new AtomicBoolean(false);
        VfxSession session = new VfxSession(stand, cleaned);
        final ArmorStand spawnedStand = stand;

        modeledEntity.queuePostInitTask(() -> {
            modeledEntity.setYBodyRotImmediately(params.spawnYaw());
            modeledEntity.setYHeadRotImmediately(params.spawnYaw());
            modeledEntity.setXHeadRotImmediately(params.spawnPitch());

            if (params.lockPitch()) {
                model.setLockPitch(true);
            }
            if (params.lockYaw()) {
                model.setLockYaw(true);
            }
            if (params.lockRotation()) {
                model.setModelRotationLocked(true);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!spawnedStand.isValid() || modeledEntity.isDestroyed()) {
                    return;
                }
                applyBoneSetup(model, params);
                if (params.useStateMachine() && params.animationState() != null) {
                    StateMachineSupport.playState(model, params.animationState(), params.animationStateSpeed(), log);
                }
            }, BONE_SETUP_DELAY_TICKS);

            if (params.doMount() && params.mountEntity() instanceof LivingEntity le && le.isValid()) {
                mountEntity(model, le, params.resolvedModel(), params.mountBone(), params.mountController());
            }
        });

        long actualDespawn = resolveDespawnDelay(params);

        scheduleFollow(session, params);
        scheduleHostDeathWatch(session, params);

        context.vfxTracker().register(session);

        scheduler.animationChain(session, model, stand, params.animationEntries());
        scheduler.changeParts(session, model, modeledEntity, stand, params.changePartEntries());

        if (actualDespawn > 0) {
            session.trackTask(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> cleanup(session), actualDespawn));
        }

        return () -> cleanup(session);
    }

    private void logFailure(String message) {
        if (context.config().debug().logSpawnFailures()) {
            log.warning(message);
        }
    }

    private long resolveDespawnDelay(VfxSpawnParams params) {
        long configured = params.removeDelay();
        if (configured == 0) {
            return 0;
        }
        if (configured < 0) {
            long computed = scheduler.computeDespawnDelay(
                    params.animationEntries(),
                    params.changePartEntries());
            return Math.max(0, computed);
        }
        return configured;
    }

    private void applyModelProperties(ActiveModel model, VfxSpawnParams params) {
        if (params.modelScale() != 1.0) {
            model.setScale(params.modelScale());
        }
        if (params.glowRgb() != null) {
            model.setGlowing(true);
            model.setGlowColor(params.glowRgb());
        }
        if (params.billboard() != null) {
            model.setBillboard(params.billboard());
        }
        if (params.onFire()) {
            model.setOnFire(true);
            model.setRenderFire(true);
        }
        model.setShadowVisible(params.shadow());
    }

    private void applyBoneSetup(ActiveModel model, VfxSpawnParams params) {
        if (params.billboard() != null) {
            model.getBones().values().forEach(bone -> bone.setBillboard(params.billboard()));
        }

        BoneOverrides overrides = params.boneOverrides();
        if (overrides != null) {
            overrides.apply(model, params.baseColor(), params.isGlowing(),
                    params.blockLight(), params.skyLight());
        }
    }

    private void scheduleFollow(VfxSession session, VfxSpawnParams params) {
        if (params.followEntity() == null) {
            return;
        }

        Entity followEntity = params.followEntity();
        boolean orientYaw = params.orientFollowYaw();
        boolean removeOnDeath = params.removeOnHostDeath() || params.removeDelay() == 0;
        DscMegConfig.LimitsSettings limits = context.config().limits();
        double maxFollowDistanceSq = limits.maxFollowDistance() > 0
                ? limits.maxFollowDistance() * limits.maxFollowDistance()
                : -1.0;
        int baseInterval = Math.max(1, limits.followIntervalTicks());
        int farInterval = Math.max(baseInterval, limits.followFarIntervalTicks());
        double farDistanceSq = limits.followFarDistance() * limits.followFarDistance();
        ArmorStand stand = session.stand();
        int[] ticksSinceTeleport = {0};

        session.trackTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!stand.isValid()) {
                return;
            }
            if (!followEntity.isValid()) {
                cleanup(session);
                return;
            }
            if (removeOnDeath && followEntity.isDead()) {
                cleanup(session);
                return;
            }

            Location followLoc = followEntity.getLocation();
            double distSq = stand.getLocation().distanceSquared(followLoc);
            if (maxFollowDistanceSq >= 0 && distSq > maxFollowDistanceSq) {
                return;
            }

            ticksSinceTeleport[0] += baseInterval;
            int requiredInterval = distSq > farDistanceSq ? farInterval : baseInterval;
            if (ticksSinceTeleport[0] < requiredInterval) {
                return;
            }
            ticksSinceTeleport[0] = 0;

            if (orientYaw) {
                stand.teleport(followLoc);
            } else {
                stand.teleport(new Location(
                        followLoc.getWorld(),
                        followLoc.getX(),
                        followLoc.getY(),
                        followLoc.getZ(),
                        stand.getLocation().getYaw(),
                        followLoc.getPitch()));
            }
        }, baseInterval, baseInterval));
    }

    private void scheduleHostDeathWatch(VfxSession session, VfxSpawnParams params) {
        if (params.followEntity() != null || params.mountEntity() == null) {
            return;
        }
        if (!params.removeOnHostDeath() && params.removeDelay() != 0) {
            return;
        }

        Entity mountEntity = params.mountEntity();
        ArmorStand stand = session.stand();

        session.trackTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!stand.isValid()) {
                return;
            }
            if (!mountEntity.isValid() || mountEntity.isDead()) {
                cleanup(session);
            }
        }, 20L, 20L));
    }

    public void cleanup(VfxSession session) {
        if (!session.cleaned().compareAndSet(false, true)) {
            return;
        }
        context.vfxTracker().unregister(session);
        for (int taskId : session.scheduledTaskIds()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        if (session.stand().isValid()) {
            session.stand().remove();
        }
    }

    private void mountEntity(
            ActiveModel model,
            LivingEntity entity,
            String resolvedModel,
            String mountBone,
            String mountController) {
        var maybeManager = model.getMountManager();
        if (maybeManager.isEmpty()) {
            log.warning("no mount behavior in model '" + resolvedModel + "'");
            return;
        }

        var mountManager = maybeManager.get();
        mountManager.setCanRide(true);
        mountManager.setCanDrive(true);

        MountControllerSupplier controller = switch (mountController == null ? "" : mountController.toLowerCase()) {
            case "flying" -> MountControllerTypes.FLYING;
            case "flying_force" -> MountControllerTypes.FLYING_FORCE;
            case "walking_force" -> MountControllerTypes.WALKING_FORCE;
            default -> MountControllerTypes.WALKING;
        };

        boolean mounted = mountManager.getSeat(mountBone)
                .map(seat -> mountManager.mountPassenger(seat, entity, controller))
                .orElseGet(() -> mountManager.mountDriver(entity, controller));

        if (!mounted) {
            log.warning("failed to mount entity on model '" + resolvedModel + "'");
        }
    }
}
