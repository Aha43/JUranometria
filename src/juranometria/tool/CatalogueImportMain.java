package juranometria.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.tool.OpenNgcRecords.DsoRow;
import juranometria.tool.Tycho2Records.StarRow;

/**
 * The offline catalogue import defined by docs/decisions/catalogue-sources.md:
 * turns the pinned Tycho-2 and OpenNGC inputs in imports/raw/ into the
 * regional resources under src/resources/catalog/m31/. Run via
 * "make import-catalogue" after scripts/download-catalogue-sources.sh.
 *
 * The tool verifies every input's SHA-256 before transforming anything and
 * produces byte-identical output for identical inputs. It is never run by
 * the application.
 */
public final class CatalogueImportMain {

    static final SkyPosition M31_CENTRE = new SkyPosition(10.684708, 41.268750);
    static final double REGION_RADIUS_DEGREES = 10.0;
    static final double STAR_LIMIT_V = 10.0;

    /** Audited 2026-08-29 from the pinned sources; see the decision document. */
    private static final Map<String, String> INPUT_SHA256 = Map.ofEntries(
            Map.entry("tyc2.dat.00.gz", "1b224570fc6eb151984ce106fdf797728649d1ea77aaf3effd2d3444cfac6df6"),
            Map.entry("tyc2.dat.01.gz", "36221f4a5cdd9c5009d64299d2e6409f8d4cdf924321768780165bd3e2a2a99a"),
            Map.entry("tyc2.dat.02.gz", "e2ea4eeb7d204dde70ac54d172b1c429540c0d23d62ec0d7500561adc44cc57b"),
            Map.entry("tyc2.dat.03.gz", "29e0cb57cba7e1651455efcb528276b0c28e1fe83f47f8ebe963e48d8b180afb"),
            Map.entry("tyc2.dat.04.gz", "d04868329b470aa320f0fe5e9c525be619a7a228b364433e5f114f2178946d9b"),
            Map.entry("tyc2.dat.05.gz", "51f08772197dcf0cd26ff098c866036340b797f71ee04be22712517c10c73c4e"),
            Map.entry("tyc2.dat.06.gz", "f55503a66abce0e11d1a4ccbcde1f4c97a6e3e769de85e005f57ba963086ab03"),
            Map.entry("tyc2.dat.07.gz", "bc15b5b1f6308477360fbb93f6a98d9029601cab68973202be0f8764c127f524"),
            Map.entry("tyc2.dat.08.gz", "315ac34c678bc2e0f568b43b6b2d7c3d7c9cf9c089b51115d18b872864e50426"),
            Map.entry("tyc2.dat.09.gz", "d66c671fa29aad10bc5fd697f68c918bb774c1ffc3c7db9c20234895268390d5"),
            Map.entry("tyc2.dat.10.gz", "d86529df819ebdf3f4b8892510973ee10e1db06930814e014dd467193e740d52"),
            Map.entry("tyc2.dat.11.gz", "fc0c3203b3d2787da54c43d9963e363c8a3926576c383b101a15350a7ca23e9c"),
            Map.entry("tyc2.dat.12.gz", "68fd9d7e7353d52dea0de043cc62b45c04d234104eb05c063dc23ea7b6759576"),
            Map.entry("tyc2.dat.13.gz", "a08ecc092e6d134742c0f594c56c5c2e02b8fd5e76cffb1779877f2fcefac3fc"),
            Map.entry("tyc2.dat.14.gz", "2846e8cc489795a4d8160d2d5b64ff0e9a00598adba35a52a4f8ccf4c5e38b3e"),
            Map.entry("tyc2.dat.15.gz", "74784a3656382090a70d778820335f4a781509dbe3fcb9e92d18cf96b2c46c72"),
            Map.entry("tyc2.dat.16.gz", "320560e3d551cc40fa1f54ef7133709bf4bd45efa6688f808be7c09be5544a4d"),
            Map.entry("tyc2.dat.17.gz", "a8154e940aa0a0e8f31d91b4bd9fd56e6849da42cddb13c7dda772439b06991a"),
            Map.entry("tyc2.dat.18.gz", "5e951a1a51df7956f205b83cbfa5501d357c843640a253c1e6d3917bebe7d928"),
            Map.entry("tyc2.dat.19.gz", "f59605a38116f517a31a7dbdee3469c077658f2f40b8afe5da2aeb832eaee3dd"),
            Map.entry("suppl_1.dat.gz", "d256a9fc47259d506e4849b054e9392a62b2ed128e48ac6a25a3a60fcc317f0e"),
            Map.entry("openngc-NGC.csv", "840fe0c9ee1332e551b2e722a0e92726cd7b157914a3d2177602832aadd3aa9e"),
            Map.entry("openngc-addendum.csv", "1d8f0914e643ada325a5a94d88d8fefad6a4937a2f77cc34f21483af22b11983"),
            Map.entry("openngc-CC-BY-SA-4.0.txt", "cde7883b9050a1104f4ac19a1572aafd6e5d7323b68351aaf51fbf4beba54966"));

