package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService.Side;
import juranometria.solar.MercuryService;
import juranometria.solar.MercuryService.MercuryObservation;

/**
 * The Mercury service against JPL Horizons, row by row, over the
 * committed fixtures under {@code docs/studies/mercury/horizons/} (issue
 * #529), and against Espenak's published named events
 * ({@code docs/studies/mercury/mercury-events.txt}): the worst difference
 * of every quantity the contract names, by era, beside the target the
 * owner ruled on #528; the side of the Sun against Horizons' /T and /L
 * on every row; χ against Meeus 48.5 applied to Horizons' own Sun and
 * Mercury wherever both were fetched. One engine, two readers: the
 * tests hold its results to the targets, the report main prints them.
 */
public final class MercuryComparison {

    static final Path HORIZONS = Path.of("docs/studies/mercury/horizons");
    static final Path EVENTS = Path.of("docs/studies/mercury/mercury-events.txt");
    static final double AU_KM = 149_597_870.7;
    static final double ARCSEC = 3600.0;

    /** East longitude, latitude, as the requests were made, at sea level. */
    static final Map<String, double[]> OBSERVERS = new LinkedHashMap<>();

    static {
        OBSERVERS.put("oslo", new double[] {10.75, 59.91});
        OBSERVERS.put("quito", new double[] {281.5, -0.18});
        OBSERVERS.put("cape-town", new double[] {18.42, -33.93});
        OBSERVERS.put("alert", new double[] {297.66, 82.50});
        OBSERVERS.put("chatham", new double[] {183.5, -43.95});
    }

    public static final List<String> ERAS = List.of("1900-1961", "1962-1971", "1972-exact", "after");

    /** The quantities, in report order. */
    public static final List<String> QUANTITIES = List.of("astrometric", "apparent, elongation > 1°",
            "horizontal", "distance km", "diameter", "elongation, > 1°", "phase angle (S-T-O), > 1°",
            "illuminated points", "χ, k in [2 %, 98 %)");

    /**
     * The targets the owner ruled on #528 (ruling 7), by quantity and
     * era; {@code NaN} is measured and printed, never asserted.
     */
    public static final Map<String, double[]> TARGETS = new LinkedHashMap<>();

    static {
        // docs/decisions/mercury-system.md, "Accuracy, measured" (the proposed
        // table) and "The ruling", 7: the complete table, without widening.
        TARGETS.put("astrometric", new double[] {0.2, 0.1, 0.05, 15.0});
        TARGETS.put("apparent, elongation > 1°", new double[] {0.5, 0.2, 0.2, 15.0});
        TARGETS.put("horizontal", new double[] {20.0, 20.0, 20.0, Double.NaN});
        TARGETS.put("distance km", new double[] {100.0, 30.0, 2.0, 8000.0});
        TARGETS.put("diameter", new double[] {0.001, 0.001, 0.001, 0.01});
        TARGETS.put("elongation, > 1°", new double[] {0.5, 0.5, 0.5, 15.0});
        TARGETS.put("phase angle (S-T-O), > 1°", new double[] {1.0, 1.0, 1.0, 45.0});
        TARGETS.put("illuminated points", new double[] {0.01, 0.01, 0.01, 0.02});
        TARGETS.put("χ, k in [2 %, 98 %)", new double[] {1.0, 1.0, 1.0, 20.0});
    }

    private static final DateTimeFormatter STAMP = DateTimeFormatter
            .ofPattern("uuuu-MMM-dd HH:mm:ss.SSS", Locale.ENGLISH);

    /** The largest absolute value seen for a quantity, and where. */
    public static final class Worst {
        private final Map<String, Double> values = new TreeMap<>();
        private final Map<String, String> where = new TreeMap<>();

        /**
         * Magnitudes compare at 1e-7 of their unit, so rows that tie at
         * the fixtures' own precision - the diameter's 0.000005″ is
         * Horizons' rounding on most rows - tie exactly and the first in
         * file order stands on every platform, rather than the last bit
         * of one machine's arithmetic choosing (CI on #505).
         */
        void note(String quantity, double value, String at) {
            double magnitude = Math.round(Math.abs(value) * 1e7) / 1e7;
            if (!values.containsKey(quantity) || magnitude > values.get(quantity)) {
                values.put(quantity, magnitude);
                where.put(quantity, at);
            }
        }

