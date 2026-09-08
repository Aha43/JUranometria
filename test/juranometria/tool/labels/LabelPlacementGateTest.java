package juranometria.tool.labels;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.RegionalDetailPolicy;
import juranometria.render.StarLabelPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The label-placement gate's own instruments (Sprint 31, issue #310).
 *
 * <p>The study's conclusions are only worth what its oracle is worth,
 * and this sprint inherits a lesson about that from the last one: an
 * oracle that cannot tell two causes apart will credit the wrong one.
 * These hold the four properties the whole document rests on - that a
 * window is the page, that withholding a star does not take its name
 * with it, that a box is not ink, and that the named fixture is three
 * separable pieces - and the two conclusions the gate turns into
 * contracts for #313.
 */
class LabelPlacementGateTest {

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    private static Page fixturePage() {
        return new Page("sagittarius-120",
                StudyPages.assemble(new ChartViewState(SAGITTARIUS, 120.0,
                        ChartViewState.defaultMagnitudeFor(120.0)),
                        StudyPages.SCREEN_WIDE, StudyPages.SCREEN_HIGH),
                ChartOptions.DEFAULTS, List.of());
    }

    @Test
    void aWindowOfThePageIsThePage() {
        // Every pair in the study is measured through a window, which
        // is only sound if the renderer draws a window's pixels the
        // way it draws the whole page's. It does - a translated
        // context onto a small image - but the study would be worth
        // nothing if it did not, so it is checked rather than assumed.
        Page page = fixturePage();
        BufferedImage whole = page.paint();
        java.awt.Rectangle frame = new java.awt.Rectangle(311, 306, 97, 62);
        BufferedImage window = page.paint(frame);
        assertEquals(frame.width, window.getWidth(),
                "the window is the size it was asked for");
        int differing = 0;
        for (int y = 0; y < frame.height; y++) {
            for (int x = 0; x < frame.width; x++) {
                if (window.getRGB(x, y)
                        != whole.getRGB(frame.x + x, frame.y + y)) {
                    differing++;
                }
            }
        }
        assertEquals(0, differing, "a window is the same pixels as the"
                + " page it is cut from");
    }

    @Test
    void withholdingAStarWouldTakeItsNameWithIt() {
        // The fault that the paint-order check caught, held here so it
        // cannot come back: removing a star from the scene removes the
        // star's own label too, so a mark's ink has to have that label
        // subtracted back out of it. Without the subtraction, "one
        // star's name across another star's mark" and "across another
        // star's name" are the same reading.
        Page page = fixturePage();
        Attribution attribution = new Attribution(page);
        Participant label = new Participant(Participant.Family.STAR_LABEL,
                StudyPages.NUNKI, "Nunki σ");
        Participant everything = new Participant(
                Participant.Family.STAR_DISC, StudyPages.NUNKI, "the star");
        Participant discAlone = everything.alsoTaking(label);

        int labelInk = attribution.inkOf(label).pixels();
        int both = attribution.inkOf(everything).pixels();
        int disc = attribution.inkOf(discAlone).pixels();
        assertTrue(labelInk > 50, "Nunki is labelled on this page: "
                + labelInk + " px");
        assertTrue(both > labelInk + 20, "removing the star takes its"
                + " name and its mark: " + both + " px against "
                + labelInk + " px of name alone");
        assertTrue(disc <= both - labelInk + 10 && disc > 5,
                "and the subtraction leaves the mark: " + disc + " px");
        assertFalse(attribution.inkOf(discAlone)
                .meeting(attribution.inkOf(label)).any(),
                "with nothing of the label left in it");
    }

    @Test
    void aBoxIsNotInkAndTheCensusKnowsIt() {
        // The nomination is generous and the attribution is not. If
        // every nominated pair became a collision the study would be
        // reporting rectangles, which is the thing the issue rules
        // out; this holds that the two numbers differ.
        Page page = new Page("orion-42",
                StudyPages.assemble(new ChartViewState(
                        new SkyPosition(83.0, 0.0), 42.0, 8.0),
                        StudyPages.SCREEN_WIDE, StudyPages.SCREEN_HIGH),
                ChartOptions.DEFAULTS, List.of());
        Census census = new Census(page);
        assertTrue(census.nominated() > census.collisions().size(),
                "nomination is looser than attribution: "
                        + census.nominated() + " pairs painted, "
                        + census.collisions().size() + " collisions");
        assertTrue(census.collisions().size() > 10,
                "and the page really does have collisions: "
                        + census.collisions().size());
    }

