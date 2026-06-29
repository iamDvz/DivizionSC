package ru.iamdvz.divizionsc.def.effect;

import java.util.HashMap;
import java.util.Map;

/**
 * Изменяемое хранилище переменных в пределах одного каста.
 * Передаётся по ссылке через производные {@link EffectContext}, поэтому
 * {@code set var} в одном эффекте виден в последующих эффектах того же каста.
 */
public final class VariableScope {

    private final Map<String, Double> numbers = new HashMap<>();
    private final Map<String, String> strings = new HashMap<>();
    private boolean aborted;

    public static VariableScope create() {
        return new VariableScope();
    }

    public void abort() {
        this.aborted = true;
    }

    public boolean isAborted() {
        return aborted;
    }

    public void setNumber(String key, double value) {
        numbers.put(key, value);
    }

    public double getNumber(String key, double fallback) {
        Double value = numbers.get(key);
        return value == null ? fallback : value;
    }

    public boolean hasNumber(String key) {
        return numbers.containsKey(key);
    }

    public void setString(String key, String value) {
        strings.put(key, value);
    }

    public String getString(String key, String fallback) {
        return strings.getOrDefault(key, fallback);
    }

    public boolean hasString(String key) {
        return strings.containsKey(key);
    }

    public Map<String, Double> numbers() {
        return numbers;
    }

    public Map<String, String> strings() {
        return strings;
    }
}
