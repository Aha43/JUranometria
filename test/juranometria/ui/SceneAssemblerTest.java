package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.catalog.Catalogue;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneAssemblerTest {

    static final SkyPosition M31 = new SkyPosition(10.684708, 41.268750);

    /** Records the bounded queries the assembler issues. */
    static final class CountingCatalogue implements Catalogue {
        int starQueries;
        SkyRegion lastRegion;
        List<Star> stars = List.of();

        @Override
        public List<Star> starsIn(SkyRegion region) {
            starQueries++;
            lastRegion = region;
            return stars.stream().filter(star -> region.contains(star.position())).toList();
        }

        @Override
        public List<DeepSkyObject> deepSkyObjectsIn(SkyRegion region) {
            return List.of();
        }
    }

    @Test
    void queryRadiusReachesTheCornersPlusTheObjectMargin() {
        // 8-degree field at 900x700: the corners sit 5.06 degrees out.
        double radius = SceneAssembler.queryRadiusDegrees(8.0, 900, 700);
        assertEquals(5.06 + SceneAssembler.OBJECT_EXTENT_MARGIN_DEGREES, radius, 0.01);
    }

    @Test
    void tallWindowsAreNotSilentlyClipped() {
        // A 500x1000 window at an 8-degree field is nearly 16 degrees tall;
        // its corners sit 8.89 degrees from the centre.
        double radius = SceneAssembler.queryRadiusDegrees(8.0, 500, 1000);
        assertEquals(8.89 + SceneAssembler.OBJECT_EXTENT_MARGIN_DEGREES, radius, 0.01);
    }

    @Test
    void pagesBeyondTheBundledCoverageAreRefusedNotSilentlySparse() {
        // Codex review, Sprint 3: a 400x1000 page at an 8-degree field has
        // corners 10.6 degrees out - past the 10-degree data cone. The
        // assembler refuses it; the component letterboxes to the maximum
        // honest page height instead.
        SceneAssembler assembler = new SceneAssembler(
                new CountingCatalogue(), M31, "Test chart", 10.0);
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> assembler.assemble(ChartViewState.DEFAULT, 400, 1000));
        assertTrue(refused.getMessage().contains("coverage ends at 10.0"));

        int maxHeight = assembler.maxPageHeightPx(M31, 8.0, 400);
        assertTrue(maxHeight < 1000, "the honest page is shorter than the window");
        ChartScene clamped = assembler.assemble(ChartViewState.DEFAULT, 400, maxHeight);
        assertEquals(maxHeight, clamped.viewport().heightPx());
    }

    @Test
    void aSafeOffsetCentreAssemblesAroundItself() {
        CountingCatalogue catalogue = new CountingCatalogue();
        SceneAssembler assembler = new SceneAssembler(catalogue, M31, "Test chart", 10.0);
        SkyPosition offset = new SkyPosition(10.684708, 43.268750); // 2 degrees north

        assertTrue(assembler.fits(offset, 8.0), "2 + 4 + 1.5 stays inside 10");
        ChartScene scene = assembler.assemble(
                ChartViewState.DEFAULT.recenteredAt(offset), 900, 700);
        assertEquals(offset, scene.viewport().centre());
        assertEquals(offset, catalogue.lastRegion.centre(),
                "the query is centred on the state's position, not the data centre");
    }

    @Test
    void anEdgeCentreNeedsANarrowerFieldAndFarBeyondNothingFits() {
        SceneAssembler assembler = new SceneAssembler(
                new CountingCatalogue(), M31, "Test chart", 10.0);

        SkyPosition edge = new SkyPosition(10.684708, 46.268750); // 5 degrees out
        assertTrue(!assembler.fits(edge, 8.0), "5 + 4 + 1.5 exceeds 10");
        assertEquals(6.0, assembler.widestFittingFieldDegrees(edge).orElseThrow(),
                "the widest complete field at 5 degrees offset is the 6-degree step");

        SkyPosition beyond = new SkyPosition(10.684708, 49.968750); // 8.7 degrees out
        assertTrue(assembler.widestFittingFieldDegrees(beyond).isEmpty(),
                "not even the 1-degree step fits at 8.7 degrees offset");

        assertThrows(IllegalArgumentException.class, () -> assembler.assemble(
                ChartViewState.DEFAULT.recenteredAt(edge), 900, 700),
                "assembling an unfitting view is an error, never sparse sky");
    }

    @Test
    void exactHorizontalEqualityDoesNotCountAsFitting() {
        // Codex review P2: at exactly 4.5 degrees offset with an 8-degree
        // field, offset + half field + margin equals the 10-degree cone and
        // the letterboxed page height is zero - fitting must demand a
        // positive drawable page.
        SceneAssembler assembler = new SceneAssembler(
                new CountingCatalogue(), M31, "Test chart", 10.0);
        SkyPosition atEquality = new SkyPosition(10.684708, 45.768750);   // 4.5 out
        SkyPosition justInside = new SkyPosition(10.684708, 45.668750);   // 4.4 out
        SkyPosition justOutside = new SkyPosition(10.684708, 45.868750);  // 4.6 out

        assertTrue(!assembler.fits(atEquality, 8.0),
                "equality would allow only a zero-height page");
        assertEquals(0, assembler.maxPageHeightPx(atEquality, 8.0, 900));
        assertTrue(assembler.fits(justInside, 8.0));
        assertTrue(assembler.maxPageHeightPx(justInside, 8.0, 900) > 0,
                "everything that fits must be drawably tall");
        assertTrue(!assembler.fits(justOutside, 8.0));
    }

    @Test
    void letterboxingTightensWithAnOffsetCentre() {
        SceneAssembler assembler = new SceneAssembler(
                new CountingCatalogue(), M31, "Test chart", 10.0);
        SkyPosition offset = new SkyPosition(10.684708, 43.268750);
        int atDataCentre = assembler.maxPageHeightPx(M31, 8.0, 900);
        int atOffset = assembler.maxPageHeightPx(offset, 8.0, 900);
        assertTrue(atOffset < atDataCentre,
                "an offset centre has less honest height available");
        assertTrue(atOffset > 0, "a 2-degree offset still supports a real page");
    }

    @Test
    void assembledScenesCarryTheViewStateAndTheQueriedObjects() {
        CountingCatalogue catalogue = new CountingCatalogue();
        Star cornerStar = new Star("corner",
                new SkyPosition(10.684708, 41.268750 + 5.0), 6.0);
        Star outsideStar = new Star("outside",
                new SkyPosition(10.684708, 41.268750 + 9.0), 6.0);
        catalogue.stars = List.of(cornerStar, outsideStar);

        SceneAssembler assembler = new SceneAssembler(catalogue, M31, "Test chart", 10.0);
        ChartViewState state = ChartViewState.DEFAULT.decreaseMagnitudeLimit();
        ChartScene scene = assembler.assemble(state, 900, 700);

        assertEquals(8.0, scene.viewport().fieldWidthDegrees());
        assertEquals(7.0, scene.limitingMagnitude());
        assertEquals("Test chart", scene.title());
        assertEquals(List.of(cornerStar), scene.stars(),
                "a near-corner star is queried; one beyond the margin is not");
        assertEquals(1, catalogue.starQueries);
        assertTrue(catalogue.lastRegion.contains(cornerStar.position()));
    }
}
