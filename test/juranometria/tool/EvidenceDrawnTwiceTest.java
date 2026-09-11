package juranometria.tool;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    /** Where the directory-watching stand-in draws. */
    static final Path BUILT =
            Path.of("build/evidence-twice-built/drawn.png");

    /** A generator that draws into a watched build directory. */
    public static final class Builder {

        private Builder() {
        }

        public static void main(String[] args) throws Exception {
            write(BUILT, 5);
        }
    }

    @Test
    void aStalePageLeftInABuildDirectoryIsNotCreditedAsDrawnTwice()
            throws Exception {
        // The review's finding on PR #328. The committed half asks
        // whether a generator wrote a file; the build half did not,
        // and simply read whatever was lying in the directory. So a
        // page left by an earlier run - by a generator since gated
        // off, renamed, or not run today - was compared with itself,
        // found equal, and counted as a rendering this run had drawn
        // twice. Two passes over one stale file is not two drawings.
        Files.createDirectories(BUILT.getParent());
        Path stale = BUILT.getParent().resolve("stale.png");
        write(stale, 2);
        // Dated a week ago, as a leftover would be: the check must
        // rest on nobody having written it, not on it being new.
        Files.setLastModifiedTime(stale,
                java.nio.file.attribute.FileTime.fromMillis(
                        1_600_000_000_000L));
        byte[] before = Files.readAllBytes(stale);

        EvidenceContractMain.DrawnTwice twice =
                EvidenceContractMain.drawTwice(
                        List.of(Builder.class.getName()),
                        List.of(),
                        List.of(BUILT.getParent().toString()));

        assertTrue(twice.claimed().contains(BUILT.toString()),
                "the page the generator actually drew in both passes"
                        + " is claimed");
        assertFalse(twice.claimed().contains(stale.toString()),
                "the page nothing drew is not claimed - a file that"
                        + " equals itself across two passes has been"
                        + " read twice, not drawn twice");
        assertEquals(List.of(), twice.differing(),
                "and nothing differs: one was drawn twice, the other"
                        + " was not drawn at all");
        assertArrayEquals(before, Files.readAllBytes(stale),
                "the stale page is left exactly as it was found;"
                        + " asking about it is not permission to"
                        + " change it");
        assertEquals(1_600_000_000_000L,
                Files.getLastModifiedTime(stale).toMillis(),
                "including the date it arrived with, so the next run"
                        + " asks the same question rather than finding"
                        + " an epoch stamp this one left behind");
    }

    /** Where the generator that gives up after one page draws. */
    static final Path INTERMITTENT =
            Path.of("build/evidence-twice/intermittent.png");

    /** A generator that draws on its first run and then stops. */
    public static final class Intermittent {

        private static boolean drawn;

        private Intermittent() {
        }

        public static void main(String[] args) throws Exception {
            if (drawn) {
                return;
            }
            drawn = true;
            write(INTERMITTENT, 6);
        }
    }

    @Test
    void aRenderingDrawnInOnlyOneOfTheTwoPassesIsABreach()
            throws Exception {
        // The second review finding on PR #328. "Written in one pass
        // and not the other" was folded in with "written in neither",
        // so a generator that drew a page once and then stopped was
        // reported as residue - one line in a count of renderings
        // held by their own class - and the run passed. A rendering
        // that comes and goes is not evidence, and calling it residue
        // says the opposite of what happened.
        Files.deleteIfExists(INTERMITTENT);

        EvidenceContractMain.DrawnTwice twice =
                EvidenceContractMain.drawTwice(
                        List.of(Intermittent.class.getName(),
                                Steady.class.getName()),
                        List.of(INTERMITTENT.toString(),
                                STEADY.toString()));

        assertEquals(List.of(INTERMITTENT.toString()),
                twice.intermittent(),
                "a page drawn in one pass and not the other is named"
                        + " as exactly that, and it is the only one");
        assertFalse(twice.claimed().contains(INTERMITTENT.toString()),
                "it is not claimed: this run cannot say it was drawn"
                        + " twice, because it was not");
        assertEquals(List.of(), twice.differing(),
                "and it is not a byte difference either - nothing was"
                        + " compared, because there was nothing to"
                        + " compare it with");
        assertTrue(twice.claimed().contains(STEADY.toString()),
                "the generator beside it that drew both times is"
                        + " still claimed, so the breach is about the"
                        + " one page and not the pass");
    }

    /** Where the record-writing stand-ins write. */
    static final Path RECORD =
            Path.of("build/evidence-twice/platform.md");
    static final Path SOMETIMES =
            Path.of("build/evidence-twice/sometimes.md");

    /** A study that writes its platform record every time. */
    public static final class Recorder {

        private Recorder() {
        }

        public static void main(String[] args) throws Exception {
            Files.createDirectories(RECORD.getParent());
            Files.writeString(RECORD, "Recorded on: `here`\n");
        }
    }

    /** A study that writes its record once and then stops. */
    public static final class SometimesRecorder {

        private static boolean written;

        private SometimesRecorder() {
        }

        public static void main(String[] args) throws Exception {
            if (written) {
                return;
            }
            written = true;
            Files.createDirectories(SOMETIMES.getParent());
            Files.writeString(SOMETIMES, "Recorded on: `here`\n");
        }
    }

    @Test
    void aPlatformRecordNobodyWroteIsNotCreditedAsReproducing()
            throws Exception {
        // The third ownership gap, found in review of PR #328 and
        // introduced by it: the platform records were read after each
        // pass and never asked whether anybody had written them. A
        // record left over from an earlier run - or one whose study
        // has stopped writing it - was read twice, compared with
        // itself, found equal, and credited as reproducing here.
        Files.createDirectories(RECORD.getParent());
        Path orphan = RECORD.getParent().resolve("orphan.md");
        Files.writeString(orphan, "Recorded on: `somewhere else`\n");
        Files.setLastModifiedTime(orphan,
                java.nio.file.attribute.FileTime.fromMillis(
                        1_600_000_000_000L));

        EvidenceContractMain.DrawnTwice twice =
                EvidenceContractMain.drawTwice(
                        List.of(Recorder.class.getName()),
                        List.of(), List.of(),
                        List.of(RECORD.toString(),
                                orphan.toString()));

        assertEquals(List.of(orphan.toString()),
                twice.recordUnwritten(),
                "the record nothing wrote is named as unwritten, and"
                        + " it is the only one");
        assertEquals(List.of(), twice.recordDiffering(),
                "it is not a byte difference: nothing was compared,"
                        + " because nothing was written");
        assertTrue(twice.recordFirst().containsKey(RECORD.toString()),
                "the record its study really did write is kept, to be"
                        + " compared against the second writing");
        assertEquals(1_600_000_000_000L,
                Files.getLastModifiedTime(orphan).toMillis(),
                "and the orphan keeps the date it arrived with");
    }

    @Test
    void aPlatformRecordWrittenInOnlyOnePassIsABreach()
            throws Exception {
        Files.deleteIfExists(SOMETIMES);

        EvidenceContractMain.DrawnTwice twice =
                EvidenceContractMain.drawTwice(
                        List.of(SometimesRecorder.class.getName(),
                                Recorder.class.getName()),
                        List.of(), List.of(),
                        List.of(SOMETIMES.toString(),
                                RECORD.toString()));

        assertEquals(List.of(SOMETIMES.toString()),
                twice.recordIntermittent(),
                "a record written in one pass and not the other is"
                        + " named as exactly that");
        assertEquals(List.of(), twice.recordUnwritten(),
                "it is not unwritten - something did write it, once,"
                        + " which is the whole problem");
        assertEquals(List.of(), twice.recordDiffering(),
                "and it is not a byte difference either");
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
