package juranometria.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.geo.GeoSegment;
import juranometria.project.PixelPoint;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A constellation keeps its own stars (Sprint 30, issue #307).
 *
 * <p>Found by the owner looking at a finished overview page: the
 * lines of a familiar figure were there and one of its nodes was
 * not. The overview's brighter default magnitude limits - which the
 * gate measured and which are right about density - were removing
 * stars that figure segments are drawn to.
 *
 * <p>Measured on the painted page, before anything was changed, on
 * the Orion pages at the defaults #299 settled:
 *
 * <pre>
 * 60 degrees at V 5.0    72 endpoints on the page,   0 with no star
 * 90 degrees at V 4.0   120 endpoints on the page,  42 with no star
 * 120 degrees at V 4.0  189 endpoints on the page,  74 with no star
 * </pre>
 *
 * <p>Orion at 60 degrees never had the defect: at V 5.0 every figure
 * star of that page is already admitted. It is kept here anyway,
 * because a rung that is right by accident is worth watching - and
 * this one is right by accident, the same rung centred on Sagittarius
 * being four endpoints short
 * (docs/studies/figure-anchors/measurements.md).
 *
 * <p>What this must not do is as important as what it must. The
 * density the gate measured stands; nothing below the limit comes in
 * except the figures' own stars; no label is promoted; and with the
 * figures switched off the exception goes with them.
 */
class FigureAnchorTest {

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);
    private static final int WIDE = 900;
    private static final int HIGH = 700;
    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private static ChartScene page(double field, double magnitude) {
        return Atlas.assembler().assemble(
                new ChartViewState(ORION, field, magnitude), WIDE, HIGH);
    }

    private static ChartScene page(double field) {
        return page(field, ChartViewState.defaultMagnitudeFor(field));
    }

    /**
     * Every endpoint of every figure segment this scene carries.
     *
     * <p>Which is more than the page shows: the geography reaches
     * past the paper, and the policy anchors a segment's stars
     * whether or not the segment's end falls on it. A star kept for a
     * figure just off the edge is a star kept for a figure.
     */
    private static List<SkyPosition> endpointsIn(ChartScene scene) {
        List<SkyPosition> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (GeoSegment segment : scene.geography().figureSegments()) {
            for (SkyPosition end
                    : List.of(segment.from(), segment.to())) {
                if (seen.add(end.raDegrees() + "," + end.decDegrees())) {
                    found.add(end);
                }
            }
        }
        return found;
    }

    /** Every figure endpoint this page actually draws on its paper. */
    private static List<SkyPosition> endpointsOn(ChartScene scene) {
        var mapping = new ViewportMapping(juranometria.project.DrawnPage.of(scene));
        var projection = Projections.forViewport(scene.viewport());
        List<SkyPosition> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (GeoSegment segment : scene.geography().figureSegments()) {
            for (SkyPosition end
                    : List.of(segment.from(), segment.to())) {
                var plane = projection.project(end);
                if (plane.isEmpty()) {
                    continue;
                }
                PixelPoint at = mapping.toPixel(plane.get());
                // Inside the area the chart draws in, which is its own
                // clip: an endpoint on the border itself is under the
                // frame, where the page paints no star either.
                if (at.x() < 1.0 || at.y() < 1.0
                        || at.x() > scene.viewport().widthPx() - 2.0
                        || at.y() > scene.viewport().heightPx() - 2.0) {
                    continue;
                }
                if (seen.add(end.raDegrees() + "," + end.decDegrees())) {
                    found.add(end);
                }
            }
        }
        return found;
    }

    /**
     * The star mark the page drew at this endpoint, or none.
     *
     * <p>The nearest, and where two are equally near the brighter -
     * the policy's own rule. Taking the first mark inside the
     * tolerance reported that a deeper page had changed which stars a
     * figure is made of, when what had changed was that a companion
     * was drawn and came first; taking the last of an equal pair
     * reported the same thing about a double. The ordering ambiguity
     * this issue found in the policy, made twice more in the test
     * that checks it.
     */
    private static ChartRenderer.DrawnMark nodeAt(
            List<ChartRenderer.DrawnMark> marks, SkyPosition endpoint) {
        ChartRenderer.DrawnMark best = null;
        double closest = FigureAnchors.SAME_STAR_DEGREES;
        for (ChartRenderer.DrawnMark mark : marks) {
            if (mark.star() == null) {
                continue;
            }
            double apart = mark.star().position()
                    .separationDegrees(endpoint);
            if (apart > closest) {
                continue;
            }
            if (apart < closest || best == null
                    || mark.star().magnitude() < best.star().magnitude()) {
                closest = apart;
                best = mark;
            }
        }
        return best;
    }

    private static List<ChartRenderer.DrawnMark> marksOf(ChartScene scene,
                                                         ChartOptions options) {
        return RENDERER.drawnMarks(scene, options);
    }

    /**
     * A page as painted, beside the same page with the stars that can
     * ink its figure endpoints withheld.
     *
     * <p>Ink at an endpoint proves nothing on its own: the figure's
     * own line ends there, under the marks, and would answer for the
     * star it was drawn to - the defect reporting itself repaired. The
     * two paintings differ in one thing, so the lines, the grid, the
     * boundaries and the furniture are laid down identically and a
     * pixel that changes is a star.
     *
     * <p>What the pixels cannot say is <em>which</em> star: on a
     * 120-degree page a companion a hundredth of a degree away paints
     * the same disc in the same place. Identity is read from the
     * renderer's published placements, which is not a second opinion
     * about the page but the geometry the page is painted from
     * (issue #168).
     */
    private record Painted(ChartScene scene, java.awt.image.BufferedImage page,
                           java.awt.image.BufferedImage withheld) {

        boolean nodeAt(SkyPosition endpoint) {
            var mapping = new ViewportMapping(juranometria.project.DrawnPage.of(scene));
            var plane = Projections.forViewport(scene.viewport())
                    .project(endpoint);
            if (plane.isEmpty()) {
                return false;
            }
            PixelPoint at = mapping.toPixel(plane.get());
            int centreX = (int) Math.round(at.x());
            int centreY = (int) Math.round(at.y());
            for (int y = centreY - 1; y <= centreY + 1; y++) {
                for (int x = centreX - 1; x <= centreX + 1; x++) {
                    if (x < 0 || y < 0 || x >= page.getWidth()
                            || y >= page.getHeight()) {
                        continue;
                    }
                    if (page.getRGB(x, y) != withheld.getRGB(x, y)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static Painted painted(ChartScene scene, ChartOptions options) {
        Set<String> ids = inkingNear(scene, endpointsOn(scene));
        return new Painted(scene, paint(scene, options),
                paint(without(scene, ids), options));
    }

    private static java.awt.image.BufferedImage paint(ChartScene scene,
                                                      ChartOptions options) {
        var image = new java.awt.image.BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, options);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** The same page with these stars withheld, and nothing else. */
    private static ChartScene without(ChartScene scene, Set<String> ids) {
        List<Star> kept = new ArrayList<>();
        for (Star star : scene.stars()) {
            if (!ids.contains(star.id())) {
                kept.add(star);
            }
        }
        return new ChartScene(scene.viewport(), kept,
                scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), scene.targetIdentity(),
                scene.geography());
    }

    /**
     * Every star whose own disc, at its own size, can reach the pixels
     * a node is read from - not merely the star an endpoint is. A
     * neighbour that shares the endpoint's pixels would otherwise go
     * on painting a node after the node was withheld.
     */
    private static Set<String> inkingNear(ChartScene scene,
                                          List<SkyPosition> endpoints) {
        var mapping = new ViewportMapping(juranometria.project.DrawnPage.of(scene));
        var projection = Projections.forViewport(scene.viewport());
        List<PixelPoint> at = new ArrayList<>();
        for (SkyPosition endpoint : endpoints) {
            projection.project(endpoint)
                    .ifPresent(plane -> at.add(mapping.toPixel(plane)));
        }
        Set<String> ids = new HashSet<>();
        for (Star star : scene.stars()) {
            var plane = projection.project(star.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint drawn = mapping.toPixel(plane.get());
            double reach = StarSizePolicy.DEFAULT
                    .radiusFor(star.magnitude()) + 2.0;
            for (PixelPoint endpoint : at) {
                if (Math.hypot(drawn.x() - endpoint.x(),
                        drawn.y() - endpoint.y()) <= reach) {
                    ids.add(star.id());
                    break;
                }
            }
        }
        return ids;
    }

    /** The same options with the page's own furniture switched off. */
    private static ChartOptions withoutFurniture() {
        ChartOptions on = ChartOptions.DEFAULTS;
        return new ChartOptions(on.deepSkyObjects(), on.deepSkyLabels(),
                on.constellationFigures(), on.constellationBoundaries(),
                on.constellationNames(), on.starNames(), on.bayerLetters(),
                on.flamsteedNumbers(), on.equatorialGrid(), false, false,
                on.galaxies(), on.openClusters(), on.globularClusters(),
                on.nebulae(), on.planetaryNebulae(), on.palette());
    }

    @Test
    void everyFigureEndpointOnThePageHasItsStar() {
        // The defect, in the terms it was found in: a drawn segment
        // whose end is a place where no star is drawn - and read off
        // the painted page, because that is where it was found.
        //
        // With the exception removed and nothing else changed, the
        // painted count returns to 42 of 120 endpoints at 90 degrees
        // and 74 of 189 at 120
        // (docs/studies/figure-anchors/measurements.md).
        for (double field : new double[] {60.0, 90.0, 120.0}) {
            ChartScene scene = page(field);
            List<SkyPosition> endpoints = endpointsOn(scene);
            assertTrue(endpoints.size() > 60,
                    field + " degrees: the page carries figures to"
                            + " check: " + endpoints.size());
            Painted reader = painted(scene, ChartOptions.DEFAULTS);
            Painted bare = painted(scene, withoutFurniture());
            int covered = 0;
            for (SkyPosition endpoint : endpoints) {
                if (reader.nodeAt(endpoint)) {
                    continue;
                }
                // The one thing that legitimately hides a drawn star:
                // the chart's own furniture, painted opaque over the
                // sky and switched off here to see underneath. It
                // happens on the released 42-degree page too, where
                // no star is held back for a figure at all.
                covered++;
                assertTrue(bare.nodeAt(endpoint),
                        field + " degrees at V "
                                + ChartViewState.defaultMagnitudeFor(field)
                                + ": the endpoint at " + endpoint
                                + " has no star painted at it, with the"
                                + " page's furniture off");
            }
            assertTrue(covered <= 6, field + " degrees: the furniture"
                    + " covers a handful of nodes, not a page of them: "
                    + covered);
        }
    }

    @Test
    void nothingElseBelowTheLimitComesWithThem() {
        // The other half, and the one that keeps the gate's measured
        // density: a star below the limit is on the page only if a
        // figure is drawn to it.
        for (double field : new double[] {60.0, 90.0, 120.0}) {
            ChartScene scene = page(field);
            double limit = ChartViewState.defaultMagnitudeFor(field);
            List<SkyPosition> endpoints = endpointsIn(scene);
            int kept = 0;
            for (ChartRenderer.DrawnMark mark
                    : marksOf(scene, ChartOptions.DEFAULTS)) {
                if (mark.star() == null
                        || mark.star().magnitude() <= limit) {
                    continue;
                }
                kept++;
                boolean anchors = false;
                for (SkyPosition endpoint : endpoints) {
                    if (mark.star().position()
                            .separationDegrees(endpoint)
                            <= FigureAnchors.SAME_STAR_DEGREES) {
                        anchors = true;
                    }
                }
                assertTrue(anchors, field + " degrees: "
                        + mark.star().id() + " at V "
                        + mark.star().magnitude() + " is below the"
                        + " limit and is not a figure's own star");
            }
            if (field > 60.0) {
                assertTrue(kept > 20, field + " degrees: the page"
                        + " really is keeping stars its limit would"
                        + " have hidden: " + kept);
            }
        }
    }

    @Test
    void anAnchorIsDrawnAtItsOwnBrightnessAndNoLarger() {
        // The size question the owner settled: an anchor is kept, not
        // promoted. Its size states its magnitude, and it is no
        // brighter for being structural.
        //
        // No floor was added, and the measurement is why. The faintest
        // anchor the pack produces over three centres is V 6.5, which
        // the existing policy draws at 2.30 px - larger than the
        // 1.32 px marks the released Home page has drawn since 1.0. A
        // star smaller than this is on every page the atlas ships
        // (docs/studies/figure-anchors/measurements.md).
        ChartScene scene = page(120.0);
        double limit = ChartViewState.defaultMagnitudeFor(120.0);
        double faintestAnchor = 0.0;
        double smallestAnchor = Double.MAX_VALUE;
        for (ChartRenderer.DrawnMark mark
                : marksOf(scene, ChartOptions.DEFAULTS)) {
            if (mark.star() == null
                    || mark.star().magnitude() <= limit) {
                continue;
            }
            assertEquals(StarSizePolicy.DEFAULT.radiusFor(
                            mark.star().magnitude()),
                    mark.reach(), 1.0e-9,
                    mark.star().id() + " is drawn at the size its own"
                            + " magnitude asks for");
            faintestAnchor = Math.max(faintestAnchor,
                    mark.star().magnitude());
            smallestAnchor = Math.min(smallestAnchor, mark.reach());
        }
        assertTrue(faintestAnchor > limit, "there is an anchor fainter"
                + " than the page's limit to measure");

        double home = Double.MAX_VALUE;
        for (ChartRenderer.DrawnMark mark : marksOf(
                Atlas.assembler().assemble(ChartViewState.DEFAULT,
                        WIDE, HIGH), ChartOptions.DEFAULTS)) {
            if (mark.star() != null) {
                home = Math.min(home, mark.reach());
            }
        }
        assertTrue(smallestAnchor > home,
                "the faintest anchor at " + smallestAnchor + " px is a"
                        + " larger mark than the " + home + " px stars"
                        + " the released Home page draws, so it is a"
                        + " node by the atlas's own standard and needs"
                        + " no floor of its own");
    }

    @Test
    void noAnchorIsGivenALabelItHadNotEarned() {
        // A node and a name are different promises. Keeping a star so
        // a shape is complete says nothing about whether the reader
        // was going to be told what it is called.
        //
        // Held twice over, and it is worth saying which guard is
        // load-bearing. Opening the label pass's own magnitude gate -
        // the mutation this issue asks for - changes nothing, because
        // the label policy's thresholds at these fields are brighter
        // than the page's limit: a name needs V 2.5 and a Bayer
        // letter V 3.5, where the page itself stops at V 4.0. An
        // anchor is below both. So the second assertion below is the
        // one that would catch a future field whose limits crossed,
        // and the first would then catch the promotion itself.
        for (double field : new double[] {60.0, 90.0, 120.0}) {
            StarLabelPolicy policy = new StarLabelPolicy(field);
            double limit = ChartViewState.defaultMagnitudeFor(field);
            assertTrue(policy.nameLimit() <= limit
                            && policy.bayerLimit() <= limit
                            && policy.flamsteedLimit() <= limit,
                    field + " degrees: no star below the page's own"
                            + " limit of V " + limit + " can qualify"
                            + " for a label at all - name V "
                            + policy.nameLimit() + ", letter V "
                            + policy.bayerLimit() + ", number V "
                            + policy.flamsteedLimit());
        }
        ChartScene scene = page(120.0);
        double limit = ChartViewState.defaultMagnitudeFor(120.0);
        var mapping = new ViewportMapping(juranometria.project.DrawnPage.of(scene));
        var labels = RENDERER.starLabelPlacements(
                ChartRenderer.TextMetrics.offscreen(),
                scene, ChartOptions.DEFAULTS);
        assertFalse(labels.isEmpty(), "the page labels its bright stars");
        for (var placed : labels) {
            assertTrue(placed.star().magnitude() <= limit,
                    placed.star().id() + " is labelled at V "
                            + placed.star().magnitude() + " on a page"
                            + " limited at V " + limit);
        }
    }

    @Test
    void switchingTheFiguresOffTakesTheExceptionWithThem() {
        // The exception exists to complete a shape. With no shape
        // drawn there is nothing to complete, and a star kept for one
        // would be an ordinary star below the limit - which is what
        // the density policy is for.
        ChartScene scene = page(120.0);
        double limit = ChartViewState.defaultMagnitudeFor(120.0);
        ChartOptions on = ChartOptions.DEFAULTS;
        ChartOptions without = new ChartOptions(on.deepSkyObjects(),
                on.deepSkyLabels(), false, on.constellationBoundaries(),
                on.constellationNames(), on.starNames(),
                on.bayerLetters(), on.flamsteedNumbers(),
                on.equatorialGrid(), on.titleBlock(), on.magnitudeKey(),
                on.galaxies(), on.openClusters(), on.globularClusters(),
                on.nebulae(), on.planetaryNebulae(), on.palette());
        for (ChartRenderer.DrawnMark mark : marksOf(scene, without)) {
            assertTrue(mark.star() == null
                            || mark.star().magnitude() <= limit,
                    "with figures off, " + (mark.star() == null ? ""
                            : mark.star().id()) + " should not be"
                            + " kept below the limit");
        }
        assertTrue(marksOf(scene, ChartOptions.DEFAULTS).size()
                        > marksOf(scene, without).size(),
                "and with them on, the page keeps more");
    }

    @Test
    void reachingForMoreStarsAddsTheFieldWithoutMovingTheFigure()
            throws Exception {
        // The reader's own magnitude control, pressed - not a state
        // built here with a fainter limit written into it. What the
        // control must not do is change which stars a constellation is
        // made of: the nodes are the same nodes, with the sky filled
        // in around them.
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "pressing a real control needs a real window");

        javax.swing.JFrame[] window = new javax.swing.JFrame[1];
        juranometria.ui.ChartComponent[] chart =
                new juranometria.ui.ChartComponent[1];
        juranometria.ui.ChartViewController navigation =
                new juranometria.ui.ChartViewController(
                        Atlas.assembler()::fits);
        juranometria.app.SwingSession.guarded(() -> {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                chart[0] = new juranometria.ui.ChartComponent(
                        Atlas.assembler());
                navigation.onChange(chart[0]::setViewState);
                navigation.recenter(ORION, 120.0);
                window[0] = new javax.swing.JFrame("figure anchors");
                window[0].setLayout(new java.awt.BorderLayout());
                window[0].add(new juranometria.ui.AtlasToolbar(navigation,
                                new juranometria.ui.SearchField(
                                        Atlas.search(), Atlas.assembler(),
                                        navigation)),
                        java.awt.BorderLayout.NORTH);
                window[0].add(chart[0], java.awt.BorderLayout.CENTER);
                window[0].setSize(1100, 800);
                window[0].setVisible(true);
            });
            javax.swing.SwingUtilities.invokeAndWait(() -> { });

            ChartScene bright = onEdt(chart[0]::currentScene);
            assertEquals(ChartViewState.defaultMagnitudeFor(120.0),
                    bright.limitingMagnitude(),
                    "the wide page arrives at its own limit");
            // What the window puts on the screen, painted by the
            // window: the evidence below is read from these pixels
            // rather than from a page rendered beside them.
            java.awt.image.BufferedImage before =
                    onEdt(() -> paintOf(chart[0]));
            assertEquals(0L, differing(before,
                            onEdt(() -> referenceOf(chart[0]))),
                    "the window paints this scene and nothing else, so"
                            + " what is measured of the page is measured"
                            + " of what the reader is looking at");
            Set<String> nodesBefore = nodesOf(bright);

            // Two presses of the control a reader has.
            javax.swing.JButton more = onEdt(() -> button(
                    window[0].getContentPane(), "More stars"));
            assertTrue(more != null, "the reader has a More stars control");
            juranometria.ui.ReaderInput.click(more);
            juranometria.ui.ReaderInput.click(more);

            ChartScene deeper = onEdt(chart[0]::currentScene);
            assertTrue(deeper.limitingMagnitude()
                            > bright.limitingMagnitude(),
                    "and pressing it reached fainter: V "
                            + bright.limitingMagnitude() + " to V "
                            + deeper.limitingMagnitude());
            java.awt.image.BufferedImage after =
                    onEdt(() -> paintOf(chart[0]));
            assertEquals(0L, differing(after,
                            onEdt(() -> referenceOf(chart[0]))),
                    "and the deeper page too");
            assertEquals(nodesBefore, nodesOf(deeper),
                    "the same stars define the figures at either limit");

            // The field came in, counted as ink rather than as marks.
            // The two paintings are the same page at two limits, so a
            // pixel that changed is a star the control admitted - with
            // one exception, which is not a star and is not sky: the
            // title block states the limiting magnitude and the key
            // draws its rows down to it, so both restate themselves.
            // The furniture's own region is measured rather than
            // guessed - it is where switching the furniture off
            // changes the page - and counted apart from the sky.
            boolean[] furniture = furnitureMask(bright, deeper);
            int offset = onEdt(chart[0]::pageOffsetY);
            long sky = 0;
            long block = 0;
            for (int y = 0; y < bright.viewport().heightPx(); y++) {
                for (int x = 0; x < bright.viewport().widthPx(); x++) {
                    if (before.getRGB(x, y + offset)
                            == after.getRGB(x, y + offset)) {
                        continue;
                    }
                    if (furniture[y * bright.viewport().widthPx() + x]) {
                        block++;
                    } else {
                        sky++;
                    }
                }
            }
            assertTrue(sky > 10000, "and the surrounding field came in:"
                    + " " + sky + " pixels of sky changed, against"
                    + " 35691 when this was measured");
            assertTrue(block > 0 && block < sky / 100, "while the"
                    + " furniture restates the limit it now draws to,"
                    + " which is neither a star nor sky: " + block
                    + " pixels, against 185 when this was measured -"
                    + " inside the blocks' own region and nowhere"
                    + " else");
        }, () -> javax.swing.SwingUtilities.invokeAndWait(() -> {
            if (window[0] != null) {
                window[0].dispose();
            }
        }));
    }

    /** Which stars this page's figures are drawn to, and painted at. */
    private static Set<String> nodesOf(ChartScene scene) {
        Set<String> nodes = new HashSet<>();
        var marks = marksOf(scene, ChartOptions.DEFAULTS);
        Painted reader = painted(scene, ChartOptions.DEFAULTS);
        Painted bare = painted(scene, withoutFurniture());
        for (SkyPosition endpoint : endpointsOn(scene)) {
            ChartRenderer.DrawnMark node = nodeAt(marks, endpoint);
            assertTrue(node != null, "every endpoint has its node");
            assertTrue(reader.nodeAt(endpoint) || bare.nodeAt(endpoint),
                    "and the page paints it: " + node.star().id()
                            + " at V " + node.star().magnitude());
            nodes.add(node.star().id());
        }
        return nodes;
    }

    /** The component's own painting, as it puts it on the screen. */
    private static java.awt.image.BufferedImage paintOf(
            javax.swing.JComponent component) {
        var image = new java.awt.image.BufferedImage(
                component.getWidth(), component.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            component.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** The same picture, made by the renderer instead of the window. */
    private static java.awt.image.BufferedImage referenceOf(
            juranometria.ui.ChartComponent chart) {
        var image = new java.awt.image.BufferedImage(chart.getWidth(),
                chart.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            g.setColor(chart.getBackground());
            g.fillRect(0, 0, chart.getWidth(), chart.getHeight());
            g.translate(0, chart.pageOffsetY());
            RENDERER.render(g, chart.currentScene(), chart.chartOptions());
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Where the page's own furniture lands, in page pixels.
     *
     * <p>Measured, not taken from the blocks' published bounds: what
     * matters here is which pixels the furniture is answerable for,
     * and that is exactly where switching it off changes the page.
     * Grown by two pixels so an antialiased edge belongs to the block
     * that drew it rather than to the sky.
     */
    private static boolean[] furnitureMask(ChartScene... scenes) {
        int wide = scenes[0].viewport().widthPx();
        int high = scenes[0].viewport().heightPx();
        boolean[] drawn = new boolean[wide * high];
        for (ChartScene scene : scenes) {
            var page = paint(scene, ChartOptions.DEFAULTS);
            var bare = paint(scene, withoutFurniture());
            for (int y = 0; y < high; y++) {
                for (int x = 0; x < wide; x++) {
                    if (page.getRGB(x, y) != bare.getRGB(x, y)) {
                        drawn[y * wide + x] = true;
                    }
                }
            }
        }
        boolean[] grown = new boolean[wide * high];
        for (int y = 0; y < high; y++) {
            for (int x = 0; x < wide; x++) {
                if (!drawn[y * wide + x]) {
                    continue;
                }
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        int at = x + dx;
                        int down = y + dy;
                        if (at >= 0 && down >= 0 && at < wide
                                && down < high) {
                            grown[down * wide + at] = true;
                        }
                    }
                }
            }
        }
        return grown;
    }

    private static long differing(java.awt.image.BufferedImage one,
                                  java.awt.image.BufferedImage other) {
        assertEquals(one.getWidth() + "x" + one.getHeight(),
                other.getWidth() + "x" + other.getHeight(),
                "two paintings of the same page");
        long count = 0;
        for (int y = 0; y < one.getHeight(); y++) {
            for (int x = 0; x < one.getWidth(); x++) {
                if (one.getRGB(x, y) != other.getRGB(x, y)) {
                    count++;
                }
            }
        }
        return count;
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

    /** A toolbar control, by the name it announces. */
    private static javax.swing.JButton button(java.awt.Component root,
                                              String name) {
        if (root instanceof javax.swing.JButton candidate
                && name.equals(candidate.getAccessibleContext()
                        .getAccessibleName())) {
            return candidate;
        }
        if (root instanceof java.awt.Container inner) {
            for (java.awt.Component child : inner.getComponents()) {
                javax.swing.JButton found = button(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void anEndpointIsOneStarAndNotItsNeighbours() {
        // Thirty of the pack's matchable endpoints have a second star
        // within the tolerance. Keeping everything inside it kept
        // those too - a magnitude 7.8 star on a page limited at V 4.0,
        // which is precisely the unrelated faint star this must not
        // admit.
        //
        // Held as the property rather than as a magnitude: every star
        // kept below the limit sits on a figure's endpoint, not near
        // one. The real matches are within a thousandth of a degree
        // and the tolerance is ten times that, so a companion picked
        // up at the edge would show here as a loose match.
        for (double field : new double[] {90.0, 120.0}) {
            ChartScene scene = page(field);
            double limit = ChartViewState.defaultMagnitudeFor(field);
            List<SkyPosition> endpoints = endpointsIn(scene);
            for (var mark : marksOf(scene, ChartOptions.DEFAULTS)) {
                if (mark.star() == null
                        || mark.star().magnitude() <= limit) {
                    continue;
                }
                double closest = Double.MAX_VALUE;
                for (SkyPosition endpoint : endpoints) {
                    closest = Math.min(closest, mark.star().position()
                            .separationDegrees(endpoint));
                }
                assertTrue(closest < 0.002,
                        mark.star().id() + " sits on a figure's own"
                                + " endpoint rather than near one: "
                                + closest + " degrees away");
            }

            // And exactly one star per endpoint, which proximity
            // alone does not say: two stars can both sit on an
            // endpoint, and keeping both is what admitted a V 7.8
            // companion. The one kept is the brightest there.
            for (SkyPosition endpoint : endpoints) {
                List<Star> kept = new ArrayList<>();
                for (var mark : marksOf(scene, ChartOptions.DEFAULTS)) {
                    if (mark.star() != null
                            && mark.star().magnitude() > limit
                            && mark.star().position()
                                    .separationDegrees(endpoint)
                                    <= FigureAnchors.SAME_STAR_DEGREES) {
                        kept.add(mark.star());
                    }
                }
                assertTrue(kept.size() <= 1, field + " degrees: an"
                        + " endpoint keeps one star, not " + kept.size()
                        + ": " + kept.stream().map(Star::id).toList());
                if (kept.size() == 1) {
                    // The nearest, with brightness breaking an exact
                    // tie and nothing else. A first draft of this
                    // asked for the brightest inside the tolerance and
                    // found Cancer's figure ending on a wide pair
                    // 0.0085 degrees apart, where the endpoint sits
                    // 0.000043 degrees from the fainter star and the
                    // brighter is two hundred times further off. The
                    // dataset names a star by its coordinates, and
                    // those coordinates are that star's.
                    for (Star star : scene.stars()) {
                        double there = star.position()
                                .separationDegrees(endpoint);
                        if (there > FigureAnchors.SAME_STAR_DEGREES) {
                            continue;
                        }
                        double ours = kept.get(0).position()
                                .separationDegrees(endpoint);
                        assertTrue(ours < there
                                        || (ours == there
                                                && kept.get(0).magnitude()
                                                        <= star.magnitude()),
                                "the star kept at this endpoint is the"
                                        + " nearest to it, or the"
                                        + " brighter of two equally"
                                        + " near: kept " + kept.get(0).id()
                                        + " at " + ours + " degrees over "
                                        + star.id() + " at " + there);
                    }
                }
            }
        }
    }

    @Test
    void aDoublesFigureNodeIsTheStarAReaderSees() {
        // Andromeda's figure ends on a pair the pack records at the
        // same position: V 4.3 and V 7.8, separated by zero. Taking
        // whichever the scan reached last kept the faint one - a node
        // three magnitudes dimmer than the star the figure is named
        // for, and a fainter star admitted for nothing.
        //
        // On the page that carries that figure, which is not Orion's:
        // a page whose geography does not reach Andromeda has no such
        // endpoint and would pass this without testing anything.
        ChartScene scene = Atlas.assembler().assemble(
                new ChartViewState(
                        new SkyPosition(10.684708, 41.268750), 120.0,
                        ChartViewState.defaultMagnitudeFor(120.0)),
                WIDE, HIGH);
        var marks = marksOf(scene, ChartOptions.DEFAULTS);
        Star faint = null;
        Star bright = null;
        for (Star star : scene.stars()) {
            if ("TYC 3268-1358-2".equals(star.id())) {
                faint = star;
            }
            if ("TYC 3268-1358-1".equals(star.id())) {
                bright = star;
            }
        }
        assertTrue(faint != null && bright != null,
                "the pack still records both components");
        assertTrue(bright.magnitude() < faint.magnitude(),
                "and the first is the brighter: V " + bright.magnitude()
                        + " against V " + faint.magnitude());
        assertEquals(0.0,
                bright.position().separationDegrees(faint.position()),
                1.0e-9, "at the same recorded place");

        boolean keptBright = false;
        boolean keptFaint = false;
        for (var mark : marks) {
            if (mark.star() == null) {
                continue;
            }
            keptBright |= bright.id().equals(mark.star().id());
            keptFaint |= faint.id().equals(mark.star().id());
        }
        assertTrue(keptBright, "the figure's node is the star a reader"
                + " sees: " + bright.id() + " at V "
                + bright.magnitude());
        assertFalse(keptFaint, "and its faint component is not admitted"
                + " with it");
    }
}
