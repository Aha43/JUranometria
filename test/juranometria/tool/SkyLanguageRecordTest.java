package juranometria.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gate's record says what the gate measured (issue #347).
 *
 * <p>Its figures are <strong>frozen at the gate boundary</strong>.
 * They describe the tree #347 evaluated - merge commit
 * {@code 8fb6789} - and are deliberately not refreshed as later work
 * adds strings. A decision record should explain what was approved;
 * one rewritten by every branch that touches the corpus stops
 * explaining anything, and #348 onwards would be editing a closed
 * document to keep a number true of a tree nobody is reviewing.
 *
 * <p>So this no longer compares the record against a live scan. It
 * did, and that was right while the gate was open - it caught the
 * record claiming 446 reader occurrences after the drain had taken
 * them to 749. Once the gate closed, the same check became a demand
 * that a historical measurement track the present, and #348's first
 * commit tripped it by adding thirteen strings.
 *
 * <p>What survives is what a frozen record can still get wrong:
 * arithmetic that does not add up, and a boundary it fails to name.
 * The live surface belongs to {@link SkyLanguageLedgerTest}, which
 * holds the scanner and the ledger to each other and is where #350's
 * translation queue is actually governed.
 */
class SkyLanguageRecordTest {

    private static final Path RECORD =
            Path.of("docs/decisions/sky-language.md");

    /** A frozen figure is only meaningful beside the tree it describes. */
    @Test
    void theRecordNamesTheTreeItsFiguresDescribe() throws Exception {
        String record = Files.readString(RECORD);

        assertTrue(record.contains("frozen at the gate boundary"),
                "the record says its figures are frozen, so a reader"
                        + " does not take them for current measurements");
        assertTrue(Pattern.compile("`[0-9a-f]{7,40}`").matcher(record)
                        .find(),
                "and names the commit they were taken on; a frozen"
                        + " number with no tree beside it is a number"
                        + " nobody can check");
    }

    /** The surface table accounts for the whole corpus. */
    @Test
    void theSurfacesSumToTheCorpusTheRecordStates() throws Exception {
        String record = Files.readString(RECORD);
        int stated = stated(record, "\\*\\*([\\d,]+) string literals\\*\\*");

        List<Integer> surfaces = new ArrayList<>();
        Matcher row = Pattern.compile(
                "\\| (?:developer tools and generators|confined"
                        + " diagnostics|application|export|reader-reachable"
                        + " diagnostics) \\| ([\\d,]+) \\|")
                .matcher(record);
        while (row.find()) {
            surfaces.add(Integer.parseInt(row.group(1).replace(",", "")));
        }

        assertEquals(5, surfaces.size(),
                "all five surfaces are tabulated: " + surfaces);
        assertEquals(stated, surfaces.stream()
                        .mapToInt(Integer::intValue).sum(),
                "and they account for the whole corpus, so a surface"
                        + " cannot be dropped from the table while the"
                        + " total stays put: " + surfaces);
    }

    /** The confidence split accounts for the whole queue. */
    @Test
    void theConfidenceSplitSumsToTheReaderSurface() throws Exception {
        String record = Files.readString(RECORD);
        int occurrences = stated(record,
                "baseline #347 established: ([\\d,]+) occurrences");
        int traced = stated(record, "\\*\\*([\\d,]+) traced\\*\\*");
        int recognised = stated(record, "\\*\\*([\\d,]+) recognised\\*\\*");
        int manual = stated(record,
                "\\*\\*([\\d,]+) manually reviewed\\*\\*");

        assertEquals(occurrences, traced + recognised + manual,
                "every reader-visible occurrence is accounted for as"
                        + " traced, recognised or manually reviewed - "
                        + traced + " + " + recognised + " + " + manual
                        + " against " + occurrences + "; a split that"
                        + " does not add up hides whichever group was"
                        + " forgotten");
        assertTrue(recognised > 0,
                "and the recognised group is real, so the candidate"
                        + " label #350 relies on means something");
    }

    /** Units cannot exceed occurrences. */
    @Test
    void thereAreNoMoreTranslationUnitsThanOccurrences() throws Exception {
        String record = Files.readString(RECORD);
        int occurrences = stated(record,
                "baseline #347 established: ([\\d,]+) occurrences");
        int units = stated(record,
                "occurrences, ([\\d,]+)\\s+unique translation");

        assertTrue(units <= occurrences,
                units + " unique translation units cannot exceed "
                        + occurrences + " occurrences; repetition"
                        + " collapses units, it cannot create them");
        assertTrue(units > 0, "and the queue is not empty");
    }

    private static int stated(String record, String pattern) {
        Matcher found = Pattern.compile(pattern).matcher(record);
        assertTrue(found.find(),
                "the record states this figure (" + pattern + ")");
        return Integer.parseInt(found.group(1).replace(",", ""));
    }
}
