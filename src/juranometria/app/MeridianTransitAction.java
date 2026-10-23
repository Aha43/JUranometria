package juranometria.app;

import java.time.Instant;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import juranometria.chart.Selection;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.sky.Observer;
import juranometria.sky.SiderealSolver;
import juranometria.sky.SkyFrame;
import juranometria.solar.JovianSystemService;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SolarSystemService;
import juranometria.ui.language.InterfaceText;

/**
 * <em>Set chart time to its next meridian transit</em> (Sprint 54,
 * issue #612 absorbing #569, ruling P4): for the selected star,
 * deep-sky object or Solar System body, the next upper culmination at
 * or after the chart time, solved by {@link SiderealSolver} on the
 * object's apparent right ascension of date - a catalogue position
 * precessed and nutated to the date; a body's place as its service
 * computes it, topocentric where parallax applies - and installed as
 * the chart time through the same path Place and Time uses. The
 * answer states the altitude on the meridian and names
 * <em>below the horizon</em> when it is, so that <em>on the
 * meridian</em> never reads as <em>observable</em>; when the solver
 * does not settle within its bound, the chart time is unchanged and
 * the answer says so. Nothing here keeps a second clock: the chart's
 * instant is read when the action runs, and the result is installed or
 * nothing is.
 */
final class MeridianTransitAction implements Function<Selection, String> {

    private final MeridianModule meridian;
    private final Supplier<SolarSystemService> solarSystem;
    private final Supplier<JovianSystemService> jovianSystem;
    private final Supplier<SaturnSystemService> saturnSystem;
    private final InterfaceText said;

    MeridianTransitAction(MeridianModule meridian, Supplier<SolarSystemService> solarSystem,
                          Supplier<JovianSystemService> jovianSystem,
                          Supplier<SaturnSystemService> saturnSystem, InterfaceText said) {
        if (meridian == null || solarSystem == null || jovianSystem == null
                || saturnSystem == null || said == null) {
            throw new IllegalArgumentException(
                    "the action installs through the meridian module and reads the services");
        }
        this.meridian = meridian;
        this.solarSystem = solarSystem;
        this.jovianSystem = jovianSystem;
        this.saturnSystem = saturnSystem;
        this.said = said;
    }

    /**
     * The selected object's apparent place of date at an instant, for
     * the observer's place, or null when the selection has none.
     */
    Function<Instant, SkyPosition> apparentOfDate(Selection selection, Observer from) {
        if (selection instanceof Selection.Object object) {
            // A catalogue place, J2000: precessed and nutated to the date.
            return at -> SkyFrame.toOfDate(object.position(), SkyFrame.julianDate(at));
        }
        if (selection instanceof Selection.Occurrence body) {
            Function<Observer, SkyPosition> place = placeOf(body.identity());
            return place == null ? null : at -> place.apply(from.at(at));
        }
        return null;
    }

    /** A body's apparent place of date for an observer, by its identity on the page. */
    private Function<Observer, SkyPosition> placeOf(String identity) {
        switch (identity) {
            case juranometria.solarchart.SolarSystemModule.SUN:
                return o -> ((SolarSystemService.SunObservation) solarSystem.get()
                        .observe(SolarSystemService.Body.SUN, o)).apparentOfDate();
            case juranometria.solarchart.SolarSystemModule.MOON:
                // Topocentric, as the Moon's service computes it (#408).
                return o -> ((SolarSystemService.MoonObservation) solarSystem.get()
                        .observe(SolarSystemService.Body.MOON, o)).apparentOfDate();
            case "venus":
                return o -> ((juranometria.solar.VenusService.VenusObservation) solarSystem.get()
                        .observe(SolarSystemService.Body.VENUS, o)).apparentOfDate();
            case "mercury":
                return o -> ((juranometria.solar.MercuryService.MercuryObservation) solarSystem
                        .get().observe(SolarSystemService.Body.MERCURY, o)).apparentOfDate();
            case juranometria.jovianchart.JovianModule.JUPITER:
                return o -> jovianSystem.get().observeJupiter(o).apparentOfDate();
            case juranometria.saturnchart.SaturnModule.SATURN:
                return o -> saturnSystem.get().observeSaturn(o).apparentOfDate();
            default:
                break;
        }
        for (JovianSystemService.Moon moon : JovianSystemService.Moon.values()) {
            if (juranometria.jovianchart.JovianModule.identityOf(moon).equals(identity)) {
                return o -> jovianSystem.get().observeMoons(o).moon(moon).apparentOfDate();
            }
        }
        for (SaturnSystemService.Moon moon : SaturnSystemService.Moon.values()) {
            if (juranometria.saturnchart.SaturnModule.identityOf(moon).equals(identity)) {
                return o -> saturnSystem.get().observeMoons(o).moon(moon).apparentOfDate();
            }
        }
        return null;
    }

    /**
     * Runs the action: solves, installs, and answers in words - what
     * was set and the altitude then, or why nothing was.
     */
    @Override
    public String apply(Selection selection) {
        Observer from = meridian.attached() ? meridian.observer() : null;
        if (from == null) {
            return said.say("inspector.transit.unknown");
        }
        Function<Instant, SkyPosition> place = apparentOfDate(selection, from);
        if (place == null) {
            return said.say("inspector.transit.unknown");
        }
        SiderealSolver.Culmination transit = SiderealSolver.nextUpperCulmination(from, place);
        if (transit == null) {
            return said.say("inspector.transit.refused",
                    Integer.toString(SiderealSolver.ROUNDS));
        }
        // The same installation path as Place and Time: the module's,
        // which is the holder's (#612, P6).
        meridian.observer(from.at(transit.instant()));
        return said.say("inspector.transit.set",
                juranometria.ui.solar.SolarTableSession.shown(transit.instant()),
                String.format(Locale.ROOT, "%.1f", transit.altitudeDegrees()),
                transit.belowHorizon() ? said.say("inspector.transit.below") : "");
    }
}
