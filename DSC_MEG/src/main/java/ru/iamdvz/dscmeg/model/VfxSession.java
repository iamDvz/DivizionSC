package ru.iamdvz.dscmeg.model;

import org.bukkit.entity.ArmorStand;
import ru.iamdvz.divizionsc.platform.TaskHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VfxSession {

    private final ArmorStand stand;
    private final UUID worldUid;
    private final AtomicBoolean cleaned;
    private final List<TaskHandle> scheduledTasks = new ArrayList<>(8);

    public VfxSession(ArmorStand stand, AtomicBoolean cleaned) {
        this.stand = stand;
        this.worldUid = stand.getWorld().getUID();
        this.cleaned = cleaned;
    }

    public ArmorStand stand() {
        return stand;
    }

    public UUID worldUid() {
        return worldUid;
    }

    public AtomicBoolean cleaned() {
        return cleaned;
    }

    public List<TaskHandle> scheduledTasks() {
        return scheduledTasks;
    }

    public void trackTask(TaskHandle handle) {
        if (handle != null && handle != TaskHandle.CANCELLED) {
            scheduledTasks.add(handle);
        }
    }
}
