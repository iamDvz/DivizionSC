package ru.iamdvz.dscmeg.model;

import java.util.List;

public record ChangePartEntry(
        String modelId,
        List<String> partIds,
        String newModelId,
        List<String> newPartIds,
        int delay
) {
}
