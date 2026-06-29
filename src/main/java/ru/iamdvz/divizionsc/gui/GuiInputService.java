package ru.iamdvz.divizionsc.gui;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Отслеживает игроков, ожидающих ввод поискового запроса в чат.
 */
public final class GuiInputService {

    private final Set<UUID> awaitingSearch = ConcurrentHashMap.newKeySet();

    public void requestSearch(UUID playerId) {
        awaitingSearch.add(playerId);
    }

    public boolean isAwaitingSearch(UUID playerId) {
        return awaitingSearch.contains(playerId);
    }

    public void clear(UUID playerId) {
        awaitingSearch.remove(playerId);
    }
}
