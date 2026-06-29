package ru.iamdvz.divizionsc.def.loader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefFileFilterTest {

    @Test
    void acceptsDscFiles() {
        assertTrue(DefFileFilter.accepts("defs-combat.dsc"));
        assertTrue(DefFileFilter.accepts("defs-MY-SPELLS.DSC"));
    }

    @Test
    void rejectsYamlAndOtherNames() {
        assertFalse(DefFileFilter.accepts("defs-combat.yml"));
        assertFalse(DefFileFilter.accepts("defs-combat.yaml"));
        assertFalse(DefFileFilter.accepts("examples.yml"));
        assertFalse(DefFileFilter.accepts("advanced.yml"));
        assertFalse(DefFileFilter.accepts("combat.dsc"));
        assertFalse(DefFileFilter.accepts(null));
    }

    @Test
    void acceptsJarEntries() {
        assertTrue(DefFileFilter.acceptsJarEntry("defs/defs-boss.dsc"));
        assertFalse(DefFileFilter.acceptsJarEntry("defs/defs-boss.yml"));
        assertFalse(DefFileFilter.acceptsJarEntry("other/defs-boss.dsc"));
    }
}
