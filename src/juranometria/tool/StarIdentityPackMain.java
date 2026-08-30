package juranometria.tool;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import juranometria.tool.StarIdentityStudyMain.Identity;
import juranometria.tool.StarIdentityStudyMain.Join;

/**
 * Generates the bundled star-identity pack (issue #113) from the
 * pinned d3-celestial starnames.json and raw Tycho-2 inputs, per
 * docs/decisions/star-identity.md: verifies every raw input's
 * SHA-256, runs the SAME join implementation the reviewed study
 * measured with (HIP through raw Tycho-2 main files and supplement,
 * brightest packed component only), refuses to write if any count
 * drifts from the reviewed decision report, and writes a plain CSV
 * resource with a checksummed manifest and the licence and
 * attribution notices beside it.
 *
 * Deterministic by construction: no timestamps, fixed Locale.ROOT
 * formatting, rows in TYC identifier order - two clean runs are
 * byte-identical. Run via "make import-star-identities".
 */
public final class StarIdentityPackMain {

    static final String PACK_NAME = "star-identities";
    static final String CSV_NAME = "star-identities.csv";

    /**
     * The reviewed decision report's numbers (docs/decisions/
     * star-identity.md): any drift from the pinned inputs is a loud
     * failure, never a silently different pack.
     */
    static final int SOURCE_ENTRIES = 4869;
    static final int JOINED = 4805;
    static final int UNMATCHED = 64;
    static final int MULTI_COMPONENT = 201;


    private StarIdentityPackMain() {
    }

    public static void main(String[] args) throws Exception {
        Path outDir = Path.of(args.length > 0 ? args[0]
                : "src/resources/catalog/star-identities");

        StarIdentityStudyMain.verifyPinnedInputs();
        Join join = StarIdentityStudyMain.join();
        requireDecisionCounts(join);

        List<Identity> rows = new ArrayList<>(join.byTyc().values());
        rows.sort(Comparator.comparing(identity -> tycKey(identity.tyc()),
                tycOrder()));
        Set<String> constellations = constellationIds();
        int names = 0;
        int bayers = 0;
        int flamsteeds = 0;
        int invalidConstellations = 0;
        StringBuilder csv = new StringBuilder(
                "tyc,name,bayer,flamsteed,constellation\n");
        for (int i = 0; i < rows.size(); i++) {
            Identity row = rows.get(i);
            // The source holds NSV catalogue-number fragments in the
            // constellation field of a few designation-less variable
            // stars; a membership that fails the cross-check is
            // carried as unknown, never as an invented fact - and
            // counted, never silent.
            if (row.constellation() != null
                    && !constellations.contains(row.constellation())) {
                if (row.name() != null || row.bayer() != null
                        || row.flamsteed() != null) {
                    throw new IllegalStateException("a designated star with"
                            + " an unknown constellation is unexplained"
                            + " ambiguity: " + row);
                }
                invalidConstellations++;
                row = new Identity(row.tyc(), row.vmag(), null, null, null,
                        null);
                rows.set(i, row);
            }
            requireHonestRow(row, constellations);
            names += row.name() != null ? 1 : 0;
            bayers += row.bayer() != null ? 1 : 0;
            flamsteeds += row.flamsteed() != null ? 1 : 0;
            csv.append(row.tyc()).append(',')
                    .append(blankIfNull(row.name())).append(',')
                    .append(blankIfNull(row.bayer())).append(',')
                    .append(blankIfNull(row.flamsteed())).append(',')
                    .append(blankIfNull(row.constellation())).append('\n');
        }

        ensureSafeToClean(outDir);
        Files.createDirectories(outDir);
        Map<String, String> checksums = new LinkedHashMap<>();
        write(outDir.resolve(CSV_NAME), csv.toString(), checksums);
        copyVerifiedLicense(outDir, checksums);
        write(outDir.resolve("NOTICE-star-identities.md"), notice(), checksums);
        String manifest = manifest(rows.size(), names, bayers, flamsteeds,
                invalidConstellations, join, checksums);
        write(outDir.resolve("manifest.properties"), manifest, null);

        Properties written = new Properties();
        written.load(new java.io.StringReader(manifest));
        validateManifest(written);

        System.out.printf(Locale.ROOT,
                "wrote %d identity rows (%d proper names, %d Bayer, %d"
                        + " Flamsteed) - the reviewed join reproduced:"
                        + " %d/%d matched, %d unmatched, %d multi-component"
                        + " systems attached to their brightest packed"
                        + " component%n",
                rows.size(), names, bayers, flamsteeds, join.joined(),
                join.sourceEntries(), join.unmatched().size(),
                join.multiComponent());
        System.out.printf(Locale.ROOT,
                "%d designation-less rows carried a non-IAU constellation"
                        + " value in the source (NSV fragments); packed as"
                        + " unknown, never invented%n",
                invalidConstellations);
        System.out.println("unmatched (fainter than V 8 or outside Tycho-2;"
                + " an honest exclusion, not a defect):");
        for (String unmatched : join.unmatched()) {
            System.out.println("  " + unmatched);
        }
        for (Map.Entry<String, String> checksum : checksums.entrySet()) {
            System.out.println("  " + checksum.getValue() + "  "
                    + checksum.getKey());
        }
    }

