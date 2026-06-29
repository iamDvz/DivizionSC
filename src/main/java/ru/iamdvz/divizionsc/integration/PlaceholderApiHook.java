package ru.iamdvz.divizionsc.integration;

import me.clip.placeholderapi.PlaceholderAPI;
import ru.iamdvz.divizionsc.def.expr.PlaceholderResolver;

/**
 * Интеграция с PlaceholderAPI: подключает резолвер %papi%-плейсхолдеров.
 * Класс загружается только если PlaceholderAPI присутствует на сервере.
 */
public final class PlaceholderApiHook {

    private PlaceholderApiHook() {
    }

    public static void register(PlaceholderResolver resolver) {
        resolver.setPapiFunction(PlaceholderAPI::setPlaceholders);
    }
}
