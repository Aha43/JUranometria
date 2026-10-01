package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rendering closure is a boundary, and it is the right one
 * (issue #398).
 *
 * <p>Two ways it could be wrong, and a test for each. It could reach
 * too little - miss a renderer, a generator the contract registers, a
 * tool the build runs - and a change there would take the narrow
 * route with nothing to catch it. Or it could reach everything, and
 * then it is not a boundary but a list of every file, and the narrow
 * route exists only on paper. Both are held here against the real
 * compiled tree, and the registries are read from their own source
 * so the closure cannot drift from what the contract actually runs.
 */
class RenderingClosureTest {

    private static final Path CLASSES = Path.of("build/classes");

    private static RenderingClosure closure;

    @BeforeAll
    static void readTheBuild() throws IOException {
        closure = RenderingClosure.of(CLASSES);
    }

    @Test
    void everyRendererInkModuleAndSkyClassIsReached() {
        List<String> missing = new ArrayList<>();
        for (String source : List.of(
                "src/juranometria/render/ChartRenderer.java",
                "src/juranometria/render/ChartOptions.java",
                "src/juranometria/render/LabelPlacement.java",
                "src/juranometria/project/Projection.java",
                "src/juranometria/sheet/ChartSheet.java",
                "src/juranometria/ui/ChartComponent.java",
                "src/juranometria/ui/ReferenceInk.java",
                "src/juranometria/ui/SheetInk.java",
                "src/juranometria/module/ChartModule.java",
                "src/juranometria/meridian/MeridianModule.java",
                "src/juranometria/ecliptic/EclipticModule.java",
                "src/juranometria/sky/SkyFrame.java",
                "src/juranometria/sky/LocalSky.java",
                "src/juranometria/sky/Ecliptic.java",
                "src/juranometria/app/AppMenuBar.java",
                "src/juranometria/app/PackagedAcceptanceMain.java")) {
            if (!closure.sources().contains(source)) {
                missing.add(source);
            }
        }
        assertEquals(List.of(), missing,
                "a renderer, an ink, a module, a sky transform or the"
                        + " menu bar a photographer draws is reached from"
                        + " a root; a change there is wide");
    }

    @Test
    void theClosureIsABoundaryNotAListOfEverything() {
        List<String> reached = new ArrayList<>();
        for (String source : List.of(
                "src/juranometria/app/JUranometriaMain.java",
                "src/juranometria/app/ViewReport.java",
                "src/juranometria/app/CopyViewReport.java",
                "src/juranometria/tool/RenderingClosure.java",
                "src/juranometria/tool/ChangeRoute.java",
                "src/juranometria/tool/ChangeClassifierMain.java")) {
            if (closure.sources().contains(source)) {
                reached.add(source);
            }
        }
        assertEquals(List.of(), reached,
                "the application's entry point, a text-only feature and"
                        + " the guard itself are outside the closure."
                        + " They are held by name where that matters,"
                        + " which is ChangeRoute's job, not this one's");
        assertTrue(closure.sources().size() < closure.classes().size(),
                "and the closure is smaller than the tree: "
                        + closure.sources().size() + " sources of "
                        + closure.classes().size() + " classes");
    }

    @Test
    void everyChainStartsAtARootAndEveryLinkIsARealReference() {
        List<String> broken = new ArrayList<>();
        for (String source : closure.sources()) {
            Optional<List<String>> chain = closure.chainTo(source);
            if (chain.isEmpty()) {
                broken.add(source + ": in the closure but no chain");
                continue;
            }
            List<String> steps = chain.get();
            if (!closure.roots().contains(steps.get(0))) {
                broken.add(source + ": chain starts at " + steps.get(0)
                        + ", not a root");
            }
            for (int i = 1; i < steps.size(); i++) {
                if (!closure.refersTo(steps.get(i - 1))
                        .contains(steps.get(i))) {
                    broken.add(source + ": " + steps.get(i - 1)
                            + " does not refer to " + steps.get(i));
                }
            }
        }
        assertEquals(List.of(), broken,
                "a chain is a reason, and a reason has to be true:"
                        + " each link is a reference the class file"
                        + " carries, and the first is a root");
    }

    @Test
    void everyGeneratorTheContractRegistersIsReached() throws IOException {
        Set<String> registered = named(
                Path.of("src/juranometria/tool/EvidenceContractMain.java"),
                "\"(juranometria\\.tool\\.[A-Za-z0-9]+Main)\"");
        assertTrue(registered.size() >= 30,
                "the premise: the contract names its generators as"
                        + " strings, and there are dozens ("
                        + registered.size() + " found)");
        List<String> missing = new ArrayList<>();
        for (String main : registered) {
            if (!closure.reached().contains(main.replace('.', '/'))) {
                missing.add(main);
            }
        }
        assertEquals(List.of(), missing,
                "a generator the contract runs by name is reached:"
                        + " the dotted name in the registry string is"
                        + " followed like a reference");
    }

    @Test
    void aGeneratorNamedOnlyInAStringIsFollowed() throws IOException {
        String contract = Files.readString(
                Path.of("src/juranometria/tool/EvidenceContractMain.java"),
                StandardCharsets.UTF_8);
        assertTrue(contract.contains(
                "\"juranometria.tool.PlaceAndTimeStudyMain\""),
                "the premise: the registry names this generator as a"
                        + " string");
        assertFalse(contract.contains("PlaceAndTimeStudyMain.class")
                || contract.contains("PlaceAndTimeStudyMain::"),
                "and never as a type, so a reference the compiler"
                        + " would record is not how it is reached");
        assertEquals(Optional.of(List.of(
                "juranometria/tool/EvidenceContractMain",
                "juranometria/tool/PlaceAndTimeStudyMain")),
                closure.chainTo(
                        "src/juranometria/tool/PlaceAndTimeStudyMain.java"),
                "the string is the edge");
    }

    @Test
    void everyInterfacePhotographerIsARoot() throws IOException {
        Set<String> photographers = named(
                Path.of("test/juranometria/tool/InterfaceEvidenceGateTest.java"),
                "GENERATORS\\.put\\(\"([A-Za-z0-9]+)\"");
        assertTrue(photographers.size() >= 12,
                "the premise: the interface gate registers a dozen ("
                        + photographers.size() + " found)");
        List<String> missing = new ArrayList<>();
        for (String name : photographers) {
            if (!closure.roots().contains("juranometria/tool/" + name)) {
                missing.add(name);
            }
        }
        assertEquals(List.of(), missing,
                "every photographer the interface gate registers is a"
                        + " root by the *SheetMain spelling; one named"
                        + " otherwise would be a generator the closure"
                        + " never starts from");
    }

    @Test
    void everyToolTheBuildRunsIsARoot() throws IOException {
        String makefile = Files.readString(Path.of("Makefile"),
                StandardCharsets.UTF_8);
        int start = makefile.indexOf("\nclasses:");
        int end = makefile.indexOf("\n\n", start + 1);
        assertTrue(start > 0 && end > start, "the classes target");
        Set<String> run = new TreeSet<>();
        Matcher m = Pattern.compile("juranometria\\.tool\\.([A-Za-z0-9]+)")
                .matcher(makefile.substring(start, end));
        while (m.find()) {
            run.add(m.group(1));
        }
        assertTrue(run.size() >= 2,
                "the premise: the build runs the index writers ("
                        + run + ")");
        List<String> missing = new ArrayList<>();
        for (String name : run) {
            if (!closure.roots().contains("juranometria/tool/" + name)) {
                missing.add(name);
            }
        }
        assertEquals(List.of(), missing,
                "a tool the build runs writes into the classes every"
                        + " renderer reads; nothing refers to it by"
                        + " class, so it is a root by name");
    }

    @Test
    void aNestedClassMapsToTheSourceThatCompiledIt() {
        assertEquals(Optional.of("src/juranometria/tool/RenderingClosure.java"),
                closure.sourceOf("juranometria/tool/RenderingClosure$ClassFile"),
                "the SourceFile attribute, not the class name: a nested"
                        + " class has no file of its own");
    }

    @Test
    void anIncompleteBuildRefusesRatherThanAnsweringNarrow(
            @org.junit.jupiter.api.io.TempDir Path scratch) throws IOException {
        Path one = scratch.resolve("juranometria/app");
        Files.createDirectories(one);
        Files.copy(CLASSES.resolve("juranometria/app/ViewReport.class"),
                one.resolve("ViewReport.class"));
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> RenderingClosure.of(scratch),
                "a tree without the roots proves nothing about them");
        assertTrue(refused.getMessage().contains("EvidenceContractMain"),
                refused.getMessage());
    }

    private static Set<String> named(Path source, String regex)
            throws IOException {
        Set<String> names = new TreeSet<>();
        Matcher m = Pattern.compile(regex).matcher(
                Files.readString(source, StandardCharsets.UTF_8));
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }
}
