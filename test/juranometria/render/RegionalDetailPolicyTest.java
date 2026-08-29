package juranometria.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionalDetailPolicyTest {

    static final SkyPosition CENTRE = new SkyPosition(10.684708, 41.268750);

    /** At 24 degrees over 900 px, true size needs a ~9.7 arcmin major axis. */
    static final DeepSkyObject LARGE_FAINT = dso("NGC 7000", DsoType.NEBULA, 60.0, 3);
    static final DeepSkyObject TINY_FAINT = dso("PGC 1234", DsoType.GALAXY, 2.0, 3);
    static final DeepSkyObject TINY_MESSIER = dso("NGC 6341", DsoType.GLOBULAR_CLUSTER, 4.0, 1);
    static final DeepSkyObject LARGE_MESSIER = dso("NGC 224", DsoType.GALAXY, 177.8, 1);
    static final DeepSkyObject SYMBOL_LESS = dso("NGC 206", DsoType.STELLAR_ASSOCIATION, 4.0, 2);

    private static DeepSkyObject dso(String id, DsoType type,
                                     double majorArcmin, int priority) {
        return new DeepSkyObject(id, List.of(), type, CENTRE,
                majorArcmin, majorArcmin / 2.0, 35.0, 9.0, priority);
    }

    private static RegionalDetailPolicy policy(double fieldDegrees, String identity) {
        ChartViewport viewport = new ChartViewport(CENTRE, fieldDegrees, 900, 700);
        ChartScene scene = new ChartScene(viewport, List.of(), List.of(),
                "Policy test", 8.0, identity);
        return new RegionalDetailPolicy(
                scene, new ViewportMapping(viewport).pixelsPerPlaneUnit());
    }

    @Test
    void theClassicFieldsKeepTheReleasedBehaviourExactly() {
        // 18 degrees sits on the boundary and must stay classic.
        for (double field : new double[] {8.0, 12.0, 18.0}) {
            RegionalDetailPolicy classic = policy(field, null);
            for (DeepSkyObject dso : List.of(
                    LARGE_FAINT, TINY_FAINT, TINY_MESSIER, LARGE_MESSIER)) {
                assertTrue(classic.drawn(dso),
                        "every symbol-bearing object draws at " + field);
                assertTrue(classic.clampAllowed(dso),
                        "the practical-minimum clamp holds at " + field);
                assertEquals(dso.labelPriority() <= 1, classic.labelled(dso),
                        "Messier-only labels hold at " + field);
            }
            assertFalse(classic.drawn(SYMBOL_LESS));
            assertFalse(classic.labelled(SYMBOL_LESS));
        }
    }

    @Test
    void regionalFieldsDrawTrueSizeOnlyWithNoClampInflation() {
        RegionalDetailPolicy regional = policy(24.0, null);

        assertTrue(regional.drawsAtTrueSize(LARGE_FAINT));
        assertTrue(regional.drawn(LARGE_FAINT),
                "a genuinely large object stays on the regional page");
        assertFalse(regional.labelled(LARGE_FAINT),
                "true size alone does not earn a non-Messier label");

        assertFalse(regional.drawsAtTrueSize(TINY_FAINT));
        assertFalse(regional.drawn(TINY_FAINT),
                "sub-minimum faint objects vanish instead of becoming speckle");
        assertFalse(regional.clampAllowed(TINY_FAINT));
        assertFalse(regional.labelled(TINY_FAINT));
    }

    @Test
    void messierObjectsAlwaysDrawButOnlyTrueSizeOnesKeepLabels() {
        RegionalDetailPolicy regional = policy(36.0, null);

        assertFalse(regional.drawsAtTrueSize(TINY_MESSIER),
                "M92's 4-arcmin core is under the minimum at 36 degrees");
        assertTrue(regional.drawn(TINY_MESSIER), "Messier objects always draw");
        assertTrue(regional.clampAllowed(TINY_MESSIER),
                "the Messier symbol may be clamped up to stay visible");
        assertFalse(regional.labelled(TINY_MESSIER),
                "a clamped Messier symbol falls silent - the M32/M110 cure");

        assertTrue(regional.drawn(LARGE_MESSIER));
        assertTrue(regional.labelled(LARGE_MESSIER),
                "true-size Messier objects keep their names");
    }

    @Test
    void theSearchedTargetIsAlwaysDrawnAndLabelledWhenItHasASymbol() {
        RegionalDetailPolicy regional = policy(36.0, "PGC 1234");

        assertTrue(regional.drawn(TINY_FAINT),
                "the chart never titles itself by a drawable object it hides");
        assertTrue(regional.clampAllowed(TINY_FAINT));
        assertTrue(regional.labelled(TINY_FAINT),
                "the target is labelled whatever its priority or size");
    }

    @Test
    void aSymbolLessTargetIsNeverForcedIntoExistence() {
        RegionalDetailPolicy regional = policy(36.0, "NGC 206");
        assertFalse(regional.drawn(SYMBOL_LESS),
                "types without an established symbol stay undrawn");
        assertFalse(regional.labelled(SYMBOL_LESS));
    }

    @Test
    void theTargetExemptionMatchesTheStableIdentityOnly() {
        RegionalDetailPolicy regional = policy(36.0, "NGC 9999");
        assertFalse(regional.drawn(TINY_FAINT),
                "a different tiny object gains nothing from the exemption");
        assertFalse(regional.labelled(TINY_FAINT));
    }

    @Test
    void trueSizeUsesTheExactViewportScale() {
        // At 24 degrees over 900 px the exact scale is width/2 / tan(12deg)
        // pixels per plane unit; the 6 px minimum falls at ~9.744 arcmin.
        // The linear approximation (900 px / 24 deg) would put it at 9.6.
        RegionalDetailPolicy regional = policy(24.0, null);
        assertTrue(regional.drawsAtTrueSize(
                dso("JUST OVER", DsoType.GALAXY, 9.8, 3)));
        assertFalse(regional.drawsAtTrueSize(
                dso("JUST UNDER", DsoType.GALAXY, 9.7, 3)),
                "9.7 arcmin passes the linear approximation but not the exact scale");
    }
}