    private CatalogueImportMain() {
    }

    public static void main(String[] args) throws Exception {
        Path rawDir = Path.of(args.length > 0 ? args[0] : "imports/raw");
        Path outDir = Path.of(args.length > 1 ? args[1] : "src/resources/catalog/m31");

        for (Map.Entry<String, String> pinned : new java.util.TreeMap<>(INPUT_SHA256).entrySet()) {
            verifyChecksum(rawDir.resolve(pinned.getKey()), pinned.getValue());
        }

        SkyRegion region = new SkyRegion(M31_CENTRE, REGION_RADIUS_DEGREES);
        Counts counts = new Counts();

        List<StarRow> stars = new ArrayList<>();
        java.util.Set<String> seenIds = new java.util.HashSet<>();
        for (int i = 0; i <= 19; i++) {
            Path file = rawDir.resolve(String.format(Locale.ROOT, "tyc2.dat.%02d.gz", i));
            readGzLines(file, line ->
                    accept(Tycho2Records.fromMainLine(line), region, stars, seenIds, counts));
        }
        counts.mainStars = stars.size();
        readGzLines(rawDir.resolve("suppl_1.dat.gz"), line ->
                accept(Tycho2Records.fromSupplementLine(line), region, stars, seenIds, counts));
        counts.supplementStars = stars.size() - counts.mainStars;
        if (seenIds.size() != stars.size()) {
            throw new IllegalStateException("duplicate star identifiers survived the import");
        }
        stars.sort(Comparator.comparingDouble(StarRow::vmag).thenComparing(StarRow::id));

        List<String[]> dsoRows = importDsos(rawDir, region, counts);

        Files.createDirectories(outDir);
        write(outDir.resolve("stars.csv"), starsCsv(stars));
        write(outDir.resolve("dsos.csv"), dsosCsv(dsoRows));
        write(outDir.resolve("PROVENANCE.md"), Notices.provenance(counts, stars.size(), dsoRows.size()));
        write(outDir.resolve("NOTICE-tycho2.md"), Notices.tycho2());
        write(outDir.resolve("NOTICE-openngc.md"), Notices.openNgc());
        Files.write(outDir.resolve("LICENSE-CC-BY-SA-4.0.txt"),
                Files.readAllBytes(rawDir.resolve("openngc-CC-BY-SA-4.0.txt")));

        System.out.println("Wrote " + stars.size() + " stars and " + dsoRows.size()
                + " deep-sky objects to " + outDir);
    }

    /** Normalization counters recorded in the generated provenance. */
    static final class Counts {
        int mainStars;
        int supplementStars;
        int fallbackPositions;
        int vtWithoutBt;
        int hpMagnitudes;
        int droppedNoVt;
        int supplementComponentsSkipped;
        int galaxies;
        int skippedOtherTypes;
        int skippedDupNonEx;
        int droppedNoMagnitude;
        int missingPositionAngle;
        int missingMinorAxis;
        int vmagFromB;
    }

    static void accept(java.util.Optional<StarRow> parsed, SkyRegion region,
                       List<StarRow> stars, java.util.Set<String> seenIds, Counts counts) {
        if (parsed.isEmpty()) {
            counts.droppedNoVt++;
            return;
        }
        StarRow row = parsed.get();
        if (row.vmag() > STAR_LIMIT_V
                || !region.contains(new SkyPosition(row.raDegrees(), row.decDegrees()))) {
            return;
        }
        // Component policy: the main catalogue wins. A supplement-1 record
        // reusing an already-imported TYC identifier is a resolved component
        // of a Hipparcos double whose photocentre the main entry carries
        // (e.g. TYC 2794-1098-1, CCDM 1009A in main versus 1009B in the
        // supplement); it is skipped and counted.
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
        stars.add(row);
    }

