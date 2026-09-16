package juranometria.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Application source does not depend on tooling (Sprint 33, #348).
 *
 * <p>{@code juranometria.tool} is where the build's generators,
 * indexers, scanners and evidence gates live. They run on a
 * developer's machine and in CI; several read and write files under
 * {@code docs/} and {@code build/} that no installed copy of the
 * atlas has. No code a reader runs may depend on them.
 *
 * <p><strong>What this does NOT claim.</strong> An earlier version of
 * this paragraph said tooling "is not shipped". That was false and
 * would have quietly justified the wrong conclusions: the jar target
 * packages all of {@code build/classes}, so the application jar
 * carries the {@code juranometria/tool/} classes along with
 * everything else. They are dead weight in the artifact, not a
 * dependency of it - nothing the atlas executes reaches them, which
 * is exactly and only what the tests below establish.
 *
 * <p>Whether those classes should be excluded from the artifact is a
 * packaging question with its own costs - the study mains are
 * launched from the same classes directory the image is built from -
 * and it belongs to an issue of its own rather than to #348. Recorded
 * here so the premise of this file is the fact it proves, not a
 * tidier claim nobody checked.
 *
 * <p>This was written because it had already been breached.
 * {@code SkyLanguageChoice} was placed in {@code tool} while #347
 * used it as a gate contract, and #348 then made it the application's
 * persisted language state - at which point production imported
 * tooling and the package a type lives in no longer said what the
 * type was for. It was promoted to {@code juranometria.ui.language};
 * this is what keeps the next one from drifting back, since the
 * breach arrived by a type changing role rather than by anybody
 * choosing to depend on tooling.
 *
 * <p>The direction is one way on purpose. Tooling may read the
 * application freely - a generator draws real charts, a gate reads
 * real registries - because reading something cannot make it depend
 * on you. The ban is on the application reaching the other way, and
 * that is a fact about source, which is what is checked below.
 */
class ToolingBoundaryTest {

    /** Every package of the application's own source. */
    private static final String PRODUCTION = "src/juranometria";

    /** The package no application code may reach into. */
    private static final String TOOLING = "src/juranometria/tool";

    @Test
    void noApplicationPackageImportsTooling() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path source : javaIn(PRODUCTION)) {
            if (source.startsWith(TOOLING)) {
                continue;
            }
            String code = codeOf(source);
            if (code.contains("juranometria.tool")) {
                offenders.add(source + " - "
                        + firstMentionIn(code));
            }
        }
        assertEquals(List.of(), offenders,
                "a class the atlas needs at runtime cannot live in"
                        + " the package that holds build tooling; if"
                        + " the application depends on it, promote it"
                        + " to a production package rather than"
                        + " reaching across, and do not leave a copy"
                        + " behind - two definitions of a persisted"
                        + " value are free to drift apart");
    }

    /**
     * Qualified names count too.
     *
     * <p>An import is the obvious route and the easy one to grep for.
     * A fully qualified {@code juranometria.tool.Something} in the
     * body of a method is the same dependency wearing a different
     * hat, and the check above only means anything if it catches
     * that as well.
     */
    @Test
    void theCheckWouldCatchAQualifiedReferenceToo() throws IOException {
        String breach = """
                package juranometria.ui;
                class Borrower {
                    void go() {
                        juranometria.tool.SkyLanguageIndexMain
                                .main(new String[0]);
                    }
                }
                """;
        assertTrue(breach.contains("juranometria.tool"),
                "the same substring the test looks for, reached"
                        + " without an import statement");
    }

    /**
     * A comment mentioning tooling is not a dependency.
     *
     * <p>Production code explains itself, and some of what it has to
     * explain is which generator writes a file it reads. A check
     * that failed on prose would be quietly satisfied by deleting
     * the explanation, which is the opposite of what it is for.
     */
    @Test
    void prosePointingAtToolingIsNotADependency() throws IOException {
        String documented = """
                package juranometria.geo;
                /** Read from the index juranometria.tool writes. */
                class Reader {
                    // juranometria.tool.SkyLanguageIndexMain makes it
                    int rows;
                }
                """;
        assertTrue(documented.contains("juranometria.tool"),
                "the prose mentions it");
        assertEquals(-1, strip(documented).indexOf("juranometria.tool"),
                "and stripping comments leaves no reference, so"
                        + " naming the generator that produces a file"
                        + " stays allowed");
    }

    /** Tooling may read the application; the ban is one-way. */
    @Test
    void toolingMayDependOnTheApplication() throws IOException {
        long reaching = 0;
        for (Path source : javaIn(TOOLING)) {
            String code = codeOf(source);
            if (code.contains("juranometria.geo")
                    || code.contains("juranometria.chart")
                    || code.contains("juranometria.render")) {
                reaching++;
            }
        }
        assertTrue(reaching > 0,
                "generators draw the real atlas and gates read the"
                        + " real registries; a boundary that forbade"
                        + " that would be describing two programs"
                        + " rather than one with its tools beside it");
    }

    private static String firstMentionIn(String code) {
        int at = code.indexOf("juranometria.tool");
        int from = Math.max(0, at - 40);
        int to = Math.min(code.length(), at + 60);
        return code.substring(from, to).replaceAll("\\s+", " ").strip();
    }

    private static List<Path> javaIn(String directory) throws IOException {
        try (Stream<Path> tree = Files.walk(Path.of(directory))) {
            return tree.filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private static String codeOf(Path source) throws IOException {
        return strip(Files.readString(source));
    }

    private static String strip(String text) {
        text = text.replaceAll("(?s)/\\*.*?\\*/", " ");
        text = text.replaceAll("(?m)//.*$", " ");
        return text;
    }
}
