package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.tool.ApplicationMark.Candidate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The candidate application marks (Sprint 23, issue #200).
 *
 * <p>The gate's claims, held here so the decision cannot quietly
 * stop being true: one geometry draws every size, every size is a
 * drawing rather than a resampling, and the mark the decision
 * recommends keeps all of its stars down to 16 px.
 */
class ApplicationMarkTest {

    private static final int[] SIZES =
            {16, 24, 32, 48, 64, 128, 256, 512, 1024};

    private static BufferedImage render(Candidate candidate, int size,
                                        int omitDot) {
        BufferedImage image = new BufferedImage(size, size,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            ApplicationMark.paint(g, candidate, size, omitDot);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static long inkPixels(BufferedImage image) {
        long ink = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) >= 128 && (argb & 0xffffff) != 0xffffff) {
                    ink++;
                }
            }
        }
        return ink;
    }

    @Test
    void everyCandidateDrawsAtEverySizeADesktopAsksFor() {
        for (Candidate candidate : Candidate.values()) {
            for (int size : SIZES) {
                BufferedImage image = render(candidate, size, -1);
                assertEquals(size, image.getWidth(), candidate + " at " + size);
                assertTrue(inkPixels(image) > 0,
                        candidate + " must be a drawing at " + size
                                + " px, not an empty card");
            }
        }
    }

    @Test
    void theRecommendedMarkKeepsEveryStarDownToSixteenPixels() {
        // The decision rests on this: a dot that changes nothing when
        // it is left out is a dot the reader does not have. Measured
        // the same way the study measures it, so the document and the
        // gate cannot disagree.
        for (int size : new int[] {16, 24, 32, 48}) {
            BufferedImage all = render(Candidate.RIFT, size, -1);
            List<String> lost = new ArrayList<>();
            for (int i = 0; i < ApplicationMark.dots(Candidate.RIFT).size();
                    i++) {
                if (!differs(all, render(Candidate.RIFT, size, i))) {
                    lost.add("dot " + i);
                }
            }
            assertEquals(List.of(), lost,
                    "Rift keeps every star at " + size + " px: " + lost);
        }
    }

    @Test
    void aSizeIsADrawingRatherThanAResampling() {
        // 16 px composed from the geometry is not the same picture as
        // 1024 px squeezed into 16, and the whole point of a coded
        // mark is that the small sizes are drawn rather than
        // shrunk. If these ever matched, the geometry would have
        // stopped being scale-aware.
        BufferedImage composed = render(Candidate.RIFT, 16, -1);
        BufferedImage shrunk = new BufferedImage(16, 16,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = shrunk.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(render(Candidate.RIFT, 1024, -1), 0, 0, 16, 16, null);
        } finally {
            g.dispose();
        }
        assertTrue(differs(composed, shrunk),
                "the 16 px mark is composed at 16 px");
    }

    @Test
    void theSameGeometryDrawnTwiceIsTheSameBytes() {
        // Every committed export comes from this; a generator that
        // wandered would make the packaging in #202 unverifiable.
        for (Candidate candidate : Candidate.values()) {
            assertFalse(differs(render(candidate, 256, -1),
                            render(candidate, 256, -1)),
                    candidate + " draws the same picture twice");
        }
    }

    @Test
    void theControlIsTheLeastDistinctive() {
        // Field is in the study to be beaten, and the decision says
        // it is the least unlike a bare card. If that ever stopped
        // being true, the recommendation would need revisiting.
        double field = unlikeBareCard(Candidate.FIELD);
        for (Candidate candidate : Candidate.values()) {
            if (candidate != Candidate.FIELD) {
                assertTrue(unlikeBareCard(candidate) > field,
                        candidate + " must differ from a bare card more"
                                + " than the generic control does");
            }
        }
    }

    private static double unlikeBareCard(Candidate candidate) {
        BufferedImage mark = render(candidate, 16, -1);
        BufferedImage bare = new BufferedImage(16, 16,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bare.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(ApplicationMark.PAPER);
            g.fill(ApplicationMark.card(16));
            g.setColor(ApplicationMark.FRAME);
            g.draw(ApplicationMark.card(16));
        } finally {
            g.dispose();
        }
        long counted = 0;
        long different = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if ((mark.getRGB(x, y) >>> 24) < 128) {
                    continue;
                }
                counted++;
                if (mark.getRGB(x, y) != bare.getRGB(x, y)) {
                    different++;
                }
            }
        }
        return counted == 0 ? 0.0 : (double) different / counted;
    }

    private static boolean differs(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }
}
