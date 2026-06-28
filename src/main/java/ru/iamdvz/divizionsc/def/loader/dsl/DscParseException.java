package ru.iamdvz.divizionsc.def.loader.dsl;

public final class DscParseException extends RuntimeException {

    public DscParseException(String message) {
        super(message);
    }

    public DscParseException(String message, int lineNumber) {
        super(message + " (line " + lineNumber + ")");
    }
}