        public double of(String quantity) {
            return values.getOrDefault(quantity, 0.0);
        }

        public String where(String quantity) {
            return where.getOrDefault(quantity, "-");
        }
    }

    /** One Espenak row against the instant and value DE440 gives through the service. */
    public record Event(String name, String row, Instant listed, Instant ours,
                        Double ourElongationDegrees, Double publishedElongationDegrees,
                        Side ourSide, Character publishedSide, double flatnessArcseconds) {

        public boolean conjunction() {
            return name.contains("conjunction");
        }

        public long minutesFromListed() {
            return Duration.between(listed, ours).toMinutes();
        }
    }

    /**
     * One NASA/GSFC transit row against the service: the instant of
     * least geocentric apparent separation of the centres within six
     * hours of the listed greatest transit, that separation, and
     * whether geocentric Mercury reads in front of the Sun at the
     * listed instant.
     */
    public record Transit(String name, String row, Instant listed, Instant ours,
                          double publishedMinimumSeparationArcseconds,
                          double ourMinimumSeparationArcseconds, boolean inFrontAtListed) {

        public long minutesFromListed() {
            return Duration.between(listed, ours).toMinutes();
        }

        public double secondsFromListed() {
            return Duration.between(listed, ours).toMillis() / 1000.0;
        }
    }

    /** Everything measured. */
    public static final class Result {
        public final Map<String, Worst> worst = new TreeMap<>();
        public final Map<String, Integer> rows = new TreeMap<>();
        public int sideAgreed;
        public final List<String> sideDisagreements = new ArrayList<>();
        public int chiRows;
        public final List<Event> events = new ArrayList<>();
        public final List<Transit> transits = new ArrayList<>();
        public double parallaxArcseconds;
        public String parallaxWhere = "-";
        public final Map<MercuryService.Phase, Integer> phases = new TreeMap<>();
        public LocalDate exactUntil;
    }

    private final MercuryService service;

    public MercuryComparison(MercuryService service) {
        this.service = service;
    }

    static Instant instant(String stamp) {
        String s = stamp.strip();
        if (!s.contains(".")) {
            s = s + ".000";
        }
        return LocalDateTime.parse(s, STAMP).toInstant(ZoneOffset.UTC);
    }

    /** A kept response's table, as comma-separated fields, stripped. */
    public static List<String[]> rows(String name) throws IOException {
        String text = Files.readString(HORIZONS.resolve(name + ".txt"), StandardCharsets.UTF_8);
        int from = text.indexOf("$$SOE");
        int to = text.indexOf("$$EOE");
        if (from < 0 || to < 0) {
            throw new IOException(name + " is not a complete Horizons response");
        }
        List<String[]> out = new ArrayList<>();
        for (String line : text.substring(from + 5, to).strip().split("\n")) {
            String[] f = line.split(",");
            for (int i = 0; i < f.length; i++) {
                f[i] = f[i].strip();
            }
            out.add(f);
        }
        return out;
    }

    /** The Mercury responses committed, by stem (without the body suffix). */
    static List<String> mercuryStems() throws IOException {
        List<String> stems = new ArrayList<>();
        try (Stream<Path> files = Files.list(HORIZONS)) {
            for (Path p : files.sorted().toList()) {
                String n = p.getFileName().toString();
                if (n.endsWith("-199.txt")) {
                    stems.add(n.substring(0, n.length() - "-199.txt".length()));
                } else if (n.endsWith(".txt") && !n.endsWith("-10.txt")) {
                    stems.add(n.substring(0, n.length() - ".txt".length()));
                }
            }
        }
        return stems;
    }

    static String site(String stem) {
        if (stem.contains("geocentric")) {
            return "geocentric";
        }
        for (String s : OBSERVERS.keySet()) {
            if (stem.contains(s)) {
                return s;
            }
        }
        throw new IllegalArgumentException("no observer in " + stem);
    }

