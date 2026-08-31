package juranometria.app;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import juranometria.render.ChartOptions;
import juranometria.ui.ChartViewController;
import juranometria.chart.ChartViewState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 1.0 contract's "stable behaviour" list, bound to the code that
 * implements it (issue #146).
 *
 * <p>Everything asserted here is a published promise: changing it
 * after 1.0 is a compatibility decision, not a refactor, and this
 * test is where that conversation starts. It deliberately asserts
 * the contract's *numbers* - the field sequence, the magnitude
 * range, the default view - rather than deriving them from the
 * code, because a test that reads its expectations from the
 * implementation cannot notice the implementation changing.
 *
 * <p>What it does not do is restate the feature journeys: those
 * already prove the behaviour works. This proves it is still the
 * behaviour that was promised.
 */
class OneZeroContractTest {

    @Test
    void theFieldSequenceIsTheOnePromised() {
        // "the discrete field sequence 36, 24, 18, 12, 8, 6, 4, 3,
        // 2, 1 degrees" - walked through the real controller, from
        // the widest bound to the narrowest.
        ChartViewController navigation =
                new ChartViewController(Atlas.assembler()::fits);
        while (navigation.state().fieldWidthDegrees() < 36.0) {
            navigation.zoomOut();
        }
        List<Double> sequence = new ArrayList<>();
        sequence.add(navigation.state().fieldWidthDegrees());
        for (int step = 0; step < 20; step++) {
            double before = navigation.state().fieldWidthDegrees();
            navigation.zoomIn();
            double after = navigation.state().fieldWidthDegrees();
            if (after == before) {
                break;
            }
            sequence.add(after);
        }

        assertEquals(List.of(36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0, 3.0,
                        2.0, 1.0), sequence,
                "the promised field sequence, walked end to end");
    }

    @Test
    void theMagnitudeRangeIsTheOnePromised() {
        // "magnitude limit 4.0-8.0 in whole steps", and the controls
        // stop at both bounds rather than wrapping or drifting.
        ChartViewController navigation =
                new ChartViewController(Atlas.assembler()::fits);
        List<Double> limits = new ArrayList<>();
        for (int step = 0; step < 12; step++) {
            navigation.decreaseMagnitudeLimit();
        }
        limits.add(navigation.state().limitingMagnitude());
        for (int step = 0; step < 12; step++) {
            double before = navigation.state().limitingMagnitude();
            navigation.increaseMagnitudeLimit();
            double after = navigation.state().limitingMagnitude();
            if (after == before) {
                break;
            }
            limits.add(after);
        }

        assertEquals(List.of(4.0, 5.0, 6.0, 7.0, 8.0), limits,
                "whole steps from the faintest bound to the brightest");
    }

    @Test
    void theDefaultViewIsTheOnePromised() {
        // "the M31 region, 8 degree field, stars to V 8.0, all chart
        // layers on (including all three star-identifier layers and
        // the equatorial grid)".
        ChartViewState home =
                new ChartViewController(Atlas.assembler()::fits).state();

        assertEquals(8.0, home.fieldWidthDegrees(), "the 8-degree page");
        assertEquals(8.0, home.limitingMagnitude(), "stars to V 8.0");
        assertTrue(home.targetLabel() != null
                        && home.targetLabel().contains("M31"),
                "the M31 region: " + home.targetLabel());

        ChartOptions defaults = ChartOptions.DEFAULTS;
        assertTrue(defaults.deepSkyObjects() && defaults.deepSkyLabels()
                        && defaults.constellationFigures()
                        && defaults.constellationBoundaries()
                        && defaults.constellationNames()
                        && defaults.starNames() && defaults.bayerLetters()
                        && defaults.flamsteedNumbers()
                        && defaults.equatorialGrid(),
                "every layer on: " + defaults);
    }

    @Test
    void thePreferenceSurfaceIsTheOnePromised() throws Exception {
        // "the JDK preferences node juranometria: appearance and the
        // nine chart.* option keys", plus the legacy key older
        // releases wrote. The node NAME is part of the promise: an
        // upgrade must find the settings the last version saved.
        String source = Files.readString(
                Path.of("src/juranometria/app/ChartOptionsStore.java"));
        assertTrue(source.contains("node(\"juranometria\")"),
                "the promised preference node");

        for (String key : List.of("chart.deepSkyObjects",
                "chart.deepSkyLabels", "chart.constellationFigures",
                "chart.constellationBoundaries", "chart.constellationNames",
                "chart.starNames", "chart.bayerLetters",
                "chart.flamsteedNumbers", "chart.equatorialGrid")) {
            assertTrue(source.contains('"' + key + '"'),
                    "the contract names " + key);
        }
        assertTrue(source.contains("chart.starLabels"),
                "and the legacy key an older store may carry");
        assertTrue(Files.readString(
                        Path.of("src/juranometria/app/AppearanceStore.java"))
                        .contains("node(\"juranometria\")"),
                "appearance shares the promised node");
    }

    @Test
    void theVersionUnderReleaseAgreesEverywhereItIsStated()
            throws Exception {
        // Tag, VERSION, changelog, and the version the application
        // reports are one number. The packaged image adds its own
        // build-info.txt to this agreement at build time; here the
        // repository's own three are checked.
        String declared = Files.readString(Path.of("VERSION")).trim();

        assertEquals(declared, AppInfo.version(),
                "the application reports the declared version");
        assertTrue(Files.readString(Path.of("CHANGELOG.md"))
                        .contains("## [" + declared + "] - "),
                "and the changelog carries its dated section");
    }
}