    @Test
    void theFixtureIsThreeSeparablePieces() {
        // The named regression fixture, in the terms the owner set:
        // Nunki's glyphs, Nunki's own disc beside which they are
        // anchored, and Namalsadirah's disc which they must not cross.
        Page page = fixturePage();
        Attribution attribution = new Attribution(page);
        Participant nunkiLabel = new Participant(
                Participant.Family.STAR_LABEL, StudyPages.NUNKI, "Nunki σ");
        Participant nunkiDisc = new Participant(
                Participant.Family.STAR_DISC, StudyPages.NUNKI, "its mark")
                .alsoTaking(nunkiLabel);
        Participant otherLabel = new Participant(
                Participant.Family.STAR_LABEL, StudyPages.NAMALSADIRAH, "φ");
        Participant otherDisc = new Participant(
                Participant.Family.STAR_DISC, StudyPages.NAMALSADIRAH,
                "Namalsadirah's mark").alsoTaking(otherLabel);

        assertFalse(attribution.meeting(nunkiLabel, nunkiDisc).collides(),
                "a star's name is anchored beside its own mark, not"
                        + " across it");
        Attribution.Meeting defect =
                attribution.meeting(nunkiLabel, otherDisc);
        assertTrue(defect.collides(), "and across an unrelated one: the"
                + " defect the owner reported");
        // As a share of the mark rather than as a pixel count. The
        // count is font rendering, and font rendering is a fact about
        // a machine: this shares 68 px on the author's macOS and 27 on
        // the Linux runner, and a threshold tuned to either would be a
        // test about the toolkit. What is true on both is that a
        // substantial part of the mark is under the name.
        int stillVisible = attribution.inkOf(otherDisc).pixels();
        int covered = defect.where().pixels();
        double share = (double) covered / (covered + stillVisible);
        assertTrue(share > 0.25, "not by a pixel or two: " + covered
                + " px of the mark covered against " + stillVisible
                + " left visible, " + Math.round(share * 100) + "%");
        assertEquals(Participant.Family.STAR_LABEL,
                defect.over().family(),
                "with the name on top of the mark");
        assertEquals(StudyPages.NAMALSADIRAH, defect.under().id(),
                "and the mark being Namalsadirah's, by catalogue id"
                        + " rather than by name");
    }

    @Test
    void theAtlasRefusesNamalsadirahTheLetterItQualifiesFor() {
        // The fourth participant, and the reason the fixture is worse
        // than it looks: the star whose mark is covered also loses its
        // own designation, to the very label that covers it. A
        // contract for #314 - the letter must come back.
        ChartScene scene = fixturePage().scene();
        Star namalsadirah = null;
        for (Star star : scene.stars()) {
            if (star.id().equals(StudyPages.NAMALSADIRAH)) {
                namalsadirah = star;
            }
        }
        assertTrue(namalsadirah != null, "the pack records the star");
        StarLabelPolicy policy = new StarLabelPolicy(120.0);
        assertEquals("φ", policy.qualifying(namalsadirah)
                        .text(true, true, true),
                "which qualifies for its Bayer letter at this field");

        var mapping = new juranometria.project.ViewportMapping(
                scene.viewport());
        var projection = juranometria.project.Projections.forViewport(
                scene.viewport());
        var metrics = Census.Metrics.forFont(ChartRenderer.labelFont());
        boolean drawn = false;
        boolean nunkiDrawn = false;
        for (ChartRenderer.StarLabelPlacement placement
                : Page.renderer().starLabelPlacements(metrics, scene,
                        ChartOptions.DEFAULTS,
                        new RegionalDetailPolicy(scene,
                                mapping.pixelsPerPlaneUnit()),
                        projection, mapping)) {
            drawn |= placement.star().id().equals(StudyPages.NAMALSADIRAH);
            nunkiDrawn |= placement.star().id().equals(StudyPages.NUNKI);
        }
        assertTrue(nunkiDrawn, "the brighter star is labelled");
        assertFalse(drawn, "and the fainter one's letter is refused,"
                + " because the label that will be drawn across its"
                + " mark got there first");
    }

