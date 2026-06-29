package ru.iamdvz.divizionsc.def.loader.simple;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import java.util.List;
import java.util.Map;

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

    @Test
    void parsesModelEngineVfxLine() {
        SimpleEffectParser.ParseResult result = parser.parse("vfx ground_slam slash 1.5 60 target follow glow");
        EffectDefinition effect = result.effect();
        assertNotNull(effect);
        assertEquals("vfx", effect.type());
        assertEquals("ground_slam", effect.text("model", ""));
        assertEquals(1.5, effect.number("model-scale", 0));
        assertEquals(60, effect.integer("remove-delay", 0));
        assertEquals("target", effect.text("at", ""));
        assertEquals(true, effect.bool("follow-target", false));
        assertEquals(true, effect.bool("glowing", false));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> animations = (List<Map<String, Object>>) effect.data().get("animations");
        assertEquals("slash", animations.getFirst().get("name"));
    }

    @Test
    void parsesFullEffectNames() {
        assertEquals("velocity", parser.parse("knockback 1.2").effect().type());
        assertEquals("damage", parser.parse("damage 5 target").effect().type());
        assertEquals(5.0, parser.parse("damage 5 target").effect().number("amount", 0));
        assertEquals("potion", parser.parse("potioneffect speed 5s 1").effect().type());
        assertEquals("stun", parser.parse("stun 2s").effect().type());
        assertEquals(40, parser.parse("stun 2s").effect().integer("duration", 0));
        assertEquals("give-money", parser.parse("give-money 100").effect().type());
        assertEquals(100.0, parser.parse("give-money 100").effect().number("amount", 0));
        assertEquals("particle_projectile", parser.parse("particle_projectile flame 1.0 20").effect().type());
    }

    @Test
    void parsesAbilityAliasAsDefCall() {
        EffectDefinition effect = parser.parse("ability spark").effect();
        assertNotNull(effect);
        assertEquals("def", effect.type());
        assertEquals("spark", effect.text("def", ""));
    }

    @Test
    void parsesMegAlias() {
        SimpleEffectParser.ParseResult result = parser.parse("meg aura idle self loop");
        EffectDefinition effect = result.effect();
        assertNotNull(effect);
        assertEquals("vfx", effect.type());
        assertEquals("aura", effect.text("model", ""));
        assertEquals("self", effect.text("at", ""));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> animations = (List<Map<String, Object>>) effect.data().get("animations");
        assertEquals(true, animations.getFirst().get("loop"));
    }
}
