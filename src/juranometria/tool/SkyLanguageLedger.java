package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a person decided about the strings automation could not route
 * (Sprint 33, issue #347).
 *
 * <p>The classifier reads statements and does not do data flow, so a
 * word carried out of a collection, returned from a method or passed
 * through an alias cannot be followed to the call that publishes it.
 * Those occurrences are not dropped and not guessed at: they are
 * listed, reviewed, and given one of six concrete dispositions.
 *
 * <p>The ledger is held to the scanner, both ways. An unresolved
 * occurrence with no entry is a gap; an entry matching nothing is a
 * decision about code that has changed. Either is a failure, because
 * a ledger that may quietly disagree with the scanner is worth less
 * than no ledger at all.
 */
public final class SkyLanguageLedger {

    private SkyLanguageLedger() {
    }

    /** Where the reviewed dispositions live. */
    public static final Path RECORD =
            Path.of("docs/studies/sky-language/manual-review.tsv");

    /**
     * The dispositions a reviewer may give.
     *
     * <p>Six, and deliberately no {@code reviewed}, {@code accepted}
     * or {@code ignore}. A disposition that says only "somebody
     * looked" records the looking and not the conclusion, and the
     * conclusion is the whole point of looking.
     */
    public static final Set<String> DISPOSITIONS = Set.of(
            "reader-prose", "reader-technical", "notation",
            "identifier", "developer-only", "false-positive");

    /**
     * Why an occurrence is excluded from the reader's surface.
     *
     * <p>Closed, so the reason is checkable. The prose beside a code
     * is for a human reader of the ledger; its adequacy is a matter
     * for review, and no rule here claims otherwise.
     */
    public static final Map<String, Set<String>> EXCLUSION_CODES =
            Map.of(
                    // Not text at all: markup, serialisation, syntax.
                    "false-positive", Set.of("regular-expression",
                            "markup-entity", "pdf-structure",
                            "svg-attribute", "format-fragment",
                            "escape-sequence"),
                    // Text, but never on its way to a reader.
                    "developer-only", Set.of("build-output",
                            "test-fixture", "developer-log"));

    /** One reviewed occurrence. */
    public record Entry(String identity, String file, String literal,
                        String scannerReason, String disposition,
                        String why) {

        /** Whether this belongs in the translation queue. */
        public boolean readersOwn() {
            return "reader-prose".equals(disposition);
        }
    }

    /** The ledger, by identity. */
    public static Map<String, Entry> read() throws IOException {
        return read(RECORD);
    }

    /** The ledger at a given path, by identity. */
    public static Map<String, Entry> read(Path record) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (String line : Files.readAllLines(record)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\t", -1);
            if (columns.length != 6 || columns[0].equals("identity")) {
                continue;
            }
            Entry entry = new Entry(columns[0], columns[1], columns[2],
                    columns[3], columns[4], columns[5]);
            if (!DISPOSITIONS.contains(entry.disposition())) {
                throw new IllegalStateException(
                        entry.file() + " \"" + entry.literal()
                                + "\" carries the disposition \""
                                + entry.disposition()
                                + "\", which is not one of "
                                + DISPOSITIONS);
            }
            if (entry.why().isBlank()) {
                throw new IllegalStateException(
                        entry.file() + " \"" + entry.literal()
                                + "\" is disposed without saying why;"
                                + " a conclusion with no reason cannot"
                                + " be reviewed");
            }
            // The two dispositions that EXCLUDE something a reader
            // might otherwise have seen must name WHAT KIND of
            // exclusion it is, from a closed set.
            //
            // An earlier version demanded fifteen characters of
            // explanation, which was an observation-shaped bound and
            // no better than the ones this sprint keeps removing:
            // four meaningless words passed it, while the exactly
            // right answer - "SVG operator" - failed. Length cannot
            // establish substance. A code can be checked; whether
            // the sentence beside it is any good is settled by
            // review, and this says so rather than pretending the
            // machine decided.
            if ("false-positive".equals(entry.disposition())
                    || "developer-only".equals(entry.disposition())) {
                String code = entry.why().contains(":")
                        ? entry.why().substring(0,
                                entry.why().indexOf(':')).strip()
                        : entry.why().strip();
                // The code must belong to THIS disposition. A valid
                // code on the wrong exclusion still passed when the
                // set was flat: "build-output" would have excused a
                // string as a false positive, which says the string
                // is not text - a different and much stronger claim
                // than saying a reader never sees it.
                Set<String> allowed =
                        EXCLUSION_CODES.get(entry.disposition());
                if (!allowed.contains(code)) {
                    throw new IllegalStateException(
                            entry.file() + " \"" + entry.literal()
                                    + "\" is excluded as "
                                    + entry.disposition()
                                    + " under the reason \"" + code
                                    + "\", which is not one of "
                                    + allowed);
                }
            }
            entries.put(entry.identity(), entry);
        }
        return entries;
    }

    /**
     * The reader's strings, with the ledger's decisions applied.
     *
     * <p>An occurrence the scanner could not route, which a reviewer
     * has called reader prose, belongs in the queue exactly as a
     * traced one does - carrying its own surface, so a chart title
     * that reaches the screen, the accessible description and three
     * writers is still recorded as reaching all of them.
     */
    public static List<SkyLanguageScan.Literal> readerSurface(
            List<SkyLanguageScan.Literal> literals,
            Map<String, Entry> ledger) {
        List<SkyLanguageScan.Literal> queue =
                new java.util.ArrayList<>();
        for (SkyLanguageScan.Literal literal : literals) {
            if (literal.readersOwn()) {
                queue.add(literal);
                continue;
            }
            Entry reviewed = ledger.get(literal.identity());
            if (reviewed != null && reviewed.readersOwn()) {
                queue.add(new SkyLanguageScan.Literal(literal.path(),
                        literal.line(), literal.text(),
                        SkyLanguageScan.Kind.VISIBLE_PROSE,
                        "manually reviewed: " + reviewed.why(), "",
                        literal.context()));
            }
        }
        return queue;
    }

    /** Occurrences the scanner could not place, on reader surfaces. */
    public static List<SkyLanguageScan.Literal> unresolved(
            List<SkyLanguageScan.Literal> literals) {
        return literals.stream()
                .filter(l -> l.kind()
                        == SkyLanguageScan.Kind.UNCLASSIFIED)
                .filter(l -> {
                    SkyLanguageScan.Surface where = l.surface();
                    return where == SkyLanguageScan.Surface.APPLICATION
                            || where == SkyLanguageScan.Surface.EXPORT
                            || where == SkyLanguageScan.Surface
                                    .READER_REACHABLE_DIAGNOSTIC;
                })
                .toList();
    }
}
