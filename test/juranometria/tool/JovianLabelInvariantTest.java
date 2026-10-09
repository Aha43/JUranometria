package juranometria.tool;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewState;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The owner's ruling 6 on #482, held on whatever platform runs it: one
 * crowded pair never suppresses every moon label. How many labels are
 * placed, moved or refused depends on this desktop's real font metrics,
 * which production uses, so the totals are a platform record; this
 * invariant is not.
 */
class JovianLabelInvariantTest {

    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-12-11T22:45:00Z"));
    private static final double[] FIELDS = {12.0, 8.0, 6.0, 4.0, 3.0, 2.0};

    private static JovianSystemService service;
    private static FontMetrics font;

    @BeforeAll
    static void load() {
        service = JovianSystemService.load();
        Graphics2D probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            font = ChartRenderer.TextMetrics.of(probe).labels();
        } finally {
            probe.dispose();
        }
    }

    @Test
    void noConfigurationAtAnyFieldLosesEveryMoonLabel() {
        List<Double> fields = new ArrayList<>();
        for (double f : FIELDS) {
            fields.add(f);
        }
        // the normal minimum is read from the chart, never assumed to be 1°
        fields.add(ChartViewState.normalMinimumFieldDegrees());
        List<String> lost = new ArrayList<>();
        int checked = 0;
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plus(Duration.ofHours(6))) {
            JovianSystemService.Configuration c = service.observeMoons(OSLO.at(t));
            for (double field : fields) {
                checked++;
                if (JovianCartographyStudyMain.losesEveryMoonLabel(field, c, font)) {
                    lost.add(String.format(Locale.ROOT, "%s at %.0f°", t, field));
                }
            }
        }
        assertTrue(checked == 1460 * fields.size(), "the whole 2026 sweep was checked: " + checked);
        assertTrue(lost.isEmpty(), "ruling 6: no configuration loses every moon label, with the label font "
                + JovianCartographyStudyMain.fontIdentity(font) + "; lost: " + lost);
    }
}
