package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import juranometria.sky.Observer;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.Configuration;
import juranometria.solar.SaturnSystemService.MoonPlace;
import juranometria.solar.SaturnSystemService.SaturnObservation;
import juranometria.solar.SaturnSystemService.VisibilityState;
import juranometria.solar.TimeRange;
import juranometria.solar.time.TimeScales;
import juranometria.ui.solar.MoonTableFormat;
import juranometria.ui.solar.SunTableFormat;

/**
 * The first Saturn table, as numbers on a page (Sprint 52, issue #575):
 * Saturn, its rings and its seven ruled moons for the established
 * observers and cases, from the bundled packs alone, before any
 * application surface exists - in the card's and the moons' columns
 * the owner ruled on #574, with Jupiter's units and rounding.
 *
 * <p>Deterministic: the instants come from the cited fixtures - the
 * named instants in {@code docs/studies/saturn-system/horizons/NAMED-CASES.txt},
 * the card's instant (the 2026 daily Oslo row nearest the Earth) and
 * Titan's greatest elongations in the hourly August series, both read
 * from the kept Horizons responses - never the clock, and the formatting
 * is {@code Locale.ROOT}. The words are the ones ruled on #574 in
 * English, held here until the reader's table (#576) carries them in
 * the language files. The evidence contract regenerates this report and
 * holds it to its committed bytes. Written to stdout by
 * {@code make saturn-table-study}.
 */
public final class SaturnTableStudyMain {

    private record Site(String name, double latitude, double eastLongitude) {
    }

    /** The established observers, the date-line one included. */
    private static final List<Site> SITES = List.of(
            new Site("Oslo (59.91° N, 10.75° E)", 59.91, 10.75),
            new Site("Quito (0.18° S, 78.50° W)", -0.18, -78.5),
            new Site("Cape Town (33.93° S, 18.42° E)", -33.93, 18.42),
            new Site("Alert (82.50° N, 62.34° W)", 82.50, -62.34),
            new Site("Chatham Islands (43.95° S, 176.50° W)", -43.95, -176.5));

    private record Case(String label, Instant instant) {
    }

    /** The states ruled on #574 (ruling 5, Jupiter's six with Saturn's name), English. */
    private static final Map<VisibilityState, String> STATE = new EnumMap<>(Map.of(
            VisibilityState.CLEAR_OF_SATURN, "clear of Saturn",
            VisibilityState.IN_FRONT_OF_SATURN, "in front of Saturn",
            VisibilityState.BEHIND_SATURN, "behind Saturn",
            VisibilityState.IN_SATURNS_SHADOW, "in Saturn's shadow",
            VisibilityState.PARTLY_IN_SATURNS_SHADOW, "partly in Saturn's shadow",
            VisibilityState.BEHIND_SATURN_IN_ITS_SHADOW, "behind Saturn, in its shadow"));

