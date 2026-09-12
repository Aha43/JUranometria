package juranometria.sheet;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartOptions;
import juranometria.project.DrawnPage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An exported sheet names the projection that drew it, not the one
 * its viewport happens to carry (Sprint 32, issue #301).
 *
 * <p>The other half of {@code DrawnPageIdentityTest}, here because an
 * exported sheet's metadata is written in this package. The atlas has
 * made this exact mistake once: until #300 every sheet it had ever
 * written said "gnomonic", which was true of every page it could draw
 * until it was not.
 *
 * <p>For the length of the celestial-globe gate a study page's
 * viewport kind is deliberately not what drew it, so the sheet is
 * held to reading the page rather than the viewport.
 * <strong>#329 owns the removal.</strong>
 */
class GlobeSheetIdentityTest {

    private static final SkyPosition CENTRE = new SkyPosition(83.0, -1.0);

    @Test
    void theSheetSaysWhatDrewThePage() {
        ChartViewport viewport = new ChartViewport(CENTRE, 180.0,
                900, 700, ChartProjection.STEREOGRAPHIC);
        ChartScene scene = new ChartScene(viewport, List.of(), List.of(),
                "Orion", 6.0, null);
        DrawnPage page = new DrawnPage(scene, renamed(
                new juranometria.project.StereographicProjection(CENTRE),
                "orthographic"));

        SheetMetadata metadata = SheetMetadata.of(page, 180.0, 6.0,
                ChartOptions.DEFAULTS, PaperSize.A4);

        assertTrue(metadata.description().contains("orthographic"),
                "the sheet states the projection that drew the page: "
                        + metadata.description());
        assertFalse(metadata.description().contains("stereographic"),
                "and never one it was not drawn by, however the"
                        + " viewport is labelled: "
                        + metadata.description());
    }

    /**
     * The same projection under another name, which is all this test
     * needs: what is on trial is where the sheet reads the name from,
     * not the geometry it reads it beside.
     */
    private static juranometria.project.Projection renamed(
            juranometria.project.Projection wrapped, String name) {
        return new juranometria.project.Projection() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public SkyPosition centre() {
                return wrapped.centre();
            }

            @Override
            public java.util.Optional<juranometria.project.PlanePoint>
                    project(SkyPosition position) {
                return wrapped.project(position);
            }

            @Override
            public java.util.Optional<SkyPosition> unproject(
                    juranometria.project.PlanePoint point) {
                return wrapped.unproject(point);
            }

            @Override
            public double planeRadius(double angleDegrees) {
                return wrapped.planeRadius(angleDegrees);
            }

            @Override
            public double angleAtPlaneRadius(double planeRadius) {
                return wrapped.angleAtPlaneRadius(planeRadius);
            }

            @Override
            public double limitDegrees() {
                return wrapped.limitDegrees();
            }

            @Override
            public double usefulCornerDegrees() {
                return wrapped.usefulCornerDegrees();
            }

            @Override
            public double visiblePlaneRadius() {
                return wrapped.visiblePlaneRadius();
            }

            @Override
            public java.util.Optional<juranometria.project.PlaneConic>
                    greatCircle(SkyPosition pole) {
                return wrapped.greatCircle(pole);
            }
        };
    }

    @Test
    void anOrdinarySheetStillReadsItsViewport() {
        // The production path is unchanged by the door: a page built
        // the ordinary way names what its viewport names, because
        // that is what drew it.
        ChartViewport viewport = new ChartViewport(CENTRE, 90.0,
                900, 700, ChartProjection.STEREOGRAPHIC);
        ChartScene scene = new ChartScene(viewport, List.of(), List.of(),
                "Orion", 6.0, null);

        SheetMetadata metadata = SheetMetadata.of(DrawnPage.of(scene),
                90.0, 6.0, ChartOptions.DEFAULTS, PaperSize.A4);

        assertTrue(metadata.description().contains("stereographic"),
                "an ordinary overview sheet says stereographic: "
                        + metadata.description());
    }
}
