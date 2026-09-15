package juranometria.tool;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The manual ledger closes the automation boundary (issue #347).
 *
 * <p>The classifier cannot follow a word through a collection, a
 * return value or an alias. That limit is real and is not going
 * away, so what matters is that nothing falls through it silently: a
 * string automation cannot route must arrive in a ledger, be given a
 * concrete disposition, and - if a person judges it reader prose -
 * reach the translation queue exactly as a traced string would.
 *
 * <p>Held in both directions. A missing entry is a gap; a stale
 * entry is a decision about code that has since changed. A ledger
 * allowed to disagree with the scanner is worth less than no ledger,
 * because it looks like coverage.
 */
class SkyLanguageLedgerTest {

    /** The ledger accounts for every unresolved occurrence, exactly. */
    @Test
    void theLedgerAndTheScannerAgreeOnWhatIsUnresolved() throws Exception {
        List<SkyLanguageScan.Literal> literals = production();
        Set<String> unresolved = new TreeSet<>();
        for (SkyLanguageScan.Literal one
                : SkyLanguageLedger.unresolved(literals)) {
            unresolved.add(one.identity());
        }
        Set<String> reviewed =
                new TreeSet<>(SkyLanguageLedger.read().keySet());

        assertEquals(unresolved, reviewed,
                "every unresolved application-surface occurrence is"
                        + " reviewed, and every review still matches"
                        + " code that exists");
        assertTrue(unresolved.size() < 150,
                "and the boundary stays small enough to read: "
                        + unresolved.size());
    }

    /** No disposition may be a shrug. */
    @Test
    void everyDispositionIsAConclusionRatherThanAnAcknowledgement()
            throws Exception {
        Map<String, SkyLanguageLedger.Entry> ledger =
                SkyLanguageLedger.read();
        assertTrue(!ledger.isEmpty(), "the ledger has entries");
        for (SkyLanguageLedger.Entry entry : ledger.values()) {
            assertTrue(SkyLanguageLedger.DISPOSITIONS
                            .contains(entry.disposition()),
                    entry.file() + " \"" + entry.literal()
                            + "\" is disposed as \""
                            + entry.disposition() + "\"");
            assertTrue(!entry.why().isBlank(),
                    "and says why: " + entry.literal());
        }
    }

    /**
     * A reviewed escape must not vanish from BOTH ledgers.
     *
     * <p>The mutation the boundary exists to survive, run on
     * synthetic input so it tests the rules rather than today's
     * source. A known reader string is driven through each of the
     * four shapes the scanner cannot route. In every case it must
     * either be routed, or arrive unresolved where the ledger will
     * demand a decision - never be classified into silence.
     */
    @Test
    void aVisibleStringInEachUntraceableShapeStillSurfaces() {
        String visible = "This sentence is read by a person";
        Map<String, String> shapes = new LinkedHashMap<>();
        shapes.put("collection element", """
                class Fixture {
                    static final String[] MANY = {"%s"};
                    void build() {
                        for (String one : MANY) {
                            add(new JLabel(one));
                        }
                    }
                }
                """);
        shapes.put("returned value", """
                class Fixture {
                    static final String GIVEN = "%s";
                    void build() {
                        add(new JLabel(heading()));
                    }
                    String heading() {
                        return GIVEN;
                    }
                }
                """);
        shapes.put("multi-step alias", """
                class Fixture {
                    static final String FIRST = "%s";
                    void build() {
                        String second = FIRST;
                        String third = second;
                        add(new JLabel(third));
                    }
                }
                """);
        shapes.put("declared but never at a sink", """
                class Fixture {
                    static final String HELD = "%s";
                    void build() {
                        record(HELD);
                    }
                }
                """);

        for (Map.Entry<String, String> shape : shapes.entrySet()) {
            List<SkyLanguageScan.Literal> found = SkyLanguageScan
                    .classify("juranometria/app/Fixture.java",
                            List.of(String.format(shape.getValue(), visible)
                                    .split("\n")))
                    .literals();
            SkyLanguageScan.Literal literal = found.stream()
                    .filter(l -> l.text().equals(visible))
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "the scan lost the string entirely in the "
                                    + shape.getKey() + " shape: "
                                    + found));

            boolean accounted = literal.readersOwn()
                    || literal.kind()
                            == SkyLanguageScan.Kind.UNCLASSIFIED;
            assertTrue(accounted,
                    "in the " + shape.getKey() + " shape the string is"
                            + " either routed to the reader or left"
                            + " unresolved for review, never quietly"
                            + " filed as something else: " + literal);
        }
    }

    /**
     * A ledger entry disposed as reader prose reaches the queue.
     *
     * <p>The other half. Without it the ledger could be a place
     * where decisions are recorded and then ignored - which reads
     * exactly like a ledger that works.
     */
    @Test
    void reviewedProseEntersTheTranslationQueueCarryingItsSurface() {
        SkyLanguageScan.File fixture = SkyLanguageScan.classify(
                "juranometria/app/Fixture.java", List.of("""
                        class Fixture {
                            static final String[] MANY = {"A sentence a reader reads"};
                            void build() {
                                for (String one : MANY) {
                                    add(new JLabel(one));
                                }
                            }
                        }
                        """.split("\n")));
        SkyLanguageScan.Literal escaped = fixture.literals().stream()
                .filter(l -> l.text().startsWith("A sentence"))
                .findFirst().orElseThrow();

        // Before review: unresolved, and outside the queue.
        assertEquals(SkyLanguageScan.Kind.UNCLASSIFIED, escaped.kind(),
                "the collection hides it from the rules");
        assertEquals(List.of(), SkyLanguageLedger.readerSurface(
                        List.of(escaped), Map.of()),
                "so with no ledger entry it is not in the queue");

        // After review: in the queue, carrying where it is published.
        Map<String, SkyLanguageLedger.Entry> ledger = Map.of(
                escaped.identity(), new SkyLanguageLedger.Entry(
                        escaped.identity(), escaped.path(),
                        escaped.text(), escaped.reason(), "reader-prose",
                        "shown on the About page"));
        List<SkyLanguageScan.Literal> queue =
                SkyLanguageLedger.readerSurface(List.of(escaped), ledger);

        assertEquals(1, queue.size(),
                "the reviewed decision puts it in the queue");
        assertEquals(SkyLanguageScan.Surface.APPLICATION,
                queue.get(0).surface(),
                "and it still records where a reader meets it");
        assertTrue(queue.get(0).route().startsWith("manually reviewed"),
                "while saying it was a person and not a rule that"
                        + " decided: " + queue.get(0).route());
    }

    private static List<SkyLanguageScan.Literal> production()
            throws Exception {
        return SkyLanguageScan.scan(Path.of(".")).stream()
                .flatMap(f -> f.literals().stream()).toList();
    }
}
