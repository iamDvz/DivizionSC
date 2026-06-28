package ru.iamdvz.divizionsc.def.model;

public enum TargetMode {
    SELF,
    ENTITY,
    BLOCK,
    NONE;

    public static TargetMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        return switch (raw.toLowerCase()) {
            case "self", "caster", "me" -> SELF;
            case "entity", "target", "living", "mob", "ent" -> ENTITY;
            case "block", "location" -> BLOCK;
            case "none" -> NONE;
            default -> NONE;
        };
    }
}
