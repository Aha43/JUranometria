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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.Configuration;
import juranometria.solar.SaturnSystemService.DiscRelation;
import juranometria.solar.SaturnSystemService.Moon;
import juranometria.solar.SaturnSystemService.MoonPlace;
import juranometria.solar.SaturnSystemService.SaturnObservation;
import juranometria.solar.SaturnSystemService.ShadowRelation;
import juranometria.solar.SolarSystemService.Horizontal;

/**
 * The Saturn service against JPL Horizons and the named cases, over the
 * committed fixtures under {@code docs/studies/saturn-system/} (issue
 * #575): the worst difference of every quantity the contract names, by
 * body and era; the ring openings B and B′ against Horizons' planetodetic
 * sub-observer and sub-solar latitudes, compared on Horizons' own
 * planetodetic reading of the service's planetocentric angle (#574's
 * identity); the visibility state against Horizons' codes on Horizons'
 * equatorial-sphere definition, every disagreement named; the reader's
 * flattened figure beside it, every row where the two differ named; the
 * ring-plane crossings in Horizons' rows - validated only against
 * Horizons (#574, ruling 8); Titan's greatest elongations of August 2026;
 * and the event instants of IMCCE's quoted rows (TASS 1.6) as the reader's
 * relations change, recorded, not gated. One engine, two readers: the
 * tests hold its results to the ruled targets, and the report main prints
 * them for the owner.
 */
public final class SaturnComparison {

    static final Path STUDY = Path.of("docs/studies/saturn-system");
    static final Path HORIZONS = STUDY.resolve("horizons");
    static final Path IMCCE_ROWS = STUDY.resolve("imcce-quoted-rows.txt");
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

    static final List<String> ERAS = List.of("1900-1961", "1962-1971", "1972-exact", "after");
    public static final List<String> BODIES = List.of("saturn", "mimas", "enceladus", "tethys", "dione",
            "rhea", "titan", "iapetus");

    private static final DateTimeFormatter STAMP = DateTimeFormatter
            .ofPattern("uuuu-MMM-dd HH:mm:ss.SSS", Locale.ENGLISH);

    /** The largest absolute value seen for a quantity, and where. */
    public static final class Worst {
        private final Map<String, Double> values = new TreeMap<>();
        private final Map<String, String> where = new TreeMap<>();

        void note(String quantity, double value, String at) {
            double magnitude = Math.abs(value);
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

        public Map<String, Double> values() {
            return values;
        }
    }

    /** A state disagreement, or a row where the two figures differ. */
    public record Case(String body, String era, String site, Instant when, String ours,
                       String theirs, double separationArcsec, double limbSumArcsec) {
        public String tag() {
            return site + " " + when;
        }
    }

    /** A sign change of the ring opening in a series: Horizons' instant and ours. */
    public record Crossing(String series, String of, Instant horizons, Instant ours) {
        public double hours() {
            return Duration.between(horizons, ours).toMillis() / 3_600_000.0;
        }
    }

    /** One of IMCCE's quoted events and the reader's matching instant. */
    public record Event(String moon, String kind, Instant imcceCentre, double semiDurationSeconds,
                        Instant imcceContact, Instant ours, String source) {
        public double seconds() {
            return ours == null ? Double.NaN : Duration.between(imcceContact, ours).toMillis() / 1000.0;
        }
    }

    /** A greatest elongation of Titan in the hourly series: Horizons' and ours. */
    public record Elongation(Instant when, double horizonsArcsec, double oursArcsec) {
    }

    /** Everything measured. */
    public static final class Result {
        /** worst by "body|era". */
        public final Map<String, Worst> worst = new TreeMap<>();
        public final Map<String, Integer> rows = new TreeMap<>();
        public final Map<String, Integer> statesAgreed = new TreeMap<>();
        public final Map<String, Integer> statesDisagreed = new TreeMap<>();
        public final List<Case> disagreements = new ArrayList<>();
        public final List<Case> figureDifferences = new ArrayList<>();
        public final Map<String, Integer> figureRows = new TreeMap<>();
        public final Map<String, Integer> positionAnglesIllConditioned = new TreeMap<>();
        public final List<Crossing> crossings = new ArrayList<>();
        public final List<Event> events = new ArrayList<>();
        public final List<Elongation> elongations = new ArrayList<>();
        public int outsideTheMoonsInterval;

