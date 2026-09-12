package juranometria.tool.globe;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;

/**
 * A globe page, assembled the way a reader's is (Sprint 32, issue
 * #329).
 *
 * <p>The gate that wrote these studies had no such thing. A hundred
 * and eighty degrees was not a rung, the projection enum had no
 * orthographic value, and {@code ChartViewState} refused both - so
 * every study went through a door in the assembler that took a
 * projection from outside and a title of its own
 * ({@code assembleForStudy}, removed with this issue).
 *
 * <p>Now the page a study measures is the page the application draws:
 * a state at the 180-degree rung, assembled by the production
 * assembler, drawn by the projection that rung belongs to. The
 * studies keep their own page sizes, because a study measuring ink by
 * band of the disc needs a square page and a reader's window is
 * whatever shape it is.
 */
final class GlobePage {

    private GlobePage() {
    }

    /** The production page at the globe rung. */
    static DrawnPage of(SkyPosition centre, double limitingMagnitude,
                        int widthPx, int heightPx) {
        return DrawnPage.of(Atlas.assembler().assemble(
                new ChartViewState(centre, 180.0, limitingMagnitude),
                widthPx, heightPx));
    }
}
