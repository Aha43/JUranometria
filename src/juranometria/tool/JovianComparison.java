package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import juranometria.sky.SkyFrame;
import juranometria.solar.JovianSystemService;
import juranometria.solar.JovianSystemService.Configuration;
import juranometria.solar.JovianSystemService.DiscRelation;
import juranometria.solar.JovianSystemService.JupiterObservation;
import juranometria.solar.JovianSystemService.Moon;
import juranometria.solar.JovianSystemService.MoonPlace;
import juranometria.solar.JovianSystemService.ShadowRelation;
import juranometria.solar.SolarSystemService.Horizontal;

/**
 * The Jovian service against JPL Horizons, row by row, over the kept
 * fixtures under {@code docs/studies/jovian-system/horizons/} (issue
 * #473): the worst difference of every quantity the contract names,
 * by body and era; the visibility state against Horizons' code on
 * Horizons' own equatorial-sphere definition, with every disagreement
 * named; the reader's oblate-figure state beside it, with every row
 * where the two definitions differ named; and the ingress and egress
 * minutes of the named evening. One engine, two readers: the tests
 * hold its results to the ruled targets, and the report main prints
 * them for the owner.
 */
public final class JovianComparison {

    static final Path HORIZONS = Path.of("docs/studies/jovian-system/horizons");
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

    /** A state disagreement, or a row where the two disc definitions differ. */
    public record Case(String body, String site, Instant when, String ours,
                       String theirs, double separationArcsec,
                       double sphereLimbSumArcsec, double figureNote) {
        public String tag() {
            return site + " " + when;
        }
    }

    /** The first and last minute of a state on a minute series. */
    public record Transition(String body, String site, String definition,
                             Instant first, Instant last) {
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
        public final List<Transition> transitions = new ArrayList<>();
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
    }

    private final JovianSystemService service;
    private final LocalDate exactUntil;
    private final Map<String, Configuration> configurations = new HashMap<>();
    private final Map<String, JupiterObservation> jupiters = new HashMap<>();

    public JovianComparison(JovianSystemService service) {
        this.service = service;
        this.exactUntil = service.timeScales().exactUntil();
    }

    /** Every fixture, every row. */
    public Result run() throws IOException {
        Result result = new Result();
        for (String name : fixtures("jupiter")) {
            compareJupiter(result, name);
        }
        for (Moon moon : Moon.values()) {
            for (String name : fixtures(moon.name().toLowerCase(Locale.ROOT))) {
                compareMoon(result, moon, name);
            }
        }
        for (Moon moon : Moon.values()) {
            for (String site : List.of("oslo", "geocentric")) {
                transitions(result, moon, site);
            }
        }
        return result;
    }

