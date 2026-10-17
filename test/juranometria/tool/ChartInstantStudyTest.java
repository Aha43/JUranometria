package juranometria.tool;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.sky.Observer;
import juranometria.ui.ChartComponent;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The #556 study's proposal held to its own words: the box states UTC
 * and the sidereal time and nothing that names a zone or an offset; on
 * paper it carries no action; and the allocation takes the lower right
 * on an ordinary page and the next free corner where the title block
 * leaves no room.
 */
class ChartInstantStudyTest {

    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-08-15T06:00:00Z"));

    @Test
    void theBoxStatesUtcAndSiderealTimeAndNoZone() {
        for (boolean norwegian : new boolean[] {false, true}) {
            List<String> screen = ChartInstantStudyMain.boxLines(OSLO, norwegian, true);
            assertEquals(5, screen.size(), "heading, observer, instant, sidereal time, action");
            assertEquals("2026-08-15 06:00:00 UTC", screen.get(2));
            for (String line : screen) {
                String l = line.toLowerCase(Locale.ROOT);
                assertFalse(l.contains("cest") || l.contains("gmt+") || l.contains("+02")
                        || l.contains("europe/") || l.contains("local time")
                        || l.contains("lokal tid"), "no zone or offset: " + line);
            }
            List<String> paper = ChartInstantStudyMain.boxLines(OSLO, norwegian, false);
            assertFalse(paper.get(4).contains("["), "no action on paper: " + paper.get(4));
        }
    }

    @Test
    void theAllocationTakesTheLowerRightAndFallsBackWhenTheTitleBlockLeavesNoRoom()
            throws Exception {
        assertEquals("LOWER_RIGHT", allocateAt(900).name());
        assertEquals("UPPER_RIGHT", allocateAt(520).name(),
                "a narrow page's lower right meets the title block");
    }

    private static Enum<?> allocateAt(int width) throws Exception {
        ChartComponent[] chart = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            chart[0] = new ChartComponent(Atlas.assembler(),
                    PageText.in(InterfaceText.forLanguage("en")));
            chart[0].setSize(width, 700);
            chart[0].setViewState(ChartViewState.DEFAULT);
        });
        Enum<?>[] out = new Enum<?>[1];
        SwingUtilities.invokeAndWait(() -> out[0] = ChartInstantStudyMain.allocate(chart[0],
                false, OSLO));
        assertTrue(out[0] != null, "a corner is found at " + width + " px");
        return out[0];
    }
}
