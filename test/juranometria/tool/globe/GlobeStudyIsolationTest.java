package juranometria.tool.globe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.tool.TestEvidenceScan;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One globe study cannot change what the next one measures (Sprint
 * 32, issues #301 and #329).
 *
 * <p>It happened. The frame study asked what fraction of the page a
 * disc should fill, and the only way to ask was a JVM-wide property;
 * run alone it put the property back by exiting, and run in one
 * process with the other ten - which is how the evidence contract
 * runs them - it left the last fraction it tried, 86 per cent, set
 * for everything after it. The pointing study then reported a pixel
 * at r = 0.95 as 83.8 degrees out from the centre instead of 71.8,
 * and every study between them measured a disc four per cent too
 * small. Nothing noticed until the studies were published and the
 * contract ran them together.
 *
 * <p>The property is gone with #329, so that leak cannot recur. What
 * can recur is its shape, and the atlas already owns the rule and the
 * scanner that reads it - {@code TestEvidenceScan}, built by #224 for
 * the test corpus. This points the same scanner at the generators,
 * because a study that reaches for process-wide state is the same
 * fault wherever it lives, and a second hand-written rule here could
 * disagree with the first.
 */
class GlobeStudyIsolationTest {

    @Test
    void noGlobeStudyReachesForStateTheProcessShares() throws IOException {
        List<TestEvidenceScan.File> studies =
                TestEvidenceScan.scan(Path.of("src/juranometria/tool/globe"));
        assertEquals(List.of(),
                studies.stream()
                        .filter(file -> !file.globalState().isEmpty())
                        .map(TestEvidenceScan.File::path)
                        .toList(),
                "a globe study touches state the whole process shares,"
                        + " which is how the frame study left every"
                        + " globe drawn after it four per cent too"
                        + " small");
    }
}
