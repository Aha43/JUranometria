package juranometria.sky;

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
 * The boundary the sky model promised (Sprint 25, issue #226).
 *
 * <p>{@code juranometria.sky} says of itself that it is "immutable,
 * and free of everything: no Swing, no preferences, no renderer, no
 * operating system, no network", and {@code juranometria.project}
 * says it is projection geometry that "draws nothing". Those were
 * prose until this test; the review asked for the promise to be
 * executable, because a boundary nothing checks is a boundary that
 * holds until the first hurried afternoon.
 *
 * <p>Two packages, and it matters that both are here. The model is
 * meant to be removable - the atlas without it is still an atlas -
 * and the projection is meant to be reusable by anything that maps
 * sky to page. A dependency on a windowing toolkit, a preferences
 * store or a socket would quietly end either claim.
 *
 * <p>It reads compiled classes rather than source. A source scan can
 * be fooled by a name that appears only in a comment and can miss a
 * type reached without an import; the constant pool records what the
 * code actually refers to.
 */
class RemovableModelBoundaryTest {

    /**
     * Written as the class file writes them, with slashes: a package
     * that draws, stores, reads or connects.
     */
    private static final List<String> FORBIDDEN = List.of(
            "java/awt",          // no toolkit: it draws nothing
            "javax/swing",       // and no widgets
            "javax/imageio",     // no images
            "juranometria/ui",   // no screen
            "juranometria/render", // and no renderer
            "juranometria/app",  // no application, no life cycle
            "java/util/prefs",   // nothing remembered between runs
            "java/net",          // no network, at build or run time
            "java/nio/file",     // no files
            "java/io/File");     // and no paths to them

    /**
     * The one exception, and it is a reviewed one: the seam names
     * {@code ChartOptions}, which lives in the renderer's package.
     * That is deliberate - the options are how the page is drawn,
     * and #215 decided a module may be told them. It is a record of
     * settings, not a renderer, and the module that consumes the
     * seam is held to the whole list.
     */
    private static final List<String> ALLOWED_IN_THE_SEAM =
            List.of("juranometria/render");

    private static List<String> forbiddenIn(String pkg) {
        return pkg.equals("juranometria/module")
                ? FORBIDDEN.stream()
                        .filter(name -> !ALLOWED_IN_THE_SEAM.contains(name))
                        .toList()
                : FORBIDDEN;
    }

    private static final Path CLASSES = Path.of("build/classes");

    @Test
    void theModelTheProjectionTheSeamAndTheModuleDependOnNoneOfThem()
            throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String pkg : List.of("juranometria/sky",
                "juranometria/project", "juranometria/meridian",
                "juranometria/ecliptic", "juranometria/module")) {
            for (Path type : classesIn(pkg)) {
                for (String forbidden : refersTo(type, forbiddenIn(pkg))) {
                    offenders.add(type.getFileName() + " -> " + forbidden);
                }
            }
        }
        assertEquals(List.of(), offenders,
                "the removable model and the projection are geometry:"
                        + " they draw nothing, store nothing, read"
                        + " nothing and connect to nothing");
    }

    @Test
    void andTheTestCanTellWhenSomethingDoes() throws IOException {
        // Without this the test above would pass just as happily if
        // the scan were looking in an empty directory or for strings
        // that never appear in any class file. The renderer really
        // does use the toolkit, so the scan must say so.
        Path renderer = CLASSES.resolve(
                "juranometria/render/ChartRenderer.class");
        assertTrue(Files.exists(renderer),
                "the classes are compiled: " + renderer.toAbsolutePath());
        assertTrue(refersTo(renderer, FORBIDDEN).contains("java/awt"),
                "a class that does draw is caught by the same scan,"
                        + " so the clean answer above is an answer and"
                        + " not a silence");
    }

    @Test
    void andItIsLookingAtSomething() throws IOException {
        // The other way to pass vacuously: a package name that has
        // moved, leaving the walk with nothing to inspect.
        assertTrue(classesIn("juranometria/sky").size() >= 4,
                "the sky model's classes are where this looks");
        assertTrue(classesIn("juranometria/project").size() >= 6,
                "and so are the projection's");
        assertTrue(classesIn("juranometria/meridian").size() >= 1,
                "and the module's");
        assertTrue(classesIn("juranometria/ecliptic").size() >= 1,
                "and the ecliptic module's");
        assertTrue(classesIn("juranometria/module").size() >= 6,
                "and the seam's");
    }

    // ----------------------------------------------------------------

    private static List<Path> classesIn(String pkg) throws IOException {
        Path root = CLASSES.resolve(pkg);
        assertTrue(Files.isDirectory(root),
                "compiled classes for " + pkg + " are in "
                        + root.toAbsolutePath());
        try (Stream<Path> tree = Files.walk(root)) {
            return tree.filter(p -> p.toString().endsWith(".class"))
                    .sorted().toList();
        }
    }

    /** Which forbidden packages this class file refers to. */
    private static List<String> refersTo(Path type, List<String> forbidden)
            throws IOException {
        String pool = new String(Files.readAllBytes(type),
                java.nio.charset.StandardCharsets.ISO_8859_1);
        return forbidden.stream().filter(pool::contains).toList();
    }
}
