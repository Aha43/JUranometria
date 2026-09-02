package juranometria.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.chart.StarSizePolicy;
import juranometria.render.SymbolFamily;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the page inventory reports (Sprint 24, issue #215).
 *
 * <p>Two things have to hold, and they pull in opposite directions.
 * The inventory must be <strong>independent of the drawing
 * filters</strong> - a reader who cannot find M110 is owed an answer
 * about M110 - and it must report visibility from
 * <strong>production truth</strong> rather than from a second set of
 * rules that agrees with the renderer today.
 */
class PageInventoryTest {

    private static final ChartOptions DEFAULTS = ChartOptions.DEFAULTS;
    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private static ChartScene page(double ra, double dec, double field) {
        return page(ra, dec, field, 900, 700);
    }

    private static ChartScene page(double ra, double dec, double field,
                                   int width, int height) {
        return Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(ra, dec), field, 8.0),
                width, height);
    }

    /**
     * The pages the acceptance asks for: an ordinary one, the RA
     * seam, a pole, a letterboxed page, and a dense field.
     */
    private static List<ChartScene> theHardPages() {
        return List.of(
                page(10.684, 41.269, 8.0),            // ordinary
                page(359.8, 0.0, 8.0),                // the RA seam
                page(0.0, 89.6, 12.0),                // a pole
                page(10.684, 41.269, 8.0, 500, 1000), // letterboxed
                page(186.6, 12.7, 36.0));             // dense: Virgo
    }

    @Test
    void everyDeepSkyObjectOnThePaperIsReportedAndNoOtherIs() {
        for (ChartScene scene : theHardPages()) {
            PageContents contents = PageInventory.of(scene, DEFAULTS);
            List<String> reported = new ArrayList<>();
            for (PageEntry.DeepSky entry : contents.deepSky()) {
                reported.add(entry.identity());
            }

            // The oracle is the geometry itself, asked object by
            // object over the whole scene rather than over what the
            // inventory happens to have kept.
            List<String> expected = new ArrayList<>();
            for (DeepSkyObject dso : scene.deepSkyObjects()) {
                if (PageExtent.onPage(scene, dso)) {
                    expected.add(dso.id());
                }
            }
            Collections.sort(expected);
            List<String> sorted = new ArrayList<>(reported);
            Collections.sort(sorted);
            assertEquals(expected, sorted, "on the "
                    + scene.viewport().fieldWidthDegrees() + "° page at "
                    + scene.viewport().centre());
            assertEquals(reported.size(),
                    new java.util.HashSet<>(reported).size(),
                    "and each is reported once");
        }
    }

    @Test
    void everythingTheChartDrawsIsOnTheInventoryAndSaysSo() {
        // The seam that matters: what the renderer paints must be
        // present and classified DRAWN. Anything else means the
        // inventory and the page disagree about the same sky.
        for (ChartScene scene : theHardPages()) {
            PageContents contents = PageInventory.of(scene, DEFAULTS);
            for (ChartRenderer.DrawnMark mark
                    : RENDERER.drawnMarks(scene, DEFAULTS)) {
                if (mark.kind() != ChartRenderer.DrawnMark.Kind.DEEP_SKY) {
                    continue;
                }
                String id = mark.deepSky().id();
                assertTrue(contents.holds(id),
                        "the page draws " + id + ", so the inventory"
                                + " holds it");
                assertEquals(PageVisibility.DRAWN,
                        contents.find(id).orElseThrow().visibility(),
                        "and calls it drawn: " + id);
            }
        }
    }

    @Test
    void hidingAFamilyChangesWhyItCannotBeSeenAndNotWhatIsThere() {
        ChartScene scene = page(10.684, 41.269, 8.0);
        ChartOptions galaxiesOff =
                DEFAULTS.withFamily(SymbolFamily.GALAXIES, false);

        PageContents shown = PageInventory.of(scene, DEFAULTS);
        PageContents hidden = PageInventory.of(scene, galaxiesOff);

        assertEquals(identities(shown), identities(hidden),
                "the page contains the same objects either way:"
                        + " presence is a fact about the sky");
        assertTrue(hidden.tally().get(PageVisibility.FAMILY_HIDDEN)
                        > shown.tally().get(PageVisibility.FAMILY_HIDDEN),
                "and switching the family off moves objects into"
                        + " hidden-by-a-chart-option: " + hidden.tally());

        // The named case from the decision, so this cannot pass by
        // moving some other object.
        assertEquals(PageVisibility.DRAWN,
                shown.find("NGC 205").orElseThrow().visibility());
        assertEquals(PageVisibility.FAMILY_HIDDEN,
                hidden.find("NGC 205").orElseThrow().visibility(),
                "M110 is still on the page, and the reader is told why"
                        + " it is not drawn");
    }

    @Test
    void aStarTooFaintForThePageIsPresentAndCalledTooFaint() {
        // At a lower limit than the pack carries, so the scene holds
        // stars the page does not draw. At the default limit the
        // bundled pack stops where the page does, and the state
        // would never arise to be checked.
        ChartScene scene = Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(10.684, 41.269), 8.0, 5.0),
                900, 700);
        PageContents contents = PageInventory.of(scene, DEFAULTS);

        int faint = 0;
        for (PageEntry entry : contents.entries()) {
            if (entry instanceof PageEntry.StarEntry star) {
                assertEquals(star.star().magnitude()
                                > scene.limitingMagnitude()
                                ? PageVisibility.BELOW_LIMIT
                                : PageVisibility.DRAWN,
                        star.visibility(),
                        "the magnitude limit is the page's own: "
                                + star.identity());
                if (star.visibility() == PageVisibility.BELOW_LIMIT) {
                    faint++;
                }
            }
        }
        assertTrue(faint > 0,
                "the scene holds stars the page does not draw - that is"
                        + " why the inventory is not the drawing list");
    }

    @Test
    void anObjectWithNoSymbolIsReportedRatherThanDroppedInSilence() {
        // A type the atlas draws nothing for is the easiest object to
        // lose: it is absent from the page and absent from a drawing
        // list, so only an inventory can tell a reader it is here.
        ChartScene scene = page(10.684, 41.269, 8.0);
        DeepSkyObject unmarked = new DeepSkyObject("TEST-NO-SYMBOL",
                List.of(), juranometria.chart.DsoType.OTHER,
                scene.viewport().centre(), 2.0, 2.0, 0.0, 9.0, 1,
                new DeepSkyObject.Recorded(2.0, 2.0, 0.0,
                        DeepSkyObject.Recorded.Band.VISUAL));
        ChartScene withIt = withDeepSky(scene,
                append(scene.deepSkyObjects(), unmarked));

        PageContents contents = PageInventory.of(withIt, DEFAULTS);
        assertEquals(PageVisibility.NO_SYMBOL,
                contents.find("TEST-NO-SYMBOL").orElseThrow().visibility(),
                "the atlas has no symbol for its type, and says so");
        assertTrue(RENDERER.drawnMarks(withIt, DEFAULTS).stream()
                        .noneMatch(mark -> mark.kind()
                                == ChartRenderer.DrawnMark.Kind.DEEP_SKY
                                && mark.deepSky().id().equals("TEST-NO-SYMBOL")),
                "and the page really does draw nothing for it");
    }

    @Test
    void theSamePageReportsTheSameThingHoweverTheCatalogueArrives() {
        // Reversed storage order must not change one row. An order
        // that depended on the catalogue's would make two runs of the
        // same page disagree, which is exactly what a total
        // tie-break on identity exists to prevent.
        for (ChartScene scene : theHardPages()) {
            List<DeepSkyObject> reversedDeepSky =
                    new ArrayList<>(scene.deepSkyObjects());
            Collections.reverse(reversedDeepSky);
            List<Star> reversedStars = new ArrayList<>(scene.stars());
            Collections.reverse(reversedStars);
            ChartScene reversed = new ChartScene(scene.viewport(),
                    reversedStars, reversedDeepSky, scene.title(),
                    scene.limitingMagnitude(), scene.targetIdentity(),
                    scene.geography());

            assertEquals(rowsOf(PageInventory.of(scene, DEFAULTS)),
                    rowsOf(PageInventory.of(reversed, DEFAULTS)),
                    "the same page, reported identically: "
                            + scene.viewport().centre());
        }
    }

    @Test
    void theInventoryIsBuiltFromThePageTheChartAlreadyHolds() {
        // "No catalogue query" is a design property, not a promise:
        // an inventory of a scene with nothing in it finds nothing,
        // because there is nowhere else for it to look.
        ChartScene scene = page(10.684, 41.269, 8.0);
        ChartScene emptied = new ChartScene(scene.viewport(), List.of(),
                List.of(), scene.title(), scene.limitingMagnitude(),
                scene.targetIdentity(), scene.geography());

        assertTrue(PageInventory.of(emptied, DEFAULTS).entries().isEmpty(),
                "no query goes out to fill the gap");
        assertFalse(PageInventory.of(scene, DEFAULTS).entries().isEmpty(),
                "and the ordinary page is not empty, so this is not"
                        + " passing by finding nothing anywhere");
    }

    @Test
    void aPageIsReportedFastEnoughToAnswerAReadersGesture() {
        // The gate deferred the budget to #215 and kept timings out
        // of the report, because a study whose output moves cannot
        // reproduce. A test may hold a budget the report cannot: the
        // dense page is the worst the atlas offers, and a reader
        // asking what is here waits for one.
        ChartScene dense = page(186.6, 12.7, 36.0);
        PageInventory.of(dense, DEFAULTS);          // warm the classes

        long start = System.nanoTime();
        PageContents contents = PageInventory.of(dense, DEFAULTS);
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(contents.entries().size() > 1000,
                "the densest page the atlas offers: "
                        + contents.entries().size() + " entries");
        assertTrue(millis < 2000, "building it took " + millis
                + " ms; the budget is 2000 ms on the supported CI path,"
                + " set well above the measured cost so it fails for a"
                + " lost algorithm rather than for a slow runner");
    }

    // ----------------------------------------------------------------

    private static List<String> identities(PageContents contents) {
        List<String> found = new ArrayList<>();
        for (PageEntry entry : contents.entries()) {
            found.add(entry.identity());
        }
        Collections.sort(found);
        return found;
    }

    /** Identity, visibility and order together: one row per entry. */
    private static List<String> rowsOf(PageContents contents) {
        List<String> rows = new ArrayList<>();
        for (PageEntry entry : contents.entries()) {
            rows.add(entry.identity() + " " + entry.visibility());
        }
        return rows;
    }

    private static List<DeepSkyObject> append(List<DeepSkyObject> objects,
                                              DeepSkyObject extra) {
        List<DeepSkyObject> all = new ArrayList<>(objects);
        all.add(extra);
        return all;
    }

    private static ChartScene withDeepSky(ChartScene scene,
                                          List<DeepSkyObject> objects) {
        return new ChartScene(scene.viewport(), scene.stars(), objects,
                scene.title(), scene.limitingMagnitude(),
                scene.targetIdentity(), scene.geography());
    }
}
