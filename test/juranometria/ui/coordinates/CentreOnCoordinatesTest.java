package juranometria.ui.coordinates;

import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.search.SearchResult;
import juranometria.ui.ChartViewController;
import juranometria.ui.SearchNavigation;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Centre on coordinates (issue #623, rulings C-1 to C-4 in
 * {@code docs/decisions/centre-on-coordinates.md}): strict J2000 entry,
 * the accepted value shown normalised, and centring through the
 * production navigation contract Search uses for coordinates - field
 * kept, no target claimed, the selection cleared - in both languages.
 */
class CentreOnCoordinatesTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");

    private static CoordinateEntry.Result read(String h, String m, String s, boolean south,
                                               String d, String am, String as) {
        return CoordinateEntry.read(h, m, s, south, d, am, as);
    }

    @Test
    void anEntryIsReadExactlyAndShownNormalised() {
        // Sh 2-115, as an observing note gives it: 20h 33m 50s, +46° 47′ 00″.
        CoordinateEntry.Result sh = read("20", "33", "50", false, "46", "47", "0");
        assertTrue(sh.accepted());
        assertEquals(15.0 * (20 + 33 / 60.0 + 50 / 3600.0), sh.position().raDegrees(), 1e-12);
        assertEquals(46 + 47 / 60.0, sh.position().decDegrees(), 1e-12);
        assertEquals(List.of("20", "33", "50", "46", "47", "00"), List.of(sh.hours(),
                sh.minutes(), sh.seconds(), sh.degrees(), sh.arcminutes(), sh.arcseconds()));

        CoordinateEntry.Result south = read("5", "7", "3.25", true, "8", "", "7.5");
        assertEquals(-(8 + 7.5 / 3600.0), south.position().decDegrees(), 1e-12);
        assertEquals(List.of("5", "07", "03.25", "8", "00", "07.5"), List.of(south.hours(),
                south.minutes(), south.seconds(), south.degrees(), south.arcminutes(),
                south.arcseconds()), "decimals as typed, round trip");
        assertTrue(south.south());

        CoordinateEntry.Result comma = read("0", "0", "1,5", false, "0", "0", "0");
        assertEquals(15.0 * 1.5 / 3600.0, comma.position().raDegrees(), 1e-12,
                "a decimal comma reads as a point");
        CoordinateEntry.Result minusZero = read("1", "0", "0", true, "0", "0", "0");
        assertFalse(minusZero.south(), "-0° is 0°: no sign without a value");
    }

    @Test
    void twentyFourHoursIsZeroAndTheEdgesAreStrict() {
        CoordinateEntry.Result twentyFour = read("24", "0", "0", false, "0", "0", "0");
        assertTrue(twentyFour.accepted());
        assertEquals(0.0, twentyFour.position().raDegrees());
        assertEquals("0", twentyFour.hours(), "shown as 0h");
        CoordinateEntry.Result nearly = read("23", "59", "59.999", false, "0", "0", "0");
        assertTrue(nearly.position().raDegrees() < 360.0);
        assertEquals(CoordinateEntry.Refusal.RIGHT_ASCENSION,
                read("24", "0", "1", false, "0", "0", "0").refusal(), "24h only as 24 00 00");
        assertEquals(CoordinateEntry.Refusal.RIGHT_ASCENSION,
                read("25", "0", "0", false, "0", "0", "0").refusal());
        assertEquals(CoordinateEntry.Refusal.RIGHT_ASCENSION,
                read("1", "60", "0", false, "0", "0", "0").refusal(), "no silent repair of 60");
        assertEquals(CoordinateEntry.Refusal.RIGHT_ASCENSION,
                read("1", "0", "60", false, "0", "0", "0").refusal());
        CoordinateEntry.Result pole = read("0", "0", "0", true, "90", "0", "0");
        assertEquals(-90.0, pole.position().decDegrees());
        assertEquals(CoordinateEntry.Refusal.DECLINATION,
                read("0", "0", "0", false, "90", "0", "0.1").refusal(),
                "a pole with more than zero beyond it");
        assertEquals(CoordinateEntry.Refusal.DECLINATION,
                read("0", "0", "0", false, "91", "0", "0").refusal());
        assertEquals(CoordinateEntry.Refusal.DECLINATION,
                read("0", "0", "0", false, "10", "60", "0").refusal());
        for (String bad : List.of("1.5", "-3", "x", "1e2", "1.2.3")) {
            CoordinateEntry.Result r = read(bad, "0", "0", false, "0", "0", "0");
            assertEquals(CoordinateEntry.Refusal.NUMBER, r.refusal(), bad);
            assertEquals(bad, r.offending());
        }
        assertEquals(CoordinateEntry.Refusal.NUMBER,
                read("", "0", "0", false, "0", "0", "0").refusal(), "hours are required");
        assertEquals(CoordinateEntry.Refusal.NUMBER,
                read("1", "0", "-1", false, "0", "0", "0").refusal());
    }

    /** The production path the application wires: Search's coordinate contract. */
    private static CoordinatesDialog.Centring production(ChartViewController controller,
                                                         SelectionModel selection,
                                                         SkyPosition[] mark) {
        return position -> {
            String shown = juranometria.chart.SkyFormat.formatRa(position.raDegrees()) + ", "
                    + juranometria.chart.SkyFormat.formatDec(position.decDegrees());
            SearchNavigation.Outcome moved = SearchNavigation.apply(new SearchResult(shown,
                    shown, SearchResult.Kind.COORDINATES, position, shown),
                    Atlas.assembler(), controller, selection);
            if (moved != SearchNavigation.Outcome.NO_FIT) {
                mark[0] = position;
            }
            return moved;
        };
    }

    @Test
    void centringKeepsTheFieldClaimsNothingAndClearsTheSelection() throws Exception {
        ChartViewController controller = new ChartViewController(Atlas.assembler()::fits);
        controller.recenter(new SkyPosition(83.8, -5.4), 12.0);
        ChartViewState before = controller.state();
        SelectionModel selection = new SelectionModel();
        selection.selectEmptySky(new SkyPosition(80.0, 0.0));
        SkyPosition[] mark = {null};
        CoordinatesDialog.Content[] content = new CoordinatesDialog.Content[1];
        SwingUtilities.invokeAndWait(() -> content[0] = new CoordinatesDialog.Content(EN,
                production(controller, selection, mark)));
        double[][] cases = {
                {20, 33, 50, 46, 47, 0, 0}, // northern: Sh 2-115
                {6, 45, 8.9, 16, 42, 58, 1}, // southern: Sirius's place
                {0, 0, 0, 0, 0, 0, 0},      // zero hours
                {23, 59, 59.9, 30, 0, 0, 1}  // near 24 hours
        };
        for (double[] c : cases) {
            SwingUtilities.invokeAndWait(() -> {
                content[0].hours.setText(Integer.toString((int) c[0]));
                content[0].minutes.setText(Integer.toString((int) c[1]));
                content[0].seconds.setText(Double.toString(c[2]));
                content[0].sign.setSelectedIndex((int) c[6]);
                content[0].degrees.setText(Integer.toString((int) c[3]));
                content[0].arcminutes.setText(Integer.toString((int) c[4]));
                content[0].arcseconds.setText(Integer.toString((int) c[5]));
                content[0].centre(); // what Enter and the button both run
            });
            double ra = 15.0 * (c[0] + c[1] / 60.0 + c[2] / 3600.0);
            double dec = (c[6] == 1 ? -1 : 1) * (c[3] + c[4] / 60.0 + c[5] / 3600.0);
            ChartViewState now = controller.state();
            assertEquals(ra, now.centre().raDegrees(), 1e-9, "centred exactly");
            assertEquals(dec, now.centre().decDegrees(), 1e-9);
            assertEquals(before.fieldWidthDegrees(), now.fieldWidthDegrees(),
                    "the field is kept on a page that shows the place");
            assertNull(now.targetLabel(), "no target is claimed for a coordinate");
            assertTrue(selection.selection() instanceof Selection.None,
                    "the selection is cleared, as Search clears it");
            assertEquals(ra, mark[0].raDegrees(), 1e-9, "the requested position is marked");
            assertTrue(content[0].status().startsWith("Centred on "), content[0].status());
            assertTrue(content[0].status().endsWith("(J2000)."), content[0].status());
        }
    }

    @Test
    void aRefusalSaysWhyAndTheChartDoesNotMove() throws Exception {
        ChartViewController controller = new ChartViewController(Atlas.assembler()::fits);
        controller.recenter(new SkyPosition(10.0, 41.0), 8.0);
        ChartViewState before = controller.state();
        SkyPosition[] mark = {null};
        CoordinatesDialog.Content[] content = new CoordinatesDialog.Content[2];
        SwingUtilities.invokeAndWait(() -> {
            content[0] = new CoordinatesDialog.Content(EN,
                    production(controller, new SelectionModel(), mark));
            content[1] = new CoordinatesDialog.Content(NB,
                    production(controller, new SelectionModel(), mark));
            for (CoordinatesDialog.Content c : content) {
                c.hours.setText("1");
                c.degrees.setText("90");
                c.arcminutes.setText("1");
                c.centre(); // what the button's action runs
            }
        });
        assertEquals(before, controller.state(), "the chart was not moved");
        assertNull(mark[0]);
        assertEquals("Declination is at most 90° with arcminutes and arcseconds below 60;"
                + " ±90° only with both zero. The chart was not moved.", content[0].status());
        assertTrue(content[1].status().startsWith("Deklinasjon er høyst 90°"), content[1].status());
    }

    @Test
    void theDialogStatesJ2000AndEveryFieldNamesItself() throws Exception {
        for (InterfaceText said : List.of(EN, NB)) {
            CoordinatesDialog.Content[] c = new CoordinatesDialog.Content[1];
            SwingUtilities.invokeAndWait(() -> c[0] = new CoordinatesDialog.Content(said,
                    position -> SearchNavigation.Outcome.RECENTERED));
            assertTrue(said.say("coordinates.heading").contains("J2000"), said.language());
            List<javax.swing.JComponent> fields = List.of(c[0].hours, c[0].minutes, c[0].seconds,
                    c[0].sign, c[0].degrees, c[0].arcminutes, c[0].arcseconds, c[0].centre);
            java.util.Set<String> names = new java.util.HashSet<>();
            for (javax.swing.JComponent f : fields) {
                String name = f.getAccessibleContext().getAccessibleName();
                assertTrue(name != null && !name.isBlank(), f.getName());
                assertTrue(names.add(name), "each field's spoken name is its own: " + name);
            }
            assertTrue(said.say("search.explain").contains("J2000"),
                    "Search says J2000 too (C-4)");
        }
    }

    @Test
    void theRequestedPositionWearsTheCrossOnTheScreenUntilCleared() throws Exception {
        SkyPosition sh2115 = new SkyPosition(15.0 * (20 + 33 / 60.0 + 50 / 3600.0), 46 + 47 / 60.0);
        juranometria.ui.ChartComponent[] chart = new juranometria.ui.ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            chart[0] = new juranometria.ui.ChartComponent(Atlas.assembler(),
                    juranometria.ui.language.PageText.in(EN));
            chart[0].setSize(900, 700);
            chart[0].setViewState(new ChartViewState(sh2115, 8.0, 8.0, null, null));
        });
        java.awt.image.BufferedImage without = paint(chart[0]);
        SwingUtilities.invokeAndWait(() -> chart[0].setRequestedPosition(sh2115));
        java.awt.image.BufferedImage with = paint(chart[0]);
        int changed = 0;
        int cx = 450 + chart[0].pageOffsetX();
        int cy = 350 + chart[0].pageOffsetY();
        for (int y = cy - 10; y <= cy + 10; y++) {
            for (int x = cx - 10; x <= cx + 10; x++) {
                if (with.getRGB(x, y) != without.getRGB(x, y)) {
                    changed++;
                }
            }
        }
        assertTrue(changed > 10, "the cross is drawn at the centre: " + changed);
        assertEquals(with.getRGB(cx, cy), without.getRGB(cx, cy),
                "and its gap leaves the place itself visible");
        SwingUtilities.invokeAndWait(() -> chart[0].setRequestedPosition(null));
        java.awt.image.BufferedImage cleared = paint(chart[0]);
        for (int y = cy - 10; y <= cy + 10; y++) {
            for (int x = cx - 10; x <= cx + 10; x++) {
                assertEquals(without.getRGB(x, y), cleared.getRGB(x, y), "cleared, nothing is left");
            }
        }
    }

    private static java.awt.image.BufferedImage paint(juranometria.ui.ChartComponent chart)
            throws Exception {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(900, 700,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        return image;
    }
}
