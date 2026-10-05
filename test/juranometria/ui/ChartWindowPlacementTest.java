package juranometria.ui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where the chart window opens and which bounds are remembered (#450,
 * ruled on #449): the companion's reachability rule, applied to the
 * chart window; a monitor that has gone leaves the window opening as
 * it always has; maximised and native full-screen bounds are not the
 * ordinary bounds the store keeps.
 */
class ChartWindowPlacementTest {

    private static final Rectangle LAPTOP = new Rectangle(0, 33, 1512, 868);
    private static final Rectangle MONITOR = new Rectangle(1512, -458, 2560, 1440);
    private static final Dimension MINIMUM = new Dimension(640, 480);

    @Test
    void whereItWasIsWhereItOpensWhileThatIsOnAScreen() {
        Rectangle was = new Rectangle(1700, -300, 1800, 1200);
        assertEquals(Optional.of(was), ChartWindowPlacement.opening(
                Optional.of(was), List.of(LAPTOP, MONITOR), MINIMUM));
    }

    @Test
    void aMonitorThatHasGoneLeavesItOpeningAsItAlwaysHas() {
        Rectangle onTheMonitor = new Rectangle(1700, -300, 1800, 1200);
        assertEquals(Optional.empty(), ChartWindowPlacement.opening(
                Optional.of(onTheMonitor), List.of(LAPTOP), MINIMUM),
                "nothing on a screen that exists: packed and centred, as before");
        assertEquals(Optional.empty(), ChartWindowPlacement.opening(
                Optional.empty(), List.of(LAPTOP), MINIMUM), "never chosen: the same");
    }

    @Test
    void aSmallerScreenShrinksItAndMovesItInside() {
        Rectangle was = new Rectangle(1200, 600, 1800, 1200);
        Rectangle fitted = ChartWindowPlacement.opening(Optional.of(was),
                List.of(LAPTOP), MINIMUM).orElseThrow();
        assertTrue(LAPTOP.contains(fitted), fitted.toString());
        assertEquals(LAPTOP.width, fitted.width);
        assertEquals(LAPTOP.height, fitted.height);
    }

    @Test
    void aTitleStripAboveEveryScreenCannotBeGrabbedSoItIsNotUsed() {
        Rectangle above = new Rectangle(200, -900, 900, 700);
        assertEquals(Optional.empty(), ChartWindowPlacement.opening(
                Optional.of(above), List.of(LAPTOP), MINIMUM));
    }

    @Test
    void itIsNeverSmallerThanItsMinimum() {
        Rectangle tiny = new Rectangle(100, 100, 10, 10);
        Rectangle opened = ChartWindowPlacement.opening(Optional.of(tiny),
                List.of(LAPTOP), MINIMUM).orElseThrow();
        assertEquals(MINIMUM.width, opened.width);
        assertEquals(MINIMUM.height, opened.height);
    }

    @Test
    void onlyOrdinaryBoundsAreWorthRemembering() {
        Rectangle screen = new Rectangle(0, 0, 1512, 982);
        Insets bars = new Insets(33, 0, 81, 0);
        // Measured on #449: packed, maximised and native full screen.
        assertTrue(ChartWindowPlacement.ordinary(new Rectangle(306, 75, 900, 785),
                true, screen, bars), "a packed window's bounds are ordinary");
        assertFalse(ChartWindowPlacement.ordinary(new Rectangle(0, 33, 1512, 868),
                false, screen, bars), "a maximised window's are the flag's, not these");
        assertFalse(ChartWindowPlacement.ordinary(new Rectangle(0, 33, 1512, 949),
                true, screen, bars), "native full screen fills the screen below"
                + " the menu bar in the normal state, and is not remembered");
        assertTrue(ChartWindowPlacement.ordinary(new Rectangle(0, 33, 1512, 868),
                true, screen, bars), "a window the reader sized to the usable"
                + " area above the dock is ordinary");
    }
}