    @Test
    void theChosenPolicyLosesNothingThePageAlreadyDraws() {
        // The decision's load-bearing claim: the least-bad fallback
        // repairs the observed defects without dropping a single label
        // the released page draws. Measured on the fixture's own page,
        // from the two painted pages rather than from the policy's
        // account of itself.
        Page page = fixturePage();
        Census today = new Census(page);
        Greedy greedy = new Greedy(page, Greedy.LEAST_BAD);
        Census after = new Census(greedy.pageWithPlacements());

        java.util.Set<String> was = new java.util.LinkedHashSet<>();
        for (Participant drawn : today.text()) {
            was.add(drawn.family() + ":" + drawn.id());
        }
        java.util.List<String> lost = new java.util.ArrayList<>();
        for (String one : was) {
            boolean kept = false;
            for (Participant drawn : after.text()) {
                kept |= (drawn.family() + ":" + drawn.id()).equals(one);
            }
            if (!kept) {
                lost.add(one);
            }
        }
        assertEquals(List.of(), lost,
                "the candidate page draws everything the released page"
                        + " draws");
        assertTrue(defects(after) * 4 < defects(today),
                "and far fewer of the owner's two defects: "
                        + defects(today) + " became " + defects(after));
        assertTrue(greedy.candidatesTried() < 4000,
                "and a finite ordered list, walked once per label"
                        + " rather than searched: "
                        + greedy.candidatesTried()
                        + " candidate positions examined");
    }

