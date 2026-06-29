package ru.iamdvz.divizionsc.def.loader;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.service.DefRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefLoaderErrorTest {

    @Test
    void compileError_isReportedNotThrown() {
        PluginConfig config = new PluginConfig(new Settings());
        DefRegistry registry = new DefRegistry();
        DefLoadReport report = new DefLoadReport();
        DefLoader loader = new DefLoader(null, config);

        int loaded = loader.loadDscSource("""
                def bad {
                  do @unknown_module
                }
                """, "test.dsc", registry, report);

        assertEquals(0, loaded);
        assertFalse(report.errors().isEmpty());
        assertTrue(registry.all().isEmpty());
    }

    @Test
    void parseError_isReportedNotThrown() {
        PluginConfig config = new PluginConfig(new Settings());
        DefRegistry registry = new DefRegistry();
        DefLoadReport report = new DefLoadReport();
        DefLoader loader = new DefLoader(null, config);

        int loaded = loader.loadDscSource("def broken {", "test.dsc", registry, report);

        assertEquals(0, loaded);
        assertFalse(report.errors().isEmpty());
    }
}
