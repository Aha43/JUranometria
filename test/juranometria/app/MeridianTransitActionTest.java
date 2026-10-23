package juranometria.app;

import java.time.Instant;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.chart.Selection;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.module.TestChartServices;
import juranometria.sky.Observer;
import juranometria.sky.SiderealSolver;
import juranometria.sky.SkyFrame;
import juranometria.solar.SolarSystemService;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Set chart time to its next meridian transit (Sprint 54, issue #612
 * absorbing #569, ruling P4), as the application wires it: the selected
 * star's or body's next upper culmination installed as the chart time
 * through the meridian module, the altitude stated and <em>below the
 * horizon</em> named; nothing installed when the selection has no place
 * of date. Installations happen on the event thread.
 */
class MeridianTransitActionTest {

    private static final Instant AUGUST = Instant.parse("2026-08-15T06:00:00Z");
    private static final Observer OSLO = new Observer(59.91, 10.75, AUGUST);
    private static final double TWO_SECONDS_DEGREES = 2.0 * 360.0 / 86400.0;
    private static SolarSystemService service;

    @BeforeAll
    static void load() {
        service = SolarSystemService.load();
    }

    private static void edt(Runnable work) throws Exception {
        SwingUtilities.invokeAndWait(work);
    }

    private static MeridianModule attached() {
        MeridianModule module = new MeridianModule(OSLO);
        module.attach(new TestChartServices());
        return module;
    }

    private static MeridianTransitAction action(MeridianModule module, String language) {
        return new MeridianTransitAction(module, () -> service,
                () -> { throw new AssertionError("Jupiter's pack is not read for this"); },
                () -> { throw new AssertionError("Saturn's pack is not read for this"); },
                InterfaceText.forLanguage(language));
    }

    private static double signed(double degrees) {
        double folded = SkyFrame.normalise(degrees);
        return folded > 180.0 ? folded - 360.0 : folded;
    }

    @Test
    void aStarSetsTheChartTimeAndStatesItsAltitudeBelowTheHorizon() throws Exception {
        MeridianModule module = attached();
        SkyPosition j2000 = new SkyPosition(100.0, -60.0);
        Selection star = new Selection.Object(Selection.Object.Kind.STAR, "test-star", j2000);
        String[] said = new String[1];
        edt(() -> said[0] = action(module, "en").apply(star));

        Instant at = module.observer().instant();
        assertFalse(at.isBefore(AUGUST), "at or after the chart time");
        assertTrue(said[0].startsWith("Chart time set to " + juranometria.ui.solar
                .SolarTableSession.shown(at) + " UTC: on the meridian at "), said[0]);
        assertTrue(said[0].endsWith("°, below the horizon"), said[0]);
        SkyPosition ofDate = SkyFrame.toOfDate(j2000, SkyFrame.julianDate(at));
        double lst = SiderealSolver.localSiderealTimeDegrees(at, OSLO.eastLongitudeDegrees());
        assertTrue(Math.abs(signed(lst - ofDate.raDegrees())) < TWO_SECONDS_DEGREES,
                "the right ascension of date is on the meridian at the chart time now");
        assertEquals(OSLO.latitudeDegrees(), module.observer().latitudeDegrees(),
                "the place is untouched");
        assertEquals(1, module.context().current().revision(), "one installation");
    }

    @Test
    void theMoonsOccurrenceIsSolvedOnTheServicesTopocentricPlace() throws Exception {
        MeridianModule module = attached();
        Selection moon = new Selection.Occurrence("moon", AUGUST, OSLO.latitudeDegrees(),
                OSLO.eastLongitudeDegrees(), new SkyPosition(0.0, 0.0));
        String[] said = new String[1];
        edt(() -> said[0] = action(module, "nb-NO").apply(moon));
        Instant at = module.observer().instant();
        assertTrue(said[0].startsWith("Karttid satt til "), said[0]);
        SkyPosition then = ((SolarSystemService.MoonObservation) service.observe(
                SolarSystemService.Body.MOON, OSLO.at(at))).apparentOfDate();
        double lst = SiderealSolver.localSiderealTimeDegrees(at, OSLO.eastLongitudeDegrees());
        assertTrue(Math.abs(signed(lst - then.raDegrees())) < TWO_SECONDS_DEGREES,
                "re-read at the chart time now, the Moon stands on the meridian");
    }

    @Test
    void nothingAndTheEmptySkyAndAnUnknownBodyInstallNothing() throws Exception {
        MeridianModule module = attached();
        MeridianTransitAction action = action(module, "en");
        String[] said = new String[3];
        edt(() -> {
            said[0] = action.apply(Selection.NOTHING);
            said[1] = action.apply(new Selection.EmptySky(new SkyPosition(10.0, 10.0)));
            said[2] = action.apply(new Selection.Occurrence("comet", AUGUST, 59.91, 10.75,
                    new SkyPosition(10.0, 10.0)));
        });
        for (String s : said) {
            assertEquals("No place of date is known for this selection; the chart time"
                    + " is unchanged.", s);
        }
        assertEquals(AUGUST, module.observer().instant());
        assertEquals(0, module.context().current().revision(), "nothing installed");
    }

    @Test
    void theInspectorOffersTheActionForAnObjectOrABodyAndSaysWhatItDid() throws Exception {
        MeridianModule module = attached();
        juranometria.chart.SelectionModel selection = new juranometria.chart.SelectionModel();
        InspectorPanel[] panel = new InspectorPanel[1];
        edt(() -> {
            panel[0] = new InspectorPanel(selection, () -> null, () -> null, s -> { },
                    InterfaceText.forLanguage("en"));
            assertFalse(panel[0].transitButton().isShowing(), "not offered until the application offers it");
            panel[0].offerMeridianTransit(action(module, "en"));
        });
        assertTrue(panel[0].transitButton().getParent().isVisible(), "offered: the box is shown");
        assertFalse(panel[0].transitButton().isEnabled(), "nothing selected: nothing to follow");
        assertEquals("", panel[0].transitText());
        assertEquals("Set chart time to its next meridian transit", panel[0].transitButton().getText());

        edt(() -> selection.selectOccurrence(new Selection.Occurrence("moon", AUGUST,
                OSLO.latitudeDegrees(), OSLO.eastLongitudeDegrees(), new SkyPosition(0.0, 0.0))));
        assertTrue(panel[0].transitButton().isEnabled(), "a drawn body has a place of date");
        edt(panel[0].transitButton()::doClick);
        assertTrue(panel[0].transitText().startsWith("Chart time set to "), panel[0].transitText());
        assertTrue(module.observer().instant().isAfter(AUGUST), "and the chart time moved");

        edt(() -> selection.selectEmptySky(new SkyPosition(10.0, 10.0)));
        assertFalse(panel[0].transitButton().isEnabled(), "empty sky has no place of date");
        edt(selection::clear);
        assertEquals("", panel[0].transitText(), "nothing selected: the answer is withdrawn");
    }
}
