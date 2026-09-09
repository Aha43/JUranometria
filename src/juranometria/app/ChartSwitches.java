package juranometria.app;

import juranometria.render.ChartOptions;
import juranometria.render.SymbolFamily;

/**
 * What a switch currently is, and how it is thrown (Sprint 31, issue
 * #312).
 *
 * <p>The keyboard reaches the same transitions the reader's own
 * controls reach - the chart-options controller for the chart's
 * layers, the modules' own seams for theirs - so that a keystroke
 * cannot mean something slightly different from the checkbox, or
 * persist somewhere else, or skip a dependency rule. There is one
 * path to the store and this is not a second one.
 */
public interface ChartSwitches {

    /** Whether this switch is on. */
    boolean on(String id);

    /**
     * Whether throwing it would show the reader anything.
     *
     * <p>False when the switch depends on a master that is off: the
     * families and the deep-sky labels ride the deep-sky master, and
     * the constellation names ride the figures. The chart-options
     * dialog greys those controls rather than letting them be
     * changed unseen, and the keyboard does the same - a keystroke
     * that changed stored state a reader cannot see is a keystroke
     * they cannot undo by looking.
     */
    boolean available(String id);

    /** Throws it, through the same transition the control uses. */
    void toggle(String id);

    /**
     * The atlas's own: the chart's options through their controller,
     * the modules through their own switches.
     *
     * @param options the controller the dialog and the chart share
     * @param ecliptic what the View menu's own item runs, and the
     *     state it runs on
     * @param observer the observer's lines, as Place and Time
     *     switches them
     */
    static ChartSwitches of(ChartOptionsController options,
                            Ecliptic ecliptic, ObserverLines observer) {
        if (options == null || ecliptic == null || observer == null) {
            throw new IllegalArgumentException(
                    "the keyboard needs the chart and both modules");
        }
        return new ChartSwitches() {

            @Override
            public boolean on(String id) {
                ChartOptions chart = options.options();
                return switch (id) {
                    case ChartKeys.DEEP_SKY -> chart.deepSkyObjects();
                    case "chart.galaxies" -> chart.galaxies();
                    case "chart.openClusters" -> chart.openClusters();
                    case "chart.globularClusters" ->
                            chart.globularClusters();
                    case "chart.nebulae" -> chart.nebulae();
                    case "chart.planetaryNebulae" ->
                            chart.planetaryNebulae();
                    case "chart.deepSkyLabels" -> chart.deepSkyLabels();
                    case "chart.starNames" -> chart.starNames();
                    case "chart.bayerLetters" -> chart.bayerLetters();
                    case "chart.flamsteedNumbers" ->
                            chart.flamsteedNumbers();
                    case ChartKeys.FIGURES -> chart.constellationFigures();
                    case "chart.constellationBoundaries" ->
                            chart.constellationBoundaries();
                    case "chart.constellationNames" ->
                            chart.constellationNames();
                    case "chart.equatorialGrid" -> chart.equatorialGrid();
                    case "chart.titleBlock" -> chart.titleBlock();
                    case "chart.magnitudeKey" -> chart.magnitudeKey();
                    case "chart.blackSky" -> chart.palette()
                            == juranometria.render.ChartPalette.BLACK_SKY;
                    case "module.ecliptic" -> ecliptic.showing();
                    case "module.meridian" -> observer.meridianShowing();
                    case "module.horizon" -> observer.horizonShowing();
                    default -> throw new IllegalArgumentException(
                            "no such switch: " + id);
                };
            }

            @Override
            public boolean available(String id) {
                ChartKeys.Toggle toggle = ChartKeys.toggle(id);
                if (toggle == null) {
                    throw new IllegalArgumentException(
                            "no such switch: " + id);
                }
                return toggle.dependsOn() == null
                        || on(toggle.dependsOn());
            }

            @Override
            public void toggle(String id) {
                if (!available(id)) {
                    // The dialog greys it; the keyboard declines it.
                    return;
                }
                switch (id) {
                    case "module.ecliptic" -> ecliptic.toggle();
                    case "module.meridian" -> observer.showing(
                            !observer.meridianShowing(),
                            observer.horizonShowing());
                    case "module.horizon" -> observer.showing(
                            observer.meridianShowing(),
                            !observer.horizonShowing());
                    default -> {
                        // Applied and confirmed together, because the
                        // palette has no OK and no Cancel. The dialog's
                        // checkbox previews and its OK button commits;
                        // a keystroke is the whole gesture, like the
                        // View menu's own Ecliptic item, which has
                        // saved the moment it was pressed since #274.
                        // The stored value is the same either way -
                        // what differs is that there is nothing here
                        // to take back, which is why the palette says
                        // what it did as it does it.
                        options.apply(withChart(options.options(), id,
                                !on(id)));
                        options.confirm();
                    }
                }
            }
        };
    }

