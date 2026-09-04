package juranometria.chart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The working selection is identities and order, nothing else
 * (issue #260): a bytecode boundary in the removable-model
 * pattern - the constant pool records what a type reaches, and a
 * source scan can be fooled by a fully qualified name.
 */
class WorkingSelectionBoundaryTest {

    /** Written as the class file writes them, with slashes. */
    private static final List<String> FORBIDDEN = List.of(
            "java/awt",            // it draws nothing
            "javax/swing",         // and owns no widgets
            "javax/imageio",       // no images
            "juranometria/ui",     // no screen
            "juranometria/render", // no renderer
            "juranometria/app",    // no application life cycle
            "java/util/prefs",     // nothing remembered between runs
            "java/net",            // no network
            "java/nio/file",       // no files
            "java/io/File");       // and no paths to them

    private static final Path CLASSES = Path.of("build/classes");

    @Test
    void theSelectionAndItsModeReachNoSurfaceStoreOrWire()
            throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String type : new String[] {
                "juranometria/chart/WorkingSelection.class",
                "juranometria/chart/WorkingSelection$Change.class",
                "juranometria/chart/SelectionMode.class"}) {
            Path compiled = CLASSES.resolve(type);
            assertTrue(Files.exists(compiled),
                    "the classes are compiled: " + compiled);
            for (String forbidden : refersTo(compiled, FORBIDDEN)) {
                offenders.add(type + " -> " + forbidden);
            }
        }
        assertEquals(List.of(), offenders,
                "the working selection is a session set of"
                        + " identities: it draws nothing, stores"
                        + " nothing, reads nothing and connects to"
                        + " nothing - and no planner domain exists"
                        + " for it to reach");
    }

    @Test
    void andTheScanCanTellWhenSomethingDoes() throws IOException {
        Path renderer = CLASSES.resolve(
                "juranometria/render/ChartRenderer.class");
        assertTrue(Files.exists(renderer));
        assertTrue(refersTo(renderer, FORBIDDEN).contains("java/awt"),
                "a class that does draw is caught by the same scan,"
                        + " so the clean answer above is an answer");
    }

    /** Which forbidden prefixes a compiled class refers to. */
    private static List<String> refersTo(Path compiled,
                                         List<String> forbidden)
            throws IOException {
        String pool = new String(Files.readAllBytes(compiled),
                java.nio.charset.StandardCharsets.ISO_8859_1);
        List<String> found = new ArrayList<>();
        for (String prefix : forbidden) {
            if (pool.contains(prefix)) {
                found.add(prefix);
            }
        }
        return found;
    }
}
