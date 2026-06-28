package ru.iamdvz.divizionsc.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

public final class EffectKeys {

    private EffectKeys() {
    }

    public static Sound resolveSound(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);

        if (upper.chars().allMatch(c -> c == '_' || Character.isLetterOrDigit(c))) {
            try {
                return Sound.valueOf(upper);
            } catch (IllegalArgumentException ignored) {
            }
        }

        NamespacedKey directKey = NamespacedKey.fromString(trimmed);
        if (directKey != null) {
            Sound direct = Registry.SOUNDS.get(directKey);
            if (direct != null) {
                return direct;
            }
        }

        String dotted = trimmed.toLowerCase(Locale.ROOT).replace('_', '.');
        if (!dotted.contains(":")) {
            Sound namespaced = Registry.SOUNDS.get(NamespacedKey.minecraft(dotted));
            if (namespaced != null) {
                return namespaced;
            }
        }

        NamespacedKey parsed = NamespacedKey.fromString(dotted);
        if (parsed != null) {
            Sound fromParsed = Registry.SOUNDS.get(parsed);
            if (fromParsed != null) {
                return fromParsed;
            }
        }

        return null;
    }

    public static Particle resolveParticle(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        try {
            return Particle.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String normalizeParticleName(String name) {
        Particle particle = resolveParticle(name);
        return particle == null ? name.trim().toUpperCase(Locale.ROOT) : particle.name();
    }
}
