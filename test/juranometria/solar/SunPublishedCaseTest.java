package juranometria.solar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.sky.Ecliptic;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A published case from outside the JPL family (issue #398, R-c).
 *
 * <p>Meeus, <i>Astronomical Algorithms</i>, example 25.a: the Sun on
 * 1992 October 13.0 TD, geocentric apparent place of date, by the
 * book's own low-precision method with a stated accuracy of 0.01°:
 * right ascension 13h 13m 31.4s (198.38083°), declination −7° 47′ 06″
 * (−7.78507°), apparent longitude 199.90895°, distance 0.99766 AU.
 * The service must land inside that stated precision - and Horizons,
 * asked for the same TT instant geocentrically, must land inside the
 * exact-era target, so the case exercises the same intermediate both
 * ways.
 */
class SunPublishedCaseTest {

    private static final double JD_TT = 2448908.5; // 1992-10-13.0 TD

    @Test
    void meeus25aIsReproducedWithinItsStatedPrecision() {
        SolarSystemService service = SolarSystemService.load();
        SkyPosition apparent = service.geocentricApparentOfDate(JD_TT);
        double published = 0.01 * 3600.0; // arcseconds
        double separation = apparent.separationDegrees(
                new SkyPosition(198.38083, -7.78507)) * 3600.0;
        assertTrue(separation < published, "within Meeus's 0.01°: "
                + separation + "″ from 13h13m31.4s −7°47′06″");
        double longitude = Ecliptic.toEcliptic(
                juranometria.sky.SkyFrame.toJ2000(apparent, JD_TT))
                .longitudeDegrees();
        // Meeus's longitude is of date; the atlas's ecliptic is J2000,
        // so compare through precession in longitude (~50.3″/yr).
        double precessed = longitude + 50.29 / 3600.0 * (1992.78 - 2000.0);
        assertTrue(Math.abs(precessed - 199.90895) * 3600.0 < published + 5.0,
                "apparent longitude within the published precision plus the"
                        + " rate's own rounding: " + precessed);
        double distanceAu = service.geocentricDistanceKm(JD_TT) / 149_597_870.7;
        assertTrue(Math.abs(distanceAu - 0.99766) < 1e-4,
                "distance 0.99766 AU to the book's four decimals: " + distanceAu);
    }

    @Test
    void horizonsAgreesOnTheSameGeocentricInstantToTheExactTarget()
            throws IOException {
        String text = Files.readString(Path.of(
                "docs/studies/solar-system/horizons/geocentric-meeus-25a-tt.txt"),
                StandardCharsets.UTF_8);
        String first = text.substring(text.indexOf("$$SOE") + 5,
                text.indexOf("$$EOE")).strip().split("\n")[0];
        String[] f = first.split(",");
        double raApp = Double.parseDouble(f[5].strip());
        double decApp = Double.parseDouble(f[6].strip());
        SkyPosition apparent = SolarSystemService.load()
                .geocentricApparentOfDate(JD_TT);
        double separation = apparent.separationDegrees(
                new SkyPosition(raApp, decApp)) * 3600.0;
        assertTrue(separation <= 1.0, "geocentric apparent of date vs"
                + " Horizons at 1992-10-13 00:00 TT: " + separation + "″");
        assertTrue(separation > 0.0, "two implementations");
    }
}
