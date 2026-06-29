package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.iamdvz.divizionsc.util.EffectKeys;

/** Нормализует {@code fx { }} в параметры EffectLib. */
public final class DscFxBlockNormalizer {

    private static final List<String> NESTED_BLOCKS = List.of(
            "timing", "base", "params",
            "sphere", "helix", "circle", "line", "arc", "tornado", "wave",
            "target", "sub-effect", "sub_effect"
    );

    private static final Map<String, String> KEY_ALIASES = Map.ofEntries(
            Map.entry("shape", "class"),
            Map.entry("type", "class"),
            Map.entry("effect", "class"),
            Map.entry("p", "particle"),
            Map.entry("count", "particles"),
            Map.entry("amount", "particles"),
            Map.entry("r", "radius"),
            Map.entry("times", "iterations"),
            Map.entry("repeats", "iterations"),
            Map.entry("interval", "period"),
            Map.entry("tick", "period"),
            Map.entry("visible-range", "visibleRange"),
            Map.entry("async", "asynchronous"),
            Map.entry("auto-orient", "autoOrient"),
            Map.entry("y-offset", "yOffset"),
            Map.entry("radius-increase", "radiusIncrease"),
            Map.entry("particle-increase", "particleIncrease"),
            Map.entry("whole-circle", "wholeCircle"),
            Map.entry("enable-rotation", "enableRotation"),
            Map.entry("is-zig-zag", "isZigZag"),
            Map.entry("zig-zags", "zigZags"),
            Map.entry("max-length", "maxLength"),
            Map.entry("curves", "curve"),
            Map.entry("strands-count", "strands"),
            Map.entry("tornado-particle", "tornadoParticle"),
            Map.entry("cloud-particle", "cloudParticle"),
            Map.entry("tornado-color", "tornadoColor"),
            Map.entry("cloud-color", "cloudColor"),
            Map.entry("cloud-speed", "cloudSpeed"),
            Map.entry("cloud-size", "cloudSize"),
            Map.entry("tornado-height", "tornadoHeight"),
            Map.entry("max-tornado-radius", "maxTornadoRadius"),
            Map.entry("show-cloud", "showCloud"),
            Map.entry("show-tornado", "showTornado"),
            Map.entry("circle-particles", "circleParticles"),
            Map.entry("cloud-particles", "cloudParticles"),
            Map.entry("circle-height", "circleHeight"),
            Map.entry("particles-front", "particlesFront"),
            Map.entry("particles-back", "particlesBack"),
            Map.entry("length-front", "lengthFront"),
            Map.entry("length-back", "lengthBack"),
            Map.entry("depth-front", "depthFront"),
            Map.entry("height-back", "heightBack"),
            Map.entry("particle-offset", "particleOffset"),
            Map.entry("particle-size", "particleSize"),
            Map.entry("particle-data", "particleData"),
            Map.entry("relative-offset", "relativeOffset"),
            Map.entry("target-offset", "targetOffset"),
            Map.entry("yaw-offset", "yawOffset"),
            Map.entry("pitch-offset", "pitchOffset"),
            Map.entry("update-locations", "updateLocations"),
            Map.entry("update-directions", "updateDirections"),
            Map.entry("disappear-with-origin", "disappearWithOriginEntity"),
            Map.entry("disappear-with-target", "disappearWithTargetEntity"),
            Map.entry("sub-effect-at-end", "subEffectAtEnd")
    );

    private DscFxBlockNormalizer() {
    }

    public static Map<String, Object> normalize(Map<String, Object> raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        mergeEntries(out, raw, true);

        for (String nested : NESTED_BLOCKS) {
            Object block = raw.get(nested);
            if (block instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) map;
                mergeEntries(out, nestedMap, false);
            }
        }

