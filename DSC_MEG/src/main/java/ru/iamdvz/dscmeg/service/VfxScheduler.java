package ru.iamdvz.dscmeg.service;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.entity.ArmorStand;
import ru.iamdvz.divizionsc.platform.Scheduler;
import ru.iamdvz.dscmeg.model.AnimationEntry;
import ru.iamdvz.dscmeg.model.ChangePartEntry;
import ru.iamdvz.dscmeg.model.VfxSession;

import java.util.List;

public final class VfxScheduler {

    private final Scheduler scheduler;

    public VfxScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void animationChain(
            VfxSession session,
            ActiveModel model,
            ArmorStand stand,
            List<AnimationEntry> entries) {
        long sequenceCursor = 0L;
        String prevName = null;

        for (int i = 0; i < entries.size(); i++) {
            AnimationEntry entry = entries.get(i);
            String name = entry.name();
            int delay = entry.delay();
            long duration = entry.duration();
            double lerpIn = entry.lerpIn();
            double lerpOut = entry.lerpOut();
            double speed = entry.speed();
            boolean overlap = entry.overlap();

            long startTick;
            if (overlap) {
                startTick = delay;
            } else {
                sequenceCursor += delay;
                startTick = sequenceCursor;
                sequenceCursor += Math.round(duration / speed);
            }

            final String capturedPrev = (!overlap && i > 0) ? prevName : null;
            final String capturedName = name;

            session.trackTask(scheduler.entityLater(stand, () -> {
                if (!stand.isValid()) {
                    return;
                }
                if (capturedPrev != null) {
                    model.getAnimationHandler().forceStopAnimation(capturedPrev);
                }
                IAnimationProperty prop = model.getAnimationHandler()
                        .playAnimation(capturedName, lerpIn, lerpOut, speed, true);
                if (prop == null) {
                    return;
                }
                if (entry.loop()) {
                    prop.setForceLoopMode(BlueprintAnimation.LoopMode.LOOP);
                }
            }, startTick));

            prevName = name;
        }
    }

    public void changeParts(
            VfxSession session,
            ActiveModel spawnedModel,
            ModeledEntity modeledEntity,
            ArmorStand stand,
            List<ChangePartEntry> entries) {
        String spawnedBpName = spawnedModel.getBlueprint().getName();

        for (ChangePartEntry entry : entries) {
            String targetModelId = entry.modelId();
            String newModelId = entry.newModelId();
            int delay = entry.delay();

            session.trackTask(scheduler.entityLater(stand, () -> {
                if (!stand.isValid()) {
                    return;
                }

                ActiveModel targetModel = targetModelId.equals(spawnedBpName)
                        ? spawnedModel
                        : modeledEntity.getModels().get(targetModelId);
                var newBp = ModelEngineAPI.getBlueprint(newModelId);
                if (targetModel == null || newBp == null) {
                    return;
                }

                var flatMap = newBp.getFlatMap();
                List<String> parts = entry.partIds();
                List<String> newParts = entry.newPartIds();

                for (int i = 0; i < parts.size(); i++) {
                    String partId = parts.get(i);
                    String newPartId = i < newParts.size() ? newParts.get(i) : partId;
                    var activeBone = targetModel.getBone(partId).orElse(null);
                    var blueprintBone = flatMap.get(newPartId);
                    if (activeBone == null || blueprintBone == null) {
                        continue;
                    }
                    activeBone.setModel(blueprintBone);
                }
            }, delay));
        }
    }

    public long computeDespawnDelay(
            List<AnimationEntry> animationEntries,
            List<ChangePartEntry> changePartEntries) {
        long sequenceCursor = 0L;
        long maxEndTick = 0L;

        for (AnimationEntry entry : animationEntries) {
            if (entry.loop()) {
                continue;
            }
            long effectiveDuration = Math.round(entry.duration() / entry.speed());
            long startTick;
            if (entry.overlap()) {
                startTick = entry.delay();
            } else {
                sequenceCursor += entry.delay();
                startTick = sequenceCursor;
                sequenceCursor += effectiveDuration;
            }
            maxEndTick = Math.max(maxEndTick, startTick + effectiveDuration);
        }

        for (ChangePartEntry entry : changePartEntries) {
            maxEndTick = Math.max(maxEndTick, entry.delay());
        }

        return Math.max(sequenceCursor, maxEndTick);
    }
}
