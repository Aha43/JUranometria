package juranometria.catalog;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledCatalogueTest {

    static final SkyPosition M31_CENTRE = new SkyPosition(10.684708, 41.268750);
    static final BundledCatalogue CATALOGUE = BundledCatalogue.load();

    @Test
    void loadsTheCompleteGeneratedRegion() {
        SkyRegion wholeRegion = new SkyRegion(M31_CENTRE, 10.0);
        assertEquals(3204, CATALOGUE.starsIn(wholeRegion).size());
        assertEquals(47, CATALOGUE.deepSkyObjectsIn(wholeRegion).size());
    }

    @Test
    void boundedQueriesIncludeAndExcludeCorrectly() {
        // Mirach (TYC 2286-1329-1) sits about seven degrees from the centre.
        SkyRegion eightDegrees = new SkyRegion(M31_CENTRE, 8.0);
        SkyRegion fiveDegrees = new SkyRegion(M31_CENTRE, 5.0);
        assertTrue(CATALOGUE.starsIn(eightDegrees).stream()
                .anyMatch(star -> star.id().equals("TYC 2286-1329-1")));
        assertTrue(CATALOGUE.starsIn(fiveDegrees).stream()
                .noneMatch(star -> star.id().equals("TYC 2286-1329-1")));

        List<String> nearby = CATALOGUE.deepSkyObjectsIn(new SkyRegion(M31_CENTRE, 1.0))
                .stream().map(DeepSkyObject::id).toList();
        assertTrue(nearby.contains("NGC 224"));
        assertTrue(nearby.contains("NGC 221"), "M32 lies within a degree");
        assertTrue(nearby.contains("NGC 205"), "M110 lies within a degree");
    }

    @Test
    void representativeKnownObjectsCarryTheirCatalogueValues() {
        DeepSkyObject m31 = CATALOGUE.deepSkyObjectsIn(new SkyRegion(M31_CENTRE, 0.1))
                .stream().filter(dso -> dso.id().equals("NGC 224")).findFirst().orElseThrow();
        assertEquals(177.83, m31.majorAxisArcmin());
        assertEquals(69.66, m31.minorAxisArcmin());
        assertEquals(35.0, m31.positionAngleDegrees());
        assertEquals(1, m31.labelPriority());
        assertTrue(m31.aliases().contains("M 31"));
        assertTrue(m31.aliases().contains("Andromeda Galaxy"));

        Star nuAnd = CATALOGUE.starsIn(new SkyRegion(new SkyPosition(12.45353, 41.07891), 0.01))
                .stream().filter(star -> star.id().equals("TYC 2801-2090-1"))
                .findFirst().orElseThrow();
        assertEquals(4.51, nuAnd.magnitude());
    }

    @Test
    void queryResultsAreImmutable() {
        List<Star> stars = CATALOGUE.starsIn(new SkyRegion(M31_CENTRE, 1.0));
        assertThrows(UnsupportedOperationException.class,
                () -> stars.add(new Star("intruder", M31_CENTRE, 1.0)));
    }

    @Test
    void malformedRecordsFailWithAClearDiagnostic() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader(
                "# comment\nTYC 1-1-1,1.0,2.0\n"));
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> BundledCatalogue.records(reader, 4, "test.csv"));
        assertTrue(failure.getMessage().contains("malformed catalogue line in test.csv"));
    }
}