    private static List<String[]> importDsos(Path rawDir, SkyRegion region, Counts counts)
            throws IOException {
        List<String[]> rows = new ArrayList<>();
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
                if (Double.isNaN(dso.raDegrees()) || Double.isNaN(dso.decDegrees())
                        || !region.contains(new SkyPosition(dso.raDegrees(), dso.decDegrees()))) {
                    continue;
                }
                if (!dso.type().equals("G")) {
                    counts.skippedOtherTypes++;
                    continue;
                }
                String[] row = galaxyRow(dso, counts);
                if (row == null) {
                    continue;
                }
                rows.add(row);
                counts.galaxies++;
            }
        }
        rows.sort(Comparator.comparing(row -> row[0]));
        return rows;
    }

    private static String[] galaxyRow(DsoRow dso, Counts counts) {
        double major = dso.majorAxisArcmin();
        if (Double.isNaN(major) || major <= 0) {
            throw new IllegalArgumentException("galaxy without major axis: " + dso.id());
        }
        double minor = dso.minorAxisArcmin();
        if (Double.isNaN(minor) || minor <= 0) {
            minor = major;
            counts.missingMinorAxis++;
        }
        double pa = dso.positionAngleDegrees();
        if (Double.isNaN(pa)) {
            pa = 0.0;
            counts.missingPositionAngle++;
        }
        pa = ((pa % 180.0) + 180.0) % 180.0;
        double vmag = dso.vmag();
        if (Double.isNaN(vmag)) {
            vmag = dso.bmag();
            if (Double.isNaN(vmag)) {
                // A documented drop: the chart model requires a magnitude.
                counts.droppedNoMagnitude++;
                return null;
            }
            counts.vmagFromB++;
        }
        boolean messier = dso.aliases().stream().anyMatch(alias -> alias.startsWith("M "));
        return new String[] {
                dso.id(),
                String.join("|", dso.aliases()),
                "GALAXY",
                String.format(Locale.ROOT, "%.6f", dso.raDegrees()),
                String.format(Locale.ROOT, "%.6f", dso.decDegrees()),
                String.format(Locale.ROOT, "%.2f", major),
                String.format(Locale.ROOT, "%.2f", minor),
                String.format(Locale.ROOT, "%.1f", pa),
                String.format(Locale.ROOT, "%.2f", vmag),
                messier ? "1" : "2",
        };
    }

    static String starsCsv(List<StarRow> stars) {
        StringBuilder csv = new StringBuilder();
        csv.append("# M31-region stars generated from the Tycho-2 Catalogue.\n");
        csv.append("# Generated resource - do not edit; see PROVENANCE.md and NOTICE-tycho2.md.\n");
        csv.append("# id,ra_deg,dec_deg,vmag\n");
        for (StarRow star : stars) {
            csv.append(String.format(Locale.ROOT, "%s,%.6f,%.6f,%.2f%n".replace("%n", "\n"),
                    star.id(), star.raDegrees(), star.decDegrees(), star.vmag()));
        }
        return csv.toString();
    }

    static String dsosCsv(List<String[]> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("# M31-region deep-sky objects generated from OpenNGC.\n");
        csv.append("# Generated resource - do not edit; see PROVENANCE.md and NOTICE-openngc.md.\n");
        csv.append("# id,aliases(|-separated),type,ra_deg,dec_deg,major_arcmin,minor_arcmin,pa_deg,vmag,label_priority\n");
        for (String[] row : rows) {
            csv.append(String.join(",", row)).append('\n');
        }
        return csv.toString();
    }

    private static void write(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void readGzLines(Path file, java.util.function.Consumer<String> consumer)
            throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                consumer.accept(line);
            }
        }
    }

    static void verifyChecksum(Path file, String expectedSha256) throws IOException {
        if (!Files.exists(file)) {
            throw new IllegalStateException("missing pinned input: " + file
                    + " (run scripts/download-catalogue-sources.sh)");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(file));
            StringBuilder actual = new StringBuilder();
            for (byte b : digest.digest()) {
                actual.append(String.format(Locale.ROOT, "%02x", b));
            }
            if (!actual.toString().equals(expectedSha256)) {
                throw new IllegalStateException("checksum mismatch for " + file
                        + "\n  expected " + expectedSha256 + "\n  actual   " + actual);
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
