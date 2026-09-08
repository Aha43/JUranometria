package juranometria.render;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where supplied geometry may go, and why (Sprint 31, issue #313).
 *
 * <p>Not a font in sight, which is the point of the boundary the gate
 * drew: the seam is handed rectangles and shapes and hands back
 * decisions, so what it does can be stated exactly here in numbers a
 * reader of this file can check by hand. Every earlier attempt to hold
 * placement to account in this repository had to go through a
 * toolkit's idea of how wide a word is, and could only ever say "about
 * this much, on this machine".
 */
class LabelPlacementTest {

    private static final double WIDE = 200.0;
    private static final double HIGH = 100.0;

    private static Rectangle2D box(double x, double y) {
        return new Rectangle2D.Double(x, y, 20.0, 10.0);
    }

    private static LabelPlacement.Request star(String id,
                                               List<Rectangle2D> candidates) {
        return new LabelPlacement.Request(LabelPlacement.Family.STAR, id,
                id, 50.0, 50.0, candidates, null, null, false, 3.0);
    }

    private static LabelPlacement placementWith(
            LabelPlacement.Obstacle... ink) {
        return new LabelPlacement(WIDE, HIGH, List.of(ink));
    }

    private static LabelPlacement.Obstacle mark(String id, Shape ink) {
        return new LabelPlacement.Obstacle(LabelPlacement.Refusal.MARK, id,
                ink);
    }

    @Test
    void theFirstFreeCandidateWinsAndNothingMoves() {
        // An uncrowded page keeps the placement it has: the first
        // candidate is where every label sits today, and a policy
        // that moved things for no reason would change released
        // pages for nothing.
        LabelPlacement placement = placementWith();
        LabelPlacement.Placement placed = placement.place(
                star("a", List.of(box(10, 10), box(40, 10))));

        assertEquals(0, placed.candidate(), "it took what it asked for");
        assertFalse(placed.moved(), "so it did not move");
        assertFalse(placed.underDuress(), "and nothing refused it");
        assertEquals(List.of(), placed.refusals(), "with nothing to say");
    }

    @Test
    void thePaperRefusesACandidateThatWouldLeaveIt() {
        LabelPlacement placement = placementWith();
        LabelPlacement.Placement placed = placement.place(
                star("a", List.of(box(WIDE - 5, 10), box(40, 10))));

        assertEquals(1, placed.candidate(), "it took the one that fits");
        assertEquals(1, placed.refusals().size(), "and says why once");
        assertEquals(LabelPlacement.Refusal.PAGE_EDGE,
                placed.refusals().get(0).kind(),
                "the paper refused the first");
    }

    @Test
    void furnitureRefusesAndSaysWhichBlock() {
        LabelPlacement placement = placementWith(
                new LabelPlacement.Obstacle(
                        LabelPlacement.Refusal.FURNITURE, "title block",
                        new Rectangle2D.Double(0, 0, 40, 40)));
        LabelPlacement.Placement placed = placement.place(
                star("a", List.of(box(10, 10), box(100, 60))));

        assertEquals(1, placed.candidate());
        assertEquals(LabelPlacement.Refusal.FURNITURE,
                placed.refusals().get(0).kind());
        assertEquals("title block", placed.refusals().get(0).by(),
                "and names the piece of furniture that refused it");
    }

    @Test
    void aMarkRefusesButNotTheOneTheLabelNames() {
        // A star's name is anchored beside its own disc by decision.
        // A policy that treated that as a collision would move every
        // label on every page.
        //
        // The two shapes here are EQUAL AND NOT THE SAME OBJECT, which
        // is the whole point: the obstacles and the requests on a real
        // page are built from separate calls and hold separate shapes.
        // An earlier version of this test handed one object to both
        // sides and so passed while production compared them by
        // reference and never matched - the exemption was dead, and a
        // star's name could be refused by its own disc.
        Shape own = new Ellipse2D.Double(5, 5, 20, 20);
        Shape ownAgain = new Ellipse2D.Double(5, 5, 20, 20);
        Shape other = new Ellipse2D.Double(100, 5, 20, 20);
        assertNotSame(own, ownAgain, "two shapes, not one");
        LabelPlacement placement = placementWith(mark("its own", ownAgain),
                mark("somebody else's", other));

        LabelPlacement.Placement beside = placement.place(
                new LabelPlacement.Request(LabelPlacement.Family.STAR,
                        "a", "a", 15.0, 15.0, List.of(box(10, 10)),
                        "its own", null, false, 3.0));
        assertEquals(0, beside.candidate(),
                "it may sit on the mark it names");
        assertEquals(List.of(), beside.refusals(),
                "and nothing refused it");

        LabelPlacement.Placement across = placement.place(
                new LabelPlacement.Request(LabelPlacement.Family.STAR,
                        "b", "b", 15.0, 15.0,
                        List.of(box(102, 8), box(60, 60)), "its own",
                        null, false, 3.0));
        assertEquals(1, across.candidate(), "and not on another");
        assertEquals("somebody else's", across.refusals().get(0).by());
        assertEquals(LabelPlacement.Refusal.MARK,
                across.refusals().get(0).kind());
    }

    @Test
    void whatCannotBeDrawnAnywhereIsOmittedAndSaysWhy() {
        // The one outcome that is an omission. The paper's edge and a
        // constellation's own region are not costs the fallback may
        // spend, so when every candidate breaks one of them there is
        // nothing left to choose - and the alternative, which this had
        // for a round, is worse: taking candidate zero anyway and
        // writing a name off the page or onto another constellation.
        LabelPlacement placement = placementWith();
        LabelPlacement.Placement placed = placement.place(
                new LabelPlacement.Request(
                        LabelPlacement.Family.CONSTELLATION, "Ori", "ORION",
                        30, 30,
                        List.of(new Rectangle2D.Double(WIDE - 5, 10, 20, 10),
                                new Rectangle2D.Double(-30, 10, 20, 10)),
                        null, new Rectangle2D.Double(0, 0, 60, 60), false,
                        0.0));

        assertTrue(placed.omitted(), "it is not drawn");
        assertEquals(null, placed.at(), "there is nowhere it could go");
        assertFalse(placed.moved(), "and it did not move: it is absent");
        assertEquals(2, placed.refusals().size(),
                "with a reason for every candidate it had");
        assertEquals(LabelPlacement.Refusal.PAGE_EDGE,
                placed.refusals().get(0).kind());
    }

    @Test
    void anOmissionIsNeverTheSilentKind() {
        // Everything a page can draw is drawn: a label with no free
        // candidate takes the least bad rather than being dropped.
        // Only the impossible is absent, and this holds the line
        // between the two on a page where both happen.
        Shape everywhere = new Rectangle2D.Double(0, 0, WIDE, HIGH);
        LabelPlacement placement = placementWith(mark("all", everywhere));
        LabelPlacement.Placement crowded = placement.place(
                star("crowded", List.of(box(10, 10), box(60, 60))));
        assertFalse(crowded.omitted(),
                "a label with nowhere free is still drawn");
        assertTrue(crowded.underDuress(), "and says it is under duress");

        LabelPlacement.Placement impossible = placement.place(
                star("impossible",
                        List.of(new Rectangle2D.Double(WIDE + 10, 10, 20,
                                10))));
        assertTrue(impossible.omitted(),
                "and a label with nowhere at all is not");
    }

    @Test
    void textAlreadyPlacedRefusesTextPlacedAfterIt() {
        LabelPlacement placement = placementWith();
        placement.place(star("first", List.of(box(10, 10))));
        LabelPlacement.Placement second = placement.place(
                star("second", List.of(box(12, 12), box(100, 60))));

        assertEquals(1, second.candidate());
        assertEquals(LabelPlacement.Refusal.TEXT,
                second.refusals().get(0).kind());
        assertEquals("STAR:first", second.refusals().get(0).by(),
                "and says whose text took the room");
    }

    @Test
    void theGuaranteedLabelTakesItsPlaceAndKeepsIt() {
        // The one label that is promised a position. It is placed
        // before anything else and its box joins the accepted set, so
        // by the time a lower-priority request is asked, the room is
        // gone: the guarantee cannot be defeated afterwards.
        Shape crowd = new Rectangle2D.Double(0, 0, 60, 60);
        LabelPlacement placement = placementWith(mark("a crowd", crowd));
        List<LabelPlacement.Placement> placed = placement.placeAll(List.of(
                star("ordinary", List.of(box(10, 10), box(100, 60))),
                new LabelPlacement.Request(LabelPlacement.Family.TARGET,
                        "target", "target", 15.0, 15.0,
                        List.of(box(10, 10)), null, null, true, 0.0)));

        LabelPlacement.Placement target = of(placed, "target");
        assertEquals(0, target.candidate(),
                "the guaranteed label is where it asked to be");
        assertEquals(box(10, 10), target.at());
        LabelPlacement.Placement ordinary = of(placed, "ordinary");
        assertNotEquals(box(10, 10), ordinary.at(),
                "and nothing else took that room");
    }

    @Test
    void theOrderIsTargetThenStarsThenDeepSkyThenNames() {
        LabelPlacement placement = placementWith();
        List<LabelPlacement.Placement> placed = placement.placeAll(List.of(
                new LabelPlacement.Request(
                        LabelPlacement.Family.CONSTELLATION, "Ori", "ORION",
                        0, 0, List.of(box(10, 10)), null, null, false, 0.0),
                new LabelPlacement.Request(LabelPlacement.Family.DEEP_SKY,
                        "M 42", "M 42", 0, 0, List.of(box(10, 10)), null,
                        null, false, 1.0),
                star("faint", List.of(box(10, 10))),
                new LabelPlacement.Request(LabelPlacement.Family.STAR,
                        "bright", "bright", 0, 0, List.of(box(10, 10)),
                        null, null, false, 1.0),
                new LabelPlacement.Request(LabelPlacement.Family.TARGET,
                        "target", "target", 0, 0, List.of(box(10, 10)),
                        null, null, true, 0.0)));

        assertEquals(List.of("target", "bright", "faint", "M 42", "Ori"),
                placed.stream().map(one -> one.request().id()).toList(),
                "the decision's own priority, brightest star first");
    }

    @Test
    void twoOfTheSameBrightnessArePlacedInTheSameOrderEveryTime() {
        // Ties break on identity, so a page does not depend on which
        // order a set happened to hand its members over in.
        List<LabelPlacement.Request> asked = new ArrayList<>(List.of(
                star("zeta", List.of(box(10, 10))),
                star("alpha", List.of(box(10, 10))),
                star("mu", List.of(box(10, 10)))));
        List<String> first = placementWith().placeAll(asked).stream()
                .map(one -> one.request().id()).toList();
        java.util.Collections.reverse(asked);
        List<String> again = placementWith().placeAll(asked).stream()
                .map(one -> one.request().id()).toList();

        assertEquals(List.of("alpha", "mu", "zeta"), first);
        assertEquals(first, again,
                "the same page however the requests arrived");
    }

    @Test
    void aNameMayNotLeaveTheRegionItsFigureOwns() {
        // A name outside its own figure is naming a different part of
        // the sky, which is worse than the collision it was avoiding.
        Shape owns = new Rectangle2D.Double(0, 0, 60, 60);
        LabelPlacement placement = placementWith();
        LabelPlacement.Placement placed = placement.place(
                new LabelPlacement.Request(
                        LabelPlacement.Family.CONSTELLATION, "Ori", "ORION",
                        30, 30, List.of(box(100, 70), box(10, 10)), null,
                        owns, false, 0.0));

        assertEquals(1, placed.candidate());
        assertEquals(LabelPlacement.Refusal.OWNERSHIP,
                placed.refusals().get(0).kind(),
                "its own figure refused the first");
        assertEquals("Ori", placed.refusals().get(0).by());
    }

    @Test
    void nothingIsDroppedAndTheLeastBadIsTaken() {
        // The load-bearing choice of the whole decision: a label a
        // reader has today is worth more drawn awkwardly than not
        // drawn. Two marks, one large and one small, and every
        // candidate covers one of them.
        Shape large = new Rectangle2D.Double(5, 5, 30, 30);
        Shape small = new Rectangle2D.Double(100, 5, 4, 30);
        LabelPlacement placement = placementWith(mark("large", large),
                mark("small", small));
        LabelPlacement.Placement placed = placement.place(
                star("a", List.of(box(10, 10), box(98, 10))));

        assertTrue(placed.underDuress(), "every candidate was refused");
        assertEquals(2, placed.refusals().size(), "and all of them said so");
        assertEquals(1, placed.candidate(),
                "so it took the one covering the least ink");
        assertEquals(box(98, 10), placed.at());
    }

    @Test
    void theFallbackWillNotSpendThePaperOrTheOwnership() {
        // Those are not costs. A label off the page is not a label,
        // and a name outside its figure is naming something else -
        // neither is a price the least-bad candidate may pay.
        Shape everywhere = new Rectangle2D.Double(0, 0, WIDE, HIGH);
        LabelPlacement placement = placementWith(mark("all", everywhere));
        LabelPlacement.Placement placed = placement.place(
                new LabelPlacement.Request(
                        LabelPlacement.Family.CONSTELLATION, "Ori", "ORION",
                        30, 30,
                        List.of(new Rectangle2D.Double(WIDE - 5, 10, 20, 10),
                                box(10, 10)),
                        null, new Rectangle2D.Double(0, 0, 60, 60), false,
                        0.0));

        assertTrue(placed.underDuress());
        assertEquals(1, placed.candidate(),
                "the off-paper candidate was not in the running");
    }

    private static LabelPlacement.Placement of(
            List<LabelPlacement.Placement> placed, String id) {
        for (LabelPlacement.Placement one : placed) {
            if (one.request().id().equals(id)) {
                return one;
            }
        }
        throw new IllegalStateException("no placement for " + id);
    }
}
