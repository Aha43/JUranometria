package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstellationStudyMainTest {

    @Test
    void aSegmentCrossingThePageWithBothEndpointsOutsideStillDraws() {
        // PR #67 review: endpoint membership is not an intersection test.
        // This short segment cuts across the page's top-left corner; both
        // endpoints project off-page, and it is short enough that no
        // subdivision sample lands on-page either.
        SkyPosition centre = new SkyPosition(0.0, 0.0);
        ChartViewport viewport = new ChartViewport(centre, 24.0, 900, 700);
        GnomonicProjection projection = new GnomonicProjection(centre);
        ViewportMapping mapping = new ViewportMapping(viewport);

        SkyPosition from = new SkyPosition(12.078, 8.976);
        SkyPosition to = new SkyPosition(11.792, 9.270);
        PixelPoint fromPx = mapping.toPixel(projection.project(from).orElseThrow());
        PixelPoint toPx = mapping.toPixel(projection.project(to).orElseThrow());
        assertTrue(fromPx.x() < 0 || fromPx.y() < 0,
                "premise: the first endpoint is off-page (" + fromPx + ")");
        assertTrue(toPx.x() < 0 || toPx.y() < 0,
                "premise: the second endpoint is off-page (" + toPx + ")");
        assertTrue(ConstellationStudyMain.angularSeparationDegrees(from, to) < 0.5,
                "premise: a single piece, no on-page subdivision sample");

        BufferedImage image = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        int visible;
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 900, 700);
            g.setColor(Color.BLACK);
            visible = ConstellationStudyMain.drawSegments(g,
                    List.of(new ConstellationStudyMain.Segment("test", from, to)),
                    projection, mapping);
        } finally {
            g.dispose();
        }
        assertEquals(1, visible, "the crossing segment counts as visible");
        int ink = 0;
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 30; x++) {
                if (image.getRGB(x, y) != 0xFFFFFFFF) {
                    ink++;
                }
            }
        }
        assertTrue(ink > 0, "the crossing segment leaves ink in the corner");
    }
}