        Worst worst(String body, String era) {
            return worst.computeIfAbsent(body + "|" + era, k -> new Worst());
        }

        public Worst worstOf(String body, String era) {
            return worst.getOrDefault(body + "|" + era, new Worst());
        }

        public int rowsOf(String body, String era) {
            return rows.getOrDefault(body + "|" + era, 0);
        }

        public int statesAgreed(String body, String exactness) {
            return statesAgreed.getOrDefault(body + "|" + exactness, 0);
        }

        public int statesDisagreed(String body, String exactness) {
            return statesDisagreed.getOrDefault(body + "|" + exactness, 0);
        }
    }

    private final SaturnSystemService service;
    private final LocalDate exactUntil;
    private final Map<String, Configuration> configurations = new HashMap<>();
    private final Map<String, SaturnObservation> saturns = new HashMap<>();

    public SaturnComparison(SaturnSystemService service) {
        this.service = service;
        this.exactUntil = service.timeScales().exactUntil();
    }

    /** Every committed fixture, every row, and the named cases. */
    public Result run() throws IOException {
        Result result = new Result();
        for (String name : fixtures("saturn")) {
            compareSaturn(result, name);
        }
        for (Moon moon : Moon.values()) {
            for (String name : fixtures(moon.name().toLowerCase(Locale.ROOT))) {
                compareMoon(result, moon, name);
            }
        }
        crossings(result, "ring-plane-window-2024-2025-saturn-oslo");
        crossings(result, "matrix-7d-saturn-oslo");
        elongations(result);
        events(result);
        return result;
    }

