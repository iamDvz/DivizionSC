package ru.iamdvz.dscmm.mechanic;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.INoTargetSkill;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.ITargetedLocationSkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.iamdvz.divizionsc.api.DivizionSCApi;
import ru.iamdvz.divizionsc.api.IntegrationCastOptions;
import ru.iamdvz.divizionsc.def.service.DefService;
import ru.iamdvz.dscmm.MmContext;
import ru.iamdvz.dscmm.util.MythicCasterResolver;

public final class DscDefMechanic implements INoTargetSkill, ITargetedEntitySkill, ITargetedLocationSkill {

    private final MmContext context;
    private final String defId;
    private final boolean allowHelper;
    private final boolean checkPermission;
    private final boolean applyCooldown;
    private final boolean requireTarget;
    private final String casterMode;

    public DscDefMechanic(MmContext context, MythicLineConfig config) {
        this.context = context;
        defId = config.getString(new String[]{"def", "ability", "id", "skill"}, "def");
        allowHelper = config.getBoolean(new String[]{"helper", "h"}, context.config().mechanicDefaults().allowHelper());
        checkPermission = config.getBoolean(new String[]{"permission", "perm"}, context.config().mechanicDefaults().checkPermission());
        applyCooldown = config.getBoolean(new String[]{"cooldown", "cd"}, context.config().mechanicDefaults().applyCooldown());
        requireTarget = config.getBoolean(new String[]{"require-target", "need-target"}, context.config().mechanicDefaults().requireTarget());
        casterMode = config.getString(new String[]{"caster", "source"}, context.config().mechanicDefaults().casterMode());
    }

    @Override
    public SkillResult cast(SkillMetadata data) {
        return castDef(data, null, null);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        LivingEntity living = null;
        Location location = null;
        if (target != null) {
            if (BukkitAdapter.adapt(target) instanceof LivingEntity entity) {
                living = entity;
                location = entity.getLocation();
            } else {
                location = BukkitAdapter.adapt(target.getLocation());
            }
        }
        return castDef(data, living, location);
    }

    @Override
    public SkillResult castAtLocation(SkillMetadata data, AbstractLocation target) {
        Location location = target == null ? null : BukkitAdapter.adapt(target);
        return castDef(data, null, location);
    }

    private SkillResult castDef(SkillMetadata data, LivingEntity targetEntity, Location targetLocation) {
        if (!DivizionSCApi.isAvailable()) {
            logFailure("DivizionSC is not available");
            return SkillResult.ERROR;
        }
        if (defId == null || defId.isBlank()) {
            logFailure("Mechanic is missing def/ability/id");
            return SkillResult.INVALID_CONFIG;
        }

        Player caster = MythicCasterResolver.resolve(data, casterMode, targetEntity);
        if (caster == null) {
            logFailure("No player caster resolved for def '" + defId + "' (caster=" + casterMode + ")");
            return SkillResult.INVALID_TARGET;
        }

        IntegrationCastOptions options = new IntegrationCastOptions(
                allowHelper,
                checkPermission,
                applyCooldown,
                requireTarget
        );
        DefService.CastResult result = DivizionSCApi.castIntegration(
                caster,
                defId,
                targetEntity,
                targetLocation,
                options
        );

        return switch (result) {
            case SUCCESS -> SkillResult.SUCCESS;
            case NOT_FOUND -> {
                logFailure("Def '" + defId + "' not found");
                yield SkillResult.INVALID_CONFIG;
            }
            case HELPER_ONLY -> {
                logFailure("Def '" + defId + "' is helper-only (set helper=true in mechanic)");
                yield SkillResult.INVALID_CONFIG;
            }
            case NO_PERMISSION -> SkillResult.CONDITION_FAILED;
            case COOLDOWN -> SkillResult.CONDITION_FAILED;
            case NO_TARGET -> SkillResult.INVALID_TARGET;
            case CANCELLED -> SkillResult.CONDITION_FAILED;
            default -> SkillResult.ERROR;
        };
    }

    private void logFailure(String message) {
        if (context.config().debug().logFailures()) {
            context.plugin().getLogger().warning("[DSC_MM] " + message);
        }
    }
}
