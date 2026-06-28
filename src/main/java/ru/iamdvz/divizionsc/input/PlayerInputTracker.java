package ru.iamdvz.divizionsc.input;

import org.bukkit.Input;
import org.bukkit.entity.Player;
import ru.iamdvz.divizionsc.def.model.TriggerType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerInputTracker {

    private final Map<UUID, InputSnapshot> previous = new HashMap<>();

    public Set<TriggerType> detectPressed(Player player, Input current) {
        InputSnapshot now = InputSnapshot.from(current);
        InputSnapshot before = previous.getOrDefault(player.getUniqueId(), InputSnapshot.empty());
        previous.put(player.getUniqueId(), now);
        return risingEdges(before, now);
    }

    public void clear(Player player) {
        previous.remove(player.getUniqueId());
    }

    private Set<TriggerType> risingEdges(InputSnapshot before, InputSnapshot now) {
        Set<TriggerType> pressed = EnumSet.noneOf(TriggerType.class);
        if (!before.forward() && now.forward()) {
            pressed.add(TriggerType.FORWARD);
        }
        if (!before.backward() && now.backward()) {
            pressed.add(TriggerType.BACKWARD);
        }
        if (!before.left() && now.left()) {
            pressed.add(TriggerType.LEFT);
        }
        if (!before.right() && now.right()) {
            pressed.add(TriggerType.RIGHT);
        }
        if (!before.jump() && now.jump()) {
            pressed.add(TriggerType.JUMP);
        }
        if (!before.sneak() && now.sneak()) {
            pressed.add(TriggerType.SNEAK);
        }
        if (!before.sprint() && now.sprint()) {
            pressed.add(TriggerType.SPRINT);
        }
        return pressed;
    }
}
