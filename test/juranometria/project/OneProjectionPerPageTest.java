package juranometria.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A page is drawn by one projection, and the atlas works it out once
 * (Sprint 32, issue #301).
 *
 * <p>It had worked it out <strong>thirteen times</strong>. A viewport
 * named a projection kind, the kind was a function of the field, so
 * every consumer that wanted a projection asked the viewport again -
 * the grid, the labels, the modules, the working marks, the page
 * extent, the inventory, the plane-to-pixel mapping, the renderer's
 * own three passes. Thirteen copies of one derivation, each correct,
 * and nobody had to keep them agreeing because they could not
 * disagree.
 *
 * <p>Then the celestial-globe gate drew a page by a projection its
 * viewport did not name, and they disagreed all at once: the stars
 * landed at orthographic radii on a plane scaled stereographically,
 * the disc covered half the page it should have filled, and every
 * label and grid line was placed by a projection that had not drawn a
 * single star on it. Thirteen implicit copies of a missing boundary,
 * discovered by giving them something to disagree about.
 *
 * <p>So {@link DrawnPage} carries the scene and its projection as one
 * value, and this holds the rule that makes it worth carrying:
 * <strong>only {@code DrawnPage.of} may work a projection out from
 * viewport state.</strong> A fourteenth copy would not be caught by
 * any test of what the atlas draws today, because today every answer
 * still agrees. It would be caught here.
 */
class OneProjectionPerPageTest {

    /** The one place a projection may be derived from viewport state. */
    private static final String FACTORY =
            "src/juranometria/project/DrawnPage.java";

    /** The derivation itself. */
    private static final String DERIVATION = "Projections.forViewport(";

    @Test
    void onlyTheFactoryWorksOutAPagesProjection() throws IOException {
        List<String> elsewhere = new ArrayList<>();
        for (Path source : sources()) {
            String where = source.toString().replace('\\', '/');
            if (where.endsWith(FACTORY)) {
                continue;
            }
            String said = Files.readString(source,
                    StandardCharsets.UTF_8);
            int at = said.indexOf(DERIVATION);
            while (at >= 0) {
                elsewhere.add(where + ":" + lineOf(said, at));
                at = said.indexOf(DERIVATION, at + 1);
            }
        }
        assertEquals(List.of(), elsewhere,
                "a page's projection is worked out once, in "
                        + FACTORY + ", and carried as part of the page."
                        + " These ask the viewport again, which is how"
                        + " a page comes to be drawn by one projection"
                        + " and described by another: " + elsewhere);
    }

    @Test
    void theFactoryReallyIsWhereItHappens() {
        // The other direction, so this test cannot pass by the
        // derivation having been renamed out of existence and the
        // scan quietly finding nothing anywhere.
        Path factory = Path.of(FACTORY);
        assertTrue(Files.exists(factory), FACTORY + " must exist");
        String said = assertReadable(factory);
        assertTrue(said.contains(DERIVATION),
                FACTORY + " is where a page's projection is worked"
                        + " out, and it no longer does: either the"
                        + " rule moved or this scan is now watching"
                        + " nothing");
    }

    @Test
    void theMappingCannotBeBuiltWithoutSayingWhichProjection() {
        // The seam that made the split possible: a mapping used to
        // take a viewport alone and ask it. Every way of building one
        // now either carries the page or names the projection aloud.
        boolean derivesInternally = assertReadable(
                Path.of("src/juranometria/project/ViewportMapping.java"))
                .contains(DERIVATION);
        assertTrue(!derivesInternally,
                "a mapping is built from a page, or from a viewport"
                        + " and a projection named together - never"
                        + " from a viewport it interrogates itself,"
                        + " because that is the copy that scaled a"
                        + " hemisphere by the wrong projection");
    }

    private static String assertReadable(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new IllegalStateException(
                    "the scan must be able to read " + file,
                    unreadable);
        }
    }

    private static int lineOf(String said, int at) {
        int line = 1;
        for (int i = 0; i < at; i++) {
            if (said.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static List<Path> sources() throws IOException {
        try (Stream<Path> tree = Files.walk(Path.of("src"))) {
            return tree.filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }
}
