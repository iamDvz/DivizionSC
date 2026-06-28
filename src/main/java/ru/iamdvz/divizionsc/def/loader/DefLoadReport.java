package ru.iamdvz.divizionsc.def.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DefLoadReport {

    private int loadedCount;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public void defLoaded() {
        loadedCount++;
    }

    public void addWarning(String message) {
        warnings.add(message);
    }

    public void addError(String message) {
        errors.add(message);
    }

    public int loadedCount() {
        return loadedCount;
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean hasIssues() {
        return !warnings.isEmpty() || !errors.isEmpty();
    }
}