    String era(Instant when) {
        LocalDate day = when.atOffset(ZoneOffset.UTC).toLocalDate();
        if (day.getYear() < 1962) {
            return "1900-1961";
        }
        if (day.getYear() < 1972) {
            return "1962-1971";
        }
        return day.isBefore(service.timeScales().exactUntil()) ? "1972-exact" : "after";
    }

    public Result run() throws IOException {
        Result result = new Result();
        result.exactUntil = service.timeScales().exactUntil();
        for (String stem : mercuryStems()) {
            String site = site(stem);
            String mercuryFile = Files.exists(HORIZONS.resolve(stem + "-199.txt")) ? stem + "-199" : stem;
            Map<Instant, String[]> sun = new HashMap<>();
            if (Files.exists(HORIZONS.resolve(stem + "-10.txt"))) {
                for (String[] f : rows(stem + "-10")) {
                    sun.put(instant(f[0]), f);
                }
            }
            for (String[] f : rows(mercuryFile)) {
                Instant when = instant(f[0]);
                MercuryObservation o;
                MercuryObservation geo = null;
                if (site.equals("geocentric")) {
                    o = service.observeGeocentric(when);
                } else {
                    double[] place = OBSERVERS.get(site);
                    o = service.observe(new Observer(place[1], place[0], when));
                    geo = service.observeGeocentric(when);
                }
                compare(result, era(when), site + " " + when, o, geo, f, sun.get(when));
            }
        }
        events(result);
        return result;
    }

    // Mercury columns (quantities 1,2,4,9,10,13,19,20,23,24,43): 3 RA 4 DEC 5 RAapp
    // 6 DECapp 7 Az 8 El 9 APmag 10 S-brt 11 Illu% 12 Ang-diam 13 r 14 rdot
    // 15 delta 16 deldot 17 S-O-T 18 /r 19 S-T-O 20 phi 21 PABlon 22 PABlat.
    // The Sun's (1,2,4,20): 3 RA 4 DEC 5 RAapp 6 DECapp 7 Az 8 El 9 delta 10 deldot.
    private void compare(Result result, String era, String tag, MercuryObservation o,
                         MercuryObservation geocentric, String[] f, String[] sun) {
        Worst w = result.worst.computeIfAbsent(era, e -> new Worst());
        result.rows.merge(era, 1, Integer::sum);
        result.phases.merge(o.phase(), 1, Integer::sum);
        double elongation = Double.parseDouble(f[17]);
        w.note("astrometric", separation(o.astrometricJ2000(), d(f[3]), d(f[4])), tag);
        if (elongation > 1.0) {
            w.note("apparent, elongation > 1°", separation(o.apparentOfDate(), d(f[5]), d(f[6])), tag);
            w.note("elongation, > 1°", (o.elongationDegrees() - elongation) * ARCSEC, tag);
            w.note("phase angle (S-T-O), > 1°", (o.phaseAngleDegrees() - d(f[19])) * ARCSEC, tag);
        }
        if (o.horizontal() != null) {
            w.note("horizontal", separation(new SkyPosition(o.horizontal().azimuthDegrees(),
                    o.horizontal().altitudeDegrees()), d(f[7]), d(f[8])), tag);
        }
        w.note("distance km", o.distanceKm() - d(f[15]) * AU_KM, tag);
        w.note("diameter", o.solidBodyDiameterArcseconds() - d(f[12]), tag);
        w.note("illuminated points", o.illuminatedFraction() * 100.0 - d(f[11]), tag);
        boolean east = f[18].contains("T");
        if (east == (o.side() == Side.EAST_OF_SUN)) {
            result.sideAgreed++;
        } else {
            result.sideDisagreements.add(tag + ": ours " + o.side() + ", Horizons " + f[18]);
        }
        if (sun != null) {
            double k = d(f[11]);
            if (k >= 2.0 && k < 98.0) {
                double oracle = brightLimb(d(sun[5]), d(sun[6]), d(f[5]), d(f[6]));
                w.note("χ, k in [2 %, 98 %)", wrap(o.brightLimbAngleDegrees() - oracle) * ARCSEC, tag);
            }
            result.chiRows++;
        }
        if (geocentric != null) {
            double parallax = separation(o.astrometricJ2000(),
                    geocentric.astrometricJ2000().raDegrees(), geocentric.astrometricJ2000().decDegrees());
            if (parallax > result.parallaxArcseconds) {
                result.parallaxArcseconds = parallax;
                result.parallaxWhere = tag;
            }
        }
    }

