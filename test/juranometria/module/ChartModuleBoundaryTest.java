package juranometria.module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartScene;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart's independence from its modules (Sprint 24, issue #215).
 *
 * <blockquote>JUranometria is a complete celestial chart whose
 * modules add removable ways to read the sky.</blockquote>
 *
 * <p>That sentence is worth nothing as prose. Asserted here: the
 * core does not import the module seam or the page services, and the
 * atlas builds and draws its ordinary chart with every module
 * absent - byte for byte the same page.
 */
class ChartModuleBoundaryTest {

    /**
     * The chart core: what the atlas is without any module at all.
     * These packages may know nothing of pages, modules, tables or
     * crosses.
     */
    private static final List<String> CORE = List.of(
            "src/juranometria/chart", "src/juranometria/render",
            "src/juranometria/project", "src/juranometria/catalog",
            "src/juranometria/geo", "src/juranometria/search");

    @Test
    void theCoreDoesNotKnowItsModules() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String directory : CORE) {
            for (Path source : javaIn(directory)) {
                String text = codeOf(source);
                if (text.contains("juranometria.page")
                        || text.contains("juranometria.module")) {
                    offenders.add(source.toString());
                }
            }
        }
        assertEquals(List.of(), offenders,
                "the dependency direction is one way: a module"
                        + " consumes the chart's services, and the"
                        + " chart never learns the module exists");
    }

    @Test
    void theServicesAndTheSeamKnowNothingOfAWindow() throws IOException {
        // UI-independent is the whole point of putting these here
        // rather than in the panel that will display them: a future
        // module subscribes without the chart learning what a table
        // is, and a headless test can hold the model.
        List<String> offenders = new ArrayList<>();
        for (String directory
                : List.of("src/juranometria/page", "src/juranometria/module")) {
            for (Path source : javaIn(directory)) {
                String text = codeOf(source);
                for (String forbidden : List.of("javax.swing", "java.awt.event",
                        "juranometria.ui", "java.util.prefs")) {
                    if (text.contains(forbidden)) {
                        offenders.add(source.getFileName() + " imports "
                                + forbidden);
                    }
                }
            }
        }
        assertEquals(List.of(), offenders,
                "no Swing, no AWT events, no UI package, and no"
                        + " preferences: working marks are ephemeral"
                        + " and the services are not a panel");
    }

    @Test
    void aModuleIsNeverHandedAGraphicsContext() throws IOException {
        // Handing one out makes the chart a generic canvas, lets a
        // module invent cartography the atlas has not decided, and
        // puts painting policy in two places. The seam offers typed
        // geometry with an ink role instead.
        for (Path source : javaIn("src/juranometria/module")) {
            String text = codeOf(source);
            assertTrue(!text.contains("Graphics2D") && !text.contains("Graphics "),
                    source.getFileName() + " must not pass a graphics"
                            + " context to a module");
        }
        // The closed set, by name rather than by count: Sprint 25's
        // gate added the great circle - a pole and a role, the one
        // geometry a straight-line reference needs and the one thing
        // a polyline could not express - and a count alone would not
        // have said which of these had arrived.
        assertEquals(java.util.List.of("GreatCircle", "Point", "Path",
                        "Region"),
                java.util.Arrays.stream(OverlayContribution.class
                                .getPermittedSubclasses())
                        .map(Class::getSimpleName).sorted(
                                java.util.Comparator.comparingInt(
                                        java.util.List.of("GreatCircle",
                                                "Point", "Path",
                                                "Region")::indexOf))
                        .toList(),
                "a great circle, a point, a path and a region - the"
                        + " geometry a module may contribute, and"
                        + " nothing else");
    }

    @Test
    void theAtlasDrawsItsOrdinaryChartWithEveryModuleAbsent() {
        // Constructed and rendered without touching juranometria.page
        // or juranometria.module at all: this is the atlas as it
        // ships if every module is deleted.
        ChartScene scene = Atlas.assembler()
                .assemble(ChartViewState.DEFAULT, 900, 700);
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);

        java.awt.image.BufferedImage page =
                renderer.renderToImage(scene, ChartOptions.DEFAULTS);
        assertEquals(900, page.getWidth());
        assertEquals(700, page.getHeight());

        // And nothing a module could hold changes that page. With
        // nothing marked, the chart is byte-identical to the chart
        // that has no modules at all.
        juranometria.chart.WorkingSelection marks =
                new juranometria.chart.WorkingSelection();
        juranometria.page.PageContents inventory =
                juranometria.page.PageInventory.of(scene, ChartOptions.DEFAULTS);
        assertTrue(!inventory.entries().isEmpty(),
                "the module has something to say about this page");
        assertTrue(marks.members().isEmpty(), "and nothing is marked");

        java.awt.image.BufferedImage again =
                renderer.renderToImage(scene, ChartOptions.DEFAULTS);
        assertTrue(identical(page, again),
                "the ordinary page is what it was: a module that has"
                        + " not been asked for anything contributes"
                        + " nothing, and the chart does not consult it"
                        + " to draw");
    }

    // ----------------------------------------------------------------

    /**
     * A source file with its comments removed.
     *
     * <p>A structural check that reads prose fails on the sentence
     * explaining the rule it is enforcing - which is exactly what
     * the first version of this test did, on
     * {@code OverlayContribution}'s own "a module never receives a
     * Graphics2D". The rule is about code, so the check reads code.
     */
    private static String codeOf(Path source) throws IOException {
        String text = Files.readString(source);
        text = text.replaceAll("(?s)/\\*.*?\\*/", " ");
        text = text.replaceAll("(?m)//.*$", " ");
        return text;
    }

    private static List<Path> javaIn(String directory) throws IOException {
        try (Stream<Path> tree = Files.walk(Path.of(directory))) {
            return tree.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static boolean identical(java.awt.image.BufferedImage a,
                                     java.awt.image.BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
