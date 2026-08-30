package juranometria.tool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.geo.ConstellationGeography;
import juranometria.geo.GeoSegment;

/**
 * Generates the bundled constellation-geography pack from the pinned
 * d3-celestial sources, per docs/decisions/constellation-geography.md:
 * verifies the raw inputs' SHA-256, carries identities and line-figure
 * segments faithfully (coordinates verbatim at the source's own
 * precision), reconstructs the IAU boundaries along their B1875
 * constant-coordinate edges within the decision's one-arcminute
 * tolerance, and writes plain CSV resources with a checksummed
 * manifest and the licence and attribution notices beside them.
 *
 * Deterministic by construction: no timestamps, fixed Locale.ROOT
 * formatting, input order preserved - two clean runs are
 * byte-identical. Run via "make import-constellations".
 */
public final class ConstellationPackMain {

    static final String PACK_NAME = "constellation-geography";
    static final String SOURCE_COMMIT = "7e720a3de062059d4c5400a379146a601d9010e0";

    /**
     * Recorded source errata (Codex review, PR #119): the pinned
     * source carries a factually wrong value; the pack corrects it,
     * records the correction in the manifest and notice, and refuses
     * to run if the source stops matching the recorded wrong value -
     * an erratum is never a silent patch and never survives source
     * drift unexamined. Crux's Latin genitive is Crucis; the source's
     * "gen" field repeats the nominative.
     */
    static final Map<String, String[]> GENITIVE_ERRATA = Map.of(
            "Cru", new String[] {"Crux", "Crucis"});

    /** The pinned raw inputs this pack is generated from. */
    static final Map<String, String> PINNED = Map.of(
            "constellations.json",
            "ab4ae692027cbc042c0d6791a84456a65eb7c55656107fd00c58ff6e55d4d8b2",
            "constellations.lines.json",
            "294f66bef5d5cf50b1e17f16d2efa1d97a15131612c68dd935adef6e7373e13c",
            "constellations.bounds.json",
            "f2e2687af6b20b24567879f838c21874d412efcc93ecc1966be07e78431cc196",
            "LICENSE",
            "a8c79239001ad4bea243d1796b85084e1fc39309c5c5a663930a1b46320254c6");

    private ConstellationPackMain() {
    }

