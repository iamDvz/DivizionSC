package ru.iamdvz.divizionsc.bind;

import org.bukkit.entity.Player;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.TriggerType;
import ru.iamdvz.divizionsc.def.service.DefService;
import ru.iamdvz.divizionsc.listener.SkillBarListener;

public final class BindCastHelper {

    private BindCastHelper() {
    }

    public static boolean tryCastCurrentSlot(PluginContext context, Player player, TriggerType trigger) {
        if (!canBindCast(context, player, trigger)) {
            return false;
        }
        DefService.CastResult result = context.binds().castCurrentSlot(player);
        if (result != DefService.CastResult.SUCCESS) {
            context.defs().notifyCastFailure(player, result, null);
        }
        return result == DefService.CastResult.SUCCESS;
    }

    public static boolean tryCastBoundSlot(PluginContext context, Player player, int hotbarSlot, TriggerType trigger) {
        if (!canBindCast(context, player, trigger)) {
            return false;
        }
        DefService.CastResult result = context.binds().castBoundSlot(player, hotbarSlot);
        if (result != DefService.CastResult.SUCCESS) {
            context.defs().notifyCastFailure(player, result, null);
        }
        return result == DefService.CastResult.SUCCESS;
    }

    private static boolean canBindCast(PluginContext context, Player player, TriggerType trigger) {
        if (!context.config().skillBar().enabled()) {
            return false;
        }
        if (!player.hasPermission(context.config().skillBar().permission())) {
            return false;
        }
        if (!context.config().skillBar().acceptsBindTrigger(trigger)) {
            return false;
        }
        return !context.config().skillBar().requireEmptyHand() || SkillBarListener.isEmptyCastHand(player);
    }
}
