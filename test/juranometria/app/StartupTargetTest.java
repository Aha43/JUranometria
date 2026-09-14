package juranometria.app;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.ui.ChartViewController;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a page is about, and what the reader asked for (issue #341).
 *
 * <p>Owner testing found the opening page drawing M31's ellipse with
 * deep-sky objects switched off, and the ellipse disappearing after
 * the first small drag - which reads as settings applied late. They
 * are not late. The options are the reader's from the first frame;
 * what overrode them was the searched-target exemption, which lets a
 * target be drawn whatever the family switches say.
 *
 * <p>The opening page carried a target because a page's subject and
 * its target were one field. They are two things:
 *
 * <ul>
 *   <li>a <strong>subject</strong> is what the page is about, and is
 *       true however the switches stand - Home is the Andromeda
 *       region whether or not galaxies are drawn;</li>
 *   <li>a <strong>target</strong> is what the reader asked for in
 *       this session, and is the later, explicit request that the
 *       exemption exists to honour (#196).</li>
 * </ul>
 *
 * <p>The application opening at Andromeda is not a reader asking for
 * M31. Nothing restores a target, either: no centre, field or target
 * is persisted anywhere, so there is no route by which an earlier
 * session's target could return and be mistaken for this one's.
 */
class StartupTargetTest {

    private static final String M31 = "NGC 224";

    private static final int WIDE_PX = 900;

    private static final int HIGH_PX = 700;

    @Test
    void homeKeepsItsNameAndClaimsNoTarget() {
        assertEquals("M31 · Andromeda Galaxy region",
                ChartViewState.DEFAULT.targetLabel(),
                "the opening page still says what it shows");
        assertNull(ChartViewState.DEFAULT.targetIdentity(),
                "and does not claim the reader searched for it");
    }

    @Test
    void homeIsUnchangedForAReaderWhoDrawsDeepSkyObjects() {
        // The overwhelming majority of readers, and the released
        // page: with the families on, the exemption never decided
        // anything, so nothing may move.
        ChartScene home = assemble(ChartViewState.DEFAULT);
        assertEquals(7, deepSkyMarks(home, ChartOptions.DEFAULTS),
                "Home draws the deep-sky objects it always drew");
        assertTrue(namesOn(home, ChartOptions.DEFAULTS).contains("M 31"),
                "and names Andromeda: "
                        + namesOn(home, ChartOptions.DEFAULTS));
    }

    @Test
    void homeDrawsNeitherM31NorItsNameWhenDeepSkyIsOff() {
        // The defect, from the first frame rather than after a drag.
        ChartScene home = assemble(ChartViewState.DEFAULT);
        ChartOptions off = withoutDeepSky();
        assertEquals(0, deepSkyMarks(home, off),
                "a reader who switched deep-sky objects off is shown"
                        + " none of them on the page they open at");
        Set<String> names = namesOn(home, off);
        assertFalse(names.contains("M 31"),
                "nor M31's name: " + names);
    }

    @Test
    void theFirstDragChangesNeitherMarksNorNames() {
        // What made this read as "settings applied late": the target
        // used to be dropped by the first recentre, so the page
        // changed under the reader's hand. With no target to drop,
        // the page a reader opens is the page they keep.
        ChartOptions off = withoutDeepSky();
        ChartScene home = assemble(ChartViewState.DEFAULT);
        ChartScene afterADrag = assemble(
                ChartViewState.DEFAULT.recenteredAt(new SkyPosition(
                        ChartViewState.DEFAULT.centre().raDegrees() + 0.01,
                        ChartViewState.DEFAULT.centre().decDegrees())));

        // Which objects and which words, not how many of each. A
        // count is satisfied by a swap - M31 leaving as something
        // else arrives - and a swap is the same visible
        // discontinuity this issue exists to remove. Where they are
        // drawn may move, because the page moved; what is drawn may
        // not.
        //
        // Mutation-proved by exactly that swap: an exemption granted
        // to the object nearest the centre before the drag and to the
        // second nearest after it draws one mark on each page, so the
        // counts stay 1 and 1, and this fails on membership -
        // expected [NGC 224] but was [NGC 221].
        assertEquals(deepSkyOn(home, off), deepSkyOn(afterADrag, off),
                "the first drag draws the same objects, not merely as"
                        + " many of them");
        assertEquals(namesOn(home, off), namesOn(afterADrag, off),
                "and writes the same words");
    }

    @Test
    void resetViewComesBackToANamedHomeWithNoTarget() {
        // Home is reached again the way a reader reaches it, and is
        // the same page: named, and claiming nothing.
        ChartViewController navigation = new ChartViewController();
        navigation.recenter(new SkyPosition(266.0, -28.0), "Sagittarius",
                "NGC 6523");
        assertNotNull(navigation.state().targetIdentity(),
                "the search set a target");

        navigation.reset();
        assertEquals("M31 · Andromeda Galaxy region",
                navigation.state().targetLabel(),
                "Reset View comes home to a named page");
        assertNull(navigation.state().targetIdentity(),
                "without granting anything the exemption");
    }

    @Test
    void anExplicitSearchStillDrawsWhatWasAskedFor() {
        // The rule that must survive (#196): a reader who searches
        // for M31 is shown M31, whatever they hid earlier, because
        // the search is the later and more explicit request.
        ChartScene searched = assemble(new ChartViewState(
                ChartViewState.DEFAULT.centre(), 8.0, 8.0,
                "M31 · Andromeda Galaxy region", M31));
        ChartOptions off = withoutDeepSky();

        assertEquals(1, deepSkyMarks(searched, off),
                "the object the reader searched for is drawn");
        assertTrue(namesOn(searched, off).contains("M 31"),
                "and named: " + namesOn(searched, off));
    }

    @Test
    void aTargetCanOnlyComeFromAskingForOne() {
        // Stated so that a later change cannot quietly invent a
        // restored target. The reader's stores hold switches, an
        // observing place and an appearance; none holds where they
        // were looking. So the only states that carry an identity
        // are the ones something explicitly gave one to, and the
        // application's own starting state is not among them.
        //
        // Asked of the type rather than of the preference tree,
        // because reading a store to prove it does not hold
        // something would touch process-wide state to learn nothing.
        assertNull(ChartViewState.DEFAULT.targetIdentity(),
                "the state the application starts from carries no"
                        + " target");
        assertNull(new ChartViewState(new SkyPosition(83.0, 0.0), 8.0,
                        8.0).targetIdentity(),
                "nor does a page reached by moving to a place");
        assertEquals(M31, new ChartViewState(
                        ChartViewState.DEFAULT.centre(), 8.0, 8.0,
                        "M31 · Andromeda Galaxy region", M31)
                        .targetIdentity(),
                "only a state given one explicitly has one");
    }

    private static void assertNull(Object what, String why) {
        org.junit.jupiter.api.Assertions.assertNull(what, why);
    }

    private static ChartOptions withoutDeepSky() {
        ChartOptions d = ChartOptions.DEFAULTS;
        return new ChartOptions(false, d.deepSkyLabels(),
                d.constellationFigures(), d.constellationBoundaries(),
                d.constellationNames(), d.starNames(), d.bayerLetters(),
                d.flamsteedNumbers(), d.equatorialGrid(), d.titleBlock(),
                d.magnitudeKey(), d.galaxies(), d.openClusters(),
                d.globularClusters(), d.nebulae(), d.planetaryNebulae(),
                d.palette());
    }

    private static ChartScene assemble(ChartViewState state) {
        return Atlas.assembler().assemble(state, WIDE_PX, HIGH_PX);
    }

    private static int deepSkyMarks(ChartScene scene,
                                    ChartOptions options) {
        return deepSkyOn(scene, options).size();
    }

    /** Which deep-sky objects this page draws, by identity. */
    private static Set<String> deepSkyOn(ChartScene scene,
                                         ChartOptions options) {
        Set<String> drawn = new LinkedHashSet<>();
        for (ChartRenderer.DrawnMark mark
                : new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(scene, options)) {
            if (mark.kind() == ChartRenderer.DrawnMark.Kind.DEEP_SKY) {
                drawn.add(mark.deepSky().id());
            }
        }
        return drawn;
    }

    private static Set<String> namesOn(ChartScene scene,
                                       ChartOptions options) {
        Set<String> names = new LinkedHashSet<>();
        BufferedImage canvas = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            for (var placed : new ChartRenderer(StarSizePolicy.DEFAULT)
                    .textPlacements(ChartRenderer.TextMetrics.of(g),
                            scene, options)) {
                if (!placed.omitted()) {
                    names.add(placed.request().text());
                }
            }
        } finally {
            g.dispose();
        }
        return names;
    }
}
