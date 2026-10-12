package juranometria.ui.solar;

import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.VenusService;
import juranometria.solar.VenusService.VenusObservation;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Venus table (issue #502), headless: the Sun's and the Moon's shell
 * over Venus's eleven columns, in the vocabulary ruled on #500 - what it
 * shows at the named cases, what it says in each language, its range and
 * refusals, its two hosts over one session, and what it never offers:
 * built without a layer here, no chart switch and no Centre on chart
 * (the application gives both since #514).
 */
class VenusTableTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    /** Espenak's 2026 greatest eastern elongation, to the hour (venus-events.txt). */
    private static final Instant ELONGATION = Instant.parse("2026-08-15T06:00:00Z");
    /** Espenak's 2012 inferior conjunction, to the hour: the transit (venus-events.txt). */
    private static final Instant TRANSIT = Instant.parse("2012-06-06T01:00:00Z");
    /** Espenak's 2026 superior conjunction, to the hour (venus-events.txt). */
    private static final Instant SUPERIOR = Instant.parse("2026-01-06T16:00:00Z");
    private static SolarSystemService service;

    @BeforeAll
    static void load() {
        service = SolarSystemService.load();
    }

    private static Observer oslo(Instant at) {
        return new Observer(59.91, 10.75, at);
    }

    private static SolarTableDialog.Content content(Observer observer, InterfaceText said) {
        return SolarTableDialog.content(() -> observer, service, said, SolarTable.venus());
    }

    private static String cell(SolarTableDialog.Content c, int row, int column) {
        return String.valueOf(c.model.getValueAt(row, column));
    }

    private static List<String> columnNames(SolarTableModel model) {
        List<String> names = new ArrayList<>();
        for (int c = 0; c < model.getColumnCount(); c++) {
            names.add(model.getColumnName(c));
        }
        return names;
    }

    @Test
    void theShellIsTheMoonsAndTheColumnsAreVenussAsRuled() {
        SolarTableDialog.Content c = content(oslo(ELONGATION), EN);
        assertEquals(List.of("Instant (UTC)", "Right ascension (J2000)", "Declination (J2000)",
                "Altitude (no refraction)", "Azimuth (from north through east)", "Distance",
                "Apparent diameter (solid body)", "Illuminated", "Phase",
                "Elongation from the Sun", "Lit side"), columnNames(c.model));
        assertEquals("venusTable", c.table.getName());
        assertEquals("Venus's computed quantities",
                c.table.getAccessibleContext().getAccessibleName());
        assertTrue(c.model.columnExplanation(6).contains("solid body")
                        && c.model.columnExplanation(6).contains("1.22 %")
                        && c.model.columnExplanation(6).contains("not drawn"),
                "the solid-body diameter, the cloud deck stated and never drawn (ruling 4): "
                        + c.model.columnExplanation(6));
        assertTrue(c.model.columnExplanation(9).contains("right ascension")
                        && c.model.columnExplanation(9).contains("does not announce a conjunction"),
                "the side by right ascension, naming no conjunction (ruling 3)");
        assertTrue(c.timeNote.getText().contains("below 8″ through 2100"),
                "the time note states the measured after-2027 cost: " + c.timeNote.getText());
    }

    @Test
    void theGreatestEasternElongationReadsAsTheServiceAnswers() {
        SolarTableDialog.Content c = content(oslo(ELONGATION), EN);
        VenusObservation o = (VenusObservation) service.observe(Body.VENUS, oslo(ELONGATION));
        assertEquals(1, c.model.getRowCount());
        assertEquals(SunTableFormat.minute(ELONGATION), cell(c, 0, 0));
        assertEquals(SunTableFormat.hms(o.astrometricJ2000()), cell(c, 0, 1));
        assertEquals(SunTableFormat.dms(o.astrometricJ2000()), cell(c, 0, 2));
        assertEquals(SunTableFormat.degrees(o.horizontal().azimuthDegrees()), cell(c, 0, 4));
        assertEquals(SunTableFormat.astronomicalUnits(o.distanceAu()) + " AU ("
                + SunTableFormat.millionKilometres(o.distanceKm()) + " mill. km)", cell(c, 0, 5));
        assertEquals(SunTableFormat.minutesSeconds(o.solidBodyDiameterArcseconds()), cell(c, 0, 6));
        assertEquals(MoonTableFormat.percent(o.illuminatedFraction()), cell(c, 0, 7));
        assertEquals("near half lit", cell(c, 0, 8));
        assertEquals(MoonTableFormat.elongation(o.elongationDegrees(), "E"), cell(c, 0, 9),
                "east of the Sun: the evening sky");
        assertEquals(MoonTableFormat.positionAngle(o.brightLimbAngleDegrees())
                + " (west-northwest)", cell(c, 0, 10), "the lit side faces the Sun, in the west");
    }

    @Test
    void norwegianSaysTheRuledWordsWithItsOwnSeparatorAndLetters() {
        SolarTableDialog.Content c = content(oslo(ELONGATION), NB);
        assertEquals("nær halvt belyst", cell(c, 0, 8));
        assertTrue(cell(c, 0, 9).endsWith("° Ø") && cell(c, 0, 9).contains(","),
                "Ø for east, a decimal comma: " + cell(c, 0, 9));
        assertTrue(cell(c, 0, 10).endsWith("(vest-nordvest)"), cell(c, 0, 10));
        assertTrue(cell(c, 0, 5).contains(" AU (") && cell(c, 0, 5).contains(","),
                cell(c, 0, 5));
        assertEquals("Tilsynelatende diameter (fast kropp)", c.model.getColumnName(6));
    }

    @Test
    void theTransitIsInFrontOfTheSunAndItsLitSideIsNotWellDefined() {
        SolarTableDialog.Content en = content(oslo(TRANSIT), EN);
        assertEquals("in front of the Sun", cell(en, 0, 8));
        assertEquals("not well-defined (near inferior conjunction)", cell(en, 0, 10));
        SolarTableDialog.Content nb = content(oslo(TRANSIT), NB);
        assertEquals("foran Solen", cell(nb, 0, 8));
        assertEquals("ikke veldefinert (nær nedre konjunksjon)", cell(nb, 0, 10));
    }

    @Test
    void superiorConjunctionIsNearFullAndItsLitSideIsNotWellDefined() {
        SolarTableDialog.Content c = content(oslo(SUPERIOR), EN);
        assertEquals("near full", cell(c, 0, 8));
        assertEquals("not well-defined (near full)", cell(c, 0, 10));
    }

    @Test
    void everyPhaseWordExistsInBothLanguages() {
        for (InterfaceText said : List.of(EN, NB)) {
            SolarTableWords w = new SolarTableWords(said, SolarTable.venus().stem());
            for (VenusService.Phase phase : VenusService.Phase.values()) {
                String word = SolarTable.Venus.phaseWord(phase, w);
                assertFalse(word.isBlank(), phase + " in " + said);
            }
        }
    }

    @Test
    void aRangeIsTheInstantAnswerAtEveryRowAndItsRefusalsSayVenus() throws Exception {
        SolarTableSession session = new SolarTableSession(() -> oslo(ELONGATION), service,
                SolarTable.venus());
        session.apply(new SolarTableSession.Query(SolarTableSession.Mode.RANGE,
                "2026-10-20 03:00", "2026-10-27 03:00", 2));
        assertEquals(SolarTableSession.Outcome.ROWS, session.result().outcome());
        assertEquals(8, session.result().rows().size(), "every day, both ends included");
        for (SolarTableModel.Row r : session.result().rows()) {
            assertEquals(service.observe(Body.VENUS, oslo(r.sample().instant())), r.observation(),
                    "each row is the instant answer at its own instant");
        }
        session.apply(new SolarTableSession.Query(SolarTableSession.Mode.RANGE,
                "2100-12-25 00:00", "2101-01-02 00:00", 2));
        assertEquals(SolarTableSession.Outcome.REFUSED_INTERVAL, session.result().outcome());
        SolarTableDialog.Content c = SolarTableDialog.content(session, EN);
        assertEquals("Venus is computed for civil dates from 1900-01-01 to 2100-12-31;"
                + " 2101-01-02 00:00:00 is outside that.", c.status());
        session.apply(new SolarTableSession.Query(SolarTableSession.Mode.RANGE,
                "2026-01-01 00:00", "2026-03-01 00:00", 0));
        assertEquals(SolarTableSession.Outcome.REFUSED_ROWS, session.result().outcome(),
                "an hourly two months asks for more than the 1 000 rows shown");
    }

    @Test
    void twoHostsShareOneSessionAndTheControllersGroupComputesNothingWhenBuilt()
            throws Exception {
        SolarTableSession session = new SolarTableSession(() -> oslo(ELONGATION), service,
                SolarTable.venus());
        SolarTableControls[] controller = new SolarTableControls[1];
        JComponent[] group = new JComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            controller[0] = new SolarTableControls(session, EN, null, false);
            group[0] = controller[0].inController();
        });
        assertEquals(0, session.computations(), "building the group asks the service nothing");
        assertNull(controller[0].onChart, "built without a layer: no Show on chart");
        assertNull(controller[0].centreOnChart, "and no Centre on chart");
        assertNull(named(group[0], "venusCard"), "no result, no card");

        SolarTableDialog.Content[] dialog = new SolarTableDialog.Content[1];
        SwingUtilities.invokeAndWait(() -> dialog[0] = SolarTableDialog.content(session, EN));
        SwingUtilities.invokeAndWait(() -> {
            controller[0].start.setText("2026-10-20 03:00");
            controller[0].rangeView.setSelected(true);
        });
        SwingUtilities.invokeAndWait(() -> dialog[0].controls().apply());
        assertEquals(SolarTableSession.Mode.INSTANT, session.result().query().mode(),
                "the dialog applied its own draft");
        assertEquals("2026-10-20 03:00", controller[0].start.getText(),
                "and the Controller's unfinished draft was left alone");
        assertNotNull(named(group[0], "venusCard"), "the Controller shows the shared result");
        assertEquals(cell(dialog[0], 0, 8), String.valueOf(controller[0].model.getValueAt(0, 8)));
    }

    /** Since #514 Venus has a chart layer: its Centre on chart target names it. */
    @Test
    void venusCentresOnItsOwnLayer() {
        SolarTableSession session = new SolarTableSession(() -> oslo(ELONGATION), service,
                SolarTable.venus());
        juranometria.ui.solar.BodyOnChart layer = new juranometria.ui.solar.BodyOnChart() {
            @Override
            public boolean showing() {
                return false;
            }

            @Override
            public void show(boolean shown) {
            }

            @Override
            public void onChange(java.util.function.Consumer<Boolean> listener) {
                listener.accept(false);
            }
        };
        SolarTableControls controls = new SolarTableControls(session, EN, layer, false);
        assertEquals(CentreOnChart.Body.VENUS, controls.target(layer).body());
        assertSame(layer, controls.target(layer).layer());
    }

    private static List<Component> all(Component root) {
        List<Component> found = new ArrayList<>();
        found.add(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                found.addAll(all(child));
            }
        }
        return found;
    }

    private static Component named(Component root, String name) {
        return all(root).stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
    }
}
