package juranometria.project;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a globe tells a reader who cannot see it (Sprint 32, issue
 * #331, step five).
 *
 * <p>A page description named the projection, the centre, the field
 * and the subject, and every one of those was true of a hemisphere
 * without telling a reader the one thing that makes a hemisphere
 * different from every other page: it shows half the sky and stops,
 * and what lies around it is paper rather than empty sky. "180
 * degrees wide, orthographic projection" is exact and says none of
 * that.
 *
 * <p>So a bounded page adds one plain sentence, and only a bounded
 * page does.
 */
class GlobeDescriptionTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private static final int WIDE_PX = 900;

    private static final int HIGH_PX = 700;

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    @Test
    void aGlobeSaysItIsOneHemisphereWithACircularEdge() {
        String said = DrawnPage.of(sceneAt(180.0)).describe(ENGLISH);

        assertTrue(said.contains("one hemisphere"),
                "a globe says how much sky it shows: " + said);
        assertTrue(said.contains("90 degrees from the centre"),
                "and how far that reaches: " + said);
        assertTrue(said.contains("paper, not sky"),
                "and what is outside it: " + said);

        // In plain words. The projection's name stays, because it is
        // the page's identity and is said everywhere else too, but it
        // is not asked to carry the explanation.
        assertTrue(said.contains("orthographic"),
                "the projection is still named: " + said);
    }

    @Test
    void anOrdinaryPageSaysExactlyWhatItAlwaysSaid() {
        // Word for word, not merely "still sensible". The released
        // pages' descriptions are what a reader who cannot see them
        // has learned to expect, and this sentence is not theirs to
        // carry: a 42-degree page has no limb and no hidden half.
        for (double field : new double[] {6.0, 42.0, 120.0}) {
            String said = DrawnPage.of(sceneAt(field)).describe(ENGLISH);
            assertFalse(said.contains("hemisphere"),
                    field + " degrees says nothing about a hemisphere:"
                            + " " + said);
            assertFalse(said.contains("limb"),
                    field + " degrees says nothing about a limb: "
                            + said);
            assertTrue(said.endsWith("North up, east left."),
                    field + " degrees ends where it always ended: "
                            + said);
        }
    }

    @Test
    void theChartHandsTheWholeSentenceToTheReader() throws Exception {
        // Through the route a screen reader actually takes, rather
        // than through the method that composes it: the component's
        // own accessible description. A sentence that existed only in
        // DrawnPage would be a sentence nobody is ever told.
        //
        // Built and read on the event thread, which is the suite's
        // settled discipline for live chart state (#220): a component
        // is Swing's to touch, and a read taken off the thread that
        // owns it is the stale-scene race that discipline exists to
        // refuse.
        juranometria.ui.ChartComponent[] chart =
                new juranometria.ui.ChartComponent[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            chart[0] = new juranometria.ui.ChartComponent(
                    Atlas.assembler(), ENGLISH);
            chart[0].setSize(WIDE_PX, HIGH_PX);
            chart[0].setViewState(
                    new ChartViewState(SAGITTARIUS, 180.0, 5.0));
        });

        String spoken = onEdt(() -> chart[0].getAccessibleContext()
                .getAccessibleDescription());
        String said = onEdt(() ->
                DrawnPage.of(chart[0].currentScene()).describe(ENGLISH));

        assertEquals(said, spoken,
                "the chart tells a reader exactly what the page says"
                        + " about itself");
        assertTrue(spoken.contains("one hemisphere"),
                "including the hemisphere sentence: " + spoken);
    }

    private static <T> T onEdt(java.util.concurrent.Callable<T> read)
            throws Exception {
        Object[] held = new Object[1];
        Exception[] failed = new Exception[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                held[0] = read.call();
            } catch (Exception thrown) {
                failed[0] = thrown;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        @SuppressWarnings("unchecked")
        T value = (T) held[0];
        return value;
    }

    private static ChartScene sceneAt(double fieldDegrees) {
        return Atlas.assembler().assemble(
                new ChartViewState(SAGITTARIUS, fieldDegrees, 5.0),
                WIDE_PX, HIGH_PX);
    }
}
