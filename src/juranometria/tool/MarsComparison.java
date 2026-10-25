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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.solar.MarsService;
import juranometria.solar.MarsService.MarsObservation;
import juranometria.solar.MarsService.MoonPlace;

/**
 * The Mars service against JPL Horizons, row by row, over the committed
 * fixtures under {@code docs/studies/mars-system/horizons/} (issue #602),
 * and against Espenak's published oppositions and conjunctions
 * ({@code docs/studies/mars-system/mars-events.txt}): the worst difference
 * of every quantity the contract names, by era, beside the target the
 * owner ruled on #601 (M-10), and the moons' states against Horizons'
 * codes on Horizons' own definition (M-8). One engine, two readers: the
 * tests hold its results to the targets, the report main prints them.
 *
 * <p>Columns are read by Horizons' own header names, never by position.
 */
public final class MarsComparison {

    static final Path HORIZONS = Path.of("docs/studies/mars-system/horizons");
    static final Path EVENTS = Path.of("docs/studies/mars-system/mars-events.txt");
    static final double AU_KM = 149_597_870.7;
    static final double ARCSEC = 3600.0;

    static final Map<String, double[]> OBSERVERS = new LinkedHashMap<>();

    static {
        OBSERVERS.put("oslo", new double[] {10.75, 59.91});
        OBSERVERS.put("quito", new double[] {281.5, -0.18});
        OBSERVERS.put("cape-town", new double[] {18.42, -33.93});
        OBSERVERS.put("alert", new double[] {297.66, 82.50});
        OBSERVERS.put("chatham", new double[] {183.5, -43.95});
    }

    public static final List<String> ERAS = List.of("1900-1961", "1962-1971", "1972-exact", "after");

    /** The quantities, in report order: Mars's, then the moons'. */
    public static final List<String> QUANTITIES = List.of(
            "Mars astrometric", "Mars apparent, > 1° from the Sun", "Mars horizontal",
            "Mars distance km", "equatorial diameter", "illuminated points", "phase angle, against phi",
            "pole position angle", "moons X, Y, separation, > 1° from the Sun",
            "moons position angle, > 20″ from Mars", "moons horizontal");

    /** Printed beside the targets and never asserted: no ruled target names them. */
    public static final List<String> PRINTED = List.of(
            "tilt, read planetodetic, against ObsSub-LAT", "pole right ascension", "pole declination",
            "moons position angle, within 20″ of Mars", "moons apparent, within 1° of the Sun");

    /**
     * The targets the owner ruled on #601 (M-10), by quantity and era;
     * {@code NaN} is measured and printed, never asserted.
     */
    public static final Map<String, double[]> TARGETS = new LinkedHashMap<>();

    static {
        // docs/decisions/mars-system.md, "Accuracy targets, proposed", ruled as
        // proposed (M-10): the complete table, without widening.
        double nan = Double.NaN;
        TARGETS.put("Mars astrometric", new double[] {0.01, 0.05, 0.01, 0.05});
        TARGETS.put("Mars apparent, > 1° from the Sun", new double[] {0.3, 0.1, 0.1, 0.5});
        TARGETS.put("Mars horizontal", new double[] {20.0, 20.0, 20.0, nan});
        TARGETS.put("Mars distance km", new double[] {1.0, 20.0, 1.0, 20.0});
        TARGETS.put("equatorial diameter", new double[] {0.001, 0.001, 0.001, 0.01});
        TARGETS.put("illuminated points", new double[] {0.001, 0.001, 0.001, 0.01});
        TARGETS.put("phase angle, against phi", new double[] {0.5, 0.5, 0.5, 10.0});
        TARGETS.put("pole position angle", new double[] {0.5, 0.5, 0.5, 0.5});
        TARGETS.put("moons X, Y, separation, > 1° from the Sun", new double[] {nan, nan, 0.05, 0.5});
        TARGETS.put("moons position angle, > 20″ from Mars", new double[] {nan, nan, 0.01, 0.01});
        TARGETS.put("moons horizontal", new double[] {nan, nan, 20.0, nan});
    }

    /**
     * The named Phobos boundary rows (#601, M-8): instants where Mars's
     * umbra is crossed within Phobos's own radius, so the state on
     * Horizons' sphere may read differently. Any other disagreement is a
     * finding.
     */
    public static final Set<String> NAMED_BOUNDARY_INSTANTS = Set.of(
            "2028-12-10T00:00:00Z", "2034-01-08T00:00:00Z");

    private static final DateTimeFormatter STAMP = DateTimeFormatter
            .ofPattern("uuuu-MMM-dd HH:mm:ss.SSS", Locale.ENGLISH);

