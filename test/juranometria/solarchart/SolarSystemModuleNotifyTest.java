package juranometria.solarchart;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The module tells every listener when a body's visibility changes
 * (Sprint 42, issue #458, ruled on #457, 5A): it is the one authority
 * for the choice, so a menu item, a Controller box and the chart
 * follow it from here, and no control surface can bypass another. A
 * listener is told the current state at once, hears real changes
 * only, and the Sun's listeners never hear the Moon's.
 */
class SolarSystemModuleNotifyTest {

    private static final SolarSystemService SERVICE = SolarSystemService.load();

    private static SolarSystemModule module() {
        Observer oslo = new Observer(59.91, 10.75, Instant.parse("2026-06-21T10:00:00Z"));
        return new SolarSystemModule(() -> oslo, () -> SERVICE, () -> true);
    }

    @Test
    void aListenerIsToldAtOnceAndOnEveryRealChange() {
        SolarSystemModule module = module();
        List<Boolean> sun = new ArrayList<>();
        List<Boolean> moon = new ArrayList<>();
        module.onSunChange(sun::add);
        module.onMoonChange(moon::add);
        assertEquals(List.of(false), sun, "told the current state at once");
        assertEquals(List.of(false), moon);

        module.sunShowing(true);
        module.sunShowing(true);
        assertEquals(List.of(false, true), sun, "a change is heard once; the same again is not");
        assertEquals(List.of(false), moon, "the Sun's change is not the Moon's");

        module.moonShowing(true);
        module.moonShowing(false);
        assertEquals(List.of(false, true, false), moon);
        assertEquals(List.of(false, true), sun);
    }

    @Test
    void theModuleStaysTheAuthorityWhateverSwitchedIt() {
        SolarSystemModule module = module();
        boolean[] seen = new boolean[1];
        module.onSunChange(shown -> seen[0] = shown);
        // A journey, a menu item or a box: all reach the module, and
        // every listener hears the one answer.
        module.sunShowing(true);
        assertEquals(true, seen[0]);
        assertEquals(true, module.sunShowing());
        assertThrows(IllegalArgumentException.class, () -> module.onSunChange(null));
        assertThrows(IllegalArgumentException.class, () -> module.onMoonChange(null));
    }
}
