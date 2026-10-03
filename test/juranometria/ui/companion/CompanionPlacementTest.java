package juranometria.ui.companion;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where the companion goes (#434, ruled on #433): where it was if a
 * reader can still reach it there, and beside the chart window if
 * not - never on a display that has gone, never larger than the
 * screen it is on.
 */
class CompanionPlacementTest {

    private static final Rectangle LAPTOP = new Rectangle(0, 25, 1440, 875);
    private static final Rectangle MONITOR = new Rectangle(1440, 0, 2560, 1415);
    private static final Dimension PREFERRED = new Dimension(360, 330);
    private static final Dimension MINIMUM = new Dimension(342, 120);
    private static final Rectangle CHART = new Rectangle(100, 60, 900, 760);

    private static Rectangle place(Rectangle remembered, List<Rectangle> screens,
                                   Rectangle owner) {
        return CompanionPlacement.place(Optional.ofNullable(remembered),
                screens, owner, PREFERRED, MINIMUM);
    }

    private static boolean inside(Rectangle window, List<Rectangle> screens) {
        return screens.stream().anyMatch(s -> s.contains(window));
    }

    @Test
    void whereItWasIsWhereItGoesWhileThatIsOnAScreen() {
        Rectangle was = new Rectangle(1900, 200, 380, 400);
        assertEquals(was, place(was, List.of(LAPTOP, MONITOR), CHART),
                "on the monitor, unchanged");
    }

    @Test
    void aDisplayThatHasGoneLeavesItBesideTheChartWindow() {
        Rectangle wasOnMonitor = new Rectangle(1900, 200, 380, 400);
        Rectangle placed = place(wasOnMonitor, List.of(LAPTOP), CHART);
        assertEquals(new Rectangle(CHART.x + CHART.width + CompanionPlacement.GAP,
                        CHART.y, PREFERRED.width, PREFERRED.height), placed,
                "the monitor is unplugged: beside the chart window's"
                        + " trailing edge, where there is room");
        assertTrue(inside(placed, List.of(LAPTOP)));
    }

    @Test
    void aSmallerDisplayShrinksItAndMovesItInside() {
        Rectangle tall = new Rectangle(1300, 25, 600, 1300);
        Rectangle placed = place(tall, List.of(LAPTOP), CHART);
        assertTrue(inside(placed, List.of(LAPTOP)),
                "no larger than the screen and inside it: " + placed);
        assertEquals(LAPTOP.height, placed.height);
    }

    @Test
    void halfOffTheScreenIsReachableAndBroughtBackOn() {
        Rectangle hanging = new Rectangle(1300, 300, 380, 400);
        Rectangle placed = place(hanging, List.of(LAPTOP), CHART);
        assertTrue(inside(placed, List.of(LAPTOP)), placed.toString());
        assertEquals(300, placed.y, "moved sideways only");
    }

    @Test
    void aHeadingAboveEveryScreenCannotBeGrabbedSoItIsNotUsed() {
        Rectangle above = new Rectangle(200, -500, 380, 520);
        Rectangle placed = place(above, List.of(LAPTOP), CHART);
        assertEquals(CHART.x + CHART.width + CompanionPlacement.GAP, placed.x,
                "only its bottom edge was on the screen: placed afresh");
    }

    @Test
    void aMaximisedChartWindowLeavesItOverTheTrailingEdge() {
        Rectangle maximised = new Rectangle(LAPTOP);
        Rectangle placed = place(null, List.of(LAPTOP), maximised);
        assertEquals(LAPTOP.x + LAPTOP.width - PREFERRED.width, placed.x,
                "no room beside it: over its trailing edge, on the screen");
        assertTrue(inside(placed, List.of(LAPTOP)));
    }

    @Test
    void itIsNeverNarrowerThanItsMinimumNorWiderThanTheScreen() {
        Rectangle placed = CompanionPlacement.place(Optional.empty(),
                List.of(LAPTOP), CHART, new Dimension(200, 300), MINIMUM);
        assertEquals(MINIMUM.width, placed.width);
        Rectangle tiny = new Rectangle(0, 0, 300, 400);
        Rectangle squeezed = CompanionPlacement.place(Optional.empty(),
                List.of(tiny), new Rectangle(0, 0, 300, 400), PREFERRED, MINIMUM);
        assertEquals(tiny.width, squeezed.width,
                "a screen narrower than the minimum wins: it is on screen");
        assertThrows(IllegalArgumentException.class, () -> place(null,
                List.of(), CHART));
    }
}
