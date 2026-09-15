package juranometria.tool;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the string audit can see, and what it cannot (issue #347).
 *
 * <p>The audit's first run found 86 reader-visible strings and
 * reported 1,517 it could not place. Both numbers were true and the
 * first was useless: a rule set that knew only {@code setText} would
 * have reported a confident, complete-looking, wrong answer, and a
 * localisation sized from it would have missed most of the
 * application. So the rules are calibrated here, against source
 * written to defeat them.
 *
 * <p>Every fixture below is <strong>synthetic input handed to the
 * scanner</strong>, never a file under {@code src/}. A calibration
 * file living in the production tree would be walked by the audit
 * itself, and the corpus would start counting its own measuring
 * equipment - reporting reader-visible strings no reader can ever
 * see.
 *
 * <p>Three results are pinned, and the third matters most. A scanner
 * that silently missed the routes it cannot follow would report a
 * clean sheet and hand #350 an incomplete translation queue; one
 * that guessed would hand it a wrong one. Each unresolved literal
 * must therefore come back with a named reason a reviewer can act
 * on.
 */
class SkyLanguageScanTest {

    /** Literals found at the call that publishes them. */
    @Test
    void wordsHandedStraightToAReaderAreFound() {
        List<SkyLanguageScan.Literal> found = literals("""
                class Fixture {
                    void build() {
                        add(new JLabel("a plain constructor"));
                        add(new javax.swing.JToggleButton("a qualified one"));
                        panel.setToolTipText("a plain setter");
                        menuItem("a factory this repository writes");
                        model.getColumnName("a table heading");
                    }
                }
                """);

        assertEquals(List.of("a plain constructor", "a qualified one",
                        "a plain setter",
                        "a factory this repository writes",
                        "a table heading"),
                textsOf(found, SkyLanguageScan.Kind.VISIBLE_PROSE),
                "every reader-visible route is seen, including the"
                        + " qualified constructor a literal name match"
                        + " missed: " + found);
    }

    /** Words split across lines keep the call that publishes them. */
    @Test
    void wordsSpreadOverSeveralLinesKeepTheirRoute() {
        // The shape this application actually writes: the call on one
        // line and the words two lines below. Judged line by line,
        // the words are anonymous and the route is lost.
        List<SkyLanguageScan.Literal> found = literals("""
                class Fixture {
                    void build() {
                        chart.getAccessibleContext()
                                .setAccessibleDescription(
                                        "one hemisphere of the sky,"
                                                + " outside the circle"
                                                + " is paper");
                    }
                }
                """);

        assertEquals(3, found.stream()
                        .filter(l -> l.kind()
                                == SkyLanguageScan.Kind.ACCESSIBILITY)
                        .count(),
                "all three fragments belong to the spoken description,"
                        + " not only the one sharing a line with the"
                        + " call: " + found);
    }

    /** Words in a constant, published by name at a known sink. */
    @Test
    void wordsHeldInAConstantAreFoundAtTheirDeclaration() {
        List<SkyLanguageScan.Literal> found = literals("""
                class Fixture {
                    static final String DESCRIPTION =
                            "a quiet atlas for learning the sky";
                    void build() {
                        add(new JLabel(DESCRIPTION));
                    }
                }
                """);

        assertEquals(List.of("a quiet atlas for learning the sky"),
                textsOf(found, SkyLanguageScan.Kind.VISIBLE_PROSE),
                "the word is the reader's where it is written, not"
                        + " only where it is shown: " + found);
    }

    /**
     * Words the scan cannot follow come back named.
     *
     * <p>This is the test that keeps the audit honest. Each of these
     * reaches a reader in the real application - the About dialog
     * publishes its notice titles exactly this way - and none can be
     * traced by reading one statement at a time. The requirement is
     * not that they be found. It is that they be <em>reported</em>,
     * with a reason precise enough to send a reviewer to the right
     * place.
     */
    @Test
    void wordsBeyondTheScanComeBackWithAReasonRatherThanSilence() {
        List<SkyLanguageScan.Literal> found = literals("""
                class Fixture {
                    static final String[][] NOTICES = {
                            {"a title read out of an array", "/res/a.md"},
                    };
                    static final String RETURNED = "a word given back";
                    static final String ALIASED = "a word passed along";
                    void build() {
                        for (String[] notice : NOTICES) {
                            add(new JLabel(notice[0]));
                        }
                        add(new JLabel(heading()));
                        String other = ALIASED;
                        add(new JLabel(other));
                    }
                    String heading() {
                        return RETURNED;
                    }
                }
                """);

        SkyLanguageScan.Literal array = one(found, "a title read out of an array");
        assertEquals(SkyLanguageScan.Kind.UNCLASSIFIED, array.kind(),
                "an array element is not claimed as understood");
        assertEquals(SkyLanguageScan.Unresolved.ALIAS_FLOW, array.reason(),
                "and the reason sends a reviewer to the loop that"
                        + " reads it: " + array);

        for (String beyond : List.of("a word given back",
                "a word passed along")) {
            SkyLanguageScan.Literal literal = one(found, beyond);
            assertEquals(SkyLanguageScan.Kind.UNCLASSIFIED, literal.kind(),
                    beyond + " travels through a return or an alias,"
                            + " which this scan does not follow");
            assertTrue(!literal.reason().isBlank(),
                    "and says so rather than vanishing: " + literal);
        }
    }

    /**
     * The rules do not claim words that are not the reader's.
     *
     * <p>The other direction, and the one that decides whether the
     * total means anything: an acceptance assertion and a preference
     * key must not be counted as text to translate. Counting
     * {@code require(...)} as reader prose made the application look
     * three times more localised than it is.
     */
    @Test
    void developerWordsAndIdentifiersAreNotCountedAsTheReadersText() {
        List<SkyLanguageScan.Literal> found = literals("""
                class Fixture {
                    static final String KEY = "chart.equatorialGrid";
                    void check() {
                        require(summary.contains("CC BY"),
                                "About summary states the licence");
                        throw new IllegalStateException(
                                "the inspector needs a selection model");
                    }
                }
                """);

        assertEquals(List.of(), textsOf(found,
                        SkyLanguageScan.Kind.VISIBLE_PROSE),
                "no developer sentence is offered for translation: "
                        + found);
        // Three, not two: the text an assertion SEARCHES FOR is as
        // developer-facing as the message it prints when the search
        // fails. Expecting two here was this test's own miscount,
        // and the fixture caught it - a probe string offered for
        // translation would have a translator rewriting the very
        // words an acceptance check greps for.
        assertEquals(3, found.stream()
                        .filter(l -> l.kind()
                                == SkyLanguageScan.Kind.DIAGNOSTIC)
                        .count(),
                "the acceptance message, the text it searches for, and"
                        + " the exception are all developer-facing: "
                        + found);
        assertEquals(SkyLanguageScan.Kind.IDENTIFIER,
                one(found, "chart.equatorialGrid").kind(),
                "and a preference key is an identifier, which no"
                        + " language changes");
    }

    private static List<SkyLanguageScan.Literal> literals(String source) {
        return SkyLanguageScan.classify("Fixture.java",
                List.of(source.split("\n"))).literals();
    }

    private static List<String> textsOf(List<SkyLanguageScan.Literal> found,
                                        SkyLanguageScan.Kind kind) {
        return found.stream().filter(l -> l.kind() == kind)
                .map(SkyLanguageScan.Literal::text).toList();
    }

    private static SkyLanguageScan.Literal one(
            List<SkyLanguageScan.Literal> found, String text) {
        return found.stream().filter(l -> l.text().equals(text))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "the scan did not report \"" + text + "\" at all,"
                                + " which is worse than misclassifying"
                                + " it: " + found));
    }
}