        resolveClass(out, raw);
        normalizeParticle(out);
        normalizeOffset(out, raw);
        normalizeDuration(out, raw);
        stripRouteKeys(out);
        out.remove("shape");
        out.remove("type");
        out.remove("effect");
        return out;
    }

    private static void mergeEntries(Map<String, Object> out, Map<String, Object> source, boolean skipNested) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            if (skipNested && NESTED_BLOCKS.contains(key)) {
                continue;
            }
            if ("position".equals(key) || "at".equals(key) || "location".equals(key)) {
                out.putIfAbsent("at", String.valueOf(entry.getValue()));
                continue;
            }
            String normalizedKey = normalizeKey(key);
            out.put(normalizedKey, entry.getValue());
        }
    }

    private static void resolveClass(Map<String, Object> out, Map<String, Object> raw) {
        if (out.containsKey("class")) {
            Object value = out.get("class");
            if (value instanceof String className && !className.endsWith("Effect")) {
                out.put("class", shapeClass(className));
            }
            return;
        }
        for (String key : List.of("shape", "type", "effect")) {
            Object value = raw.get(key);
            if (value instanceof String shape) {
                out.put("class", shapeClass(shape));
                return;
            }
        }
        out.putIfAbsent("class", "SphereEffect");
    }

    private static void normalizeParticle(Map<String, Object> out) {
        Object particle = out.get("particle");
        if (particle instanceof String raw) {
            out.put("particle", EffectKeys.normalizeParticleName(raw));
        }
        for (String key : List.of("tornadoParticle", "cloudParticle")) {
            Object value = out.get(key);
            if (value instanceof String raw) {
                out.put(key, EffectKeys.normalizeParticleName(raw));
            }
        }
    }

    private static void normalizeOffset(Map<String, Object> out, Map<String, Object> raw) {
        if (!out.containsKey("relativeOffset")) {
            Object offset = firstPresent(raw, "offset", "relative-offset", "relative_offset");
            if (offset != null) {
                out.put("relativeOffset", parseVector(offset));
            }
        }
        if (!out.containsKey("targetOffset")) {
            Object offset = firstPresent(raw, "target-offset", "target_offset");
            if (offset != null) {
                out.put("targetOffset", parseVector(offset));
            }
        }
        if (!out.containsKey("particleOffset")) {
            Object offset = firstPresent(raw, "particle-offset", "particle_offset");
            if (offset != null) {
                out.put("particleOffset", parseVector(offset));
            }
        }
        out.remove("offset");
    }

    private static void normalizeDuration(Map<String, Object> out, Map<String, Object> raw) {
        Object duration = out.get("duration");
        if (duration == null) {
            duration = firstPresent(raw, "time", "length-time");
        }
        if (duration == null) {
            return;
        }
        if (duration instanceof Number number) {
            out.put("duration", number.intValue());
            return;
        }
        String text = String.valueOf(duration).trim().toLowerCase(Locale.ROOT);
        if (text.endsWith("s")) {
            double seconds = Double.parseDouble(text.substring(0, text.length() - 1));
            out.put("duration", (int) Math.round(seconds * 20));
            return;
        }
        if (text.endsWith("t")) {
            out.put("duration", (int) Math.round(Double.parseDouble(text.substring(0, text.length() - 1))));
            return;
        }
        try {
            out.put("duration", (int) Math.round(Double.parseDouble(text)));
        } catch (NumberFormatException ignored) {
            out.put("duration", duration);
        }
    }

    private static void stripRouteKeys(Map<String, Object> out) {
        out.remove("position");
        out.remove("location");
    }

    private static Object firstPresent(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            if (raw.containsKey(key)) {
                return raw.get(key);
            }
        }
        return null;
    }

    private static String normalizeKey(String key) {
        String alias = KEY_ALIASES.get(key);
        if (alias != null) {
            return alias;
        }
        if (key.contains("-") || key.contains("_")) {
            return toCamelCase(key);
        }
        return key;
    }

    private static String toCamelCase(String key) {
        String normalized = key.replace('_', '-');
        StringBuilder builder = new StringBuilder();
        boolean upper = false;
        for (char ch : normalized.toCharArray()) {
            if (ch == '-') {
                upper = true;
                continue;
            }
            builder.append(upper ? Character.toUpperCase(ch) : ch);
            upper = false;
        }
        return builder.toString();
    }

    private static String parseVector(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("0,0,0");
        }
        return String.valueOf(raw).replace(" ", "");
    }

    public static String shapeClass(String shape) {
        String token = shape.trim();
        if (token.endsWith("Effect")) {
            return token;
        }
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "helix", "spiral" -> "HelixEffect";
            case "circle", "ring" -> "CircleEffect";
            case "line", "beam", "ray" -> "LineEffect";
            case "arc" -> "ArcEffect";
            case "tornado" -> "TornadoEffect";
            case "wave" -> "WaveEffect";
            case "cube" -> "CubeEffect";
            case "square" -> "SquareEffect";
            case "cuboid" -> "CuboidEffect";
            case "star" -> "StarEffect";
            case "smoke" -> "SmokeEffect";
            case "flame" -> "FlameEffect";
            case "atom" -> "AtomEffect";
            case "trace" -> "TraceEffect";
            case "cloud" -> "CloudEffect";
            default -> "SphereEffect";
        };
    }
}