    public static void main(String[] args) throws Exception {
        Path rawDir = Path.of(args.length > 0 ? args[0]
                : "imports/raw/constellations");
        Path outDir = Path.of(args.length > 1 ? args[1]
                : "src/resources/geo/constellations");

        for (Map.Entry<String, String> pinned : new TreeMap<>(PINNED).entrySet()) {
            Path file = rawDir.resolve(pinned.getKey());
            if (!Files.exists(file)) {
                throw new IllegalStateException("missing raw input " + file
                        + "; run scripts/download-constellation-sources.sh");
            }
            String actual = PinnedInputs.sha256Hex(Files.readAllBytes(file));
            if (!pinned.getValue().equals(actual)) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "raw input %s fails its pinned checksum: expected %s,"
                                + " found %s", pinned.getKey(),
                        pinned.getValue(), actual));
            }
        }

        // Identities: the 88 constellations; Serpens appears twice in the
        // source (its two sky parts) under one identity.
        List<String[]> identities = new ArrayList<>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (Object feature : features(rawDir.resolve("constellations.json"))) {
            Map<String, Object> f = MiniJson.object(feature);
            String id = (String) f.get("id");
            Map<String, Object> p = MiniJson.object(f.get("properties"));
            if (!id.equals(p.get("desig"))) {
                throw new IllegalStateException("id and IAU designation differ"
                        + " for " + id + "; the pack treats them as one");
            }
            if (!seen.add(id)) {
                continue;
            }
            String genitive = (String) p.get("gen");
            String[] erratum = GENITIVE_ERRATA.get(id);
            if (erratum != null) {
                if (!erratum[0].equals(genitive)) {
                    throw new IllegalStateException("recorded genitive"
                            + " erratum for " + id + " expects the source to"
                            + " carry '" + erratum[0] + "' but it carries '"
                            + genitive + "'; re-examine the erratum");
                }
                genitive = erratum[1];
            }
            String[] row = {id, (String) p.get("name"), genitive,
                    (String) p.get("rank")};
            requireNoCommas(row);
            identities.add(row);
        }
        if (identities.size() != 88) {
            throw new IllegalStateException("expected the 88 IAU"
                    + " constellations, found " + identities.size());
        }

        // Line figures: coordinates carried verbatim at source precision.
        List<String[]> figures = new ArrayList<>();
        for (Object feature : features(rawDir.resolve("constellations.lines.json"))) {
            Map<String, Object> f = MiniJson.object(feature);
            String id = (String) f.get("id");
            Map<String, Object> geometry = MiniJson.object(f.get("geometry"));
            for (Object lineObject : MiniJson.array(geometry.get("coordinates"))) {
                List<Object> line = MiniJson.array(lineObject);
                for (int i = 1; i < line.size(); i++) {
                    figures.add(new String[] {id,
                            raOf(line.get(i - 1)), decOf(line.get(i - 1)),
                            raOf(line.get(i)), decOf(line.get(i))});
                }
            }
        }

        // Boundaries: reconstructed along B1875 constant-coordinate edges.
        List<String> ringIds = new ArrayList<>();
        List<List<SkyPosition>> rings = ConstellationStudyMain.loadBoundaryRings(
                rawDir.resolve("constellations.bounds.json").toFile(), ringIds);
        BoundaryReconstruction.Report report =
                BoundaryReconstruction.reconstructRings(rings, ringIds);
        if (report.worstReconstructionDeviationDegrees()
                > BoundaryReconstruction.RECONSTRUCTION_TOLERANCE_DEGREES) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "boundary reconstruction exceeds the decision tolerance:"
                            + " %.4f arcmin",
                    report.worstReconstructionDeviationDegrees() * 60.0));
        }
        List<String[]> boundaries = new ArrayList<>();
        for (ConstellationStudyMain.Segment piece : report.reconstructed()) {
            boundaries.add(new String[] {piece.constellation(),
                    format6(piece.from().raDegrees()),
                    format6(piece.from().decDegrees()),
                    format6(piece.to().raDegrees()),
                    format6(piece.to().decDegrees())});
        }

        ensureSafeToClean(outDir);
        Files.createDirectories(outDir);

        Map<String, String> checksums = new LinkedHashMap<>();
        write(outDir.resolve("constellations.csv"),
                csv("id,latin,genitive,rank", identities), checksums);
        write(outDir.resolve("figures.csv"),
                csv("id,ra1,dec1,ra2,dec2", figures), checksums);
        write(outDir.resolve("boundaries.csv"),
                csv("id,ra1,dec1,ra2,dec2", boundaries), checksums);
        Files.copy(rawDir.resolve("LICENSE"),
                outDir.resolve("LICENSE-BSD-3-Clause.txt"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        write(outDir.resolve("NOTICE-constellations.md"), notice(), null);
        write(outDir.resolve("manifest.properties"), manifest(
                identities.size(), figures.size(), boundaries.size(),
                report, checksums), null);

        System.out.printf(Locale.ROOT,
                "wrote %d identities, %d figure segments, %d boundary pieces"
                        + " (worst reconstruction deviation %.4f arcmin)%n",
                identities.size(), figures.size(), boundaries.size(),
                report.worstReconstructionDeviationDegrees() * 60.0);
        for (Map.Entry<String, String> checksum : checksums.entrySet()) {
            System.out.println("  " + checksum.getValue() + "  "
                    + checksum.getKey());
        }
        measureQueries(outDir);
    }

    /** Loads the generated pack back and times the 36-degree queries. */
    private static void measureQueries(Path outDir) {
        ConstellationGeography geography = ConstellationGeography.load(name -> {
            try {
                Path file = outDir.resolve(name);
                return Files.exists(file) ? Files.newInputStream(file) : null;
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        // The real widest page: a 36-degree field at 900x700 reaches
        // atan(hypot(tan 18, tan 18 * 700/900)) = 22.37 degrees at the
        // corners - the radius scene assembly will actually ask for.
        double halfWidth = Math.tan(Math.toRadians(18.0));
        double corner = Math.toDegrees(Math.atan(
                Math.hypot(halfWidth, halfWidth * 700.0 / 900.0)));
        SkyRegion widest = new SkyRegion(
                new SkyPosition(83.818667, -5.389667), corner);
        geography.figureSegmentsIn(widest);
        geography.boundarySegmentsIn(widest);
        long t0 = System.nanoTime();
        List<GeoSegment> figures = geography.figureSegmentsIn(widest);
        long t1 = System.nanoTime();
        List<GeoSegment> boundaries = geography.boundarySegmentsIn(widest);
        long t2 = System.nanoTime();
        System.out.printf(Locale.ROOT,
                "verified reload; warm widest-page query (36-degree field,"
                        + " %.2f-degree corner radius): %d figure segments in"
                        + " %.2f ms, %d boundary pieces in %.2f ms - linear"
                        + " scan, no index needed%n",
                corner, figures.size(), (t1 - t0) / 1e6,
                boundaries.size(), (t2 - t1) / 1e6);
    }

    private static String csv(String header, List<String[]> rows) {
        StringBuilder out = new StringBuilder(header).append('\n');
        for (String[] row : rows) {
            out.append(String.join(",", row)).append('\n');
        }
        return out.toString();
    }

    private static void write(Path path, String content,
                              Map<String, String> checksums) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes);
        if (checksums != null) {
            checksums.put(path.getFileName().toString(),
                    PinnedInputs.sha256Hex(bytes));
        }
    }

    private static void requireNoCommas(String[] row) {
        for (String field : row) {
            if (field == null || field.contains(",")) {
                throw new IllegalStateException(
                        "field breaks the CSV: " + java.util.Arrays.toString(row));
            }
        }
    }

    private static String raOf(Object coordinates) {
        double lon = MiniJson.number(MiniJson.array(coordinates).get(0));
        return format4((lon + 360.0) % 360.0);
    }

    private static String decOf(Object coordinates) {
        return format4(MiniJson.number(MiniJson.array(coordinates).get(1)));
    }

    private static String format4(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String format6(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static List<Object> features(Path file) throws IOException {
        Map<String, Object> root = MiniJson.object(
                MiniJson.parse(Files.readString(file)));
        return MiniJson.array(root.get("features"));
    }

    private static String manifest(int constellations, int figures,
                                   int boundaries,
                                   BoundaryReconstruction.Report report,
                                   Map<String, String> checksums) {
        Map<String, String> entries = new TreeMap<>();
        entries.put("format.version", "1");
        entries.put("pack.name", PACK_NAME);
        entries.put("coordinate.frame", "ICRS-J2000");
        entries.put("layers", "identities,figures,boundaries");
        entries.put("rows.constellations", Integer.toString(constellations));
        entries.put("rows.figure.segments", Integer.toString(figures));
        entries.put("rows.boundary.segments", Integer.toString(boundaries));
        entries.put("boundary.reconstruction",
                "B1875-constant-coordinate, IAU-1976 precession, 1.0 deg step");
        entries.put("boundary.tolerance.arcmin", "1.0");
        entries.put("boundary.worst.deviation.arcmin", String.format(Locale.ROOT,
                "%.4f", report.worstReconstructionDeviationDegrees() * 60.0));
        entries.put("source", "d3-celestial (Olaf Frohn)");
        for (Map.Entry<String, String[]> erratum
                : new TreeMap<>(GENITIVE_ERRATA).entrySet()) {
            entries.put("erratum.genitive." + erratum.getKey(),
                    erratum.getValue()[1] + " (source carries "
                            + erratum.getValue()[0] + ")");
        }
        entries.put("source.commit", SOURCE_COMMIT);
        entries.put("license", "BSD-3-Clause");
        entries.put("audit.date", "2026-08-29");
        for (Map.Entry<String, String> checksum : checksums.entrySet()) {
            entries.put("checksum." + checksum.getKey(), checksum.getValue());
        }
        StringBuilder lines = new StringBuilder();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            lines.append(entry.getKey()).append('=')
                    .append(entry.getValue()).append('\n');
        }
        return lines.toString();
    }

    private static String notice() {
        return """
                # Notice: constellation-geography data

                The constellation identities, traditional line figures, and
                boundary data in this pack are derived from **d3-celestial**
                by Olaf Frohn, https://github.com/ofrohn/d3-celestial,
                pinned at commit `%s`,
                and redistributed under the **BSD-3-Clause** licence (full
                text in `LICENSE-BSD-3-Clause.txt` beside this notice).

                Provenance, per docs/decisions/constellation-geography.md:

                - **Boundaries** digitize Eugene Delporte's IAU-adopted 1930
                  delimitation (corner-data lineage of Davenhall & Leggett
                  1989, VizieR VI/49). This pack reconstructs the edges along
                  their constant-RA/Dec B1875 arcs (IAU-1976 precession) to
                  within one arcminute of the true boundary.
                - **Line figures** follow the convention drawn on the
                  IAU/Sky & Telescope constellation charts (published under
                  CC BY 4.0), with Olaf Frohn's modifications. Stick figures
                  are an editorial convention, not an IAU standard.
                - **Names and abbreviations** are the IAU's standard Latin
                  names for the 88 constellations. One recorded erratum:
                  the source's genitive for Crux repeats the nominative
                  ("Crux"); this pack carries the correct Latin genitive
                  **Crucis**, with the correction declared in the
                  manifest.
                """.formatted(SOURCE_COMMIT);
    }

    /**
     * Refuses to clean a directory the generator does not own: an
     * existing non-empty output location must carry this pack's manifest
     * as an ownership marker (the bright-sky generator's rule).
     */
    static void ensureSafeToClean(Path outDir) throws IOException {
        if (!Files.exists(outDir)) {
            return;
        }
        try (var listing = Files.list(outDir)) {
            if (listing.findAny().isEmpty()) {
                return;
            }
        }
        Path manifest = outDir.resolve("manifest.properties");
        if (!Files.exists(manifest)) {
            throw new IllegalStateException(outDir + " is not empty and has"
                    + " no manifest; refusing to delete files this generator"
                    + " does not own");
        }
        try (InputStream stream = Files.newInputStream(manifest)) {
            java.util.Properties properties = new java.util.Properties();
            properties.load(stream);
            if (!PACK_NAME.equals(properties.getProperty("pack.name"))) {
                throw new IllegalStateException(outDir + " belongs to pack "
                        + properties.getProperty("pack.name")
                        + "; refusing to clean");
            }
        }
        try (var walk = Files.walk(outDir)) {
            for (Path path : walk.sorted(java.util.Comparator.reverseOrder())
                    .toList()) {
                Files.delete(path);
            }
        }
    }
}
