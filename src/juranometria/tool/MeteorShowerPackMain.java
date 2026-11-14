package juranometria.tool;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import juranometria.chart.SkyPosition;
import juranometria.meteor.MeteorShower;
import juranometria.meteor.MeteorShowerPack;
import juranometria.meteor.MeteorShowers;
import juranometria.solar.SolarSystemService;

/**
 * Generates the bundled meteor-shower pack (#716) from the pinned raw
 * sources under {@code imports/raw/meteor-showers}, per
 * {@code docs/decisions/meteor-showers.md}.
 *
 * <p>What it refuses, loudly and before writing anything:
 * <ul>
 * <li>a raw input that is absent or does not match its SHA-256;</li>
 * <li>a selection rule (R-11) that does not yield exactly the showers
 * that were keyed twice;</li>
 * <li>any field in which the two keyings of the IMO tables differ
 * (R-4: the parsed text extraction against a keying from the page
 * image);</li>
 * <li>a shower whose IAU number in the IMO list is not the IAU list's,
 * or whose radiant table read at the maximum is more than two degrees
 * from the working list's radiant.</li>
 * </ul>
 *
 * <p>What it computes, and the pack marks as the atlas's own: the solar
 * longitude of 00:00 UTC on the first date of activity and of 24:00 UTC
 * on the last, and of 00:00 UTC on each radiant row's date, in the
 * edition's year around the maximum, from the production Sun (R-6).
 *
 * <p>Deterministic: no timestamps, {@code Locale.ROOT}, showers in the
 * order of the year. Run via {@code make import-meteor-showers}.
 */
public final class MeteorShowerPackMain {

    static final Path RAW = Path.of("imports/raw/meteor-showers");
    static final int EDITION_YEAR = 2027;
    static final String EDITION = "IMO Meteor Shower Calendar 2027";
    static final String EDITION_REFERENCE = "IMO_INFO(3-26), edited by J. Rendtel";
    /** "Correct according to the best information available in June 2026" (Table 5's heading). */
    static final String CORRECT_AS_OF = "2026-06";
    static final String CREDIT =
            "Source: International Meteor Organization (IMO) – www.imo.net";
    static final String NOTICE_NAME = "NOTICE-meteor-showers.md";

    /** The raw inputs the pack is made from, each with its checksum. */
    static final Map<String, String> SHA256 = new LinkedHashMap<>();

    static {
        SHA256.put("imo-cal2027.pdf",
                "9e6b9a1609fb89ebb8571756b15670b2ccf22e64ed22ae999ff604705d773eec");
        SHA256.put("iaumdc-streamestablisheddata.txt",
                "67fdf79e83c2e690f35cb0eb6e0b8a5db7b4416bedc264eb61a5fc056dcad0f4");
        SHA256.put("transcribed/imo-2027-table5.tsv",
                "8a4760ba8bfafee56061dd34ab71ec446585ceb66255f905f662cb75c31b2833");
        SHA256.put("transcribed/imo-2027-table6.tsv",
                "37a44e58ebfc8bab69b1ca50d28bc9c836e0ec6f395b61f012f17c230961ecc1");
        SHA256.put("transcribed/imo-2027-second-keying-table5.tsv",
                "00e26476f95d6b0904f37fdbf23168134881e08fd9e45147df94e2fdbe03d585");
        SHA256.put("transcribed/imo-2027-second-keying-table6.tsv",
                "ba674ce646da2cec431a4ce52987fa3f65cf07427a6e2996c2140bae28858512");
    }

    /**
     * Ruled normalizations of a parent body's name (owner, 2026-11-14): per
     * code, the IAU list's statement as the import finds it and the name the
     * pack carries instead. The Leonids' comet is 55P/Tempel-Tuttle, after
     * Wilhelm Tempel; the list's most frequent spelling, "Temple", is an
     * evident error. The import refuses when the list no longer says what
     * is recorded here, and the manifest keeps the source's value.
     */
    static final Map<String, String[]> PARENT_NORMALIZED = Map.of(
            "LEO", new String[] {"55P/Temple-Tuttle", "55P/Tempel\u2013Tuttle"});

