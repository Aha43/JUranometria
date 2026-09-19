package juranometria.render;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A label does not flicker while the reader drags (issue #340).
 *
 * <p>Owner testing found M32's designation jumping back and forth on
 * the Andromeda page. The mark was innocent - it moves smoothly to
 * within two thousandths of a pixel - and so was the pan solver,
 * which keeps the grabbed sky under the pointer exactly. What moved
 * was the name.
 *
 * <p>Every one of M32's eight candidate positions is buried in M31's
 * disc, so the label is always placed under duress and the rule that
 * decides is "cover the least ink". Being inside the disc, every
 * candidate covers exactly the box's own area - 420 square pixels -
 * and the eight values came back from the area arithmetic differing
 * by <strong>five hundredths of a billionth of a square pixel</strong>.
 * A strict comparison let whichever of them happened to round down
 * take the label, and as the page moved that changed almost every
 * frame: <strong>163 hops in 201 steps, up to 64 pixels</strong>.
 *
 * <p>The repair carries no memory of previous frames, and these hold
 * that as firmly as they hold the stability: a settled page must give
 * the same answer however the reader arrived at it, because the page
 * a reader prints must be the page a reader sees, and a printed sheet
 * has no history.
 */
class LabelStabilityTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private static final int WIDE_PX = 1000;

    private static final int HIGH_PX = 700;

    /** M31, where the defect was reported. */
    private static final SkyPosition ANDROMEDA =
            new SkyPosition(10.6847, 41.2687);

    /** M32's own catalogue identity. */
    private static final String M32 = "NGC 221";

    /** The field the oscillation was found at. */
    private static final double CROWDED_FIELD = 6.0;

    /**
     * How far the reader wanders before coming back.
     *
     * <p>Measured, not chosen: a quarter of a degree away every
     * label takes the same candidate it takes here, so a route
     * ending there could not tell a remembered answer from a
     * recomputed one. A degree away M32 takes candidate 1 where the
     * target page gives it 0, which is what makes the comparison
     * able to fail.
     */
    private static final double AWAY_DEGREES = 1.0;

    @Test
    void aLabelDoesNotFlickerAsThePageIsDragged() {
        // The reproduction from the issue, at its own field: a slow
        // drag east, a four-hundredth of the field at a time.
        int hops = 0;
        int seen = 0;
        int previous = -2;
        double worst = 0.0;
        Rectangle2D was = null;
        for (int step = 0; step <= 200; step++) {
            LabelPlacement.Placement placed = labelOf(
                    draggedTo(step), M32);
            if (placed == null || placed.omitted()) {
                continue;
            }
            seen++;
            if (previous != -2 && placed.candidate() != previous) {
                hops++;
                worst = Math.max(worst, Math.hypot(
                        placed.at().getCenterX() - was.getCenterX(),
                        placed.at().getCenterY() - was.getCenterY()));
            }
            previous = placed.candidate();
            was = placed.at();
        }

        // The premise: the label is on the page throughout, so a
        // count of zero hops is about stability and not about a name
        // that was never written.
        assertTrue(seen >= 200,
                "the label is written at every step of the drag: "
                        + seen);
        assertTrue(hops <= 2,
                "a slow drag moves the label " + hops + " times"
                        + " (worst " + worst + " px), where the defect"
                        + " moved it 163 times in 201 steps");
    }

    @Test
    void everyCandidateIsBuriedSoTheChoiceIsMadeUnderDuress() {
        // Why this page is the one that failed, stated rather than
        // assumed. If some candidate were free the label would take
        // it and the least-ink rule would never be reached, and this
        // test would be measuring a different page from the one the
        // reader reported.
        LabelPlacement.Placement placed = labelOf(draggedTo(190), M32);
        assertTrue(placed != null && !placed.omitted(),
                "the label is written");
        assertTrue(placed.underDuress(),
                "every candidate is refused, so the placement is the"
                        + " least bad rather than the first free");
        assertEquals(placed.request().candidates().size(),
                placed.refusals().size(),
                "all of them, not merely the ones before the winner");
    }

    @Test
    void aSettledPageDoesNotDependOnHowTheReaderReachedIt() {
        // The constraint the repair had to satisfy, made into a test
        // rather than left as an intention. A page opened directly
        // and the same page arrived at by dragging must place their
        // text identically, because the sheet a reader prints carries
        // no history - and any future stabilisation that remembers a
        // previous frame has to answer this.
        // Andromeda, where the defect was, and Sagittarius, which
        // carries enough names for the comparison to be about a page
        // rather than about four labels.
        //
        // The order is the whole test. The baseline is taken FIRST,
        // before any other page has been placed, so it is the answer
        // a reader gets who opens this page and nothing else. Only
        // then is the drag walked, and only then is the same page
        // assembled again - freshly, not the object the baseline
        // used - and placed. If anything survived between
        // placements, the second answer would have had a chance to
        // differ from the first, and this compares exactly those two.
        int compared = 0;
        boolean coveredTheOneThatMoved = false;
        for (SkyPosition centre : List.of(centreAt(200),
                new SkyPosition(266.0, -28.0))) {
            List<LabelPlacement.Placement> beforeAnyHistory =
                    placementsOn(pageAt(centre));

            // The route: a drag up to that page, and then away from
            // it and back - each page placed, so a cache or a
            // remembered candidate has something to have remembered.
            //
            // The detour is the part that makes this able to fail. A
            // neighbouring page gives the same answer, so history
            // ending there would be indistinguishable from no history
            // at all; a degree away, M32 takes a different candidate,
            // so anything carried forward from there would show.
            for (int step = 190; step < 200; step++) {
                placementsOn(pageAt(walkedTo(centre, step)));
            }
            placementsOn(pageAt(new SkyPosition(
                    centre.raDegrees() + AWAY_DEGREES,
                    centre.decDegrees())));

            List<LabelPlacement.Placement> afterTheWalk =
                    placementsOn(pageAt(centre));

            assertEquals(beforeAnyHistory.size(), afterTheWalk.size(),
                    "the same page carries the same text either way");
            for (int at = 0; at < beforeAnyHistory.size(); at++) {
                LabelPlacement.Placement first = beforeAnyHistory.get(at);
                LabelPlacement.Placement later = afterTheWalk.get(at);
                assertEquals(first.request().id(), later.request().id(),
                        "in the same order");
                assertEquals(first.candidate(), later.candidate(),
                        first.request().id() + " takes the same"
                                + " position whether the reader opened"
                                + " this page or dragged onto it");
                assertEquals(first.at(), later.at(),
                        first.request().id() + " is written in exactly"
                                + " the same place");
                compared++;
                if (M32.equals(first.request().id())) {
                    coveredTheOneThatMoved = true;
                }
            }
        }
        assertTrue(coveredTheOneThatMoved,
                "and the comparison covers the label that moved");
        // The coverage premise, which is the count these two pages
        // actually carry rather than a number chosen to be
        // impressive: ten labels, M32's among them. If the pages ever
        // carry fewer, this says so instead of quietly comparing
        // nothing.
        assertTrue(compared >= 10,
                "every label both pages carry was compared: "
                        + compared);
    }

    @Test
    void arealImprovementStillWinsHoweverSmall() {
        // The other half of the rule, and the one that keeps it from
        // being a licence to ignore the cost. The allowance is the
        // width of the arithmetic's error, not a tolerance for real
        // differences: a later candidate that genuinely covers less
        // ink must still take the label, even when the difference is
        // far smaller than a reader could see.
        //
        // Built here rather than found on a page, because a fixture
        // with a stated difference says what it is testing. One
        // obstacle, two candidates of the same size, the second
        // overlapping it by a five-hundred-thousandth less.
        Rectangle2D obstacle = new Rectangle2D.Double(0, 0, 1000, 100);
        Rectangle2D first = new Rectangle2D.Double(10, 90, 50, 20);
        Rectangle2D second = new Rectangle2D.Double(10, 90.000001, 50, 20);
        double byHowMuch = area(obstacle, first) - area(obstacle, second);

        // The premise: the difference is real - above the arithmetic's
        // own error by orders of magnitude - and still far below one
        // device pixel of ink.
        assertTrue(byHowMuch > 1.0e-9 * area(obstacle, first),
                "the fixture's difference is larger than the noise the"
                        + " rule discounts: " + byHowMuch);
        assertTrue(byHowMuch < 1.0,
                "and smaller than a pixel, so this is not being won by"
                        + " being obvious: " + byHowMuch);

        LabelPlacement placing = new LabelPlacement(1000, 700,
                List.of(new LabelPlacement.Obstacle(
                        LabelPlacement.Refusal.MARK, "a mark",
                        obstacle)));
        LabelPlacement.Placement placed = placing.place(
                new LabelPlacement.Request(
                        LabelPlacement.Family.STAR, "a star", "a star",
                        30.0, 100.0, List.of(first, second), null, null,
                        false, 0.0));

        assertTrue(placed.underDuress(),
                "both candidates are refused, so the least-ink rule"
                        + " decides");
        assertEquals(1, placed.candidate(),
                "and the cheaper one takes the label, by "
                        + byHowMuch + " square pixels");
    }

    /** How much of the obstacle a box covers. */
    private static double area(Rectangle2D obstacle, Rectangle2D box) {
        Rectangle2D shared = obstacle.createIntersection(box);
        return shared.isEmpty() ? 0.0
                : shared.getWidth() * shared.getHeight();
    }

    /** A freshly assembled page centred here. */
    private static ChartScene pageAt(SkyPosition centre) {
        return Atlas.assembler().assemble(
                new ChartViewState(centre, CROWDED_FIELD, 8.0),
                WIDE_PX, HIGH_PX);
    }

    /** The page the reader has after this many steps of the drag. */
    private static ChartScene draggedTo(int step) {
        return Atlas.assembler().assemble(
                new ChartViewState(centreAt(step), CROWDED_FIELD, 8.0),
                WIDE_PX, HIGH_PX);
    }

    /** A centre this far along a drag east from a given place. */
    private static SkyPosition walkedTo(SkyPosition from, int step) {
        return new SkyPosition(
                from.raDegrees() + (step - 200) * CROWDED_FIELD / 400.0,
                from.decDegrees());
    }

    private static SkyPosition centreAt(int step) {
        return new SkyPosition(
                ANDROMEDA.raDegrees() + step * CROWDED_FIELD / 400.0,
                ANDROMEDA.decDegrees());
    }

    private static LabelPlacement.Placement labelOf(ChartScene scene,
                                                    String id) {
        for (LabelPlacement.Placement placed : placementsOn(scene)) {
            if (placed.request().id().equals(id)) {
                return placed;
            }
        }
        return null;
    }

    /** What production places on this page, through its own route. */
    private static List<LabelPlacement.Placement> placementsOn(
            ChartScene scene) {
        BufferedImage canvas = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            return new ArrayList<>(
                    new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                            .textPlacements(
                                    ChartRenderer.TextMetrics.of(g),
                                    scene, ChartOptions.DEFAULTS));
        } finally {
            g.dispose();
        }
    }
}
