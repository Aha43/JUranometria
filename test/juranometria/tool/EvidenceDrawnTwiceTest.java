package juranometria.tool;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That the portable contract really draws every rendering twice
 * (Sprint 31, issue #315; found in review of PR #322).
 *
 * <p>The portable contract's whole replacement for cross-platform
 * byte equality is <em>same-environment reproduction</em>: the runner
 * draws each page twice and the bytes must match. A first version of
 * that ran the report generators, captured what they had drawn, and
 * then re-ran only the image generators - so a page drawn by a report
 * generator was compared with itself and reported as reproduced. A
 * nondeterministic report image would have passed while the run
 * claimed every rendering had been drawn twice.
 *
 * <p>So the mechanism takes its generators as an argument, and this
 * hands it one that draws something different every time. If the
 * contract ever stops running a generator on both sides of its
 * snapshot, this fails.
 */
class EvidenceDrawnTwiceTest {

    /** Where the stand-in generators draw. */
    static final Path FLICKERING =
            Path.of("build/evidence-twice/flickering.png");
    static final Path STEADY =
            Path.of("build/evidence-twice/steady.png");

    /** A generator that draws a different page every time it runs. */
    public static final class Flickering {

        private static int drawn;

        private Flickering() {
        }

        public static void main(String[] args) throws Exception {
            write(FLICKERING, ++drawn);
        }
    }

    /** A generator that draws the same page every time. */
    public static final class Steady {

        private Steady() {
        }

        public static void main(String[] args) throws Exception {
            write(STEADY, 7);
        }
    }

    private static void write(Path where, int grey) throws Exception {
        Files.createDirectories(where.getParent());
        BufferedImage page = new BufferedImage(4, 4,
                BufferedImage.TYPE_INT_RGB);
        page.setRGB(0, 0, grey << 16 | grey << 8 | grey);
        ImageIO.write(page, "png", new File(where.toString()));
    }

    @Test
    void aGeneratorThatDrawsSomethingElseEachTimeIsCaught()
            throws Exception {
        EvidenceContractMain.DrawnTwice twice =
                EvidenceContractMain.drawTwice(
                        List.of(Flickering.class.getName(),
                                Steady.class.getName()),
                        List.of(FLICKERING.toString(),
                                STEADY.toString()));

        assertEquals(List.of(FLICKERING.toString()), twice.differing(),
                "a generator whose page changes between two runs on"
                        + " one machine is exactly what the portable"
                        + " contract exists to catch, and the only"
                        + " thing it may report as differing");
        assertTrue(twice.claimed().contains(STEADY.toString()),
                "the steady one was drawn on both passes, so it is a"
                        + " rendering this run may speak for");
        assertTrue(twice.claimed().contains(FLICKERING.toString()),
                "and so was the flickering one");
        assertTrue(twice.first().containsKey(STEADY.toString()),
                "the first drawing is kept, because the comparison is"
                        + " against it and not against a committed"
                        + " file from another machine");
    }

    @Test
    void aRenderingNobodyDrewIsNotReportedAsDrawnTwice()
            throws Exception {
        // The other half of the review's finding. A file that no
        // generator touched still exists on disk and still equals
        // itself; reporting it as "reproduced" would be the contract
        // claiming a check it never made.
        Files.createDirectories(STEADY.getParent());
        Path untouched = STEADY.getParent().resolve("untouched.png");
        write(untouched, 3);

        EvidenceContractMain.DrawnTwice twice =
                EvidenceContractMain.drawTwice(
                        List.of(Steady.class.getName()),
                        List.of(STEADY.toString(),
                                untouched.toString()));

        assertEquals(List.of(), twice.differing(),
                "nothing differs: one was drawn twice and the other"
                        + " was not drawn at all");
        assertTrue(twice.claimed().contains(STEADY.toString()),
                "the drawn one is claimed");
        assertFalse(twice.claimed().contains(untouched.toString()),
                "and the untouched one is not, so no verdict speaks"
                        + " for it");
    }
}
