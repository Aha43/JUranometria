package juranometria.tool;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.Row;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solar.TimeRange;
import juranometria.solar.time.TimeScales;

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

    private static final List<Case> INSTANTS = List.of(
            new Case("March equinox 2026", "2026-03-20T14:46:00Z"),
            new Case("June solstice 2026", "2026-06-21T02:24:00Z"),
            new Case("September equinox 2026", "2026-09-22T22:05:00Z"),
            new Case("December solstice 2026", "2026-12-21T20:50:00Z"),
            new Case("A chosen \"today\"", "2026-09-29T12:00:00Z"),
            new Case("Local midnight, midsummer", "2026-06-21T23:00:00Z"),
            new Case("First civil instant", "1900-01-01T00:00:00Z"),
            new Case("Last civil instant", "2100-12-31T23:59:59Z"));

    private static final DateTimeFormatter MINUTE = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);

    private SunTableStudyMain() {
    }

    public static void main(String[] args) {
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
                + " and are marked *est.* in the Instant column.");
        System.out.println();

        for (Site site : SITES) {
            System.out.println("## " + site.name());
            System.out.println();
            header();
            for (Case c : INSTANTS) {
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
        String instant = MINUTE.format(o.instant())
                + (appended ? " †" : "")
                + (o.timeConfidence() == TimeScales.Confidence.EXACT
                        ? "" : " *est.*");
        System.out.println("| " + label + " | " + instant + " | "
                + hms(o.astrometricJ2000()) + " | " + dms(o.astrometricJ2000())
                + " | " + String.format(Locale.ROOT, "%.2f°",
                        o.eclipticLongitudeJ2000Degrees())
                + " | " + altitude(o.horizontal().altitudeDegrees())
                + " | " + String.format(Locale.ROOT, "%.2f°",
                        o.horizontal().azimuthDegrees())
                + " | " + String.format(Locale.ROOT, "%.6f AU (%.3f mill. km)",
                        o.distanceAu(), o.distanceKm() / 1e6)
                + " | " + minutesSeconds(o.angularDiameterArcseconds()) + " |");
    }

    /** Hours, minutes and seconds to a tenth of a second. */
    static String hms(SkyPosition p) {
        double hours = p.raDegrees() / 15.0;
        int h = (int) hours;
        double m = (hours - h) * 60.0;
        int mm = (int) m;
        double s = (m - mm) * 60.0;
        s = Math.round(s * 10.0) / 10.0;
        if (s >= 60.0) {
            s -= 60.0;
            mm++;
        }
        if (mm >= 60) {
            mm -= 60;
            h = (h + 1) % 24;
        }
        return String.format(Locale.ROOT, "%02dh %02dm %04.1fs", h, mm, s);
    }

    /** Degrees, minutes and seconds to a second, sign always shown. */
    static String dms(SkyPosition p) {
        double dec = p.decDegrees();
        String sign = dec < 0 ? "−" : "+";
        double a = Math.abs(dec);
        int d = (int) a;
        double m = (a - d) * 60.0;
        int mm = (int) m;
        long s = Math.round((m - mm) * 60.0);
        if (s >= 60) {
            s -= 60;
            mm++;
        }
        if (mm >= 60) {
            mm -= 60;
            d++;
        }
        return String.format(Locale.ROOT, "%s%d° %02d′ %02d″",
                sign, d, mm, s);
    }

    /** Altitude to 0.01°, keeping the number when below the horizon. */
    static String altitude(double degrees) {
        String value = String.format(Locale.ROOT, "%.2f°", degrees)
                .replace("-", "−");
        return degrees < 0 ? value + " (below the horizon)" : value;
    }

    /** Arcminutes and arcseconds to a tenth of a second. */
    static String minutesSeconds(double arcseconds) {
        int m = (int) (arcseconds / 60.0);
        double s = arcseconds - m * 60.0;
        s = Math.round(s * 10.0) / 10.0;
        if (s >= 60.0) {
            s -= 60.0;
            m++;
        }
        return String.format(Locale.ROOT, "%d′ %04.1f″", m, s);
    }
}
