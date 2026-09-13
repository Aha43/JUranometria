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
                            modules().collect())) {
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
                ReferenceInk.namePlacements(page, modules().collect());
        assertTrue(!placed.isEmpty(),
                "this page carries a reference name");
        for (ReferenceInk.NamePlacement each : placed) {
            assertTrue(disc.contains(each.box()), each.name()
                    + " is inside the disc");
        }
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
                            modules().collect())) {
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
                    ChartPalette.WHITE_PAPER);
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