    /** Any drift from the reviewed report is loud, never silent. */
    static void requireDecisionCounts(Join join) {
        if (join.sourceEntries() != SOURCE_ENTRIES
                || join.joined() != JOINED
                || join.unmatched().size() != UNMATCHED
                || join.multiComponent() != MULTI_COMPONENT) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "join drifts from the reviewed decision report"
                            + " (docs/decisions/star-identity.md): expected"
                            + " %d entries, %d joined, %d unmatched, %d"
                            + " multi-component; found %d, %d, %d, %d",
                    SOURCE_ENTRIES, JOINED, UNMATCHED, MULTI_COMPONENT,
                    join.sourceEntries(), join.joined(),
                    join.unmatched().size(), join.multiComponent()));
        }
    }

    /**
     * The row-level honesty contract: no field breaks the CSV, a
     * designation never floats without its constellation, and a
     * constellation is always one of the bundled geography's 88
     * identities - cross-checked, as the decision promises.
     */
    static void requireHonestRow(Identity row, Set<String> constellations) {
        for (String field : new String[] {row.tyc(), row.name(), row.bayer(),
                row.flamsteed(), row.constellation()}) {
            if (field != null && (field.contains(",")
                    || field.contains("\n"))) {
                throw new IllegalStateException(
                        "field breaks the CSV in row " + row);
            }
        }
        if ((row.bayer() != null || row.flamsteed() != null)
                && row.constellation() == null) {
            throw new IllegalStateException("a designation without its"
                    + " constellation is meaningless: " + row);
        }
        if (row.constellation() != null
                && !constellations.contains(row.constellation())) {
            throw new IllegalStateException("unknown constellation '"
                    + row.constellation() + "' in row " + row);
        }
    }

    /** The 88 identities from the bundled constellation geography. */
    static Set<String> constellationIds() throws IOException {
        Set<String> ids = new TreeSet<>();
        for (String line : Files.readAllLines(Path.of(
                "src/resources/geo/constellations/constellations.csv"))) {
            if (line.startsWith("id,") || line.isBlank()) {
                continue;
            }
            ids.add(line.substring(0, line.indexOf(',')));
        }
        if (ids.size() != 88) {
            throw new IllegalStateException("expected the 88 constellation"
                    + " identities, found " + ids.size());
        }
        return ids;
    }

    /**
     * Validates a pack manifest - the loader's own contract
     * (juranometria.catalog.StarIdentities), shared so an unsupported
     * format or foreign pack fails loudly on both the generating and
     * the loading side.
     */
    static void validateManifest(Properties manifest) {
        juranometria.catalog.StarIdentities.validateManifest(manifest);
    }

    private static String manifest(int rows, int names, int bayers,
                                   int flamsteeds, int invalidConstellations,
                                   Join join,
                                   Map<String, String> checksums) {
        Map<String, String> entries = new TreeMap<>();
        entries.put("format.version", "1");
        entries.put("pack.name", PACK_NAME);
        entries.put("join.contract",
                "HIP-through-raw-Tycho-2-main-and-supplement,"
                        + " brightest-packed-component-only");
        entries.put("join.pack", "bright-sky");
        entries.put("source.entries", Integer.toString(join.sourceEntries()));
        entries.put("join.matched", Integer.toString(join.joined()));
        entries.put("join.unmatched", Integer.toString(join.unmatched().size()));
        entries.put("join.multi.component",
                Integer.toString(join.multiComponent()));
        entries.put("rows", Integer.toString(rows));
        entries.put("rows.with.name", Integer.toString(names));
        entries.put("rows.with.bayer", Integer.toString(bayers));
        entries.put("rows.with.flamsteed", Integer.toString(flamsteeds));
        entries.put("rows.constellation.invalid.in.source.carried.unknown",
                Integer.toString(invalidConstellations));
        entries.put("names.character",
                "traditional-star-names-not-per-name-IAU-certified");
        entries.put("source", "d3-celestial starnames.json (Olaf Frohn)");
        entries.put("source.commit", ConstellationPackMain.SOURCE_COMMIT);
        entries.put("license", "BSD-3-Clause");
        entries.put("audit.date", "2026-08-30");
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
                # Notice: star-identity data

                The traditional star names, Bayer designations, Flamsteed
                numbers, and constellation memberships in this pack are
                derived from **d3-celestial** by Olaf Frohn,
                https://github.com/ofrohn/d3-celestial, pinned at commit
                `%s`
                (`starnames.json`), and redistributed under the
                **BSD-3-Clause** licence (full text in
                `LICENSE-BSD-3-Clause.txt` beside this notice).

                Provenance, per docs/decisions/star-identity.md:

                - The proper names are **traditional star names** as
                  compiled by the source - largely coinciding with the IAU
                  WGSN approved set, but this pack does not claim per-name
                  IAU certification. Bayer letters and Flamsteed numbers
                  are historical designation systems, not IAU standards.
                - Upstream compilation sources recorded by d3-celestial:
                  the HD-DM-GC-HR-HIP-Bayer-Flamsteed Cross Index (Kostjuk
                  2002, VizieR IV/27A), the FK5/common-name cross index
                  (IV/22), the GCVS, and the IAU constellation chart data.
                - Identities attach to the bright-sky pack's stars by
                  Hipparcos number through the raw Tycho-2 catalogue (main
                  files and supplement); a multi-component system's
                  identity attaches to its brightest packed component
                  only. Stars without an entry simply have no identity.
                """.formatted(ConstellationPackMain.SOURCE_COMMIT);
    }

    /** Copies the pinned upstream BSD licence text beside the data. */
    private static void copyVerifiedLicense(Path outDir,
                                            Map<String, String> checksums)
            throws Exception {
        Path raw = Path.of("imports/raw/constellations/LICENSE");
        String expected = ConstellationPackMain.PINNED.get("LICENSE");
        byte[] bytes = Files.readAllBytes(raw);
        String actual = PinnedInputs.sha256Hex(bytes);
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    "raw LICENSE fails its pinned checksum: " + actual);
        }
        Path target = outDir.resolve("LICENSE-BSD-3-Clause.txt");
        Files.write(target, bytes);
        checksums.put(target.getFileName().toString(), actual);
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

    private static String blankIfNull(String value) {
        return value == null ? "" : value;
    }

    /** A numeric sort key for "TYC region-star-component" identifiers. */
    private static long[] tycKey(String tyc) {
        String[] parts = tyc.substring("TYC ".length()).split("-");
        return new long[] {Long.parseLong(parts[0]), Long.parseLong(parts[1]),
                Long.parseLong(parts[2])};
    }

    private static Comparator<long[]> tycOrder() {
        return Comparator.<long[]>comparingLong(key -> key[0])
                .thenComparingLong(key -> key[1])
                .thenComparingLong(key -> key[2]);
    }

    /**
     * Refuses to clean a directory the generator does not own: an
     * existing non-empty output location must carry this pack's
     * manifest as an ownership marker (the established generators'
     * rule).
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
            Properties properties = new Properties();
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