    /** The largest absolute value seen for a quantity, and where; ties keep the first. */
    public static final class Worst {
        private final Map<String, Double> values = new TreeMap<>();
        private final Map<String, String> where = new TreeMap<>();

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

        public boolean has(String quantity) {
            return values.containsKey(quantity);
        }

        public String where(String quantity) {
            return where.getOrDefault(quantity, "-");
        }
    }

    /** One Espenak row against the instant DE440 gives through the service. */
    public record Event(String row, boolean opposition, Instant listed, Instant ours) {

        public long minutesFromListed() {
            return Duration.between(listed, ours).toMinutes();
        }
    }

    /** One state disagreement against Horizons' code. */
    public record StateCase(String moon, String tag, Instant instant, String ours, String horizons) {
    }

    public static final class Result {
        public final Map<String, Worst> worst = new TreeMap<>();
        public final Map<String, Integer> rows = new TreeMap<>();
        public final Map<String, Integer> moonRows = new TreeMap<>();
        public final Map<String, Integer> stateAgreed = new TreeMap<>();
        public final List<StateCase> stateDisagreements = new ArrayList<>();
        public final Map<String, Integer> codes = new TreeMap<>();
        public final Map<String, Integer> figureDiffers = new TreeMap<>();
        public final Map<MarsService.Phase, Integer> phases = new TreeMap<>();
        public final List<Event> events = new ArrayList<>();
        public LocalDate exactUntil;
    }

    private final MarsService service;

    public MarsComparison(MarsService service) {
        this.service = service;
    }

    public static Instant instant(String stamp) {
        String s = stamp.strip();
        if (!s.contains(".")) {
            s = s + ".000";
        }
        return LocalDateTime.parse(s, STAMP).toInstant(ZoneOffset.UTC);
    }

    /** A kept response: its header names and its rows, by name. */
    public record Table(List<String> names, List<String[]> rows) {

        public int column(String prefix) {
            for (int i = 0; i < names.size(); i++) {
                if (names.get(i).startsWith(prefix)) {
                    return i;
                }
            }
            throw new IllegalArgumentException("no column " + prefix + " in " + names);
        }

        public boolean hasColumn(String prefix) {
            return names.stream().anyMatch(n -> n.startsWith(prefix));
        }
    }

    public static Table table(String stem) throws IOException {
        String text = Files.readString(HORIZONS.resolve(stem + ".txt"), StandardCharsets.UTF_8);
        int from = text.indexOf("$$SOE");
        int to = text.indexOf("$$EOE");
        if (from < 0 || to < 0) {
            throw new IOException(stem + " is not a complete Horizons response");
        }
        String[] before = text.substring(0, from).strip().split("\n");
        String header = null;
        for (int i = before.length - 1; i >= 0; i--) {
            if (before[i].strip().startsWith("Date__")) {
                header = before[i];
                break;
            }
        }
        if (header == null) {
            throw new IOException(stem + " has no column header");
        }
        List<String> names = new ArrayList<>();
        for (String n : header.split(",")) {
            names.add(n.strip());
        }
        List<String[]> rows = new ArrayList<>();
        for (String line : text.substring(from + 5, to).strip().split("\n")) {
            String[] f = line.split(",");
            for (int i = 0; i < f.length; i++) {
                f[i] = f[i].strip();
            }
            rows.add(f);
        }
        return new Table(names, rows);
    }

    public static List<String> stems(String body) throws IOException {
        List<String> stems = new ArrayList<>();
        try (Stream<Path> files = Files.list(HORIZONS)) {
            for (Path p : files.sorted().toList()) {
                String n = p.getFileName().toString();
                if (n.endsWith(".txt") && n.contains("-" + body + "-") && !n.startsWith("magnitudes-")) {
                    stems.add(n.substring(0, n.length() - ".txt".length()));
                }
            }
        }
        return stems;
    }

