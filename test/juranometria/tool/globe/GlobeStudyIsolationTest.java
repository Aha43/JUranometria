package juranometria.tool.globe;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * That one globe study cannot change what the next one measures
 * (Sprint 32, issue #301; found by the review of PR #336).
 *
 * <p>The frame study asks what fraction of the page a disc should
 * fill, and the only way to ask is a JVM-wide property. Run on its
 * own it put the property back to nothing by finishing; run in one
 * process with the other ten - which is how the evidence contract
 * runs them - it left the last fraction it tried, 86%, set for
 * everything after it. The pointing study then reported a pixel at
 * r = 0.95 as 83.8 degrees out from the centre instead of 71.8, and
 * every study between them measured a disc four percent too small.
 *
 * <p>Nothing found that until these studies were published and the
 * contract ran them together. So it is held here: the property is
 * restored, <em>and</em> a globe assembled afterwards has the disc it
 * would have had if the frame study had never run - the second half
 * because a property put back by luck rather than by the code would
 * satisfy the first.
 */
class GlobeStudyIsolationTest {

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    private static final int SIDE_PX = 600;

    /** What the atlas's own default frame puts on a page. */
    private static final double DEFAULT_FRAME = 0.90;

    @Test
    void theFrameStudyPutsTheJvmBackAsItFoundIt() throws Exception {
        String before = System.getProperty(
                GlobeFrameStudyMain.FRAME_PROPERTY);
        double discBefore = discRadiusPx();

        PrintStream said = System.out;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream(),
                    true, "UTF-8"));
            GlobeFrameStudyMain.main(new String[0]);
        } finally {
            System.setOut(said);
        }

        assertEquals(before,
                System.getProperty(GlobeFrameStudyMain.FRAME_PROPERTY),
                "the frame study left the JVM-wide globe frame where"
                        + " its last candidate happened to stop, so"
                        + " every globe drawn after it in this process"
                        + " is a different size");
        assertEquals(discBefore, discRadiusPx(), 1.0e-9,
                "a globe assembled after the frame study is not the"
                        + " size it would have been before it: the"
                        + " study has changed what the next study"
                        + " measures");
    }

    @Test
    void andTheDefaultIsTheAtlassOwn() {
        assertNull(System.getProperty(
                        GlobeFrameStudyMain.FRAME_PROPERTY),
                "a suite that begins with the frame already overridden"
                        + " is measuring something else");
        assertEquals(DEFAULT_FRAME * SIDE_PX / 2.0, discRadiusPx(),
                1.0e-9,
                "the globe's disc is nine tenths of the page's short"
                        + " side, and the studies measure radii as"
                        + " fractions of that");
    }

    /** The drawn disc's radius in pixels, as the page has it. */
    private static double discRadiusPx() {
        DrawnPage page = Atlas.assembler().assembleForStudy(
                SAGITTARIUS, 180.0, 5.0, "Sagittarius and Scorpius",
                new GlobeProjection(SAGITTARIUS), SIDE_PX, SIDE_PX);
        return new ViewportMapping(page).pixelsPerPlaneUnit()
                * page.projection().visiblePlaneRadius();
    }
}
