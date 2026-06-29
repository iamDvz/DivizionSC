package ru.iamdvz.divizionsc.def.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConditionParserTest {

    @Test
    void parsesNumericComparisonWithPercent() {
        ConditionSpec spec = ConditionParser.parse("health < 50%");
        assertEquals("health", spec.type());
        assertFalse(spec.negate());
        assertEquals(Comparison.LT, spec.comparison());
        assertEquals("50%", spec.firstArg(null));
    }

    @Test
    void parsesGluedOperator() {
        ConditionSpec spec = ConditionParser.parse("distance<=10");
        assertEquals("distance", spec.type());
        assertEquals(Comparison.LE, spec.comparison());
        assertEquals("10", spec.firstArg(null));
    }

    @Test
    void parsesBangNegation() {
        ConditionSpec spec = ConditionParser.parse("!holding DIAMOND_SWORD");
        assertEquals("holding", spec.type());
        assertTrue(spec.negate());
        assertEquals("DIAMOND_SWORD", spec.firstArg(null));
    }

    @Test
    void parsesNotPrefix() {
        ConditionSpec spec = ConditionParser.parse("not sneaking");
        assertEquals("sneaking", spec.type());
        assertTrue(spec.negate());
        assertFalse(spec.hasComparison());
    }

    @Test
    void notEqualsIsNotNegationPrefix() {
        ConditionSpec spec = ConditionParser.parse("variable combo != 0");
        assertEquals("variable", spec.type());
        assertFalse(spec.negate());
        assertEquals(Comparison.NE, spec.comparison());
        assertEquals("combo", spec.firstArg(null));
        assertEquals("0", spec.arg(1, null));
    }

    @Test
    void aliasesNormalize() {
        assertEquals("health", ConditionParser.parse("hp >= 1").type());
        assertEquals("distance", ConditionParser.parse("dist > 5").type());
        assertEquals("permission", ConditionParser.parse("perm foo.bar").type());
    }
}
