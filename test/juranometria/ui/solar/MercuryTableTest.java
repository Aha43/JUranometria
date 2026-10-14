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
import juranometria.solar.MercuryService;
import juranometria.solar.MercuryService.MercuryObservation;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Mercury table (issue #530), headless: Venus's shell over Mercury's
 * eleven columns, in the vocabulary ruled on #528 - what it shows at the
 * named cases, what it says in each language, its range and refusals,
 * the date-line observer, its two hosts over one session, and what it
 * never offers: no chart switch and no Centre on chart, since nothing
 * of Mercury is drawn.
 */
class MercuryTableTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    /** Espenak's 2026 greatest eastern elongation, to the hour (mercury-events.txt). */
    private static final Instant ELONGATION = Instant.parse("2026-02-19T18:00:00Z");
    /** The NASA/GSFC catalogue's 2019 greatest transit, to the minute (mercury-events.txt). */
    private static final Instant TRANSIT = Instant.parse("2019-11-11T15:20:00Z");
    /** Espenak's 2026 superior conjunction, to the hour (mercury-events.txt). */
    private static final Instant SUPERIOR = Instant.parse("2026-01-21T16:00:00Z");
    private static SolarSystemService service;

    @BeforeAll
    static void load() {
        service = SolarSystemService.load();
    }

    private static Observer oslo(Instant at) {
        return new Observer(59.91, 10.75, at);
    }

    private static SolarTableDialog.Content content(Observer observer, InterfaceText said) {
        return SolarTableDialog.content(() -> observer, service, said, SolarTable.mercury());
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
    void theShellIsTheMoonsAndTheColumnsAreMercurysAsRuled() {
        SolarTableDialog.Content c = content(oslo(ELONGATION), EN);
        assertEquals(List.of("Instant (UTC)", "Right ascension (J2000)", "Declination (J2000)",
                "Altitude (no refraction)", "Azimuth (from north through east)", "Distance",
                "Apparent diameter (solid body)", "Illuminated", "Phase",
                "Elongation from the Sun", "Lit side"), columnNames(c.model));
        assertEquals("mercuryTable", c.table.getName());
        assertEquals("Mercury's computed quantities",
                c.table.getAccessibleContext().getAccessibleName());
        assertTrue(c.model.columnExplanation(6).contains("2440.53 km")
                        && c.model.columnExplanation(6).contains("2438.26")
                        && c.model.columnExplanation(6).contains("mean radius is not substituted"),
                "the equatorial radius, the triaxial figure stated once (ruling 4): "
                        + c.model.columnExplanation(6));
        assertTrue(c.model.columnExplanation(8).contains("the same bands as Venus's"),
                "the shared phase bands (ruling 5)");
        assertTrue(c.model.columnExplanation(9).contains("right ascension")
                        && c.model.columnExplanation(9).contains("does not announce a conjunction"),
                "the side by right ascension, naming no conjunction (ruling 3)");
        assertTrue(c.timeNote.getText().contains("below 15″ through 2100"),
                "the time note states the measured after-2027 cost: " + c.timeNote.getText());
    }

    @Test
    void theGreatestEasternElongationReadsAsTheServiceAnswers() {
        SolarTableDialog.Content c = content(oslo(ELONGATION), EN);
        MercuryObservation o = (MercuryObservation) service.observe(Body.MERCURY, oslo(ELONGATION));
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
                + " (west-southwest)", cell(c, 0, 10), "the lit side faces the Sun, in the west");
    }

    @Test
    void norwegianSaysTheRuledWordsWithItsOwnSeparatorAndLetters() {
        SolarTableDialog.Content c = content(oslo(ELONGATION), NB);
        assertEquals("nær halvt belyst", cell(c, 0, 8));
        assertTrue(cell(c, 0, 9).endsWith("° Ø") && cell(c, 0, 9).contains(","),
                "Ø for east, a decimal comma: " + cell(c, 0, 9));
        assertTrue(cell(c, 0, 10).endsWith("(vest-sørvest)"), cell(c, 0, 10));
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
            SolarTableWords w = new SolarTableWords(said, SolarTable.mercury().stem());
            SolarTableWords v = new SolarTableWords(said, SolarTable.venus().stem());
            for (MercuryService.Phase phase : MercuryService.Phase.values()) {
                String word = SolarTable.Mercury.phaseWord(phase, w);
                assertEquals(SolarTable.Venus.phaseWord(
                        VenusService.Phase.valueOf(phase.name()), v), word,
                        "the shared vocabulary, word for word (ruling 5)");
                assertFalse(word.isBlank(), phase + " in " + said);
            }
        }
    }

    @Test
    void aRangeIsTheInstantAnswerAtEveryRowAndItsRefusalsSayMercury() throws Exception {
        SolarTableSession session = new SolarTableSession(() -> oslo(ELONGATION), service,
                SolarTable.mercury());
        session.apply(new SolarTableSession.Query(SolarTableSession.Mode.RANGE,
                "2026-03-03 11:00", "2026-03-10 11:00", 2));
        assertEquals(SolarTableSession.Outcome.ROWS, session.result().outcome());
        assertEquals(8, session.result().rows().size(), "every day, both ends included");
        StringBuilder sides = new StringBuilder();
        for (SolarTableModel.Row r : session.result().rows()) {
            sides.append(((MercuryObservation) r.observation()).side()
                    == SolarSystemService.Side.EAST_OF_SUN ? 'E' : 'W');
        }
        assertEquals("EEEEWWWW", sides.toString(), "the side changes once, by right ascension,"
                + " through the 2026-03-07 inferior conjunction");
        for (SolarTableModel.Row r : session.result().rows()) {
            assertEquals(service.observe(Body.MERCURY, oslo(r.sample().instant())), r.observation(),
                    "each row is the instant answer at its own instant");
        }
        session.apply(new SolarTableSession.Query(SolarTableSession.Mode.RANGE,
                "2100-12-25 00:00", "2101-01-02 00:00", 2));
        assertEquals(SolarTableSession.Outcome.REFUSED_INTERVAL, session.result().outcome());
        SolarTableDialog.Content c = SolarTableDialog.content(session, EN);
        assertEquals("Mercury is computed for civil dates from 1900-01-01 to 2100-12-31;"
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
                SolarTable.mercury());
        SolarTableControls[] controller = new SolarTableControls[1];
        JComponent[] group = new JComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            controller[0] = new SolarTableControls(session, EN, null, false);
            group[0] = controller[0].inController();
        });
        assertEquals(0, session.computations(), "building the group asks the service nothing");
        assertNull(controller[0].onChart, "built without a layer: no Show on chart");
        assertNull(controller[0].centreOnChart, "and no Centre on chart");
        assertNull(named(group[0], "mercuryCard"), "no result, no card");

        SolarTableDialog.Content[] dialog = new SolarTableDialog.Content[1];
        SwingUtilities.invokeAndWait(() -> dialog[0] = SolarTableDialog.content(session, EN));
        SwingUtilities.invokeAndWait(() -> {
            controller[0].start.setText("2026-03-03 11:00");
            controller[0].rangeView.setSelected(true);
        });
        SwingUtilities.invokeAndWait(() -> dialog[0].controls().apply());
        assertEquals(SolarTableSession.Mode.INSTANT, session.result().query().mode(),
                "the dialog applied its own draft");
        assertEquals("2026-03-03 11:00", controller[0].start.getText(),
                "and the Controller's unfinished draft was left alone");
        assertNotNull(named(group[0], "mercuryCard"), "the Controller shows the shared result");
        assertEquals(cell(dialog[0], 0, 8), String.valueOf(controller[0].model.getValueAt(0, 8)));
    }

    /** The date-line observer: the Chatham Islands' row is the service's own answer there. */
    @Test
    void theDateLineObserverReadsItsOwnSky() {
        Observer chatham = new Observer(-43.95, -176.5, ELONGATION);
        SolarTableDialog.Content c = SolarTableDialog.content(() -> chatham, service, EN,
                SolarTable.mercury());
        MercuryObservation o = (MercuryObservation) service.observe(Body.MERCURY, chatham);
        assertEquals(SunTableFormat.minute(ELONGATION), cell(c, 0, 0), "the instant is UTC");
        assertEquals(SunTableFormat.degrees(o.horizontal().altitudeDegrees())
                + (o.horizontal().altitudeDegrees() < 0 ? " (below the horizon)" : ""),
                cell(c, 0, 3));
        assertEquals(SunTableFormat.degrees(o.horizontal().azimuthDegrees()), cell(c, 0, 4));
        assertEquals("near half lit", cell(c, 0, 8));
        assertEquals(MoonTableFormat.elongation(o.elongationDegrees(), "E"), cell(c, 0, 9));
    }

    /** No Mercury is drawn (#530): its Centre on chart target is refused by name. */
    @Test
    void mercuryHasNoLayerToCentreOn() {
        SolarTableSession session = new SolarTableSession(() -> oslo(ELONGATION), service,
                SolarTable.mercury());
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
        // Built as every host builds it: without a Show on chart switch.
        SolarTableControls controls = new SolarTableControls(session, EN, null, false);
        assertNull(controls.onChart);
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> controls.target(layer));
        assertTrue(refused.getMessage().contains("Mercury has no chart layer"),
                refused.getMessage());
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
