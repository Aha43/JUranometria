package juranometria.app;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartRenderer;

/**
 * Writes the deterministic M31 reference image through the renderer, so the
 * chart can be judged and reviewed without a screen capture.
 */
public final class ChartImageMain {

    private ChartImageMain() {
    }

    public static void main(String[] args) throws IOException {
        File target = new File(args.length > 0 ? args[0] : "build/m31-chart.png");
        ChartScene scene = new ChartScene(
                new ChartViewport(M31Chart.CENTRE, M31Chart.FIELD_WIDTH_DEGREES, 900, 700),
                M31Chart.loadStars(), M31Chart.loadDeepSkyObjects(),
                M31Chart.TITLE, M31Chart.LIMITING_MAGNITUDE);
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        ImageIO.write(renderer.renderToImage(scene), "png", target);
        System.out.println("Wrote " + target.getPath());
    }
}
