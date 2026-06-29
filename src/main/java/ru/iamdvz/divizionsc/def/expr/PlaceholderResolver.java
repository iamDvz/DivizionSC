package ru.iamdvz.divizionsc.def.expr;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.iamdvz.divizionsc.def.effect.DistanceUtil;
import ru.iamdvz.divizionsc.def.effect.EffectContext;

/**
 * Подстановка плейсхолдеров в строки/формулы.
 * Встроенные токены вида {@code {caster_health}}, {@code {target_distance}}, {@code {var_NAME}},
 * затем (если доступен PlaceholderAPI) обычные {@code %papi%}-плейсхолдеры через подключаемую функцию.
 */
public final class PlaceholderResolver {

    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z0-9_.]+)}");

    private volatile BiFunction<Player, String, String> papi = (player, text) -> text;

    /** Устанавливается интеграцией PlaceholderAPI (фаза интеграций). */
    public void setPapiFunction(BiFunction<Player, String, String> function) {
        this.papi = function == null ? (player, text) -> text : function;
    }

    public String resolve(String input, EffectContext ctx) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String replaced = replaceBuiltins(input, ctx);
        if (replaced.indexOf('%') >= 0 && ctx.caster() != null) {
            replaced = papi.apply(ctx.caster(), replaced);
        }
        return replaced;
    }

    private String replaceBuiltins(String input, EffectContext ctx) {
        if (input.indexOf('{') < 0) {
            return input;
        }
        Matcher matcher = TOKEN.matcher(input);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1).toLowerCase();
            String value = builtin(token, ctx);
            matcher.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : matcher.group()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String builtin(String token, EffectContext ctx) {
        Player caster = ctx.caster();
        LivingEntity target = ctx.targetEntity();
        if (token.startsWith("var_")) {
            String name = token.substring(4);
            if (ctx.vars().hasNumber(name)) {
                return trimNumber(ctx.vars().getNumber(name, 0));
            }
            return ctx.vars().getString(name, "0");
        }
        return switch (token) {
            case "caster_name" -> caster != null ? caster.getName() : "";
            case "caster_health" -> caster != null ? trimNumber(caster.getHealth()) : "0";
            case "caster_max_health" -> caster != null ? trimNumber(maxHealth(caster)) : "0";
            case "caster_level" -> caster != null ? String.valueOf(caster.getLevel()) : "0";
            case "caster_x" -> caster != null ? trimNumber(caster.getLocation().getX()) : "0";
            case "caster_y" -> caster != null ? trimNumber(caster.getLocation().getY()) : "0";
            case "caster_z" -> caster != null ? trimNumber(caster.getLocation().getZ()) : "0";
            case "caster_world" -> caster != null ? caster.getWorld().getName() : "";
            case "target_health" -> target != null ? trimNumber(target.getHealth()) : "0";
            case "target_max_health" -> target != null ? trimNumber(maxHealth(target)) : "0";
            case "target_name" -> target != null ? target.getName() : "";
            case "target_distance" -> trimNumber(DistanceUtil.forFormula(ctx));
            case "def_id", "ability" -> ctx.def() != null ? ctx.def().id() : "";
            case "def_cooldown" -> ctx.def() != null ? trimNumber(ctx.def().cooldown()) : "0";
            case "random" -> trimNumber(ThreadLocalRandom.current().nextDouble());
            case "chain_depth" -> String.valueOf(ctx.chainDepth());
            default -> null;
        };
    }

    @SuppressWarnings("deprecation")
    private static double maxHealth(LivingEntity entity) {
        return entity.getMaxHealth();
    }

    private static String trimNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