    public static String site(String stem) {
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

    public String era(Instant when) {
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
        for (String stem : stems("mars")) {
            Table t = table(stem);
            String site = site(stem);
            for (String[] f : t.rows()) {
                Instant when = instant(f[0]);
                MarsObservation o = site.equals("geocentric") ? service.observeGeocentric(when)
                        : service.observe(observer(site, when));
                mars(result, era(when), stem + " " + when, o, t, f);
            }
        }
        for (String body : List.of("phobos", "deimos")) {
            MarsService.Moon moon = MarsService.Moon.valueOf(body.toUpperCase(Locale.ROOT));
            for (String stem : stems(body)) {
                Table t = table(stem);
                String site = site(stem);
                for (String[] f : t.rows()) {
                    Instant when = instant(f[0]);
                    if (!service.moonsAnswerAt(when)) {
                        continue;
                    }
                    MarsService.Configuration c = site.equals("geocentric")
                            ? service.observeMoonsGeocentric(when)
                            : service.observeMoons(observer(site, when));
                    moon(result, era(when), stem + " " + when, when, body, c, c.moon(moon), t, f);
                }
            }
        }
        events(result);
        return result;
    }

    public static Observer observer(String site, Instant when) {
        double[] place = OBSERVERS.get(site);
        return new Observer(place[1], place[0], when);
    }

    private void mars(Result result, String era, String tag, MarsObservation o, Table t, String[] f) {
        Worst w = result.worst.computeIfAbsent(era, e -> new Worst());
        result.rows.merge(era, 1, Integer::sum);
        result.phases.merge(o.phase(), 1, Integer::sum);
        w.note("Mars astrometric", separation(o.astrometricJ2000(), d(f, t, "R.A.___"), d(f, t, "DEC____")), tag);
        double app = separation(o.apparentOfDate(), d(f, t, "R.A.__(a-app)"), d(f, t, "DEC___(a-app)"));
        if (o.elongationDegrees() > 1.0) {
            w.note("Mars apparent, > 1° from the Sun", app, tag);
        }
        if (o.horizontal() != null) {
            w.note("Mars horizontal", separation(new SkyPosition(o.horizontal().azimuthDegrees(),
                    o.horizontal().altitudeDegrees()), d(f, t, "Azimuth"), d(f, t, "Elevation")), tag);
        }
        w.note("Mars distance km", o.distanceKm() - d(f, t, "delta") * AU_KM, tag);
        w.note("equatorial diameter", o.equatorialDiameterArcseconds() - d(f, t, "Ang-diam"), tag);
        w.note("illuminated points", o.illuminatedFraction() * 100.0 - d(f, t, "Illu%"), tag);
        w.note("phase angle, against phi", (o.phaseAngleDegrees() - d(f, t, "phi")) * ARCSEC, tag);
        w.note("pole position angle", wrap(o.poleAngleDegrees() - d(f, t, "NP.ang")) * ARCSEC, tag);
        double ratio = 3396.19 / 3376.20;
        double detic = Math.toDegrees(Math.atan(Math.tan(Math.toRadians(o.tiltDegrees())) * ratio * ratio));
        w.note("tilt, read planetodetic, against ObsSub-LAT", (detic - d(f, t, "ObsSub-LAT")) * ARCSEC, tag);
    }

    private void moon(Result result, String era, String tag, Instant when, String body,
                      MarsService.Configuration c, MoonPlace p, Table t, String[] f) {
        Worst w = result.worst.computeIfAbsent(era, e -> new Worst());
        result.moonRows.merge(era, 1, Integer::sum);
        boolean far = c.mars().elongationDegrees() > 1.0;
        double dx = p.xArcseconds() - d(f, t, "X_(sat-prim)");
        double dy = p.yArcseconds() - d(f, t, "Y_(sat-prim)");
        double ds = p.separationArcseconds() - d(f, t, "ang-sep");
        double xy = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(ds)));
        double apparent = separation(p.apparentOfDate(), d(f, t, "R.A.__(a-app)"), d(f, t, "DEC___(a-app)"));
        if (far) {
            w.note("moons X, Y, separation, > 1° from the Sun", xy, tag);
        } else {
            w.note("moons apparent, within 1° of the Sun", apparent, tag);
        }
        double pa = wrap(p.positionAngleDegrees() - d(f, t, "SatPANG"));
        w.note(p.separationArcseconds() > 20.0 ? "moons position angle, > 20″ from Mars"
                : "moons position angle, within 20″ of Mars", pa, tag);
        if (p.horizontal() != null) {
            w.note("moons horizontal", separation(new SkyPosition(p.horizontal().azimuthDegrees(),
                    p.horizontal().altitudeDegrees()), d(f, t, "Azimuth"), d(f, t, "Elevation")), tag);
        }
        String horizons = f[t.column("vis.")].replace("/", "").strip();
        if (horizons.isEmpty()) {
            horizons = "*";
        }
        String ours = code(p.sphericalDiscRelation(), p.sphericalShadowRelation());
        result.codes.merge(body + " " + horizons, 1, Integer::sum);
        if (ours.equals(horizons)) {
            result.stateAgreed.merge(body, 1, Integer::sum);
        } else {
            result.stateDisagreements.add(new StateCase(body, tag, when, ours, horizons));
        }
        if (p.discRelation() != p.sphericalDiscRelation() || p.shadowRelation() != p.sphericalShadowRelation()) {
            result.figureDiffers.merge(body, 1, Integer::sum);
        }
    }

    /** Horizons' visibility codes, on its own definition. */
    static String code(MarsService.DiscRelation disc, MarsService.ShadowRelation shadow) {
        if (disc == MarsService.DiscRelation.IN_FRONT) {
            return "t";
        }
        if (disc == MarsService.DiscRelation.BEHIND) {
            return switch (shadow) {
                case SUNLIT -> "O";
                case PARTLY_IN_SHADOW -> "P";
                case IN_SHADOW -> "U";
            };
        }
        return switch (shadow) {
            case SUNLIT -> "*";
            case PARTLY_IN_SHADOW -> "p";
            case IN_SHADOW -> "u";
        };
    }

    // ---- Espenak's named events -------------------------------------------

    private void events(Result result) throws IOException {
        for (String line : Files.readAllLines(EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            boolean opposition = line.contains("Mars at Opposition");
            boolean conjunction = line.contains("Mars in Conjunction with Sun");
            if (!opposition && !conjunction) {
                continue; // perihelia, aphelia and occultations are named rows, not asserted
            }
            String[] f = line.strip().split("\\s+");
            Instant listed = LocalDateTime.parse(String.format(Locale.ROOT, "%s-%s-%s %s:00",
                    f[0], f[1], f[2], f[3]), DateTimeFormatter.ofPattern("uuuu-MMM-dd HH:mm", Locale.ENGLISH))
                    .toInstant(ZoneOffset.UTC);
            double etListed = service.timeScales().tt(listed).secondsPastJ2000();
            double et = zero(etListed - 3 * 86400.0, etListed + 3 * 86400.0, opposition ? 180.0 : 0.0);
            Instant ours = listed.plusMillis(Math.round((et - etListed) * 1000.0));
            result.events.add(new Event(line.strip(), opposition, listed, ours));
        }
    }

    /** Where the geocentric apparent longitude from the Sun passes the target, to a millisecond. */
    private double zero(double from, double to, double target) {
        double step = 3600.0;
        double a = from;
        double fa = wrap(service.geocentricLongitudeFromSunAtEt(a) - target);
        for (double b = from + step; b <= to; b += step) {
            double fb = wrap(service.geocentricLongitudeFromSunAtEt(b) - target);
            if (Math.signum(fa) != Math.signum(fb) && Math.abs(fa - fb) < 90.0) {
                double lo = a;
                double hi = b;
                while (hi - lo > 0.001) {
                    double mid = (lo + hi) / 2.0;
                    double fm = wrap(service.geocentricLongitudeFromSunAtEt(mid) - target);
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
        throw new IllegalStateException("no crossing within three days of the listed hour");
    }

    // ---- helpers -------------------------------------------------------------

    public static double separation(SkyPosition p, double raDeg, double decDeg) {
        return p.separationDegrees(new SkyPosition(raDeg, decDeg)) * ARCSEC;
    }

    public static double wrap(double deg) {
        return ((deg + 540.0) % 360.0) - 180.0;
    }

    public static double d(String[] f, Table t, String column) {
        return Double.parseDouble(f[t.column(column)]);
    }

    // ---- the report -------------------------------------------------------

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
        out.append("\n| Mars rows |");
        for (String e : ERAS) {
            out.append(' ').append(r.rows.getOrDefault(e, 0)).append(" |");
        }
        out.append("\n| moon rows |");
        for (String e : ERAS) {
            out.append(' ').append(r.moonRows.getOrDefault(e, 0)).append(" |");
        }
        out.append('\n');
        for (String q : QUANTITIES) {
            out.append("| ").append(q).append(" |");
            for (int i = 0; i < ERAS.size(); i++) {
                Worst w = r.worst.get(ERAS.get(i));
                double t = TARGETS.get(q)[i];
                if (w == null || !w.has(q)) {
                    out.append(" - |");
                    continue;
                }
                double v = w.of(q);
                boolean miss = !Double.isNaN(t) && v > t;
                out.append(String.format(Locale.ROOT, " %s%s %s%s |", miss ? "**" : "", value(q, v),
                        Double.isNaN(t) ? "(not asserted)" : "(≤ " + value(q, t) + ")", miss ? "**" : ""));
            }
            out.append('\n');
        }
        for (String q : PRINTED) {
            out.append("| ").append(q).append(" |");
            for (String e : ERAS) {
                Worst w = r.worst.get(e);
                out.append(w == null || !w.has(q) ? " - |" : " " + value(q, w.of(q)) + " (printed) |");
            }
            out.append('\n');
        }
        out.append(String.format(Locale.ROOT, "%nA bold cell misses its ruled target. The exact-time"
                + " interval ends %s. Arcseconds unless named; illuminated in percentage points.%n%n",
                r.exactUntil));
        out.append("Where the worst rows are:\n\n");
        for (String e : ERAS) {
            Worst w = r.worst.get(e);
            if (w == null) {
                continue;
            }
            for (String q : QUANTITIES) {
                if (w.has(q)) {
                    out.append("- ").append(e).append(", ").append(q).append(": ")
                            .append(value(q, w.of(q))).append(" at ").append(w.where(q)).append('\n');
                }
            }
            for (String q : PRINTED) {
                if (w.has(q)) {
                    out.append("- ").append(e).append(", ").append(q).append(": ")
                            .append(value(q, w.of(q))).append(" at ").append(w.where(q)).append('\n');
                }
            }
        }
        out.append("\nThe phase words over every Mars row: ");
        StringBuilder words = new StringBuilder();
        for (Map.Entry<MarsService.Phase, Integer> e : r.phases.entrySet()) {
            words.append(words.length() == 0 ? "" : ", ").append(e.getKey()).append(' ').append(e.getValue());
        }
        out.append(words).append(".\n");
        out.append("\n## The moons' states against Horizons' codes\n\n"
                + "On Horizons' definition (#601, M-8): limb to limb on Mars's equatorial sphere and the"
                + " moon's largest radius, the shadow Mars's equatorial-sphere umbra at the moon's"
                + " emission instant. The reader's state uses the oblate figure; *figure differs*"
                + " counts rows where that changes the state. Only rows inside the moons' interval"
                + " are compared.\n\n| moon | agreeing | disagreeing | figure differs | Horizons' codes |\n"
                + "|---|---:|---:|---:|---|\n");
        for (String body : List.of("phobos", "deimos")) {
            long disagree = r.stateDisagreements.stream().filter(s -> s.moon().equals(body)).count();
            StringBuilder codes = new StringBuilder();
            for (Map.Entry<String, Integer> e : r.codes.entrySet()) {
                if (e.getKey().startsWith(body + " ")) {
                    codes.append(codes.length() == 0 ? "" : ", ").append('`')
                            .append(e.getKey().substring(body.length() + 1)).append("` ").append(e.getValue());
                }
            }
            out.append(String.format(Locale.ROOT, "| %s | %d | %d | %d | %s |%n", body,
                    r.stateAgreed.getOrDefault(body, 0), disagree, r.figureDiffers.getOrDefault(body, 0), codes));
        }
        for (StateCase s : r.stateDisagreements) {
            boolean named = NAMED_BOUNDARY_INSTANTS.contains(s.instant().toString());
            out.append("- ").append(s.tag()).append(": ours `").append(s.ours()).append("`, Horizons `")
                    .append(s.horizons()).append('`')
                    .append(named ? " - a named umbra-boundary row (M-8)" : " - NOT a named row").append('\n');
        }
        out.append("\n## Espenak's oppositions and conjunctions against DE440, through the service\n\n"
                + "An opposition is where the geocentric apparent ecliptic longitudes of date differ by"
                + " 180°, a conjunction where they are equal, each found within three days of the"
                + " listed hour. Espenak's rows give the hour (#601, M-12): the service's instant is"
                + " asserted within ±1 hour of it. Perihelia, aphelia and lunar occultations are named"
                + " rows, not asserted.\n\n| published row | DE440 (UT) | from the listed hour |\n|---|---|---|\n");
        for (Event e : r.events) {
            out.append(String.format(Locale.ROOT, "| %s | %s | %+d min |%n", e.row(),
                    e.ours().toString().substring(0, 19).replace('T', ' '), e.minutesFromListed()));
        }
        return out.toString();
    }

    private static String value(String quantity, double v) {
        if (quantity.endsWith("km")) {
            return String.format(Locale.ROOT, "%.3f km", v);
        }
        if (quantity.contains("diameter") || quantity.startsWith("illuminated")) {
            return String.format(Locale.ROOT, "%.5f%s", v, quantity.startsWith("illuminated") ? "" : "″");
        }
        if (quantity.contains("position angle")) {
            return quantity.startsWith("moons") ? String.format(Locale.ROOT, "%.4f°", v)
                    : String.format(Locale.ROOT, "%.3f″", v);
        }
        return String.format(Locale.ROOT, "%.4f″", v);
    }
}
