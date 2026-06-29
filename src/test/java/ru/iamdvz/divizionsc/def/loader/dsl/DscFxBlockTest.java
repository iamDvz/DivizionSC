package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DscFxBlockTest {

    private final DscParser parser = new DscParser();

    @Test
    void parsesDetailedSphereFxBlock() {
        String source = """
                ability test {
                  cast {
                    fx {
                      shape sphere
                      particle heart
                      at self
                      offset 0,1,0
                      sphere {
                        radius 1.2
                        particles 20
                        y-offset 0.3
                        radius-increase 0.02
                      }
                      timing {
                        iterations 15
                        period 2
                        delay 0
                      }
                      base {
                        visible-range 64
                        auto-orient true
                        async false
                      }
                    }
                  }
                }
                """;

        EffectDefinition fx = compileFirst(source);
        assertEquals("effectlib", fx.type());
        assertEquals("SphereEffect", fx.text("class", ""));
        assertEquals("HEART", fx.text("particle", ""));
        assertEquals("caster", fx.text("at", ""));
        assertEquals(1.2, fx.number("radius", 0));
        assertEquals(20, fx.integer("particles", 0));
        assertEquals(15, fx.integer("iterations", 0));
        assertEquals(2, fx.integer("period", 0));
        assertEquals(64f, fx.number("visibleRange", 0));
        assertEquals(true, fx.data().get("autoOrient"));
        assertEquals(false, fx.data().get("asynchronous"));
        assertEquals("0,1,0", fx.text("relativeOffset", ""));
    }

    @Test
    void parsesHelixFxBlockWithRoute() {
        String source = """
                ability test {
                  cast {
                    fx >> target {
                      shape helix
                      particle flame
                      helix {
                        radius 1.5
                        strands 2
                        curve 5
                        particles 50
                        rotation 180
                      }
                      timing {
                        iterations 30
                        period 2
                      }
                    }
                  }
                }
                """;

        EffectDefinition fx = compileFirst(source);
        assertEquals("HelixEffect", fx.text("class", ""));
        assertEquals("target", fx.text("at", ""));
        assertEquals(1.5, fx.number("radius", 0));
        assertEquals(2, fx.integer("strands", 0));
        assertEquals(5f, fx.number("curve", 0));
    }

    @Test
    void parsesCircleAndLineShapes() {
        String source = """
                module vfx_pack {
                  effects {
                    fx {
                      shape circle
                      particle cloud
                      circle {
                        radius 3.0
                        particles 40
                        whole-circle true
                        enable-rotation true
                        orient true
                      }
                    }
                    fx {
                      class LineEffect
                      particle electric_spark
                      line {
                        length 8.0
                        particles 30
                        is-zig-zag true
                        zig-zags 4
                      }
                    }
                  }
                }
                """;

        List<DefDefinition> defs = new DscCompiler(new PluginConfig(new Settings()))
                .compile(parser.parse(source, "test.dsc"));
        EffectDefinition circle = defs.getFirst().effects().get(0);
        EffectDefinition line = defs.getFirst().effects().get(1);

        assertEquals("CircleEffect", circle.text("class", ""));
        assertEquals(true, circle.data().get("wholeCircle"));
        assertEquals(true, circle.data().get("enableRotation"));
        assertEquals(true, circle.data().get("orient"));

        assertEquals("LineEffect", line.text("class", ""));
        assertEquals(8.0, line.number("length", 0));
        assertEquals(true, line.data().get("isZigZag"));
        assertEquals(4, line.integer("zigZags", 0));
    }

    @Test
    void normalizerMapsShapeAliases() {
        assertEquals("HelixEffect", DscFxBlockNormalizer.shapeClass("spiral"));
        assertEquals("CircleEffect", DscFxBlockNormalizer.shapeClass("ring"));
        assertEquals("TornadoEffect", DscFxBlockNormalizer.shapeClass("tornado"));

        Map<String, Object> normalized = DscFxBlockNormalizer.normalize(Map.of(
                "shape", "sphere",
                "particle", "heart",
                "duration", "3s"
        ));
        assertEquals("SphereEffect", normalized.get("class"));
        assertEquals(60, normalized.get("duration"));
        assertFalse(normalized.containsKey("shape"));
    }

    private EffectDefinition compileFirst(String source) {
        List<DefDefinition> defs = new DscCompiler(new PluginConfig(new Settings()))
                .compile(parser.parse(source, "test.dsc"));
        return defs.getFirst().effects().getFirst();
    }
}
