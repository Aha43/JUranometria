package juranometria.app;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.SkyPosition;
import juranometria.chart.WorkingSelection;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the export session actually wires together (Sprint 29,
 * issue #286).
 *
 * <p>The policy - never replace without asking - is held against
 * {@link ExportSheet} elsewhere. Holding only that leaves the
 * question of whether the running application <em>uses</em> it:
 * a session that passed a decision of "always yes" would satisfy
 * every one of those tests and replace a reader's file without a
 * word (PR #291 round 2). So this drives the session's own path.
 */
class ExportSheetSessionTest {

    private static ChartViewController navigation() {
        ChartViewController navigation = new ChartViewController();
        navigation.recenter(new SkyPosition(83.0, 0.0), 42.0);
        return navigation;
    }

    private static ChartComponent chart(ChartViewController navigation)
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler());
            holder[0].setSize(770, 523);
            holder[0].setViewState(navigation.state());
        });
        SwingUtilities.invokeAndWait(() -> { });
        return holder[0];
    }

    @Test
    void theSessionAsksBeforeReplacingAndHonoursTheAnswer(
            @TempDir Path folder) throws Exception {
        Path existing = Files.writeString(folder.resolve("orion.svg"),
                "a chart the reader already had");
        ChartViewController navigation = navigation();
        ChartComponent chart = chart(navigation);
        ChartOptionsController options = new ChartOptionsController(
                ChartOptionsStore.forNode(java.util.prefs.Preferences
                        .userRoot().node("juranometria-export-session-"
                                + System.nanoTime())));

        List<String> asked = new ArrayList<>();
        var refused = assertInstanceOf(ExportSheet.Outcome.Refused.class,
                ExportSheetSession.exportTo(
                        folder.resolve("orion").toFile(),
                        new ExportSheet.Request(SheetFormat.SVG,
                                PaperSize.A4, 300, false),
                        navigation, chart, options,
                        new WorkingSelection(),
                        replacing -> {
                            asked.add(replacing.getName());
                            return false;
                        }),
                "the session's own path refuses when the reader says"
                        + " no");
        assertEquals(List.of("orion.svg"), asked,
                "and asks about the file that would actually be"
                        + " replaced");
        assertTrue(refused.reason().contains("left as it was"),
                "saying so: " + refused.reason());
        assertEquals("a chart the reader already had",
                Files.readString(existing),
                "with the reader's file untouched");

        // And a yes goes through the same path to a real sheet.
        assertInstanceOf(ExportSheet.Outcome.Written.class,
                ExportSheetSession.exportTo(
                        folder.resolve("orion").toFile(),
                        new ExportSheet.Request(SheetFormat.SVG,
                                PaperSize.A4, 300, false),
                        navigation, chart, options,
                        new WorkingSelection(), replacing -> true),
                "and writes when they say yes");
        assertTrue(Files.readString(existing).startsWith("<svg"),
                "the chart the reader was looking at");
    }

    @Test
    void theDecisionTheApplicationUsesPutsTheQuestionOnAScreen() {
        // The bypass this closes is a quiet one: replacing
        // ExportSheetSession's decision with a constant yes would
        // leave every policy test passing and every reader's file
        // silently overwritten. What distinguishes a real decision
        // from a constant is that it needs a person - so, asked
        // where there is no screen, it must fail rather than answer.
        Assumptions.assumeTrue(GraphicsEnvironment.isHeadless(),
                "this asks what happens when there is no display");

        ExportSheet.ReplaceDecision decision =
                ExportSheetSession.replaceDecision(null);
        assertThrows(java.awt.HeadlessException.class,
                () -> decision.mayReplace(new File("orion.svg")),
                "the application's own decision asks a person, and"
                        + " cannot answer without one - a constant"
                        + " would have returned quietly here");
    }

    @Test
    void theOneRouteAReaderTakesUsesThatDecisionAndNotAConstant()
            throws Exception {
        // open() puts two modal dialogs on the screen, so it cannot
        // be driven from a test - which is exactly why it is the
        // place a constant could be substituted and nothing would
        // fail (PR #291 round 2). What can be checked is the wiring
        // itself, read from the source, and the bypass this is
        // guarding against is a one-line edit that this sees.
        String source = Files.readString(Path.of(
                "src/juranometria/app/ExportSheetSession.java"));
        String open = source.substring(
                source.indexOf("public static void open("),
                source.indexOf("/** Where the reader wants it"));

        assertTrue(open.contains("replaceDecision(owner)"),
                "the reader's own route asks through the decision"
                        + " that puts a question on the screen:\n"
                        + open);
        for (String constant : List.of("-> true", "->true",
                "mayReplace(existing) || true")) {
            assertTrue(!open.contains(constant),
                    "and never answers the question itself: " + constant);
        }
        assertTrue(open.contains("exportTo("),
                "through the path this class drives, so what is tested"
                        + " above is what runs");
    }

    @Test
    void theDecisionTheApplicationUsesIsNotAConstant() {
        // The same claim where there is a display, without opening a
        // modal dialog: two decisions built for different owners are
        // different objects that close over their owner, which a
        // constant would not be.
        ExportSheet.ReplaceDecision one =
                ExportSheetSession.replaceDecision(null);
        ExportSheet.ReplaceDecision two =
                ExportSheetSession.replaceDecision(null);
        assertTrue(one != two,
                "the decision is built per export rather than being"
                        + " one shared answer");
        assertTrue(ExportSheet.ReplaceDecision.REFUSE != one,
                "and it is not the never-replace default either");
    }
}
