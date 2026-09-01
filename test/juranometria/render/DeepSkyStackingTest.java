package juranometria.render;

import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cartographic stacking rule (Sprint 23, issue #201).
 *
 * <p>The atlas's founding page names three galaxies and showed two.
 * Catalogue order is storage order: the bundled rows reach the
 * default page as NGC 205, NGC 221, NGC 224, so M 31's opaque
 * 178-arcminute disc was painted last and swallowed M 32 whole. Its
 * label still drew, because labels are a later pass, leaving a
 * reader a name with no mark to attach it to.
 *
 * <p>Every fact here is taken from production:
 * {@link ChartRenderer#drawnMarks} publishes both the placements and
 * the order they are painted in, the application's own assembler
 * builds the page, and how much ink a mark leaves is measured by
 * rendering the same page again without it.
 */
class DeepSkyStackingTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);
    private static final ChartOptions OPTIONS = ChartOptions.DEFAULTS;

    private static final String M31 = "NGC 224";
    private static final String M32 = "NGC 221";
    private static final String M110 = "NGC 205";

    /** The released default page, exactly as the application opens it. */
    private static ChartScene defaultPage() {
        return Atlas.assembler().assemble(ChartViewState.DEFAULT, 900, 700);
    }

    private static List<ChartRenderer.DrawnMark> deepSky(ChartScene scene) {
        List<ChartRenderer.DrawnMark> out = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark
                : RENDERER.drawnMarks(scene, OPTIONS)) {
            if (mark.kind() == ChartRenderer.DrawnMark.Kind.DEEP_SKY) {
                out.add(mark);
            }
        }
        return out;
    }

    private static ChartRenderer.DrawnMark named(
            List<ChartRenderer.DrawnMark> marks, String id) {
        for (ChartRenderer.DrawnMark mark : marks) {
            if (id.equals(mark.deepSky().id())) {
                return mark;
            }
        }
        return null;
    }

    private static int paintedAt(List<ChartRenderer.DrawnMark> marks,
                                 String id) {
        for (int i = 0; i < marks.size(); i++) {
            if (id.equals(marks.get(i).deepSky().id())) {
                return i;
            }
        }
        return -1;
    }

    /** The same page without one object, so the rest is unchanged. */
    private static ChartScene without(ChartScene scene, String id) {
        List<DeepSkyObject> kept = new ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (!id.equals(dso.id())) {
                kept.add(dso);
            }
        }
        return new ChartScene(scene.viewport(), scene.stars(), kept,
                scene.title(), scene.limitingMagnitude(),
                scene.targetIdentity(), scene.geography());
    }

    /**
     * The pixels this object is responsible for on the finished page:
     * render it, render the page without it, count what changed.
     * Nothing is inferred from geometry - the drawing answers.
     */
    private static int survivingInk(ChartScene scene, String id) {
        BufferedImage with = RENDERER.renderToImage(scene);
        BufferedImage none = RENDERER.renderToImage(without(scene, id));
        int changed = 0;
        for (int y = 0; y < with.getHeight(); y++) {
            for (int x = 0; x < with.getWidth(); x++) {
                if (with.getRGB(x, y) != none.getRGB(x, y)) {
                    changed++;
                }
            }
        }
        return changed;
    }

    @Test
    void theDefectIsRealAndCatalogueOrderIsWhatCausedIt() {
        ChartScene scene = defaultPage();
        List<ChartRenderer.DrawnMark> marks = deepSky(scene);
        ChartRenderer.DrawnMark m31 = named(marks, M31);
        ChartRenderer.DrawnMark m32 = named(marks, M32);
        assertNotNull(m31, "M 31 is on the default page");
        assertNotNull(m32, "M 32 is on the default page");

        // 1. The scene really does arrive in storage order, with the
        //    big galaxy last. Without this the defect has no cause.
        List<String> stored = new ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            stored.add(dso.id());
        }
        assertTrue(stored.indexOf(M32) < stored.indexOf(M31),
                "the premise: the catalogue stores M 32 before M 31, so"
                        + " painting in storage order paints M 31 over"
                        + " it - " + stored);

        // 2. M 31's drawn disc contains the whole of M 32's ellipse.
        //    Not "overlaps": every point of the smaller mark is
        //    inside the larger one, so no part of it could survive.
        Area escaping = new Area(m32.outline());
        escaping.subtract(new Area(m31.outline()));
        assertTrue(escaping.isEmpty(),
                "the premise: M 31's drawn outline entirely contains"
                        + " M 32's, so whichever is painted second"
                        + " decides whether M 32 exists");

        // 3. And that disc is opaque. Rendered, not assumed: a page
        //    holding only M 31 has no paper left at M 32's centre.
        ChartScene onlyM31 = keepOnly(scene, M31);
        BufferedImage image = RENDERER.renderToImage(onlyM31);
        int rgb = image.getRGB((int) Math.round(m32.centre().x()),
                (int) Math.round(m32.centre().y())) & 0xffffff;
        assertFalse(rgb == 0xffffff,
                "the premise: the galaxy symbol fills its interior, so"
                        + " painting it last erases what is under it");
    }

    private static ChartScene keepOnly(ChartScene scene, String id) {
        List<DeepSkyObject> kept = new ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (id.equals(dso.id())) {
                kept.add(dso);
            }
        }
        return new ChartScene(scene.viewport(), List.of(), kept,
                scene.title(), scene.limitingMagnitude(), null,
                scene.geography());
    }

    @Test
    void allThreeAndromedaGalaxiesLeaveInkOnTheDefaultPage() {
        ChartScene scene = defaultPage();
        List<ChartRenderer.DrawnMark> marks = deepSky(scene);

        // The largest goes behind, so its companions survive it.
        assertTrue(paintedAt(marks, M31) < paintedAt(marks, M32),
                "M 31 is painted before M 32");
        assertTrue(paintedAt(marks, M31) < paintedAt(marks, M110),
                "M 31 is painted before M 110");

        for (String id : List.of(M31, M32, M110)) {
            assertTrue(survivingInk(scene, id) > 0,
                    id + " must leave identifiable ink on the page:"
                            + " a label with no mark is what this"
                            + " issue is about");
        }
    }

    @Test
    void eachOfTheThreeCanBeReachedByPointing() {
        ChartScene scene = defaultPage();
        List<ChartRenderer.DrawnMark> marks = deepSky(scene);
        ChartHitTest hits = new ChartHitTest(RENDERER);

        for (String id : List.of(M31, M32, M110)) {
            ChartRenderer.DrawnMark mark = named(marks, id);
            assertNotNull(mark, id);
            ChartHitTest.Hit hit = hits.at(scene, OPTIONS,
                    mark.centre().x(), mark.centre().y());
            assertNotNull(hit, "pointing at " + id + " is on the paper");
            List<String> offered = new ArrayList<>();
            for (juranometria.chart.Selection.Object candidate
                    : hit.candidates()) {
                offered.add(candidate.catalogueId());
            }
            assertTrue(offered.contains(id),
                    "pointing at " + id + " must offer it: " + offered);
            // Standing on the small companion answers with the
            // companion, not the disc it sits on.
            if (!id.equals(M31)) {
                assertEquals(id, offered.get(0),
                        "the tighter mark answers first: " + offered);
            }
        }
    }

    @Test
    void theOrderDoesNotDependOnHowTheCatalogueArrives() {
        ChartScene scene = defaultPage();
        List<DeepSkyObject> reversed =
                new ArrayList<>(scene.deepSkyObjects());
        java.util.Collections.reverse(reversed);
        ChartScene other = new ChartScene(scene.viewport(), scene.stars(),
                reversed, scene.title(), scene.limitingMagnitude(),
                scene.targetIdentity(), scene.geography());

        // The premise: the input really is in a different order.
        assertFalse(scene.deepSkyObjects().equals(reversed),
                "the premise: the reversed scene is not the original");

        List<String> a = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark : deepSky(scene)) {
            a.add(mark.deepSky().id());
        }
        List<String> b = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark : deepSky(other)) {
            b.add(mark.deepSky().id());
        }
        assertEquals(a, b, "the painting order is the chart's, not the"
                + " catalogue's: reversing the input must change"
                + " nothing");

        BufferedImage first = RENDERER.renderToImage(scene);
        BufferedImage second = RENDERER.renderToImage(other);
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) {
                    throw new AssertionError("the page differs at " + x
                            + "," + y + " when the catalogue arrives"
                            + " reversed");
                }
            }
        }
    }

    @Test
    void turningAnEllipseDoesNotChangeWhatGoesBehindWhat() {
        // A bounding box grows as an ellipse turns: a long thin galaxy
        // at 45 degrees bounds a bigger square than the same galaxy
        // upright, though it covers exactly as much paper. Ordering by
        // one would let a companion surface or submerge as its
        // neighbour rotates. Two galaxies, the first long and thin and
        // the second round, sized so the box and the area disagree.
        SkyPosition centre = new SkyPosition(10.0, 20.0);
        DeepSkyObject thin = galaxy("A-THIN", centre, 40.0, 4.0, 45.0);
        DeepSkyObject round = galaxy("B-ROUND",
                new SkyPosition(10.05, 20.0), 22.0, 22.0, 0.0);

        List<String> upright = orderOf(centre, galaxy("A-THIN", centre,
                40.0, 4.0, 0.0), round);
        List<String> turned = orderOf(centre, thin, round);

        assertEquals(upright, turned,
                "the stacking order must not turn with the ellipse");
        // And it is the genuinely larger footprint that goes behind:
        // pi/4 * 22 * 22 = 380 square arcmin beats pi/4 * 40 * 4 = 126.
        assertEquals("B-ROUND", turned.get(0),
                "the larger painted area goes behind, whatever the"
                        + " bounding boxes say: " + turned);
    }

    private static List<String> orderOf(SkyPosition centre,
                                        DeepSkyObject... objects) {
        ChartScene scene = new ChartScene(
                new juranometria.chart.ChartViewport(centre, 2.0, 900, 700),
                List.of(), List.of(objects), "stacking", 8.0);
        List<String> ids = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark : deepSky(scene)) {
            ids.add(mark.deepSky().id());
        }
        return ids;
    }

    private static DeepSkyObject galaxy(String id, SkyPosition at,
                                        double major, double minor,
                                        double positionAngle) {
        return new DeepSkyObject(id, List.of(),
                juranometria.chart.DsoType.GALAXY, at, major, minor,
                positionAngle, 9.0, 1);
    }
}
