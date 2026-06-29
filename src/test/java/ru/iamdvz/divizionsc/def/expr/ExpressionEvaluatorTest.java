package ru.iamdvz.divizionsc.def.expr;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.def.effect.DistanceUtil;
import ru.iamdvz.divizionsc.def.effect.EffectContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionEvaluatorTest {

    private final ExpressionEvaluator evaluator = new ExpressionEvaluator(new PlaceholderResolver());

    @Test
    void parsesNumericLiteral() {
        assertEquals(8.0, evaluator.evaluate("8", null, 0), 0.001);
    }

    @Test
    void invalidFormula_returnsFallback() {
        assertEquals(3.0, evaluator.evaluate("health +", null, 3), 0.001);
    }

    @Test
    void distanceVariable_withoutTarget_isZero() {
        EffectContext ctx = new EffectContext(null, null, null, null, null, null, 0);
        assertEquals(0, evaluator.evaluate("distance", ctx, -1), 0.001);
        assertEquals(0, DistanceUtil.forFormula(ctx), 0.001);
    }
}
