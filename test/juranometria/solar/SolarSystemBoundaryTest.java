package juranometria.solar;

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
 * The Solar System service is removable and the chart learns nothing
 * from it (issues #397, #399).
 *
 * <p>Read from compiled classes, as {@code RemovableModelBoundaryTest}
 * reads them. Two directions. The service refers to no toolkit, no
 * screen, no renderer, no application, no preference store, no
 * network and no file system: it is a pack, a clock-free time
 * reading and arithmetic. And the chart core, the renderer, the chart
 * options, the projection and Place and Time's own packages refer to
 * nothing under {@code juranometria.solar}: the road to 4.0 says the
 * fixed J2000 chart learns no ephemeris, and this is where that is
 * held.
 */
class SolarSystemBoundaryTest {

    private static final Path CLASSES = Path.of("build/classes");

    private static final List<String> FORBIDDEN_TO_THE_SERVICE = List.of(
            "java/awt", "javax/swing", "javax/imageio",
            "juranometria/ui", "juranometria/render", "juranometria/app",
            "juranometria/tool",
            "java/util/prefs", "java/net", "java/nio/file", "java/io/File",
            "java/lang/ProcessBuilder", "java/time/Clock",
            "currentTimeMillis", "nanoTime");

    private static final List<String> MUST_NOT_KNOW_THE_SERVICE = List.of(
            "juranometria/chart", "juranometria/render", "juranometria/project",
            "juranometria/catalog", "juranometria/geo", "juranometria/search",
            "juranometria/sky", "juranometria/meridian", "juranometria/ecliptic",
            "juranometria/module", "juranometria/ui/placeandtime");

    @Test
    void theServiceDrawsStoresReadsAndConnectsNothing() throws IOException {
        List<String> leaks = new ArrayList<>();
        for (Path type : classesIn("juranometria/solar")) {
            for (String forbidden : refersTo(type, FORBIDDEN_TO_THE_SERVICE)) {
                leaks.add(type.getFileName() + " refers to " + forbidden);
            }
        }
        assertEquals(List.of(), leaks,
                "the service is a pack, a time reading and arithmetic; it"
                        + " draws nothing, remembers nothing and reads no clock");
    }

    @Test
    void theChartAndPlaceAndTimeLearnNothingOfTheService() throws IOException {
        List<String> leaks = new ArrayList<>();
        for (String pkg : MUST_NOT_KNOW_THE_SERVICE) {
            for (Path type : classesIn(pkg)) {
                for (String found : refersTo(type, List.of("juranometria/solar"))) {
                    leaks.add(type.getFileName() + " in " + pkg + " refers to "
                            + found);
                }
            }
        }
        assertEquals(List.of(), leaks,
                "the fixed J2000 chart, its renderer and options, the"
                        + " projection and the observer model refer to nothing"
                        + " under juranometria.solar");
    }

    @Test
    void theServiceReadsOnlyItsOwnPack() throws IOException {
        List<String> strays = new ArrayList<>();
        for (Path type : classesIn("juranometria/solar")) {
            String pool = Files.readString(type, StandardCharsets.ISO_8859_1);
            for (String other : List.of("/resources/catalog", "/resources/geo",
                    "/resources/sky-language", "/resources/interface-language")) {
                if (pool.contains(other)) {
                    strays.add(type.getFileName() + " names " + other);
                }
            }
        }
        assertEquals(List.of(), strays, "one pack, its own");
    }

    @Test
    void andTheCheckIsLookingAtSomething() throws IOException {
        assertTrue(classesIn("juranometria/solar").size() >= 8,
                "the service, its pack, its reader, its time scales");
        assertTrue(classesIn("juranometria/sky").size() >= 5);
    }

    private static List<Path> classesIn(String pkg) throws IOException {
        Path dir = CLASSES.resolve(pkg);
        assertTrue(Files.isDirectory(dir), dir + " exists; run make classes");
        try (Stream<Path> tree = Files.walk(dir)) {
            return tree.filter(p -> p.toString().endsWith(".class"))
                    .sorted().toList();
        }
    }

    private static List<String> refersTo(Path type, List<String> names)
            throws IOException {
        String pool = Files.readString(type, StandardCharsets.ISO_8859_1);
        return names.stream().filter(pool::contains).toList();
    }
}
