package ru.iamdvz.divizionsc.api;

public record IntegrationCastOptions(
        boolean allowHelper,
        boolean checkPermission,
        boolean applyCooldown,
        boolean requireTarget
) {

    public static IntegrationCastOptions defaults() {
        return new IntegrationCastOptions(true, false, false, false);
    }

    public static IntegrationCastOptions mythicDefaults() {
        return defaults();
    }
}
