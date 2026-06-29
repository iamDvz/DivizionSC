package ru.iamdvz.divizionsc.bind;

import java.util.Arrays;
import java.util.UUID;

public final class PlayerBinds {

    public static final int HOTBAR_SIZE = 9;

    private final UUID playerId;
    private final String[] slots;
    private volatile boolean dirty;

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
        dirty = true;
    }

    public void clear(int hotbarSlot) {
        set(hotbarSlot, null);
    }

    public boolean isDirty() {
        return dirty;
    }

    /** Сливает данные из БД: не перезаписывает слоты, изменённые локально. */
    public void mergeFromDb(String[] dbSlots) {
        if (dbSlots == null) {
            return;
        }
        if (!dirty) {
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                slots[i] = dbSlots[i];
            }
            return;
        }
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (isBlank(slots[i]) && !isBlank(dbSlots[i])) {
                slots[i] = dbSlots[i];
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String[] slotsCopy() {
        return Arrays.copyOf(slots, HOTBAR_SIZE);
    }
}
