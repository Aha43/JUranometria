package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartPalette;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A module's lines stop where the sky does (Sprint 32, issue #331,
 * step four).
 *
 * <p>The modules draw the observer's own geometry - the meridian, the
 * mathematical horizon, the ecliptic - and on a globe they were drawn
 * by the paper's rules rather than the sky's: curves and places inked
 * out past the limb, and one module's name was written entirely
 * outside the disc, where it named nothing.
 *
 * <p>The same two rules the chart itself now keeps, and for the same
 * reasons. <strong>Sky ink is clipped</strong> to the limb, because
 * geometry drawn beyond it is geometry the sphere does not have.
 * <strong>Names are not clipped</strong> but placed: a name cut by
 * the limb is a false name in exactly the way a name cut by the
 * paper's edge is, so the whole of it goes inside or none of it does.
 *
 * <p>Each of the three curves is checked on a page of its own, so
 * that one of them working cannot cover for another.
 */
class GlobeModuleInkTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText
                            .forLanguage("en"));

    private static final int SIDE_PX = 900;

    /**
     * How far past the limb a stroke may reach, as step one derived
     * it: the clip is exact geometry, so a line drawn on the limb has
     * its outer half cut rather than painted, leaving antialiasing
     * and the raster's own half-pixel of centre sampling.
     */
    private static final double ALLOWED_DEPTH_PX = 1.5;

    /**
     * The module study's own centres, chosen there so that each
     * module has something on the page. Taken rather than invented:
     * the study records that its first run, on centres picked
     * without that care, reported no module names at all - which
     * would let every check below pass while measuring nothing.
     */
    private static final Observer OSLO = new Observer(59.9, 10.7,
            Instant.parse("2026-03-20T21:33:00Z"));

    private static final SkyPosition ZENITH =
            new juranometria.sky.LocalSky(OSLO).zenith();

    private static final SkyPosition HORIZON_SOUTH =
            new SkyPosition(ZENITH.raDegrees(),
                    Math.max(-90.0, ZENITH.decDegrees() - 90.0));

    private static final SkyPosition ECLIPTIC_HIGH =
            new SkyPosition(90.0, 23.4);

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    @Test
    void noModuleInksBeyondTheLimbOnAnyOfItsPages() {
        for (SkyPosition centre : List.of(ZENITH, HORIZON_SOUTH,
                ECLIPTIC_HIGH, SAGITTARIUS)) {
            DrawnPage page = globe(centre);
            double deepest = deepestBeyondTheLimb(page);
            assertTrue(deepest <= ALLOWED_DEPTH_PX,
                    "a module inks " + deepest + " px past the limb on"
                            + " the page centred at " + centre.raDegrees()
                            + "/" + centre.decDegrees() + ", where a"
                            + " stroke drawn on the limb accounts for "
                            + ALLOWED_DEPTH_PX);
        }
    }

    @Test
    void thereWasReallyGeometryBeyondTheLimbToClip() {
        // The premise. A module whose curves never left the disc
        // would pass the test above while proving nothing, so this
        // establishes that these pages genuinely put module geometry
        // out past the limb - it is the clip that keeps it off the
        // paper, not the sky's good manners.
        int reaching = 0;
        for (SkyPosition centre : List.of(ZENITH, HORIZON_SOUTH,
                ECLIPTIC_HIGH, SAGITTARIUS)) {
            DrawnPage page = globe(centre);
            ViewportMapping mapping = new ViewportMapping(page);
            var region = mapping.regionFor(page.scene().viewport(),
                    page.projection());
            for (var owned : modules().collect()) {
                if (!(owned.geometry() instanceof
                        juranometria.module.OverlayContribution.GreatCircle
                                circle)) {
                    continue;
                }
                // The unclipped conic: does this circle's image leave
                // the disc on this page at all?
                var conic = page.projection().greatCircle(circle.pole());
                if (conic.isEmpty()) {
                    continue;
                }
                var curve = mapping.onPage(conic.get(), region);
                if (curve instanceof
                        juranometria.project.PlaneCurve.Straight) {
                    // A diameter's line continues across the paper.
                    reaching++;
                }
            }
        }
        assertTrue(reaching > 0,
                "at least one module curve is a line that would run"
                        + " off the disc if nothing stopped it: "
                        + reaching);
    }

    @Test
    void everyModuleNameIsWhollyInsideTheLimbOrNotWrittenAtAll() {
        int written = 0;
        for (SkyPosition centre : List.of(ZENITH, HORIZON_SOUTH,
                ECLIPTIC_HIGH, SAGITTARIUS)) {
            DrawnPage page = globe(centre);
            Ellipse2D disc = limbOf(page);
            for (ReferenceInk.NamePlacement placed
                    : ReferenceInk.namePlacements(page,
                            modules().collect(), ENGLISH,
                            java.util.List.of())) {
                written++;
                assertTrue(disc.contains(placed.box()),
                        placed.name() + " is written at "
                                + placed.box() + ", which is not"
                                + " wholly inside the limb on the page"
                                + " centred at " + centre.raDegrees()
                                + "/" + centre.decDegrees());
            }
        }
        assertTrue(written > 0,
                "the fixture writes module names at all: " + written);
    }

    @Test
    void aNameAnchoredOnTheLimbIsPulledInsideRatherThanLost() {
        // The case that made this necessary. A reference line on a
        // globe is cut by the limb, so the end of the drawn curve is
        // ON the limb, and a name inset from it lands outside the
        // sky. It is walked back along its own line until it fits.
        DrawnPage page = globe(HORIZON_SOUTH);
        Ellipse2D disc = limbOf(page);
        List<ReferenceInk.NamePlacement> placed =
                ReferenceInk.namePlacements(page, modules().collect(), ENGLISH, java.util.List.of());
        assertTrue(!placed.isEmpty(),
                "this page carries a reference name");
        for (ReferenceInk.NamePlacement each : placed) {
            assertTrue(disc.contains(each.box()), each.name()
                    + " is inside the disc");
        }
    }

    @Test
    void everyModuleNameIsNearestTheCurveItNames() {
        // Inside the limb is not the same as attached to the right
        // thing. A name walked towards the middle of the disc leaves
        // the ellipse it names at once, and on a page carrying a
        // meridian, a horizon and an ecliptic it can end up nearer a
        // line it says nothing about - a label that passes every
        // containment check and is still a lie (review of #331).
        //
        // So the reader's own question: of the reference lines on
        // this page, which is this name beside? It has to be its own,
        // and strictly.
        int judged = 0;
        double crowded = Double.MAX_VALUE;
        for (SkyPosition centre : List.of(ZENITH, HORIZON_SOUTH,
                ECLIPTIC_HIGH, SAGITTARIUS)) {
            DrawnPage page = globe(centre);
            var contributions = modules().collect();
            for (ReferenceInk.NamePlacement placed
                    : ReferenceInk.namePlacements(page, contributions,
                            ENGLISH, java.util.List.of())) {
                Double own = null;
                double nearestOther = Double.MAX_VALUE;
                String rival = "-";
                for (var owned : contributions) {
                    if (!(owned.geometry() instanceof
                            juranometria.module.OverlayContribution
                                    .GreatCircle circle)) {
                        continue;
                    }
                    double away = farFrom(page, circle, placed.box());
                    if (Double.isNaN(away)) {
                        continue;
                    }
                    if (circle.accessibleName().equals(placed.name())) {
                        own = away;
                    } else if (away < nearestOther) {
                        nearestOther = away;
                        rival = circle.accessibleName();
                    }
                }
                if (own == null || nearestOther == Double.MAX_VALUE) {
                    continue;
                }
                judged++;
                crowded = Math.min(crowded, nearestOther);
                assertTrue(own < nearestOther,
                        "\"" + placed.name() + "\" is written "
                                + Math.round(own) + " px from its own"
                                + " line and " + Math.round(nearestOther)
                                + " px from \"" + rival + "\", on the"
                                + " page centred at "
                                + centre.raDegrees() + "/"
                                + centre.decDegrees());
            }
        }
        assertTrue(judged >= 2,
                "the pages really do carry names with a rival line to"
                        + " be confused with: " + judged);
        // And the rivals are near enough that this discriminates. A
        // page whose other lines were half a disc away would satisfy
        // the check above while proving nothing; here the nearest
        // rival to a judged name is within a few tens of pixels, and
        // before the repair one of them was nearer than the name's
        // own line.
        assertTrue(crowded < 40.0,
                "a rival line is close enough for the question to be"
                        + " a real one: nearest is " + crowded + " px");
    }

    /** How far this box sits from a great circle's drawn run. */
    private static double farFrom(DrawnPage page,
            juranometria.module.OverlayContribution.GreatCircle circle,
            Rectangle2D box) {
        ViewportMapping mapping = new ViewportMapping(page);
        var runs = juranometria.project.GreatCirclePage.clip(
                page.projection(), mapping,
                mapping.regionFor(page.scene().viewport(),
                        page.projection()),
                circle.pole());
        double nearest = Double.NaN;
        for (var run : runs) {
            for (int at = 0; at <= 200; at++) {
                var point = run.at(at / 200.0);
                // From the box itself rather than from its middle: a
                // long name would otherwise be judged further from
                // its own line than a short one written in the same
                // place, which measures the wording and not the
                // placement.
                double dx = Math.max(0.0, Math.max(
                        box.getMinX() - point.x(),
                        point.x() - box.getMaxX()));
                double dy = Math.max(0.0, Math.max(
                        box.getMinY() - point.y(),
                        point.y() - box.getMaxY()));
                double away = Math.hypot(dx, dy);
                if (Double.isNaN(nearest) || away < nearest) {
                    nearest = away;
                }
            }
        }
        return nearest;
    }

    @Test
    void anOrdinaryPageIsUnchangedByAnyOfIt() {
        // The rule reaches the globe and nothing else: on a page
        // whose sky has no edge the sky IS the paper, so a module
        // draws exactly what it always drew.
        for (double field : new double[] {42.0, 120.0}) {
            ChartScene scene = Atlas.assembler().assemble(
                    new ChartViewState(SAGITTARIUS, field, 5.0),
                    SIDE_PX, SIDE_PX);
            DrawnPage page = DrawnPage.of(scene);
            ViewportMapping mapping = new ViewportMapping(page);
            assertTrue(!mapping.regionFor(scene.viewport(),
                            page.projection()).bounded(),
                    field + " degrees has no limb");
            Rectangle2D paper = new Rectangle2D.Double(0.0, 0.0,
                    SIDE_PX, SIDE_PX);
            for (ReferenceInk.NamePlacement placed
                    : ReferenceInk.namePlacements(page,
                            modules().collect(), ENGLISH,
                            java.util.List.of())) {
                assertTrue(paper.contains(placed.box()),
                        field + " degrees: " + placed.name()
                                + " is on the paper, as it always was");
            }
        }
    }

    /** How far past the limb the modules ink, in pixels of depth. */
    private static double deepestBeyondTheLimb(DrawnPage page) {
        BufferedImage canvas = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, SIDE_PX, SIDE_PX);
            ReferenceInk.paint(g, page, modules().collect(),
                    ChartPalette.WHITE_PAPER, ENGLISH,
                    java.util.List.of());
        } finally {
            g.dispose();
        }
        ViewportMapping mapping = new ViewportMapping(page);
        var region = mapping.regionFor(page.scene().viewport(),
                page.projection());
        double deepest = 0.0;
        for (int y = 0; y < SIDE_PX; y++) {
            for (int x = 0; x < SIDE_PX; x++) {
                if ((canvas.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) {
                    continue;
                }
                double from = Math.hypot(x + 0.5 - region.limbX(),
                        y + 0.5 - region.limbY());
                deepest = Math.max(deepest, from - region.limbRadius());
            }
        }
        return deepest;
    }

    private static Ellipse2D limbOf(DrawnPage page) {
        ViewportMapping mapping = new ViewportMapping(page);
        var region = mapping.regionFor(page.scene().viewport(),
                page.projection());
        return new Ellipse2D.Double(region.limbX() - region.limbRadius(),
                region.limbY() - region.limbRadius(),
                2.0 * region.limbRadius(), 2.0 * region.limbRadius());
    }

    private static DrawnPage globe(SkyPosition centre) {
        return DrawnPage.of(Atlas.assembler().assemble(
                new ChartViewState(centre, 180.0, 5.0),
                SIDE_PX, SIDE_PX));
    }

    /** The meridian and ecliptic modules, both showing. */
    private static OverlayRegistry modules() {
        OverlayRegistry registry = new OverlayRegistry();
        MeridianModule meridian = new MeridianModule(OSLO);
        meridian.showing(true, true, true);
        registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);
        return registry;
    }
}
