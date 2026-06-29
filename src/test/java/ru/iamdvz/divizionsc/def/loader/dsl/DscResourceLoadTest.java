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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DscResourceLoadTest {

    private static final String EXAMPLES = "defs/defs-examples.dsc";
    private static final String FX_EXAMPLES = "defs/defs-fx-examples.dsc";

    @Test
    void compilesBundledExamples() throws Exception {
        compileResource(EXAMPLES);
    }

    @Test
    void compilesBundledFxExamples() throws Exception {
        compileResource(FX_EXAMPLES);
    }

    @Test
    void loadsBundledExamplesThroughDefLoader() throws Exception {
        PluginConfig config = new PluginConfig(new Settings());
        DefRegistry registry = new DefRegistry();
        DefLoadReport report = new DefLoadReport();
        DefLoader loader = new DefLoader(null, config);

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(EXAMPLES)) {
            assertNotNull(stream);
            int loaded = loader.loadStream(stream, "defs-examples.dsc", registry, report);
            assertTrue(loaded >= 10);
            assertTrue(registry.find("heal").isPresent());
            assertTrue(registry.find("fireball").isPresent());
            assertTrue(registry.find("passive_thorns").isPresent());
            assertTrue(registry.find("instant_smite").isPresent());
            assertTrue(report.errors().isEmpty());
        }
    }

    private void compileResource(String resource) throws Exception {
        PluginConfig config = new PluginConfig(new Settings());
        DefLoader loader = new DefLoader(null, config);
        DefLoadReport report = new DefLoadReport();

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            int loaded = loader.loadDscSource(source, resource, new DefRegistry(), report);
            assertTrue(loaded > 0, resource);
            assertTrue(report.errors().isEmpty(), report.errors().toString());
        }
    }
}
