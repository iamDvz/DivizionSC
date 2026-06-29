package ru.iamdvz.divizionsc.def.service;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManaServiceTest {

    @Test
    void tracksAndSpendsManaByPlayerId() {
        Settings settings = new Settings();
        settings.mana.enabled = true;
        settings.mana.defaultAmount = 20;
        settings.opBypass.enabled = false;
        PluginConfig config = new PluginConfig(settings);
        ManaService mana = new ManaService(config);

        UUID playerId = UUID.randomUUID();
        mana.initPlayer(playerId);
        assertEquals(20.0, mana.current(playerId), 0.001);

        assertTrue(mana.spend(playerId, 8));
        assertEquals(12.0, mana.current(playerId), 0.001);
        assertFalse(mana.spend(playerId, 20));
    }

    @Test
    void disabledManaIgnoresCost() {
        Settings settings = new Settings();
        settings.mana.enabled = false;
        PluginConfig config = new PluginConfig(settings);
        ManaService mana = new ManaService(config);

        UUID playerId = UUID.randomUUID();
        mana.initPlayer(playerId);
        assertTrue(mana.canAfford(playerId, 999));
    }
}
