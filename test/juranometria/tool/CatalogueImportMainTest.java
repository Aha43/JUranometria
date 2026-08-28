package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import juranometria.chart.SkyRegion;
import juranometria.tool.CatalogueImportMain.Counts;
import juranometria.tool.Tycho2Records.StarRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogueImportMainTest {

    static final SkyRegion REGION = new SkyRegion(
            CatalogueImportMain.M31_CENTRE, CatalogueImportMain.REGION_RADIUS_DEGREES);

    private static StarRow row(String id, double ra, double dec, double vmag) {
        return new StarRow(id, ra, dec, vmag, false, false, false);
    }

    @Test
    void boundarySelectionRespectsRegionAndLimit() {
        List<StarRow> stars = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        Counts counts = new Counts();

        CatalogueImportMain.accept(Optional.of(row("in", 10.7, 41.3, 9.99)),
                REGION, stars, seen, counts);
        CatalogueImportMain.accept(Optional.of(row("at limit", 10.7, 41.3, 10.0)),
                REGION, stars, seen, counts);
        CatalogueImportMain.accept(Optional.of(row("too faint", 10.7, 41.3, 10.01)),
                REGION, stars, seen, counts);
        CatalogueImportMain.accept(Optional.of(row("outside", 10.7, 52.0, 5.0)),
                REGION, stars, seen, counts);
        CatalogueImportMain.accept(Optional.empty(), REGION, stars, seen, counts);

        assertEquals(List.of("in", "at limit"),
                stars.stream().map(StarRow::id).toList());
        assertEquals(1, counts.droppedNoVt);
    }

    @Test
    void anIdentifierCollisionKeepsTheFirstEntryAndCountsTheSkip() {
        List<StarRow> stars = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        Counts counts = new Counts();

        CatalogueImportMain.accept(Optional.of(row("TYC 2794-1098-1", 3.142351, 44.707198, 6.52)),
                REGION, stars, seen, counts);
        CatalogueImportMain.accept(Optional.of(row("TYC 2794-1098-1", 3.142175, 44.707449, 9.77)),
                REGION, stars, seen, counts);

        assertEquals(1, stars.size(), "the main-catalogue entry wins the collision");
        assertEquals(6.52, stars.get(0).vmag());
        assertEquals(1, counts.supplementComponentsSkipped);
    }

    @Test
    void starCsvRenderingIsDeterministic() {
        List<StarRow> stars = List.of(
                row("TYC 1-8-1", 2.31750494, 2.23184345, 12.146),
                row("TYC 1-13-1", 1.12558209, 2.26739400, 8.50638));
        String first = CatalogueImportMain.starsCsv(stars);
        assertEquals(first, CatalogueImportMain.starsCsv(stars));
        assertEquals("""
                # M31-region stars generated from the Tycho-2 Catalogue.
                # Generated resource - do not edit; see PROVENANCE.md and NOTICE-tycho2.md.
                # id,ra_deg,dec_deg,vmag
                TYC 1-8-1,2.317505,2.231843,12.15
                TYC 1-13-1,1.125582,2.267394,8.51
                """, first);
    }

    @Test
    void checksumMismatchFailsClearly() throws Exception {
        Path file = Files.createTempFile("juranometria-import-test", ".dat");
        try {
            Files.writeString(file, "not the pinned bytes");
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> CatalogueImportMain.verifyChecksum(file, "00".repeat(32)));
            assertEquals(true, failure.getMessage().contains("checksum mismatch"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void missingInputFailsClearly() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> CatalogueImportMain.verifyChecksum(
                        Path.of("build/does-not-exist.dat"), "00".repeat(32)));
        assertEquals(true, failure.getMessage().contains("missing pinned input"));
    }
}
