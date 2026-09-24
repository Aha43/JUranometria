package juranometria.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.Cardinal;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartStructure;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

/**
 * The four cardinal landmarks on the pages a reader actually opens
 * (#359 completion).
 *
 * <p>The owner's finding: on a 180-degree globe centred on the
 * zenith, with the mathematical horizon on, the application drew
 * the horizon and no N, E, S or W. The unit contracts placed the
 * marks on a near-empty synthetic page with nothing reserved; the
 * real page reserves its stars' labels, the constellation names and
 * the furniture, and every adjacent letter box was refused.
 *
 * <p>So these journeys use the whole application page - the real
 * catalogue, furniture, localized words and the module's own
 * contributions - and hold what a reader must get: all four exact
 * landmarks on the zenith globe in both languages and on both
 * grounds, each mark at its exact sky position; nothing at all once
 * the horizon is off; the horizon's accent on mark and letter both;
 * and on a narrower page, the exact cardinal point in view carrying
 * its letter, while a horizon segment with no cardinal point in view
 * rightly carries none.
 *
 * <p>Every journey runs on the event thread, as the application
 * does: a chart built and painted off it can race the toolkit's own
 * work and paint the opening page instead of the one asked for.
 */
class CardinalLandmarksJourneyTest {

    /** Runs a journey on the event thread, rethrowing what it throws. */
    private static void onEdt(Runnable journey) throws Exception {
        Throwable[] thrown = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                journey.run();
            } catch (Throwable failure) {
                thrown[0] = failure;
            }
        });
        if (thrown[0] instanceof Error error) {
            throw error;
        }
        if (thrown[0] instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (thrown[0] != null) {
            throw new AssertionError(thrown[0]);
        }
    }

    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-03-20T21:33:00Z"));

    private record Chart(ChartComponent component,
                         MeridianModule module) { }

    private static Chart chart(String language, ChartPalette ground,
                               ChartViewState state) {
        juranometria.project.PageWords words =
                juranometria.ui.language.PageText.in(
                        juranometria.ui.language.InterfaceText
                                .forLanguage(language));
        ChartComponent chart = new ChartComponent(Atlas.assembler(),
                words);
        chart.setSize(900, 700);
        chart.setChartOptions(ChartOptions.DEFAULTS.withPalette(ground));
        ChartModuleHost host = new ChartModuleHost(chart,
                new juranometria.chart.SelectionModel(), request -> { });
        MeridianModule module = host.attach(new MeridianModule(OSLO));
        module.showing(false, true, false);
        chart.setViewState(state);
        return new Chart(chart, module);
    }

    private static ChartViewState zenithGlobe() {
        return new ChartViewState(new LocalSky(OSLO).zenith(), 180.0,
                ChartViewState.defaultMagnitudeFor(180.0));
    }

    private static BufferedImage paint(ChartComponent chart) {
        BufferedImage image = new BufferedImage(chart.getWidth(),
                chart.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            chart.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static Map<Cardinal, ReferenceInk.DirectionPlacement>
            rendered(ChartComponent chart) {
        paint(chart);
        Map<Cardinal, ReferenceInk.DirectionPlacement> by =
                new TreeMap<>();
        for (ReferenceInk.DirectionPlacement placed
                : chart.renderedDirections()) {
            by.put(placed.cardinal(), placed);
        }
        return by;
    }

    /** Where the page puts this exact sky position, in page pixels. */
    private static PixelPoint exact(ChartComponent chart,
                                    SkyPosition at) {
        DrawnPage page = DrawnPage.of(chart.currentScene());
        return page.projection().project(at)
                .map(new ViewportMapping(page)::toPixel).orElseThrow();
    }

    // ---- the zenith globe -------------------------------------------

    @Test
    void theZenithGlobeCarriesAllFourDirectionsInEnglish() throws Exception {
        onEdt(() -> theZenithGlobeCarriesAllFourDirectionsInEnglishJourney());
    }

    private static void theZenithGlobeCarriesAllFourDirectionsInEnglishJourney() {
        holdsAllFour("en", Map.of(Cardinal.NORTH, "N", Cardinal.EAST, "E",
                Cardinal.SOUTH, "S", Cardinal.WEST, "W"));
    }

    @Test
    void theZenithGlobeCarriesAllFourDirectionsInNorwegian() throws Exception {
        onEdt(() -> theZenithGlobeCarriesAllFourDirectionsInNorwegianJourney());
    }

    private static void theZenithGlobeCarriesAllFourDirectionsInNorwegianJourney() {
        holdsAllFour("nb-NO", Map.of(Cardinal.NORTH, "N",
                Cardinal.EAST, "Ø", Cardinal.SOUTH, "S",
                Cardinal.WEST, "V"));
    }

    private static void holdsAllFour(String language,
                                     Map<Cardinal, String> letters) {
        for (ChartPalette ground : ChartPalette.values()) {
            Chart c = chart(language, ground, zenithGlobe());
            Map<Cardinal, ReferenceInk.DirectionPlacement> by =
                    rendered(c.component());
            assertEquals(EnumSet.allOf(Cardinal.class), by.keySet(),
                    language + " on " + ground.storedAs()
                            + ": the zenith globe must carry all four"
                            + " landmarks, got " + by.keySet());
            Rectangle2D paper = new Rectangle2D.Double(0, 0,
                    c.component().currentScene().viewport().widthPx(),
                    c.component().currentScene().viewport().heightPx());
            for (Cardinal direction : Cardinal.values()) {
                ReferenceInk.DirectionPlacement placed = by.get(direction);
                assertEquals(letters.get(direction), placed.letter(),
                        language + ": " + direction + " is lettered in the"
                                + " page's language");
                PixelPoint where = exact(c.component(),
                        new LocalSky(OSLO).cardinal(direction));
                assertTrue(Math.abs(where.x() - placed.at().x()) < 1e-6
                                && Math.abs(where.y() - placed.at().y())
                                        < 1e-6,
                        direction + " sits at its exact horizon point "
                                + where + ", not " + placed.at());
                assertTrue(paper.contains(placed.box()),
                        direction + "'s letter stays on the paper");
                // On the zenith globe every landmark is on the limb,
                // and its letter lies outward, wholly beyond the
                // circular sky.
                PixelPoint centre = exact(c.component(),
                        new LocalSky(OSLO).zenith());
                double limb = Math.hypot(placed.at().x() - centre.x(),
                        placed.at().y() - centre.y());
                Rectangle2D box = placed.box();
                double nearest = Math.hypot(
                        Math.max(Math.max(box.getMinX() - centre.x(),
                                centre.x() - box.getMaxX()), 0.0),
                        Math.max(Math.max(box.getMinY() - centre.y(),
                                centre.y() - box.getMaxY()), 0.0));
                assertTrue(nearest >= limb,
                        direction + "'s letter lies outside the circular"
                                + " sky");
            }
        }
    }

    @Test
    void theNorwegianSouthClearsTheWrappedTitleBlock() throws Exception {
        onEdt(() -> theNorwegianSouthClearsTheWrappedTitleBlockJourney());
    }

    private static void theNorwegianSouthClearsTheWrappedTitleBlockJourney() {
        // The longer Norwegian caption used to run the lower-left
        // title block past the page centre and over the exact south
        // point. On a bounded page it now wraps, and its right edge
        // stays left of the south point by the landmark's clearance.
        for (ChartPalette ground : ChartPalette.values()) {
            Chart c = chart("nb-NO", ground, zenithGlobe());
            Map<Cardinal, ReferenceInk.DirectionPlacement> by =
                    rendered(c.component());
            ReferenceInk.DirectionPlacement south = by.get(Cardinal.SOUTH);
            assertTrue(south != null, "the south landmark is placed");
            juranometria.chart.ChartScene scene =
                    c.component().currentScene();
            juranometria.render.ChartRenderer renderer =
                    new juranometria.render.ChartRenderer(
                            juranometria.chart.StarSizePolicy.DEFAULT,
                            c.component().words());
            BufferedImage probe = new BufferedImage(1, 1,
                    BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = probe.createGraphics();
            java.awt.Rectangle block;
            java.awt.FontMetrics letters;
            try {
                block = renderer.titleBlockBounds(g, scene);
            } finally {
                g.dispose();
            }
            letters = juranometria.render.EquatorialGrid.labelMetrics();
            double clearance = juranometria.render.CardinalLandmark
                    .clearance(letters, c.component().words());
            assertTrue(block.getMaxX() <= south.at().x() - clearance,
                    "the block ends " + block.getMaxX() + ", clear of the"
                            + " south point at " + south.at().x()
                            + " by the landmark's " + clearance);
            assertTrue(!block.intersects(south.box()),
                    "the south letter does not touch the title block");
            Rectangle2D diamond = new Rectangle2D.Double(
                    south.at().x() - 6, south.at().y() - 6, 12, 12);
            assertTrue(!block.intersects(diamond),
                    "the south mark is not drawn through the title");
            // The same caption on an unbounded page keeps its three
            // lines; the globe's block is taller because it wrapped.
            juranometria.chart.ChartScene open = Atlas.assembler()
                    .assemble(new ChartViewState(
                            new LocalSky(OSLO).zenith(), 120.0,
                            ChartViewState.defaultMagnitudeFor(120.0)),
                            900, 700);
            java.awt.Graphics2D g2 = probe.createGraphics();
            java.awt.Rectangle unwrapped;
            try {
                unwrapped = new juranometria.render.ChartRenderer(
                        juranometria.chart.StarSizePolicy.DEFAULT,
                        c.component().words()).titleBlockBounds(g2, open);
            } finally {
                g2.dispose();
            }
            assertTrue(block.height > unwrapped.height,
                    "the Norwegian caption really wrapped: "
                            + block.height + " px against "
                            + unwrapped.height + " unwrapped");
        }
    }

    // ---- off means gone ---------------------------------------------

    @Test
    void horizonOffRemovesTheLineAndEveryMark() throws Exception {
        onEdt(() -> horizonOffRemovesTheLineAndEveryMarkJourney());
    }

    private static void horizonOffRemovesTheLineAndEveryMarkJourney() {
        Chart c = chart("en", ChartPalette.WHITE_PAPER, zenithGlobe());
        BufferedImage on = paint(c.component());
        c.module().showing(false, false, false);
        BufferedImage off = paint(c.component());
        assertTrue(c.component().renderedDirections().isEmpty(),
                "no horizon, no directions");
        c.module().detach();
        BufferedImage bare = paint(c.component());
        assertTrue(same(off, bare),
                "horizon off is the page with no module ink at all:"
                        + " no line, no mark, no letter");
        assertTrue(!same(on, off), "and on was really different");
    }

    // ---- emphasis carries mark and letter ---------------------------

    @Test
    void horizonEmphasisCarriesMarkAndLetter() throws Exception {
        onEdt(() -> horizonEmphasisCarriesMarkAndLetterJourney());
    }

    private static void horizonEmphasisCarriesMarkAndLetterJourney() {
        for (ChartPalette ground : ChartPalette.values()) {
            Chart c = chart("en", ground, zenithGlobe());
            BufferedImage plain = paint(c.component());
            List<ReferenceInk.DirectionPlacement> placed =
                    List.copyOf(c.component().renderedDirections());
            assertEquals(4, placed.size(),
                    "the emphasized journey needs the four landmarks");
            c.component().toggleEmphasis(ChartStructure.HORIZON);
            BufferedImage raised = paint(c.component());
            int accent = juranometria.render.StructureStyle.resolve(
                    ground, ChartStructure.HORIZON, true,
                    java.awt.Color.BLACK, new java.awt.BasicStroke(1f))
                    .color().getRGB();
            int groundInk = ground.ground().getRGB();
            int offset = c.component().pageOffsetY();
            for (ReferenceInk.DirectionPlacement one : placed) {
                Rectangle2D mark = new Rectangle2D.Double(
                        one.at().x() - 5, one.at().y() - 5 + offset,
                        10, 10);
                Rectangle2D letter = new Rectangle2D.Double(
                        one.box().getX(), one.box().getY() + offset,
                        one.box().getWidth(), one.box().getHeight());
                assertTrue(carries(plain, raised, mark, accent, groundInk),
                        ground.storedAs() + ": " + one.cardinal()
                                + "'s mark takes the horizon accent");
                assertTrue(carries(plain, raised, letter, accent, groundInk),
                        ground.storedAs() + ": " + one.cardinal()
                                + "'s letter takes the horizon accent");
            }
        }
    }

    // ---- a narrower page --------------------------------------------

    @Test
    void aNarrowPageNearACardinalPointCarriesItsLetter() throws Exception {
        onEdt(() -> aNarrowPageNearACardinalPointCarriesItsLetterJourney());
    }

    private static void aNarrowPageNearACardinalPointCarriesItsLetterJourney() {
        LocalSky sky = new LocalSky(OSLO);
        SkyPosition north = sky.cardinal(Cardinal.NORTH);
        // Ten degrees above the north point, towards the zenith: the
        // exact point sits below the page centre, in plain view.
        SkyPosition centre = between(north, sky.zenith(), 10.0);
        for (String language : List.of("en", "nb-NO")) {
            Chart c = chart(language, ChartPalette.WHITE_PAPER,
                    new ChartViewState(centre, 42.0,
                            ChartViewState.defaultMagnitudeFor(42.0)));
            Map<Cardinal, ReferenceInk.DirectionPlacement> by =
                    rendered(c.component());
            assertTrue(by.containsKey(Cardinal.NORTH),
                    language + ": the exact north point is in view, so"
                            + " it carries its letter; got "
                            + by.keySet());
            PixelPoint where = exact(c.component(), north);
            assertTrue(Math.abs(where.x() - by.get(Cardinal.NORTH).at().x())
                            < 1e-6
                            && Math.abs(where.y()
                                    - by.get(Cardinal.NORTH).at().y())
                                    < 1e-6,
                    "at its exact horizon point");
        }
    }

    @Test
    void aHorizonSegmentWithNoCardinalPointInViewCarriesNone() throws Exception {
        onEdt(() -> aHorizonSegmentWithNoCardinalPointInViewCarriesNoneJourney());
    }

    private static void aHorizonSegmentWithNoCardinalPointInViewCarriesNoneJourney() {
        LocalSky sky = new LocalSky(OSLO);
        // The horizon due north-east, half-way between two exact
        // cardinal points: the line crosses the page and no landmark
        // belongs on it. Recorded, so no one "fixes" it by repeating
        // letters along the line.
        SkyPosition northEast = between(sky.cardinal(Cardinal.NORTH),
                sky.cardinal(Cardinal.EAST), 45.0);
        Chart c = chart("en", ChartPalette.WHITE_PAPER,
                new ChartViewState(northEast, 24.0,
                        ChartViewState.defaultMagnitudeFor(24.0)));
        BufferedImage on = paint(c.component());
        assertTrue(c.component().renderedDirections().isEmpty(),
                "no exact cardinal point in view, no landmark");
        c.module().showing(false, false, false);
        assertTrue(!same(on, paint(c.component())),
                "while the horizon line itself is on the page");
    }

    // ---- arithmetic -------------------------------------------------

    /** The point this many degrees from {@code from} towards {@code to}. */
    private static SkyPosition between(SkyPosition from, SkyPosition to,
                                       double degrees) {
        double[] a = unit(from);
        double[] b = unit(to);
        double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        double[] perp = {b[0] - dot * a[0], b[1] - dot * a[1],
                b[2] - dot * a[2]};
        double n = Math.sqrt(perp[0] * perp[0] + perp[1] * perp[1]
                + perp[2] * perp[2]);
        double t = Math.toRadians(degrees);
        double[] v = new double[3];
        for (int i = 0; i < 3; i++) {
            v[i] = a[i] * Math.cos(t) + perp[i] / n * Math.sin(t);
        }
        double ra = Math.toDegrees(Math.atan2(v[1], v[0]));
        return new SkyPosition(ra < 0 ? ra + 360.0 : ra,
                Math.toDegrees(Math.asin(v[2])));
    }

    private static double[] unit(SkyPosition p) {
        double ra = Math.toRadians(p.raDegrees());
        double dec = Math.toRadians(p.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static boolean same(BufferedImage one, BufferedImage other) {
        return java.util.Arrays.equals(
                one.getRGB(0, 0, one.getWidth(), one.getHeight(), null, 0,
                        one.getWidth()),
                other.getRGB(0, 0, other.getWidth(), other.getHeight(),
                        null, 0, other.getWidth()));
    }

    /**
     * Some pixel in the box changed to the accent laid on the ground:
     * a glyph's antialiased edge is a blend of the two, one blend
     * factor across all three channels, and a curved letter may have
     * no pixel of pure accent at all.
     */
    private static boolean carries(BufferedImage plain,
                                   BufferedImage raised, Rectangle2D box,
                                   int accent, int ground) {
        for (int y = (int) Math.floor(box.getMinY());
             y <= (int) Math.ceil(box.getMaxY()); y++) {
            for (int x = (int) Math.floor(box.getMinX());
                 x <= (int) Math.ceil(box.getMaxX()); x++) {
                if (x < 0 || y < 0 || x >= plain.getWidth()
                        || y >= plain.getHeight()
                        || plain.getRGB(x, y) == raised.getRGB(x, y)) {
                    continue;
                }
                if (blendOf(raised.getRGB(x, y), accent, ground)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether a colour is ground-to-accent at one factor, mostly ink. */
    private static boolean blendOf(int colour, int accent, int ground) {
        double factor = Double.NaN;
        for (int shift = 0; shift <= 16; shift += 8) {
            int c = (colour >> shift) & 0xff;
            int a = (accent >> shift) & 0xff;
            int g = (ground >> shift) & 0xff;
            if (Math.abs(a - g) < 32) {
                continue;
            }
            double f = (double) (c - g) / (a - g);
            if (Double.isNaN(factor)) {
                factor = f;
            } else if (Math.abs(f - factor) > 0.03) {
                return false;
            }
        }
        return !Double.isNaN(factor) && factor > 0.5 && factor < 1.03;
    }
}
