package ru.iamdvz.divizionsc.def.effect;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Все токены, которыми может начинаться строка эффекта (короткие и полные имена).
 */
public final class EffectVerbs {

    private static final Set<String> VERBS;
    private static final Map<String, String> CANONICAL;

    static {
        Set<String> verbs = new HashSet<>();
        Map<String, String> canonical = new HashMap<>();

        register(verbs, canonical, "damage", "dmg");
        register(verbs, canonical, "heal");
        register(verbs, canonical, "teleport", "tp");
        register(verbs, canonical, "velocity", "vel", "knockback");
        register(verbs, canonical, "sound", "snd");
        register(verbs, canonical, "particle", "ptl");
        register(verbs, canonical, "potion", "pot", "potioneffect");
        register(verbs, canonical, "lightning", "lit");
        register(verbs, canonical, "message", "msg");
        register(verbs, canonical, "def", "call", "chain", "ability");
        register(verbs, canonical, "projectile", "proj");
        register(verbs, canonical, "effectlib", "fx");
        register(verbs, canonical, "vfx", "meg", "modelengine", "model");
        register(verbs, canonical, "command", "cmd");
        register(verbs, canonical, "delay", "wait");
        register(verbs, canonical, "set", "setvar", "set_var");
        register(verbs, canonical, "require");
        register(verbs, canonical, "if");
        register(verbs, canonical, "chance");
        register(verbs, canonical, "loop");
        register(verbs, canonical, "area");
        register(verbs, canonical, "stun");
        register(verbs, canonical, "raycast", "beam");
        register(verbs, canonical, "particle_projectile", "ppj");
        register(verbs, canonical, "pull");
        register(verbs, canonical, "push");
        register(verbs, canonical, "money");
        register(verbs, canonical, "give-money");
        register(verbs, canonical, "take-money");
        register(verbs, canonical, "dash");
        register(verbs, canonical, "blink");
        register(verbs, canonical, "shield");
        register(verbs, canonical, "summon");
        register(verbs, canonical, "totem");
        register(verbs, canonical, "give", "give_item", "giveitem");
        register(verbs, canonical, "repeat");
        register(verbs, canonical, "shape", "shape_particle");
        register(verbs, canonical, "aura");
        register(verbs, canonical, "ignite");
        register(verbs, canonical, "glow", "glowing");
        register(verbs, canonical, "invis", "invisibility");
        register(verbs, canonical, "title");
        register(verbs, canonical, "swap");
        register(verbs, canonical, "explosion", "explode");
        register(verbs, canonical, "cleanse", "purge");
        register(verbs, canonical, "launch");
        register(verbs, canonical, "root");
        register(verbs, canonical, "chain");

        VERBS = Collections.unmodifiableSet(verbs);
        CANONICAL = Collections.unmodifiableMap(canonical);
    }

    private EffectVerbs() {
    }

    private static void register(Set<String> verbs, Map<String, String> canonical, String type, String... aliases) {
        verbs.add(type);
        canonical.put(type, type);
        for (String alias : aliases) {
            verbs.add(alias);
            canonical.put(alias, type);
        }
    }

    public static boolean isVerb(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return VERBS.contains(token.toLowerCase(Locale.ROOT));
    }

    public static String canonicalType(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return CANONICAL.get(token.toLowerCase(Locale.ROOT));
    }

    public static Set<String> all() {
        return VERBS;
    }
}
