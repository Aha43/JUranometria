package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import juranometria.sky.Observer;
import juranometria.solar.MercuryService;
import juranometria.solar.MercuryService.LimbConditioning;
import juranometria.solar.MercuryService.MercuryObservation;
import juranometria.solar.MercuryService.Phase;
import juranometria.solar.SolarSystemService.Side;
import juranometria.solar.TimeRange;
import juranometria.solar.time.TimeScales;
import juranometria.ui.solar.MoonTableFormat;
import juranometria.ui.solar.SunTableFormat;

/**
 * The first Mercury table, as numbers on a page (Sprint 49, issue
 * #529): representative rows for the owner to read before any
 * application surface exists, from the bundled packs alone, in the
 * Moon table's columns, units and rounding - the family the contract
 * ruled Mercury into on #528, as Venus before it.
 *
 * <p>Deterministic: the named instants come from the cited fixtures -
 * Espenak's almanac rows, the two lunar occultations and the NASA/GSFC
 * transit rows in {@code docs/studies/mercury/mercury-events.txt}, and
 * the representative appearances' rows in
 * {@code docs/studies/mercury/horizons/appearances-oslo-199.txt} -
 * "today" is a chosen day, never the clock, and the formatting is
 * {@code Locale.ROOT}. The phase words are the ones ruled on #528 in
 * English, held here until the reader's table (#530) carries them in
 * the language files. The evidence contract regenerates this report
 * and holds it to its committed bytes. Written to stdout by
 * {@code make mercury-table-study}.
 */
public final class MercuryTableStudyMain {

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

    /** The phase words ruled on #528 (ruling 5), English. */
    private static final Map<Phase, String> PHASE = new EnumMap<>(Map.of(
            Phase.IN_FRONT_OF_SUN, "in front of the Sun",
            Phase.BEHIND_SUN, "behind the Sun",
            Phase.NEAR_INFERIOR_CONJUNCTION, "near inferior conjunction",
            Phase.CRESCENT, "crescent",
            Phase.NEAR_HALF_LIT, "near half lit",
            Phase.GIBBOUS, "gibbous",
            Phase.NEAR_FULL, "near full"));

    private MercuryTableStudyMain() {
    }

