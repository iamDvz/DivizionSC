package ru.iamdvz.divizionsc.bind;

import java.util.Arrays;
import java.util.UUID;

public final class PlayerBinds {

    public static final int HOTBAR_SIZE = 9;

    private final UUID playerId;
    private final String[] slots;

    public PlayerBinds(UUID playerId) {
        this.playerId = playerId;
        this.slots = new String[HOTBAR_SIZE];
    }

    public PlayerBinds(UUID playerId, String[] slots) {
        this.playerId = playerId;
        this.slots = Arrays.copyOf(slots, HOTBAR_SIZE);
    }

    public UUID playerId() {
        return playerId;
    }

    public String get(int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot >= HOTBAR_SIZE) {
            return null;
        }
        return slots[hotbarSlot];
    }

    public void set(int hotbarSlot, String defId) {
        if (hotbarSlot < 0 || hotbarSlot >= HOTBAR_SIZE) {
            return;
        }
        slots[hotbarSlot] = defId;
    }

    public void clear(int hotbarSlot) {
        set(hotbarSlot, null);
    }

    public String[] slotsCopy() {
        return Arrays.copyOf(slots, HOTBAR_SIZE);
    }
}
