package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryReconstructionTest {

    @Test
    void precessionRoundTripsToMilliarcsecondPrecision() {
        SkyPosition[] points = {
                new SkyPosition(83.818667, -5.389667),
                new SkyPosition(0.5, 88.9),
                new SkyPosition(210.0, -82.5),
        };
        for (SkyPosition p : points) {
            SkyPosition back = PrecessionB1875.toJ2000(PrecessionB1875.toB1875(p));
            assertEquals(p.raDegrees(), back.raDegrees(), 1e-9);
            assertEquals(p.decDegrees(), back.decDegrees(), 1e-9);
        }
        // Precession over 125 years moves an equatorial position by
        // roughly 1.74 degrees in RA - the transform must actually move.
        SkyPosition m42At1875 = PrecessionB1875.toB1875(
                new SkyPosition(83.818667, -5.389667));
        assertTrue(Math.abs(m42At1875.raDegrees() - 83.818667) > 1.0,
                "B1875 must differ from J2000 by over a degree here");
    }

    @Test
    void aLongPolarConstantDecEdgeReconstructsWithinToleranceWhereItsChordFails() {
        // The Chamaeleon-style case the decision measured: a constant-Dec
        // B1875 edge near the pole spanning 98 degrees of RA.
        SkyPosition a = PrecessionB1875.toJ2000(new SkyPosition(111.0, -82.5));
        SkyPosition b = PrecessionB1875.toJ2000(new SkyPosition(209.0, -82.5));
        BoundaryReconstruction.Report report =
                BoundaryReconstruction.reconstructRings(
                        List.of(List.of(a, b)), List.of("Cha"));

        assertEquals(1, report.constantDecEdges());
        assertEquals(0, report.constantRaEdges());
        assertTrue(report.worstChordDeviationDegrees() > 1.0,
                "the straight J2000 chord misses the true edge by degrees");
        assertTrue(report.worstReconstructionDeviationDegrees()
                        <= BoundaryReconstruction.RECONSTRUCTION_TOLERANCE_DEGREES,
                "the sampled polyline stays within the 1-arcmin tolerance");
        // Every reconstructed vertex precesses back onto the B1875 edge.
        for (ConstellationStudyMain.Segment piece : report.reconstructed()) {
            assertEquals(-82.5, PrecessionB1875.toB1875(piece.from()).decDegrees(),
                    1e-6, "reconstructed points lie on the constant-Dec arc");
        }
    }

    @Test
    void sourceChordedChainsAreRestoredToTheTrueConstantDecArc() {
        // The Ursa Minor/Cepheus case: the source replaces the constant-Dec
        // +88 B1875 arc with great-circle chord samples whose interior
        // vertices fit neither constant coordinate. Rebuild that shape.
        SkyPosition start1875 = new SkyPosition(345.0, 88.0);
        SkyPosition end1875 = new SkyPosition(120.0, 88.0);
        SkyPosition start = PrecessionB1875.toJ2000(start1875);
        SkyPosition end = PrecessionB1875.toJ2000(end1875);
        List<SkyPosition> ring = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            ring.add(ConstellationStudyMain.interpolate(start, end, i / 4.0));
        }
        BoundaryReconstruction.Report report =
                BoundaryReconstruction.reconstructRings(
                        List.of(ring), List.of("UMi"));

        assertEquals(1, report.exceptionalChains().size(),
                "the chord chain is classified, not silently accepted");
        assertTrue(report.exceptionalChains().get(0).contains("UMi"));
        for (ConstellationStudyMain.Segment piece : report.reconstructed()) {
            assertEquals(88.0, PrecessionB1875.toB1875(piece.from()).decDegrees(),
                    1e-6, "the restored arc holds constant B1875 declination");
        }
        assertTrue(report.worstReconstructionDeviationDegrees()
                <= BoundaryReconstruction.RECONSTRUCTION_TOLERANCE_DEGREES);
    }

    @Test
    void aShapeFittingNeitherConstantCoordinateIsRefusedNotBent() {
        SkyPosition a = PrecessionB1875.toJ2000(new SkyPosition(10.0, 10.0));
        SkyPosition b = PrecessionB1875.toJ2000(new SkyPosition(20.0, 30.0));
        assertThrows(IllegalStateException.class, () ->
                BoundaryReconstruction.reconstructRings(
                        List.of(List.of(a, b)), List.of("XXX")),
                "boundaries are never silently bent to fit");
    }
}