    /** The accuracy fixture the tests hold in CI (owner's ruling of 2026-11-14). */
    static final Path GMN_FIXTURE = Path.of("test/resources/meteor-showers/gmn-radiant-samples.tsv");
    static final String GMN_RAW = "gmn_shower_table_20230518.txt";
    static final String GMN_URL = "https://raw.githubusercontent.com/wmpg/WesternMeteorPyLib/"
            + "master/wmpl/share/gmn_shower_table_20230518.txt";

    private static final List<String> MONTHS = List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

    /** One row of a keyed Table 5: the fields as text, exactly as keyed. */
    private record Listed(List<String> fields) {
        String code() {
            return fields.get(0);
        }
    }

    /** What the IAU list says of one code. */
    private record Identity(int number, String name, String parent) {
    }

    private MeteorShowerPackMain() {
    }

    public static void main(String[] args) throws Exception {
        Path outDir = Path.of(args.length > 0 ? args[0] : "src/resources/meteor-showers");
        for (Map.Entry<String, String> pinned : SHA256.entrySet()) {
            PinnedInputs.verifyChecksum(RAW.resolve(pinned.getKey()), pinned.getValue());
        }
        Map<String, Listed> first = table5(RAW.resolve("transcribed/imo-2027-table5.tsv"));
        Map<String, Listed> second =
                table5(RAW.resolve("transcribed/imo-2027-second-keying-table5.tsv"));
        Map<String, List<String>> firstRows =
                table6(RAW.resolve("transcribed/imo-2027-table6.tsv"));
        Map<String, List<String>> secondRows =
                table6(RAW.resolve("transcribed/imo-2027-second-keying-table6.tsv"));
        Map<String, List<Identity>> iau = new LinkedHashMap<>();
        String lastUpdate = iau(RAW.resolve("iaumdc-streamestablisheddata.txt"), iau);

        // R-11, applied to the whole working list: it must select what was keyed twice.
        List<String> selected = new ArrayList<>();
        for (Listed listed : first.values()) {
            String zhr = listed.fields().get(10).replace("+", "");
            boolean strong = zhr.chars().allMatch(Character::isDigit)
                    && Integer.parseInt(zhr) >= 10;
            List<Identity> known = iau.get(listed.code());
            if (strong && known != null && !known.get(0).name().startsWith("Daytime")) {
                selected.add(listed.code());
            }
        }
        if (!selected.equals(new ArrayList<>(second.keySet()))) {
            throw new IllegalStateException("the selection rule yields " + selected
                    + " and the second keying holds " + second.keySet());
        }
        // R-4: the two keyings agree in every field, or nothing is written.
        int compared = 0;
        for (String code : selected) {
            if (!first.get(code).equals(second.get(code))) {
                throw new IllegalStateException("the two keyings of Table 5 differ for "
                        + code + ": " + first.get(code) + " against " + second.get(code));
            }
            compared += first.get(code).fields().size();
            List<String> a = new ArrayList<>(firstRows.getOrDefault(code, List.of()));
            List<String> b = new ArrayList<>(secondRows.getOrDefault(code, List.of()));
            a.sort(Comparator.naturalOrder());
            b.sort(Comparator.naturalOrder());
            if (a.isEmpty() || !a.equals(b)) {
                throw new IllegalStateException("the two keyings of Table 6 differ for "
                        + code + ": " + a + " against " + b);
            }
            compared += a.size() * 4;
        }

        SolarSystemService solar = SolarSystemService.load();
        List<MeteorShower> showers = new ArrayList<>();
        Map<String, Instant> maxima = new LinkedHashMap<>();
        for (String code : selected) {
            List<String> f = first.get(code).fields();
            Identity identity = iau.get(code).get(0);
            if (identity.number() != Integer.parseInt(f.get(1))) {
                throw new IllegalStateException(code + " is IAU number " + identity.number()
                        + " in the IAU list and " + f.get(1) + " in the IMO list");
            }
            Instant maximum = date(f.get(4), EDITION_YEAR);
            Instant start = nearest(f.get(2), maximum, -1);
            Instant end = nearest(f.get(3), maximum, +1).plusSeconds(86400);
            List<MeteorShower.Row> rows = new ArrayList<>();
            double anchor = Double.parseDouble(f.get(5));
            for (String row : firstRows.get(code)) {
                String[] r = row.split("\t");
                Instant at = nearest(MONTHS.get(Integer.parseInt(r[0]) - 1) + r[1], maximum, 0);
                rows.add(new MeteorShower.Row(Integer.parseInt(r[0]), Integer.parseInt(r[1]),
                        round(solar.solarLongitudeJ2000Degrees(at)), new SkyPosition(
                                Double.parseDouble(r[2]), Double.parseDouble(r[3]))));
            }
            rows.sort(Comparator.comparingDouble(
                    r -> signed(r.solarLongitude() - anchor)));
            String parent = identity.parent();
            String[] ruled = PARENT_NORMALIZED.get(code);
            if (ruled != null) {
                if (!ruled[0].equals(parent)) {
                    throw new IllegalStateException(code + ": the ruled normalization expects"
                            + " the IAU list to state \"" + ruled[0] + "\" and it states \""
                            + parent + "\" - rule again");
                }
                parent = ruled[1];
            }
            MeteorShower shower = new MeteorShower(code, identity.number(), identity.name(),
                    parent, spaced(f.get(2)), spaced(f.get(3)),
                    round(solar.solarLongitudeJ2000Degrees(start)),
                    round(solar.solarLongitudeJ2000Degrees(end)), spaced(f.get(4)), f.get(5),
                    new SkyPosition(Double.parseDouble(f.get(6)), Double.parseDouble(f.get(7))),
                    Double.parseDouble(f.get(8)), f.get(9), f.get(10), rows);
            double apart = MeteorShowers.radiantAt(shower, shower.maximumLongitude()).at()
                    .separationDegrees(shower.radiantAtMaximum());
            if (apart > 2.0) {
                throw new IllegalStateException(code + ": the radiant table read at the"
                        + " maximum is " + apart + " degrees from the working list's radiant");
            }
            if (!MeteorShowers.active(shower, shower.maximumLongitude())) {
                throw new IllegalStateException(code + ": the maximum is outside the activity");
            }
            showers.add(shower);
            maxima.put(code, maximum);
        }
        showers.sort(Comparator.comparing(s -> maxima.get(s.code())));

        StringBuilder list = new StringBuilder(MeteorShowerPack.SHOWERS_HEADER).append('\n');
        StringBuilder radiants = new StringBuilder(MeteorShowerPack.RADIANTS_HEADER).append('\n');
        int rowCount = 0;
        for (MeteorShower s : showers) {
            list.append(String.join("\t", s.code(), Integer.toString(s.iauNumber()),
                    s.iauName(), s.parentBody(), s.activityStart(), s.activityEnd(),
                    f4(s.startLongitude()), f4(s.endLongitude()), s.maximumDate(),
                    s.maximumLongitudeText(), whole(s.radiantAtMaximum().raDegrees()),
                    whole(s.radiantAtMaximum().decDegrees()), whole(s.vInfinity()),
                    s.populationIndex(), s.zhr())).append('\n');
            for (MeteorShower.Row r : s.rows()) {
                radiants.append(String.join("\t", s.code(), Integer.toString(r.month()),
                        Integer.toString(r.day()), f4(r.solarLongitude()),
                        whole(r.at().raDegrees()), whole(r.at().decDegrees()))).append('\n');
                rowCount++;
            }
        }

        ensureSafeToClean(outDir);
        Files.createDirectories(outDir);
        Map<String, String> checksums = new TreeMap<>();
        write(outDir.resolve(MeteorShowerPack.SHOWERS), list.toString(), checksums);
        write(outDir.resolve(MeteorShowerPack.RADIANTS), radiants.toString(), checksums);
        write(outDir.resolve(NOTICE_NAME), notice(lastUpdate), checksums);
        Map<String, String> manifest = new TreeMap<>();
        manifest.put("format.version", "1");
        manifest.put("pack.name", "meteor-showers");
        manifest.put("edition", EDITION);
        manifest.put("edition.reference", EDITION_REFERENCE);
        manifest.put("edition.year", Integer.toString(EDITION_YEAR));
        manifest.put("edition.correctAsOf", CORRECT_AS_OF);
        manifest.put("credit", CREDIT);
        manifest.put("identity.source", "IAU Meteor Data Center, established meteor showers, V.2");
        manifest.put("identity.lastUpdate", lastUpdate);
        manifest.put("showers", Integer.toString(showers.size()));
        manifest.put("radiant.rows", Integer.toString(rowCount));
        manifest.put("keying.fields.compared", Integer.toString(compared));
        manifest.put("computed", "start_solar_longitude, end_solar_longitude and each radiant"
                + " row's solar_longitude are JUranometria's, from the calendar's dates and"
                + " the DE440 Sun; every other number is the source's as printed");
        for (Map.Entry<String, String[]> ruled : new TreeMap<>(PARENT_NORMALIZED).entrySet()) {
            manifest.put("parent.normalized." + ruled.getKey() + ".source", ruled.getValue()[0]);
            manifest.put("parent.normalized." + ruled.getKey() + ".shipped", ruled.getValue()[1]);
        }
        for (Map.Entry<String, String> pinned : SHA256.entrySet()) {
            manifest.put("source.sha256." + pinned.getKey().replace('/', '.'),
                    pinned.getValue());
        }
        for (Map.Entry<String, String> c : checksums.entrySet()) {
            manifest.put("checksum." + c.getKey(), c.getValue());
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> e : manifest.entrySet()) {
            text.append(e.getKey()).append('=').append(escaped(e.getValue())).append('\n');
        }
        write(outDir.resolve("manifest.properties"), text.toString(), null);
        Properties written = new Properties();
        written.load(new StringReader(text.toString()));
        MeteorShowerPack.validateManifest(written);
        MeteorShowerPack loaded = MeteorShowerPack.load(name -> {
            try {
                return Files.newInputStream(outDir.resolve(name));
            } catch (IOException e) {
                return null;
            }
        });
        if (!loaded.showers().equals(showers)) {
            throw new IllegalStateException("the written pack does not read back as written");
        }
        if (args.length == 0) {
            int samples = writeGmnFixture(showers);
            System.out.printf(Locale.ROOT, "wrote %d Global Meteor Network samples to %s%n",
                    samples, GMN_FIXTURE);
        }
        System.out.printf(Locale.ROOT, "wrote %d showers and %d radiant rows to %s;"
                + " %d fields agreed between the two keyings%n", showers.size(), rowCount,
                outDir, compared);
    }

