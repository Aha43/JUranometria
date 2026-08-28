package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards over the complete generated regional catalogue as bundled on the
 * classpath, independent of how the importer produced it.
 */
class GeneratedCatalogueTest {

    @Test
    void everyGeneratedStarIdentifierIsUnique() {
        List<String> ids = firstFields("/resources/catalog/m31/stars.csv");
        Set<String> unique = new HashSet<>(ids);
        assertEquals(ids.size(), unique.size(),
                "duplicate star identifiers in the generated catalogue");
        assertTrue(ids.size() > 3000, "the regional star catalogue looks truncated");
    }

    @Test
    void everyGeneratedDsoIdentifierIsUnique() {
        List<String> ids = firstFields("/resources/catalog/m31/dsos.csv");
        Set<String> unique = new HashSet<>(ids);
        assertEquals(ids.size(), unique.size(),
                "duplicate DSO identifiers in the generated catalogue");
        assertTrue(ids.contains("NGC 224"), "M31 itself must be present");
    }

    @Test
    void everyBundledRowLiesInsideTheDeclaredCoverage() {
        // The scene assembler's coverage rule promises data to 10 degrees
        // of the M31 centre; this ties that constant to the actual rows.
        juranometria.chart.SkyRegion coverage = new juranometria.chart.SkyRegion(
                new juranometria.chart.SkyPosition(10.684708, 41.268750), 10.0);
        for (String resource : new String[] {
                "/resources/catalog/m31/stars.csv", "/resources/catalog/m31/dsos.csv"}) {
            for (double[] position : positions(resource)) {
                assertTrue(coverage.contains(
                                new juranometria.chart.SkyPosition(position[0], position[1])),
                        resource + " holds a row outside the declared coverage");
            }
        }
    }

    private static List<double[]> positions(String resource) {
        List<double[]> positions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                assertResource(resource), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                // Stars carry ra,dec at indices 1,2; DSOs at 3,4.
                int raIndex = fields.length == 4 ? 1 : 3;
                positions.add(new double[] {
                        Double.parseDouble(fields[raIndex]),
                        Double.parseDouble(fields[raIndex + 1])});
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return positions;
    }

    private static List<String> firstFields(String resource) {
        List<String> ids = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                assertResource(resource), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                ids.add(line.substring(0, line.indexOf(',')));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ids;
    }

    private static java.io.InputStream assertResource(String resource) {
        java.io.InputStream stream = GeneratedCatalogueTest.class.getResourceAsStream(resource);
        assertNotNull(stream, resource + " must ship on the classpath");
        return stream;
    }
}
