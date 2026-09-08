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
        for (String constellation : new java.util.TreeSet<>(
                owned.constellations())) {
            Ink ink = attribution.inkOf(new Participant(
                    Participant.Family.FIGURE_LINE, constellation,
                    "its figure"));
            java.awt.geom.Rectangle2D drawn = ink.bounds();
            if (drawn == null) {
                continue;
            }
            java.awt.Shape hull = owned.regionOf(constellation);
            // The hull is over the pieces' midpoints and the ink is
            // the stroked line through them, so ink stands a stroke's
            // width outside a hull that is correct; the hull grown by
            // two pixels is the claim being made.
            java.awt.geom.Area grown = new java.awt.geom.Area(
                    new java.awt.BasicStroke(4.0f).createStrokedShape(hull));
            grown.add(new java.awt.geom.Area(hull));
            assertTrue(grown.contains(drawn.getCenterX(),
                            drawn.getCenterY()),
                    constellation + ": its own figure's ink is centred"
                            + " inside the region it owns");
        }
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