    /**
     * Writes the smallest extract of the Global Meteor Network's shower
     * table that holds the accuracy claim of R-10: for each shower, the
     * table's line nearest each printed radiant row and nearest the
     * maximum, within 0.11 degrees of solar longitude, verbatim. The table
     * is CC BY 4.0; the extract carries its source, digest, licence and
     * attribution, lives with the tests and is never shipped.
     */
    private static int writeGmnFixture(List<MeteorShower> showers) throws IOException {
        String digest = MeteorStudyData.SHA256.get(GMN_RAW);
        PinnedInputs.verifyChecksum(RAW.resolve(GMN_RAW), digest);
        Map<String, List<String>> byCode = new LinkedHashMap<>();
        for (String line : Files.readAllLines(RAW.resolve(GMN_RAW), StandardCharsets.US_ASCII)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String[] f = line.trim().split("\\s+");
            if (!f[1].equals("nan") && !f[2].equals("nan")) {
                byCode.computeIfAbsent(f[6], k -> new ArrayList<>()).add(line);
            }
        }
        StringBuilder out = new StringBuilder();
        out.append("# An extract of the Global Meteor Network's shower lookup table, for the nine\n")
                .append("# meteor showers of JUranometria's pack: the table's lines nearest each printed\n")
                .append("# radiant row and nearest each maximum, verbatim. Test data only; not shipped.\n")
                .append("#\n")
                .append("# Source: \"Shower lookup table measured using GMN data up to May 18, 2023\"\n")
                .append("#   ").append(GMN_URL).append('\n')
                .append("#   SHA-256 of the source file: ").append(digest).append('\n')
                .append("# Licence: Creative Commons Attribution 4.0 International (CC BY 4.0),\n")
                .append("#   https://creativecommons.org/licenses/by/4.0/ - the Global Meteor Network's\n")
                .append("#   data are released under it (https://globalmeteornetwork.org/data/).\n")
                .append("# Attribution: Global Meteor Network, https://globalmeteornetwork.org/;\n")
                .append("#   Vida et al. 2020, MNRAS 491, 2688-2705; Vida et al. 2021, MNRAS 506, 5046-5074.\n")
                .append("# Changes: lines selected, nothing else; every data line is the source's own.\n")
                .append("# Written by make import-meteor-showers (MeteorShowerPackMain); do not edit.\n")
                .append("#\n")
                .append("# Sol  SCE lon  SCE lat       Vg  Disp  IAU IAU\n")
                .append("# deg      deg      deg     km/s  deg    No  cd\n");
        int count = 0;
        for (MeteorShower shower : showers) {
            List<Double> wanted = new ArrayList<>();
            for (MeteorShower.Row row : shower.rows()) {
                wanted.add(row.solarLongitude());
            }
            wanted.add(shower.maximumLongitude());
            Map<Double, String> chosen = new TreeMap<>();
            for (double longitude : wanted) {
                String best = null;
                double bestOff = Double.MAX_VALUE;
                for (String line : byCode.getOrDefault(shower.code(), List.of())) {
                    double off = Math.abs(signed(
                            Double.parseDouble(line.trim().split("\\s+")[0]) - longitude));
                    if (off < bestOff) {
                        best = line;
                        bestOff = off;
                    }
                }
                if (best != null && bestOff <= 0.11) {
                    chosen.put(Double.parseDouble(best.trim().split("\\s+")[0]), best);
                }
            }
            for (String line : chosen.values()) {
                out.append(line).append('\n');
                count++;
            }
        }
        Files.createDirectories(GMN_FIXTURE.getParent());
        Files.write(GMN_FIXTURE, out.toString().getBytes(StandardCharsets.US_ASCII));
        return count;
    }

