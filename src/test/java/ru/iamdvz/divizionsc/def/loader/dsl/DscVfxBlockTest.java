package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DscVfxBlockTest {

    private final DscParser parser = new DscParser();

    @Test
    void parsesFullVfxBlockLikeMagicSpells() {
        String source = """
                ability meg_flamethrower {
                  meta { key: cmd }
                  cast {
                    projectile(SNOWBALL, speed=0.1) {
                      tick {
                        vfx {
                          position projectile
                          model-name vfx_flamethrower
                          remove-delay 0
                          bones {
                            flame {
                              tint ffaa00
                              glow ff6600
                            }
                          }
                          animations {
                            skill {
                              delay 0
                              loop true
                              speed 1.0
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        DscScript script = parser.parse(source, "test.dsc");
        PluginConfig config = new PluginConfig(new Settings());
        List<DefDefinition> defs = new DscCompiler(config).compile(script);

        assertEquals(1, defs.size());
        EffectDefinition projectile = defs.getFirst().effects().getFirst();
        assertEquals("projectile", projectile.type());
        assertEquals(0.1, projectile.number("speed", 0));

        List<EffectDefinition> onTick = projectile.children("on_tick");
        assertEquals(1, onTick.size());

        EffectDefinition vfx = onTick.getFirst();
        assertEquals("vfx", vfx.type());
        assertEquals("projectile", vfx.text("at", ""));
        assertEquals("vfx_flamethrower", vfx.text("model-name", ""));
        assertEquals(0, vfx.integer("remove-delay", -99));

        @SuppressWarnings("unchecked")
        Map<String, Object> bones = (Map<String, Object>) vfx.data().get("bones");
        assertNotNull(bones);
        @SuppressWarnings("unchecked")
        Map<String, Object> flame = (Map<String, Object>) bones.get("flame");
        assertEquals("ffaa00", flame.get("tint"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> animations = (List<Map<String, Object>>) vfx.data().get("animations");
        assertEquals("skill", animations.getFirst().get("name"));
        assertEquals(true, animations.getFirst().get("loop"));
    }

    @Test
    void parsesStandaloneVfxBlock() {
        String source = """
                ability test {
                  cast {
                    vfx {
                      model-name slash_effect
                      position target
                      animations {
                        slash {
                          speed 1.2
                        }
                      }
                    }
                  }
                }
                """;

        DscScript script = parser.parse(source, "test.dsc");
        EffectDefinition vfx = new DscCompiler(new PluginConfig(new Settings()))
                .compile(script).getFirst().effects().getFirst();
        assertEquals("vfx", vfx.type());
        assertEquals("target", vfx.text("at", ""));
        assertTrue(vfx.data().containsKey("animations"));
    }
}
