package juranometria.solar;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.solar.SaturnSystemService.Moon;
import juranometria.solar.SaturnSystemService.MoonPlace;
import juranometria.solar.SaturnSystemService.Rival;
import juranometria.solar.SaturnSystemService.SaturnObservation;
import juranometria.tool.SaturnComparison;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mutation proof for Saturn (issue #575): the mistakes the contract
 * ruled on #574 guards against would be caught by the same oracle. Each
 * case is a rival - one step of the service done the wrong way - held to
 * Horizons' own rows at Oslo (the committed daily 2026 year, or the
 * weekly matrix where the mistake grows with the years); each
 * must fail its ruled target by a margin while the service itself meets
 * it, so the targets are known to bite.
 */
class SaturnMutationTest {

    private static final String SATURN_YEAR = "dense-2026-saturn-oslo";
    private static SaturnSystemService service;

    @BeforeAll
    static void load() {
        service = SaturnSystemService.load();
    }

    private static Observer oslo(String[] f) {
        return SaturnComparison.observer("oslo", SaturnComparison.stamp(f[0]));
    }

    private static double sep(SkyPosition p, String ra, String dec) {
        return p.separationDegrees(new SkyPosition(Double.parseDouble(ra.strip()),
                Double.parseDouble(dec.strip()))) * 3600.0;
    }

    /** Horizons' planetodetic reading of a planetocentric angle, on the pack's spheroid. */
    private static double planetodetic(double centricDegrees) {
        SaturnPack.Constants k = service.pack().constants();
        double ratio = k.saturnEquatorialRadiusKm() / k.saturnPolarRadiusKm();
        return Math.toDegrees(Math.atan(Math.tan(Math.toRadians(centricDegrees)) * ratio * ratio));
    }

    @Test
    void theBarycentreForTheCentreFailsTheAstrometricTarget() throws IOException {
        List<String> h = SaturnComparison.header(SATURN_YEAR);
        int ra = SaturnComparison.column(h, "R.A.___");
        int dec = SaturnComparison.column(h, "DEC____");
        double rival = 0.0;
        double ours = 0.0;
        for (String[] f : SaturnComparison.rows(SATURN_YEAR)) {
            rival = Math.max(rival, sep(service.observeSaturnRival(oslo(f), Rival.BARYCENTRE_FOR_CENTRE)
                    .astrometricJ2000(), f[ra], f[dec]));
            ours = Math.max(ours, sep(service.observeSaturn(oslo(f)).astrometricJ2000(), f[ra], f[dec]));
        }
        assertTrue(ours <= 0.01, "the service meets the ruled 0.01″: " + ours);
        assertTrue(rival > 0.02, "the 6 → 699 offset moves Saturn by several hundredths of an arcsecond;"
                + " the barycentre misses the 0.01″ target: " + rival + "″");
    }

    @Test
    void thePlanetodeticLatitudeForTheOpeningFailsTheRingTarget() throws IOException {
        List<String> h = SaturnComparison.header(SATURN_YEAR);
        int obs = SaturnComparison.column(h, "ObsSub-LAT");
        double rival = 0.0;
        for (String[] f : SaturnComparison.rows(SATURN_YEAR)) {
            SaturnObservation o = service.observeSaturnRival(oslo(f), Rival.PLANETODETIC_OPENING);
            rival = Math.max(rival, Math.abs(planetodetic(o.ringOpeningDegrees())
                    - Double.parseDouble(f[obs].strip())) * 3600.0);
        }
        assertTrue(rival > 1000.0, "Horizons' planetodetic latitude is not the opening; taken for it, the"
                + " opening is wrong by more than a quarter of a degree in 2026: " + rival + "″");
    }

    @Test
    void anUnaberratedSunFailsTheSunsOpeningTarget() throws IOException {
        List<String> h = SaturnComparison.header(SATURN_YEAR);
        int sun = SaturnComparison.column(h, "SunSub-LAT");
        double rival = 0.0;
        double ours = 0.0;
        for (String[] f : SaturnComparison.rows(SATURN_YEAR)) {
            double horizons = Double.parseDouble(f[sun].strip());
            rival = Math.max(rival, Math.abs(planetodetic(service.observeSaturnRival(oslo(f),
                    Rival.UNABERRATED_SUN).sunRingOpeningDegrees()) - horizons) * 3600.0);
            ours = Math.max(ours, Math.abs(planetodetic(service.observeSaturn(oslo(f))
                    .sunRingOpeningDegrees()) - horizons) * 3600.0);
        }
        assertTrue(ours <= 0.01, "the service meets the ruled 0.01″: " + ours);
        assertTrue(rival > 1.0, "Saturn's own motion aberrates the Sun it sees by arcseconds; left out,"
                + " B′ misses the 0.01″ target: " + rival + "″");
    }

    @Test
    void offsetsFromTheJ2000PlacesFailTheOffsetTarget() throws IOException {
        String iapetus = "matrix-7d-iapetus-oslo";
        List<String> h = SaturnComparison.header(iapetus);
        int x = SaturnComparison.column(h, "X_(sat-prim)");
        int y = SaturnComparison.column(h, "Y_(sat-prim)");
        double rival = 0.0;
        double ours = 0.0;
        for (String[] f : SaturnComparison.rows(iapetus)) {
            if (SaturnComparison.stamp(f[0]).isBefore(java.time.Instant.parse("2000-01-01T00:00:00Z"))) {
                continue;
            }
            MoonPlace r = service.configurationRival(oslo(f), Rival.J2000_OFFSETS).moon(Moon.IAPETUS);
            MoonPlace p = service.observeMoons(oslo(f)).moon(Moon.IAPETUS);
            double hx = Double.parseDouble(f[x].strip());
            double hy = Double.parseDouble(f[y].strip());
            rival = Math.max(rival, Math.hypot(r.xArcseconds() - hx, r.yArcseconds() - hy));
            ours = Math.max(ours, Math.hypot(p.xArcseconds() - hx, p.yArcseconds() - hy));
        }
        assertTrue(ours <= 0.5, "the service meets the ruled 0.5″: " + ours);
        assertTrue(rival > 0.5, "the century's precession turns Iapetus's offsets from the J2000 places"
                + " away from Horizons' apparent ones, past the 0.5″ target: " + rival + "″");
    }
}
