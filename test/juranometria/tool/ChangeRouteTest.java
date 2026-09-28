package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import juranometria.tool.ChangeRoute.Finding;
import juranometria.tool.ChangeRoute.Route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mutation proof for the rendering-neutral gate (issue #398): every
 * kind of change the owner ruled wide forces the wide route, and the
 * narrow route is reachable at all.
 *
 * <p>Each case is one path of the kind named in the ruling - a
 * renderer, a generator, a committed image, a provenance row, a
 * packaging input, the classifier itself - handed to the rule as a
 * change, with the expectation that it comes back wide and says why.
 * Then the other half: a test, a decision record, a calculation
 * class nothing rendering-shaped refers to, come back narrow, so the
 * gate is a gate and not a bar.
 */
class ChangeRouteTest {

    private static final Set<String> CLOSURE = Set.of(
            "src/juranometria/render/ChartRenderer.java",
            "src/juranometria/sky/SkyFrame.java");

    private static final Function<String, Optional<List<String>>> CHAIN =
            source -> CLOSURE.contains(source)
                    ? Optional.of(List.of("juranometria/app/ChartImageMain",
                            "juranometria/render/ChartRenderer"))
                    : Optional.empty();

    private static final Predicate<String> PRESENT =
            path -> !path.contains("Removed");

    @Test
    void eachInputTheRegimeConsumesIsWideByName() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("docs/studies/place-and-time/page.png",
                        "committed evidence"),
                Map.entry("docs/studies/PROVENANCE.md",
                        "committed evidence"),
                Map.entry("docs/studies/ecliptic/reference-vectors.txt",
                        "committed evidence"),
                Map.entry("docs/reference/m31-stars.png",
                        "reference image"),
                Map.entry("docs/gallery/index.html", "gallery"),
                Map.entry("src/resources/interface-language/en.properties",
                        "bundled resource"),
                Map.entry("src/resources/solar-system/de440-subset.bsp",
                        "bundled resource"),
                Map.entry("packaging/LICENSING.md", "packaging input"),
                Map.entry("scripts/build-app-image.sh", "packaging script"),
                Map.entry("scripts/lib-versions.env", "packaging script"),
                Map.entry(".github/workflows/test.yml", "workflow"),
                Map.entry(".github/workflows/classify.yml", "workflow"),
                Map.entry("Makefile", "build input"),
                Map.entry("VERSION", "VERSION feeds"),
                Map.entry("LICENSING.md", "shipped in every package"),
                Map.entry("src/juranometria/app/JUranometriaMain.java",
                        "entry point"),
                Map.entry("src/juranometria/app/PackagedAcceptanceMain.java",
                        "qualification"),
                Map.entry("src/juranometria/tool/RenderingClosure.java",
                        "the guard itself"),
                Map.entry("src/juranometria/tool/ChangeRoute.java",
                        "the guard itself"),
                Map.entry("src/juranometria/tool/ChangeClassifierMain.java",
                        "the guard itself"));
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, String> e : expected.entrySet()) {
            Finding f = one(e.getKey());
            if (f.route() != Route.WIDE) {
                wrong.add(e.getKey() + " came back narrow");
            } else if (!f.reason().contains(e.getValue())) {
                wrong.add(e.getKey() + " is wide but says \"" + f.reason()
                        + "\", not \"" + e.getValue() + "\"");
            }
        }
        assertEquals(List.of(), wrong,
                "an input the rendering regime consumes without"
                        + " compiling it is wide whatever the closure"
                        + " says, and the finding names the reason");
    }

    @Test
    void aSourceInTheClosureIsWideAndSaysHowItIsReached() {
        Finding f = one("src/juranometria/render/ChartRenderer.java");
        assertEquals(Route.WIDE, f.route());
        assertTrue(f.reason().startsWith("in the rendering closure: ")
                && f.reason().contains("ChartImageMain -> "),
                "the reason is the chain from a root: " + f.reason());
    }

    @Test
    void aRemovedSourceIsWideBecauseItsDependentsCannotBeRead() {
        Finding f = one("src/juranometria/app/RemovedThing.java");
        assertEquals(Route.WIDE, f.route());
        assertTrue(f.reason().startsWith("removed source"), f.reason());
    }

    @Test
    void whatCannotReachRenderingIsNarrow() {
        List<String> wrong = new ArrayList<>();
        for (String path : List.of(
                "test/juranometria/solar/SunEphemerisTest.java",
                "test/juranometria/tool/ChangeRouteTest.java",
                "docs/decisions/sun-computation.md",
                "docs/development.md",
                "CHANGELOG.md",
                "README.md",
                "src/juranometria/solar/SunEphemeris.java",
                "src/juranometria/app/ViewReport.java")) {
            Finding f = one(path);
            if (f.route() != Route.NARROW) {
                wrong.add(path + ": " + f.reason());
            }
        }
        assertEquals(List.of(), wrong,
                "a test, a decision record, a calculation nothing"
                        + " rendering-shaped refers to: narrow, or the"
                        + " gate is a bar");
    }

    @Test
    void theRouteOfAChangeIsWideIfAnyPathIs() {
        List<Finding> mixed = ChangeRoute.classify(List.of(
                "docs/decisions/sun-computation.md",
                "src/juranometria/sky/SkyFrame.java",
                "test/juranometria/sky/SkyFrameTest.java"),
                CLOSURE, CHAIN, PRESENT);
        assertEquals(Route.WIDE, ChangeRoute.routeOf(mixed));
        List<Finding> narrow = ChangeRoute.classify(List.of(
                "docs/decisions/sun-computation.md",
                "test/juranometria/sky/SkyFrameTest.java"),
                CLOSURE, CHAIN, PRESENT);
        assertEquals(Route.NARROW, ChangeRoute.routeOf(narrow));
        assertEquals(Route.NARROW, ChangeRoute.routeOf(List.of()),
                "nothing changed reaches nothing");
    }

    @Test
    void theGuardNamesExactlyItsOwnFilesAndTheyExist() {
        List<String> guard = ChangeRoute.ALWAYS_WIDE.stream()
                .filter(rule -> rule.reason().equals("the guard itself"))
                .map(rule -> {
                    for (String candidate : List.of(
                            "src/juranometria/tool/RenderingClosure.java",
                            "src/juranometria/tool/ChangeRoute.java",
                            "src/juranometria/tool/ChangeClassifierMain.java",
                            "src/juranometria/tool/Other.java")) {
                        if (rule.matches().test(candidate)) {
                            return candidate;
                        }
                    }
                    return "(matches nothing named here)";
                }).toList();
        assertEquals(List.of(
                "src/juranometria/tool/RenderingClosure.java",
                "src/juranometria/tool/ChangeRoute.java",
                "src/juranometria/tool/ChangeClassifierMain.java"), guard,
                "the three files that are the guard");
        for (String path : guard) {
            assertTrue(Files.isRegularFile(Path.of(path)),
                    path + " exists, so the rule names a real file");
        }
    }

    @Test
    void onTheRealTreeARendererIsWideAndACalculationIsNot()
            throws IOException {
        RenderingClosure real = RenderingClosure.of(Path.of("build/classes"));
        List<Finding> findings = ChangeRoute.classify(List.of(
                "src/juranometria/render/ChartRenderer.java",
                "src/juranometria/sky/Ecliptic.java",
                "src/juranometria/app/ViewReport.java",
                "test/juranometria/app/ViewReportTest.java"),
                real.sources(), real::chainTo, ChangeRoute::existsInTree);
        assertEquals(List.of(Route.WIDE, Route.WIDE, Route.NARROW,
                Route.NARROW),
                findings.stream().map(Finding::route).toList(),
                findings.toString());
        for (Finding wide : findings.subList(0, 2)) {
            String chain = wide.reason().substring(
                    "in the rendering closure: ".length());
            String root = chain.split(" -> ")[0];
            assertTrue(real.roots().contains(root),
                    wide.path() + " is reached from a root: " + chain);
        }
    }

    private static Finding one(String path) {
        return ChangeRoute.classify(List.of(path), CLOSURE, CHAIN, PRESENT)
                .get(0);
    }
}