    private static final DateTimeFormatter NAMED = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss",
            Locale.ROOT);

    private SaturnTableStudyMain() {
    }

    /** The named instants, the card's instant and Titan's elongations, read from the fixtures. */
    static List<Case> cases() throws IOException {
        List<Case> cases = new ArrayList<>();
        for (String line : Files.readAllLines(SaturnComparison.HORIZONS.resolve("NAMED-CASES.txt"),
                StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String stamp = line.substring(0, 19);
            String label = line.substring(19).strip().replace('-', ' ');
            cases.add(new Case("Named: " + label,
                    LocalDateTime.parse(stamp, NAMED).toInstant(ZoneOffset.UTC)));
        }
        cases.add(new Case("The card's instant: the 2026 daily row nearest the Earth", cardInstant()));
        for (Instant elongation : titanElongations()) {
            cases.add(new Case("Titan at greatest elongation (Horizons' hourly row)", elongation));
        }
        cases.sort(java.util.Comparator.comparing(Case::instant));
        return cases;
    }

    /** The daily 2026 Oslo row at the smallest distance in Horizons' own column. */
    public static Instant cardInstant() throws IOException {
        String name = "dense-2026-saturn-oslo";
        int delta = SaturnComparison.column(SaturnComparison.header(name), "delta");
        Instant best = null;
        double nearest = Double.MAX_VALUE;
        for (String[] f : SaturnComparison.rows(name)) {
            double d = Double.parseDouble(f[delta].strip());
            if (d < nearest) {
                nearest = d;
                best = SaturnComparison.stamp(f[0]);
            }
        }
        return best;
    }

    /** The local maxima of Horizons' separation in the hourly Titan series. */
    static List<Instant> titanElongations() throws IOException {
        String name = "titan-orbit-2026-08-hourly-oslo";
        int sep = SaturnComparison.column(SaturnComparison.header(name), "ang-sep");
        List<String[]> rows = SaturnComparison.rows(name);
        List<Instant> out = new ArrayList<>();
        for (int k = 1; k + 1 < rows.size(); k++) {
            double before = Double.parseDouble(rows.get(k - 1)[sep].strip());
            double here = Double.parseDouble(rows.get(k)[sep].strip());
            double after = Double.parseDouble(rows.get(k + 1)[sep].strip());
            if (here >= before && here > after) {
                out.add(SaturnComparison.stamp(rows.get(k)[0]));
            }
        }
        return out;
    }

    public static void main(String[] args) throws IOException {
        SaturnSystemService service = SaturnSystemService.load();
        TimeScales scales = service.timeScales();
        List<Case> cases = cases();
        System.out.println("# Saturn, computed: the first table");
        System.out.println();
        System.out.println("Sprint 52, issue #575. Every row below comes from the bundled Saturn pack's"
                + " SAT441 excerpts and the Solar System pack's Sun, Earth and pinned IERS leap-second"
                + " file, offline, by `juranometria.solar.SaturnSystemService`, under the contract ruled"
                + " on #574. Chart position is topocentric astrometric ICRS/J2000; altitude and azimuth"
                + " are apparent and airless, azimuth from north through east; distance is observer to"
                + " the light-time-corrected centre of Saturn (not the barycentre); the apparent"
                + " diameter is across the equator and from pole to pole of the projected figure at the"
                + " ring opening; the north pole's position angle is of date, from celestial north"
                + " through east. The rings' opening is the true geometric angle - the observer's"
                + " planetocentric latitude over Saturn's pole - with the face seen; the opening to the"
                + " Sun is the Sun's, as Saturn sees it, and the unlit face is named when the two are on"
                + " opposite sides of the ring plane; the rings' size is the A Ring's outer edge across"
                + " the major and the minor axis. A moon's offsets are Horizons' apparent differential"
                + " coordinates, e and w for east and west, n and s for north and south; its state is"
                + " from its disc against Saturn's flattened figure and its place against the same"
                + " figure's shadow, in Jupiter's precedence. The words are the ones ruled on #574, held"
                + " here in English until the reader's table carries them.");
        System.out.println();
        System.out.println("Time is exact from " + scales.exactFrom() + " until " + scales.exactUntil()
                + " (the pinned record's own validity); earlier and later instants use an estimated"
                + " clock correction and are marked *est.* in the Instant column. Saturn answers from "
                + service.pack().saturnFirstDay() + " to " + service.pack().saturnLastDay() + "; its moons"
                + " from " + service.pack().moonsFirstDay() + " to " + service.pack().moonsLastDay() + ".");
        System.out.println();

        for (Site site : SITES) {
            System.out.println("## Saturn - " + site.name());
            System.out.println();
            saturnHeader();
            for (Case c : cases) {
                saturnRow(c.label(), service.observeSaturn(new Observer(site.latitude(),
                        site.eastLongitude(), c.instant())), false);
            }
            System.out.println();
        }

        Site oslo = SITES.get(0);
        System.out.println("## The seven moons - " + oslo.name());
        System.out.println();
        for (Case c : cases) {
            System.out.println("### " + c.label() + ", " + SunTableFormat.minute(c.instant()) + " UTC");
            System.out.println();
            if (c.instant().isBefore(service.pack().moonsFirstDay().atStartOfDay(ZoneOffset.UTC).toInstant())) {
                System.out.println("The moons are computed for civil dates from " + service.pack().moonsFirstDay()
                        + " to " + service.pack().moonsLastDay() + "; this instant is outside that, so only"
                        + " Saturn is shown above.");
                System.out.println();
                continue;
            }
            moons(service.observeMoons(new Observer(oslo.latitude(), oslo.eastLongitude(), c.instant())));
        }

        Instant card = cardInstant();
        Instant start = card.minus(Duration.ofHours(6));
        Instant end = card.plus(Duration.ofHours(6)).plus(Duration.ofMinutes(30));
        System.out.println("## Oslo, every 2 hours around the card's instant: " + SunTableFormat.minute(start)
                + " to " + SunTableFormat.minute(end) + " UTC");
        System.out.println();
        System.out.println("A range samples; it does not search for the moment a moon enters or leaves"
                + " Saturn's disc or shadow. The end is not on the 2-hour grid, so it is appended and"
                + " marked †.");
        System.out.println();
        for (TimeRange.Sample sample : new TimeRange(start, end, Duration.ofHours(2)).samples()) {
            Configuration c = service.observeMoons(new Observer(oslo.latitude(), oslo.eastLongitude(),
                    sample.instant()));
            System.out.println("### " + SunTableFormat.minute(sample.instant()) + (sample.appendedEnd() ? " †" : "")
                    + " UTC");
            System.out.println();
            moons(c);
        }
        System.out.println("Dates before " + scales.exactFrom() + " and after " + scales.exactUntil()
                + " use an estimated clock correction. After 2026, uncertainty in local horizon"
                + " coordinates cannot yet be stated precisely; there the moons' states are the"
                + " geometry at an estimated instant, not predicted times of events.");
    }

    private static void saturnHeader() {
        System.out.println("| Case | Instant (UTC) | Right ascension (J2000) | Declination (J2000)"
                + " | Altitude (no refraction) | Azimuth | Distance | Diameter, equator / poles"
                + " | Illuminated | North pole | Rings, opening to the observer | Rings, opening to the Sun"
                + " | Rings, apparent size |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|---|---|");
    }

    private static String opening(double degrees) {
        if (Math.abs(degrees) < 0.005) {
            return String.format(Locale.ROOT, "%.2f° (edge-on)", Math.abs(degrees));
        }
        return String.format(Locale.ROOT, "%.2f° (%s face)", Math.abs(degrees), degrees > 0 ? "north" : "south");
    }

    private static void saturnRow(String label, SaturnObservation o, boolean appended) {
        String instant = SunTableFormat.minute(o.instant()) + (appended ? " †" : "")
                + (o.timeConfidence() == TimeScales.Confidence.EXACT ? "" : " *est.*");
        double outer = o.ringRadiiKm().get(3);
        System.out.println("| " + label + " | " + instant + " | "
                + SunTableFormat.hms(o.astrometricJ2000()) + " | " + SunTableFormat.dms(o.astrometricJ2000())
                + " | " + SunTableFormat.altitude(o.horizontal().altitudeDegrees(), "(below the horizon)")
                + " | " + SunTableFormat.degrees(o.horizontal().azimuthDegrees())
                + " | " + SunTableFormat.astronomicalUnits(o.distanceAu()) + " AU"
                + " | " + String.format(Locale.ROOT, "%.1f″ / %.1f″", o.equatorialDiameterArcseconds(),
                        o.polarDiameterArcseconds())
                + " | " + MoonTableFormat.percent(o.illuminatedFraction())
                + " | " + String.format(Locale.ROOT, "%.1f°", o.poleAngleDegrees())
                + " | " + opening(o.ringOpeningDegrees())
                + " | " + opening(o.sunRingOpeningDegrees()) + (o.unlitFaceSeen() ? "; the unlit face is seen" : "")
                + " | " + String.format(Locale.ROOT, "%.1f″ × %.1f″", 2.0 * o.ringSemiMajorArcseconds(outer),
                        2.0 * o.ringSemiMinorArcseconds(outer))
                + " |");
    }

    private static void moons(Configuration c) {
        System.out.println("| Moon | Side | East–west | North–south | Separation | Saturn radii | State |");
        System.out.println("|---|---|---|---|---|---|---|");
        for (MoonPlace p : c.moons()) {
            String name = p.moon().name().charAt(0) + p.moon().name().substring(1).toLowerCase(Locale.ROOT);
            System.out.println(String.format(Locale.ROOT, "| %s | %s | %.1f″ %s | %.1f″ %s | %.1f″ | %.2f | %s |",
                    name, p.xArcseconds() >= 0 ? "east" : "west", Math.abs(p.xArcseconds()),
                    p.xArcseconds() >= 0 ? "e" : "w", Math.abs(p.yArcseconds()), p.yArcseconds() >= 0 ? "n" : "s",
                    p.separationArcseconds(), p.separationSaturnRadii(), STATE.get(p.state())));
        }
        System.out.println();
    }
}
