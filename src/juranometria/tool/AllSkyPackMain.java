package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import juranometria.catalog.PackManifest;
import juranometria.catalog.SkyTiling;
import juranometria.chart.SkyPosition;
import juranometria.tool.OpenNgcRecords.DsoRow;
import juranometria.tool.Tycho2Records.StarRow;

/**
 * Generates the built-in bright all-sky catalogue pack defined by
 * docs/decisions/all-sky-tiling.md: stars to Johnson V 8.0 from the pinned
 * Tycho-2 inputs and the complete eligible OpenNGC object set, partitioned
 * into the radec-grid-30 tiles, with a versioned manifest carrying
 * per-file checksums, generated notices, and provenance. Run via
 * "make import-allsky" after scripts/download-catalogue-sources.sh.
 *
 * Output is deterministic: identical pinned inputs reproduce every file
 * byte-identically. The tool is never run by the application.
 */
public final class AllSkyPackMain {

    static final String PACK_NAME = "bright-sky";
    static final double STAR_LIMIT_V = 8.0;

    private AllSkyPackMain() {
    }

    public static void main(String[] args) throws Exception {
        Path rawDir = Path.of(args.length > 0 ? args[0] : "imports/raw");
        Path outDir = Path.of(args.length > 1 ? args[1]
                : "src/resources/catalog/" + PACK_NAME);

        PinnedInputs.verifyAll(rawDir);

        PackCounts counts = new PackCounts();
        Map<String, List<StarRow>> starTiles = collectStars(rawDir, counts);
        Map<String, List<String[]>> dsoTiles = collectDsos(rawDir, counts);

        // The generator owns outDir completely: regenerate from clean so a
        // changed catalogue can never leave stale, unchecksummed files.
        ensureSafeToClean(outDir);
        deleteRecursively(outDir);
        Files.createDirectories(outDir.resolve("tiles"));
        Map<String, String> checksums = new TreeMap<>();
        long dataBytes = 0;
        Set<String> tileIds = new java.util.TreeSet<>();
        tileIds.addAll(starTiles.keySet());
        tileIds.addAll(dsoTiles.keySet());
        for (String tileId : tileIds) {
            Path tileDir = outDir.resolve("tiles").resolve(tileId);
            Files.createDirectories(tileDir);
            if (starTiles.containsKey(tileId)) {
                dataBytes += write(tileDir.resolve("stars.csv"),
                        starsCsv(starTiles.get(tileId)), checksums, tileId + "/stars.csv");
            }
            if (dsoTiles.containsKey(tileId)) {
                dataBytes += write(tileDir.resolve("dsos.csv"),
                        dsosCsv(dsoTiles.get(tileId)), checksums, tileId + "/dsos.csv");
            }
        }

        write(outDir.resolve("manifest.properties"),
                manifest(counts, checksums), null, null);
        write(outDir.resolve("PROVENANCE.md"),
                PackNotices.provenance(counts, tileIds.size()), null, null);
        write(outDir.resolve("NOTICE-tycho2.md"), PackNotices.tycho2(), null, null);
        write(outDir.resolve("NOTICE-openngc.md"), PackNotices.openNgc(), null, null);
        Files.write(outDir.resolve("LICENSE-CC-BY-SA-4.0.txt"),
                Files.readAllBytes(rawDir.resolve("openngc-CC-BY-SA-4.0.txt")));

        System.out.printf(Locale.ROOT,
                "Wrote %s: %d stars, %d DSOs, %d tiles, %.2f MiB of tile data%n",
                outDir, counts.starsWritten, counts.dsosWritten, tileIds.size(),
                dataBytes / 1_048_576.0);
    }

    /** Normalization counters recorded in the generated provenance. */
    static final class PackCounts {
        int starsWritten;
        int mainStars;
        int supplementStars;
        int fallbackPositions;
        int vtWithoutBt;
        int hpMagnitudes;
        int droppedNoVt;
        int supplementComponentsSkipped;
        int dsosWritten;
        int skippedDupNonEx;
        int droppedNoPosition;
        int dsosWithoutAnyMagnitude;
        int dsosWithoutVMagnitude;
        int dsosWithoutDimensions;
        int dsosWithoutPositionAngle;
        double maxObjectSemiExtentDegrees;
        final Map<String, Integer> dsoTypes = new TreeMap<>();
    }

