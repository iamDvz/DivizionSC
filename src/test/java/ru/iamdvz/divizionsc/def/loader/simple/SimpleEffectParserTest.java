package ru.iamdvz.divizionsc.def.loader.simple;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SimpleEffectParserTest {

    private final SimpleEffectParser parser = new SimpleEffectParser();

    @Test
    void parsesHealLine() {
        SimpleEffectParser.ParseResult result = parser.parse("heal 8");
        EffectDefinition effect = result.effect();
        assertNotNull(effect);
        assertEquals("heal", effect.type());
        assertEquals(8.0, effect.number("amount", 0));
    }

    @Test
    void parsesDamageLine() {
        SimpleEffectParser.ParseResult result = parser.parse("dmg 5");
        EffectDefinition effect = result.effect();
        assertNotNull(effect);
        assertEquals("damage", effect.type());
        assertEquals(5.0, effect.number("amount", 0));
    }

    @Test
    void parsesInlineDelay() {
        SimpleEffectParser.ParseResult result = parser.parse("wait 10t: heal 4");
        EffectDefinition effect = result.effect();
        assertNotNull(effect);
        assertEquals("delay", effect.type());
        assertEquals(10, effect.integer("ticks", 0));
        assertEquals(1, effect.children("effects").size());
        assertEquals("heal", effect.children("effects").getFirst().type());
    }

    @Test
    void ignoresEmptyLine() {
        SimpleEffectParser.ParseResult result = parser.parse("   ");
        assertNull(result.effect());
    }
}
