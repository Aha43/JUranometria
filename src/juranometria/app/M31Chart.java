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

    /**
     * The bundled data's cone radius, matching the import region stated in
     * src/resources/catalog/m31/PROVENANCE.md; a test guards that every
     * bundled row actually lies within it.
     */
    public static final double DATA_COVERAGE_RADIUS_DEGREES = 10.0;

    private M31Chart() {
    }

    private static final class Holder {
        static final SceneAssembler ASSEMBLER = new SceneAssembler(
                BundledCatalogue.load(), CENTRE, TITLE, DATA_COVERAGE_RADIUS_DEGREES);
    }

    /** The application's scene assembler over the bundled catalogue. */
    public static SceneAssembler assembler() {
        return Holder.ASSEMBLER;
    }
}
