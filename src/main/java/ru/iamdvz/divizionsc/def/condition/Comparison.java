package ru.iamdvz.divizionsc.def.condition;

/**
 * Оператор сравнения для условий def.
 */
public enum Comparison {
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),
    EQ("=="),
    NE("!="),
    NONE("");

    private final String symbol;

    Comparison(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static Comparison fromSymbol(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case "<" -> LT;
            case "<=", "=<" -> LE;
            case ">" -> GT;
            case ">=", "=>" -> GE;
            case "==", "=" -> EQ;
            case "!=", "<>" -> NE;
            default -> null;
        };
    }

    public boolean test(double left, double right) {
        return switch (this) {
            case LT -> left < right;
            case LE -> left <= right;
            case GT -> left > right;
            case GE -> left >= right;
            case EQ -> left == right;
            case NE -> left != right;
            case NONE -> true;
        };
    }
}