    static List<String> fixtures(String body) throws IOException {
        try (Stream<Path> files = Files.list(HORIZONS)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".txt") && n.contains("-" + body + "-"))
                    .map(n -> n.substring(0, n.length() - 4))
                    .sorted().toList();
        }
    }

    static String siteOf(String name, String body) {
        return name.substring(name.indexOf("-" + body + "-") + body.length() + 2);
    }

    static Instant stamp(String field) {
        String stamp = field.strip();
        if (!stamp.contains(".")) {
            stamp += ".000";
        }
        return LocalDateTime.parse(stamp, STAMP).toInstant(ZoneOffset.UTC);
    }

    static List<String[]> rows(String name) throws IOException {
        String text = Files.readString(HORIZONS.resolve(name + ".txt"), StandardCharsets.UTF_8);
        String table = text.substring(text.indexOf("$$SOE") + 5, text.indexOf("$$EOE")).strip();
        List<String[]> rows = new ArrayList<>();
        for (String line : table.split("\n")) {
            if (!line.isBlank()) {
                rows.add(line.split(","));
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

    private JupiterObservation jupiter(String site, Instant when) {
        String key = site + "|" + when;
        JupiterObservation j = jupiters.get(key);
        if (j == null) {
            j = site.equals("geocentric") ? service.observeJupiterGeocentric(when)
                    : service.observeJupiter(observer(site, when));
            jupiters.put(key, j);
        }
        return j;
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

    static Observer observer(String site, Instant when) {
        double[] o = OBSERVERS.get(site);
        if (o == null) {
            throw new IllegalArgumentException("no observer named " + site);
        }
        return new Observer(o[1], o[0], when);
    }

    private boolean insideTheMoonsInterval(Instant when) {
        LocalDate day = when.atOffset(ZoneOffset.UTC).toLocalDate();
        return !day.isBefore(service.pack().moonsFirstDay())
                && !day.isAfter(service.pack().moonsLastDay());
    }

    /**
     * Jupiter's columns: 0 date, 3 RA, 4 DEC (astrometric ICRF), 5 and 6
     * apparent of date, 7 azimuth, 8 elevation, 9 Illu%, 10 Ang-diam,
     * 11 NP.ang, 12 NP.dist, 13 delta (AU), 15 S-T-O.
     */
    private void compareJupiter(Result result, String name) throws IOException {
        String site = siteOf(name, "jupiter");
        for (String[] f : rows(name)) {
            Instant when = stamp(f[0]);
            JupiterObservation o = jupiter(site, when);
            String era = era(when);
            Worst w = result.worst("jupiter", era);
            result.rows.merge("jupiter|" + era, 1, Integer::sum);
            String at = site + " " + when;
            w.note("astrometric", arcsec(o.astrometricJ2000(), d(f[3]), d(f[4])), at);
            double apparent = arcsec(o.apparentOfDate(), d(f[5]), d(f[6]));
            w.note(o.elongationDegrees() > 1.0 ? "apparent" : "apparent, Sun within 1 degree",
                    apparent, at);
            if (!site.equals("geocentric")) {
                w.note("horizontal", horizontalArcsec(o.horizontal(), d(f[7]), d(f[8])), at);
            }
            w.note("distance km", o.distanceKm() - d(f[13]) * AU_KM, at);
            w.note("equatorial diameter", o.equatorialDiameterArcseconds() - d(f[10]), at);
            w.note("pole angle deg", wrapped(o.poleAngleDegrees() - d(f[11])), at);
            w.note("illuminated points", o.illuminatedFraction() - d(f[9]) / 100.0, at);
            w.note("phase angle deg", o.phaseAngleDegrees() - d(f[15]), at);
        }
    }

    /**
     * A moon's columns: 0 date, 3 RA, 4 DEC, 5 and 6 apparent, 7 azimuth,
     * 8 elevation, 9 X, 10 Y, 11 SatPANG, 12 ang-sep, 13 vis., 14 Ang-diam,
     * 15 delta (AU), 17 S-T-O.
     */
    private void compareMoon(Result result, Moon moon, String name) throws IOException {
        String body = moon.name().toLowerCase(Locale.ROOT);
        String site = siteOf(name, body);
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
            w.note("astrometric", arcsec(p.astrometricJ2000(), d(f[3]), d(f[4])), at);
            double apparent = arcsec(p.apparentOfDate(), d(f[5]), d(f[6]));
            w.note(c.jupiter().elongationDegrees() > 1.0 ? "apparent"
                    : "apparent, Sun within 1 degree", apparent, at);
            if (!site.equals("geocentric")) {
                w.note("horizontal", horizontalArcsec(p.horizontal(), d(f[7]), d(f[8])), at);
            }
            w.note("distance km", p.distanceKm() - d(f[15]) * AU_KM, at);
            w.note("diameter", p.angularDiameterArcseconds() - d(f[14]), at);
            w.note("X", p.xArcseconds() - d(f[9]), at);
            w.note("Y", p.yArcseconds() - d(f[10]), at);
            w.note("separation", p.separationArcseconds() - d(f[12]), at);
            w.note("position angle deg", wrapped(p.positionAngleDegrees() - d(f[11])), at);

            // The state, on Horizons' definition and on the reader's.
            String theirs = f[13].replace("/", "").strip();
            if (theirs.isEmpty()) {
                theirs = "*";
            }
            String ours = code(p.sphericalDiscRelation(), p.shadowRelation());
            double sphereLimbs = Math.toDegrees(Math.asin(
                    service.pack().constants().jupiterEquatorialRadiusKm()
                            / (c.jupiter().distanceKm()))) * ARCSEC
                    + p.angularDiameterArcseconds() / 2.0;
            if (ours.equals(theirs)) {
                result.statesAgreed.merge(body, 1, Integer::sum);
            } else {
                result.statesDisagreed.merge(body, 1, Integer::sum);
                result.disagreements.add(new Case(body, site, when, ours, theirs,
                        p.separationArcseconds(), sphereLimbs, p.depthKm()));
            }
            result.figureRows.merge(body, 1, Integer::sum);
            if (p.discRelation() != p.sphericalDiscRelation()) {
                result.figureDifferences.add(new Case(body, site, when,
                        code(p.discRelation(), p.shadowRelation()), ours,
                        p.separationArcseconds(), sphereLimbs,
                        c.jupiter().subObserverLatitudeDegrees()));
            }
        }
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

    /** First and last minute in front of Jupiter on the named evening's minute series. */
    private void transitions(Result result, Moon moon, String site) throws IOException {
        String body = moon.name().toLowerCase(Locale.ROOT);
        String name = "triple-transit-2026-12-11-minutes-" + body + "-" + site;
        Instant[] horizons = new Instant[2];
        Instant[] sphere = new Instant[2];
        Instant[] figure = new Instant[2];
        for (String[] f : rows(name)) {
            Instant when = stamp(f[0]);
            String theirs = f[13].replace("/", "").strip();
            MoonPlace p = configuration(site, when).moon(moon);
            span(horizons, when, theirs.equals("t"));
            span(sphere, when, p.sphericalDiscRelation() == DiscRelation.IN_FRONT);
            span(figure, when, p.discRelation() == DiscRelation.IN_FRONT);
        }
        result.transitions.add(new Transition(body, site, "horizons", horizons[0], horizons[1]));
        result.transitions.add(new Transition(body, site, "sphere", sphere[0], sphere[1]));
        result.transitions.add(new Transition(body, site, "figure", figure[0], figure[1]));
    }

    private static void span(Instant[] span, Instant when, boolean inFront) {
        if (inFront) {
            if (span[0] == null) {
                span[0] = when;
            }
            span[1] = when;
        }
    }

    /** The result as the report main prints it. */
    public static String report(Result r) {
        StringBuilder md = new StringBuilder();
        md.append("## Worst differences from Horizons, by body and era\n\n");
        for (String body : List.of("jupiter", "io", "europa", "ganymede", "callisto")) {
            md.append("### ").append(body).append("\n\n");
            md.append("| era | rows | quantity | worst | at |\n|---|---:|---|---:|---|\n");
            for (String era : ERAS) {
                Worst w = r.worstOf(body, era);
                int rows = r.rowsOf(body, era);
                if (rows == 0) {
                    continue;
                }
                for (Map.Entry<String, Double> q : w.values().entrySet()) {
                    md.append(String.format(Locale.ROOT, "| %s | %d | %s | %s | %s |\n",
                            era, rows, q.getKey(), number(q.getKey(), q.getValue()),
                            w.where(q.getKey())));
                }
            }
            md.append('\n');
        }
        md.append(String.format(Locale.ROOT, "Moon rows before the moons' interval"
                + " (skipped, Jupiter alone answers there): %d.\n\n", r.outsideTheMoonsInterval));
        md.append("## Visibility states against Horizons' codes (equatorial sphere, limb to limb)\n\n");
        md.append("| body | agree | disagree |\n|---|---:|---:|\n");
        for (String body : List.of("io", "europa", "ganymede", "callisto")) {
            md.append(String.format(Locale.ROOT, "| %s | %d | %d |\n", body,
                    r.statesAgreed.getOrDefault(body, 0), r.statesDisagreed.getOrDefault(body, 0)));
        }
        md.append("\nDisagreements, every one:\n\n");
        if (r.disagreements.isEmpty()) {
            md.append("- none\n");
        }
        for (Case c : r.disagreements) {
            md.append(String.format(Locale.ROOT, "- %s at %s: ours %s, Horizons %s;"
                    + " separation %.2f\" against a limb sum of %.2f\"; depth %+.0f km\n",
                    c.body(), c.tag(), c.ours(), c.theirs(), c.separationArcsec(),
                    c.sphereLimbSumArcsec(), c.figureNote()));
        }
        md.append("\n## The reader's oblate figure against the sphere\n\n");
        md.append(String.format(Locale.ROOT, "Rows where the two disc definitions differ: %d"
                + " of %d, every one named:\n\n", r.figureDifferences.size(),
                r.figureRows.values().stream().mapToInt(Integer::intValue).sum()));
        if (r.figureDifferences.isEmpty()) {
            md.append("- none\n");
        }
        for (Case c : r.figureDifferences) {
            md.append(String.format(Locale.ROOT, "- %s at %s: figure %s, sphere %s;"
                    + " separation %.2f\" against the sphere's limb sum %.2f\";"
                    + " sub-observer latitude %+.2f deg\n", c.body(), c.tag(), c.ours(),
                    c.theirs(), c.separationArcsec(), c.sphereLimbSumArcsec(), c.figureNote()));
        }
        md.append("\n## The named evening, 2026-12-11: first and last minute in front of Jupiter\n\n");
        md.append("| moon | observer | definition | first | last |\n|---|---|---|---|---|\n");
        for (Transition t : r.transitions) {
            md.append(String.format(Locale.ROOT, "| %s | %s | %s | %s | %s |\n", t.body(),
                    t.site(), t.definition(), minute(t.first()), minute(t.last())));
        }
        return md.toString();
    }

    static String minute(Instant when) {
        return when == null ? "-" : when.toString().substring(0, 16).replace('T', ' ');
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