    // ---- Espenak's named events ---------------------------------------------

    private void events(Result result) throws IOException {
        for (String line : Files.readAllLines(EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            if (f[0].contains("occultation")) {
                continue; // named instants only, as ruled: no assertion is made on them
            }
            Instant listed = Instant.parse(f[1].replace("Z", ":00Z"));
            double etListed = service.timeScales().tt(listed).secondsPastJ2000();
            if (f[0].contains("transit")) {
                transit(result, f, listed, etListed);
                continue;
            }
            double from = etListed - 3 * 86400.0;
            double to = etListed + 3 * 86400.0;
            if (f[0].contains("conjunction")) {
                double et = conjunction(from, to);
                Instant ours = listed.plusMillis(Math.round((et - etListed) * 1000.0));
                result.events.add(new Event(f[0], f[2], listed, ours, null, null,
                        null, null, 0.0));
            } else {
                double et = greatestElongation(from, to);
                Instant ours = listed.plusMillis(Math.round((et - etListed) * 1000.0));
                MercuryObservation at = service.geocentricAtEt(et);
                MercuryObservation atListed = service.geocentricAtEt(etListed);
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("([0-9.]+)°([EW])").matcher(f[2]);
                if (!m.find()) {
                    throw new IOException("no elongation in " + f[2]);
                }
                result.events.add(new Event(f[0], f[2], listed, ours, at.elongationDegrees(),
                        Double.parseDouble(m.group(1)), at.side(), m.group(2).charAt(0),
                        (at.elongationDegrees() - atListed.elongationDegrees()) * ARCSEC));
            }
        }
    }

    /**
     * A transit row: the published columns are year, month, day,
     * contacts I, II, greatest, III, IV, the minimum separation of the
     * centres in arcseconds, the Sun's RA and Dec, GST and the series.
     */
    private void transit(Result result, String[] f, Instant listed, double etListed) {
        String[] c = f[2].trim().split("\\s+");
        double published = Double.parseDouble(c[8]);
        double et = leastSeparation(etListed - 6 * 3600.0, etListed + 6 * 3600.0);
        Instant ours = listed.plusMillis(Math.round((et - etListed) * 1000.0));
        MercuryObservation at = service.geocentricAtEt(et);
        MercuryObservation atListed = service.geocentricAtEt(etListed);
        result.transits.add(new Transit(f[0], f[2], listed, ours, published,
                at.elongationDegrees() * ARCSEC,
                atListed.phase() == MercuryService.Phase.IN_FRONT_OF_SUN));
    }

    /** The geocentric apparent separation's minimum, by golden section, to a second. */
    private double leastSeparation(double from, double to) {
        double g = (Math.sqrt(5.0) - 1.0) / 2.0;
        double a = from;
        double b = to;
        double c = b - g * (b - a);
        double d = a + g * (b - a);
        while (b - a > 1.0) {
            if (service.geocentricAtEt(c).elongationDegrees()
                    < service.geocentricAtEt(d).elongationDegrees()) {
                b = d;
            } else {
                a = c;
            }
            c = b - g * (b - a);
            d = a + g * (b - a);
        }
        return (a + b) / 2.0;
    }

    /** The zero of the geocentric apparent longitude difference, to a millisecond. */
    private double conjunction(double from, double to) {
        double step = 3600.0;
        double a = from;
        double fa = service.geocentricLongitudeFromSunAtEt(a);
        for (double b = from + step; b <= to; b += step) {
            double fb = service.geocentricLongitudeFromSunAtEt(b);
            if (Math.signum(fa) != Math.signum(fb) && Math.abs(fa - fb) < 90.0) {
                double lo = a;
                double hi = b;
                while (hi - lo > 0.001) {
                    double mid = (lo + hi) / 2.0;
                    double fm = service.geocentricLongitudeFromSunAtEt(mid);
                    if (Math.signum(fm) == Math.signum(fa)) {
                        lo = mid;
                    } else {
                        hi = mid;
                    }
                }
                return (lo + hi) / 2.0;
            }
            a = b;
            fa = fb;
        }
        throw new IllegalStateException("no conjunction within three days");
    }

    /** The geocentric elongation's maximum, by golden section, to a second. */
    private double greatestElongation(double from, double to) {
        double g = (Math.sqrt(5.0) - 1.0) / 2.0;
        double a = from;
        double b = to;
        double c = b - g * (b - a);
        double d = a + g * (b - a);
        while (b - a > 1.0) {
            if (service.geocentricAtEt(c).elongationDegrees()
                    > service.geocentricAtEt(d).elongationDegrees()) {
                b = d;
            } else {
                a = c;
            }
            c = b - g * (b - a);
            d = a + g * (b - a);
        }
        return (a + b) / 2.0;
    }

    // ---- the oracle's own arithmetic ------------------------------------------

    /** Meeus 48.5 from Horizons' own Sun (a0, d0) and Mercury (a, d), degrees. */
    public static double brightLimb(double a0deg, double d0deg, double adeg, double ddeg) {
        double a0 = Math.toRadians(a0deg);
        double d0 = Math.toRadians(d0deg);
        double a = Math.toRadians(adeg);
        double d = Math.toRadians(ddeg);
        double y = Math.cos(d0) * Math.sin(a0 - a);
        double x = Math.sin(d0) * Math.cos(d) - Math.cos(d0) * Math.sin(d) * Math.cos(a0 - a);
        double chi = Math.toDegrees(Math.atan2(y, x));
        return chi < 0.0 ? chi + 360.0 : chi;
    }

    /**
     * The angle between two directions, arcseconds, by the atlas's own
     * haversine (as {@code JovianComparison}): well conditioned at the
     * milliarcsecond differences the exact era measures, where the arc
     * cosine of a dot product is quantised at machine precision and
     * rounds differently on different hardware (found by CI on #505).
     */
    static double separation(SkyPosition p, double raDeg, double decDeg) {
        return p.separationDegrees(new SkyPosition(raDeg, decDeg)) * ARCSEC;
    }

    static double wrap(double deg) {
        return ((deg + 540.0) % 360.0) - 180.0;
    }

    private static double d(String s) {
        return Double.parseDouble(s);
    }

    // ---- the report -------------------------------------------------------------

    public static String report(Result r) {
        StringBuilder out = new StringBuilder();
        out.append("## Against Horizons, worst absolute difference by era, beside the ruled target\n\n");
        out.append("| quantity |");
        for (String e : ERAS) {
            out.append(' ').append(e).append(" |");
        }
        out.append("\n|---|");
        for (int i = 0; i < ERAS.size(); i++) {
            out.append("---|");
        }
        out.append("\n| rows |");
        for (String e : ERAS) {
            out.append(' ').append(r.rows.getOrDefault(e, 0)).append(" |");
        }
        out.append('\n');
        for (String q : QUANTITIES) {
            out.append("| ").append(q).append(" |");
            for (int i = 0; i < ERAS.size(); i++) {
                Worst w = r.worst.get(ERAS.get(i));
                double v = w == null ? 0.0 : w.of(q);
                double t = TARGETS.get(q)[i];
                out.append(String.format(Locale.ROOT, " %s %s |", value(q, v),
                        Double.isNaN(t) ? "(not asserted)" : "(≤ " + value(q, t) + ")"));
            }
            out.append('\n');
        }
        out.append(String.format(Locale.ROOT, "%nThe exact-time interval ends %s. Arcseconds"
                + " unless named; illuminated in percentage points.%n%n", r.exactUntil));
        out.append("Where the worst rows are:\n\n");
        for (String e : ERAS) {
            Worst w = r.worst.get(e);
            if (w == null) {
                continue;
            }
            for (String q : QUANTITIES) {
                out.append("- ").append(e).append(", ").append(q).append(": ")
                        .append(value(q, w.of(q))).append(" at ").append(w.where(q)).append('\n');
            }
        }
        out.append(String.format(Locale.ROOT, "%n## The side of the Sun%n%nHorizons' /T and /L"
                        + " against the service's side by apparent right ascension of date: %d rows agree,"
                        + " %d disagree.%n", r.sideAgreed, r.sideDisagreements.size()));
        for (String s : r.sideDisagreements) {
            out.append("- ").append(s).append('\n');
        }
        out.append(String.format(Locale.ROOT, "%nχ was compared on %d rows where Horizons' Sun was"
                + " fetched beside Mercury.%n%nThe parallax the service applies (topocentric minus"
                + " geocentric astrometric place) reaches %.2f″ at %s.%n", r.chiRows,
                r.parallaxArcseconds, r.parallaxWhere));
        out.append("\nThe phase words over every row: ");
        StringBuilder words = new StringBuilder();
        for (Map.Entry<MercuryService.Phase, Integer> e : r.phases.entrySet()) {
            words.append(words.length() == 0 ? "" : ", ").append(e.getKey()).append(' ').append(e.getValue());
        }
        out.append(words).append(".\n");
        out.append("\n## Espenak's named events against DE440, through the service\n\n"
                + "Conjunction: the geocentric apparent ecliptic longitudes of date equal; greatest"
                + " elongation: the geocentric apparent elongation's maximum. Ruled assertions (#528,"
                + " ruling 6): a conjunction falls within ±1 hour of the listed hour - Espenak's"
                + " Mercury rows read as rounded to the nearest hour, so the instants fall on both"
                + " sides of it; a greatest elongation agrees to 0.1° on its side and falls within"
                + " ±2 hours - the maximum is too flat for more: the elongation at the listed hour"
                + " is within the flatness below of the maximum. The two lunar occultations are"
                + " named instants only.\n\n");
        out.append("| case | published row | DE440 (UT) | from the listed hour | elongation | side | flatness |\n"
                + "|---|---|---|---|---|---|---|\n");
        for (Event e : r.events) {
            out.append(String.format(Locale.ROOT, "| %s | %s | %s | %+d min | %s | %s | %s |%n",
                    e.name(), e.row(), e.ours().toString().substring(0, 19).replace('T', ' '),
                    e.minutesFromListed(),
                    e.conjunction() ? "" : String.format(Locale.ROOT, "%.3f° (published %.1f°)",
                            e.ourElongationDegrees(), e.publishedElongationDegrees()),
                    e.conjunction() ? "" : e.ourSide() + " (published " + e.publishedSide() + ")",
                    e.conjunction() ? "" : String.format(Locale.ROOT, "%.3f″", e.flatnessArcseconds())));
        }
        out.append("\n## The transits (NASA/GSFC, Espenak) against DE440, through the service\n\n"
                + "Greatest transit: the instant of least geocentric apparent separation of the"
                + " centres, within six hours of the listed one; beside it the minimum separation"
                + " against the published one, and whether geocentric Mercury reads in front of the"
                + " Sun at the listed instant. Ruled assertions (#528, ruling 6): the instant within"
                + " ±5 minutes, the separation within 0.2″, in front of the Sun at every listed"
                + " greatest transit.\n\n");
        out.append("| transit | published greatest | DE440 (UT) | from the listed minute | min. separation"
                + " | published | in front at the listed instant |\n|---|---|---|---|---|---|---|\n");
        for (Transit t : r.transits) {
            out.append(String.format(Locale.ROOT, "| %s | %s | %s | %+.1f min | %.1f″ | %.1f″ | %s |%n",
                    t.name(), t.listed().toString().substring(0, 16).replace('T', ' '),
                    t.ours().toString().substring(0, 19).replace('T', ' '),
                    t.secondsFromListed() / 60.0, t.ourMinimumSeparationArcseconds(),
                    t.publishedMinimumSeparationArcseconds(), t.inFrontAtListed() ? "yes" : "no"));
        }
        return out.toString();
    }

    private static String value(String quantity, double v) {
        if (quantity.equals("distance km")) {
            return String.format(Locale.ROOT, "%.1f km", v);
        }
        if (quantity.equals("diameter")) {
            return String.format(Locale.ROOT, "%.4f″", v);
        }
        if (quantity.equals("illuminated points")) {
            return String.format(Locale.ROOT, "%.4f", v);
        }
        return String.format(Locale.ROOT, "%.3f″", v);
    }
}
