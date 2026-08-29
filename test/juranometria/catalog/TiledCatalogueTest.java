package juranometria.catalog;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TiledCatalogueTest {

    static final SkyPosition M31 = new SkyPosition(10.684708, 41.268750);
    static final SkyPosition M42 = new SkyPosition(83.818667, -5.389667);

    @Test
    void queriesReadOnlyIntersectingTiles() {
        TiledCatalogue catalogue = TiledCatalogue.load();
        catalogue.starsIn(new SkyRegion(M31, 7.0));
        assertEquals(java.util.Set.of("r00-d4"), catalogue.loadedTileIds(),
                "the default M31 query reads exactly its one tile");

        catalogue.starsIn(new SkyRegion(new SkyPosition(0.5, 45.0), 5.0));
        assertTrue(catalogue.loadedTileIds().contains("r11-d4"),
                "an RA-wrap query reads the wrap neighbour");
        assertEquals(2, catalogue.loadedTileIds().size(),
                "the wrap query reuses the cached r00-d4 and adds only r11-d4");
    }

    @Test
    void m42OpensACompleteLocalField() {
        TiledCatalogue catalogue = TiledCatalogue.load();
        SkyRegion orion = new SkyRegion(M42, 6.56);
        List<Star> stars = catalogue.starsIn(orion);
        assertTrue(stars.size() > 250, "the Orion field is rich at V 8: " + stars.size());

        DeepSkyObject m42 = catalogue.deepSkyObjectsIn(new SkyRegion(M42, 0.1)).stream()
                .filter(dso -> dso.id().equals("NGC 1976")).findFirst().orElseThrow();
        assertEquals(DsoType.CLUSTER_WITH_NEBULA, m42.type());
        assertEquals(90.0, m42.majorAxisArcmin());
        assertTrue(m42.aliases().contains("M 42"));
    }

    @Test
    void polarAndSouthernQueriesReturnDeterministicUniqueResults() {
        TiledCatalogue catalogue = TiledCatalogue.load();
        List<Star> polar = catalogue.starsIn(
                new SkyRegion(new SkyPosition(10.0, 89.0), 2.0));
        assertTrue(polar.stream().anyMatch(star -> star.id().equals("TYC 4628-237-1")),
                "Polaris is found over the pole");
        assertEquals(polar, catalogue.starsIn(new SkyRegion(new SkyPosition(10.0, 89.0), 2.0)),
                "repeated queries are deterministic");
        assertEquals(polar.stream().map(Star::id).distinct().count(), polar.size(),
                "no duplicates across polar tile boundaries");

        List<DeepSkyObject> southern = catalogue.deepSkyObjectsIn(
                new SkyRegion(new SkyPosition(6.0, -72.0), 3.0));
        assertTrue(southern.stream().anyMatch(dso -> dso.id().equals("NGC 104")),
                "47 Tucanae anchors the southern field");
    }

    @Test
    void unknownValuesMapToTheDocumentedDisplayDecisions() {
        TiledCatalogue catalogue = TiledCatalogue.load();
        DeepSkyObject ngc7801 = catalogue.deepSkyObjectsIn(
                        new SkyRegion(new SkyPosition(0.089333, 50.745), 0.05)).stream()
                .filter(dso -> dso.id().equals("NGC 7801")).findFirst().orElseThrow();
        assertEquals(DsoType.OPEN_CLUSTER, ngc7801.type());
        assertEquals(4.20, ngc7801.majorAxisArcmin(), "the recorded major axis survives");
        assertEquals(4.20, ngc7801.minorAxisArcmin(), "absent minor mirrors major");
        assertEquals(0.0, ngc7801.positionAngleDegrees(), "absent angle displays as 0");
        assertTrue(Double.isNaN(ngc7801.magnitude()),
                "absent photometry stays honestly unknown");
    }

    /** A tiny in-memory pack for failure-honesty tests. */
    private static Function<String, InputStream> pack(Map<String, String> files) {
        return name -> {
            String content = files.get(name);
            return content == null ? null
                    : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        };
    }

    private static Map<String, String> validTinyPack() {
        String tile = "# c\nTYC 1-1-1,10.000000,40.000000,5.00\n";
        String checksum = juranometria.tool.TestHashes.sha256(tile);
        Map<String, String> files = new HashMap<>();
        files.put("manifest.properties", """
                format.version=1
                pack.name=bright-sky
                coverage.type=all-sky
                stars.limit.vmag=8.0
                tiling.scheme=radec-grid-30
                sources.tycho2.catalogue=I/259
                sources.openngc.release=v20260501
                license.stars=CC BY-NC 3.0 IGO
                license.dsos=CC-BY-SA-4.0
                checksum.tiles/r00-d4/stars.csv=""" + checksum + "\n");
        files.put("tiles/r00-d4/stars.csv", tile);
        return files;
    }

    @Test
    void integrityFailuresAreClearNeverSparse() {
        Map<String, String> missingTile = validTinyPack();
        missingTile.remove("tiles/r00-d4/stars.csv");
        TiledCatalogue broken = TiledCatalogue.load(pack(missingTile));
        IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> broken.starsIn(new SkyRegion(M31, 1.0)));
        assertTrue(missing.getMessage().contains("missing"));

        Map<String, String> corrupted = validTinyPack();
        corrupted.put("tiles/r00-d4/stars.csv", "# c\nTYC 1-1-1,10.000000,40.000000,5.01\n");
        TiledCatalogue tampered = TiledCatalogue.load(pack(corrupted));
        IllegalStateException mismatch = assertThrows(IllegalStateException.class,
                () -> tampered.starsIn(new SkyRegion(M31, 1.0)));
        assertTrue(mismatch.getMessage().contains("checksum"));

        Map<String, String> incompatible = validTinyPack();
        incompatible.put("manifest.properties",
                incompatible.get("manifest.properties").replace(
                        "format.version=1", "format.version=99"));
        assertThrows(IllegalArgumentException.class,
                () -> TiledCatalogue.load(pack(incompatible)));

        assertThrows(IllegalStateException.class,
                () -> TiledCatalogue.load(pack(Map.of())),
                "a pack without a manifest fails at load");
    }

    @Test
    void aValidTinyPackServesItsStar() {
        TiledCatalogue tiny = TiledCatalogue.load(pack(validTinyPack()));
        List<Star> stars = tiny.starsIn(new SkyRegion(new SkyPosition(10.0, 40.0), 1.0));
        assertEquals("TYC 1-1-1", stars.get(0).id());
    }
}
