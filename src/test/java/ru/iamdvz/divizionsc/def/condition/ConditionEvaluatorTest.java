package ru.iamdvz.divizionsc.def.condition;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.def.effect.DistanceUtil;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.expr.ExpressionEvaluator;
import ru.iamdvz.divizionsc.def.expr.PlaceholderResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionEvaluatorTest {

    private final ConditionEvaluator evaluator = new ConditionEvaluator(
            new PlaceholderResolver(),
            new ExpressionEvaluator(new PlaceholderResolver())
    );

    @Test
    void distanceWithoutTarget_failsLessThanCheck() {
        EffectContext ctx = new EffectContext(null, null, null, null, null, null, 0);
        ConditionSpec spec = ConditionParser.parse("distance < 8");
        assertFalse(evaluator.matches(spec, ctx));
    }

    @Test
    void permissionRequiresCaster() {
        EffectContext ctx = new EffectContext(null, null, null, null, null, null, 0);
        ConditionSpec spec = ConditionParser.parse("permission divizionsc.admin");
        assertFalse(evaluator.matches(spec, ctx));
    }

    @Test
    void negateInvertsResult() {
        EffectContext ctx = new EffectContext(null, null, null, null, null, null, 0);
        ConditionSpec spec = ConditionParser.parse("!has-target");
        assertTrue(evaluator.matches(spec, ctx));
    }

    @Test
    void distanceUtil_noTarget_isMaxValue() {
        assertEquals(DistanceUtil.NO_TARGET, DistanceUtil.between(
                new EffectContext(null, null, null, null, null, null, 0)));
    }
}
