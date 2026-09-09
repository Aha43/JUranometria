package juranometria.render;

import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a page's text and figure geometry is, asked of the renderer
 * rather than rebuilt beside it (Sprint 31, issue #313).
 *
 * <p>The seam is held to its own arithmetic in {@code
 * LabelPlacementTest}, with no toolkit in sight. This is the other
 * side of the boundary: that what production hands over is what
 * production draws, on both projections and at both extents.
 */
class LabelGeometryTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private static FontMetrics metrics() {
        java.awt.image.BufferedImage scratch =
                new java.awt.image.BufferedImage(1, 1,
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = scratch.createGraphics();
        try {
            return g.getFontMetrics(ChartRenderer.labelFont());
        } finally {
            g.dispose();
        }
    }

    private static ChartScene page(double ra, double dec, double field,
                                   int wide, int high) {
        return Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(ra, dec), field,
                ChartViewState.defaultMagnitudeFor(field)), wide, high);
    }

    @Test
    void aSymbolPublishesItsInkAndNotItsSilhouette() {
        // Praesepe's ring is a dotted circle around nothing. The
        // silhouette is what a reader aims at (#168) and claims the
        // middle; the ink does not, and a label inside the ring
        // covers nothing at all.
        ChartScene scene = page(130.1, 19.67, 3.0, 900, 700);
        ChartRenderer.DrawnMark ring = null;
        for (ChartRenderer.DrawnMark mark
                : RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (mark.deepSky() != null
                    && ChartRenderer.symbolFor(mark.deepSky())
                            == ChartRenderer.Symbol.DOTTED_CIRCLE
                    && (ring == null || mark.reach() > ring.reach())) {
                ring = mark;
            }
        }
        assertTrue(ring != null && ring.reach() > 20.0,
                "the page draws an open cluster with room inside it");

        Rectangle2D middle = new Rectangle2D.Double(
                ring.centre().x() - 4.0, ring.centre().y() - 4.0, 8.0, 8.0);
        assertTrue(ring.outline().intersects(middle),
                "its silhouette claims the middle");
        assertFalse(ring.ink().intersects(middle),
                "and its ink does not");
        assertTrue(new java.awt.geom.Area(ring.ink()).isEmpty()
                        == false,
                "while there is ink to speak of");
    }

    @Test
    void aStarsInkIsItsDisc() {
        ChartScene scene = page(83.0, 0.0, 8.0, 900, 700);
        int checked = 0;
        for (ChartRenderer.DrawnMark mark
                : RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (mark.star() == null) {
                continue;
            }
            assertEquals(mark.outline(), mark.ink(),
                    mark.star().id() + ": a star's mark is a filled disc,"
                            + " so its ink is its outline");
            checked++;
        }
        assertTrue(checked > 100, "on a page full of stars: " + checked);
    }

    @Test
    void aFiguresInkIsItsDrawnPiecesAndNotItsChord() {
        // The property the gate insists on: a curve is not its
        // endpoints and not the straight line between them. A
        // constellation figure on a 120-degree stereographic page
        // bends, and its published ink bends with it.
        ChartScene scene = page(266.0, -28.0, 120.0, 900, 700);
        Map<String, ChartRenderer.FigureInk> ink =
                RENDERER.figureInk(scene, ChartOptions.DEFAULTS);
        assertFalse(ink.isEmpty(), "the page draws figures");

        // Piece by piece, along each chain of pieces that join end to
        // end: how far the drawn line departs from the straight one
        // between that chain's own ends. A chord would give zero.
        double worst = 0.0;
        String bent = null;
        int chains = 0;
        for (ChartRenderer.FigureInk figure : ink.values()) {
            for (List<double[]> chain : chainsOf(figure.ink())) {
                if (chain.size() < 3) {
                    continue;
                }
                chains++;
                double[] from = chain.get(0);
                double[] to = chain.get(chain.size() - 1);
                for (double[] point : chain) {
                    double away = distanceFromLine(point, from, to);
                    if (away > worst) {
                        worst = away;
                        bent = figure.constellationId();
                    }
                }
            }
        }
        assertTrue(chains > 10, "the page draws figures in many pieces:"
                + " " + chains + " chains");
        assertTrue(worst > 2.0, "and a drawn figure leaves the chord"
                + " between its own ends: " + bent + " by "
                + String.format(java.util.Locale.ROOT, "%.1f", worst)
                + " px, where a chord would give none");
    }

    /** The chains of pieces that join end to end, in drawing order. */
    private static List<List<double[]>> chainsOf(Shape ink) {
        List<List<double[]>> chains = new java.util.ArrayList<>();
        List<double[]> chain = new java.util.ArrayList<>();
        double[] point = new double[6];
        double[] last = null;
        for (java.awt.geom.PathIterator along =
                ink.getPathIterator(null); !along.isDone(); along.next()) {
            int kind = along.currentSegment(point);
            double[] here = new double[] {point[0], point[1]};
            if (kind == java.awt.geom.PathIterator.SEG_MOVETO) {
                if (last == null || Math.hypot(here[0] - last[0],
                        here[1] - last[1]) > 1.0e-6) {
                    if (chain.size() > 1) {
                        chains.add(chain);
                    }
                    chain = new java.util.ArrayList<>();
                    chain.add(here);
                }
            } else {
                chain.add(here);
            }
            last = here;
        }
        if (chain.size() > 1) {
            chains.add(chain);
        }
        return chains;
    }

    @Test
    void aFiguresNameIsAnchoredWhereTheRendererAnchorsIt() {
        // The anchor is published rather than re-derived, and this is
        // what that buys: on the 90-degree Orion page Pyxis is drawn
        // across the paper with no segment end on it at all, so a
        // rule built from endpoints does not name it and the atlas
        // does.
        ChartScene scene = page(83.0, 0.0, 90.0, 900, 700);
        Map<String, ChartRenderer.FigureInk> ink =
                RENDERER.figureInk(scene, ChartOptions.DEFAULTS);
        ChartRenderer.FigureInk pyxis = ink.get("Pyx");
        assertTrue(pyxis != null, "Pyxis leaves ink on this page");
        assertTrue(pyxis.nameAnchor() != null,
                "and has somewhere to hang its name");
        assertTrue(new java.awt.geom.Area(
                        LabelGeometry.hullOf(pyxis.ink()))
                        .contains(pyxis.nameAnchor().x(),
                                pyxis.nameAnchor().y()),
                "inside the region its own ink owns");
    }

    @Test
    void everyPublishedFigureOwnsItsOwnInk() {
        // The region a name may move in contains the figure it names.
        // Asked of every constellation the page draws, and of every
        // point of the ink rather than of a bounding box's centre.
        ChartScene scene = page(266.0, -28.0, 120.0, 900, 700);
        int checked = 0;
        for (ChartRenderer.FigureInk figure
                : RENDERER.figureInk(scene, ChartOptions.DEFAULTS)
                        .values()) {
            Shape hull = LabelGeometry.hullOf(figure.ink());
            if (hull == null) {
                continue;
            }
            java.awt.geom.Area region = new java.awt.geom.Area(hull);
            region.add(new java.awt.geom.Area(new java.awt.BasicStroke(2.0f)
                    .createStrokedShape(hull)));
            for (double[] point : pointsOf(figure.ink())) {
                assertTrue(region.contains(point[0], point[1]),
                        figure.constellationId() + ": every piece of its"
                                + " ink is inside the region it owns");
            }
            checked++;
        }
        assertTrue(checked > 10, "on a useful number of figures: "
                + checked);
    }

    @Test
    void bothProjectionsAndBothExtentsAskAndAnswer() {
        // Gnomonic and stereographic, the screen and the A4 sheet's
        // own chart area: the seam is handed a page of requests and
        // places every one of them, and no request is dropped.
        for (int[] extent : new int[][] {{900, 700}, {770, 523}}) {
            for (double field : new double[] {8.0, 120.0}) {
                ChartScene scene = page(83.0, 0.0, field, extent[0],
                        extent[1]);
                FontMetrics metrics = metrics();
                List<LabelPlacement.Request> asked =
                        new java.util.ArrayList<>();
                asked.addAll(LabelGeometry.starLabels(RENDERER, metrics,
                        scene, ChartOptions.DEFAULTS));
                asked.addAll(LabelGeometry.deepSkyLabels(RENDERER, metrics,
                        scene, ChartOptions.DEFAULTS));
                asked.addAll(LabelGeometry.constellationNames(RENDERER,
                        metrics, scene, ChartOptions.DEFAULTS));
                assertFalse(asked.isEmpty(), field + "° at " + extent[0]
                        + "x" + extent[1] + ": the page has text");

                LabelPlacement placement = new LabelPlacement(extent[0],
                        extent[1], LabelGeometry.obstaclesOn(RENDERER,
                                metrics, scene, ChartOptions.DEFAULTS));
                List<LabelPlacement.Placement> placed =
                        placement.placeAll(asked);
                assertEquals(asked.size(), placed.size(),
                        field + "° at " + extent[0] + "x" + extent[1]
                                + ": every request is answered, none"
                                + " dropped");
                // Real pages do omit, and it is worth knowing exactly
                // when: a star close enough to the edge that all eight
                // of its candidates leave the paper. The atlas today
                // draws such a label clipped by the page; the decision
                // says text keeps two pixels off the edge, so the seam
                // refuses it and says so. Which of the two a reader
                // gets is #314's to settle when the families migrate.
                int omitted = 0;
                for (LabelPlacement.Placement one : placed) {
                    if (!one.omitted()) {
                        continue;
                    }
                    omitted++;
                    for (LabelPlacement.Refused refused : one.refusals()) {
                        // The two things that are not costs: a label
                        // off the paper is not a label, and a name
                        // outside its own figure is naming something
                        // else. A mark, a piece of furniture or another
                        // label can never omit anything - those the
                        // fallback pays for.
                        assertTrue(refused.kind()
                                        == LabelPlacement.Refusal.PAGE_EDGE
                                        || refused.kind()
                                        == LabelPlacement.Refusal.OWNERSHIP,
                                one.request().id() + " is omitted only"
                                        + " because every candidate it"
                                        + " had left the paper or its own"
                                        + " figure, not because"
                                        + " something was in the way: "
                                        + refused.kind());
                    }
                    assertEquals(one.request().candidates().size(),
                            one.refusals().size(),
                            one.request().id() + ": every candidate"
                                    + " accounted for");
                }
                // Not a rare case at eight degrees: a page of large
                // nebulae straddling its own edge has many objects
                // whose symbol shows and whose label would fall off
                // the paper. What must be true is that omission is
                // the EDGE's business and nobody else's - nothing
                // well inside the page is ever absent.
                for (LabelPlacement.Placement one : placed) {
                    if (!one.omitted()) {
                        continue;
                    }
                    boolean nearTheEdge =
                            one.request().anchorX() < 100.0
                                    || one.request().anchorX()
                                            > extent[0] - 100.0
                                    || one.request().anchorY() < 100.0
                                    || one.request().anchorY()
                                            > extent[1] - 100.0;
                    boolean itsOwnFigureRefused = false;
                    for (LabelPlacement.Refused refused : one.refusals()) {
                        itsOwnFigureRefused |= refused.kind()
                                == LabelPlacement.Refusal.OWNERSHIP;
                    }
                    assertTrue(nearTheEdge || itsOwnFigureRefused,
                            one.request().id() + " is omitted at "
                                    + Math.round(one.request().anchorX())
                                    + "," + Math.round(
                                            one.request().anchorY())
                                    + " - nothing in the middle of a page"
                                    + " goes missing unless its own"
                                    + " figure has nowhere to put it");
                }
                assertTrue(omitted < placed.size(), field + "° at "
                        + extent[0] + "x" + extent[1] + ": the page"
                        + " places most of what it asks for: " + omitted
                        + " of " + placed.size() + " omitted");
            }
        }
    }

    @Test
    void everyLabelTheAtlasDrawsIsOneTheSeamIsAskedFor() {
        // Twice now the geometry has asked for text on rules of its
        // own instead of production's, and both times the study read
        // the difference as the decision's cost. The two sets are the
        // same question - what does this page name? - so the renderer
        // publishing one and the geometry inventing the other is the
        // defect, and this is where it fails.
        for (ChartOptions options : List.of(ChartOptions.DEFAULTS,
                withoutDeepSkyLabels(ChartOptions.DEFAULTS))) {
            for (double field : new double[] {8.0, 36.0, 120.0}) {
                ChartScene wide = page(83.0, 0.0, field, 900, 700);
                ChartScene scene = new ChartScene(wide.viewport(),
                        wide.stars(), wide.deepSkyObjects(), wide.title(),
                        wide.limitingMagnitude(), searched(wide),
                        wide.geography());
                FontMetrics metrics = metrics();
                java.util.Set<String> asked = new java.util.HashSet<>();
                for (LabelPlacement.Request request
                        : LabelGeometry.starLabels(RENDERER, metrics,
                                scene, options)) {
                    asked.add("star:" + request.id());
                }
                for (LabelPlacement.Request request
                        : LabelGeometry.deepSkyLabels(RENDERER, metrics,
                                scene, options)) {
                    asked.add("deep sky:" + request.id());
                }
                for (LabelPlacement.Request request
                        : LabelGeometry.constellationNames(RENDERER,
                                metrics, scene, options)) {
                    asked.add("name:" + request.id());
                }

                String where = field + "° with deep-sky labels "
                        + (options.deepSkyLabels() ? "on" : "off") + ": ";
                var mapping = new juranometria.project.ViewportMapping(
                        scene.viewport());
                var projection = juranometria.project.Projections
                        .forViewport(scene.viewport());
                var detail = new RegionalDetailPolicy(scene,
                        mapping.pixelsPerPlaneUnit());
                for (ChartRenderer.StarLabelPlacement placement
                        : RENDERER.starLabelPlacements(metrics, scene,
                                options, detail, projection, mapping)) {
                    assertTrue(asked.contains("star:"
                                    + placement.star().id()),
                            where + "the page draws "
                                    + placement.text()
                                    + " and the seam is asked for it");
                }
                for (var dso : RENDERER.labelledDeepSky(scene, options)) {
                    if (projection.project(dso.position()).isEmpty()) {
                        continue;
                    }
                    assertTrue(asked.contains("deep sky:" + dso.id()),
                            where + "the page draws "
                                    + ChartRenderer.labelTextFor(dso)
                                    + " and the seam is asked for it");
                }
                Map<String, ChartRenderer.FigureInk> ink =
                        RENDERER.figureInk(scene, options);
                for (Map.Entry<String, String> name
                        : scene.geography().latinNames().entrySet()) {
                    ChartRenderer.FigureInk figure = ink.get(name.getKey());
                    if (figure == null || figure.nameAnchor() == null) {
                        continue;
                    }
                    assertTrue(asked.contains("name:" + name.getKey()),
                            where + "the page draws " + name.getValue()
                                    + " and the seam is asked for it");
                }
            }
        }
    }

    /** The first deep-sky object the page draws a symbol for. */
    private static String searched(ChartScene scene) {
        for (var dso : RENDERER.labelledDeepSky(scene,
                ChartOptions.DEFAULTS)) {
            return dso.id();
        }
        return null;
    }

    private static ChartOptions withoutDeepSkyLabels(ChartOptions from) {
        return new ChartOptions(from.deepSkyObjects(), false,
                from.constellationFigures(),
                from.constellationBoundaries(),
                from.constellationNames(), from.starNames(),
                from.bayerLetters(), from.flamsteedNumbers(),
                from.equatorialGrid(), from.titleBlock(),
                from.magnitudeKey(), from.galaxies(), from.openClusters(),
                from.globularClusters(), from.nebulae(),
                from.planetaryNebulae(), from.palette());
    }

    @Test
    void thePagesFurnitureIsAmongTheThingsTextMustAvoid() {
        // The title block is opaque and drawn last, so a label under
        // it is a label nobody reads. It has to reach the seam as an
        // obstacle, and be refused as furniture rather than as
        // something else.
        ChartScene scene = page(83.0, 0.0, 8.0, 900, 700);
        FontMetrics metrics = metrics();
        List<LabelPlacement.Obstacle> ink = LabelGeometry.obstaclesOn(
                RENDERER, metrics, scene, ChartOptions.DEFAULTS);
        LabelPlacement.Obstacle block = null;
        for (LabelPlacement.Obstacle one : ink) {
            if (one.kind() == LabelPlacement.Refusal.FURNITURE
                    && "title block".equals(one.id())) {
                block = one;
            }
        }
        assertTrue(block != null,
                "the page's title block is among its obstacles");

        Rectangle2D over = block.ink().getBounds2D();
        Rectangle2D onIt = new Rectangle2D.Double(over.getCenterX() - 10,
                over.getCenterY() - 5, 20, 10);
        // Against the block alone, so the answer names the block
        // rather than whichever star's disc happens to sit under it -
        // several things refuse a box in the corner of a dense page,
        // and the seam reports the first true one.
        LabelPlacement.Placement placed =
                new LabelPlacement(900, 700, List.of(block)).place(
                        new LabelPlacement.Request(
                                LabelPlacement.Family.STAR, "a", "a",
                                over.getCenterX(), over.getCenterY(),
                                List.of(onIt,
                                        new Rectangle2D.Double(400, 300,
                                                20, 10)),
                                null, null, false, 3.0));
        assertEquals(1, placed.candidate(),
                "and text is not written on it");
        assertEquals(LabelPlacement.Refusal.FURNITURE,
                placed.refusals().get(0).kind());
        assertEquals("title block", placed.refusals().get(0).by());
    }

    @Test
    void thePageIsPlacedWithoutAskingEverythingAboutEverything() {
        // The gate's budget is 60 ms at the densest 120-degree page,
        // and a wall-clock assertion is the wrong way to hold it: this
        // machine placed the page in 45 ms and the runner that builds
        // it took 154, so such a test states which computer it is
        // running on and nothing about the atlas.
        //
        // What is the same everywhere is the work. Every candidate box
        // asks the obstacles near it rather than all of them, and this
        // holds that: a page of two hundred labels against a thousand
        // pieces of ink makes far fewer comparisons than the product
        // of the two, which is what an unindexed pass would make.
        ChartScene scene = page(266.0, -28.0, 120.0, 900, 700);
        FontMetrics metrics = metrics();
        List<LabelPlacement.Request> asked = new java.util.ArrayList<>();
        asked.addAll(LabelGeometry.starLabels(RENDERER, metrics, scene,
                ChartOptions.DEFAULTS));
        asked.addAll(LabelGeometry.deepSkyLabels(RENDERER, metrics, scene,
                ChartOptions.DEFAULTS));
        asked.addAll(LabelGeometry.constellationNames(RENDERER, metrics,
                scene, ChartOptions.DEFAULTS));
        List<LabelPlacement.Obstacle> ink = LabelGeometry.obstaclesOn(
                RENDERER, metrics, scene, ChartOptions.DEFAULTS);

        LabelPlacement placement = new LabelPlacement(900, 700, ink);
        placement.placeAll(asked);
        long everything = (long) asked.size() * ink.size();
        assertTrue(placement.comparisons() < everything / 10,
                asked.size() + " labels against " + ink.size()
                        + " pieces of ink: " + placement.comparisons()
                        + " comparisons, where asking each of one about"
                        + " each of the other would be " + everything);
    }

    @Test
    void theIndexDecidesWhatIsAskedAndNeverWhatIsAnswered() {
        // An index that changed an answer would be a placement policy
        // of its own. Every request on a real page is placed twice -
        // once against a page-sized cell, where the index can exclude
        // nothing, and once against the index proper - and the two
        // must agree on every box.
        ChartScene scene = page(83.0, 0.0, 90.0, 900, 700);
        FontMetrics metrics = metrics();
        List<LabelPlacement.Request> asked = new java.util.ArrayList<>();
        asked.addAll(LabelGeometry.starLabels(RENDERER, metrics, scene,
                ChartOptions.DEFAULTS));
        asked.addAll(LabelGeometry.deepSkyLabels(RENDERER, metrics, scene,
                ChartOptions.DEFAULTS));
        asked.addAll(LabelGeometry.constellationNames(RENDERER, metrics,
                scene, ChartOptions.DEFAULTS));
        List<LabelPlacement.Obstacle> ink = LabelGeometry.obstaclesOn(
                RENDERER, metrics, scene, ChartOptions.DEFAULTS);

        List<LabelPlacement.Placement> indexed =
                new LabelPlacement(900, 700, ink).placeAll(asked);
        List<LabelPlacement.Placement> everywhere =
                new LabelPlacement(900, 700, ink, 100_000.0)
                        .placeAll(asked);
        assertEquals(everywhere.size(), indexed.size());
        for (int at = 0; at < indexed.size(); at++) {
            assertEquals(everywhere.get(at).request().id(),
                    indexed.get(at).request().id(),
                    "the same requests in the same order");
            assertEquals(everywhere.get(at).at(), indexed.get(at).at(),
                    indexed.get(at).request().id()
                            + " is placed in the same box either way");
            assertEquals(everywhere.get(at).candidate(),
                    indexed.get(at).candidate());
            assertEquals(everywhere.get(at).underDuress(),
                    indexed.get(at).underDuress());
        }
    }

    private static List<double[]> pointsOf(Shape shape) {
        List<double[]> points = new java.util.ArrayList<>();
        double[] point = new double[6];
        for (java.awt.geom.PathIterator along =
                shape.getPathIterator(null, 0.5); !along.isDone();
                along.next()) {
            int kind = along.currentSegment(point);
            if (kind == java.awt.geom.PathIterator.SEG_MOVETO
                    || kind == java.awt.geom.PathIterator.SEG_LINETO) {
                points.add(new double[] {point[0], point[1]});
            }
        }
        return points;
    }

    private static double distanceFromLine(double[] point, double[] from,
                                           double[] to) {
        double length = Math.hypot(to[0] - from[0], to[1] - from[1]);
        if (length < 1.0e-9) {
            return Math.hypot(point[0] - from[0], point[1] - from[1]);
        }
        return Math.abs((to[0] - from[0]) * (from[1] - point[1])
                - (from[0] - point[0]) * (to[1] - from[1])) / length;
    }
}
