package juranometria.jovianchart;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.JovianSystemService.JupiterObservation;
import juranometria.tool.JovianChartGeometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Jovian chart module (issue #484, ruled on #482): hidden by default
 * and offering nothing while hidden; shown, Jupiter at the service's own
 * J2000 place with its true figure, its pole derived in the chart's
 * frame, and the ruled 6 px minimum; below the horizon only while the
 * horizon is drawn; nothing outside 1900-2100; Jupiter alone in 1900-1999.
 */
class JovianModuleTest {

    private static JovianSystemService service;

    @BeforeAll
    static void load() {
        service = JovianSystemService.load();
    }

    private static Observer oslo(String instant) {
        return new Observer(59.91, 10.75, Instant.parse(instant));
    }

    private static JovianModule module(Observer[] at, boolean[] horizon) {
        return new JovianModule(() -> at[0], () -> service, () -> horizon[0]);
    }

    @Test
    void hiddenByDefaultItOffersNothingAndNeverReadsTheService() {
        int[] loads = {0};
        JovianModule module = new JovianModule(() -> oslo("2026-12-11T22:45:00Z"),
                () -> {
                    loads[0]++;
                    return service;
                }, () -> true);
        assertFalse(module.showing());
        assertEquals(List.of(), module.contributedGeometry());
        assertEquals(0, loads[0], "the 53 MB pack is not read while the switch is off");
    }

    @Test
    void shownItOffersJupiterWhereTheServicePutsItWithItsTrueFigureAndTheRuledMinimum() {
        Observer at = oslo("2026-12-11T22:45:00Z");
        JovianModule module = module(new Observer[] {at}, new boolean[] {false});
        module.showing(true);
        List<OverlayContribution> offered = module.contributedGeometry();
        assertEquals(1, offered.size());
        OverlayContribution.OblateBody jupiter = (OverlayContribution.OblateBody) offered.get(0);
        JupiterObservation expected = service.observeJupiter(at);
        assertEquals(JovianModule.JUPITER, jupiter.identity());
        assertEquals(expected.astrometricJ2000(), jupiter.at());
        assertEquals(expected.equatorialDiameterArcseconds(), jupiter.equatorialDiameterArcseconds());
        assertEquals(expected.polarDiameterArcseconds(), jupiter.polarDiameterArcseconds());
        assertEquals(6.0, jupiter.minimumMarkPx(), "ruling 2: 6 px or true size, whichever is larger");
        assertEquals(expected.distanceKm(), jupiter.distanceKm());
        assertEquals(InkRole.BODY, jupiter.role());
        assertFalse(jupiter.belowHorizon(), "the horizon is not drawn");
    }

    @Test
    void thePoleIsTheFrameContractsNeverTheTablesOfDateAngle() {
        JovianModule module = module(new Observer[] {null}, new boolean[] {false});
        double widest = 0.0;
        for (int year = 1900; year <= 2100; year += 10) {
            Observer at = oslo(year + "-03-01T00:00:00Z");
            JupiterObservation j = service.observeJupiter(at);
            double jd = service.timeScales().tt(j.instant()).jdTt();
            double study = JovianChartGeometry.positionAngleJ2000(j.astrometricJ2000(),
                    JovianChartGeometry.pole(service.pack().constants(), jd));
            double production = service.poleAngleJ2000Degrees(j);
            assertEquals(study, production, 1e-9, "the study's derivation, at " + year);
            double turned = Math.abs(((production - j.poleAngleDegrees()) + 540.0) % 360.0 - 180.0);
            widest = Math.max(widest, turned);
        }
        assertTrue(widest > 0.1, "the of-date angle differs, so it is not what is drawn: " + widest);
        assertEquals(List.of(), module.contributedGeometry(), "hidden");
    }

    @Test
    void belowTheHorizonOnlyWhileTheHorizonIsDrawn() {
        // 12:20 UTC on 1 December 2026 at Oslo: Jupiter is below the horizon.
        Observer at = oslo("2026-12-01T12:20:00Z");
        assertTrue(service.observeJupiter(at).horizontal().altitudeDegrees() < 0.0);
        boolean[] horizon = {true};
        JovianModule module = module(new Observer[] {at}, horizon);
        module.showing(true);
        assertTrue(((OverlayContribution.OblateBody) module.contributedGeometry().get(0))
                .belowHorizon());
        horizon[0] = false;
        assertFalse(((OverlayContribution.OblateBody) module.contributedGeometry().get(0))
                .belowHorizon(), "with the horizon hidden it is drawn normally");
    }

    @Test
    void outsideTheNumbersYearsNothingIsDrawnAndInThe1900sJupiterIs() {
        Observer[] at = {oslo("1899-12-31T12:00:00Z")};
        JovianModule module = module(at, new boolean[] {false});
        module.showing(true);
        assertEquals(List.of(), module.contributedGeometry(), "before 1900");
        at[0] = oslo("2101-01-02T12:00:00Z");
        assertEquals(List.of(), module.contributedGeometry(), "after 2100");
        at[0] = oslo("1950-06-01T22:00:00Z");
        assertEquals(1, module.contributedGeometry().size(),
                "Jupiter has numbers from 1900; its moons, which do not, are #485's");
        at[0] = null;
        assertEquals(List.of(), module.contributedGeometry(), "no observer, nothing");
    }

    @Test
    void theSwitchTellsItsFollowersOfRealChangesOnly() {
        JovianModule module = module(new Observer[] {null}, new boolean[] {false});
        List<Boolean> heard = new ArrayList<>();
        module.onChange(heard::add);
        assertEquals(List.of(false), heard, "told at once");
        module.showing(true);
        module.showing(true);
        module.showing(false);
        assertEquals(List.of(false, true, false), heard);
        assertThrows(IllegalArgumentException.class, () -> module.onChange(null));
        assertThrows(IllegalArgumentException.class,
                () -> new JovianModule(null, () -> service, () -> true));
    }
}
