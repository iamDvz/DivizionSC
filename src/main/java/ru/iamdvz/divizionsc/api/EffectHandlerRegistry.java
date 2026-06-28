package ru.iamdvz.divizionsc.api;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EffectHandlerRegistry {

    private final Map<String, EffectHandler> handlers = new ConcurrentHashMap<>();

    public void register(EffectHandler handler) {
        handlers.put(normalize(handler.type()), handler);
    }

    public void unregister(String type) {
        handlers.remove(normalize(type));
    }

    public Optional<EffectHandler> find(String type) {
        return Optional.ofNullable(handlers.get(normalize(type)));
    }

    private static String normalize(String type) {
        return type == null ? "" : type.toLowerCase(Locale.ROOT);
    }
}
