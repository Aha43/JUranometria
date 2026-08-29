package juranometria.geo;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstellationGeographyTest {

    private static final ConstellationGeography GEOGRAPHY =
            ConstellationGeography.load();

    private static final SkyRegion WHOLE_SKY =
            new SkyRegion(new SkyPosition(0.0, 0.0), 180.0);

    @Test
    void theBundledPackLoadsTheEightyEightConstellations() {
        assertEquals(88, GEOGRAPHY.constellations().size());
        Constellation orion = GEOGRAPHY.constellations().stream()
                .filter(c -> c.id().equals("Ori")).findFirst().orElseThrow();
        // Identity checked against the cited source (d3-celestial
        // constellations.json, pinned commit): name, genitive, rank.
        assertEquals("Orion", orion.latinName());
        assertEquals("Orionis", orion.genitive());
        assertEquals(1, orion.rank());
    }

    @Test
    void figureCoordinatesAreCarriedVerbatimFromTheSource() {
        // The source's first Andromeda figure vertex is (30.9748, 42.3297)
        // (GeoJSON longitude 30.9748 -> the same RA). It must survive
        // import exactly at the source's own precision.
        boolean found = GEOGRAPHY.figureSegmentsIn(WHOLE_SKY).stream()
                .filter(s -> s.constellationId().equals("And"))
                .anyMatch(s -> s.from().raDegrees() == 30.9748
                        && s.from().decDegrees() == 42.3297);
        assertTrue(found, "the cited source vertex must be present verbatim");
    }

    @Test
    void boundaryPiecesNearThePoleHoldTheReconstructedConstantDecArc() {
        // The restored Ursa Minor/Cepheus arc: its pieces precess back to
        // B1875 declination +88.0000 within the decision tolerance.
        List<GeoSegment> polar = GEOGRAPHY.boundarySegmentsIn(
                new SkyRegion(new SkyPosition(37.946619, 89.264135), 1.5));
        assertTrue(!polar.isEmpty(), "the pole region has boundary pieces");
        int onArc = 0;
        for (GeoSegment piece : polar) {
            SkyPosition b1875 = juranometria.tool.PrecessionB1875
                    .toB1875(piece.from());
            if (Math.abs(b1875.decDegrees() - 88.0) < 1e-4) {
                onArc++;
            }
        }
        assertTrue(onArc > 0, "restored +88 degree arc pieces are present"
                + " and hold constant B1875 declination");
    }

    @Test
    void queriesFindOrionAroundM42AndRespectTheRegion() {
        SkyRegion m42 = new SkyRegion(new SkyPosition(83.818667, -5.389667), 15.0);
        List<GeoSegment> figures = GEOGRAPHY.figureSegmentsIn(m42);
        assertTrue(figures.stream().anyMatch(
                        s -> s.constellationId().equals("Ori")),
                "Orion's figure is inside the M42 regional query");
        assertTrue(figures.stream().noneMatch(
                        s -> s.constellationId().equals("UMi")),
                "the far northern sky stays outside the region");
    }

    @Test
    void queriesCrossTheRaWrapAndReachBothPoles() {
        // Sculptor's northern border runs along constant declination
        // straight across RA 0; a small region there must see both sides.
        List<GeoSegment> wrap = GEOGRAPHY.boundarySegmentsIn(
                new SkyRegion(new SkyPosition(0.05, -25.0), 2.0));
        assertTrue(wrap.stream().anyMatch(s -> s.from().raDegrees() > 350.0),
                "a region at RA 0 finds pieces west of the wrap");
        assertTrue(wrap.stream().anyMatch(s -> s.from().raDegrees() < 10.0),
                "a region at RA 0 finds pieces east of the wrap");

        assertTrue(!GEOGRAPHY.boundarySegmentsIn(new SkyRegion(
                        new SkyPosition(200.0, 89.7), 2.0)).isEmpty(),
                "the Ursa Minor arc sits within two degrees of the pole");
        assertTrue(GEOGRAPHY.boundarySegmentsIn(new SkyRegion(
                        new SkyPosition(20.0, -89.7), 4.0)).isEmpty(),
                "the south pole's neighbourhood lies inside Octans - the"
                        + " nearest boundary (Mensa's, B1875 dec -85) is"
                        + " farther than four degrees");
        assertTrue(GEOGRAPHY.boundarySegmentsIn(new SkyRegion(
                                new SkyPosition(20.0, -89.7), 5.5)).stream()
                        .anyMatch(s -> s.constellationId().equals("Oct")),
                "widening the polar region reaches Octans' own boundary");
    }

    @Test
    void aSegmentCrossingTheRegionWithBothEndpointsOutsideIsReturned() {
        // Take a long figure segment, query a small region at its middle:
        // both endpoints lie far outside, the arc crosses the region.
        GeoSegment longSegment = GEOGRAPHY.figureSegmentsIn(WHOLE_SKY).stream()
                .filter(s -> separation(s.from(), s.to()) > 15.0)
                .findFirst().orElseThrow();
        SkyPosition middle = midpoint(longSegment.from(), longSegment.to());
        SkyRegion small = new SkyRegion(middle, 1.0);
        assertTrue(separation(longSegment.from(), middle) > 5.0
                        && separation(longSegment.to(), middle) > 5.0,
                "premise: both endpoints are far outside the region");
        assertTrue(GEOGRAPHY.figureSegmentsIn(small).contains(longSegment),
                "the crossing segment is returned without endpoint membership");
    }

    @Test
    void aTamperedResourceFailsItsChecksumLoudly() {
        Function<String, InputStream> tampered = name -> {
            InputStream real = ConstellationGeography.class.getResourceAsStream(
                    ConstellationGeography.RESOURCE_ROOT + name);
            if (!"figures.csv".equals(name)) {
                return real;
            }
            try (real) {
                byte[] bytes = real.readAllBytes();
                bytes[bytes.length - 2] ^= 1;
                return new ByteArrayInputStream(bytes);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ConstellationGeography.load(tampered));
        assertTrue(failure.getMessage().contains("checksum"),
                "corruption is a diagnostic, never a sparse sky: "
                        + failure.getMessage());
    }

    @Test
    void aForeignCoordinateFrameIsRejectedNotMisplaced() {
        // PR #68 review: the declared frame must be enforced - a B1950
        // pack interpreted as J2000 would place geography wrongly.
        Function<String, InputStream> foreign = name -> {
            InputStream real = ConstellationGeography.class.getResourceAsStream(
                    ConstellationGeography.RESOURCE_ROOT + name);
            if (!"manifest.properties".equals(name)) {
                return real;
            }
            try (real) {
                String manifest = new String(real.readAllBytes(),
                        StandardCharsets.UTF_8).replace(
                        "coordinate.frame=ICRS-J2000",
                        "coordinate.frame=FK4-B1950");
                return new ByteArrayInputStream(
                        manifest.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ConstellationGeography.load(foreign));
        assertTrue(failure.getMessage().contains("FK4-B1950"),
                "the diagnostic names the rejected frame: "
                        + failure.getMessage());
    }

    @Test
    void aMissingResourceIsADiagnosticNotANullSky() {
        Function<String, InputStream> missing = name ->
                "boundaries.csv".equals(name) ? null
                        : ConstellationGeography.class.getResourceAsStream(
                                ConstellationGeography.RESOURCE_ROOT + name);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ConstellationGeography.load(missing));
        assertTrue(failure.getMessage().contains("boundaries.csv"));
    }

    @Test
    void theWidestRegionalQueryStaysWellInsideSceneAssemblyBudgets() {
        // The true widest page: a 36-degree field at 900x700 reaches
        // 22.37 degrees at its corners (PR #68 review).
        double halfWidth = Math.tan(Math.toRadians(18.0));
        SkyRegion widest = new SkyRegion(
                new SkyPosition(83.818667, -5.389667),
                Math.toDegrees(Math.atan(
                        Math.hypot(halfWidth, halfWidth * 700.0 / 900.0))));
        GEOGRAPHY.figureSegmentsIn(widest);
        long start = System.nanoTime();
        GEOGRAPHY.figureSegmentsIn(widest);
        GEOGRAPHY.boundarySegmentsIn(widest);
        long elapsed = System.nanoTime() - start;
        assertTrue(elapsed < 100_000_000L,
                "a warm widest-field linear scan must stay far below scene"
                        + " budgets (took " + elapsed / 1e6 + " ms)");
    }

    private static double separation(SkyPosition a, SkyPosition b) {
        double[] u = unit(a);
        double[] v = unit(b);
        return Math.toDegrees(Math.acos(Math.clamp(
                u[0] * v[0] + u[1] * v[1] + u[2] * v[2], -1.0, 1.0)));
    }

    private static SkyPosition midpoint(SkyPosition a, SkyPosition b) {
        double[] u = unit(a);
        double[] v = unit(b);
        double[] m = {u[0] + v[0], u[1] + v[1], u[2] + v[2]};
        double length = Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);
        double ra = Math.toDegrees(Math.atan2(m[1] / length, m[0] / length));
        return new SkyPosition((ra + 360.0) % 360.0,
                Math.toDegrees(Math.asin(m[2] / length)));
    }

    private static double[] unit(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }
}
