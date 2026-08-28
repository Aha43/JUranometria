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
 * The bundled regional catalogue, parsed once at load from the generated
 * classpath resources produced by the import tool (see the PROVENANCE.md
 * and NOTICE files beside the data). Queries filter immutable in-memory
 * lists and never touch files or the network.
 */
public final class BundledCatalogue implements Catalogue {

    private static final String STARS_RESOURCE = "/resources/catalog/m31/stars.csv";
    private static final String DSOS_RESOURCE = "/resources/catalog/m31/dsos.csv";

    private final List<Star> stars;
    private final List<DeepSkyObject> deepSkyObjects;

    private BundledCatalogue(List<Star> stars, List<DeepSkyObject> deepSkyObjects) {
        this.stars = List.copyOf(stars);
        this.deepSkyObjects = List.copyOf(deepSkyObjects);
    }

    /** Loads the generated regional resources from the classpath. */
    public static BundledCatalogue load() {
        return new BundledCatalogue(
                parseStars(records(STARS_RESOURCE, 4)),
                parseDeepSkyObjects(records(DSOS_RESOURCE, 10)));
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

    private static List<Star> parseStars(List<String[]> records) {
        List<Star> stars = new ArrayList<>(records.size());
        for (String[] fields : records) {
            stars.add(new Star(
                    fields[0],
                    new SkyPosition(Double.parseDouble(fields[1]), Double.parseDouble(fields[2])),
                    Double.parseDouble(fields[3])));
        }
        return stars;
    }

    private static List<DeepSkyObject> parseDeepSkyObjects(List<String[]> records) {
        List<DeepSkyObject> deepSkyObjects = new ArrayList<>(records.size());
        for (String[] fields : records) {
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
        return deepSkyObjects;
    }

    private static List<String[]> records(String resource, int fieldCount) {
        InputStream stream = BundledCatalogue.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("bundled catalogue resource missing: " + resource);
        }
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return records(reader, fieldCount, resource);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read bundled catalogue: " + resource, e);
        }
    }

    /** Reads comment-aware CSV records, failing clearly on a malformed line. */
    static List<String[]> records(BufferedReader reader, int fieldCount, String name)
            throws IOException {
        List<String[]> records = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length != fieldCount) {
                throw new IllegalStateException(
                        "malformed catalogue line in " + name + ": " + line);
            }
            records.add(fields);
        }
        return records;
    }
}
