package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.def.loader.DefLoader;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.service.DefRegistry;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DscAdvancedLoadTest {

    private static final String ADVANCED = "defs/defs-advanced.dsc";

    @Test
    void compilesAdvancedExamples() throws Exception {
        PluginConfig config = new PluginConfig(new Settings());
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(ADVANCED)) {
            assertNotNull(stream, ADVANCED);
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            List<DefDefinition> defs = new DscCompiler(config).compile(new DscParser().parse(source, ADVANCED));
            assertFalse(defs.isEmpty());
        }
    }

    @Test
    void loadsAdvancedThroughDefLoader() throws Exception {
        PluginConfig config = new PluginConfig(new Settings());
        DefRegistry registry = new DefRegistry();
        DefLoadReport report = new DefLoadReport();
        DefLoader loader = new DefLoader(null, config);

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(ADVANCED)) {
            assertNotNull(stream);
            int loaded = loader.loadStream(stream, "defs-advanced.dsc", registry, report);
            assertTrue(loaded >= 10, "loaded=" + loaded);
            assertTrue(registry.find("arcane_meteor").isPresent());
            assertTrue(registry.find("boss_enrage").isPresent());
            assertTrue(registry.find("passive_lifesteal").isPresent());
            assertTrue(report.errors().isEmpty(), report.errors().toString());
        }
    }
}
