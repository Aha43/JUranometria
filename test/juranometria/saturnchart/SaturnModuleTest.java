package juranometria.saturnchart;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import juranometria.module.OverlayContribution;
import juranometria.sky.Observer;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.SaturnObservation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Saturn module (issue #579, the owner's rulings on #578): hidden by
 * default and reading nothing; shown, it offers Saturn as a ringed body
 * where the service puts it, with the service's figure, the pole derived
 * in the chart's frame, the planetocentric ring opening and the ruled
 * ring radii; below the horizon only while the horizon is drawn; nothing
 * outside the numbers' years; and a switch that tells its followers of
 * real changes only.
 */
class SaturnModuleTest {

    /** The contract's card instant (#574): the 2026 Oslo day nearest the Earth. */
    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-10-05T00:00:00Z"));
    private static SaturnSystemService service;

    @BeforeAll
    static void load() {
        service = SaturnSystemService.load();
    }

    @Test
    void hiddenByDefaultItOffersNothingAndNeverReadsTheService() {
        AtomicInteger reads = new AtomicInteger();
        SaturnModule module = new SaturnModule(() -> OSLO, () -> {
            reads.incrementAndGet();
            return service;
        }, () -> false);
        assertFalse(module.showing());
        assertEquals(List.of(), module.contributedGeometry());
        assertEquals(0, reads.get(), "hidden, the module does not touch the pack");
        assertEquals(1, module.timesAsked());
    }

    @Test
    void shownItOffersSaturnWhereTheServicePutsItWithItsFigureRingsAndTheRuledMinimum() {
        SaturnModule module = new SaturnModule(() -> OSLO, () -> service, () -> false);
        module.showing(true);
        List<OverlayContribution> offered = module.contributedGeometry();
        assertEquals(1, offered.size(), "Saturn alone in this pull request: the moons follow");
        OverlayContribution.RingedBody saturn = (OverlayContribution.RingedBody) offered.get(0);
        SaturnObservation s = service.observeSaturn(OSLO);
        assertEquals(SaturnModule.SATURN, saturn.identity());
        assertEquals("Saturn", saturn.accessibleName());
        assertEquals(s.astrometricJ2000(), saturn.at(), "the service's astrometric J2000 place");
        assertEquals(s.equatorialDiameterArcseconds(), saturn.equatorialDiameterArcseconds());
        assertEquals(s.polarDiameterArcseconds(), saturn.polarDiameterArcseconds(),
                "the polar diameter at the ring opening, the contract's");
        assertEquals(s.ringOpeningDegrees(), saturn.ringOpeningDegrees(),
                "B, planetocentric, never Horizons' planetodetic latitude");
        assertEquals(s.unlitFaceSeen(), saturn.unlitFaceSeen());
        assertEquals(s.ringRadiiKm(), saturn.ringRadiiKm(), "the ruled four edges");
        assertEquals(s.distanceKm(), saturn.distanceKm());
        assertEquals(6.0, saturn.minimumMarkPx(), "Jupiter's ruled minimum (ruling 1 on #578)");
        assertFalse(saturn.belowHorizon());
        assertTrue(saturn.selectable(), "a selectable occurrence (#513)");
        assertEquals(OSLO.instant(), saturn.moment().instant());
    }

    @Test
    void thePoleIsTheFrameContractsNeverTheTablesOfDateAngle() {
        SaturnModule module = new SaturnModule(() -> OSLO, () -> service, () -> false);
        module.showing(true);
        OverlayContribution.RingedBody saturn =
                (OverlayContribution.RingedBody) module.contributedGeometry().get(0);
        SaturnObservation s = service.observeSaturn(OSLO);
        assertEquals(service.poleAngleJ2000Degrees(s), saturn.poleAngleDegrees(),
                "derived in J2000 at Saturn's place");
        assertTrue(Math.abs(saturn.poleAngleDegrees() - s.poleAngleDegrees()) > 1e-6,
                "and not the table's of-date angle: " + saturn.poleAngleDegrees()
                        + " against " + s.poleAngleDegrees());
        // Over the century the two frames' angles part by more than half a
        // degree (the #578 study, section C): the of-date angle is never drawn.
        double turn = 0.0;
        for (int year = 2000; year <= 2100; year += 10) {
            SaturnObservation then = service.observeSaturnGeocentric(
                    Instant.parse(year + "-03-01T00:00:00Z"));
            turn = Math.max(turn, Math.abs(service.poleAngleJ2000Degrees(then)
                    - then.poleAngleDegrees()));
        }
        assertTrue(turn > 0.1, "the frames part over the century: " + turn);
    }

    @Test
    void belowTheHorizonOnlyWhileTheHorizonIsDrawn() {
        // The first December 2026 evening at Oslo with Saturn just set,
        // found by the service rather than typed.
        Observer low = null;
        for (Instant t = Instant.parse("2026-12-01T12:00:00Z"); low == null;
                t = t.plusSeconds(600)) {
            double alt = service.observeSaturn(OSLO.at(t)).horizontal().altitudeDegrees();
            if (alt < -1.0 && alt > -4.0) {
                low = OSLO.at(t);
            }
        }
        Observer at = low;
        boolean[] horizon = {false};
        SaturnModule module = new SaturnModule(() -> at, () -> service, () -> horizon[0]);
        module.showing(true);
        assertFalse(((OverlayContribution.RingedBody) module.contributedGeometry().get(0))
                .belowHorizon(), "with no horizon drawn the celestial chart draws it normally");
        horizon[0] = true;
        assertTrue(((OverlayContribution.RingedBody) module.contributedGeometry().get(0))
                .belowHorizon(), "below a drawn horizon it says so");
    }

    @Test
    void outsideTheNumbersYearsNothingIsDrawn() {
        SaturnModule module = new SaturnModule(
                () -> OSLO.at(Instant.parse("1899-12-31T00:00:00Z")), () -> service, () -> false);
        module.showing(true);
        assertEquals(List.of(), module.contributedGeometry(), "1899: the table says why");
        SaturnModule inRange = new SaturnModule(
                () -> OSLO.at(Instant.parse("1950-06-01T00:00:00Z")), () -> service, () -> false);
        inRange.showing(true);
        assertEquals(1, inRange.contributedGeometry().size(), "1950: Saturn is drawn");
        SaturnModule nobody = new SaturnModule(() -> null, () -> service, () -> false);
        nobody.showing(true);
        assertEquals(List.of(), nobody.contributedGeometry(), "no observer, nothing");
    }

    @Test
    void theSwitchTellsItsFollowersOfRealChangesOnly() {
        SaturnModule module = new SaturnModule(() -> OSLO, () -> service, () -> false);
        List<Boolean> heard = new ArrayList<>();
        module.onChange(heard::add);
        assertEquals(List.of(false), heard, "told the current state at once");
        module.showing(true);
        module.showing(true);
        module.showing(false);
        assertEquals(List.of(false, true, false), heard);
    }
}
