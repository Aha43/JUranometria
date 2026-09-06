package juranometria.chart;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.catalog.TiledCatalogue;
import juranometria.render.ChartRenderer;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 42-degree sheet step (Sprint 29, issue #284).
 *
 * <p>The gate measured a distortion budget and chose one number
 * (docs/decisions/printable-chart.md). What it bought is a single new
 * step at the top of the existing gnomonic sequence - no new
 * projection, no chooser, no persistence. What it promised is that
 * everything below it is untouched.
 *
 * <p>Both halves are held here: the step is the one that was chosen
 * and nothing wider is reachable, and every released page still
 * renders to the pixel it rendered to before.
 */
class WiderFieldStepTest {

    private static final Path RELEASED_PAGES =
            Path.of("docs/studies/wider-field/released-pages.txt");

    @Test
    void theSequenceGainedTheStepTheGateChoseAndNothingElse() {
        assertEquals(List.of(42.0, 36.0, 24.0, 18.0, 12.0, 8.0, 6.0,
                        4.0, 3.0, 2.0, 1.0),
                ChartViewState.fieldWidthSteps(),
                "one step, at the top, in the released order");

        // The fields the gate measured and rejected stay unreachable.
        // 45 and 48 exceed the 12% anisotropy budget; 40 is inside it
        // but was not chosen, and an unchosen field is not a step.
        for (double rejected : new double[] {40.0, 45.0, 48.0, 60.0}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new ChartViewState(new SkyPosition(83.0, 0.0),
                            rejected, 6.0),
                    rejected + " degrees is not a step the gate chose");
        }
    }

    @Test
    void zoomingOutReachesTheSheetPageAndStopsThere() {
        ChartViewState state = ChartViewState.DEFAULT;
        while (state.canZoomOut()) {
            state = state.zoomOut();
        }
        assertEquals(42.0, state.fieldWidthDegrees(),
                "the widest page a reader can reach is the sheet page");
        assertFalse(state.canZoomOut(), "and it is the end of the road");
        assertSame(state, state.zoomOut(),
                "zooming out there is a clean no-op, as at every other"
                        + " bound");

        // And it is one step above the released widest, not a jump
        // past it: the reader still passes through 36.
        assertEquals(36.0, state.zoomIn().fieldWidthDegrees(),
                "36 is now a stop on the way rather than the end");
        assertEquals(42.0, state.zoomIn().zoomOut().fieldWidthDegrees(),
                "and the step is reversible like every other");
    }

    @Test
    void theSheetPageStaysInsideTheProjectionCeilingSprintSixDeclared() {
        // docs/decisions/regional-zoom.md drew a line three sprints
        // before this one: a corner radial scale of 1.25 - reached
        // near a 44-degree field at this aspect - is the boundary
        // beyond which the gnomonic chart may not honestly go. The
        // sheet step was chosen on a different budget (anisotropy,
        // 12%), so that the two agree is worth holding rather than
        // assuming.
        double half = Math.tan(Math.toRadians(42.0) / 2.0);
        double corner = Math.atan(Math.hypot(half, half * 700.0 / 900.0));
        double radialScale = 1.0 / (Math.cos(corner) * Math.cos(corner));
        assertTrue(radialScale < 1.25,
                "the sheet page's corner scale stays under the"
                        + " declared ceiling: " + String.format(
                                "%.4f", radialScale));
        assertTrue(radialScale > 1.2,
                "and it is genuinely near it rather than trivially"
                        + " under: " + String.format("%.4f", radialScale));
    }

    @Test
    void everyReleasedPageStillDrawsTheSameThingsInTheSamePlaces()
            throws Exception {
        // The gate's promise, settled the only way it can be. The
        // rows were taken from the released build itself; see the
        // file's own header for how.
        //
        // Geometry is arithmetic, so this half holds on any machine.
        // Rasterisation is not - fonts and the JDK's 2D pipeline
        // differ - so the pixel half is checked only where the rows
        // were recorded, and skipped out loud everywhere else rather
        // than quietly passing.
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        List<String> rows = rows();
        assertEquals(80, rows.size(),
                "ten released steps, four centres, two grounds");

        boolean samePlatform = platformOfRecord()
                .equals(juranometria.tool.WiderFieldStudyMain.platform());
        List<String> changed = new ArrayList<>();
        List<String> rasterised = new ArrayList<>();
        for (String row : rows) {
            String[] cell = row.trim().split("\\s+");
            double field = Double.parseDouble(cell[0]);
            double[] centre = {Double.parseDouble(cell[1]),
                    Double.parseDouble(cell[2])};
            boolean black = cell[3].equals("black");

            String marks = juranometria.tool.WiderFieldStudyMain
                    .markFingerprint(renderer, centre, field);
            if (!marks.equals(cell[4])) {
                changed.add(row.trim() + " now draws " + marks);
            }
            if (samePlatform) {
                String pixels = juranometria.tool.WiderFieldStudyMain
                        .fingerprint(renderer, centre, field, black);
                if (!pixels.equals(cell[5])) {
                    rasterised.add(row.trim() + " now renders " + pixels);
                }
            }
        }
        assertEquals(List.of(), changed,
                "widening the sequence changed what no released page"
                        + " draws, or where");
        assertEquals(List.of(), rasterised,
                "nor, on the platform the rows were recorded on, a"
                        + " single pixel of one");
    }

    /** The platform the committed pixel column is an oracle for. */
    private static String platformOfRecord() throws Exception {
        for (String line : Files.readAllLines(RELEASED_PAGES)) {
            if (line.startsWith("Recorded on: `")) {
                return line.substring(line.indexOf('`') + 1,
                        line.lastIndexOf('`'));
            }
        }
        throw new AssertionError("the oracle does not say which"
                + " platform its pixel column came from");
    }

    @Test
    void theSheetPageQueriesItsOwnCornersWithoutAskingForTheSky() {
        // "Queries cover every visible direction" is not a claim
        // about a radius, it is a claim about objects. So: take the
        // page's actual corners, ask the catalogue directly what is
        // there, and require the assembled scene to hold all of it.
        SkyPosition centre = new SkyPosition(83.0, 0.0);
        ChartViewState sheet = new ChartViewState(centre, 42.0, 8.0);
        ChartScene scene = Atlas.assembler().assemble(sheet, 900, 700);
        ChartViewport viewport = scene.viewport();

        TiledCatalogue catalogue = TiledCatalogue.load();
        List<String> missing = new ArrayList<>();
        int corners = 0;
        for (int[] pixel : new int[][] {{1, 1}, {898, 1}, {1, 698},
                {898, 698}}) {
            SkyPosition corner = PanSolver.skyFromPlane(viewport.centre(),
                    PanSolver.planeFromPixel(viewport,
                            new PixelPoint(pixel[0], pixel[1])));
            for (Star near : catalogue.starsIn(new SkyRegion(corner, 1.0))) {
                if (near.magnitude() > sheet.limitingMagnitude()) {
                    continue;
                }
                corners++;
                if (scene.stars().stream().noneMatch(
                        each -> each.position().separationDegrees(
                                near.position()) < 1.0e-9)) {
                    missing.add(near.identity() + " at the corner "
                            + corner);
                }
            }
        }
        assertTrue(corners > 10,
                "the corners of a 42-degree Orion page have stars to"
                        + " miss - 19 of them when this was written: "
                        + corners);
        assertEquals(List.of(), missing,
                "and the query reached every one of them - a radius"
                        + " left at the old field would not have");

        // Corner stars alone cannot settle this: the pack's object
        // margin is wider than the difference between the released
        // corner and the sheet one, so a radius left at 36 degrees
        // would still cover them. What tells them apart is how far
        // the query actually reached.
        double half = Math.tan(Math.toRadians(42.0) / 2.0);
        double cornerDegrees = Math.toDegrees(Math.atan(
                Math.hypot(half, half * 700.0 / 900.0)));
        double margin = catalogue.manifest().maxObjectSemiExtentDegrees();
        double reached = scene.stars().stream()
                .mapToDouble(each ->
                        each.position().separationDegrees(centre))
                .max().orElseThrow();
        assertEquals(cornerDegrees + margin, reached, 0.1,
                "the query reached this page's own corner plus the"
                        + " pack's declared margin - a radius left at"
                        + " the released field would stop "
                        + String.format("%.1f", cornerDegrees + margin
                                - (Math.toDegrees(Math.atan(Math.hypot(
                                        Math.tan(Math.toRadians(36.0) / 2.0),
                                        Math.tan(Math.toRadians(36.0) / 2.0)
                                                * 700.0 / 900.0))) + margin))
                        + " degrees short");

        // Without turning the page into an all-sky query: the scene
        // holds a page's worth of sky, not the pack's.
        assertTrue(scene.stars().size()
                        < catalogue.starsIn(new SkyRegion(centre, 180.0))
                                .size() / 4,
                "the sheet page is still a page: " + scene.stars().size()
                        + " stars");
    }

    private static List<String> rows() throws Exception {
        List<String> rows = new ArrayList<>();
        boolean inTable = false;
        for (String line : Files.readAllLines(RELEASED_PAGES)) {
            if (line.startsWith("field  ra")) {
                inTable = true;
            } else if (inTable && !line.isBlank()) {
                rows.add(line);
            }
        }
        return rows;
    }
}
