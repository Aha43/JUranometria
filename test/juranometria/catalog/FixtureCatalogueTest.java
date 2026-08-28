package juranometria.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixtureCatalogueTest {

    static final SkyPosition M31_CENTRE = new SkyPosition(10.684708, 41.268750);

    @Test
    void loadsTheBundledFixtureFromTheClasspath() {
        FixtureCatalogue catalogue = FixtureCatalogue.loadBundled();
        SkyRegion wholeRegion = new SkyRegion(M31_CENTRE, 10.0);
        assertEquals(20, catalogue.starsIn(wholeRegion).size());
        assertEquals(3, catalogue.deepSkyObjectsIn(wholeRegion).size());
    }

    @Test
    void aTightRegionReturnsTheThreeGalaxiesAndExcludesOutsideObjects() {
        FixtureCatalogue catalogue = FixtureCatalogue.loadBundled();
        SkyRegion oneDegree = new SkyRegion(M31_CENTRE, 1.0);

        List<String> dsoIds = catalogue.deepSkyObjectsIn(oneDegree).stream()
                .map(DeepSkyObject::id).toList();
        assertEquals(List.of("M31", "M32", "M110"), dsoIds);

        // The nearest fixture stars (nu And, 32 And) sit more than one
        // degree from the M31 centre.
        assertEquals(List.of(), catalogue.starsIn(oneDegree));
    }

    @Test
    void aFinderRegionReturnsTheNearbyStarsOnly() {
        FixtureCatalogue catalogue = FixtureCatalogue.loadBundled();
        SkyRegion twoDegrees = new SkyRegion(M31_CENTRE, 2.0);

        List<String> starIds = catalogue.starsIn(twoDegrees).stream()
                .map(Star::id).toList();
        assertEquals(List.of("nu And", "32 And"), starIds);
        assertTrue(starIds.stream().noneMatch("bet And"::equals),
                "Mirach lies about seven degrees away and must be excluded");
    }

    @Test
    void m31CarriesDimensionsOrientationAndAliases() {
        FixtureCatalogue catalogue = FixtureCatalogue.loadBundled();
        DeepSkyObject m31 = catalogue.deepSkyObjectsIn(new SkyRegion(M31_CENTRE, 0.1))
                .stream().filter(dso -> dso.id().equals("M31")).findFirst().orElseThrow();

        assertEquals(DsoType.GALAXY, m31.type());
        assertEquals(199.53, m31.majorAxisArcmin());
        assertEquals(70.79, m31.minorAxisArcmin());
        assertEquals(35.0, m31.positionAngleDegrees());
        assertEquals(1, m31.labelPriority());
        assertTrue(m31.aliases().contains("NGC 224"));
    }

    @Test
    void provenanceIsBundledBesideTheData() {
        assertNotNull(FixtureCatalogue.class.getResource("/resources/catalog/PROVENANCE.md"),
                "PROVENANCE.md must ship on the classpath beside the fixture data");
    }
}
