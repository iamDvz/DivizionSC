package ru.iamdvz.divizionsc.def.condition;

import java.util.List;

/**
 * Разобранное условие def (DSC).
 * Вычисление выполняется в фазе движка (condition-эвалуаторы), здесь — только данные.
 *
 * @param type       нормализованный тип условия (health, distance, permission, chance, variable, ...)
 * @param negate     инвертировать результат (префикс {@code !} или {@code not})
 * @param comparison оператор сравнения, либо {@link Comparison#NONE}
 * @param args       операнды/аргументы условия (без оператора)
 * @param raw        исходная строка условия
 */
public record ConditionSpec(
        String type,
        boolean negate,
        Comparison comparison,
        List<String> args,
        String raw
) {

    public ConditionSpec {
        args = args == null ? List.of() : List.copyOf(args);
    }

    public String arg(int index, String fallback) {
        return index >= 0 && index < args.size() ? args.get(index) : fallback;
    }

    public String firstArg(String fallback) {
        return arg(0, fallback);
    }

    public boolean hasComparison() {
        return comparison != null && comparison != Comparison.NONE;
    }
}
