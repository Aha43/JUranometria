package juranometria.ecliptic;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.module.TestChartServices;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.Ecliptic;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The removable ecliptic module (Sprint 28, issue #273).
 *
 * <p>Asked of the module's own contract and of the rendered page,
 * never of a reconstruction: what a reader sees is what the real
 * registry and the real painter put down.
 */
class EclipticModuleTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT);

    /** A page carrying the March equinox, where the circle crosses. */
    private static final ChartScene SCENE = Atlas.assembler().assemble(
            new ChartViewState(new SkyPosition(0.0, 0.0), 24.0, 8.0),
            900, 700);

    private static BufferedImage page(ChartScene scene,
                                      ChartOptions options,
                                      List<OverlayRegistry.Owned> ink) {
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, options,
                    (layerG, painted) -> ReferenceInk.paint(layerG,
                            painted, ink, options.palette()));
        } finally {
            g.dispose();
        }
        return image;
    }

    private static BufferedImage ordinary(ChartScene scene,
                                          ChartOptions options) {
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, options);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static int differences(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static EclipticModule attached(TestChartServices services) {
        EclipticModule module = new EclipticModule();
        module.attach(services);
        return module;
    }

    // ---- what it owns -----------------------------------------------

    @Test
    void aFreshModuleShowsNothingAndDoesNoWork() {
        // The gate's released default: a reader who has never asked
        // for the ecliptic does not get it, and installing the module
        // must not redraw everyone's chart.
        EclipticModule module = new EclipticModule();
        assertFalse(module.showing(),
                "a fresh module is hidden");
        assertEquals(List.of(), module.contributedGeometry(),
                "and offers the chart nothing");
        assertEquals(0, module.timesTheGeometryWasBuilt(),
                "and does no ecliptic work at all - it returns before"
                        + " any of the geometry is worked out");
    }

    @Test
    void showingItOffersTheCircleAndItsFourLandmarks() {
        EclipticModule module = new EclipticModule();
        module.showing(true);
        List<OverlayContribution> offered = module.contributedGeometry();

        assertEquals(5, offered.size(),
                "one circle and four cardinal landmarks");
        assertTrue(offered.get(0) instanceof
                        OverlayContribution.GreatCircle circle
                        && circle.reference()
                                == OverlayContribution.Reference.PERMANENT
                        && circle.pole().equals(Ecliptic.POLE),
                "the circle is the reviewed pole, offered as a"
                        + " permanent circle of the sphere");
        assertEquals(List.of("march-equinox", "june-solstice",
                        "september-equinox", "december-solstice"),
                offered.stream().skip(1)
                        .map(OverlayContribution::identity).toList(),
                "the landmarks carry the model's own identities");
        assertEquals(List.of("March equinox", "June solstice",
                        "September equinox", "December solstice"),
                offered.stream().skip(1)
                        .map(OverlayContribution::accessibleName).toList(),
                "and the model's own reader-facing names, so no second"
                        + " list of names exists to drift from it");
        assertTrue(offered.stream().skip(1).allMatch(each ->
                        each instanceof OverlayContribution.Point point
                        && point.mark()
                                == OverlayContribution.Mark.LANDMARK),
                "each is a landmark, not a place a reader stands"
                        + " under");
        assertTrue(offered.stream().allMatch(each ->
                        each.role() == InkRole.REFERENCE_LINE),
                "all of it is reference ink");
    }

    @Test
    void everyLandmarkOfferedSitsOnTheCircleOffered() {
        EclipticModule module = new EclipticModule();
        module.showing(true);
        List<OverlayContribution> offered = module.contributedGeometry();
        SkyPosition pole = ((OverlayContribution.GreatCircle)
                offered.get(0)).pole();
        for (OverlayContribution each : offered.subList(1, 5)) {
            SkyPosition at = ((OverlayContribution.Point) each).at();
            assertEquals(90.0, pole.separationDegrees(at), 1.0e-9,
                    each.accessibleName() + " lies on the circle the"
                            + " module offered, not on some other one");
        }
    }

    // ---- lifecycle --------------------------------------------------

    @Test
    void attachingContributesAndDetachingWithdraws() {
        TestChartServices services = new TestChartServices();
        EclipticModule module = attached(services);
        module.showing(true);

        assertEquals(5, services.overlays.collect().size(),
                "attached and showing, its geometry is on offer");
        assertTrue(services.overlays.holds(EclipticModule.ID),
                "under its own name");

        module.detach();
        assertEquals(List.of(), services.overlays.collect(),
                "detached, it offers nothing");
        assertFalse(services.overlays.holds(EclipticModule.ID),
                "and holds nothing");
    }

    @Test
    void attachingTwiceIsRefused() {
        TestChartServices services = new TestChartServices();
        EclipticModule module = attached(services);
        assertThrows(IllegalStateException.class,
                () -> module.attach(services),
                "attaching twice would leave the first contribution"
                        + " with nothing to withdraw it");
    }

    @Test
    void detachingLeavesAnotherModulesInkAlone() {
        TestChartServices services = new TestChartServices();
        MeridianModule meridian = new MeridianModule(
                new Observer(59.9, 10.7,
                        java.time.Instant.parse("2026-03-20T21:33:00Z")));
        meridian.attach(services);
        meridian.showing(true, true, true);
        EclipticModule ecliptic = attached(services);
        ecliptic.showing(true);

        assertEquals(8, services.overlays.collect().size(),
                "three from the meridian, five from the ecliptic");

        ecliptic.detach();
        assertEquals(3, services.overlays.collect().size(),
                "detaching one removes only its own contribution");
        assertTrue(services.overlays.holds(MeridianModule.ID),
                "and leaves the other module holding its ink");
    }

    // ---- what a reader sees ------------------------------------------

    @Test
    void withTheModuleHiddenThePageIsTheOrdinaryChart() {
        TestChartServices services = new TestChartServices();
        attached(services);
        assertEquals(0, differences(
                        ordinary(SCENE, ChartOptions.DEFAULTS),
                        page(SCENE, ChartOptions.DEFAULTS,
                                services.overlays.collect())),
                "an attached module that shows nothing leaves the"
                        + " released page byte for byte");
    }

    @Test
    void withItShowingTheCirclePassesThroughItsLandmarks() {
        TestChartServices services = new TestChartServices();
        EclipticModule module = attached(services);
        module.showing(true);
        BufferedImage drawn = page(SCENE, ChartOptions.DEFAULTS,
                services.overlays.collect());
        BufferedImage plain = ordinary(SCENE, ChartOptions.DEFAULTS);

        assertTrue(differences(plain, drawn) > 0,
                "the ecliptic is on the page");

        // The March equinox is on this page, at the RA origin. Ink
        // appears within a mark's width of exactly where the model
        // says it is.
        double[] at = pixelOf(SCENE, Ecliptic.landmark("march-equinox")
                .orElseThrow().at());
        assertTrue(inkNear(plain, drawn, (int) Math.round(at[0]),
                        (int) Math.round(at[1]), 8),
                "and its landmark is drawn where the model puts it,"
                        + " not somewhere near it");
    }

    private static double[] pixelOf(ChartScene scene, SkyPosition at) {
        return onPage(scene, at).orElseThrow();
    }

    /** Where this lands on the page, if the projection has it. */
    private static java.util.Optional<double[]> onPage(ChartScene scene,
                                                       SkyPosition at) {
        return new juranometria.project.GnomonicProjection(
                        scene.viewport().centre()).project(at)
                .map(new juranometria.project.ViewportMapping(
                        scene.viewport())::toPixel)
                .map(p -> new double[] {p.x(), p.y()});
    }

    private static boolean inkNear(BufferedImage plain,
                                   BufferedImage drawn,
                                   int x, int y, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px >= 0 && py >= 0 && px < drawn.getWidth()
                        && py < drawn.getHeight()
                        && plain.getRGB(px, py) != drawn.getRGB(px, py)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void bothGroundsCarryTheSameGeometryInTheirOwnInk() {
        TestChartServices services = new TestChartServices();
        EclipticModule module = attached(services);
        module.showing(true);
        ChartOptions black = ChartOptions.DEFAULTS
                .withPalette(ChartPalette.BLACK_SKY);

        BufferedImage onPaper = page(SCENE, ChartOptions.DEFAULTS,
                services.overlays.collect());
        BufferedImage onBlack = page(SCENE, black,
                services.overlays.collect());
        BufferedImage plainPaper = ordinary(SCENE, ChartOptions.DEFAULTS);
        BufferedImage plainBlack = ordinary(SCENE, black);

        assertTrue(differences(plainPaper, onPaper) > 0,
                "the ecliptic is drawn on white paper");
        assertTrue(differences(plainBlack, onBlack) > 0,
                "and on black sky");

        // The same geometry, asked where the geometry says it is
        // rather than by comparing pixel sets - antialiasing against
        // two different grounds does not produce the same fringe, and
        // demanding that it did would be testing the compositor.
        int checked = 0;
        for (Ecliptic.Landmark landmark : Ecliptic.landmarks()) {
            // Half the ecliptic is behind the observer on any page,
            // and the projection refuses it - which is the projection
            // being honest, not a landmark going missing.
            java.util.Optional<double[]> projected =
                    onPage(SCENE, landmark.at());
            if (projected.isEmpty()) {
                continue;
            }
            int x = (int) Math.round(projected.get()[0]);
            int y = (int) Math.round(projected.get()[1]);
            if (x < 10 || y < 10
                    || x >= SCENE.viewport().widthPx() - 10
                    || y >= SCENE.viewport().heightPx() - 10) {
                continue;
            }
            checked++;
            assertTrue(inkNear(plainPaper, onPaper, x, y, 10),
                    landmark.name() + " is drawn on white paper");
            assertTrue(inkNear(plainBlack, onBlack, x, y, 10),
                    landmark.name() + " is drawn on black sky, in the"
                            + " same place - the ground changes, the"
                            + " geometry does not move");
        }
        assertTrue(checked > 0,
                "and at least one landmark was actually on this page,"
                        + " so the loop above is an answer");

        // And the ink is the palette's, not the painter's: the two
        // grounds put down different colours for the same line.
        assertNotEquals(ChartPalette.WHITE_PAPER.figureInk(),
                ChartPalette.BLACK_SKY.figureInk(),
                "the two palettes ink a figure differently");
        java.util.Set<Integer> paperInk = inkOf(plainPaper, onPaper);
        java.util.Set<Integer> blackInk = inkOf(plainBlack, onBlack);
        assertFalse(paperInk.isEmpty(), "the paper page has ink on it");
        assertFalse(blackInk.isEmpty(), "and so does the black one");
        // Not the exact palette colour, and not two disjoint sets: a
        // one-pixel line is antialiased against its ground, so the
        // pure ink need never appear and some mid-grey blends occur
        // on both. What the palette owning the ink actually means is
        // the direction of contrast - dark on paper, pale on black -
        // and a painter with a colour of its own could not do both.
        assertTrue(meanLuminance(paperInk)
                        < luminance(ChartPalette.WHITE_PAPER.ground()
                                .getRGB()),
                "on white paper the ecliptic is darker than its"
                        + " ground");
        assertTrue(meanLuminance(blackInk)
                        > luminance(ChartPalette.BLACK_SKY.ground()
                                .getRGB()),
                "and on black sky it is paler than its ground - the"
                        + " same geometry, inked by whichever palette"
                        + " is in force");
    }

    private static double luminance(int rgb) {
        return 0.2126 * ((rgb >> 16) & 0xff)
                + 0.7152 * ((rgb >> 8) & 0xff)
                + 0.0722 * (rgb & 0xff);
    }

    private static double meanLuminance(java.util.Set<Integer> colours) {
        return colours.stream().mapToDouble(
                EclipticModuleTest::luminance).average().orElseThrow();
    }

    /** The colours the reference layer put down, whatever they are. */
    private static java.util.Set<Integer> inkOf(BufferedImage plain,
                                                BufferedImage drawn) {
        java.util.Set<Integer> colours = new java.util.HashSet<>();
        for (int y = 0; y < plain.getHeight(); y++) {
            for (int x = 0; x < plain.getWidth(); x++) {
                if (plain.getRGB(x, y) != drawn.getRGB(x, y)) {
                    colours.add(drawn.getRGB(x, y));
                }
            }
        }
        return colours;
    }

    @Test
    void theEclipticAndTheMeridianComposeAndOrderDoesNotDecideThePage() {
        Observer observer = new Observer(59.9, 10.7,
                java.time.Instant.parse("2026-03-20T21:33:00Z"));

        TestChartServices first = new TestChartServices();
        MeridianModule meridianFirst = new MeridianModule(observer);
        meridianFirst.attach(first);
        meridianFirst.showing(true, true, true);
        EclipticModule eclipticSecond = attached(first);
        eclipticSecond.showing(true);

        TestChartServices second = new TestChartServices();
        EclipticModule eclipticFirst = attached(second);
        eclipticFirst.showing(true);
        MeridianModule meridianSecond = new MeridianModule(observer);
        meridianSecond.attach(second);
        meridianSecond.showing(true, true, true);

        assertEquals(0, differences(
                        page(SCENE, ChartOptions.DEFAULTS,
                                first.overlays.collect()),
                        page(SCENE, ChartOptions.DEFAULTS,
                                second.overlays.collect())),
                "two modules attached either way round draw the same"
                        + " page: which astronomical role covers"
                        + " another is not decided by attachment order");
    }

    @Test
    void thePageIsContinuousAtTheRightAscensionSeamAndOnAPolarPage() {
        TestChartServices services = new TestChartServices();
        EclipticModule module = attached(services);
        module.showing(true);

        // The seam: a page centred on RA 0, where an unwrapped angle
        // would break the line.
        ChartScene seam = Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(0.0, 0.0), 36.0, 8.0),
                900, 700);
        assertTrue(differences(ordinary(seam, ChartOptions.DEFAULTS),
                        page(seam, ChartOptions.DEFAULTS,
                                services.overlays.collect())) > 0,
                "the ecliptic crosses the right-ascension seam and is"
                        + " drawn there");

        // A polar page: the ecliptic is far away and must simply be
        // absent, with no invented chord across sky it never crosses.
        ChartScene polar = Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(0.0, 88.0), 8.0, 8.0),
                900, 700);
        assertEquals(0, differences(
                        ordinary(polar, ChartOptions.DEFAULTS),
                        page(polar, ChartOptions.DEFAULTS,
                                services.overlays.collect())),
                "and on a page the ecliptic does not reach, the module"
                        + " draws nothing rather than promising a line"
                        + " that is not there");
    }

    @Test
    void switchingItOnAndOffIsPaintOnly() {
        // The module asks the chart to draw again; it never asks it
        // to move, to reassemble, or to query anything.
        TestChartServices services = new TestChartServices();
        EclipticModule module = new EclipticModule();
        module.attach(services);

        int before = services.redraws;
        module.showing(true);
        assertEquals(before + 1, services.redraws,
                "showing it asks for one repaint");
        assertEquals(0, services.requested.size(),
                "and moves the chart nowhere");
        assertEquals(0, services.inventoryReads,
                "and asks nothing about what is on the page");

        module.showing(false);
        assertEquals(before + 2, services.redraws,
                "and hiding it asks for one more");
        assertEquals(0, services.requested.size(),
                "still moving nothing");
    }
}