    private static Map<String, List<StarRow>> collectStars(Path rawDir, PackCounts counts)
            throws IOException {
        Map<String, List<StarRow>> tiles = new TreeMap<>();
        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i <= 19; i++) {
            Path file = rawDir.resolve(String.format(Locale.ROOT, "tyc2.dat.%02d.gz", i));
            PinnedInputs.readGzLines(file, line ->
                    acceptStar(Tycho2Records.fromMainLine(line), tiles, seenIds, counts));
        }
        counts.mainStars = counts.starsWritten;
        PinnedInputs.readGzLines(rawDir.resolve("suppl_1.dat.gz"), line ->
                acceptStar(Tycho2Records.fromSupplementLine(line), tiles, seenIds, counts));
        counts.supplementStars = counts.starsWritten - counts.mainStars;
        if (seenIds.size() != counts.starsWritten) {
            throw new IllegalStateException("duplicate star identifiers survived the pack");
        }
        for (List<StarRow> tile : tiles.values()) {
            tile.sort(Comparator.comparingDouble(StarRow::vmag).thenComparing(StarRow::id));
        }
        return tiles;
    }

    private static void acceptStar(java.util.Optional<StarRow> parsed,
                                   Map<String, List<StarRow>> tiles,
                                   Set<String> seenIds, PackCounts counts) {
        if (parsed.isEmpty()) {
            counts.droppedNoVt++;
            return;
        }
        StarRow row = parsed.get();
        if (row.vmag() > STAR_LIMIT_V) {
            return;
        }
        // Component policy from the Sprint 3 contract: the main catalogue
        // wins an identifier collision with supplement-1.
        if (!seenIds.add(row.id())) {
            counts.supplementComponentsSkipped++;
            return;
        }
        if (row.usedFallbackPosition()) {
            counts.fallbackPositions++;
        }
        if (row.usedVtWithoutBt()) {
            counts.vtWithoutBt++;
        }
        if (row.usedHpMagnitude()) {
            counts.hpMagnitudes++;
        }
        String tileId = SkyTiling.tileId(new SkyPosition(row.raDegrees(), row.decDegrees()));
        tiles.computeIfAbsent(tileId, key -> new ArrayList<>()).add(row);
        counts.starsWritten++;
    }

    private static Map<String, List<String[]>> collectDsos(Path rawDir, PackCounts counts)
            throws IOException {
        Map<String, List<String[]>> tiles = new TreeMap<>();
        Set<String> seenIds = new HashSet<>();
        for (String file : new String[] {"openngc-NGC.csv", "openngc-addendum.csv"}) {
            List<String> lines = Files.readAllLines(rawDir.resolve(file), StandardCharsets.UTF_8);
            Map<String, Integer> header = OpenNgcRecords.headerIndex(lines.get(0));
            for (String line : lines.subList(1, lines.size())) {
                if (line.isBlank()) {
                    continue;
                }
                DsoRow dso = OpenNgcRecords.fromLine(line, header);
                if (dso.type().equals("Dup") || dso.type().equals("NonEx")) {
                    counts.skippedDupNonEx++;
                    continue;
                }
                if (Double.isNaN(dso.raDegrees()) || Double.isNaN(dso.decDegrees())) {
                    counts.droppedNoPosition++;
                    continue;
                }
                String[] row = dsoRow(dso, counts);
                if (!seenIds.add(dso.id())) {
                    throw new IllegalStateException("duplicate DSO identifier: " + dso.id());
                }
                String tileId = SkyTiling.tileId(
                        new SkyPosition(dso.raDegrees(), dso.decDegrees()));
                tiles.computeIfAbsent(tileId, key -> new ArrayList<>()).add(row);
                counts.dsosWritten++;
                counts.dsoTypes.merge(dso.type(), 1, Integer::sum);
            }
        }
        for (List<String[]> tile : tiles.values()) {
            tile.sort(Comparator.comparing(row -> row[0]));
        }
        return tiles;
    }

    /**
     * Normalizes any usable OpenNGC type into the pack's row format.
     * Unknown values stay explicitly empty - the pack preserves facts and
     * never invents dimensions, angles, or magnitudes - and positioned
     * objects without any photometry are kept (Codex review, PR #45).
     */
    private static String[] dsoRow(DsoRow dso, PackCounts counts) {
        if (Double.isNaN(dso.vmag()) && Double.isNaN(dso.bmag())) {
            counts.dsosWithoutAnyMagnitude++;
        } else if (Double.isNaN(dso.vmag())) {
            counts.dsosWithoutVMagnitude++;
        }
        if (Double.isNaN(dso.majorAxisArcmin())) {
            counts.dsosWithoutDimensions++;
        }
        double pa = dso.positionAngleDegrees();
        if (Double.isNaN(pa)) {
            counts.dsosWithoutPositionAngle++;
        } else {
            pa = ((pa % 180.0) + 180.0) % 180.0;
        }
        if (!Double.isNaN(dso.majorAxisArcmin())) {
            counts.maxObjectSemiExtentDegrees = Math.max(
                    counts.maxObjectSemiExtentDegrees, dso.majorAxisArcmin() / 120.0);
        }
        boolean messier = dso.aliases().stream().anyMatch(alias -> alias.startsWith("M "));
        return new String[] {
                dso.id(),
                String.join("|", dso.aliases()),
                dso.type(),
                String.format(Locale.ROOT, "%.6f", dso.raDegrees()),
                String.format(Locale.ROOT, "%.6f", dso.decDegrees()),
                optional(dso.majorAxisArcmin(), "%.2f"),
                optional(dso.minorAxisArcmin(), "%.2f"),
                Double.isNaN(dso.positionAngleDegrees()) ? ""
                        : String.format(Locale.ROOT, "%.1f", pa),
                optional(dso.vmag(), "%.2f"),
                optional(dso.bmag(), "%.2f"),
                messier ? "1" : "2",
        };
    }

    private static String optional(double value, String format) {
        return Double.isNaN(value) ? "" : String.format(Locale.ROOT, format, value);
    }

    private static String starsCsv(List<StarRow> stars) {
        StringBuilder csv = new StringBuilder();
        csv.append("# Bright-sky pack star tile generated from the Tycho-2 Catalogue.\n");
        csv.append("# Generated resource - do not edit; see ../../PROVENANCE.md.\n");
        csv.append("# id,ra_deg,dec_deg,vmag\n");
        for (StarRow star : stars) {
            csv.append(String.format(Locale.ROOT, "%s,%.6f,%.6f,%.2f\n",
                    star.id(), star.raDegrees(), star.decDegrees(), star.vmag()));
        }
        return csv.toString();
    }

    /**
     * The comma-separated row format cannot represent a comma inside a
     * field; the generator fails rather than emitting a malformed row
     * (Codex review, PR #45 follow-up).
     */
    private static void requireNoCommas(String[] row) {
        for (String field : row) {
            if (field.indexOf(',') >= 0) {
                throw new IllegalStateException(
                        "field contains a comma the row format cannot represent: "
                                + field + " (row " + row[0] + ")");
            }
        }
    }

    private static String dsosCsv(List<String[]> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("# Bright-sky pack DSO tile generated from OpenNGC.\n");
        csv.append("# Generated resource - do not edit; see ../../PROVENANCE.md.\n");
        csv.append("# id,aliases(|-separated),type(OpenNGC token),ra_deg,dec_deg,"
                + "major_arcmin,minor_arcmin,pa_deg,vmag,bmag,label_priority\n");
        csv.append("# Empty fields mean the source records no value.\n");
        for (String[] row : rows) {
            requireNoCommas(row);
            csv.append(String.join(",", row)).append('\n');
        }
        return csv.toString();
    }

    private static String manifest(PackCounts counts, Map<String, String> checksums) {
        StringBuilder lines = new StringBuilder();
        Map<String, String> entries = new TreeMap<>();
        entries.put("format.version", "1");
        entries.put("pack.name", PACK_NAME);
        entries.put("coverage.type", "all-sky");
        entries.put("stars.limit.vmag",
                String.format(Locale.ROOT, "%.1f", STAR_LIMIT_V));
        entries.put("tiling.scheme", PackManifest.TILING_SCHEME);
        entries.put("layers", "stars,dsos");
        // Rounded up so the declared value is always a safe query margin.
        entries.put("objects.max.semi.extent.degrees", String.format(Locale.ROOT,
                "%.2f", Math.ceil(counts.maxObjectSemiExtentDegrees * 100.0) / 100.0));
        entries.put("rows.stars.total", Integer.toString(counts.starsWritten));
        entries.put("rows.dsos.total", Integer.toString(counts.dsosWritten));
        entries.put("sources.tycho2.catalogue", "I/259");
        entries.put("sources.openngc.release", "v20260501");
        entries.put("license.stars", "CC BY-NC 3.0 IGO");
        entries.put("license.dsos", "CC-BY-SA-4.0");
        entries.put("audit.date", "2026-08-29");
        for (Map.Entry<String, String> checksum : checksums.entrySet()) {
            entries.put("checksum.tiles/" + checksum.getKey(), checksum.getValue());
        }
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            lines.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return lines.toString();
    }

    private static long write(Path path, String content,
                              Map<String, String> checksums, String checksumKey)
            throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes);
        if (checksums != null) {
            checksums.put(checksumKey, PinnedInputs.sha256Hex(bytes));
        }
        return bytes.length;
    }

    /**
     * Refuses to clean a directory the generator does not own: an existing
     * non-empty output location must carry this pack's manifest as an
     * ownership marker, so a mistyped argument can never delete arbitrary
     * files (Codex review, PR #45 follow-up).
     */
    static void ensureSafeToClean(Path outDir) throws IOException {
        if (!Files.exists(outDir)) {
            return;
        }
        try (var entries = Files.list(outDir)) {
            if (entries.findAny().isEmpty()) {
                return;
            }
        }
        Path marker = outDir.resolve("manifest.properties");
        if (!Files.exists(marker)
                || !Files.readString(marker, StandardCharsets.UTF_8)
                        .contains("pack.name=" + PACK_NAME)) {
            throw new IllegalStateException("refusing to clean " + outDir
                    + ": it is not an existing " + PACK_NAME
                    + " pack (no owning manifest found)");
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