    /** Espenak's events, the transits and the representative appearances, read from the fixtures. */
    private static List<Case> cases() throws IOException {
        List<Case> cases = new ArrayList<>();
        for (String line : Files.readAllLines(MercuryComparison.EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            Instant instant = Instant.parse(f[1].replace("Z", ":00Z"));
            String label;
            if (f[0].contains("transit")) {
                label = "Greatest transit (NASA/GSFC, Espenak, to the minute)";
            } else if (f[0].contains("occultation")) {
                label = f[2].substring(13).strip() + " (Espenak, to the minute)";
            } else {
                label = f[2].substring(10).strip() + " (Espenak, to the hour)";
            }
            cases.add(new Case(label, instant));
        }
        for (String[] f : MercuryComparison.rows("appearances-oslo-199")) {
            cases.add(new Case("A representative appearance (Horizons' weekly row)",
                    MercuryComparison.instant(f[0])));
        }
        cases.add(new Case("A chosen \"today\"", Instant.parse("2026-10-14T12:00:00Z")));
        cases.add(new Case("First civil instant", Instant.parse("1900-01-01T00:00:00Z")));
        cases.add(new Case("Last civil instant", Instant.parse("2100-12-31T23:59:59Z")));
        cases.sort(java.util.Comparator.comparing(Case::instant));
        return cases;
    }

    public static void main(String[] args) throws IOException {
        MercuryService service = MercuryService.load();
        TimeScales scales = service.timeScales();
        List<Case> cases = cases();
        System.out.println("# Mercury, computed: the first table");
        System.out.println();
        System.out.println("Sprint 49, issue #529. Every row below comes from the bundled planets"
                + " pack's DE440 excerpt for Mercury and the Solar System pack's Sun, Earth and"
                + " pinned IERS leap-second file, offline, by `juranometria.solar.MercuryService`,"
                + " under the contract ruled on #528. Chart position is topocentric astrometric"
                + " ICRS/J2000; altitude and azimuth are apparent and airless, azimuth from north"
                + " through east; distance is observer to the light-time-corrected centre, in AU"
                + " and million km; the apparent diameter is the solid body's, from the PCK's"
                + " equatorial radius of 2 440.53 km (the figure is triaxial, 2 440.53 × 2 440.53"
                + " × 2 438.26 km, and the mean radius is never substituted); the illuminated"
                + " fraction follows the Moon's S-T-O phase angle at Mercury; the phase word is in"
                + " front of or behind the Sun when Mercury's centre is within the Sun's apparent"
                + " radius plus its own, and otherwise the Moon's bands of the fraction; the"
                + " elongation is the unsigned angle from the apparent Sun in this observer's sky,"
                + " with E when Mercury is east of the Sun there in right ascension (the evening"
                + " sky) and W when west - it names no conjunction; the lit side is the position"
                + " angle of the bright limb's midpoint from celestial north through east, of date,"
                + " with the nearest of sixteen compass points, and is not well-defined near"
                + " inferior conjunction and near full. The phase words are the ones ruled on #528,"
                + " held here in English until the reader's table carries them.");
        System.out.println();
        System.out.println("Time is exact from " + scales.exactFrom() + " until "
                + scales.exactUntil() + " (the pinned record's own validity); earlier and later"
                + " instants use an estimated clock correction and are marked *est.* in the"
                + " Instant column. Espenak's almanac events are his published hours and the two"
                + " lunar occultations his published minutes, the transits the NASA/GSFC"
                + " catalogue's greatest-transit minutes, all read from"
                + " `docs/studies/mercury/mercury-events.txt`; the representative appearances are"
                + " Horizons' weekly Oslo rows nearest a quarter or three quarters lit, read from"
                + " `docs/studies/mercury/horizons/appearances-oslo-199.txt`.");
        System.out.println();

        for (Site site : SITES) {
            System.out.println("## " + site.name());
            System.out.println();
            header();
            for (Case c : cases) {
                row(c.label(), service.observe(new Observer(site.latitude(),
                        site.eastLongitude(), c.instant())), false);
            }
            System.out.println();
        }

        Observer oslo = new Observer(59.91, 10.75, Instant.parse("2026-01-25T17:00:00Z"));
        System.out.println("## Oslo, every 3 days through the 2026 evening and morning apparitions:"
                + " 2026-01-25 17:00 to 2026-04-25 17:00 UTC");
        System.out.println();
        System.out.println("The grid of the committed `apparition-2026-oslo-199.txt`. The disc"
                + " grows as the lit part shrinks towards the 2026-03-07 inferior conjunction,"
                + " where the side of the Sun changes, and the sequence runs back on the morning"
                + " side; Mercury's whole apparition takes the weeks Venus's takes months.");
        System.out.println();
        header();
        for (MercuryService.Row r : service.observe(oslo, new TimeRange(oslo.instant(),
                Instant.parse("2026-04-25T17:00:00Z"), Duration.ofDays(3)))) {
            row("", r.observation(), r.sample().appendedEnd());
        }
        System.out.println();
        System.out.println("## Oslo, hourly through the 2019 transit:"
                + " 2019-11-11 12:00 to 2019-11-11 19:00 UTC");
        System.out.println();
        System.out.println("The catalogue's contacts are 12:35 and 18:04 UT, greatest transit"
                + " 15:20; from Oslo the Sun sets during the transit.");
        System.out.println();
        header();
        for (MercuryService.Row r : service.observe(oslo, new TimeRange(
                Instant.parse("2019-11-11T12:00:00Z"), Instant.parse("2019-11-11T19:00:00Z"),
                Duration.ofHours(1)))) {
            row("", r.observation(), r.sample().appendedEnd());
        }
        System.out.println();
        System.out.println("Dates before " + scales.exactFrom() + " and after "
                + scales.exactUntil() + " use an estimated clock correction."
                + " After 2026, uncertainty in local horizon coordinates cannot"
                + " yet be stated precisely.");
    }

    private static void header() {
        System.out.println("| Case | Instant (UTC) | Right ascension (J2000) | Declination (J2000)"
                + " | Altitude (no refraction) | Azimuth (from north through east)"
                + " | Distance | Apparent diameter (solid body) | Illuminated | Phase"
                + " | Elongation from the Sun | Lit side |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|---|");
    }

    private static void row(String label, MercuryObservation o, boolean appended) {
        String instant = SunTableFormat.minute(o.instant())
                + (appended ? " †" : "")
                + (o.timeConfidence() == TimeScales.Confidence.EXACT ? "" : " *est.*");
        String litSide = o.brightLimbConditioning() == LimbConditioning.WELL_DEFINED
                ? MoonTableFormat.positionAngle(o.brightLimbAngleDegrees()) + " ("
                        + MoonTableFormat.letters(o.brightLimbCompassPoint()) + ")"
                : "not well-defined (near inferior conjunction or full)";
        System.out.println("| " + label + " | " + instant + " | "
                + SunTableFormat.hms(o.astrometricJ2000()) + " | "
                + SunTableFormat.dms(o.astrometricJ2000())
                + " | " + SunTableFormat.altitude(o.horizontal().altitudeDegrees(),
                        "(below the horizon)")
                + " | " + SunTableFormat.degrees(o.horizontal().azimuthDegrees())
                + " | " + SunTableFormat.astronomicalUnits(o.distanceAu()) + " AU, "
                + SunTableFormat.millionKilometres(o.distanceKm()) + " million km"
                + " | " + SunTableFormat.minutesSeconds(o.solidBodyDiameterArcseconds())
                + " | " + MoonTableFormat.percent(o.illuminatedFraction())
                + " | " + PHASE.get(o.phase())
                + " | " + MoonTableFormat.elongation(o.elongationDegrees(),
                        o.side() == Side.EAST_OF_SUN ? "E" : "W")
                + " | " + litSide
                + " |");
    }
}
