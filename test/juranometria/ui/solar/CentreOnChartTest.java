package juranometria.ui.solar;

import java.awt.event.ActionEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.SolarSystemService;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Centre on chart (issue #483): one action for the Sun and the Moon from
 * the Controller's group and the dialog alike - the typed query
 * computed first, the layer turned on through its own switch, the chart
 * centred on the table's J2000 place at the chart's normal minimum
 * field, the chart window brought forward - with the refusals the
 * sessions already make, the keyboard
 * reaching and pressing it, both languages, and Jupiter's seam prepared
 * without a button.
 */
class CentreOnChartTest {

    private static SolarSystemService solar;
    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    private static final Instant WHEN = Instant.parse("2026-06-21T10:00:00Z");

    @BeforeAll
    static void load() {
        solar = SolarSystemService.load();
    }

    /** The chart, as the action sees it: where it was sent, and how often it came forward. */
    static final class FakeChart implements CentreOnChart.Chart {
        SkyPosition centre;
        double field;
        int forward;
        int centred;
        double minimum = 1.0;

        @Override
        public void centre(SkyPosition j2000, double fieldWidthDegrees) {
            centre = j2000;
            field = fieldWidthDegrees;
            centred++;
        }

        @Override
        public void bringForward() {
            forward++;
        }

        @Override
        public double normalMinimumFieldDegrees() {
            return minimum;
        }
    }

    /** A body's layer switch that counts what it was asked and tells its listeners. */
    static final class FakeLayer implements BodyOnChart {
        boolean shown;
        int asked;
        final List<Consumer<Boolean>> listeners = new ArrayList<>();

        FakeLayer(boolean shown) {
            this.shown = shown;
        }

        @Override
        public boolean showing() {
            return shown;
        }

        @Override
        public void show(boolean shown) {
            asked++;
            this.shown = shown;
            listeners.forEach(l -> l.accept(shown));
        }

        @Override
        public void onChange(Consumer<Boolean> listener) {
            listeners.add(listener);
            listener.accept(shown);
        }
    }

    static Observer oslo(Instant when) {
        return new Observer(59.91, 10.75, when);
    }

    static void onEdt(Runnable body) throws Exception {
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        if (failure[0] instanceof Error e) {
            throw e;
        }
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
    }

    /** Presses a button as the keyboard does - Space, pressed and released. */
    static void press(JButton button) {
        Object pressed = button.getInputMap(JButton.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("SPACE"));
        Object released = button.getInputMap(JButton.WHEN_FOCUSED)
                .get(KeyStroke.getKeyStroke("released SPACE"));
        button.getActionMap().get(pressed).actionPerformed(new ActionEvent(button, 0, "SPACE"));
        button.getActionMap().get(released).actionPerformed(new ActionEvent(button, 0, "SPACE"));
    }

    static SkyPosition firstRow(SolarTableSession session) {
        return switch (session.result().rows().get(0).observation()) {
            case SolarSystemService.SunObservation s -> s.astrometricJ2000();
            case SolarSystemService.MoonObservation m -> m.astrometricJ2000();
        };
    }

    @Test
    void theChartIsCentredOnTheTablesJ2000PlaceAtTheNormalMinimumFieldAndComesForward()
            throws Exception {
        for (SolarTable body : List.of(SolarTable.sun(), SolarTable.moon())) {
            SolarTableSession session = new SolarTableSession(() -> oslo(WHEN), solar, body);
            FakeChart chart = new FakeChart();
            CentreOnChart action = new CentreOnChart(chart);
            FakeLayer layer = new FakeLayer(true);
            onEdt(() -> {
                SolarTableControls c = new SolarTableControls(session, EN, layer, false, action, layer);
                press(c.centreOnChart);
                assertEquals(firstRow(session), chart.centre, body.prefix() + ": the table's J2000 place");
                assertEquals(1.0, chart.field, "the normal minimum field");
                assertEquals(1, chart.forward, "the chart window came forward");
                assertEquals(1, session.computations(), "computed once, as typed");
                assertEquals(1, c.model.getRowCount(), "and the table shows what was centred on");
            });
        }
        assertEquals(1.0, ChartViewState.normalMinimumFieldDegrees(),
                "the chart's narrowest rung today, read from its steps");
    }

    @Test
    void theFieldIsTheChartsMinimumNotAConstantSoAFinerRungMovesIt() throws Exception {
        SolarTableSession session = new SolarTableSession(() -> oslo(WHEN), solar, SolarTable.moon());
        FakeChart chart = new FakeChart();
        chart.minimum = 0.25;
        CentreOnChart action = new CentreOnChart(chart);
        FakeLayer layer = new FakeLayer(true);
        onEdt(() -> {
            press(new SolarTableControls(session, EN, null, false, action, layer).centreOnChart);
            assertEquals(0.25, chart.field, "a finer normal minimum (#481) is used as offered");
        });
    }

    @Test
    void theTypedQueryIsComputedFirstNotAResultDisplayedEarlier() throws Exception {
        Instant[] now = {WHEN};
        SolarTableSession session = new SolarTableSession(() -> oslo(now[0]), solar, SolarTable.moon());
        FakeChart chart = new FakeChart();
        CentreOnChart action = new CentreOnChart(chart);
        FakeLayer layer = new FakeLayer(true);
        onEdt(() -> {
            SolarTableControls c = new SolarTableControls(session, EN, layer, false, action, layer);
            c.apply();
            SkyPosition shownEarlier = firstRow(session);
            // Place and Time moves a day on, and the reader types a range
            // that is not applied yet.
            now[0] = WHEN.plusSeconds(86_400);
            c.rangeView.setSelected(true);
            c.start.setText("2026-06-25 10:00");
            c.end.setText("2026-06-25 14:00");
            c.step.setSelectedIndex(0);
            press(c.centreOnChart);
            assertEquals(SolarTableSession.Mode.RANGE, session.result().query().mode(),
                    "the typed range was applied");
            assertEquals("2026-06-25 10:00", session.result().query().start());
            assertEquals(firstRow(session), chart.centre, "centred on its first instant");
            assertNotEquals(shownEarlier, chart.centre, "not on what was shown before");
            assertEquals(Instant.parse("2026-06-25T10:00:00Z"),
                    session.result().rows().get(0).observation().instant());
        });
    }

    @Test
    void aLayerThatIsOffIsTurnedOnThroughItsSwitchAndOneThatIsOnIsLeftAlone() throws Exception {
        SolarTableSession session = new SolarTableSession(() -> oslo(WHEN), solar, SolarTable.moon());
        FakeChart chart = new FakeChart();
        CentreOnChart action = new CentreOnChart(chart);
        FakeLayer off = new FakeLayer(false);
        List<Boolean> heard = new ArrayList<>();
        off.onChange(heard::add);
        onEdt(() -> {
            SolarTableControls c = new SolarTableControls(session, EN, off, false, action, off);
            assertFalse(c.onChart.isSelected(), "the box shows the layer off");
            press(c.centreOnChart);
            assertTrue(off.shown, "the layer is on");
            assertEquals(1, off.asked, "asked once, through its switch");
            assertTrue(c.onChart.isSelected(), "the box hears it, as the switch notifies");
            assertEquals(List.of(false, true), heard, "every listener hears the change once");
            press(c.centreOnChart);
            assertEquals(1, off.asked, "a layer already on is not asked again");
        });
    }

    @Test
    void bothHostsPressTheSameActionAndFollowOneResult() throws Exception {
        SolarTableSession session = new SolarTableSession(() -> oslo(WHEN), solar, SolarTable.moon());
        FakeChart chart = new FakeChart();
        CentreOnChart action = new CentreOnChart(chart);
        FakeLayer layer = new FakeLayer(false);
        onEdt(() -> {
            SolarTableControls controller = new SolarTableControls(session, EN, layer, false,
                    action, layer);
            assertEquals(0, session.computations(), "building the Controller's group computes nothing");
            SolarTableDialog.Content dialog = SolarTableDialog.content(session, NB, action, layer);
            assertEquals(1, session.computations(),
                    "the Sun's and the Moon's dialogs compute on opening (ruled on #400; unchanged)");
            assertNotNull(dialog.controls().centreOnChart, "the dialog has the button too");
            press(dialog.controls().centreOnChart);
            assertEquals(1, action.presses(CentreOnChart.Body.MOON));
            assertEquals(firstRow(session), chart.centre);
            assertEquals(1, controller.model.getRowCount(), "the Controller shows the dialog's result");
            press(controller.centreOnChart);
            assertEquals(2, action.presses(CentreOnChart.Body.MOON), "one action, two hosts");
            assertEquals(2, action.centred(CentreOnChart.Body.MOON));
            assertEquals(0, action.presses(CentreOnChart.Body.SUN));
        });
    }

    @Test
    void aRefusalMovesNothingAndTurnsNothingOn() throws Exception {
        Observer[] at = {null};
        SolarTableSession session = new SolarTableSession(() -> at[0], solar, SolarTable.sun());
        FakeChart chart = new FakeChart();
        CentreOnChart action = new CentreOnChart(chart);
        FakeLayer layer = new FakeLayer(false);
        onEdt(() -> {
            SolarTableControls c = new SolarTableControls(session, EN, layer, false, action, layer);
            press(c.centreOnChart);
            assertNull(chart.centre, "no observer: nothing to centre on");
            assertFalse(layer.shown, "and the layer is left as it was");
            at[0] = oslo(Instant.parse("2101-01-01T00:00:00Z"));
            press(c.centreOnChart);
            assertEquals(SolarTableSession.Outcome.REFUSED_INTERVAL, session.result().outcome());
            assertNull(chart.centre, "outside the years: the session's refusal, nothing moved");
            assertEquals(0, chart.forward);
            assertTrue(c.status().startsWith("The Sun is computed for civil dates"), c.status());
        });
    }

    @Test
    void theKeyboardReachesAndPressesItInBothLanguages() throws Exception {
        SolarTableSession session = new SolarTableSession(() -> oslo(WHEN), solar, SolarTable.moon());
        FakeChart chart = new FakeChart();
        CentreOnChart action = new CentreOnChart(chart);
        FakeLayer layer = new FakeLayer(true);
        onEdt(() -> {
            SolarTableControls en = new SolarTableControls(session, EN, null, true, action, layer);
            SolarTableControls nb = new SolarTableControls(session, NB, null, true, action, layer);
            JButton b = en.centreOnChart;
            assertTrue(b.isFocusable(), "the keyboard reaches it");
            assertEquals("Centre on chart", b.getText());
            assertEquals("Centre the chart on the Moon", b.getAccessibleContext().getAccessibleName());
            assertTrue(b.getAccessibleContext().getAccessibleDescription().contains("J2000"),
                    "the explanation says what it does");
            assertEquals('H', (char) b.getMnemonic());
            assertEquals("Sentrer på kartet", nb.centreOnChart.getText());
            assertEquals("Sentrer kartet på Månen", nb.centreOnChart.getAccessibleContext()
                    .getAccessibleName());
            assertEquals('S', (char) nb.centreOnChart.getMnemonic());
            assertEquals("Centre the chart on the Sun", new SolarTableControls(
                    new SolarTableSession(() -> oslo(WHEN), solar, SolarTable.sun()), EN, null,
                    false, action, layer).centreOnChart.getAccessibleContext().getAccessibleName());
            // Enter and Space both press it while it has the keyboard.
            for (String key : List.of("ENTER", "SPACE")) {
                Object pressed = b.getInputMap(JButton.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(key));
                Object released = b.getInputMap(JButton.WHEN_FOCUSED)
                        .get(KeyStroke.getKeyStroke("released " + key));
                assertNotNull(pressed, key + " is bound");
                int before = action.presses(CentreOnChart.Body.MOON);
                b.getActionMap().get(pressed).actionPerformed(new ActionEvent(b, 0, key));
                b.getActionMap().get(released).actionPerformed(new ActionEvent(b, 0, key));
                assertEquals(before + 1, action.presses(CentreOnChart.Body.MOON), key + " presses it");
            }
        });
    }

    @Test
    void jupitersSeamIsPreparedWithoutAButton() throws Exception {
        JovianSystemService jovian = JovianSystemService.load();
        JovianTableSession session = new JovianTableSession(
                () -> oslo(Instant.parse("2026-12-11T22:45:00Z")), () -> jovian);
        FakeChart chart = new FakeChart();
        CentreOnChart action = new CentreOnChart(chart);
        FakeLayer layer = new FakeLayer(false);
        onEdt(() -> {
            JovianTableControls c = new JovianTableControls(session, EN, false);
            // a finer field for Jupiter, as #481 may later offer
            assertTrue(action.centre(c.target(layer, minimum -> minimum / 4)));
            assertEquals(session.result().entries().get(0).jupiter().astrometricJ2000(), chart.centre);
            assertEquals(0.25, chart.field, "the target's field, not the Sun's and the Moon's");
            assertTrue(layer.shown, "its layer would be turned on");
            SolarSystemSection section = new SolarSystemSection(
                    new SolarTableSession(() -> oslo(WHEN), solar, SolarTable.sun()),
                    new SolarTableSession(() -> oslo(WHEN), solar, SolarTable.moon()),
                    layer, layer, session, action, EN);
            assertNotNull(section.sun().centreOnChart);
            assertNotNull(section.moon().centreOnChart);
            javax.swing.JComponent jupiterGroup = section.jupiter().inController();
            assertNull(find(jupiterGroup, "jupiterCentreOnChart"),
                    "no Jupiter button until something Jovian is drawn (#484)");
        });
    }

    private static java.awt.Component find(java.awt.Container from, String name) {
        for (java.awt.Component c : from.getComponents()) {
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof java.awt.Container inner) {
                java.awt.Component found = find(inner, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