    private static Map<String, Listed> table5(Path file) throws IOException {
        Map<String, Listed> rows = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.startsWith("#") && !line.isBlank()) {
                Listed listed = new Listed(List.of(line.split("\t")));
                if (listed.fields().size() != 11) {
                    throw new IllegalStateException("a Table 5 row has eleven fields: " + line);
                }
                rows.put(listed.code(), listed);
            }
        }
        return rows;
    }

    /** Each code's rows as "month, day, ra, dec" text. */
    private static Map<String, List<String>> table6(Path file) throws IOException {
        Map<String, List<String>> rows = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.startsWith("#") && !line.isBlank()) {
                int tab = line.indexOf('\t');
                rows.computeIfAbsent(line.substring(0, tab), k -> new ArrayList<>())
                        .add(line.substring(tab + 1));
            }
        }
        return rows;
    }

    /** Reads the established showers; returns the file's stated date of last update. */
    private static String iau(Path file, Map<String, List<Identity>> into) throws IOException {
        // UTF-8 with a few stray Latin-1 bytes in its comment lines.
        String text = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        String lastUpdate = "";
        Map<String, Map<String, Integer>> parents = new LinkedHashMap<>();
        Map<String, Identity> named = new LinkedHashMap<>();
        for (String line : text.split("\n")) {
            if (line.startsWith(":") && line.contains("Last update:")) {
                lastUpdate = line.substring(line.indexOf("Last update:") + 12,
                        line.indexOf("Modified")).trim();
            }
            if (!line.startsWith("\"")) {
                continue;
            }
            String[] f = line.split("\\|");
            for (int i = 0; i < f.length; i++) {
                f[i] = f[i].trim().replace("\"", "").trim();
            }
            if (f.length < 32 || !f[1].chars().allMatch(Character::isDigit)
                    || !"1".equals(f[4])) {
                continue; // not a single established shower
            }
            named.putIfAbsent(f[3], new Identity(Integer.parseInt(f[1]), f[6], ""));
            if (!f[31].isEmpty()) {
                parents.computeIfAbsent(f[3], k -> new TreeMap<>()).merge(f[31], 1, Integer::sum);
            }
        }
        if (lastUpdate.isEmpty()) {
            throw new IllegalStateException("the IAU list does not state its last update");
        }
        for (Map.Entry<String, Identity> e : named.entrySet()) {
            into.put(e.getKey(), List.of(new Identity(e.getValue().number(),
                    e.getValue().name(), parentOf(parents.get(e.getKey())))));
        }
        return lastUpdate;
    }

    /**
     * The parent body the IAU list's solutions state most often, in the
     * spelling it uses most; two statements that differ only in
     * punctuation and spacing count as one. Empty when no solution
     * states one or when two different statements tie, because the
     * atlas does not choose between them.
     */
    private static String parentOf(Map<String, Integer> stated) {
        if (stated == null) {
            return "";
        }
        Map<String, Integer> groups = new TreeMap<>();
        for (Map.Entry<String, Integer> e : stated.entrySet()) {
            groups.merge(plain(e.getKey()), e.getValue(), Integer::sum);
        }
        String winner = "";
        int most = 0;
        boolean tie = false;
        for (Map.Entry<String, Integer> e : groups.entrySet()) {
            if (e.getValue() > most) {
                winner = e.getKey();
                most = e.getValue();
                tie = false;
            } else if (e.getValue() == most) {
                tie = true;
            }
        }
        if (tie) {
            return "";
        }
        String best = "";
        int count = 0;
        for (Map.Entry<String, Integer> e : stated.entrySet()) {
            if (plain(e.getKey()).equals(winner) && e.getValue() > count) {
                best = e.getKey();
                count = e.getValue();
            }
        }
        return best;
    }

    private static String plain(String stated) {
        return stated.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** 00:00 UTC of a printed date such as "Jan04" in a year. */
    private static Instant date(String printed, int year) {
        int month = MONTHS.indexOf(printed.substring(0, 3)) + 1;
        int day = Integer.parseInt(printed.substring(3));
        // "Jan 0" is the last day of the month before.
        return LocalDate.of(year, month, 1).plusDays(day - 1L).atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    /**
     * The printed date in the year that puts it nearest the maximum; with
     * a side, the nearest at or before it (-1) or at or after it (+1).
     */
    private static Instant nearest(String printed, Instant maximum, int side) {
        Instant best = null;
        for (int year = EDITION_YEAR - 1; year <= EDITION_YEAR + 1; year++) {
            Instant at = date(printed, year);
            if (side < 0 && at.isAfter(maximum) || side > 0 && at.isBefore(maximum)) {
                continue;
            }
            if (best == null || Math.abs(at.getEpochSecond() - maximum.getEpochSecond())
                    < Math.abs(best.getEpochSecond() - maximum.getEpochSecond())) {
                best = at;
            }
        }
        return best;
    }

    private static String spaced(String printed) {
        return printed.substring(0, 3) + " " + printed.substring(3);
    }

    private static double round(double degrees) {
        return Math.round(degrees * 1e4) / 1e4;
    }

    private static String f4(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String whole(double value) {
        return Long.toString(Math.round(value));
    }

    private static double signed(double degrees) {
        double d = degrees % 360.0;
        return d > 180.0 ? d - 360.0 : d <= -180.0 ? d + 360.0 : d;
    }

    private static String escaped(String value) {
        StringBuilder out = new StringBuilder();
        for (char c : value.toCharArray()) {
            out.append(c < 128 ? String.valueOf(c) : String.format("\\u%04x", (int) c));
        }
        return out.toString();
    }

    private static void ensureSafeToClean(Path outDir) throws IOException {
        if (!Files.exists(outDir)) {
            return;
        }
        try (var listing = Files.list(outDir)) {
            if (listing.findAny().isEmpty()) {
                return;
            }
        }
        Path manifest = outDir.resolve("manifest.properties");
        Properties owned = new Properties();
        if (Files.exists(manifest)) {
            owned.load(new StringReader(Files.readString(manifest)));
        }
        if (!"meteor-showers".equals(owned.getProperty("pack.name"))) {
            throw new IllegalStateException(outDir + " is not empty and is not this pack;"
                    + " refusing to write over files this generator does not own");
        }
    }

    private static void write(Path file, String text, Map<String, String> checksums)
            throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes);
        if (checksums != null) {
            checksums.put(file.getFileName().toString(), PinnedInputs.sha256Hex(bytes));
        }
    }

    private static String notice(String iauLastUpdate) {
        return """
                # Meteor showers

                JUranometria carries a small offline table of nine annual meteor
                showers: when each is active, where its radiant is, and when its
                nominal maximum falls.

                ## Activity, radiant and rate: International Meteor Organization

                %s

                The values are from the %s (%s), Table 5, the Working List of
                Visual Meteor Showers, and Table 6, the radiant positions during the
                year. The calendar states that the working list was correct according
                to the best information available in June 2026 (%s). For each of the nine showers
                the pack holds the calendar's activity dates, the date and solar
                longitude of maximum, the radiant at maximum, the atmospheric entry
                velocity, the population index, the zenithal hourly rate, and the
                radiant table's rows.

                The International Meteor Organization's data policy
                (https://www.imo.net/about/using-imo-data/) allows its published data
                to be used, reproduced and shared for any purpose with this credit,
                and asks that values a user computes be marked as the user's own.
                JUranometria carries a derived subset of nine showers, not the
                calendar in full or in substantial part and not the IMO's dataset as
                a file, database or service of its own.

                JUranometria is not affiliated with, sponsored by or endorsed by the
                International Meteor Organization, and does not use its logo.

                The nine showers' values were keyed twice from the calendar,
                independently, and the pack is written only when the two keyings
                agree in every field. The pack is one edition of the calendar. The
                IMO revises its calendar every year; a newer edition enters
                JUranometria as a new reviewed import that names its edition, and
                until then the values shown are this edition's.

                **Computed by JUranometria, not by the IMO:** the Sun's ecliptic
                longitude at the first and last date of each activity period and at
                each radiant row's date; every radiant position between or beyond the
                printed rows; every instant of nominal maximum; and everything said
                about a shower at a particular place and chart time. The calendar does
                not print the equinox of its radiant table; JUranometria assumes
                J2000.

                ## Identity: IAU Meteor Data Center

                The IAU number, three-letter code, name and, where the list states
                one, the parent body are from the IAU Meteor Data Center's list of
                established meteor showers, V.2 (last update: %s).
                One name is corrected: the list's solutions most often spell the
                Leonids' parent comet "55P/Temple-Tuttle"; JUranometria carries
                55P/Tempel\u2013Tuttle, the comet's name, and keeps the list's
                spelling in the pack's manifest.
                The Meteor Data Center asks that its database be cited as:

                - Jenniskens, P.; Jopek, T.J.; Janches, D.; Hajdukova, M.; Kokhirova,
                  G.I.; Rudawska, R.; 2020, Planetary and Space Science, Vol. 182,
                  article id. 104821
                - Jopek, T.J.; Kanuchova, Z.; 2017, Planetary and Space Science,
                  Volume 143, p. 3-6
                - Jopek, T.J.; Kanuchova, Z.; 2014, in The Meteoroids 2013, A.M.
                  University Press, p. 353-364
                - Jopek, T.J.; Jenniskens, P.M.; 2011, in Meteoroids: The Smallest
                  Solar System Bodies, NASA/CP-2011-216469, p. 7-13

                ## Not shipped

                The Global Meteor Network's shower table (CC BY 4.0) was used only to
                check the accuracy of the radiant evaluation and is not part of
                JUranometria. A small extract of it, credited, is held with the
                project's tests.
                """.formatted(CREDIT, EDITION, EDITION_REFERENCE, CORRECT_AS_OF, iauLastUpdate);
    }
}
