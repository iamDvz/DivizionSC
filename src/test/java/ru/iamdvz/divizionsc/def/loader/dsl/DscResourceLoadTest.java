package ru.iamdvz.divizionsc.def.loader.dsl;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.def.model.DefDefinition;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DscResourceLoadTest {

    @Test
    void compilesBundledScriptAndBossPacks() throws Exception {
        PluginConfig config = new PluginConfig(new YamlConfiguration());
        DscCompiler compiler = new DscCompiler(config);

        for (String resource : List.of("defs/defs-script.dsc", "defs/defs-boss.dsc")) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream, resource);
                String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                DscScript script = new DscParser().parse(source, resource);
                List<DefDefinition> defs = compiler.compile(script);
                assertFalse(defs.isEmpty(), resource);
            }
        }
    }
}
