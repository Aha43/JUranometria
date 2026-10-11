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
import juranometria.solar.SolarSystemService.Side;
import juranometria.solar.TimeRange;
import juranometria.solar.VenusService;
import juranometria.solar.VenusService.LimbConditioning;
import juranometria.solar.VenusService.Phase;
import juranometria.solar.VenusService.VenusObservation;
import juranometria.solar.time.TimeScales;
import juranometria.ui.solar.MoonTableFormat;
import juranometria.ui.solar.SunTableFormat;

/**
 * The first Venus table, as numbers on a page (Sprint 47, issue #501):
 * representative rows for the owner to read before any application
 * surface exists, from the bundled packs alone, in the Moon table's
 * columns, units and rounding - the family the contract ruled Venus
 * into on #500.
 *
 * <p>Deterministic: the named instants come from the cited fixtures -
 * Espenak's rows in {@code docs/studies/venus/venus-events.txt} and the
 * representative appearances' rows in
 * {@code docs/studies/venus/horizons/appearances-oslo-299.txt} - "today"
 * is a chosen day, never the clock, and the formatting is
 * {@code Locale.ROOT}. The phase words are the ones ruled on #500 in
 * English, held here until the reader's table (#502) carries them in
 * the language files. The evidence contract regenerates this report
 * and holds it to its committed bytes. Written to stdout by
 * {@code make venus-table-study}.
 */
public final class VenusTableStudyMain {

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

    /** The phase words ruled on #500 (ruling 5), English. */
    private static final Map<Phase, String> PHASE = new EnumMap<>(Map.of(
            Phase.IN_FRONT_OF_SUN, "in front of the Sun",
            Phase.BEHIND_SUN, "behind the Sun",
            Phase.NEAR_INFERIOR_CONJUNCTION, "near inferior conjunction",
            Phase.CRESCENT, "crescent",
            Phase.NEAR_HALF_LIT, "near half lit",
            Phase.GIBBOUS, "gibbous",
            Phase.NEAR_FULL, "near full"));

    private VenusTableStudyMain() {
    }

    /** Espenak's events and the representative appearances, read from the fixtures. */
    private static List<Case> cases() throws IOException {
        List<Case> cases = new ArrayList<>();
        for (String line : Files.readAllLines(VenusComparison.EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            cases.add(new Case(f[2].substring(10).strip() + " (Espenak, to the hour)",
                    Instant.parse(f[1].replace("Z", ":00Z"))));
        }
        for (String[] f : VenusComparison.rows("appearances-oslo-299")) {
            cases.add(new Case("A representative appearance (Horizons' weekly row)",
                    VenusComparison.instant(f[0])));
        }
        cases.add(new Case("A chosen \"today\"", Instant.parse("2026-10-11T12:00:00Z")));
        cases.add(new Case("First civil instant", Instant.parse("1900-01-01T00:00:00Z")));
        cases.add(new Case("Last civil instant", Instant.parse("2100-12-31T23:59:59Z")));
        cases.sort(java.util.Comparator.comparing(Case::instant));
        return cases;
    }

    public static void main(String[] args) throws IOException {
        VenusService service = VenusService.load();
        TimeScales scales = service.timeScales();
        List<Case> cases = cases();
        System.out.println("# Venus, computed: the first table");
        System.out.println();
        System.out.println("Sprint 47, issue #501. Every row below comes from the bundled planets"
                + " pack's DE440 excerpt for Venus and the Solar System pack's Sun, Earth and"
                + " pinned IERS leap-second file, offline, by `juranometria.solar.VenusService`,"
                + " under the contract ruled on #500. Chart position is topocentric astrometric"
                + " ICRS/J2000; altitude and azimuth are apparent and airless, azimuth from north"
                + " through east; distance is observer to the light-time-corrected centre, in AU"
                + " and million km; the apparent diameter is the solid body's, from the IAU radius"
                + " of 6 051.8 km (the visible cloud deck is about 1.22 % larger, never drawn); the"
                + " illuminated fraction follows the Moon's S-T-O phase angle at Venus; the phase"
                + " word is in front of or behind the Sun when Venus's centre is within the Sun's"
                + " apparent radius plus its own, and otherwise the Moon's bands of the fraction;"
                + " the elongation is the unsigned angle from the apparent Sun in this observer's"
                + " sky, with E when Venus is east of the Sun there in right ascension (the evening"
                + " sky) and W when west - it names no conjunction; the lit side is the position"
                + " angle of the bright limb's midpoint from celestial north through east, of date,"
                + " with the nearest of sixteen compass points, and is not well-defined near"
                + " inferior conjunction and near full. The phase words are the ones ruled on #500,"
                + " held here in English until the reader's table carries them.");
        System.out.println();
        System.out.println("Time is exact from " + scales.exactFrom() + " until "
                + scales.exactUntil() + " (the pinned record's own validity); earlier and later"
                + " instants use an estimated clock correction and are marked *est.* in the"
                + " Instant column. Espenak's events are his published hours, read from"
                + " `docs/studies/venus/venus-events.txt`; the representative appearances are"
                + " Horizons' weekly Oslo rows nearest a quarter or three quarters lit, read from"
                + " `docs/studies/venus/horizons/appearances-oslo-299.txt`.");
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

        Observer oslo = new Observer(59.91, 10.75, Instant.parse("2026-06-01T21:00:00Z"));
        System.out.println("## Oslo, every 14 days through the 2026-27 apparition:"
                + " 2026-06-01 21:00 to 2027-01-31 21:00 UTC");
        System.out.println();
        System.out.println("The disc grows as the lit part shrinks, and the side of the Sun changes"
                + " around inferior conjunction. The end is not on the 14-day grid,"
                + " so it is appended and marked †.");
        System.out.println();
        header();
        for (VenusService.Row r : service.observe(oslo, new TimeRange(oslo.instant(),
                Instant.parse("2027-01-31T21:00:00Z"), Duration.ofDays(14)))) {
            row("", r.observation(), r.sample().appendedEnd());
        }
        System.out.println();
        System.out.println("## Oslo, hourly through the 2012 transit:"
                + " 2012-06-05 21:00 to 2012-06-06 06:00 UTC");
        System.out.println();
        header();
        for (VenusService.Row r : service.observe(oslo, new TimeRange(
                Instant.parse("2012-06-05T21:00:00Z"), Instant.parse("2012-06-06T06:00:00Z"),
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

    private static void row(String label, VenusObservation o, boolean appended) {
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
