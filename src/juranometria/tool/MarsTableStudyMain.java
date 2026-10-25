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
import juranometria.solar.MarsService;
import juranometria.solar.MarsService.Configuration;
import juranometria.solar.MarsService.LimbConditioning;
import juranometria.solar.MarsService.MarsObservation;
import juranometria.solar.MarsService.MoonPlace;
import juranometria.solar.MarsService.Phase;
import juranometria.solar.MarsService.VisibilityState;
import juranometria.solar.SolarSystemService.Side;
import juranometria.solar.TimeRange;
import juranometria.solar.time.TimeScales;
import juranometria.ui.solar.MoonTableFormat;
import juranometria.ui.solar.SunTableFormat;

/**
 * The first Mars table, as numbers on a page (issue #602): representative
 * rows for the owner to read before any application surface exists, from
 * the bundled packs alone, in the card and moon columns the contract
 * proposed and the owner ruled on #601 (M-9).
 *
 * <p>Deterministic: the named instants are the study's boundary instants
 * and Espenak's rows in {@code docs/studies/mars-system/mars-events.txt},
 * "today" is a chosen day, never the clock, and the formatting is
 * {@code Locale.ROOT}. The words are the ruled ones in English, held here
 * until the reader's table (#603) carries them in the language files. The
 * evidence contract regenerates this report and holds it to its committed
 * bytes. Written to stdout by {@code make mars-table-study}.
 */
public final class MarsTableStudyMain {

    private record Site(String name, double latitude, double eastLongitude) {
    }

    private static final List<Site> SITES = List.of(
            new Site("Oslo (59.91° N, 10.75° E)", 59.91, 10.75),
            new Site("Quito (0.18° S, 78.50° W)", -0.18, -78.5),
            new Site("Cape Town (33.93° S, 18.42° E)", -33.93, 18.42),
            new Site("Alert (82.50° N, 62.34° W)", 82.50, -62.34),
            new Site("Chatham Islands (43.95° S, 176.50° W)", -43.95, -176.5));

    private record Case(String label, Instant instant) {
    }

    private static final Map<Phase, String> PHASE = new EnumMap<>(Map.of(
            Phase.IN_FRONT_OF_SUN, "in front of the Sun",
            Phase.BEHIND_SUN, "behind the Sun",
            Phase.NEAR_INFERIOR_CONJUNCTION, "near inferior conjunction",
            Phase.CRESCENT, "crescent",
            Phase.NEAR_HALF_LIT, "near half lit",
            Phase.GIBBOUS, "gibbous",
            Phase.NEAR_FULL, "near full"));

    private static final Map<VisibilityState, String> STATE = new EnumMap<>(Map.of(
            VisibilityState.BEHIND_MARS_IN_ITS_SHADOW, "behind Mars, in its shadow",
            VisibilityState.BEHIND_MARS, "behind Mars",
            VisibilityState.IN_FRONT_OF_MARS, "in front of Mars",
            VisibilityState.IN_MARS_SHADOW, "in Mars's shadow",
            VisibilityState.PARTLY_IN_MARS_SHADOW, "partly in Mars's shadow",
            VisibilityState.CLEAR_OF_MARS, "clear of Mars"));

    private MarsTableStudyMain() {
    }

