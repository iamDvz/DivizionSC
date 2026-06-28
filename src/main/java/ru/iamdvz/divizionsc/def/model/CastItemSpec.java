package ru.iamdvz.divizionsc.def.model;

import org.bukkit.Material;

import java.util.List;

public record CastItemSpec(
        Material material,
        String name,
        List<String> lore,
        int customModelData
) {
}
