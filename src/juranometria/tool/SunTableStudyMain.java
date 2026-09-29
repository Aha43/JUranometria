package juranometria.tool;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.Row;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solar.TimeRange;
import juranometria.solar.time.TimeScales;
import juranometria.ui.solar.SunTableFormat;

/**
 * The first Sun table, as numbers on a page (Sprint 35, issue #399):
 * representative rows for the owner to read before any application
 * surface exists, in exactly the columns, units and rounding the
 * contract froze (#398, R-d), from the bundled pack alone.
 *
 * <p>Deterministic: the instants are literals - "today" is a chosen
 * day, never the clock - and the formatting is {@code Locale.ROOT}.
 * The evidence contract regenerates this report and holds it to its
 * committed bytes. Written to stdout by {@code make sun-study}.
 */
public final class SunTableStudyMain {

    private record Site(String name, double latitude, double eastLongitude) {
    }

    private static final List<Site> SITES = List.of(
            new Site("Oslo (59.91° N, 10.75° E)", 59.91, 10.75),
            new Site("Quito (0.18° S, 78.50° W)", -0.18, -78.5),
            new Site("Cape Town (33.93° S, 18.42° E)", -33.93, 18.42),
            new Site("Alert (82.50° N, 62.34° W)", 82.50, -62.34));

    private record Case(String label, String instant) {
    }

    /**
     * The seasonal instants come from the cited fixture, never from a
     * literal here; the others are chosen instants and say so.
     */
    private static List<Case> instants() throws java.io.IOException {
        Map<String, SeasonalEventsFixture.Event> seasons =
                SeasonalEventsFixture.read();
        return List.of(
                new Case("March equinox 2026 (IMCCE)",
                        seasons.get("march-equinox").instant().toString()),
                new Case("June solstice 2026 (IMCCE)",
                        seasons.get("june-solstice").instant().toString()),
                new Case("September equinox 2026 (IMCCE)",
                        seasons.get("september-equinox").instant().toString()),
                new Case("December solstice 2026 (IMCCE)",
                        seasons.get("december-solstice").instant().toString()),
                new Case("A chosen \"today\"", "2026-09-29T12:00:00Z"),
                new Case("Near local solar midnight, midsummer (23:00 UTC)",
                        "2026-06-21T23:00:00Z"),
                new Case("First civil instant", "1900-01-01T00:00:00Z"),
                new Case("Last civil instant", "2100-12-31T23:59:59Z"));
    }

    private SunTableStudyMain() {
    }

    public static void main(String[] args) throws java.io.IOException {
        List<Case> instants = instants();
        SolarSystemService service = SolarSystemService.load();
        TimeScales scales = service.timeScales();
        System.out.println("# The Sun, computed: the first table");
        System.out.println();
        System.out.println("Sprint 35, issue #399. Every row below comes from the"
                + " bundled DE440 excerpt and the pinned IERS leap-second file,"
                + " offline, by `juranometria.solar.SolarSystemService`; the"
                + " columns, units and rounding are the contract frozen in"
                + " #398. Chart position is topocentric astrometric ICRS/J2000;"
                + " altitude and azimuth are apparent and airless, azimuth from"
                + " north through east; distance is observer to the"
                + " light-time-corrected centre; the apparent diameter uses the"
                + " IAU nominal solar radius of 695 700 km.");
        System.out.println();
        System.out.println("Time is exact from " + scales.exactFrom() + " until "
                + scales.exactUntil() + " (the pinned record's own validity);"
                + " earlier and later instants use an estimated clock correction"
                + " and are marked *est.* in the Instant column. The seasonal"
                + " instants are the IMCCE's published values, read from"
                + " `docs/studies/solar-system/seasons-2026.txt`; the instant"
                + " shown is rounded to the minute.");
        System.out.println();

        for (Site site : SITES) {
            System.out.println("## " + site.name());
            System.out.println();
            header();
            for (Case c : instants) {
                Observer observer = new Observer(site.latitude(),
                        site.eastLongitude(), Instant.parse(c.instant()));
                SunObservation o = (SunObservation) service.observe(Body.SUN,
                        observer);
                row(c.label(), o, false);
            }
            System.out.println();
        }

        System.out.println("## Oslo, a daily range: 2026-06-20 10:00 to"
                + " 2026-06-24 07:30 UTC, every day");
        System.out.println();
        System.out.println("The end is not on the daily grid, so it is appended"
                + " and marked †.");
        System.out.println();
        header();
        Observer oslo = new Observer(59.91, 10.75,
                Instant.parse("2026-06-20T10:00:00Z"));
        TimeRange range = new TimeRange(oslo.instant(),
                Instant.parse("2026-06-24T07:30:00Z"), Duration.ofDays(1));
        for (Row r : service.observe(Body.SUN, oslo, range)) {
            row("", (SunObservation) r.observation(), r.sample().appendedEnd());
        }
        System.out.println();
        System.out.println("## Oslo, an hourly range through a summer night:"
                + " 2026-06-21 20:00 to 2026-06-22 04:00 UTC");
        System.out.println();
        header();
        TimeRange night = new TimeRange(Instant.parse("2026-06-21T20:00:00Z"),
                Instant.parse("2026-06-22T04:00:00Z"), Duration.ofHours(1));
        for (Row r : service.observe(Body.SUN, oslo, night)) {
            row("", (SunObservation) r.observation(), r.sample().appendedEnd());
        }
        System.out.println();
        System.out.println("Dates before " + scales.exactFrom() + " and after "
                + scales.exactUntil() + " use an estimated clock correction."
                + " After 2026, uncertainty in local horizon coordinates cannot"
                + " yet be stated precisely. The effect on the J2000 chart"
                + " position remains below 6″ through 2100 under the"
                + " adopted model.");
    }

    private static void header() {
        System.out.println("| Case | Instant (UTC) | Right ascension (J2000)"
                + " | Declination (J2000) | Ecliptic longitude (J2000)"
                + " | Altitude (no refraction) | Azimuth (from north through east)"
                + " | Distance | Apparent diameter |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
    }

    private static void row(String label, SunObservation o, boolean appended) {
        String instant = SunTableFormat.minute(o.instant())
                + (appended ? " \u2020" : "")
                + (o.timeConfidence() == TimeScales.Confidence.EXACT
                        ? "" : " *est.*");
        System.out.println("| " + label + " | " + instant + " | "
                + SunTableFormat.hms(o.astrometricJ2000()) + " | "
                + SunTableFormat.dms(o.astrometricJ2000())
                + " | " + SunTableFormat.degrees(o.eclipticLongitudeJ2000Degrees())
                + " | " + SunTableFormat.altitude(o.horizontal().altitudeDegrees(),
                        "(below the horizon)")
                + " | " + SunTableFormat.degrees(o.horizontal().azimuthDegrees())
                + " | " + SunTableFormat.astronomicalUnits(o.distanceAu())
                + " AU (" + SunTableFormat.millionKilometres(o.distanceKm())
                + " mill. km)"
                + " | " + SunTableFormat.minutesSeconds(o.angularDiameterArcseconds())
                + " |");
    }
}
