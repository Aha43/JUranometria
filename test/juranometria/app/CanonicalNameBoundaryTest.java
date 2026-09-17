package juranometria.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A family's identity is not its name (Sprint 33, issue #350).
 *
 * <p>{@code SymbolFamily.canonicalName()} returns a {@code String}
 * that reads like a label, because it is one: "Galaxies". It exists
 * for evidence reports, diagnostics and canonical comparison, and it
 * is in English because those readers are developers.
 *
 * <p>Which makes it exactly the kind of value that drifts back onto a
 * screen. The enum was stripped of {@code label()},
 * {@code description()} and {@code prose()} so that the words a
 * reader sees come from the interface-language layer - and a future
 * caller writing {@code setText(family.canonicalName())} would undo
 * that silently, in one line, with a compiler that has no opinion.
 *
 * <p>So the ban is on the <em>sink</em>, not the value. Tooling may
 * print it, tests may compare it, serialisation may store it; what
 * reader-facing production code may not do is put it somewhere a
 * person reads.
 */
class CanonicalNameBoundaryTest {

    /** Where the application's own reader-facing surfaces live. */
    private static final List<String> READER_FACING = List.of(
            "src/juranometria/ui", "src/juranometria/app",
            "src/juranometria/page");

    /**
     * Sinks that put a string in front of a person.
     *
     * <p>Visible text and the accessible channel both count: a name
     * spoken to a screen reader in the wrong language is as wrong as
     * one drawn in it, and is the half that goes unnoticed longer.
     */
    private static final Pattern SINK = Pattern.compile(
            "(setText|setToolTipText|setAccessibleName"
                    + "|setAccessibleDescription|setTitle|addTab"
                    + "|new JLabel|new JCheckBox|new JButton"
                    + "|new JRadioButton|Explain\\.control"
                    + "|Explain\\.selfExplanatory|Explain\\.dynamic)"
                    + "\\s*\\([^;]{0,400}?canonicalName\\s*\\(\\)",
            Pattern.DOTALL);

    @Test
    void noReaderFacingCodeShowsACanonicalName() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String directory : READER_FACING) {
            for (Path source : javaIn(directory)) {
                String code = codeOf(source);
                Matcher found = SINK.matcher(code);
                while (found.find()) {
                    offenders.add(source + ": "
                            + found.group().replaceAll("\\s+", " ")
                                    .substring(0, Math.min(90,
                                            found.group().length())));
                }
            }
        }
        assertEquals(List.of(), offenders,
                "a family's canonical name is identity, not a word a"
                        + " reader reads. It is English on purpose,"
                        + " for reports a developer reads; showing it"
                        + " would put an untranslated label back on a"
                        + " screen one line at a time. Ask"
                        + " SymbolFamilyText in the reader's interface"
                        + " language instead");
    }

    /**
     * The check would catch the breach it names.
     *
     * <p>Proven on a fixture rather than trusted, because a pattern
     * that matched nothing would pass this file for ever and read
     * exactly like a boundary being kept.
     */
    @Test
    void theCheckWouldCatchADisplayedCanonicalName() {
        assertTrue(SINK.matcher(
                        "label.setText(family.canonicalName());").find(),
                "a canonical name sent to visible text is caught");
        assertTrue(SINK.matcher("box.getAccessibleContext()"
                                + ".setAccessibleName(family.canonicalName());")
                        .find(),
                "and one sent to the spoken channel, which is where it"
                        + " would go unnoticed longest");
        assertTrue(SINK.matcher("tabs.addTab(family.canonicalName(),"
                        + " panel);").find(),
                "and a tab title");
    }

    /** Identity uses stay allowed, and are not mistaken for display. */
    @Test
    void comparisonAndDiagnosticsAreNotDisplay() {
        assertTrue(!SINK.matcher(
                        "if (mapped.contains(family.canonicalName()))")
                        .find(),
                "comparing identity is what a canonical name is for");
        assertTrue(!SINK.matcher("System.out.printf(\"%s%n\","
                        + " family.canonicalName());").find(),
                "and so is naming a family in a report a developer"
                        + " reads");
        assertTrue(!SINK.matcher(
                        "fail(family.canonicalName() + \" drew wrong\");")
                        .find(),
                "and a diagnostic message about a rendering fault");
    }

    private static List<Path> javaIn(String directory) throws IOException {
        try (Stream<Path> tree = Files.walk(Path.of(directory))) {
            return tree.filter(p -> p.toString().endsWith(".java"))
                    .sorted().toList();
        }
    }

    private static String codeOf(Path source) throws IOException {
        String text = Files.readString(source);
        text = text.replaceAll("(?s)/\\*.*?\\*/", " ");
        return text.replaceAll("(?m)//.*$", " ");
    }
}
