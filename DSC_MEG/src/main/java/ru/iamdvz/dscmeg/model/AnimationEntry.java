package ru.iamdvz.dscmeg.model;

public record AnimationEntry(
        String name,
        int delay,
        long duration,
        double lerpIn,
        double lerpOut,
        double speed,
        boolean overlap,
        boolean loop
) {
}
