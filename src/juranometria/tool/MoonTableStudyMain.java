package juranometria.tool;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.CompassPoint;
import juranometria.solar.SolarSystemService.LimbConditioning;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solar.SolarSystemService.Phase;
import juranometria.solar.SolarSystemService.Row;
import juranometria.solar.SolarSystemService.Side;
import juranometria.solar.TimeRange;
import juranometria.solar.time.TimeScales;
import juranometria.ui.solar.MoonTableFormat;
import juranometria.ui.solar.SunTableFormat;

/**
 * The first Moon table, as numbers on a page (Sprint 36, issue #407):
 * representative rows for the owner to read before any application
 * surface exists, in exactly the columns, units and rounding the
 * contract froze (#406, M5 and M6), from the bundled pack alone.
 *
 * <p>Deterministic: the instants are literals or come from the cited
 * fixtures - "today" is a chosen day, never the clock - and the
 * formatting is {@code Locale.ROOT}. The words are English here; the
 * reader's table takes them from the language files (#408). The
 * evidence contract regenerates this report and holds it to its
 * committed bytes. Written to stdout by {@code make moon-study}.
 */
public final class MoonTableStudyMain {

    private record Site(String name, double latitude, double eastLongitude) {
    }

    private static final List<Site> SITES = List.of(
            new Site("Oslo (59.91° N, 10.75° E)", 59.91, 10.75),
            new Site("Quito (0.18° S, 78.50° W)", -0.18, -78.5),
            new Site("Cape Town (33.93° S, 18.42° E)", -33.93, 18.42),
            new Site("Alert (82.50° N, 62.34° W)", 82.50, -62.34));

    private record Case(String label, String instant) {
    }

    /** The English words of the phase categories (#406, M5). */
    static final Map<Phase, String> PHASE_WORDS = Map.of(
            Phase.NEAR_NEW, "near new Moon",
            Phase.WAXING_CRESCENT, "waxing crescent",
            Phase.NEAR_FIRST_QUARTER, "near first quarter",
            Phase.WAXING_GIBBOUS, "waxing gibbous",
            Phase.NEAR_FULL, "near full Moon",
            Phase.WANING_GIBBOUS, "waning gibbous",
            Phase.NEAR_LAST_QUARTER, "near last quarter",
            Phase.WANING_CRESCENT, "waning crescent");

    /** The English words of the sixteen compass points (#406, M6). */
    static final String[] COMPASS_WORDS = {"north", "north-northeast",
            "northeast", "east-northeast", "east", "east-southeast", "southeast",
            "south-southeast", "south", "south-southwest", "southwest",
            "west-southwest", "west", "west-northwest", "northwest",
            "north-northwest"};

    /**
     * The named events come from the cited fixture, never from a
     * literal here; the others are chosen instants and say so.
     */
    private static List<Case> instants() throws java.io.IOException {
        Map<String, MoonEventsFixture.Event> events = MoonEventsFixture.read();
        return List.of(
                new Case("New Moon, June 2026 (Espenak)",
                        events.get("new-moon-june").instant().toString()),
                new Case("First quarter, June 2026 (Espenak)",
                        events.get("first-quarter-june").instant().toString()),
                new Case("Full Moon, June 2026 (Espenak)",
                        events.get("full-moon-june").instant().toString()),
                new Case("Last quarter, July 2026 (Espenak)",
                        events.get("last-quarter-july").instant().toString()),
                new Case("Nearest perigee of 2026 (Espenak)",
                        events.get("perigee-nearest").instant().toString()),
                new Case("Farthest apogee of 2026 (Espenak)",
                        events.get("apogee-farthest").instant().toString()),
                new Case("A chosen \"today\"", "2026-09-29T12:00:00Z"),
                new Case("Near local solar midnight, midsummer (23:00 UTC)",
                        "2026-06-21T23:00:00Z"),
                new Case("First civil instant", "1900-01-01T00:00:00Z"),
                new Case("Last civil instant", "2100-12-31T23:59:59Z"));
    }

    private MoonTableStudyMain() {
    }

