package juranometria.tool.labels;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.Observer;

/**
 * The pages this gate is decided from (Sprint 31, issue #310).
 *
 * <p>The released Home page and the detail fields a reader spends most
 * of their time in; the three overview rungs at six centres the sky
 * treats differently; both grounds; the screen extent, a small window
 * and the two paper extents the sheet writes; a searched target, a
 * working selection, and the two reference layers a module can put on
 * the chart.
 *
 * <p>Every one is assembled by the production assembler from the
 * bundled pack, so a page here is a page a reader can reach.
 */
public final class StudyPages {

    private StudyPages() {
    }

    /** The screen the released studies use. */
    public static final int SCREEN_WIDE = 900;
    public static final int SCREEN_HIGH = 700;

    /** The chart area an A4 landscape sheet writes, in points. */
    private static final int A4_WIDE = 770;
    private static final int A4_HIGH = 523;

    /** The chart area a US Letter landscape sheet writes, in points. */
    private static final int LETTER_WIDE = 720;
    private static final int LETTER_HIGH = 540;

    /** A window a reader can actually shrink the atlas to. */
    private static final int SMALL_WIDE = 700;
    private static final int SMALL_HIGH = 500;

    /** An evening in Bergen, so the observer's lines are somewhere. */
    private static final Observer OBSERVER =
            new Observer(60.39, 5.32,
                    Instant.parse("2026-01-15T21:00:00Z"));

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);
    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);
    private static final SkyPosition CYGNUS = new SkyPosition(310.0, 40.0);
    private static final SkyPosition CRUX = new SkyPosition(187.0, -60.0);
    private static final SkyPosition POLE = new SkyPosition(0.0, 90.0);
    private static final SkyPosition SEAM = new SkyPosition(0.0, 0.0);

    /** Nunki, the named regression fixture's star. */
    public static final String NUNKI = "TYC 6868-1829-1";

    /** Namalsadirah, the bright mark its name runs into. */
    public static final String NAMALSADIRAH = "TYC 6867-2428-1";

    /** One page of the corpus, and why it is in it. */
    public record Look(Page page, String extent, String why) {
    }

    public static List<Look> corpus() {
        List<Look> looks = new ArrayList<>();

        // The released page a reader opens the atlas on.
        looks.add(screen("home", ChartViewState.DEFAULT,
                "the released Home page, and the control for every"
                        + " claim about not making it worse"));
        looks.add(screen("orion-08", state(ORION, 8.0),
                "a detail page: every label form the policy allows"));
        looks.add(screen("orion-18", state(ORION, 18.0),
                "the field where Latin Bayer letters stop"));
        looks.add(screen("orion-36", state(ORION, 36.0),
                "the widest detail page"));
        looks.add(screen("orion-42", state(ORION, 42.0),
                "the sheet page, gnomonic's last rung"));
        looks.add(screen("orion-60", state(ORION, 60.0),
                "the first overview rung"));
        looks.add(screen("orion-90", state(ORION, 90.0),
                "the winter sky, where the owner saw the defect"));
        looks.add(screen("orion-120", state(ORION, 120.0),
                "the widest page the atlas draws"));
        looks.add(screen("sagittarius-90", state(SAGITTARIUS, 90.0),
                "the Milky Way, densest labelling in the sky"));
        looks.add(screen("sagittarius-120", state(SAGITTARIUS, 120.0),
                "the named fixture's page: Nunki against Namalsadirah"));
        looks.add(screen("cygnus-90", state(CYGNUS, 90.0),
                "the summer triangle and the Milky Way's north"));
        looks.add(screen("crux-90", state(CRUX, 90.0),
                "the southern sky, small figures close together"));
        looks.add(screen("pole-120", state(POLE, 120.0),
                "the pole, where the graticule converges"));
        looks.add(screen("seam-120", state(SEAM, 120.0),
                "the right-ascension seam"));

        // The same sky, drawn where a reader has changed something.
        looks.add(new Look(new Page("orion-120-small",
                assemble(state(ORION, 120.0), SMALL_WIDE, SMALL_HIGH),
                ChartOptions.DEFAULTS, List.of()),
                SMALL_WIDE + "x" + SMALL_HIGH,
                "a reduced window: the same sky in less room"));
        looks.add(new Look(new Page("orion-42-a4",
                assemble(state(ORION, 42.0), A4_WIDE, A4_HIGH),
                ChartOptions.DEFAULTS, List.of()),
                "A4 " + A4_WIDE + "x" + A4_HIGH,
                "the A4 sheet's own chart extent"));
        looks.add(new Look(new Page("orion-42-letter",
                assemble(state(ORION, 42.0), LETTER_WIDE, LETTER_HIGH),
                ChartOptions.DEFAULTS, List.of()),
                "Letter " + LETTER_WIDE + "x" + LETTER_HIGH,
                "the US Letter sheet's own chart extent"));
        looks.add(new Look(new Page("orion-90-black",
                assemble(state(ORION, 90.0), SCREEN_WIDE, SCREEN_HIGH),
                ChartOptions.DEFAULTS.withPalette(ChartPalette.BLACK_SKY),
                List.of()),
                SCREEN_WIDE + "x" + SCREEN_HIGH,
                "black sky: the other ground the atlas ships"));
        looks.add(new Look(new Page("sagittarius-90-key",
                assemble(state(SAGITTARIUS, 90.0), SCREEN_WIDE, SCREEN_HIGH),
                Participant.Options.magnitudeKey(ChartOptions.DEFAULTS, true),
                List.of()),
                SCREEN_WIDE + "x" + SCREEN_HIGH,
                "with the magnitude key, the second piece of furniture"));

        // The reference layers a module puts on the chart.
        looks.add(new Look(new Page("sagittarius-90-ecliptic",
                assemble(state(SAGITTARIUS, 90.0), SCREEN_WIDE, SCREEN_HIGH),
                ChartOptions.DEFAULTS, ecliptic()),
                SCREEN_WIDE + "x" + SCREEN_HIGH,
                "the ecliptic module's line, its name and its landmarks"));
        looks.add(new Look(new Page("orion-90-observer",
                assemble(state(ORION, 90.0), SCREEN_WIDE, SCREEN_HIGH),
                ChartOptions.DEFAULTS, observerLines()),
                SCREEN_WIDE + "x" + SCREEN_HIGH,
                "the meridian, the mathematical horizon and the zenith"));

        // What the reader is doing, rather than what the sky is.
        ChartScene searched = assemble(new ChartViewState(
                new SkyPosition(283.816320, -26.296594), 90.0,
                ChartViewState.defaultMagnitudeFor(90.0),
                "Nunki · σ Sagittarii", NUNKI),
                SCREEN_WIDE, SCREEN_HIGH);
        looks.add(new Look(new Page("nunki-searched", searched,
                ChartOptions.DEFAULTS, List.of()),
                SCREEN_WIDE + "x" + SCREEN_HIGH,
                "a searched target: the one label that is guaranteed"));
        ChartScene marked = assemble(state(ORION, 18.0), SCREEN_WIDE,
                SCREEN_HIGH);
        looks.add(new Look(new Page("orion-18-selected", marked,
                ChartOptions.DEFAULTS, List.of())
                .withSelection(brightestFew(marked, 3)),
                SCREEN_WIDE + "x" + SCREEN_HIGH,
                "a working selection: three members wearing their rings"));
        return List.copyOf(looks);
    }

    private static Look screen(String slug, ChartViewState state,
                               String why) {
        return new Look(new Page(slug,
                assemble(state, SCREEN_WIDE, SCREEN_HIGH),
                ChartOptions.DEFAULTS, List.of()),
                SCREEN_WIDE + "x" + SCREEN_HIGH, why);
    }

    private static ChartViewState state(SkyPosition centre, double field) {
        return new ChartViewState(centre, field,
                ChartViewState.defaultMagnitudeFor(field));
    }

    public static ChartScene assemble(ChartViewState state, int wide,
                                      int high) {
        return Atlas.assembler().assemble(state, wide, high);
    }

    /** The brightest few identities on a page, as a reader might mark. */
    private static List<String> brightestFew(ChartScene scene, int many) {
        List<juranometria.chart.Star> stars =
                new ArrayList<>(scene.stars());
        stars.sort(java.util.Comparator.comparingDouble(
                juranometria.chart.Star::magnitude)
                .thenComparing(juranometria.chart.Star::id));
        List<String> chosen = new ArrayList<>();
        for (int at = 0; at < Math.min(many, stars.size()); at++) {
            chosen.add(stars.get(at).id());
        }
        return chosen;
    }

    /** The ecliptic module's own contribution, from the module. */
    private static List<OverlayRegistry.Owned> ecliptic() {
        EclipticModule module = new EclipticModule();
        module.showing(true);
        return owned("ecliptic", module.contributedGeometry());
    }

    /** The meridian module's own contribution, from the module. */
    private static List<OverlayRegistry.Owned> observerLines() {
        MeridianModule module = new MeridianModule(OBSERVER);
        module.showing(true, true, true);
        return owned("meridian", module.contributedGeometry());
    }

    private static List<OverlayRegistry.Owned> owned(String moduleId,
            List<OverlayContribution> geometry) {
        List<OverlayRegistry.Owned> all = new ArrayList<>();
        for (OverlayContribution one : geometry) {
            all.add(new OverlayRegistry.Owned(moduleId, one));
        }
        return List.copyOf(all);
    }
}