    @Test
    void theStudyPaintsTheProductionPageAndNothingElse() {
        // The gate must not change what the atlas draws, and its own
        // page type is the first place that could go wrong: a Page
        // with no candidate text and no overlays must be the released
        // render, to the pixel.
        Page page = fixturePage();
        BufferedImage mine = page.paint();
        BufferedImage theirs = new BufferedImage(page.wide(), page.high(),
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = theirs.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), ChartOptions.DEFAULTS);
        } finally {
            g.dispose();
        }
        int differing = 0;
        for (int y = 0; y < page.high(); y++) {
            for (int x = 0; x < page.wide(); x++) {
                if (mine.getRGB(x, y) != theirs.getRGB(x, y)) {
                    differing++;
                }
            }
        }
        assertEquals(0, differing,
                "the study's page is the renderer's page");
    }

    @Test
    void aConstellationOwnsTheHullOfItsOwnVisibleInk() {
        // The region a name may move in is computed from geometry for
        // speed, so it is checked against ink: every pixel the
        // renderer actually inks for a constellation's figure must
        // fall inside that constellation's hull. Geometry that has
        // drifted from what is drawn would let a name sit where its
        // figure is not.
        Page page = fixturePage();
        FigureRegion owned = FigureRegion.of(page);
        Attribution attribution = new Attribution(page);
        assertTrue(owned.constellations().size() > 10,
                "the page carries figures to check: "
                        + owned.constellations().size());
        // A third thing withholding a figure takes with it, after its
        // own name: the stars #307 keeps *because* that figure is
        // drawn to them. Remove Hydra's segments and the faint stars
        // its lines end on go too, so their discs land in the ink
        // attributed to the figure - a seven-pixel blob forty pixels
        // off the end of a hull that was perfectly correct. Star discs
        // are filled and their outlines are published, so they can be
        // excluded exactly.
        java.awt.geom.Area discs = new java.awt.geom.Area();
        for (ChartRenderer.DrawnMark mark
                : Page.renderer().drawnMarks(page.scene(),
                        ChartOptions.DEFAULTS)) {
            if (mark.star() != null) {
                discs.add(new java.awt.geom.Area(
                        new java.awt.BasicStroke(3.0f)
                                .createStrokedShape(mark.outline())));
                discs.add(new java.awt.geom.Area(mark.outline()));
            }
        }
        int checked = 0;
        int strayed = 0;
        int total = 0;
        for (String constellation : new java.util.TreeSet<>(
                owned.constellations())) {
            // Withholding a constellation's figure takes its NAME
            // with it - the renderer anchors a name on visible figure
            // ink, and a figure with no ink is not named - so the
            // name's glyphs have to be subtracted back out. The same
            // trap as the star and its label, sprung on the same
            // study, and found by this test failing on eight pixels
            // of Aquarius that were the letters of AQUARIUS.
            Ink ink = attribution.inkOf(new Participant(
                    Participant.Family.FIGURE_LINE, constellation,
                    "its figure").alsoTaking(new Participant(
                            Participant.Family.CONSTELLATION_NAME,
                            constellation, "its name")));
            if (!ink.any()) {
                continue;
            }
            java.awt.Shape hull = owned.regionOf(constellation);
            // Two bounds, both over the page rather than tuned per
            // constellation. The hull is taken over the ends of the
            // pieces the renderer's subdivision cuts, re-derived here,
            // and this study's re-derivation is not bit-identical to
            // the renderer's on a curved 120-degree line: Aquarius
            // puts eight pixels outside the hull grown by two, Ara one
            // outside it grown by four. What must hold is that the
            // region contains the figure - nearly every pixel close in
            // and no pixel far out - and that is asked of all of them
            // together. #313 will have the renderer's own geometry and
            // will not need the slack.
            java.awt.geom.Area near = grownBy(hull, 4.0);
            near.add(discs);
            java.awt.geom.Area far = grownBy(hull, 12.0);
            far.add(discs);
            strayed += ink.strayingFrom(near);
            assertEquals(0, ink.strayingFrom(far),
                    constellation + ": no pixel of its figure's ink is"
                            + " far outside the region it owns");
            total += ink.pixels();
            checked++;
        }
        assertTrue(checked > 10, "on a useful number of figures: "
                + checked);
        assertTrue(strayed < total / 200, "and all but a handful of the"
                + " page's " + total + " pixels of figure ink are"
                + " within four pixels of their own region: " + strayed
                + " are not");
    }

    private static java.awt.geom.Area grownBy(java.awt.Shape hull,
                                              double margin) {
        java.awt.geom.Area grown = new java.awt.geom.Area(
                new java.awt.BasicStroke((float) (2.0 * margin))
                        .createStrokedShape(hull));
        grown.add(new java.awt.geom.Area(hull));
        return grown;
    }

    @Test
    void aNameIsAnchoredWhereTheAtlasAnchorsIt() {
        // The policy's centroid must be the renderer's centroid, or
        // every constellation number in the study is about a different
        // page. Measured against the name the released page actually
        // draws, found by withholding it.
        Page page = fixturePage();
        FigureRegion owned = FigureRegion.of(page);
        Attribution attribution = new Attribution(page);
        int checked = 0;
        for (java.util.Map.Entry<String, String> name
                : page.scene().geography().latinNames().entrySet()) {
            Ink ink = attribution.inkOf(new Participant(
                    Participant.Family.CONSTELLATION_NAME, name.getKey(),
                    name.getValue()));
            java.awt.geom.Rectangle2D drawn = ink.bounds();
            double[] centroid = owned.centroidOf(name.getKey());
            if (drawn == null || centroid == null
                    || drawn.getMinX() <= 1 || drawn.getMaxX() >= page.wide() - 1) {
                // A name clipped by the page edge has ink whose
                // centre is not its own centre; the page's own rule
                // lets it clip, and this is not the test for that.
                continue;
            }
            checked++;
            // Eight pixels, not one, and the slack is the subject of
            // this study rather than sloppiness: a name is drawn
            // before every mark on the page, so its VISIBLE ink is
            // what the discs drawn over it have left, and its centre
            // shifts with them. Pavo's ink sits five pixels right of
            // its anchor for that reason. What this catches is drift
            // of a different order - an endpoint-based anchor misses
            // by tens of pixels, or by the whole page.
            assertTrue(Math.abs(drawn.getCenterX() - centroid[0]) < 8.0,
                    name.getKey() + ": the policy anchors the name where"
                            + " the atlas draws it, x " + centroid[0]
                            + " against ink centred at "
                            + drawn.getCenterX());
            // And in y, which the first version of this test did not
            // ask at all: an anchor can be right across the page and
            // wrong down it, and half a check is worse than none
            // because it reads as a whole one. The renderer draws the
            // string with its baseline AT the centroid, and these
            // names are capitals with no descender, so the bottom of
            // the ink is the baseline.
            assertTrue(Math.abs(drawn.getMaxY() - centroid[1]) < 4.0,
                    name.getKey() + ": and down the page, baseline "
                            + centroid[1] + " against ink ending at "
                            + drawn.getMaxY());
        }
        assertTrue(checked > 8, "on a useful number of names: " + checked);
    }

    @Test
    void noNameLeavesItsOwnFigureUnderTheChosenPolicy() {
        // The decision's rule, held on the policy the decision is
        // taken from - which an earlier draft did not do, stating the
        // rule in prose while measuring a policy without it.
        for (String slug : List.of("sagittarius-120", "crux-90")) {
            Page page = null;
            for (StudyPages.Look look : StudyPages.corpus()) {
                if (look.page().slug().equals(slug)) {
                    page = look.page();
                }
            }
            assertTrue(page != null, "the corpus carries " + slug);
            Greedy chosen = new Greedy(page, Greedy.LEAST_BAD);
            assertEquals(0, chosen.namesOffTheirOwnFigure(),
                    slug + ": every name overlaps the region its own"
                            + " figure owns");
            Greedy unowned = new Greedy(page, Greedy.LEAST_BAD_UNOWNED);
            assertTrue(unowned.namesOffTheirOwnFigure() > 0,
                    slug + ": and the rule is doing something - without"
                            + " it, " + unowned.namesOffTheirOwnFigure()
                            + " names leave their own figure");
        }
    }

    @Test
    void aSymbolsDrawnShapesAreWhereItsInkIs() {
        // The division of labour the placement contract needs: shapes
        // decide placement, because the same page must come out the
        // same way on every machine, and pixels judge the shapes,
        // because only the render knows what was drawn. An earlier
        // round had it the other way round and let rasterised ink
        // decide what a label could sit on.
        int judged = 0;
        int strayed = 0;
        int total = 0;
        for (double[] look : new double[][] {{130.1, 19.67, 3.0},
                {10.684708, 41.268750, 8.0}, {83.0, 0.0, 8.0},
                {56.75, 24.12, 3.0}}) {
            Page page = new Page("judged",
                    StudyPages.assemble(new ChartViewState(
                            new SkyPosition(look[0], look[1]), look[2], 8.0),
                            StudyPages.SCREEN_WIDE, StudyPages.SCREEN_HIGH),
                    ChartOptions.DEFAULTS, List.of());
            Attribution attribution = new Attribution(page);
            for (ChartRenderer.DrawnMark mark
                    : Page.renderer().drawnMarks(page.scene(),
                            ChartOptions.DEFAULTS)) {
                if (mark.deepSky() == null || mark.reach() < 5.0) {
                    continue;
                }
                java.awt.geom.Area drawn = SymbolInk.of(mark);
                if (drawn.isEmpty()) {
                    continue;
                }
                Ink inked = attribution.inkOf(new Participant(
                        Participant.Family.DEEP_SKY_SYMBOL,
                        mark.deepSky().id(), "its symbol"));
                if (inked.pixels() < 20) {
                    // Too little left visible to judge a shape by:
                    // another symbol or a label is drawn over most of
                    // it, and what is left is somebody else's problem.
                    continue;
                }
                judged++;
                total += inked.pixels();
                strayed += inked.strayingFrom(grownBy(drawn, 2.0));
            }
        }
        assertTrue(judged > 8, "the pages draw symbols to judge: "
                + judged);
        assertTrue(strayed < total / 20, "the shapes cover the ink: "
                + strayed + " of " + total + " inked pixels lie more"
                + " than two pixels outside the shapes drawn for them");
    }

    @Test
    void anOpenClusterReservesItsRingAndNotItsInterior() {
        // And the other half: the shapes must not claim what the
        // renderer leaves blank. An open cluster is a dotted ring
        // around nothing, and its published silhouette - what a reader
        // aims at - says otherwise.
        Page page = new Page("praesepe-03",
                StudyPages.assemble(new ChartViewState(
                        new SkyPosition(130.1, 19.67), 3.0, 8.0),
                        StudyPages.SCREEN_WIDE, StudyPages.SCREEN_HIGH),
                ChartOptions.DEFAULTS, List.of());
        ChartRenderer.DrawnMark widest = null;
        for (ChartRenderer.DrawnMark mark
                : Page.renderer().drawnMarks(page.scene(),
                        ChartOptions.DEFAULTS)) {
            if (mark.deepSky() != null
                    && mark.deepSky().type() == juranometria.chart.DsoType
                            .OPEN_CLUSTER
                    && (widest == null || mark.reach() > widest.reach())) {
                widest = mark;
            }
        }
        assertTrue(widest != null && widest.reach() > 20.0,
                "the page draws an open cluster with room inside it");

        java.awt.geom.Rectangle2D middle =
                new java.awt.geom.Rectangle2D.Double(
                        widest.centre().x() - 4.0,
                        widest.centre().y() - 4.0, 8.0, 8.0);
        assertTrue(widest.outline().intersects(middle),
                "the silhouette claims the middle of the ring");
        assertFalse(SymbolInk.of(widest).intersects(middle),
                "the shapes drawn for it do not");
        Attribution attribution = new Attribution(page);
        Ink inked = attribution.inkOf(new Participant(
                Participant.Family.DEEP_SKY_SYMBOL, widest.deepSky().id(),
                "its ring"));
        assertFalse(inked.within(middle).any(),
                "and neither does the renderer: "
                        + inked.within(middle).pixels() + " px");
    }

    private static int defects(Census census) {
        int found = 0;
        for (Attribution.Meeting meeting : census.collisions()) {
            Participant.Family over = meeting.over().family();
            Participant.Family under = meeting.under().family();
            if (over == Participant.Family.STAR_LABEL
                    && under == Participant.Family.STAR_DISC
                    || over == Participant.Family.STAR_LABEL
                            && under == Participant.Family.CONSTELLATION_NAME
                    || over == Participant.Family.STAR_DISC
                            && under == Participant.Family.CONSTELLATION_NAME) {
                found++;
            }
        }
        return found;
    }
}
