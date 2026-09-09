package juranometria.tool.labels;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

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
        //
        // Since #314 the census holds the page's placement still while
        // it withholds ink, which answers the same trap at its source
        // - the label is drawn from a decision the withholding did not
        // change - and both halves are checked here, because the
        // subtraction is what guards a page whose decision is not
        // held.
        Page page = fixturePage();
        Participant label = new Participant(Participant.Family.STAR_LABEL,
                StudyPages.NUNKI, "Nunki σ");
        Participant everything = new Participant(
                Participant.Family.STAR_DISC, StudyPages.NUNKI, "the star");
        Participant discAlone = everything.alsoTaking(label);

        Attribution loose = new Attribution(page, false);
        int labelInk = loose.inkOf(label).pixels();
        int both = loose.inkOf(everything).pixels();
        int disc = loose.inkOf(discAlone).pixels();
        assertTrue(labelInk > 50, "Nunki is labelled on this page: "
                + labelInk + " px");
        assertTrue(both > labelInk + 20, "with the page free to replace"
                + " it, removing the star takes its name and its mark: "
                + both + " px against " + labelInk + " px of name alone");
        assertTrue(disc <= both - labelInk + 10 && disc > 5,
                "and the subtraction leaves the mark: " + disc + " px");
        assertFalse(loose.inkOf(discAlone).meeting(loose.inkOf(label)).any(),
                "with nothing of the label left in it");

        Attribution held = new Attribution(page);
        int heldBoth = held.inkOf(everything).pixels();
        assertTrue(heldBoth < labelInk, "and with the decision held,"
                + " removing the star takes the mark alone: " + heldBoth
                + " px against " + labelInk + " px of name");
        assertTrue(Math.abs(held.inkOf(discAlone).pixels() - heldBoth) <= 2,
                "which is what the subtraction was for");
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
        // Each is measurable on its own, which is what makes the
        // repair below a measurement rather than a claim.
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

        assertTrue(attribution.inkOf(nunkiLabel).pixels() > 50,
                "the name is on the page: "
                        + attribution.inkOf(nunkiLabel).pixels() + " px");
        assertTrue(attribution.inkOf(nunkiDisc).pixels() > 5,
                "so is its own mark");
        assertTrue(attribution.inkOf(otherDisc).pixels() > 5,
                "so is the mark it was crossing");

        assertFalse(attribution.meeting(nunkiLabel, nunkiDisc).collides(),
                "a star's name is anchored beside its own mark, not"
                        + " across it");
        // The defect the owner reported, measured the way it was
        // reported: 68 px of Namalsadirah's disc under Nunki's name on
        // the author's machine, 27 on the Linux runner, a quarter of
        // the mark either way. Not one pixel now.
        Attribution.Meeting repaired =
                attribution.meeting(nunkiLabel, otherDisc);
        assertFalse(repaired.collides(), "and since #314 not across an"
                + " unrelated one either: the defect the owner reported,"
                + " repaired on the page it was reported on - "
                + repaired.where().pixels() + " px shared");
    }

    @Test
    void namalsadirahKeepsTheLetterItQualifiesFor() {
        // The fourth participant, and the reason the fixture was worse
        // than it looked: the star whose mark was covered also lost
        // its own designation, to the very label that covered it. The
        // gate made the letter coming back a contract for #314, and
        // this is where it is paid.
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

        java.awt.geom.Rectangle2D letter = null;
        java.awt.geom.Rectangle2D nunki = null;
        for (ChartRenderer.StarLabelPlacement placement
                : Page.renderer().starLabelPlacements(
                        ChartRenderer.TextMetrics.offscreen(), scene,
                        ChartOptions.DEFAULTS)) {
            if (placement.star().id().equals(StudyPages.NAMALSADIRAH)) {
                letter = placement.box();
            }
            if (placement.star().id().equals(StudyPages.NUNKI)) {
                nunki = placement.box();
            }
        }
        assertTrue(nunki != null, "the brighter star is labelled");
        assertTrue(letter != null, "and the fainter one has its letter"
                + " back: the atlas no longer declines to name a star"
                + " in order to protect a name it then draws across"
                + " that star's mark");
        assertFalse(nunki.intersects(letter),
                "and the two names do not share a pixel of box");
    }

    @Test
    void theChosenPolicyLosesNothingThePageAlreadyDraws() {
        // The decision's load-bearing claim, now asked of the atlas
        // itself: production places this page and the study's own
        // greedy pass places it separately, from the same document and
        // sharing no code, and the two are compared on the painted
        // page rather than on either one's account of itself.
        Page page = fixturePage();
        Census production = new Census(page);
        Greedy greedy = new Greedy(page, Greedy.LEAST_BAD);
        Census candidate = new Census(greedy.pageWithPlacements());

        java.util.Set<String> theirs = new java.util.LinkedHashSet<>();
        for (Participant drawn : candidate.text()) {
            theirs.add(drawn.family() + ":" + drawn.id());
        }
        java.util.Set<String> ours = new java.util.LinkedHashSet<>();
        for (Participant drawn : production.text()) {
            ours.add(drawn.family() + ":" + drawn.id());
        }
        // What production draws and the study does not is the study's
        // business; what production does NOT draw has to be the
        // decision's doing, and the seam says which of the two it is.
        // Either the label was omitted - every candidate off the paper
        // or off its own figure - or the fallback spent the furniture,
        // which the gate lets it spend, and the title block is opaque
        // and drawn last, so the label is behind it. The study's own
        // pass writes its text over the furniture and so keeps a label
        // production covers.
        java.awt.geom.Rectangle2D block = ChartRenderer.titleBlockBounds(
                Census.Metrics.forFont(ChartRenderer.labelFont()),
                page.scene());
        java.util.Set<String> explained = new java.util.LinkedHashSet<>();
        for (var placement : Page.renderer().textPlacements(
                ChartRenderer.TextMetrics.offscreen(), page.scene(),
                page.options())) {
            if (placement.omitted()
                    || block != null && block.contains(placement.at())) {
                explained.add(familyOf(placement.request()) + ":"
                        + placement.request().id());
            }
        }
        java.util.List<String> unexplained = new java.util.ArrayList<>();
        for (String one : theirs) {
            if (!ours.contains(one) && !explained.contains(one)) {
                unexplained.add(one);
            }
        }
        assertEquals(List.of(), unexplained,
                "every piece of text the atlas does not draw is one the"
                        + " decision omitted or one the fallback put"
                        + " behind the title block");
        assertTrue(defects(production) <= defects(candidate),
                "and the atlas has no more of the owner's two defects"
                        + " than the study's own pass: "
                        + defects(production) + " against "
                        + defects(candidate));
    }

    private static String familyOf(
            juranometria.render.LabelPlacement.Request request) {
        return switch (request.family()) {
            case CONSTELLATION -> Participant.Family.CONSTELLATION_NAME
                    .toString();
            case DEEP_SKY -> Participant.Family.DEEP_SKY_LABEL.toString();
            default -> Participant.Family.STAR_LABEL.toString();
        };
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
    void aNameIsDrawnWhereTheAtlasPlacesIt() {
        // Two claims, and they were one before #314: the anchor the
        // policy computes is the renderer's own centroid, and the name
        // is drawn at the box the page placed it in. Until the
        // families migrated those were the same statement, because a
        // name was always drawn on its anchor. Now a name may move,
        // and each half has to be asked separately - measured against
        // the name the page actually draws, found by withholding it.
        Page page = fixturePage().withTextHeld();
        FigureRegion owned = FigureRegion.of(page);
        Attribution attribution = new Attribution(page);
        java.util.Map<String, java.awt.geom.Rectangle2D> placed =
                new java.util.HashMap<>();
        java.util.Map<String, double[]> anchors = new java.util.HashMap<>();
        for (var placement : Page.renderer().textPlacements(
                ChartRenderer.TextMetrics.offscreen(), page.scene(),
                page.options())) {
            if (placement.request().family()
                    != juranometria.render.LabelPlacement.Family
                            .CONSTELLATION) {
                continue;
            }
            anchors.put(placement.request().id(), new double[] {
                    placement.request().anchorX(),
                    placement.request().anchorY()});
            if (!placement.omitted()) {
                placed.put(placement.request().id(), placement.at());
            }
        }
        int checked = 0;
        for (java.util.Map.Entry<String, String> name
                : page.scene().geography().latinNames().entrySet()) {
            double[] centroid = owned.centroidOf(name.getKey());
            double[] anchor = anchors.get(name.getKey());
            if (centroid == null || anchor == null) {
                continue;
            }
            // The anchor, which the study and the renderer must agree
            // on or every constellation number in the study is about a
            // different page.
            assertTrue(Math.hypot(anchor[0] - centroid[0],
                            anchor[1] - centroid[1]) < 1.0,
                    name.getKey() + ": the policy anchors the name where"
                            + " the atlas anchors it, " + anchor[0] + ","
                            + anchor[1] + " against " + centroid[0] + ","
                            + centroid[1]);
            java.awt.geom.Rectangle2D box = placed.get(name.getKey());
            Ink ink = attribution.inkOf(new Participant(
                    Participant.Family.CONSTELLATION_NAME, name.getKey(),
                    name.getValue()));
            java.awt.geom.Rectangle2D drawn = ink.bounds();
            if (box == null) {
                assertTrue(drawn == null, name.getKey() + ": a name the"
                        + " page omits is not drawn");
                continue;
            }
            if (drawn == null) {
                continue;
            }
            checked++;
            // And the glyphs are inside the box the page placed, which
            // is the claim every obstacle on the page depends on: a
            // box that does not hold its own text reserves the wrong
            // paper. One pixel of slack for the antialiased edge.
            assertTrue(box.getX() - 1 <= drawn.getMinX()
                            && drawn.getMaxX() <= box.getMaxX() + 1
                            && box.getY() - 1 <= drawn.getMinY()
                            && drawn.getMaxY() <= box.getMaxY() + 1,
                    name.getKey() + ": the name is drawn inside the box"
                            + " the page placed it in, " + drawn
                            + " against " + box);
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
    void aSymbolsDrawnShapesAreWhereItsInkIsAndNoMore() {
        // The division of labour the placement contract needs: shapes
        // decide placement, because the same page must come out the
        // same way on every machine, and pixels judge the shapes,
        // because only the render knows what was drawn.
        //
        // Two-sided, and per symbol. A containment check alone - "the
        // ink is inside the shapes" - is satisfied by any shape big
        // enough, and three shapes were: a solid ring where the atlas
        // dots one pixel on and three off, a stroke half again as wide
        // as the atlas's, and a planetary's circle 1.7 times too
        // large. Each refuses candidates over ink that is not there.
        // An aggregate over a page hides all three behind the symbols
        // that are right, so each symbol answers for itself.
        int judged = 0;
        for (double[] look : new double[][] {{130.1, 19.67, 3.0},
                {10.684708, 41.268750, 8.0}, {83.0, 0.0, 8.0},
                {56.75, 24.12, 3.0},
                // The Helix, for the one symbol the other pages do
                // not carry, and large enough to measure: a
                // planetary's circle with its spokes reaching past it.
                {337.41, -20.84, 3.0}}) {
            Page page = new Page("judged",
                    StudyPages.assemble(new ChartViewState(
                            new SkyPosition(look[0], look[1]), look[2], 8.0),
                            StudyPages.SCREEN_WIDE, StudyPages.SCREEN_HIGH),
                    ChartOptions.DEFAULTS, List.of());
            Attribution attribution = new Attribution(page);
            // The ink of the whole deep-sky family, for asking whether
            // a shape's own pixels have any symbol under them at all.
            // Its own is too strict: symbols overlap, and IC 434's box
            // on the Orion page is a third covered by its neighbours,
            // so a third of a perfectly correct shape has no ink of
            // its own beneath it.
            ChartOptions noLabels = Participant.Options.deepSkyLabels(
                    ChartOptions.DEFAULTS, false);
            Ink familyInk = Ink.between(
                    page.withOptions(noLabels).paint(),
                    page.withOptions(Participant.Options.deepSkyObjects(
                            noLabels, false)).paint());
            java.awt.geom.Rectangle2D furniture =
                    Census.Metrics.titleBlockOf(page);
            var mapping = new juranometria.project.ViewportMapping(
                    page.scene().viewport());
            var projection = juranometria.project.Projections.forViewport(
                    page.scene().viewport());
            var metrics = Census.Metrics.forFont(ChartRenderer.labelFont());
            for (ChartRenderer.DrawnMark mark
                    : Page.renderer().drawnMarks(page.scene(),
                            ChartOptions.DEFAULTS)) {
                if (mark.deepSky() == null || mark.reach() < 3.0) {
                    continue;
                }
                java.awt.geom.Area drawn = new java.awt.geom.Area(mark.ink());
                var plane = projection.project(mark.deepSky().position());
                if (drawn.isEmpty() || plane.isEmpty()) {
                    continue;
                }
                // Withholding an object takes its label with it, the
                // same trap as the star and its name; the label's own
                // published box takes it back out.
                Participant symbol = new Participant(
                        Participant.Family.DEEP_SKY_SYMBOL,
                        mark.deepSky().id(), "its symbol")
                        .alsoTaking(new Participant(
                                Participant.Family.DEEP_SKY_LABEL,
                                mark.deepSky().id(), "its label")
                                .within(ChartRenderer.labelBounds(metrics,
                                        mark.deepSky(),
                                        mapping.toPixel(plane.get()),
                                        mapping.pixelsPerPlaneUnit())));
                Ink ink = attribution.inkOf(symbol);
                boolean[] model = rasterised(drawn, page.wide(),
                        page.high());
                int claimedPixels = countOf(model);
                double claimed = claimedPixels;
                if (ink.pixels() < 40 || claimedPixels < 40) {
                    // Too little of it visible or on the paper to
                    // judge a shape by.
                    continue;
                }
                judged++;
                String what = mark.deepSky().id() + " ("
                        + ChartRenderer.symbolFor(mark.deepSky()) + ")";

                // Where: the ink lies in the band the shapes run
                // through - phase-free, because the dash phase is the
                // one thing this reconstruction cannot match.
                assertEquals(0, ink.strayingFrom(
                                grownBy(bandOf(mark), 2.0)),
                        what + ": its ink lies where its shapes are");

                // How much: model and ink rasterised the same way, so
                // antialiasing and the page's own edge fall on both
                // sides. A solid ring claims four times its dotted
                // ink, and is caught here and nowhere else.
                // Two ends, and the upper one is loose on purpose.
                // M32's pale fill sits inside M31's pale fill, and the
                // two are the same grey, so withholding M32 changes
                // only its outline and the measured ink is half what
                // is drawn. What this end is for is a symbol claiming
                // several times its ink - a solid ring claims four -
                // and it still catches that.
                double ratio = claimed / ink.pixels();
                assertTrue(ratio > 0.6 && ratio < 2.0, what + ": it"
                        + " claims about as much as it inks - "
                        + claimedPixels
                        + " against " + ink.pixels() + ", a ratio of "
                        + String.format(Locale.ROOT, "%.2f", ratio));

                // And where it claims it: the shape's own pixels
                // must have ink under them. This is what catches a
                // dash pattern laid out in the wrong frame - same
                // dots, same spacing, interleaved with the atlas's
                // instead of on top of them - which an earlier draft
                // admitted as unavoidable and is not.
                assertTrue(blankShare(model, familyInk, page.wide(),
                                page.high(), furniture) < 0.2,
                        what + ": and what it claims has ink under it");

                // And three checks aimed at the three ways this
                // reconstruction has actually been wrong, because the
                // two above are a net rather than a sight: a symbol
                // partly hidden under another has fewer visible ink
                // pixels than shape, so the ratio that would catch a
                // solid ring at 1.48 also fails an honest occluded box
                // at 1.46. Each of these asks its own question.
                switch (ChartRenderer.symbolFor(mark.deepSky())) {
                    // Dotted, one pixel on and three off: the shape
                    // must be in many pieces and must cover roughly
                    // half of the same ring drawn solid. Drawn solid,
                    // it is two pieces and all of it.
                    case DOTTED_CIRCLE -> {
                        // The dash pattern is pinned from both ends,
                        // because either alone admits the wrong one.
                        // The atlas dots this ring two and a half
                        // pixels on and two and a half off; a study
                        // that dotted it one on and three off - which
                        // is the boundary stroke, and which this study
                        // did - has the same "many pieces covering
                        // about half" as a lot of other patterns.
                        double solid = countOf(rasterised(
                                new java.awt.geom.Area(
                                        new java.awt.BasicStroke(1.0f)
                                                .createStrokedShape(
                                                        mark.outline())),
                                page.wide(), page.high()));
                        int pieces = subpathsOf(drawn);
                        assertTrue(pieces > 20, what + ": its ring is"
                                + " dotted, not solid - " + pieces
                                + " pieces");
                        // How often a dash starts, along the ring.
                        double period = perimeterOf(mark.outline())
                                / pieces;
                        assertTrue(Math.abs(period - 5.0) < 0.8,
                                what + ": a dash every "
                                        + String.format(Locale.ROOT,
                                                "%.2f", period)
                                        + " pixels, where the atlas"
                                        + " starts one every 5.0");
                        // And how much of each period is the dash.
                        double share = claimed / solid;
                        // Measured: 0.68 for the atlas's own pattern,
                        // 0.48 for one on and three off, 1.00 solid.
                        // Antialiasing bleeds each dash's ends, which
                        // is why half the ring inks rather more than
                        // half of it.
                        assertTrue(share > 0.58 && share < 0.80, what
                                + ": and the dots cover half of the"
                                + " ring rather than a quarter of it"
                                + " or all of it: "
                                + String.format(Locale.ROOT, "%.2f",
                                        share));
                    }
                    // Stroked along its own boundary at one pixel:
                    // area over perimeter is the width, whatever else
                    // is drawn over it.
                    case BOX -> {
                        double width = areaOf(drawn)
                                / perimeterOf(mark.outline());
                        assertTrue(width > 0.85 && width < 1.2, what
                                + ": drawn at the atlas's own one-pixel"
                                + " stroke, not "
                                + String.format(Locale.ROOT, "%.2f",
                                        width));
                    }
                    // A circle at a fraction of the spokes' reach,
                    // with nothing between the two. A circle drawn at
                    // the wrong radius lands in that gap, where the
                    // renderer inks nothing either.
                    case PLANETARY -> {
                        // Between its circle and the reach of its
                        // spokes there is nothing, and the emptiest
                        // line through that gap is the diagonal
                        // between two spokes. A circle drawn at the
                        // wrong radius lands on it.
                        double turn = -Math.toRadians(mark.deepSky()
                                .positionAngleDegrees()) + Math.PI / 4.0;
                        for (double at : new double[] {
                                mark.reach() / 1.7, mark.reach() / 2.0}) {
                            double x = mark.centre().x()
                                    + at * Math.cos(turn);
                            double y = mark.centre().y()
                                    + at * Math.sin(turn);
                            java.awt.geom.Rectangle2D spot =
                                    new java.awt.geom.Rectangle2D.Double(
                                            x - 1.5, y - 1.5, 3.0, 3.0);
                            assertFalse(drawn.intersects(spot), what
                                    + ": nothing is drawn between its"
                                    + " circle and its spokes' reach,"
                                    + " and nothing is claimed there"
                                    + " either - " + Math.round(at)
                                    + " px out along the diagonal");
                            assertFalse(ink.within(spot).any(), what
                                    + ": and the renderer inks nothing"
                                    + " there");
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        assertTrue(judged > 8, "the pages draw symbols to judge: "
                + judged);
    }

    /**
     * The band a symbol's ink runs in: its dotted ring drawn solid,
     * so a check about where ink is does not depend on where along
     * the ring each dot falls. Everything else is its own ink.
     */
    private static java.awt.geom.Area bandOf(ChartRenderer.DrawnMark mark) {
        if (ChartRenderer.symbolFor(mark.deepSky())
                == ChartRenderer.Symbol.DOTTED_CIRCLE) {
            return new java.awt.geom.Area(new java.awt.BasicStroke(1.0f)
                    .createStrokedShape(mark.outline()));
        }
        return new java.awt.geom.Area(mark.ink());
    }

    /** The shape as pixels, rasterised the way the chart is. */
    private static boolean[] rasterised(java.awt.geom.Area shape, int wide,
                                        int high) {
        BufferedImage image = new BufferedImage(wide, high,
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, wide, high);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(java.awt.Color.BLACK);
            g.fill(shape);
        } finally {
            g.dispose();
        }
        boolean[] mask = new boolean[wide * high];
        int[] pixels = ((java.awt.image.DataBufferInt)
                image.getRaster().getDataBuffer()).getData();
        for (int at = 0; at < pixels.length; at++) {
            mask[at] = (pixels[at] & 0xffffff) != 0xffffff;
        }
        return mask;
    }

    /** The area of a shape, by the shoelace formula over its outline. */
    private static double areaOf(java.awt.geom.Area shape) {
        double twice = 0.0;
        double[] point = new double[6];
        double startX = 0.0;
        double startY = 0.0;
        double lastX = 0.0;
        double lastY = 0.0;
        for (java.awt.geom.PathIterator along =
                shape.getPathIterator(null, 0.05); !along.isDone();
                along.next()) {
            switch (along.currentSegment(point)) {
                case java.awt.geom.PathIterator.SEG_MOVETO -> {
                    startX = point[0];
                    startY = point[1];
                    lastX = point[0];
                    lastY = point[1];
                }
                case java.awt.geom.PathIterator.SEG_LINETO -> {
                    twice += lastX * point[1] - point[0] * lastY;
                    lastX = point[0];
                    lastY = point[1];
                }
                case java.awt.geom.PathIterator.SEG_CLOSE -> {
                    twice += lastX * startY - startX * lastY;
                    lastX = startX;
                    lastY = startY;
                }
                default -> {
                }
            }
        }
        return Math.abs(twice) / 2.0;
    }

    /** How many separate pieces a shape is drawn in. */
    private static int subpathsOf(java.awt.geom.Area shape) {
        int pieces = 0;
        double[] point = new double[6];
        for (java.awt.geom.PathIterator along =
                shape.getPathIterator(null); !along.isDone();
                along.next()) {
            if (along.currentSegment(point)
                    == java.awt.geom.PathIterator.SEG_MOVETO) {
                pieces++;
            }
        }
        return pieces;
    }

    /** The length once round a shape's outline. */
    private static double perimeterOf(java.awt.Shape shape) {
        double length = 0.0;
        double[] point = new double[6];
        double startX = 0.0;
        double startY = 0.0;
        double lastX = 0.0;
        double lastY = 0.0;
        for (java.awt.geom.PathIterator along =
                shape.getPathIterator(null, 0.05); !along.isDone();
                along.next()) {
            switch (along.currentSegment(point)) {
                case java.awt.geom.PathIterator.SEG_MOVETO -> {
                    startX = point[0];
                    startY = point[1];
                    lastX = point[0];
                    lastY = point[1];
                }
                case java.awt.geom.PathIterator.SEG_LINETO -> {
                    length += Math.hypot(point[0] - lastX,
                            point[1] - lastY);
                    lastX = point[0];
                    lastY = point[1];
                }
                case java.awt.geom.PathIterator.SEG_CLOSE -> {
                    length += Math.hypot(startX - lastX, startY - lastY);
                    lastX = startX;
                    lastY = startY;
                }
                default -> {
                }
            }
        }
        return length;
    }

    private static java.awt.geom.Area annulus(double centreX,
                                              double centreY, double inner,
                                              double outer) {
        java.awt.geom.Area ring = new java.awt.geom.Area(
                new java.awt.geom.Ellipse2D.Double(centreX - outer,
                        centreY - outer, 2.0 * outer, 2.0 * outer));
        ring.subtract(new java.awt.geom.Area(
                new java.awt.geom.Ellipse2D.Double(centreX - inner,
                        centreY - inner, 2.0 * inner, 2.0 * inner)));
        return ring;
    }

    private static java.awt.geom.Area intersection(java.awt.geom.Area one,
                                                   java.awt.geom.Area other) {
        java.awt.geom.Area both = (java.awt.geom.Area) one.clone();
        both.intersect(other);
        return both;
    }

    private static int countOf(boolean[] mask) {
        int lit = 0;
        for (boolean one : mask) {
            if (one) {
                lit++;
            }
        }
        return lit;
    }

    /**
     * What share of a shape's pixels have no ink within two of them,
     * outside the furniture.
     *
     * <p>The furniture has to come out: IC 434's box on the Orion page
     * runs down past the page's edge and under the title block, which
     * is painted last and opaque, so a third of a perfectly correct
     * shape has nothing visible beneath it. That is the block covering
     * a symbol, not a symbol drawn in the wrong place.
     */
    private static double blankShare(boolean[] model, Ink ink, int wide,
                                     int high,
                                     java.awt.geom.Rectangle2D furniture) {
        int claimed = 0;
        int blank = 0;
        for (int y = 0; y < high; y++) {
            for (int x = 0; x < wide; x++) {
                if (!model[y * wide + x]
                        || furniture != null && furniture.contains(x, y)) {
                    continue;
                }
                claimed++;
                if (!ink.within(new java.awt.geom.Rectangle2D.Double(
                        x - 1.0, y - 1.0, 3.0, 3.0)).any()) {
                    blank++;
                }
            }
        }
        return claimed == 0 ? 0.0 : (double) blank / claimed;
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
        assertFalse(widest.ink().intersects(middle),
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
