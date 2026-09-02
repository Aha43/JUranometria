package juranometria.ui;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartRenderer;

/**
 * The released default page, as the machine running the test draws
 * it (issue #209, found by the display CI it added).
 *
 * <p>Three journeys ended by comparing their final page to
 * {@code docs/reference/m31-stars.png} byte for byte. On a display
 * runner they all failed at once - 50,650 bytes against the
 * reference's 53,501 - because text rasterization differs between
 * JDK builds and OS font stacks, and Linux does not draw the atlas's
 * labels the way macOS does.
 *
 * <p>The 1.0 contract says so plainly, and said so before these
 * assertions were written: equality with the committed reference is
 * <em>recorded per environment and not required across
 * environments</em>. The assertions required it. They only ever
 * passed because they never ran anywhere else - which is precisely
 * the hole the display job was added to close, and it found this on
 * its first run.
 *
 * <p>So the journeys now compare against <strong>the released page
 * as this machine draws it</strong>. That is the claim they were
 * always making - "the journey comes home to where every reader
 * begins" - without borrowing a promise about pixels that the
 * project deliberately does not make.
 */
final class ReleasedPage {

    private ReleasedPage() {
    }

    /** The reference's own geometry. */
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    /** The default page, drawn here, at the reference's geometry. */
    static byte[] here() throws Exception {
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(renderer.renderToImage(Atlas.assembler().assemble(
                ChartViewState.DEFAULT, WIDTH, HEIGHT)), "png", bytes);
        return bytes.toByteArray();
    }

    /** What the repository committed, on the maintainer's machines. */
    static byte[] committed() throws Exception {
        return Files.readAllBytes(Path.of("docs/reference/m31-stars.png"));
    }
}