    public static void main(String[] args) throws java.io.IOException {
        List<Case> instants = instants();
        SolarSystemService service = SolarSystemService.load();
        TimeScales scales = service.timeScales();
        System.out.println("# The Moon, computed: the first table");
        System.out.println();
        System.out.println("Sprint 36, issue #407. Every row below comes from the"
                + " bundled DE440 excerpt and the pinned IERS leap-second file,"
                + " offline, by `juranometria.solar.SolarSystemService`; the"
                + " columns, units and rounding are the contract ruled in"
                + " #406. Chart position is topocentric astrometric ICRS/J2000;"
                + " altitude and azimuth are apparent and airless, azimuth from"
                + " north through east; distance is observer to the"
                + " light-time-corrected centre; the apparent diameter uses the"
                + " IAU mean lunar radius of 1 737.4 km; the illuminated"
                + " fraction follows the phase angle at the Moon; the phase"
                + " word is the visual category from that fraction and the"
                + " global waxing/waning sequence; the elongation is the"
                + " unsigned angle from the apparent Sun in this observer's"
                + " sky, with E when the Moon is east of the Sun there (the"
                + " evening sky) and W when west; the lit side is the position"
                + " angle of the bright limb's midpoint from celestial north"
                + " through east, with the nearest of sixteen compass points,"
                + " and is not well-defined near new and full Moon.");
        System.out.println();
        System.out.println("Time is exact from " + scales.exactFrom() + " until "
                + scales.exactUntil() + " (the pinned record's own validity);"
                + " earlier and later instants use an estimated clock correction"
                + " and are marked *est.* in the Instant column. The named"
                + " events are Fred Espenak's published instants at minute"
                + " precision, read from"
                + " `docs/studies/solar-system/moon-events-2026.txt`; the"
                + " instant shown is rounded to the minute.");
        System.out.println();

        for (Site site : SITES) {
            System.out.println("## " + site.name());
            System.out.println();
            header();
            for (Case c : instants) {
                Observer observer = new Observer(site.latitude(),
                        site.eastLongitude(), Instant.parse(c.instant()));
                MoonObservation o = (MoonObservation) service.observe(Body.MOON,
                        observer);
                row(c.label(), o, false);
            }
            System.out.println();
        }

        System.out.println("## Oslo, a daily range through one lunation:"
                + " 2026-06-15 03:00 to 2026-07-14 09:44 UTC, every day");
        System.out.println();
        System.out.println("The end is not on the daily grid, so it is appended"
                + " and marked †.");
        System.out.println();
        header();
        Observer oslo = new Observer(59.91, 10.75,
                Instant.parse("2026-06-15T03:00:00Z"));
        TimeRange lunation = new TimeRange(oslo.instant(),
                Instant.parse("2026-07-14T09:44:00Z"), Duration.ofDays(1));
        for (Row r : service.observe(Body.MOON, oslo, lunation)) {
            row("", (MoonObservation) r.observation(), r.sample().appendedEnd());
        }
        System.out.println();
        System.out.println("## Oslo, an hourly range through a summer night:"
                + " 2026-06-21 20:00 to 2026-06-22 04:00 UTC");
        System.out.println();
        header();
        TimeRange night = new TimeRange(Instant.parse("2026-06-21T20:00:00Z"),
                Instant.parse("2026-06-22T04:00:00Z"), Duration.ofHours(1));
        for (Row r : service.observe(Body.MOON, oslo, night)) {
            row("", (MoonObservation) r.observation(), r.sample().appendedEnd());
        }
        System.out.println();
        System.out.println("Dates before " + scales.exactFrom() + " and after "
                + scales.exactUntil() + " use an estimated clock correction."
                + " After 2026, uncertainty in local horizon coordinates cannot"
                + " yet be stated precisely.");
    }

    private static void header() {
        System.out.println("| Case | Instant (UTC) | Right ascension (J2000)"
                + " | Declination (J2000)"
                + " | Altitude (no refraction) | Azimuth (from north through east)"
                + " | Distance | Apparent diameter | Illuminated | Phase"
                + " | Elongation from the Sun | Lit side |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|---|");
    }

    static String litSide(MoonObservation o) {
        if (o.brightLimbConditioning() == LimbConditioning.NEAR_NEW_OR_FULL) {
            return o.phase() == Phase.NEAR_NEW
                    ? "not well-defined (near new Moon)"
                    : "not well-defined (near full Moon)";
        }
        CompassPoint point = o.brightLimbCompassPoint();
        return MoonTableFormat.positionAngle(o.brightLimbAngleDegrees())
                + " (" + COMPASS_WORDS[point.ordinal()] + ")";
    }

    private static void row(String label, MoonObservation o, boolean appended) {
        String instant = SunTableFormat.minute(o.instant())
                + (appended ? " †" : "")
                + (o.timeConfidence() == TimeScales.Confidence.EXACT
                        ? "" : " *est.*");
        System.out.println("| " + label + " | " + instant + " | "
                + SunTableFormat.hms(o.astrometricJ2000()) + " | "
                + SunTableFormat.dms(o.astrometricJ2000())
                + " | " + SunTableFormat.altitude(o.horizontal().altitudeDegrees(),
                        "(below the horizon)")
                + " | " + SunTableFormat.degrees(o.horizontal().azimuthDegrees())
                + " | " + MoonTableFormat.kilometres(o.distanceKm())
                + " | " + SunTableFormat.minutesSeconds(o.angularDiameterArcseconds())
                + " | " + MoonTableFormat.percent(o.illuminatedFraction())
                + " | " + PHASE_WORDS.get(o.phase())
                + " | " + MoonTableFormat.elongation(o.elongationDegrees(),
                        o.side() == Side.EAST_OF_SUN ? "E" : "W")
                + " | " + litSide(o)
                + " |");
    }
}
