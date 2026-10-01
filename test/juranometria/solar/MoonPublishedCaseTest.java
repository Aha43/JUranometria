package juranometria.solar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.solar.SolarSystemService.MoonGeometry;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A published case from outside the JPL family (issue #406, M8).
 *
 * <p>Meeus, <i>Astronomical Algorithms</i> (2nd ed., 1998), examples
 * 47.a and 48.a: the Moon on 1992 April 12.0 TD, geocentric, by the
 * book's truncated ELP-2000/82 series, which the book puts at about
 * 10″ in longitude and 4″ in latitude - apparent right ascension
 * 134.688470° (8h 58m 45.2s), declination +13.768368°, distance
 * 368 409.7 km; phase angle 69.0756°, illuminated fraction 0.6786,
 * bright-limb position angle 285.0°. The service must land within the
 * book's own precision; the phase angle it computes is Horizons'
 * S-T-O, which carries two aberrations the book's geometric angle does
 * not, up to 41″ between them, and the comparison allows exactly that.
 * Horizons, asked for the same TT instant geocentrically, must land
 * inside the exact-era target, so the case exercises the same
 * intermediate both ways. The #406 study measured the same case with
 * Skyfield on DE440: α −2.0″, δ +0.3″, Δ +29.7 km, k −0.0001,
 * χ +0.04° from the book, so a misquoted digit would show here.
 */
class MoonPublishedCaseTest {

    private static final double JD_TT = 2448724.5; // 1992-04-12.0 TD

    @Test
    void meeus47aAnd48aAreReproducedWithinTheBooksPrecision() {
        SolarSystemService service = SolarSystemService.load();
        MoonGeometry g = service.geocentricMoon(JD_TT);
        double separation = g.apparentOfDate().separationDegrees(
                new SkyPosition(134.688470, 13.768368)) * 3600.0;
        assertTrue(separation < 15.0, "within the book's truncation: "
                + separation + "″ from 8h58m45.2s +13°46′06″");
        assertTrue(Math.abs(g.distanceKm() - 368409.7) < 50.0,
                "distance 368 409.7 km within the truncated series' reach: "
                        + g.distanceKm());
        double phaseAngle = (g.phaseAngleDegrees() - 69.0756) * 3600.0;
        assertTrue(Math.abs(phaseAngle) < 60.0, "phase angle 69.0756°, allowing"
                + " the two aberrations S-T-O carries and the book does not: "
                + phaseAngle + "″");
        assertTrue(Math.abs(g.illuminatedFraction() - 0.6786) < 0.0003,
                "illuminated fraction 0.6786 to the book's four decimals: "
                        + g.illuminatedFraction());
        assertTrue(Math.abs(g.brightLimbAngleDegrees() - 285.0) < 0.1,
                "bright-limb angle 285.0° to the book's tenth: "
                        + g.brightLimbAngleDegrees());
        assertTrue(g.trend() == SolarSystemService.Trend.WAXING
                && g.phase() == SolarSystemService.Phase.WAXING_GIBBOUS,
                "1992 April 12 was a waxing gibbous Moon: " + g.phase());
    }

    @Test
    void horizonsAgreesOnTheSameGeocentricInstantToTheExactTarget()
            throws IOException {
        String text = Files.readString(Path.of(
                "docs/studies/solar-system/horizons-moon/geocentric-meeus-47a-tt.txt"),
                StandardCharsets.UTF_8);
        String first = text.substring(text.indexOf("$$SOE") + 5,
                text.indexOf("$$EOE")).strip().split("\n")[0];
        // Quantities 1,2,10,13,20,23,24,31,43 for a geocentric request.
        String[] f = first.split(",");
        double raApp = Double.parseDouble(f[5].strip());
        double decApp = Double.parseDouble(f[6].strip());
        double illuminated = Double.parseDouble(f[7].strip());
        double deltaAu = Double.parseDouble(f[9].strip());
        double sto = Double.parseDouble(f[13].strip());
        MoonGeometry g = SolarSystemService.load().geocentricMoon(JD_TT);
        double separation = g.apparentOfDate().separationDegrees(
                new SkyPosition(raApp, decApp)) * 3600.0;
        assertTrue(separation <= 1.0 && separation > 0.0, "geocentric apparent"
                + " of date vs Horizons at 1992-04-12 00:00 TT: " + separation + "″");
        assertTrue(Math.abs(g.distanceKm() - deltaAu * 149_597_870.7) <= 1.0,
                "distance within a kilometre: " + g.distanceKm());
        assertTrue(Math.abs(g.phaseAngleDegrees() - sto) * 3600.0 <= 5.0,
                "S-T-O within 5″: " + (g.phaseAngleDegrees() - sto) * 3600.0);
        assertTrue(Math.abs(g.illuminatedFraction() * 100.0 - illuminated) <= 0.01,
                "illuminated within 0.01 percentage point");
    }
}
