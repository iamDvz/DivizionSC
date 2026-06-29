package ru.iamdvz.divizionsc.def.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistanceUtilTest {

    @Test
    void noCaster_returnsNoTarget() {
        EffectContext ctx = new EffectContext(null, null, null, null, null, null, 0);
        assertEquals(DistanceUtil.NO_TARGET, DistanceUtil.between(ctx));
        assertEquals(0, DistanceUtil.forFormula(ctx));
    }
}
