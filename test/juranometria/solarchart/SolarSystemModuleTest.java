package juranometria.solarchart;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.module.TestChartServices;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.SunObservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Solar System module's contract (Sprint 37, issue #415): it
 * contributes the Sun as a body at the tables' own position and size,
 * reads Place and Time's observer only when asked, dims below the
 * horizon only while the horizon is drawn, and owns no clock, no
 * observer and no copy of either.
 */
class SolarSystemModuleTest {

    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-03-20T14:45:53Z"));
    private static final SolarSystemService SERVICE = SolarSystemService.load();

    private static final class Rig {
        Observer observer = OSLO;
        boolean horizonDrawn = true;
        int observerReads;
        int serviceReads;
        final SolarSystemModule module = new SolarSystemModule(() -> {
            observerReads++;
            return observer;
        }, () -> {
            serviceReads++;
            return SERVICE;
        }, () -> horizonDrawn);
    }

    private static OverlayContribution.Body sun(List<OverlayContribution> offered) {
        assertEquals(1, offered.size(), "one body: the Sun");
        return (OverlayContribution.Body) offered.get(0);
    }

    @Test
    void hiddenItContributesNothingAndReadsNothing() {
        Rig rig = new Rig();
        assertFalse(rig.module.sunShowing(), "hidden until switched on");
        assertEquals(List.of(), rig.module.contributedGeometry());
        assertEquals(0, rig.observerReads, "no observer read for a hidden Sun");
        assertEquals(0, rig.serviceReads, "and the pack is never loaded for it");
    }

    @Test
    void shownItContributesTheSunWhereTheTablesPutIt() {
        Rig rig = new Rig();
        rig.module.sunShowing(true);
        OverlayContribution.Body sun = sun(rig.module.contributedGeometry());
        SunObservation expected = (SunObservation) SERVICE.observe(Body.SUN, OSLO);
        assertEquals(SolarSystemModule.SUN, sun.identity());
        assertEquals(InkRole.BODY, sun.role());
        assertEquals(expected.astrometricJ2000(), sun.at(),
                "the topocentric astrometric J2000 position, the table's");
        assertEquals(expected.angularDiameterArcseconds(),
                sun.angularDiameterArcseconds(), 0.0, "the true diameter");
        assertTrue(sun.lit() == null, "the Sun is lit entirely");
        assertTrue(expected.horizontal().altitudeDegrees() > 0.0,
                "at the equinox instant the Sun is up at Oslo");
        assertFalse(sun.belowHorizon());
    }

    @Test
    void shownTheMoonCarriesHowItIsLitExactlyAsTheTableStatesIt() {
        Rig rig = new Rig();
        rig.module.moonShowing(true);
        List<OverlayContribution> offered = rig.module.contributedGeometry();
        assertEquals(1, offered.size(), "the Moon's switch offers the Moon alone");
        OverlayContribution.Body moon = (OverlayContribution.Body) offered.get(0);
        juranometria.solar.SolarSystemService.MoonObservation expected =
                (juranometria.solar.SolarSystemService.MoonObservation)
                        SERVICE.observe(Body.MOON, OSLO);
        assertEquals(SolarSystemModule.MOON, moon.identity());
        assertEquals(expected.astrometricJ2000(), moon.at());
        assertEquals(expected.angularDiameterArcseconds(),
                moon.angularDiameterArcseconds(), 0.0);
        assertEquals(expected.distanceKm(), moon.distanceKm(), 0.0);
        assertEquals(expected.illuminatedFraction(), moon.lit().illuminatedFraction(), 0.0,
                "k, the table's");
        assertEquals(expected.brightLimbAngleDegrees(),
                moon.lit().brightLimbAngleDegrees(), 0.0, "χ, the table's, never recomputed");
        assertEquals(expected.phaseAngleDegrees(), moon.lit().phaseAngleDegrees(), 0.0,
                "i, the table's");
        rig.module.sunShowing(true);
        List<OverlayContribution> both = rig.module.contributedGeometry();
        assertEquals(2, both.size());
        OverlayContribution.Body sun = (OverlayContribution.Body) both.get(0);
        assertTrue(sun.distanceKm() > moon.distanceKm(),
                "the Sun is the farther, so the page paints it first");
    }

    @Test
    void belowTheHorizonIsSaidOnlyWhileTheHorizonIsDrawn() {
        Rig rig = new Rig();
        rig.module.sunShowing(true);
        rig.observer = OSLO.at(Instant.parse("2026-03-20T21:33:00Z"));
        SunObservation night = (SunObservation) SERVICE.observe(Body.SUN, rig.observer);
        assertTrue(night.horizontal().altitudeDegrees() < 0.0, "night at Oslo");
        assertTrue(sun(rig.module.contributedGeometry()).belowHorizon(),
                "with the horizon drawn, the Sun is below it");
        rig.horizonDrawn = false;
        assertFalse(sun(rig.module.contributedGeometry()).belowHorizon(),
                "with the horizon hidden, the celestial chart draws it normally");
    }

    @Test
    void theObserverIsReadWhenAskedAndNeverKept() {
        Rig rig = new Rig();
        rig.module.sunShowing(true);
        OverlayContribution.Body first = sun(rig.module.contributedGeometry());
        assertEquals(1, rig.observerReads);
        rig.observer = new Observer(-0.18, -78.5, OSLO.instant());
        OverlayContribution.Body second = sun(rig.module.contributedGeometry());
        assertEquals(2, rig.observerReads, "one read per contribution");
        assertTrue(first.at().separationDegrees(second.at()) * 3600.0 > 3.0,
                "Quito's Sun differs from Oslo's by its parallax");
        rig.observer = null;
        assertEquals(List.of(), rig.module.contributedGeometry(),
                "no observer, no Sun: Place and Time is not attached");
    }

    @Test
    void attachedItIsPulledByTheChartAndWithdrawnOnDetach() {
        Rig rig = new Rig();
        TestChartServices services = new TestChartServices();
        rig.module.attach(services);
        assertTrue(rig.module.attached());
        assertThrows(IllegalStateException.class, () -> rig.module.attach(services));
        rig.module.sunShowing(true);
        assertEquals(1, services.redraws, "a switch asks for a redraw");
        List<OverlayRegistry.Owned> collected = services.overlays.collect();
        assertEquals(1, collected.size());
        assertEquals(SolarSystemModule.ID, collected.get(0).moduleId());
        assertTrue(rig.module.timesAsked() >= 1, "pulled when collected");
        rig.module.detach();
        assertFalse(rig.module.attached());
        assertEquals(List.of(), services.overlays.collect(),
                "detached, it offers nothing");
    }

    @Test
    void nullsAreRefusedByName() {
        assertThrows(IllegalArgumentException.class,
                () -> new SolarSystemModule(null, () -> SERVICE, () -> true));
        assertThrows(IllegalArgumentException.class,
                () -> new SolarSystemModule(() -> OSLO, null, () -> true));
        assertThrows(IllegalArgumentException.class,
                () -> new SolarSystemModule(() -> OSLO, () -> SERVICE, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Rig().module.attach(null));
    }

    @Test
    void theModuleOwnsNoClockAndNoStore() throws Exception {
        String code = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/juranometria/solarchart/SolarSystemModule.java"));
        for (String forbidden : List.of("Instant.now", "currentTimeMillis",
                "Clock.", "Preferences", "java.io.File", "java.net", "javax.swing",
                "java.awt")) {
            assertFalse(code.contains(forbidden), "the module mentions " + forbidden);
        }
    }
}
