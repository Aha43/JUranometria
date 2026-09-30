package juranometria.project;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rotation contract of the #414 ruling (C3), held before anything
 * is drawn with it (issue #416): at a body's projected position the
 * page's own north n̂ and east ê, a position angle χ turned into the
 * page direction cos χ · n̂ + sin χ · ê, and the round trip back to χ.
 *
 * <p>Held where the cartography study measured north turning: the
 * centre of an equatorial page, where the contract is the identity;
 * three quarters of the way to each corner of the 36° page centred at
 * +45° and at +75° of declination, of the 8° page at +45° and of a 1°
 * page; and near the pole.
 */
class PageBasisTest {

    private static DrawnPage page(double centreDec, double field) {
        return DrawnPage.of(Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(90.0, centreDec), field, 8.0, null, null), 900, 700));
    }

    /** The page centre and the four points three quarters of the way to its corners. */
    private static List<SkyPosition> probes(DrawnPage page) {
        ViewportMapping mapping = new ViewportMapping(page);
        double x = 450.0 * 0.75 / mapping.pixelsPerPlaneUnit();
        double y = 350.0 * 0.75 / mapping.pixelsPerPlaneUnit();
        List<SkyPosition> at = new ArrayList<>();
        at.add(page.projection().unproject(new PlanePoint(0.0, 0.0)).orElseThrow());
        for (double[] corner : new double[][] {{x, y}, {-x, y}, {x, -y}, {-x, -y}}) {
            at.add(page.projection().unproject(new PlanePoint(corner[0], corner[1]))
                    .orElseThrow());
        }
        return at;
    }

    private static void roundTrips(DrawnPage page, SkyPosition at) {
        PageBasis basis = PageBasis.at(page, at).orElseThrow();
        for (double chi = 0.0; chi < 360.0; chi += 7.5) {
            double[] d = basis.direction(chi);
            assertEquals(1.0, Math.hypot(d[0], d[1]), 1e-12, "a unit direction");
            double back = basis.positionAngleDegrees(d[0], d[1]);
            double miss = Math.abs(((back - chi) % 360.0 + 540.0) % 360.0 - 180.0);
            assertTrue(miss < 1e-9, "χ " + chi + " came back as " + back + " at " + at);
        }
    }

    @Test
    void atTheCentreOfAnEquatorialPageNorthIsUpAndEastIsLeft() {
        DrawnPage page = page(0.0, 24.0);
        PageBasis basis = PageBasis.at(page, new SkyPosition(90.0, 0.0)).orElseThrow();
        assertEquals(0.0, basis.northX(), 1e-6);
        assertEquals(-1.0, basis.northY(), 1e-6);
        assertEquals(-1.0, basis.eastX(), 1e-6);
        assertEquals(0.0, basis.eastY(), 1e-6);
        // There the contract is the identity: χ = 90° is due east, page-left.
        double[] east = basis.direction(90.0);
        assertEquals(-1.0, east[0], 1e-6);
        assertEquals(0.0, east[1], 1e-6);
        // And the ruled dot-product form agrees with the exact inverse.
        for (double chi = 0.0; chi < 360.0; chi += 15.0) {
            double[] d = basis.direction(chi);
            double dotForm = Math.toDegrees(Math.atan2(
                    d[0] * basis.eastX() + d[1] * basis.eastY(),
                    d[0] * basis.northX() + d[1] * basis.northY()));
            double exact = basis.positionAngleDegrees(d[0], d[1]);
            double miss = Math.abs(((dotForm - exact) % 360.0 + 540.0) % 360.0 - 180.0);
            assertTrue(miss < 1e-6, "χ " + chi + ": " + dotForm + " against " + exact);
        }
    }

    @Test
    void theRoundTripHoldsAtEveryPageTheStudyMeasured() {
        for (double[] measured : new double[][] {{0.0, 36.0}, {45.0, 36.0}, {75.0, 36.0},
                {45.0, 8.0}, {45.0, 1.0}}) {
            DrawnPage page = page(measured[0], measured[1]);
            for (SkyPosition at : probes(page)) {
                roundTrips(page, at);
            }
        }
    }

    @Test
    void northTurnsAtTheCornerOfATangentPageAndTheBasisIsSkewed() {
        // The study's 36° page at +45°: north turns at the corner, and
        // n̂ and ê stop being perpendicular - which is why the contract
        // keeps both vectors and inverts them exactly.
        DrawnPage page = page(45.0, 36.0);
        List<SkyPosition> at = probes(page);
        PageBasis centre = PageBasis.at(page, at.get(0)).orElseThrow();
        assertEquals(90.0, centre.skewDegrees(), 1e-3, "square at the centre, to the one-arcsecond step");
        double least = 90.0;
        for (SkyPosition corner : at.subList(1, at.size())) {
            PageBasis basis = PageBasis.at(page, corner).orElseThrow();
            least = Math.min(least, Math.min(basis.skewDegrees(), 180.0 - basis.skewDegrees()));
            double turn = Math.toDegrees(Math.atan2(basis.northX(), -basis.northY()));
            assertTrue(Math.abs(turn) > 5.0, "north turns at the corner: " + turn);
        }
        assertTrue(least < 89.0, "the basis is skewed at a corner: " + least);
    }

    @Test
    void nearThePoleTheBasisStillRoundTrips() {
        DrawnPage page = page(89.0, 8.0);
        roundTrips(page, new SkyPosition(10.0, 89.9));
        roundTrips(page, new SkyPosition(200.0, 88.5));
    }
}
