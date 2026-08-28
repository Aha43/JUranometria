package juranometria.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;

/**
 * The bundled, hand-curated M31-region fixture, parsed once from classpath
 * resources at load time. Provenance is recorded beside the data files;
 * see the PROVENANCE.md resource.
 */
public final class FixtureCatalogue implements Catalogue {

    private static final String STARS_RESOURCE = "/resources/catalog/m31-region-stars.csv";
    private static final String DSOS_RESOURCE = "/resources/catalog/m31-region-dsos.csv";

    private final List<Star> stars;
    private final List<DeepSkyObject> deepSkyObjects;

    private FixtureCatalogue(List<Star> stars, List<DeepSkyObject> deepSkyObjects) {
        this.stars = List.copyOf(stars);
        this.deepSkyObjects = List.copyOf(deepSkyObjects);
    }

    /** Loads the bundled fixture from the classpath. */
    public static FixtureCatalogue loadBundled() {
        List<Star> stars = new ArrayList<>();
        for (String[] fields : readRecords(STARS_RESOURCE, 4)) {
            stars.add(new Star(
                    fields[0],
                    new SkyPosition(Double.parseDouble(fields[1]), Double.parseDouble(fields[2])),
                    Double.parseDouble(fields[3])));
        }
        List<DeepSkyObject> deepSkyObjects = new ArrayList<>();
        for (String[] fields : readRecords(DSOS_RESOURCE, 10)) {
            deepSkyObjects.add(new DeepSkyObject(
                    fields[0],
                    fields[1].isEmpty() ? List.of() : List.of(fields[1].split("\\|")),
                    DsoType.valueOf(fields[2]),
                    new SkyPosition(Double.parseDouble(fields[3]), Double.parseDouble(fields[4])),
                    Double.parseDouble(fields[5]),
                    Double.parseDouble(fields[6]),
                    Double.parseDouble(fields[7]),
                    Double.parseDouble(fields[8]),
                    Integer.parseInt(fields[9])));
        }
        return new FixtureCatalogue(stars, deepSkyObjects);
    }

    @Override
    public List<Star> starsIn(SkyRegion region) {
        return stars.stream().filter(star -> region.contains(star.position())).toList();
    }

    @Override
    public List<DeepSkyObject> deepSkyObjectsIn(SkyRegion region) {
        return deepSkyObjects.stream()
                .filter(dso -> region.contains(dso.position()))
                .toList();
    }

    private static List<String[]> readRecords(String resource, int fieldCount) {
        InputStream stream = FixtureCatalogue.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("bundled fixture resource missing: " + resource);
        }
        List<String[]> records = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                if (fields.length != fieldCount) {
                    throw new IllegalStateException(
                            "malformed fixture line in " + resource + ": " + line);
                }
                records.add(fields);
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to read bundled fixture: " + resource, e);
        }
        return records;
    }
}
