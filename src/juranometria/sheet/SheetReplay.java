package juranometria.sheet;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * A recorded sheet played back into any {@link Graphics2D}
 * (Sprint 29, issue #286).
 *
 * <p>The raster formats need the sheet as pixels, and there are two
 * ways to get them: render the chart a second time into a bigger
 * grid, or replay what the first render did. The gate's prototype
 * took the first, and it is why every label on its PNG was a quarter
 * of the size it should have been - a second render at a different
 * extent is a different page, however similar it looks.
 *
 * <p>So this replays. The PNG a reader saves is the same ink, in the
 * same places, as the SVG and the PDF beside it, because all three
 * come from one recording of one render.
 */
final class SheetReplay {

    private SheetReplay() {
    }

    /** Draws the recording into a graphics already scaled and placed. */
    static void into(Graphics2D g, SheetRecorder recorder) {
        Shape original = g.getClip();
        // In the order it happened. Replaying every shape and then
        // every label puts a constellation name back on top of the
        // panel that was drawn to cover it (PR #292).
        for (SheetRecorder.Operation operation : recorder.operations()) {
            g.setClip(operation.clip() == null ? original
                    : operation.clip());
            if (operation instanceof SheetRecorder.Drawn drawn) {
                g.setColor(drawn.colour());
                if (drawn.filled()) {
                    g.fill(drawn.shape());
                } else {
                    SheetRecorder.BasicStrokeSpec stroke = drawn.stroke();
                    g.setStroke(new BasicStroke(stroke.width(),
                            stroke.cap(), stroke.join(),
                            stroke.miterLimit(), stroke.dash(),
                            stroke.dashPhase()));
                    g.draw(drawn.shape());
                }
            } else {
                SheetRecorder.Text text = (SheetRecorder.Text) operation;
                g.setColor(text.colour());
                g.setFont(text.font());
                g.drawString(text.text(), (float) text.x(),
                        (float) text.y());
            }
        }
        g.setClip(original);
    }
}
