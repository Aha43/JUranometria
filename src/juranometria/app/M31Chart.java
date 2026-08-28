package juranometria.app;

import juranometria.catalog.BundledCatalogue;
import juranometria.chart.SkyPosition;
import juranometria.ui.SceneAssembler;

/**
 * The fixed M31 chart wiring: the bundled regional catalogue behind a
 * scene assembler centred on M31. The catalogue is loaded once, at first
 * use during application setup, never during painting.
 */
public final class M31Chart {

    public static final SkyPosition CENTRE = new SkyPosition(10.684708, 41.268750);
    public static final String TITLE = "M31 · Andromeda Galaxy region";

    private M31Chart() {
    }

    private static final class Holder {
        static final SceneAssembler ASSEMBLER =
                new SceneAssembler(BundledCatalogue.load(), CENTRE, TITLE);
    }

    /** The application's scene assembler over the bundled catalogue. */
    public static SceneAssembler assembler() {
        return Holder.ASSEMBLER;
    }
}
