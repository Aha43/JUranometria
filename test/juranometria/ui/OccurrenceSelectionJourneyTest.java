package juranometria.ui;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.app.InspectorPanel;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.page.LeadSelection;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A selected body is an occurrence (Sprint 48, issue #513), through
 * the real component: a module offers the Moon's disc with the moment
 * it was computed for and the selectable mark; a real click on the
 * disc selects its occurrence - identity, instant, observer, J2000
 * place - and makes it the working selection's member and lead; the
 * Inspector, in both languages, heads it with the page's name and
 * states the instant and the observer; the page drawn for another
 * instant re-resolves the selection; the body withdrawn leaves the
 * selection with the instant it held, and the Inspector says so; an
 * additive click toggles it out; and a body offered without the mark
 * still consumes the click as the empty sky there (#414 ruling C6).
 */
class OccurrenceSelectionJourneyTest {

    private static final Instant FIRST_QUARTER = Instant.parse("2026-03-26T10:32:00Z");
    private static final Observer OSLO = new Observer(59.913, 10.752, FIRST_QUARTER);
    private static final SolarSystemService SERVICE = SolarSystemService.load();

    private ChartComponent chart;
    private SelectionModel selection;
    private juranometria.chart.WorkingSelection working;
    private ChartModuleHost host;
    private Observer observer = OSLO;
    private boolean selectable = true;
    private boolean offering = true;

    /** A study module: the Moon's disc, with its moment, selectable or not. */
    private final ChartModule module = new ChartModule() {
        private Runnable withdraw;

        @Override
        public String name() {
            return "study occurrence";
        }

        @Override
        public void attach(ChartServices services) {
            withdraw = services.contribute("study.occurrence", () -> {
                if (!offering) {
                    return List.of();
                }
                MoonObservation moon = (MoonObservation) SERVICE.observe(Body.MOON, observer);
                return List.of(new OverlayContribution.Body(SolarSystemModule.MOON, "Moon",
                        moon.astrometricJ2000(), moon.angularDiameterArcseconds(),
                        new OverlayContribution.Lit(moon.illuminatedFraction(),
                                moon.brightLimbAngleDegrees(), moon.phaseAngleDegrees()),
                        false, moon.distanceKm(), InkRole.BODY,
                        OverlayContribution.Moment.of(observer), selectable));
            });
        }

        @Override
        public void detach() {
            if (withdraw != null) {
                withdraw.run();
            }
        }
    };

    private MoonObservation moon(Observer at) {
        return (MoonObservation) SERVICE.observe(Body.MOON, at);
    }

    private void build(String language) throws Exception {
        PageText words = PageText.in(InterfaceText.forLanguage(language));
        SwingUtilities.invokeAndWait(() -> {
            chart = new ChartComponent(Atlas.assembler(), words);
            chart.setSize(900, 700);
            chart.setViewState(new ChartViewState(moon(observer).astrometricJ2000(), 1.0,
                    8.0, null, null));
            selection = new SelectionModel();
            // The host's own working selection, bridged to the answering
            // model as the application wires it; the click writes it.
            host = new ChartModuleHost(chart, selection, request -> { });
            working = host.workingSelection();
            SelectInteraction.install(chart, selection, working,
                    new juranometria.chart.SelectionMode());
            host.attach(module);
        });
        flush();
        paint();
    }

    private void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    private void paint() throws Exception {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        flush();
    }

    private void click(double pageX, double pageY, boolean additive) throws Exception {
        int cx = (int) Math.round(pageX) + chart.pageOffsetX();
        int cy = (int) Math.round(pageY) + chart.pageOffsetY();
        // The interaction's own stated mask, which a headless toolkit
        // answers where the toolkit itself refuses (CI's test job).
        int modifiers = additive ? SelectInteraction.toggleModifierMask() : 0;
        for (int id : new int[] {MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED,
                MouseEvent.MOUSE_CLICKED}) {
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(new MouseEvent(chart, id,
                    System.nanoTime() / 1_000_000, modifiers, cx, cy, 1, false,
                    MouseEvent.BUTTON1)));
        }
        flush();
    }

    private void redraw() throws Exception {
        SwingUtilities.invokeAndWait(host::redraw);
        paint();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (host != null) {
            SwingUtilities.invokeAndWait(host::detachAll);
        }
    }

    @Test
    void aClickOnASelectableBodyIsItsOccurrenceAndFollowsThePage() throws Exception {
        build("en");
        ReferenceInk.BodyPlacement drawn = chart.renderedBodies().get(0);
        assertTrue(drawn.selectable() && FIRST_QUARTER.equals(drawn.moment().instant()),
                "the drawn Moon carries its moment: " + drawn);
        assertEquals(1, host.inventory().bodies().size(), "and is in the page's inventory");
        assertTrue(host.inventory().holds(SolarSystemModule.MOON));

        click(drawn.centre().x(), drawn.centre().y(), false);
        Selection.Occurrence chosen = assertInstanceOf(Selection.Occurrence.class,
                selection.selection(), "the click selects the occurrence");
        assertEquals(SolarSystemModule.MOON, chosen.identity());
        assertEquals(FIRST_QUARTER, chosen.instant());
        assertEquals(59.913, chosen.latitudeDegrees());
        assertEquals(10.752, chosen.eastLongitudeDegrees());
        assertTrue(chosen.position().separationDegrees(moon(OSLO).astrometricJ2000()) < 0.01,
                "at the drawn J2000 place");
        assertEquals(List.of(), selection.candidates(), "no candidates: the ink is its own");
        assertEquals(List.of(SolarSystemModule.MOON), working.members(),
                "the working selection holds its identity");
        assertEquals(SolarSystemModule.MOON, working.lead());

        // The Inspector, over the page's inventory: name, instant, observer, place.
        InspectorPanel[] panel = new InspectorPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new InspectorPanel(selection, chart::currentScene, chart::chartOptions,
                    s -> { }, InterfaceText.forLanguage("en"));
            panel[0].showWorkingSet(working, host::inventory);
            // As the application wires it: the page's bodies changing
            // refreshes what the Inspector says.
            chart.onBodiesChange(panel[0]::refresh);
        });
        flush();
        String said = String.join(" | ", panel[0].lines());
        assertTrue(said.startsWith("Moon"), "headed by the page's name: " + said);
        assertTrue(said.contains("Computed for 2026-03-26 10:32:00 UTC"), said);
        assertTrue(said.contains("latitude 59.913, longitude 10.752 east"), said);
        assertTrue(said.contains("ICRS J2000") && !said.contains("Not drawn"), said);

        // Another instant, twenty minutes on - the Moon moves about ten
        // arcminutes, still on the 1° page: the page draws it elsewhere,
        // and the selection follows it to that instant and place.
        observer = OSLO.at(FIRST_QUARTER.plus(Duration.ofMinutes(20)));
        redraw();
        Selection.Occurrence later = assertInstanceOf(Selection.Occurrence.class,
                selection.selection());
        assertEquals(observer.instant(), later.instant(), "re-resolved to the drawn instant");
        assertNotEquals(chosen.position(), later.position(), "and the drawn place");
        assertEquals(List.of(SolarSystemModule.MOON), working.members(), "membership unchanged");
        flush();
        said = String.join(" | ", panel[0].lines());
        assertTrue(said.contains("Computed for 2026-03-26 10:52:00 UTC"), said);

        // Withdrawn: the selection keeps the instant it held; the Inspector says so.
        offering = false;
        redraw();
        assertEquals(later, selection.selection(), "kept, with its instant");
        assertEquals(List.of(), host.inventory().bodies());
        flush();
        said = String.join(" | ", panel[0].lines());
        assertTrue(said.contains("No longer present at the page's current moment"), said);
        assertTrue(said.contains("Computed for 2026-03-26 10:52:00 UTC"), said);

        // Offered again at that instant: an additive click toggles it out
        // of the working set; the lead bridge answers nothing.
        offering = true;
        redraw();
        drawn = chart.renderedBodies().get(0);
        click(drawn.centre().x(), drawn.centre().y(), true);
        assertEquals(List.of(), working.members(), "toggled out");
        assertInstanceOf(Selection.None.class, selection.selection(),
                "nothing leads, so nothing is answered: " + selection.selection());
    }

    @Test
    void theLeadBridgeResolvesABodyFromThePageInBothLanguages() throws Exception {
        for (String language : List.of("en", "nb-NO")) {
            observer = OSLO;
            build(language);
            // The host bridges its working selection's lead to the
            // answering model; a body member added by name leads.
            SwingUtilities.invokeAndWait(() -> working.add(SolarSystemModule.MOON));
            flush();
            Selection.Occurrence led = assertInstanceOf(Selection.Occurrence.class,
                    selection.selection(), "a body member leads as its occurrence");
            assertEquals(FIRST_QUARTER, led.instant());
            InspectorPanel[] panel = new InspectorPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new InspectorPanel(selection, chart::currentScene,
                        chart::chartOptions, s -> { }, InterfaceText.forLanguage(language));
                panel[0].showWorkingSet(working, host::inventory);
            });
            flush();
            String said = String.join(" | ", panel[0].lines());
            String name = PageText.in(InterfaceText.forLanguage(language))
                    .bodyName(SolarSystemModule.MOON);
            assertTrue(said.startsWith(name), language + ": " + said);
            assertTrue(said.contains(language.equals("en") ? "Computed for" : "Beregnet for"),
                    language + ": " + said);
            SwingUtilities.invokeAndWait(host::detachAll);
            host = null;
        }
    }

    @Test
    void aBodyNotMarkedSelectableStillConsumesTheClickAsEmptySky() throws Exception {
        selectable = false;
        build("en");
        ReferenceInk.BodyPlacement drawn = chart.renderedBodies().get(0);
        assertFalse(drawn.selectable());
        assertTrue(drawn.moment() != null, "it still states its moment");
        assertTrue(host.inventory().holds(SolarSystemModule.MOON),
                "and is in the inventory, unselectable");
        assertTrue(LeadSelection.resolve(host.inventory(), SolarSystemModule.MOON).isEmpty(),
                "which the lead bridge will not answer");
        click(drawn.centre().x(), drawn.centre().y(), false);
        assertInstanceOf(Selection.EmptySky.class, selection.selection(),
                "the opaque disc consumed the click (C6): " + selection.selection());
        assertEquals(List.of(), working.members());
        SkyPosition under = ((Selection.EmptySky) selection.selection()).position();
        assertTrue(under.separationDegrees(moon(OSLO).astrometricJ2000()) < 0.05);
    }
}