    /** The ecliptic, as its own session switches it. */
    interface Ecliptic {
        boolean showing();

        void toggle();
    }

    /** The observer's lines, as Place and Time switches them. */
    interface ObserverLines {
        boolean meridianShowing();

        boolean horizonShowing();

        void showing(boolean meridian, boolean horizon);
    }

    /**
     * The reader's chart options with one layer switched.
     *
     * <p>Every component is read out, one is changed, and the record
     * is built once. Twelve constructor calls with one argument
     * different in each is twelve places for a component to be
     * dropped, which is the sort of mistake that shows up as a layer
     * quietly switching itself off.
     */
    private static ChartOptions withChart(ChartOptions from, String id,
                                          boolean to) {
        for (SymbolFamily family : SymbolFamily.values()) {
            if (id.equals(idOf(family))) {
                return from.withFamily(family, to);
            }
        }
        if (id.equals("chart.blackSky")) {
            return from.withPalette(to
                    ? juranometria.render.ChartPalette.BLACK_SKY
                    : juranometria.render.ChartPalette.WHITE_PAPER);
        }
        boolean deepSky = from.deepSkyObjects();
        boolean deepSkyLabels = from.deepSkyLabels();
        boolean figures = from.constellationFigures();
        boolean boundaries = from.constellationBoundaries();
        boolean names = from.constellationNames();
        boolean starNames = from.starNames();
        boolean bayer = from.bayerLetters();
        boolean flamsteed = from.flamsteedNumbers();
        boolean grid = from.equatorialGrid();
        boolean titleBlock = from.titleBlock();
        boolean magnitudeKey = from.magnitudeKey();
        switch (id) {
            case ChartKeys.DEEP_SKY -> deepSky = to;
            case "chart.deepSkyLabels" -> deepSkyLabels = to;
            case ChartKeys.FIGURES -> figures = to;
            case "chart.constellationBoundaries" -> boundaries = to;
            case "chart.constellationNames" -> names = to;
            case "chart.starNames" -> starNames = to;
            case "chart.bayerLetters" -> bayer = to;
            case "chart.flamsteedNumbers" -> flamsteed = to;
            case "chart.equatorialGrid" -> grid = to;
            case "chart.titleBlock" -> titleBlock = to;
            case "chart.magnitudeKey" -> magnitudeKey = to;
            default -> throw new IllegalArgumentException(
                    "no such switch: " + id);
        }
        return new ChartOptions(deepSky, deepSkyLabels, figures,
                boundaries, names, starNames, bayer, flamsteed, grid,
                titleBlock, magnitudeKey, from.galaxies(),
                from.openClusters(), from.globularClusters(),
                from.nebulae(), from.planetaryNebulae(), from.palette());
    }

    private static String idOf(SymbolFamily family) {
        return switch (family) {
            case GALAXIES -> "chart.galaxies";
            case OPEN_CLUSTERS -> "chart.openClusters";
            case GLOBULAR_CLUSTERS -> "chart.globularClusters";
            case NEBULAE -> "chart.nebulae";
            case PLANETARY_NEBULAE -> "chart.planetaryNebulae";
        };
    }
}
