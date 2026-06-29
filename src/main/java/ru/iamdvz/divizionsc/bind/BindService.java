package ru.iamdvz.divizionsc.bind;

import org.bukkit.entity.Player;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.service.DefService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BindService {

    private final PluginContext context;
    private final BindRepository repository;

    public BindService(PluginContext context, BindRepository repository) {
        this.context = context;
        this.repository = repository;
    }

    public PlayerBinds binds(Player player) {
        return repository.load(player.getUniqueId());
    }

    public void bind(Player player, int hotbarSlot, String defId) {
        Optional<DefDefinition> def = context.defRegistry().find(defId);
        if (def.isEmpty()) {
            player.sendMessage(context.messages().format("def-not-found", Map.of("def", defId, "id", defId)));
            return;
        }
        if (def.get().helper()) {
            player.sendMessage(context.messages().format("helper-only", Map.of("def", defId)));
            return;
        }
        if (def.get().passive()) {
            player.sendMessage(context.messages().format("passive-only", Map.of("def", defId)));
            return;
        }
        PlayerBinds binds = binds(player);
        binds.set(hotbarSlot, defId);
        repository.save(binds);
    }

    public void unbind(Player player, int hotbarSlot) {
        PlayerBinds binds = binds(player);
        binds.clear(hotbarSlot);
        repository.save(binds);
    }

    public Optional<String> boundDef(Player player, int hotbarSlot) {
        String defId = binds(player).get(hotbarSlot);
        if (defId == null || defId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(defId);
    }

    public DefService.CastResult castBoundSlot(Player player, int hotbarSlot) {
        Optional<String> defId = boundDef(player, hotbarSlot);
        if (defId.isEmpty()) {
            return DefService.CastResult.NOT_BOUND;
        }
        return context.defs().castBound(player, defId.get());
    }

    public DefService.CastResult castCurrentSlot(Player player) {
        return castBoundSlot(player, player.getInventory().getHeldItemSlot());
    }

    public List<DefDefinition> availableDefs(Player player) {
        List<DefDefinition> result = new ArrayList<>();
        for (DefDefinition def : context.defRegistry().all()) {
            if (def.helper() || def.passive()) {
                continue;
            }
            if (player.hasPermission(def.permission()) || player.hasPermission("divizionsc.cast.*")) {
                result.add(def);
            }
        }
        return result;
    }

    public void unload(UUID playerId) {
        repository.unload(playerId);
    }
}
