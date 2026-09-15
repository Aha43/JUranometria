package juranometria.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The decision record states what is true now (issue #347).
 *
 * <p>A document is the one kind of evidence nothing regenerates. Its
 * figures are typed once and then quietly outlive the thing they
 * describe: the gate's own record went to review claiming 446
 * reader-visible occurrences after the drain had taken them to 749,
 * and 25 negative attachment margins after the density correction had
 * taken them to 23. Every measured figure was reproducible, and the
 * sentences around them were stale.
 *
 * <p>So the numbers a reader of the record relies on are read back
 * out of it and compared against the thing that produced them. The
 * prose is a human's to write; the figures inside it are checked.
 */
class SkyLanguageRecordTest {

    private static final Path RECORD =
            Path.of("docs/decisions/sky-language.md");

    private static final Path STUDY =
            Path.of("docs/studies/sky-language/placement.md");

    /** The audit figures, against a live scan. */
    @Test
    void theRecordStatesTheAuditItActuallyHas() throws Exception {
        String record = Files.readString(RECORD);
        List<SkyLanguageScan.Literal> literals =
                SkyLanguageScan.scan(Path.of(".")).stream()
                        .flatMap(f -> f.literals().stream()).toList();
        Map<String, SkyLanguageLedger.Entry> ledger =
                SkyLanguageLedger.read();
        List<SkyLanguageScan.Literal> queue =
                SkyLanguageLedger.readerSurface(literals, ledger);

        assertStated(record, "\\*\\*([\\d,]+) string literals\\*\\*",
                literals.size(), "the corpus it partitions");
        assertStated(record,
                "surface: ([\\d,]+) occurrences", queue.size(),
                "the reader-visible occurrences");
        assertStated(record,
                "occurrences, ([\\d,]+) unique translation",
                (int) queue.stream()
                        .map(SkyLanguageScan.Literal::text)
                        .distinct().count(),
                "the unique translation units");

        for (SkyLanguageScan.Surface surface
                : SkyLanguageScan.Surface.values()) {
            long held = literals.stream()
                    .filter(l -> l.surface() == surface).count();
            assertTrue(record.contains("| " + group(held) + " |"),
                    "the record's surface table states "
                            + surface + " as " + held);
        }
    }

    /** The confidence split, which #350 relies on. */
    @Test
    void theRecordStatesHowFirmlyEachFigureIsKnown() throws Exception {
        String record = Files.readString(RECORD);
        List<SkyLanguageScan.Literal> queue =
                SkyLanguageLedger.readerSurface(
                        SkyLanguageScan.scan(Path.of(".")).stream()
                                .flatMap(f -> f.literals().stream())
                                .toList(),
                        SkyLanguageLedger.read());

        long manual = queue.stream()
                .filter(l -> l.route().startsWith("manually reviewed"))
                .count();
        long recognised = queue.stream()
                .filter(l -> l.confidence()
                        == SkyLanguageScan.Confidence.RECOGNISED)
                .count();

        assertStated(record, "\\*\\*([\\d,]+) recognised\\*\\*",
                (int) recognised,
                "the candidates, which must not be presented as"
                        + " proven reader strings");
        assertStated(record, "\\*\\*([\\d,]+) manually reviewed\\*\\*",
                (int) manual, "the ledger's own contribution");
        assertStated(record, "\\*\\*([\\d,]+) traced\\*\\*",
                (int) (queue.size() - recognised - manual),
                "the traced remainder");
    }

    /** Figures the record quotes from the study it cites. */
    @Test
    void theRecordAgreesWithTheStudyItQuotes() throws Exception {
        String record = Files.readString(RECORD);
        String study = Files.readString(STUDY);

        Matcher measured = Pattern.compile(
                "\\*\\*Negative margins: (\\d+)\\*\\*").matcher(study);
        assertTrue(measured.find(), "the study reports its margins");
        assertStated(record, "The (\\d+) negative margins",
                Integer.parseInt(measured.group(1)),
                "the attachment margins it cites from the study");
    }

    private static void assertStated(String record, String pattern,
                                     int measured, String what) {
        Matcher stated = Pattern.compile(pattern).matcher(record);
        assertTrue(stated.find(),
                "the record states " + what + " (" + pattern + ")");
        assertEquals(measured,
                Integer.parseInt(stated.group(1).replace(",", "")),
                "the record states " + what + " as \""
                        + stated.group(1) + "\" where it is now "
                        + measured + "; a document is the one evidence"
                        + " nothing regenerates, so its figures go"
                        + " stale in silence");
    }

    /** As the record writes a number: thousands separated. */
    private static String group(long value) {
        return String.format(java.util.Locale.ROOT, "%,d", value);
    }
}