    private static List<Case> cases() throws IOException {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("First civil instant", Instant.parse("1900-01-01T00:00:00Z")));
        cases.add(new Case("J2000", Instant.parse("2000-01-01T12:00:00Z")));
        cases.add(new Case("A chosen \"today\"", Instant.parse("2026-10-25T12:00:00Z")));
        cases.add(new Case("Last civil instant", Instant.parse("2100-12-31T23:59:59Z")));
        DateTimeFormatter hour = DateTimeFormatter.ofPattern("uuuu-MMM-dd HH:mm", Locale.ENGLISH);
        for (String line : Files.readAllLines(MarsComparison.EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")
                    || !(line.contains("Opposition") || line.contains("Conjunction"))) {
                continue;
            }
            String[] f = line.strip().split("\\s+");
            Instant listed = LocalDateTime.parse(String.format(Locale.ROOT, "%s-%s-%s %s:00",
                    f[0], f[1], f[2], f[3]), hour).toInstant(ZoneOffset.UTC);
            cases.add(new Case((line.contains("Opposition") ? "Opposition" : "Conjunction with the Sun")
                    + " (Espenak, to the hour)", listed));
        }
        cases.sort(java.util.Comparator.comparing(Case::instant));
        return cases;
    }

    public static void main(String[] args) throws IOException {
        MarsService service = MarsService.load();
        TimeScales scales = service.timeScales();
        List<Case> cases = cases();
        System.out.println("# Mars, computed: the first table");
        System.out.println();
        System.out.println("Issue #602. Every row below comes from the bundled planets pack's DE440"
                + " excerpt for Mars (the Mars barycentre, within 0.210 m of the centre), the Mars"
                + " System pack's MAR099 excerpt for Phobos and Deimos, and the Solar System pack's"
                + " Sun, Earth and pinned IERS leap-second file, offline, by"
                + " `juranometria.solar.MarsService`, under the contract ruled on #601. Chart"
                + " position is topocentric astrometric ICRS/J2000; altitude and azimuth are apparent"
                + " and airless, azimuth from north through east; the apparent diameter is across the"
                + " equator and from pole to pole of the projected figure at the tilt; the"
                + " illuminated fraction and the phase angle take the Sun where Mars sees it; the"
                + " north pole's position angle is of date, from celestial north through east, from"
                + " the IAU 2015 pole with its periodic terms; the tilt is the observer's"
                + " planetocentric latitude over that pole, with the pole tipped toward the observer;"
                + " the elongation is the unsigned angle from the apparent Sun, with E or W by right"
                + " ascension; the lit side is the bright limb's position angle with the nearest of"
                + " sixteen compass points. A moon's offsets are Horizons' apparent differential"
                + " coordinates, e and w, n and s; its state is from its disc against Mars's"
                + " flattened figure and its place against the same figure's shadow, in Jupiter's"
                + " precedence. The words are the ones ruled on #601, held here in English until the"
                + " reader's table carries them.");
        System.out.println();
        System.out.println("Time is exact from " + scales.exactFrom() + " until " + scales.exactUntil()
                + " (the pinned record's own validity); earlier and later instants use an estimated"
                + " clock correction and are marked *est.* in the Instant column. Mars answers from "
                + service.firstDay() + " to " + service.lastDay() + "; Phobos and Deimos from "
                + MarsService.MOONS_FIRST_DAY + " up to, not including, 2050-01-01 00:00 TDB - the"
                + " reader is told 2000 to " + MarsService.MOONS_LAST_YEAR + ". Espenak's hours are"
                + " his published rows; the service's own instants of those events are in"
                + " `service-accuracy.md`.");
        System.out.println();

        for (Site site : SITES) {
            System.out.println("## Mars - " + site.name());
            System.out.println();
            header();
            for (Case c : cases) {
                row(c.label(), service.observe(new Observer(site.latitude(), site.eastLongitude(),
                        c.instant())), false);
            }
            System.out.println();
        }

        Observer oslo = new Observer(59.91, 10.75, Instant.parse("2026-10-25T21:00:00Z"));
        Instant end = Instant.parse("2027-06-01T21:00:00Z");
        System.out.println("## Oslo, every 14 days through the 2027 opposition: "
                + SunTableFormat.minute(oslo.instant()) + " to " + SunTableFormat.minute(end) + " UTC");
        System.out.println();
        System.out.println("The disc grows towards the 2027 opposition and shrinks after it; Mars is"
                + " never less than gibbous. The end is not on the 14-day grid, so it is appended and"
                + " marked †.");
        System.out.println();
        header();
        for (MarsService.Row r : service.observe(oslo, new TimeRange(oslo.instant(), end, Duration.ofDays(14)))) {
            row("", r.observation(), r.sample().appendedEnd());
        }
        System.out.println();

        System.out.println("## Phobos and Deimos, from the Earth's centre, at Espenak's opposition hours");
        System.out.println();
        for (Case c : cases) {
            if (!c.label().startsWith("Opposition")) {
                continue;
            }
            System.out.println("### " + c.label() + ", " + SunTableFormat.minute(c.instant()) + " UTC");
            System.out.println();
            if (!service.moonsAnswerAt(c.instant())) {
                System.out.println("Phobos and Deimos are computed for 2000 to " + MarsService.MOONS_LAST_YEAR
                        + "; this instant is outside that, so only Mars is shown above.");
                System.out.println();
                continue;
            }
            moons(service.observeMoonsGeocentric(c.instant()));
        }

        Instant start = Instant.parse("2018-07-28T00:00:00Z");
        Instant stop = Instant.parse("2018-07-28T08:10:00Z");
        System.out.println("## Phobos, from the Earth's centre, every 40 minutes through one orbit at the 2018"
                + " opposition: " + SunTableFormat.minute(start) + " to " + SunTableFormat.minute(stop) + " UTC");
        System.out.println();
        System.out.println("A range samples; it does not search for the moment a moon enters or leaves Mars's"
                + " disc or shadow. Phobos circles Mars in under eight hours; at this opposition it is"
                + " eclipsed and passes behind and in front of Mars on one orbit.");
        System.out.println();
        System.out.println("| Instant (UTC) | Side | East–west | North–south | Separation | Mars radii | State |");
        System.out.println("|---|---|---|---|---|---|---|");
        for (TimeRange.Sample sample : new TimeRange(start, stop, Duration.ofMinutes(40)).samples()) {
            MoonPlace p = service.observeMoonsGeocentric(sample.instant()).moon(MarsService.Moon.PHOBOS);
            System.out.println(String.format(Locale.ROOT, "| %s%s | %s | %.1f″ %s | %.1f″ %s | %.1f″ | %.2f | %s |",
                    SunTableFormat.minute(sample.instant()), sample.appendedEnd() ? " †" : "",
                    p.xArcseconds() >= 0 ? "east" : "west", Math.abs(p.xArcseconds()),
                    p.xArcseconds() >= 0 ? "e" : "w", Math.abs(p.yArcseconds()), p.yArcseconds() >= 0 ? "n" : "s",
                    p.separationArcseconds(), p.separationMarsRadii(), STATE.get(p.state())));
        }
        System.out.println();

        System.out.println("## The moons' last minutes");
        System.out.println();
        for (String when : new String[] {"2049-12-31T23:58:00Z", "2049-12-31T23:59:30Z"}) {
            Instant t = Instant.parse(when);
            System.out.println("- " + when.replace('T', ' ').replace("Z", " UTC") + ": "
                    + (service.moonsAnswerAt(t) ? "the moons answer."
                    : refusal(service, t)));
        }
        System.out.println();
        System.out.println("Dates before " + scales.exactFrom() + " and after " + scales.exactUntil()
                + " use an estimated clock correction. After 2026, uncertainty in local horizon"
                + " coordinates cannot yet be stated precisely; there the moons' states are the"
                + " geometry at an estimated instant, not predicted times of events.");
    }

    private static String refusal(MarsService service, Instant t) {
        try {
            service.observeMoonsGeocentric(t);
            return "the moons answer.";
        } catch (IllegalArgumentException e) {
            return "refused: \"" + e.getMessage() + "\"";
        }
    }

    private static void header() {
        System.out.println("| Case | Instant (UTC) | Right ascension (J2000) | Declination (J2000)"
                + " | Altitude (no refraction) | Azimuth | Distance | Diameter, equator / poles"
                + " | Illuminated | Phase | Elongation from the Sun | Lit side | North pole | Tilt |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|");
    }

    private static void row(String label, MarsObservation o, boolean appended) {
        String instant = SunTableFormat.minute(o.instant()) + (appended ? " †" : "")
                + (o.timeConfidence() == TimeScales.Confidence.EXACT ? "" : " *est.*");
        String litSide = o.brightLimbConditioning() == LimbConditioning.WELL_DEFINED
                ? MoonTableFormat.positionAngle(o.brightLimbAngleDegrees()) + " ("
                        + MoonTableFormat.letters(o.brightLimbCompassPoint()) + ")"
                : "not well-defined (near full)";
        System.out.println("| " + label + " | " + instant + " | "
                + SunTableFormat.hms(o.astrometricJ2000()) + " | " + SunTableFormat.dms(o.astrometricJ2000())
                + " | " + SunTableFormat.altitude(o.horizontal().altitudeDegrees(), "(below the horizon)")
                + " | " + SunTableFormat.degrees(o.horizontal().azimuthDegrees())
                + " | " + SunTableFormat.astronomicalUnits(o.distanceAu()) + " AU, "
                + SunTableFormat.millionKilometres(o.distanceKm()) + " million km"
                + " | " + String.format(Locale.ROOT, "%.1f″ / %.1f″", o.equatorialDiameterArcseconds(),
                        o.polarDiameterArcseconds())
                + " | " + MoonTableFormat.percent(o.illuminatedFraction())
                + " | " + PHASE.get(o.phase())
                + " | " + MoonTableFormat.elongation(o.elongationDegrees(),
                        o.side() == Side.EAST_OF_SUN ? "E" : "W")
                + " | " + litSide
                + " | " + String.format(Locale.ROOT, "%.1f°", o.poleAngleDegrees())
                + " | " + String.format(Locale.ROOT, "%.2f° (%s pole toward us)", Math.abs(o.tiltDegrees()),
                        o.northPoleTowardObserver() ? "north" : "south")
                + " |");
    }

    private static void moons(Configuration c) {
        System.out.println("| Moon | Side | East–west | North–south | Separation | Mars radii | State |");
        System.out.println("|---|---|---|---|---|---|---|");
        for (MoonPlace p : c.moons()) {
            String name = p.moon().name().charAt(0) + p.moon().name().substring(1).toLowerCase(Locale.ROOT);
            System.out.println(String.format(Locale.ROOT, "| %s | %s | %.1f″ %s | %.1f″ %s | %.1f″ | %.2f | %s |",
                    name, p.xArcseconds() >= 0 ? "east" : "west", Math.abs(p.xArcseconds()),
                    p.xArcseconds() >= 0 ? "e" : "w", Math.abs(p.yArcseconds()), p.yArcseconds() >= 0 ? "n" : "s",
                    p.separationArcseconds(), p.separationMarsRadii(), STATE.get(p.state())));
        }
        System.out.println();
    }
}