    /** The committed responses of a body, the magnitude run excepted (no positions in it). */
    static List<String> fixtures(String body) throws IOException {
        try (Stream<Path> files = Files.list(HORIZONS)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".txt") && n.contains("-" + body + "-")
                            && !n.startsWith("magnitudes-"))
                    .map(n -> n.substring(0, n.length() - 4))
                    .sorted().toList();
        }
    }

    static String siteOf(String name, String body) {
        return name.substring(name.indexOf("-" + body + "-") + body.length() + 2);
    }

    public static Instant stamp(String field) {
        String stamp = field.strip();
        if (!stamp.contains(".")) {
            stamp += ".000";
        }
        return LocalDateTime.parse(stamp, STAMP).toInstant(ZoneOffset.UTC);
    }

    /** The column names of a kept response, as its header line states them. */
    public static List<String> header(String name) throws IOException {
        String text = Files.readString(HORIZONS.resolve(name + ".txt"), StandardCharsets.UTF_8);
        String[] head = text.substring(0, text.indexOf("$$SOE")).strip().split("\n");
        List<String> names = new ArrayList<>();
        for (String c : head[head.length - 2].split(",", -1)) {
            names.add(c.strip());
        }
        return names;
    }

    public static int column(List<String> header, String prefix) {
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i).startsWith(prefix)) {
                return i;
            }
        }
        throw new IllegalStateException("no column " + prefix);
    }

    public static List<String[]> rows(String name) throws IOException {
        String text = Files.readString(HORIZONS.resolve(name + ".txt"), StandardCharsets.UTF_8);
        String table = text.substring(text.indexOf("$$SOE") + 5, text.indexOf("$$EOE")).strip();
        List<String[]> rows = new ArrayList<>();
        for (String line : table.split("\n")) {
            if (!line.isBlank()) {
                rows.add(line.split(",", -1));
            }
        }
        return rows;
    }

    private static double d(String field) {
        return Double.parseDouble(field.strip());
    }

    String era(Instant when) {
        LocalDate day = when.atOffset(ZoneOffset.UTC).toLocalDate();
        if (!day.isBefore(exactUntil)) {
            return "after";
        }
        int year = day.getYear();
        return year < 1962 ? "1900-1961" : year < 1972 ? "1962-1971" : "1972-exact";
    }

    static double arcsec(SkyPosition a, double raDeg, double decDeg) {
        return a.separationDegrees(new SkyPosition(raDeg, decDeg)) * ARCSEC;
    }

    static double horizontalArcsec(Horizontal h, double azDeg, double elDeg) {
        return new SkyPosition(h.azimuthDegrees(), h.altitudeDegrees())
                .separationDegrees(new SkyPosition(azDeg, elDeg)) * ARCSEC;
    }

    static double wrapped(double degrees) {
        double w = SkyFrame.normalise(degrees);
        return w > 180.0 ? w - 360.0 : w;
    }

    /**
     * The planetodetic latitude, degrees, of a planetocentric one on
     * Saturn's spheroid: Horizons' reading of the same direction (#574).
     */
    double planetodetic(double centricDegrees) {
        double ratio = service.pack().constants().saturnEquatorialRadiusKm()
                / service.pack().constants().saturnPolarRadiusKm();
        return Math.toDegrees(Math.atan(Math.tan(Math.toRadians(centricDegrees)) * ratio * ratio));
    }

    private SaturnObservation saturn(String site, Instant when) {
        String key = site + "|" + when;
        SaturnObservation s = saturns.get(key);
        if (s == null) {
            s = site.equals("geocentric") ? service.observeSaturnGeocentric(when)
                    : service.observeSaturn(observer(site, when));
            saturns.put(key, s);
        }
        return s;
    }

    private Configuration configuration(String site, Instant when) {
        String key = site + "|" + when;
        Configuration c = configurations.get(key);
        if (c == null) {
            c = site.equals("geocentric") ? service.observeMoonsGeocentric(when)
                    : service.observeMoons(observer(site, when));
            configurations.put(key, c);
        }
        return c;
    }

    public static Observer observer(String site, Instant when) {
        double[] o = OBSERVERS.get(site);
        if (o == null) {
            throw new IllegalArgumentException("no observer named " + site);
        }
        return new Observer(o[1], o[0], when);
    }

    private boolean insideTheMoonsInterval(Instant when) {
        LocalDate day = when.atOffset(ZoneOffset.UTC).toLocalDate();
        return !day.isBefore(service.pack().moonsFirstDay()) && !day.isAfter(service.pack().moonsLastDay());
    }

    private void compareSaturn(Result result, String name) throws IOException {
        String site = siteOf(name, "saturn");
        List<String> h = header(name);
        int ra = column(h, "R.A.___"), dec = column(h, "DEC____"), raA = column(h, "R.A.__(a-app)"),
                decA = column(h, "DEC___(a-app)"), az = column(h, "Azimuth"), el = column(h, "Elevation"),
                illu = column(h, "Illu%"), diam = column(h, "Ang-diam"), obs = column(h, "ObsSub-LAT"),
                sun = column(h, "SunSub-LAT"), np = column(h, "NP.ang"), delta = column(h, "delta"),
                sto = column(h, "S-T-O");
        for (String[] f : rows(name)) {
            Instant when = stamp(f[0]);
            SaturnObservation o = saturn(site, when);
            String era = era(when);
            Worst w = result.worst("saturn", era);
            result.rows.merge("saturn|" + era, 1, Integer::sum);
            String at = site + " " + when;
            w.note("astrometric", arcsec(o.astrometricJ2000(), d(f[ra]), d(f[dec])), at);
            double apparent = arcsec(o.apparentOfDate(), d(f[raA]), d(f[decA]));
            w.note(o.elongationDegrees() > 1.0 ? "apparent" : "apparent, Sun within 1°", apparent, at);
            if (!site.equals("geocentric")) {
                w.note("horizontal", horizontalArcsec(o.horizontal(), d(f[az]), d(f[el])), at);
            }
            w.note("distance km", o.distanceKm() - d(f[delta]) * AU_KM, at);
            w.note("equatorial diameter", o.equatorialDiameterArcseconds() - d(f[diam]), at);
            w.note("pole angle deg", wrapped(o.poleAngleDegrees() - d(f[np])), at);
            w.note("illuminated points", o.illuminatedFraction() - d(f[illu]) / 100.0, at);
            w.note("phase angle deg", o.phaseAngleDegrees() - d(f[sto]), at);
            w.note("ring opening B", (planetodetic(o.ringOpeningDegrees()) - d(f[obs])) * ARCSEC, at);
            w.note("ring opening to the Sun B'",
                    (planetodetic(o.sunRingOpeningDegrees()) - d(f[sun])) * ARCSEC, at);
        }
    }

    private void compareMoon(Result result, Moon moon, String name) throws IOException {
        String body = moon.name().toLowerCase(Locale.ROOT);
        String site = siteOf(name, body);
        List<String> h = header(name);
        int ra = column(h, "R.A.___"), dec = column(h, "DEC____"), raA = column(h, "R.A.__(a-app)"),
                decA = column(h, "DEC___(a-app)"), az = column(h, "Azimuth"), el = column(h, "Elevation"),
                x = column(h, "X_(sat-prim)"), y = column(h, "Y_(sat-prim)"), pa = column(h, "SatPANG"),
                sep = column(h, "ang-sep"), vis = column(h, "vis."), diam = column(h, "Ang-diam"),
                delta = column(h, "delta");
        for (String[] f : rows(name)) {
            Instant when = stamp(f[0]);
            if (!insideTheMoonsInterval(when)) {
                result.outsideTheMoonsInterval++;
                continue;
            }
            Configuration c = configuration(site, when);
            MoonPlace p = c.moon(moon);
            String era = era(when);
            Worst w = result.worst(body, era);
            result.rows.merge(body + "|" + era, 1, Integer::sum);
            String at = site + " " + when;
            w.note("astrometric", arcsec(p.astrometricJ2000(), d(f[ra]), d(f[dec])), at);
            double apparent = arcsec(p.apparentOfDate(), d(f[raA]), d(f[decA]));
            w.note(c.saturn().elongationDegrees() > 1.0 ? "apparent" : "apparent, Sun within 1°",
                    apparent, at);
            if (!site.equals("geocentric")) {
                w.note("horizontal", horizontalArcsec(p.horizontal(), d(f[az]), d(f[el])), at);
            }
            w.note("distance km", p.distanceKm() - d(f[delta]) * AU_KM, at);
            w.note("diameter", p.angularDiameterArcseconds() - d(f[diam]), at);
            w.note("X", p.xArcseconds() - d(f[x]), at);
            w.note("Y", p.yArcseconds() - d(f[y]), at);
            w.note("separation", p.separationArcseconds() - d(f[sep]), at);
            double paResidual = wrapped(p.positionAngleDegrees() - d(f[pa]));
            w.note("position angle deg", paResidual, at);
            // As Jupiter's: X and Y are authoritative, and where their
            // residual could turn the angle by more than the row's ruled
            // target at the row's separation - 0.1° through the exact
            // interval, 0.2° after it (#575) - the angle is ill-conditioned.
            double displacement = Math.hypot(p.xArcseconds() - d(f[x]), p.yArcseconds() - d(f[y]));
            double cone = Math.toDegrees(Math.atan2(displacement, d(f[sep])));
            if (cone > positionAngleTarget(era)) {
                result.positionAnglesIllConditioned.merge(body + "|" + era, 1, Integer::sum);
                w.note("position angle deg, ill-conditioned rows", paResidual, at);
            } else {
                w.note("position angle deg, well-conditioned rows", paResidual, at);
            }
            String theirs = f[vis].replace("/", "").strip();
            if (theirs.isEmpty()) {
                theirs = "*";
            }
            String ours = code(p.sphericalDiscRelation(), p.sphericalShadowRelation());
            double limbSum = Math.toDegrees(Math.asin(service.pack().constants().saturnEquatorialRadiusKm()
                    / c.saturn().distanceKm())) * ARCSEC + p.angularDiameterArcseconds() / 2.0;
            String exactness = era.equals("after") ? "after" : "exact";
            if (ours.equals(theirs)) {
                result.statesAgreed.merge(body + "|" + exactness, 1, Integer::sum);
            } else {
                result.statesDisagreed.merge(body + "|" + exactness, 1, Integer::sum);
                result.disagreements.add(new Case(body, era, site, when, ours, theirs,
                        p.separationArcseconds(), limbSum));
            }
            result.figureRows.merge(body, 1, Integer::sum);
            String reader = code(p.discRelation(), p.shadowRelation());
            if (!reader.equals(ours)) {
                result.figureDifferences.add(new Case(body, era, site, when, reader, ours,
                        p.separationArcseconds(), limbSum));
            }
        }
    }

    /** The ruled position-angle target for an era: 0.1° (#574), 0.2° after the exact interval (#575). */
    public static double positionAngleTarget(String era) {
        return era.equals("after") ? 0.2 : 0.1;
    }

    /** Horizons' code for a disc and shadow relation. */
    public static String code(DiscRelation disc, ShadowRelation shadow) {
        return switch (disc) {
            case IN_FRONT -> "t";
            case BEHIND -> switch (shadow) {
                case SUNLIT -> "O";
                case PARTLY_IN_SHADOW -> "P";
                case IN_SHADOW -> "U";
            };
            case CLEAR -> switch (shadow) {
                case SUNLIT -> "*";
                case PARTLY_IN_SHADOW -> "p";
                case IN_SHADOW -> "u";
            };
        };
    }

    /**
     * Every sign change of the ring openings in a series, the Earth's (B)
     * and the Sun's (B′), each interpolated linearly between the two rows -
     * Horizons' planetodetic latitudes against the service's planetocentric
     * angles, which change sign at the same instant.
     */
    private void crossings(Result result, String name) throws IOException {
        String site = siteOf(name, "saturn");
        List<String> h = header(name);
        int obs = column(h, "ObsSub-LAT");
        int sun = column(h, "SunSub-LAT");
        Instant previous = null;
        double[] last = null;
        for (String[] f : rows(name)) {
            Instant when = stamp(f[0]);
            SaturnObservation o = saturn(site, when);
            double[] now = {d(f[obs]), o.ringOpeningDegrees(), d(f[sun]), o.sunRingOpeningDegrees()};
            if (last != null) {
                for (int k = 0; k < 2; k++) {
                    Instant theirs = signChange(previous, when, last[2 * k], now[2 * k]);
                    Instant ours = signChange(previous, when, last[2 * k + 1], now[2 * k + 1]);
                    if (theirs != null || ours != null) {
                        result.crossings.add(new Crossing(name, k == 0 ? "the Earth" : "the Sun",
                                theirs, ours));
                    }
                }
            }
            previous = when;
            last = now;
        }
    }

    private static Instant signChange(Instant a, Instant b, double va, double vb) {
        if (va == 0.0 || va * vb < 0.0) {
            double f = va / (va - vb);
            return a.plusMillis(Math.round(Duration.between(a, b).toMillis() * f));
        }
        return null;
    }

    /** Titan's greatest elongations in the hourly August 2026 series. */
    private void elongations(Result result) throws IOException {
        String name = "titan-orbit-2026-08-hourly-oslo";
        List<String> h = header(name);
        int sep = column(h, "ang-sep");
        List<String[]> rows = rows(name);
        for (int k = 1; k + 1 < rows.size(); k++) {
            double before = d(rows.get(k - 1)[sep]);
            double here = d(rows.get(k)[sep]);
            double after = d(rows.get(k + 1)[sep]);
            if (here >= before && here > after) {
                Instant when = stamp(rows.get(k)[0]);
                MoonPlace titan = configuration("oslo", when).moon(Moon.TITAN);
                result.elongations.add(new Elongation(when, here, titan.separationArcseconds()));
            }
        }
    }

    private static final Pattern IMCCE_ROW = Pattern.compile(
            "^(\\S+):(\\d+)\\t\\s*(\\d)\\s+([A-Z]{2})\\.([A-Z])\\s+(\\d{4})\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\.\\s*(\\d+)\\."
                    + "\\s*(\\d+)\\.\\s+(-?[\\d.]+)\\s+(-?[\\d.]+)\\s+(-?\\d+)\\.");

    /**
     * IMCCE's quoted events (TASS 1.6, TT, each for the satellite's
     * centre): the reader's relation changes at a limb contact - the
     * moon's disc first or last touching Saturn's figure or its umbra -
     * which IMCCE's centre instant less its semi-duration names. The
     * service is asked geocentrically on a one-minute TT grid within
     * three hours of that contact and the change interpolated to the
     * second. Recorded, not gated (#574).
     */
    private void events(Result result) throws IOException {
        for (String line : Files.readAllLines(IMCCE_ROWS, StandardCharsets.UTF_8)) {
            if (line.startsWith("#")) {
                continue;
            }
            Matcher m = IMCCE_ROW.matcher(line);
            if (!m.find()) {
                throw new IllegalStateException("an IMCCE row the engine cannot read: " + line);
            }
            Moon moon = moonOf(Integer.parseInt(m.group(3)));
            if (moon == null) {
                continue;
            }
            String kind = m.group(4) + "." + m.group(5);
            LocalDateTime tt = LocalDateTime.of(Integer.parseInt(m.group(6)), Integer.parseInt(m.group(7)),
                    Integer.parseInt(m.group(8)), Integer.parseInt(m.group(9)), Integer.parseInt(m.group(10)))
                    .plusSeconds(Integer.parseInt(m.group(11)));
            Instant centre = tt.toInstant(ZoneOffset.UTC);
            double sd = Double.parseDouble(m.group(14));
            Instant contact = centre.minusMillis(Math.round(sd * 1000.0));
            boolean into = kind.endsWith(".D") || kind.endsWith(".I");
            Instant ours = contactNear(moon, m.group(4), into, contact);
            result.events.add(new Event(moon.name().toLowerCase(Locale.ROOT), kind, centre, sd, contact,
                    ours, m.group(1) + ":" + m.group(2)));
        }
    }

    private static Moon moonOf(int imcceNumber) {
        for (Moon moon : Moon.values()) {
            if (moon.id() == 600 + imcceNumber) {
                return moon;
            }
        }
        return null;
    }

    /** The reader's change nearest a TT instant, for an IMCCE kind: EC, OC or TR. */
    private Instant contactNear(Moon moon, String kind, boolean into, Instant ttContact) {
        double centreEt = Duration.between(Instant.parse("2000-01-01T12:00:00Z"), ttContact).toMillis() / 1000.0;
        Instant best = null;
        double bestOffset = Double.MAX_VALUE;
        boolean before = false;
        double step = 60.0;
        for (int i = -180; i <= 180; i++) {
            double et = centreEt + i * step;
            boolean now = inside(moon, kind, et);
            if (i > -180 && now != before && now == into) {
                // refine to the second inside this minute
                double lo = et - step;
                double hi = et;
                for (int r = 0; r < 20; r++) {
                    double mid = (lo + hi) / 2.0;
                    if (inside(moon, kind, mid) == into) {
                        hi = mid;
                    } else {
                        lo = mid;
                    }
                }
                double offset = Math.abs(hi - centreEt);
                if (offset < bestOffset) {
                    bestOffset = offset;
                    best = Instant.parse("2000-01-01T12:00:00Z").plusMillis(Math.round(hi * 1000.0));
                }
            }
            before = now;
        }
        return best;
    }

    /** Whether the reader's relation for an IMCCE kind holds at an ephemeris time. */
    private boolean inside(Moon moon, String kind, double et) {
        MoonPlace p = service.configurationAtEt(et).moon(moon);
        return switch (kind) {
            case "EC" -> p.shadowRelation() != ShadowRelation.SUNLIT;
            case "OC" -> p.discRelation() == DiscRelation.BEHIND;
            case "TR" -> p.discRelation() == DiscRelation.IN_FRONT;
            default -> throw new IllegalStateException("IMCCE kind " + kind);
        };
    }

    /** The result as the report main prints it. */
    public static String report(Result r) {
        StringBuilder md = new StringBuilder();
        md.append("## Worst differences from Horizons, by body and era\n\n")
                .append("B and B′ are compared on Horizons' planetodetic reading of the service's")
                .append(" planetocentric angles (the identity #574 measured); the reader is given the")
                .append(" planetocentric angle, never the planetodetic one.\n\n");
        for (String body : BODIES) {
            md.append("### ").append(body).append("\n\n");
            md.append("| era | rows | quantity | worst | at |\n|---|---:|---|---:|---|\n");
            for (String era : ERAS) {
                Worst w = r.worstOf(body, era);
                int rows = r.rowsOf(body, era);
                if (rows == 0) {
                    continue;
                }
                for (Map.Entry<String, Double> q : w.values().entrySet()) {
                    md.append(String.format(Locale.ROOT, "| %s | %d | %s | %s | %s |\n", era, rows,
                            q.getKey(), number(q.getKey(), q.getValue()), w.where(q.getKey())));
                }
            }
            md.append('\n');
        }
        md.append(String.format(Locale.ROOT, "Moon rows before the moons' interval (skipped, Saturn alone"
                + " answers there): %d.\n\n", r.outsideTheMoonsInterval));
        md.append("## Position angle: conditioning\n\n")
                .append("As Jupiter's: X and Y are authoritative; a row is ill-conditioned where their")
                .append(" residual could turn the angle by more than the row's ruled target at its")
                .append(" separation - 0.1 degrees through the exact interval, 0.2 degrees after it.\n\n")
                .append("| body | era | rows | ill-conditioned |\n|---|---|---:|---:|\n");
        for (String body : BODIES.subList(1, BODIES.size())) {
            for (String era : ERAS) {
                if (r.rowsOf(body, era) == 0) {
                    continue;
                }
                md.append(String.format(Locale.ROOT, "| %s | %s | %d | %d |\n", body, era, r.rowsOf(body, era),
                        r.positionAnglesIllConditioned.getOrDefault(body + "|" + era, 0)));
            }
        }
        md.append("\n## Visibility states against Horizons' codes (equatorial sphere, limb to limb)\n\n")
                .append("| body | exact: agree | exact: disagree | after: agree | after: disagree |\n")
                .append("|---|---:|---:|---:|---:|\n");
        for (String body : BODIES.subList(1, BODIES.size())) {
            md.append(String.format(Locale.ROOT, "| %s | %d | %d | %d | %d |\n", body,
                    r.statesAgreed(body, "exact"), r.statesDisagreed(body, "exact"),
                    r.statesAgreed(body, "after"), r.statesDisagreed(body, "after")));
        }
        md.append("\nDisagreements, every one:\n\n");
        if (r.disagreements.isEmpty()) {
            md.append("- none\n");
        }
        for (Case c : r.disagreements) {
            md.append(String.format(Locale.ROOT, "- %s (%s) at %s: ours %s, Horizons %s; separation %.4f\""
                    + " against a limb sum of %.4f\"\n", c.body(), c.era(), c.tag(), c.ours(), c.theirs(),
                    c.separationArcsec(), c.limbSumArcsec()));
        }
        md.append("\n## The reader's flattened figure against the sphere\n\n");
        md.append(String.format(Locale.ROOT, "Rows where the reader's state (oblate disc and umbra) and the"
                + " sphere's differ: %d of %d, every one named:\n\n", r.figureDifferences.size(),
                r.figureRows.values().stream().mapToInt(Integer::intValue).sum()));
        if (r.figureDifferences.isEmpty()) {
            md.append("- none\n");
        }
        for (Case c : r.figureDifferences) {
            md.append(String.format(Locale.ROOT, "- %s (%s) at %s: figure %s, sphere %s; separation %.4f\""
                    + " against the sphere's limb sum %.4f\"\n", c.body(), c.era(), c.tag(), c.ours(),
                    c.theirs(), c.separationArcsec(), c.limbSumArcsec()));
        }
        md.append("\n## Ring-plane crossings - validated only against Horizons\n\n")
                .append("Each sign change of the openings between two rows, interpolated linearly in each")
                .append(" series alike. Horizons answers from the same SAT441 family and DE441; no")
                .append(" independent published authority checks these instants (#574, ruling 8).\n\n")
                .append("| series | crossing of | Horizons | ours | hours |\n|---|---|---|---|---:|\n");
        for (Crossing c : r.crossings) {
            md.append(String.format(Locale.ROOT, "| %s | %s | %s | %s | %s |\n", c.series(), c.of(),
                    minute(c.horizons()), minute(c.ours()),
                    c.horizons() == null || c.ours() == null ? "-"
                            : String.format(Locale.ROOT, "%+.3f", c.hours())));
        }
        md.append("\n## Titan's greatest elongations, hourly at Oslo, August 2026\n\n")
                .append("| instant | Horizons | ours | difference |\n|---|---:|---:|---:|\n");
        for (Elongation e : r.elongations) {
            md.append(String.format(Locale.ROOT, "| %s | %.2f\" | %.3f\" | %+.3f\" |\n", minute(e.when()),
                    e.horizonsArcsec(), e.oursArcsec(), e.oursArcsec() - e.horizonsArcsec()));
        }
        md.append("\n## IMCCE's quoted events (TASS 1.6, TT) - recorded, not gated\n\n")
                .append("IMCCE's instant is the satellite centre's; the reader's relation changes at a limb")
                .append(" contact, IMCCE's instant less its semi-duration. Ours is the service's change, on the")
                .append(" reader's flattened figure, geocentric, nearest that contact within three hours.")
                .append(" IMCCE's README gives the semi-duration as half the time between first and last")
                .append(" contact \"with the umbra cone or the visibility cone\" of its own model, so these")
                .append(" differences mix the two theories' timing with the two definitions of contact; the")
                .append(" like-for-like comparison, satellite centre against satellite centre over all")
                .append(" 21,572 events, is the #574 study's `imcce-comparison.md`.\n\n")
                .append("| moon | event | IMCCE centre (TT) | semi-duration | IMCCE contact (TT) | ours (TT) | seconds | row |\n")
                .append("|---|---|---|---:|---|---|---:|---|\n");
        for (Event e : r.events) {
            md.append(String.format(Locale.ROOT, "| %s | %s | %s | %.0f s | %s | %s | %s | %s |\n", e.moon(),
                    e.kind(), second(e.imcceCentre()), e.semiDurationSeconds(), second(e.imcceContact()),
                    e.ours() == null ? "none found" : second(e.ours()),
                    e.ours() == null ? "-" : String.format(Locale.ROOT, "%+.0f", e.seconds()), e.source()));
        }
        return md.toString();
    }

    static String minute(Instant when) {
        return when == null ? "-" : when.toString().substring(0, 16).replace('T', ' ');
    }

    static String second(Instant when) {
        return when == null ? "-" : when.toString().substring(0, 19).replace('T', ' ');
    }

    private static String number(String quantity, double value) {
        if (quantity.contains("km")) {
            return String.format(Locale.ROOT, "%.2f km", value);
        }
        if (quantity.contains("deg")) {
            return String.format(Locale.ROOT, "%.4f deg", value);
        }
        if (quantity.contains("points")) {
            return String.format(Locale.ROOT, "%.5f", value);
        }
        return String.format(Locale.ROOT, "%.4f\"", value);
    }

}
