package juranometria.meteorchart;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.meteor.MeteorShowers;
import juranometria.meteor.SyntheticShowers;
import juranometria.module.OverlayContribution;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart module (#716, R-12): only the selected shower becomes ink,
 * only while the chart time is inside its activity, and always with the
 * moment that defines the radiant. Over the invented pack and Sun.
 */
class MeteorShowerModuleTest {

    private static final MeteorShowers SERVICE = SyntheticShowers.service();
    private Observer now = new Observer(60.0, 10.0, SyntheticShowers.at(100.0));
    private boolean horizon;

    private MeteorShowerModule module() {
        return new MeteorShowerModule(() -> now, () -> SERVICE, () -> horizon);
    }

    @Test
    void nothingIsOfferedUntilAShowerIsSelected() {
        MeteorShowerModule module = module();
        assertTrue(module.contributedGeometry().isEmpty(),
                "a shower active at the chart time is not ink until it is selected");
        module.select("AAA");
        assertEquals(1, module.contributedGeometry().size());
    }

    @Test
    void theRadiantIsOfferedOnlyWhileTheChartTimeIsInsideTheActivity() {
        MeteorShowerModule module = module();
        module.select("AAA");
        now = now.at(SyntheticShowers.at(89.9));
        assertTrue(module.contributedGeometry().isEmpty(), "before the activity");
        assertEquals("AAA", module.selected(), "and the selection survives");
        now = now.at(SyntheticShowers.at(90.0));
        assertEquals(1, module.contributedGeometry().size());
        now = now.at(SyntheticShowers.at(110.0));
        assertTrue(module.contributedGeometry().isEmpty(), "after it");
    }

    @Test
    void theRadiantCarriesTheMomentAndThePlaceThatDefineIt() {
        MeteorShowerModule module = module();
        module.select("AAA");
        OverlayContribution.Radiant radiant = module.radiant().orElseThrow();
        assertEquals("meteor-shower.AAA", radiant.identity());
        assertEquals(now.instant(), radiant.moment().instant());
        assertEquals(60.0, radiant.moment().latitudeDegrees());
        assertEquals(10.0, radiant.moment().eastLongitudeDegrees());
        assertTrue(radiant.selectable(), "an occurrence a reader can select (R-15)");
        assertEquals(MeteorShowers.radiantAt(SERVICE.shower("AAA").orElseThrow(), 100.0).at(),
                radiant.at(), "the radiant for that chart time, not the maximum's");
        now = now.at(SyntheticShowers.at(104.0));
        assertFalse(radiant.at().equals(module.radiant().orElseThrow().at()),
                "another chart time, another place on the sky");
    }

    @Test
    void belowTheHorizonIsSaidOnlyWhileTheHorizonIsDrawn() {
        MeteorShowerModule module = module();
        module.select("AAA");
        // Find an hour of the active day at which the radiant is below this observer's horizon.
        Observer down = null;
        for (int minute = 0; minute < 1440 && down == null; minute += 10) {
            Observer candidate = now.at(SyntheticShowers.at(100.0).plusSeconds(minute * 60L));
            now = candidate;
            if (new LocalSky(candidate).altitudeDegrees(
                    module.radiant().orElseThrow().at()) < -1.0) {
                down = candidate;
            }
        }
        assertTrue(down != null, "the premise: this radiant sets at this latitude");
        now = down;
        horizon = false;
        assertFalse(module.radiant().orElseThrow().belowHorizon());
        horizon = true;
        assertTrue(module.radiant().orElseThrow().belowHorizon());
    }

    @Test
    void withoutAnObserverThereIsNoChartTimeAndSoNoRadiant() {
        MeteorShowerModule module = module();
        module.select("AAA");
        now = null;
        assertTrue(module.contributedGeometry().isEmpty());
    }

    @Test
    void selectingTellsListenersOnceAndRefusesAShowerThePackDoesNotCarry() {
        MeteorShowerModule module = module();
        List<String> told = new ArrayList<>();
        module.onChange(told::add);
        module.select("BBB");
        module.select("BBB");
        module.select(null);
        assertEquals(java.util.Arrays.asList("BBB", null), told);
        assertThrows(IllegalArgumentException.class, () -> module.select("ZZZ"));
    }

    @Test
    void aRadiantIsNeverBuiltWithoutItsMoment() {
        assertThrows(IllegalArgumentException.class, () -> new OverlayContribution.Radiant(
                "meteor-shower.AAA", "Alphaids", new juranometria.chart.SkyPosition(10, 10),
                false, null, false));
    }
}
