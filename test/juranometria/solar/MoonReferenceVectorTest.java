package juranometria.solar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solar.SolarSystemService.Side;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Moon against JPL Horizons, row by row, to the era-dependent
 * targets the owner ruled (issue #406, M9; issue #407).
 *
 * <p>{@code docs/studies/solar-system/horizons-moon/} holds Horizons'
 * responses kept whole - request URL, time and body digest in each
 * header - for the same five observers as the Sun: the named
 * instants, a 7-day matrix across 1900–2100 (30 days would alias the
 * 29.5-day lunation) and a daily 2026 series at Oslo: 52 870 rows,
 * plus the Sun on Oslo's grid so the bright-limb angle can be held to
 * Horizons' own inputs. Horizons is an independent implementation in
 * the same JPL authority family (DE441 there, DE440 here), with its
 * own time scales and Earth orientation.
 *
 * <p>Targets, as ruled on #406 (M9) and revised on #407 by
 * measurement. Through the exact-time interval: astrometric ≤ 1.0″ for
 * 1900–1961 (the accepted Espenak–Meeus ΔT sits about 1.3 s from the
 * record there, and the Moon moves 0.55″ a second), ≤ 0.5″ for
 * 1962–1971 (Horizons reads those civil instants about 0.7 s
 * differently), ≤ 0.3″ from 1972 (Place and Time's UT1 = UTC moves the
 * station up to 420 m, which is 0.24″ of the Moon's parallax at the
 * equator); apparent ≤ 1″; horizontal ≤ 20″; distance ≤ 1 km;
 * diameter ≤ 0.01″; illuminated fraction ≤ 0.01 percentage point;
 * elongation ≤ 1″; Horizons-definition phase angle ≤ 5″; bright-limb
 * angle ≤ 0.1° when 2 % ≤ k ≤ 98 %. After the interval the two families
 * themselves diverge for the Moon (DE441 assumes an undamped lunar
 * core, 11.9 km of distance by 2100) and the ΔT models differ by up to
 * 133 s: the ruled budgets are 90″ of direction, 25 km of distance,
 * 0.1″ of diameter (the 11.9 km at 0.005″ per km) and 0.03 percentage
 * points of illumination (133 s at 0.51″ per second on the phase
 * angle), horizontal unasserted. Each widened number bounds an
 * accepted time-scale or DE440/DE441 difference, never unexplained
 * implementation error, and beside every target the measured maximum
 * of 2026-09-29 is pinned, so drift cannot hide inside the wider
 * limit. A measurement beyond a target stops the work rather than
 * widening the number. Every worst case must be greater than zero:
 * two implementations, not one compared with itself.
 */
class MoonReferenceVectorTest {

    static final Path HORIZONS = Path.of("docs/studies/solar-system/horizons-moon");

    /**
     * East longitude, latitude, altitude km, as the requests were made:
     * at sea level, where the contract's observer stands. The Sun's
     * fixtures carry the sites' real heights, which cost the Sun under
     * 0.01″; Quito's 2.85 km costs the Moon 1.6″ of parallax, measured
     * on a first fetch before these responses replaced it.
     */
    static final Map<String, double[]> OBSERVERS = Map.of(
            "oslo", new double[] {10.75, 59.91, 0.0},
            "quito", new double[] {281.5, -0.18, 0.0},
            "cape-town", new double[] {18.42, -33.93, 0.0},
            "alert", new double[] {297.66, 82.50, 0.0},
            "chatham", new double[] {183.5, -43.95, 0.0});

    private static final DateTimeFormatter STAMP = DateTimeFormatter
            .ofPattern("uuuu-MMM-dd HH:mm:ss.SSS", Locale.ENGLISH);

    /** One Horizons row: quantities 1,2,4,10,13,14,20,23,24,31,43. */
    record Row(String site, Instant when, double ra, double dec,
               double raApp, double decApp, double az, double el,
               double illuminatedPercent, double diam,
               double deltaAu, double elongation, String leadsOrTrails,
               double phaseAngle, double eclLon, double eclLat) {
    }

    /** One row of the Sun on the Moon's Oslo grid: quantities 1,2,4,20,31. */
    record SunRow(Instant when, double raApp, double decApp, double eclLon) {
    }

    private static SolarSystemService service;
    private static LocalDate exactUntil;

    @BeforeAll
    static void loadTheService() {
        service();
    }

    /** The service, loaded once - here or from a sibling test. */
    static SolarSystemService service() {
        if (service == null) {
            service = SolarSystemService.load();
            exactUntil = service.timeScales().exactUntil();
        }
        return service;
    }

    static Instant stamp(String field) {
        String stamp = field.strip();
        if (!stamp.contains(".")) {
            stamp += ".000";
        }
        return LocalDateTime.parse(stamp, STAMP).toInstant(ZoneOffset.UTC);
    }

    static List<String> lines(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        return List.of(text.substring(text.indexOf("$$SOE") + 5,
                text.indexOf("$$EOE")).strip().split("\n"));
    }

    static List<Row> rows(String name) throws IOException {
        String site = OBSERVERS.keySet().stream()
                .filter(name::endsWith).findFirst().orElseThrow();
        List<Row> rows = new ArrayList<>();
        for (String line : lines(HORIZONS.resolve(name + ".txt"))) {
            String[] f = line.split(",");
            rows.add(new Row(site, stamp(f[0]), d(f[3]), d(f[4]), d(f[5]), d(f[6]),
                    d(f[7]), d(f[8]), d(f[9]), d(f[10]), d(f[13]), d(f[15]),
                    f[16].strip(), d(f[17]), d(f[18]), d(f[19])));
        }
        return rows;
    }

    static List<SunRow> sunRows() throws IOException {
        List<SunRow> rows = new ArrayList<>();
        for (String line : lines(HORIZONS.resolve("sun-matrix-7d-oslo.txt"))) {
            String[] f = line.split(",");
            rows.add(new SunRow(stamp(f[0]), d(f[5]), d(f[6]), d(f[11])));
        }
        return rows;
    }

    private static double d(String field) {
        return Double.parseDouble(field.strip());
    }

    static Observer observerAt(Row row) {
        double[] site = OBSERVERS.get(row.site);
        return new Observer(site[1], site[0], row.when);
    }

    static MoonObservation observe(Row row) {
        return (MoonObservation) service().observe(Body.MOON, observerAt(row));
    }

    static double arcsec(SkyPosition a, double raDeg, double decDeg) {
        return SunReferenceVectorTest.arcsec(a, raDeg, decDeg);
    }

    /** The signed difference of two angles, wrapped to (−180°, 180°]. */
    static double wrapped(double degrees) {
        double d = SkyFrame.normalise(degrees);
        return d > 180.0 ? d - 360.0 : d;
    }

    private static boolean exact(Instant when) {
        service();
        return when.atOffset(ZoneOffset.UTC).toLocalDate().isBefore(exactUntil);
    }

    private static String era(Instant when) {
        int year = when.atOffset(ZoneOffset.UTC).getYear();
        if (!exact(when)) {
            return "after";
        }
        return year < 1962 ? "1900-1961" : year < 1972 ? "1962-1971" : "1972-exact";
    }

    static List<String> fixtures(String prefix) throws IOException {
        try (Stream<Path> files = Files.list(HORIZONS)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith(prefix) && n.endsWith(".txt"))
                    .map(n -> n.substring(0, n.length() - 4))
                    .sorted().toList();
        }
    }

    @Test
    void everyRowAgreesWithHorizonsToTheEraTarget() throws IOException {
        Map<String, SunReferenceVectorTest.Worst> byEra = new LinkedHashMap<>();
        Map<String, Integer> rowsByEra = new LinkedHashMap<>();
        for (String era : List.of("1900-1961", "1962-1971", "1972-exact", "after")) {
            byEra.put(era, new SunReferenceVectorTest.Worst());
            rowsByEra.put(era, 0);
        }
        int sideAgreed = 0;
        int sideDisagreed = 0;
        double sideWorstRa = 0.0;
        String sideWorstAt = "";
        List<String> names = new ArrayList<>();
        names.addAll(fixtures("named-"));
        names.addAll(fixtures("matrix-7d-"));
        names.addAll(fixtures("dense-2026-"));
        for (String name : names) {
            for (Row row : rows(name)) {
                MoonObservation o = observe(row);
                String era = era(row.when);
                SunReferenceVectorTest.Worst w = byEra.get(era);
                rowsByEra.merge(era, 1, Integer::sum);
                String at = row.site + " " + row.when;
                w.note("astrometric", arcsec(o.astrometricJ2000(), row.ra, row.dec), at);
                w.note("apparent", arcsec(o.apparentOfDate(), row.raApp, row.decApp), at);
                w.note("horizontal", SunReferenceVectorTest.horizontalArcsec(
                        o.horizontal(), row.az, row.el), at);
                w.note("distance km", o.distanceKm() - row.deltaAu * 149_597_870.7, at);
                w.note("diameter", o.angularDiameterArcseconds() - row.diam, at);
                w.note("illuminated %", o.illuminatedFraction() * 100.0
                        - row.illuminatedPercent, at);
                w.note("elongation", (o.elongationDegrees() - row.elongation) * 3600.0, at);
                w.note("phase angle", (o.phaseAngleDegrees() - row.phaseAngle) * 3600.0, at);
                // Which side of the Sun, in this observer's sky: Horizons
                // says /T (trails: evening) or /L (leads: morning).
                Side horizons = row.leadsOrTrails.equals("/T") ? Side.EAST_OF_SUN
                        : Side.WEST_OF_SUN;
                if (horizons == o.side()) {
                    sideAgreed++;
                } else {
                    sideDisagreed++;
                    // How far the row is from the definition's own edge:
                    // equal right ascensions, or opposite ones.
                    double raEast = Math.abs(wrapped(o.apparentOfDate().raDegrees()
                            - sunApparentRa(o, row)));
                    raEast = Math.min(raEast, 180.0 - raEast);
                    if (raEast > sideWorstRa) {
                        sideWorstRa = raEast;
                        sideWorstAt = at + " (" + row.leadsOrTrails + ", ψ "
                                + row.elongation + "°)";
                    }
                }
            }
        }
        for (String era : byEra.keySet()) {
            report(era, byEra.get(era), rowsByEra.get(era));
        }
        System.out.printf(Locale.ROOT, "  side agreed %d, disagreed %d; worst"
                + " disagreement %.4f° of apparent RA at %s%n", sideAgreed,
                sideDisagreed, sideWorstRa, sideWorstAt);
        assertTrue(rowsByEra.get("1900-1961") > 15000
                && rowsByEra.get("1962-1971") > 2500
                && rowsByEra.get("1972-exact") > 14000
                && rowsByEra.get("after") > 19000, "the fixtures have their rows: "
                + rowsByEra);

        for (String era : List.of("1900-1961", "1962-1971", "1972-exact")) {
            SunReferenceVectorTest.Worst w = byEra.get(era);
            // The accepted ΔT model's residual before 1962; Horizons' own
            // reading of 1962-1971; UT1 = UTC through the station after.
            double astrometric = era.equals("1900-1961") ? 1.0
                    : era.equals("1962-1971") ? 0.5 : 0.3;
            hold(era, w, "astrometric", astrometric);
            hold(era, w, "apparent", 1.0);
            hold(era, w, "horizontal", 20.0);
            hold(era, w, "distance km", 1.0);
            hold(era, w, "diameter", 0.01);
            hold(era, w, "illuminated %", 0.01);
            hold(era, w, "elongation", 1.0);
            hold(era, w, "phase angle", 5.0);
        }
        SunReferenceVectorTest.Worst after = byEra.get("after");
        hold("after", after, "astrometric", 90.0);
        hold("after", after, "apparent", 90.0);
        hold("after", after, "distance km", 25.0);
        // The DE440/DE441 distance divergence, 11.9 km at 0.005″ per km.
        hold("after", after, "diameter", 0.1);
        // The ΔT models' 133 s on the phase angle: percentage points of k.
        hold("after", after, "illuminated %", 0.03);
        hold("after", after, "elongation", 90.0);
        hold("after", after, "phase angle", 90.0);
        // Horizontal after the exact interval: measured, printed, not asserted.

        // The measured maxima, pinned: what this implementation measured
        // on 2026-09-29 against these fixtures, to four decimals. A later
        // change that moves any of them is a change with a name.
        for (Map.Entry<String, double[]> pin : PINNED.entrySet()) {
            String[] key = pin.getKey().split("/");
            double worst = byEra.get(key[0]).get(key[1]);
            assertTrue(worst <= pin.getValue()[0] + 0.00005, key[0] + ": " + key[1]
                    + " measured " + worst + ", pinned at " + pin.getValue()[0]
                    + " at " + byEra.get(key[0]).where.get(key[1]));
        }

        for (SunReferenceVectorTest.Worst w : byEra.values()) {
            for (String key : w.values.keySet()) {
                assertTrue(w.get(key) > 0.0, key + ": two implementations, not"
                        + " one compared with itself");
            }
        }
        // The side is a definition, not a measurement: it may differ
        // from Horizons' only where the Moon and the Sun share a right
        // ascension, or sit opposite in it, to within the two readings'
        // own disagreement.
        assertTrue(sideDisagreed * 1000 < sideAgreed, "the side agrees with"
                + " Horizons on all but a few rows: " + sideDisagreed);
        assertTrue(sideWorstRa < 0.01, "and every disagreement is at RA"
                + " equality or opposition: " + sideWorstRa + "° at " + sideWorstAt);
    }

    /** The measured maxima of 2026-09-29, era/quantity, to four decimals. */
    static final Map<String, double[]> PINNED = Map.ofEntries(
            Map.entry("1900-1961/astrometric", new double[] {0.7398}),
            Map.entry("1900-1961/apparent", new double[] {0.8045}),
            Map.entry("1900-1961/horizontal", new double[] {1.3993}),
            Map.entry("1900-1961/distance km", new double[] {0.1010}),
            Map.entry("1900-1961/diameter", new double[] {0.0009}),
            Map.entry("1900-1961/illuminated %", new double[] {0.0027}),
            Map.entry("1900-1961/elongation", new double[] {0.8360}),
            Map.entry("1900-1961/phase angle", new double[] {0.8475}),
            Map.entry("1962-1971/astrometric", new double[] {0.2349}),
            Map.entry("1962-1971/apparent", new double[] {0.1662}),
            Map.entry("1962-1971/horizontal", new double[] {2.5290}),
            Map.entry("1962-1971/distance km", new double[] {0.0640}),
            Map.entry("1962-1971/diameter", new double[] {0.0007}),
            Map.entry("1962-1971/illuminated %", new double[] {0.0026}),
            Map.entry("1962-1971/elongation", new double[] {0.3836}),
            Map.entry("1962-1971/phase angle", new double[] {0.3776}),
            Map.entry("1972-exact/astrometric", new double[] {0.1976}),
            Map.entry("1972-exact/apparent", new double[] {0.2402}),
            Map.entry("1972-exact/horizontal", new double[] {12.1845}),
            Map.entry("1972-exact/distance km", new double[] {0.3400}),
            Map.entry("1972-exact/diameter", new double[] {0.0022}),
            Map.entry("1972-exact/illuminated %", new double[] {0.0026}),
            Map.entry("1972-exact/elongation", new double[] {0.3533}),
            Map.entry("1972-exact/phase angle", new double[] {0.3262}),
            Map.entry("after/astrometric", new double[] {86.6727}),
            Map.entry("after/apparent", new double[] {86.7864}),
            Map.entry("after/distance km", new double[] {11.9003}),
            Map.entry("after/diameter", new double[] {0.0610}),
            Map.entry("after/illuminated %", new double[] {0.0185}),
            Map.entry("after/elongation", new double[] {79.2245}),
            Map.entry("after/phase angle", new double[] {79.2696}));

    /** The Sun's apparent RA of date at the row, through the service. */
    private static double sunApparentRa(MoonObservation moon, Row row) {
        SolarSystemService.SunObservation sun = (SolarSystemService.SunObservation)
                service.observe(Body.SUN, observerAt(row));
        return sun.apparentOfDate().raDegrees();
    }

    private static void hold(String era, SunReferenceVectorTest.Worst w,
                             String key, double target) {
        assertTrue(w.get(key) <= target, era + ": " + key + " ≤ " + target
                + "; worst " + w.get(key) + " at " + w.where.get(key));
    }

    private static void report(String era, SunReferenceVectorTest.Worst w, int rows) {
        System.out.println("Moon vs Horizons, " + era + " (" + rows + " rows):");
        for (String key : w.values.keySet()) {
            System.out.printf(Locale.ROOT, "  %-14s worst %10.4f at %s%n", key,
                    w.get(key), w.where.get(key));
        }
    }

    @Test
    void theBrightLimbAngleAgreesWithOneComputedFromHorizonsOwnInputs()
            throws IOException {
        // Horizons publishes no bright-limb angle; Meeus 48.5 applied to
        // its own apparent Sun and Moon at Oslo is the oracle, away from
        // new and full where the angle means nothing.
        Map<Instant, SunRow> suns = new LinkedHashMap<>();
        for (SunRow s : sunRows()) {
            suns.put(s.when, s);
        }
        double worst = 0.0;
        String worstAt = "";
        int held = 0;
        int skipped = 0;
        for (Row row : rows("matrix-7d-oslo")) {
            SunRow sun = suns.get(row.when);
            assertTrue(sun != null, "the Sun is on the same grid at " + row.when);
            // The two implementations' k agree to 0.01 percentage point;
            // a row that close to a threshold may be classified either
            // way, so the oracle is held only clear of both thresholds.
            double k = row.illuminatedPercent / 100.0;
            if (k < SolarSystemService.NEAR_NEW_BELOW + 0.0005
                    || k >= SolarSystemService.NEAR_FULL_FROM - 0.0005) {
                skipped++;
                continue;
            }
            MoonObservation o = observe(row);
            assertEquals(SolarSystemService.LimbConditioning.WELL_DEFINED,
                    o.brightLimbConditioning(), "well-defined at k = " + k);
            double oracle = SolarSystemService.brightLimbAngle(
                    new SkyPosition(SkyFrame.normalise(sun.raApp), sun.decApp),
                    new SkyPosition(SkyFrame.normalise(row.raApp), row.decApp));
            double difference = Math.abs(wrapped(o.brightLimbAngleDegrees() - oracle));
            if (difference > worst) {
                worst = difference;
                worstAt = row.when + " k=" + k;
            }
            held++;
        }
        System.out.printf(Locale.ROOT, "Bright-limb angle vs Meeus 48.5 on"
                + " Horizons' own inputs, Oslo weekly: %d rows held, %d near"
                + " new/full skipped, worst %.4f° at %s%n", held, skipped, worst,
                worstAt);
        assertTrue(held > 8000 && skipped > 1000, held + " held, " + skipped
                + " skipped: about a sixth of the rows lie within 2 % of new or full");
        assertTrue(worst <= 0.1, "χ ≤ 0.1° away from new and full; worst "
                + worst + "° at " + worstAt);
        assertTrue(worst > 0.0, "two implementations");
    }

    @Test
    void waxingAndWaningIsTheSameForEveryObserverAtTheSameInstant()
            throws IOException {
        // The five matrices share one grid, so the classification can
        // be compared across observers instant by instant; the side in
        // each observer's sky may differ near conjunction and does.
        Map<Instant, SolarSystemService.Trend> trends = new LinkedHashMap<>();
        int sidesDiffered = 0;
        int compared = 0;
        Map<Instant, Side> sides = new LinkedHashMap<>();
        for (String name : fixtures("matrix-7d-")) {
            for (Row row : rows(name)) {
                MoonObservation o = observe(row);
                SolarSystemService.Trend seen = trends.putIfAbsent(row.when, o.trend());
                if (seen != null) {
                    assertEquals(seen, o.trend(), "waxing or waning is global,"
                            + " not " + row.site + "'s, at " + row.when);
                    compared++;
                    if (sides.get(row.when) != o.side()) {
                        sidesDiffered++;
                    }
                } else {
                    sides.put(row.when, o.side());
                }
                // And the trend agrees with the geometry it names.
                assertEquals(o.elongationInLongitudeDegrees() < 180.0,
                        o.trend() == SolarSystemService.Trend.WAXING, "at " + row.when);
            }
        }
        System.out.println("Trend compared across observers at " + compared
                + " (instant, observer) pairs; the observer-specific side differed"
                + " at " + sidesDiffered + " of them");
        assertTrue(compared > 40000, "compared " + compared);
        assertTrue(sidesDiffered > 0, "the side is the observer's own, and near"
                + " conjunction it shows: " + sidesDiffered);
    }

    @Test
    void theNamedInstantsAreEachWithinTheirTarget() throws IOException {
        List<String> checked = new ArrayList<>();
        for (String name : fixtures("named-")) {
            for (Row row : rows(name)) {
                MoonObservation o = observe(row);
                double astrometric = arcsec(o.astrometricJ2000(), row.ra, row.dec);
                int year = row.when.atOffset(ZoneOffset.UTC).getYear();
                double target = !exact(row.when) ? 90.0 : year < 1962 ? 1.0
                        : year < 1972 ? 0.5 : 0.3;
                assertTrue(astrometric <= target, row.site + " " + row.when
                        + ": astrometric " + astrometric + "″");
                checked.add(row.site + " " + row.when);
            }
        }
        assertEquals(5 * 13, checked.size(),
                "five observers × thirteen named instants");
    }

    @Test
    void eachFixtureStillHashesToTheDigestItRecords() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        int checked = 0;
        try (Stream<Path> files = Files.list(HORIZONS)) {
            for (Path file : files.filter(p -> p.getFileName().toString()
                    .endsWith(".txt")).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                if (!text.startsWith("# JPL Horizons response")) {
                    continue;
                }
                String recorded = text.lines()
                        .filter(l -> l.startsWith("# body-sha256: "))
                        .map(l -> l.substring("# body-sha256: ".length()))
                        .findFirst().orElseThrow();
                String bodyText = text.substring(text.indexOf("API VERSION"));
                String actual = HexFormat.of().formatHex(
                        sha.digest(bodyText.getBytes(StandardCharsets.UTF_8)));
                assertEquals(recorded, actual, file + " carries the response it"
                        + " was given, byte for byte");
                assertTrue(text.contains("# request-url: https://ssd.jpl.nasa.gov/")
                        && text.contains("# requested-utc: "),
                        file + " records its request and when it was made");
                checked++;
            }
        }
        assertEquals(13, checked, "thirteen responses kept whole: five named,"
                + " five matrices, the daily year, the geocentric case and the"
                + " Sun on Oslo's grid");
    }
}
