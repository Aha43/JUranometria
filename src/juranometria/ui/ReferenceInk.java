package juranometria.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import juranometria.chart.ChartScene;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.project.CurveRun;
import juranometria.project.GreatCirclePage;
import juranometria.project.PageBasis;
import juranometria.project.PageRegion;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartRenderer;
import juranometria.render.EquatorialGrid;

/**
 * How the chart inks a module's reference geometry (Sprint 25,
 * issue #227).
 *
 * <p>The module says <em>where</em>, <em>what for</em> and
 * <em>what it is</em>; the chart decides what that looks like, where
 * it sits in the stack and whether it is drawn at all. Nothing here
 * knows what a meridian or a horizon is: it is given poles, points
 * and names.
 *
 * <p>The ink was chosen by drawing it over real pages in both themes
 * (docs/decisions/place-and-time.md):
 *
 * <ul>
 *   <li>a line across the sky: solid, 1 px, the grey the chart
 *       already uses for constellation figures;</li>
 *   <li>a boundary of what can be seen: dashed 6-on 4-off, the same
 *       weight and grey - a boundary of visibility is not a thing in
 *       the sky;</li>
 *   <li>a reference point: a small open ring with an upward tick - a
 *       <em>place</em>, and deliberately not the cross Sprint 24
 *       uses for working marks.</li>
 * </ul>
 *
 * <p>Labels are the geometry's own accessible name, drawn once where
 * the line leaves the paper, in the grid-label grey. They take no
 * part in the star-label collision policy: reference ink is
 * furniture, and a meridian that displaced a star's name would be
 * the observer editing the sky.
 *
 * <p><strong>Off the page is silence.</strong> On most pages none of
 * this crosses the paper at all, and the chart draws nothing rather
 * than promising a line that is not there. The clipping is analytic
 * - a great circle is straight under this projection - so what is on
 * the paper is decided by the geometry and never by a threshold in
 * pixels.
 */
public final class ReferenceInk {

    private ReferenceInk() {
    }

    private static final BasicStroke SOLID = new BasicStroke(1.0f);

    /** Six on, four off: seen as a boundary, read as a line. */
    private static final BasicStroke DASHED = new BasicStroke(1.0f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {6.0f, 4.0f}, 0.0f);

    /**
     * Long dash, short dot: the cartographic datum line.
     *
     * <p>Chosen by the Sprint 28 gate by drawing the candidates over
     * production pages beside the meridian, in both grounds
     * (docs/decisions/ecliptic.md). It is distinct from all three
     * lines it must not be confused with: the meridian's solid, the
     * horizon's even dash, and the constellation boundaries' fine
     * dots - which is what disqualified a plain dotted candidate.
     */
    private static final BasicStroke DASH_DOT = new BasicStroke(1.0f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {12.0f, 4.0f, 2.0f, 4.0f}, 0.0f);

    /** The zenith ring and its tick, in pixels. */
    private static final double RING = 5.0;
    private static final double TICK = 4.0;

    /** Half the diagonal of a landmark's diamond, in pixels. */
    private static final double DIAMOND =
            juranometria.render.CardinalLandmark.DIAMOND;

    /** How far a label sits off the paper's edge. */
    private static final double LABEL_INSET = 4.0;

    /**
     * Paints every contributed reference geometry.
     *
     * <p>The order is the chart's, not the modules'. Circles are
     * drawn before points, so a zenith ring is never buried under a
     * line through it, and within each kind the contributions are
     * taken in key order rather than in the order their modules
     * happened to attach. Two modules attached the other way round
     * produce the same page.
     */
    /** The ordinary entry point (review of #335). */
    public static List<DirectionPlacement> paint(Graphics2D g,
                      ChartScene scene,
                      List<OverlayRegistry.Owned> contributions,
                      juranometria.render.ChartPalette palette,
                      juranometria.project.PageWords words,
                      List<java.awt.Shape> reserved) {
        return paint(g, scene, contributions, palette, words, reserved,
                null);
    }

    /** The same page with structures emphasized (#361). */
    public static List<DirectionPlacement> paint(Graphics2D g,
                      ChartScene scene,
                      List<OverlayRegistry.Owned> contributions,
                      juranometria.render.ChartPalette palette,
                      juranometria.project.PageWords words,
                      List<java.awt.Shape> reserved,
                      java.util.Set<juranometria.render.ChartStructure> emphasized) {
        return paint(g, DrawnPage.of(scene), contributions, palette,
                words, reserved, emphasized);
    }

    /**
     * @param words the page's language, which owns the cardinal
     *     letters and spoken names (#359); required, like the page's
     *     other words, because a fallback language would be a quiet
     *     lie about what the reader asked for
     * @param reserved ink and text the page has already placed -
     *     subordinate reference words refuse to be written through
     *     any of it
     */
    public static List<DirectionPlacement> paint(Graphics2D g,
                      DrawnPage page,
                      List<OverlayRegistry.Owned> contributions,
                      juranometria.render.ChartPalette palette,
                      juranometria.project.PageWords words,
                      List<java.awt.Shape> reserved) {
        return paint(g, page, contributions, palette, words, reserved,
                structure -> false);
    }

    /**
     * The same, with the reader's emphasized structure - if any -
     * taking its accent (#361).
     *
     * <p>The chart maps each contribution's identity centrally
     * ({@code ChartStructure.ofIdentity}); an identity the chart
     * does not know keeps canonical ink under every selection.
     * Emphasis is ink only: order, clipping, placement, precedence
     * and the line names' ink stay exactly canonical.
     */
    public static List<DirectionPlacement> paint(Graphics2D g,
                      DrawnPage page,
                      List<OverlayRegistry.Owned> contributions,
                      juranometria.render.ChartPalette palette,
                      juranometria.project.PageWords words,
                      List<java.awt.Shape> reserved,
                      java.util.Set<juranometria.render.ChartStructure> emphasized) {
        return paint(g, page, contributions, palette, words, reserved,
                juranometria.render.ChartStructure.membersOf(emphasized));
    }

    /**
     * The released one-target form (#361): membership decided by
     * identity with the one target, never by building a set, so it
     * remains a route independent of the set-shaped one.
     */
    public static List<DirectionPlacement> paint(Graphics2D g,
                      DrawnPage page,
                      List<OverlayRegistry.Owned> contributions,
                      juranometria.render.ChartPalette palette,
                      juranometria.project.PageWords words,
                      List<java.awt.Shape> reserved,
                      juranometria.render.ChartStructure emphasized) {
        return paint(g, page, contributions, palette, words, reserved,
                structure -> structure == emphasized);
    }

    private static List<DirectionPlacement> paint(Graphics2D g,
                      DrawnPage page,
                      List<OverlayRegistry.Owned> contributions,
                      juranometria.render.ChartPalette palette,
                      juranometria.project.PageWords words,
                      List<java.awt.Shape> reserved,
                      java.util.function.Predicate<
                              juranometria.render.ChartStructure>
                              emphasized) {
        ChartScene scene = page.scene();
        if (contributions.isEmpty()) {
            return List.of();
        }
        // Jupiter's cartographic symbol (#484, ruling 7 on #482): this
        // layer paints beneath every catalogue mark, so a symbol drawn
        // here can never erase a real star.
        paintSymbols(g, page, contributions, palette);
        List<OverlayRegistry.Owned> reference = new ArrayList<>();
        for (OverlayRegistry.Owned owned : contributions) {
            if (owned.geometry().role() == InkRole.REFERENCE_LINE) {
                reference.add(owned);
            }
        }
        if (reference.isEmpty()) {
            return List.of();
        }
        reference.sort(Comparator.comparing(OverlayRegistry.Owned::key));

        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        Rectangle2D paper = ChartRenderer.paperOf(scene);
        // The paper, and the limb if this projection has one: a
        // curve is clipped to where there is sky, not only to where
        // there is paper.
        PageRegion region = mapping.regionFor(scene.viewport(), projection);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clip(paper);
            // A module's curves and points are sky, and are clipped
            // where the sky ends (#331, step four). Its names are
            // not: a name cut by the limb is a false name, so they
            // are kept inside by where they are put, exactly as the
            // chart's own labels are.
            // Only a bounded page's clip is touched at all. These
            // are geometric no-ops on ordinary paper - the sky IS
            // the paper there - but a vector writer records every
            // clip it is given, so doing them unconditionally
            // rewrote the released sheets' SVG and PDF without
            // moving a single pixel. The contract caught it on
            // sheet-a4-modules.
            boolean bounded = region.bounded();
            java.awt.Shape sky = skyOf(region, paper);
            java.awt.Shape wholePaper = g2.getClip();
            if (bounded) {
                g2.clip(sky);
            }
            // Circles first, then their names, then the points.
            //
            // The names are placed together rather than each with its
            // own line, because at an overview's fields four
            // reference lines leave the paper near the same corner
            // and a name written where the last one already is is not
            // a name. Production has never needed the rule - at 42
            // degrees no page carries more than two - so it is
            // written to change nothing there, and the released pages
            // are the check that it does not
            // (docs/decisions/overview-projection.md).
            for (OverlayRegistry.Owned owned : reference) {
                if (owned.geometry()
                        instanceof OverlayContribution.GreatCircle circle) {
                    drawCircle(g2, projection, mapping, region, circle,
                            palette, emphasized);
                }
            }
            // The names are the published decision, written rather
            // than decided again here: one placement, drawn by this
            // and readable by anyone (issue #313).
            if (bounded) {
                g2.setClip(wholePaper);
            }
            // One layout for everything this layer writes (#313,
            // #359): the cardinals choose first against the page's
            // own ink alone - the ruled precedence, an accepted
            // cardinal outranks the layer's generic line names - and
            // the names then relocate through their existing
            // candidates or are omitted around them. On a page with
            // no accepted cardinal the seed is empty and every name
            // lands exactly where it always did.
            g2.setColor(palette.gridLabelInk());
            g2.setFont(EquatorialGrid.GRID_LABEL_FONT);
            FontMetrics metrics = g2.getFontMetrics();
            // The Solar System bodies' discs (#415 owner checkpoint):
            // painted later and opaque, so every word this layer
            // writes keeps clear of them rather than being covered.
            List<Shape> discs = bodyDiscs(page, contributions);
            Laid laid = layoutWith(projection, mapping, paper, region,
                    sky, bounded, reference, words, reserved, discs,
                    metrics);
            List<Rectangle2D> taken = laid.taken();
            for (NamePlacement placed : laid.names()) {
                g2.drawString(placed.name(),
                        (float) placed.box().getMinX(),
                        (float) (placed.box().getMaxY()
                                - metrics.getDescent()));
            }
            if (bounded) {
                g2.clip(sky);
            }
            for (OverlayRegistry.Owned owned : reference) {
                if (owned.geometry()
                        instanceof OverlayContribution.Point point) {
                    drawPoint(g2, projection, mapping, paper, sky,
                            bounded, point, taken, reserved, discs,
                            palette, emphasized);
                }
            }
            // The cardinal ink itself, still under the sky clip: the
            // limb halves a limb mark's diamond, which is #331's law
            // and a boundary tick's honest shape. The cardinal
            // landmarks read with the horizon (#361's central map),
            // so they take the horizon's accent with it - mark and
            // letter both, as the grid's notation does with its
            // curves - and placement never moves.
            boolean horizonRaised = emphasized.test(
                    juranometria.render.ChartStructure.HORIZON);
            juranometria.render.StructureStyle.Style mark =
                    juranometria.render.StructureStyle.resolve(palette,
                            juranometria.render.ChartStructure.HORIZON,
                            horizonRaised, palette.figureInk(), SOLID);
            java.awt.Color letterInk =
                    juranometria.render.StructureStyle.resolve(palette,
                            juranometria.render.ChartStructure.HORIZON,
                            horizonRaised, palette.gridLabelInk(), SOLID)
                            .color();
            for (DirectionPlacement placed : laid.directions()) {
                g2.setColor(mark.color());
                g2.setStroke(mark.stroke());
                g2.draw(diamond(placed.at()));
                // The letter under the paper clip: an outward letter
                // lies beyond the limb by design, and the sky clip
                // that halves the diamond would erase it.
                java.awt.Shape underSky = g2.getClip();
                g2.setClip(wholePaper);
                g2.setColor(letterInk);
                g2.drawString(placed.letter(),
                        (float) placed.box().getMinX(),
                        (float) (placed.box().getMaxY()
                                - metrics.getDescent()));
                g2.setClip(underSky);
            }
            return laid.directions();
        } finally {
            g2.dispose();
        }
    }

    /**
     * A rendered cardinal direction: which direction, the letter the
     * page's language draws for it, the spoken name that language
     * gives a reader who cannot see it, the exact horizon point, and
     * the box the letter takes (#359).
     *
     * <p>Both texts ride the decision so that every surface reads
     * the same answer: a direction that is rendered carries its
     * localized visible and spoken words here, and a direction that
     * is not rendered appears nowhere at all.
     */
    public record DirectionPlacement(juranometria.chart.Cardinal cardinal,
            String letter, String spokenName, PixelPoint at,
            Rectangle2D box) {
    }

    /**
     * Everything this layer writes on a page, decided without
     * drawing it (#313's pattern, #359): the cardinal marks first,
     * against the page's own reserved ink alone, and the line names
     * around them - the ruled precedence, applied only when a
     * cardinal is actually accepted, because an empty seed leaves
     * every name where it always was.
     */
    public static List<DirectionPlacement> directionPlacements(
            DrawnPage page, List<OverlayRegistry.Owned> contributions,
            juranometria.project.PageWords words,
            List<java.awt.Shape> reserved) {
        return laidOut(page, contributions, words, reserved)
                .directions();
    }

    /**
     * Where this page's reference-line names go - after the accepted
     * cardinals have taken their boxes (#359), which is why the
     * page's words and reserved ink are part of the question.
     */
    public static List<NamePlacement> namePlacements(DrawnPage page,
            List<OverlayRegistry.Owned> contributions,
            juranometria.project.PageWords words,
            List<java.awt.Shape> reserved) {
        return laidOut(page, contributions, words, reserved).names();
    }

    /** One layout, drawn by paint and readable by anyone. */
    private record Laid(List<DirectionPlacement> directions,
            List<NamePlacement> names, List<Rectangle2D> taken) {
    }

    private static Laid laidOut(DrawnPage page,
            List<OverlayRegistry.Owned> contributions,
            juranometria.project.PageWords words,
            List<java.awt.Shape> reserved) {
        ChartScene scene = page.scene();
        List<OverlayRegistry.Owned> reference =
                referenceOf(contributions);
        if (reference.isEmpty()) {
            return new Laid(List.of(), List.of(), new ArrayList<>());
        }
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        Rectangle2D paper = ChartRenderer.paperOf(scene);
        PageRegion region = mapping.regionFor(scene.viewport(),
                projection);
        return layoutWith(projection, mapping, paper, region,
                skyOf(region, paper), region.bounded(), reference,
                words, reserved, bodyDiscs(page, contributions),
                EquatorialGrid.labelMetrics());
    }

    private static Laid layoutWith(Projection projection,
            ViewportMapping mapping, Rectangle2D paper,
            PageRegion region, java.awt.Shape sky, boolean bounded,
            List<OverlayRegistry.Owned> reference,
            juranometria.project.PageWords words,
            List<java.awt.Shape> reserved, List<Shape> discs,
            FontMetrics metrics) {
        List<Rectangle2D> taken = new ArrayList<>();
        List<java.awt.Shape> reservedAndDiscs = new ArrayList<>(reserved);
        reservedAndDiscs.addAll(discs);
        List<DirectionPlacement> directions = decideDirections(
                projection, mapping, paper, sky, bounded,
                directionMarksIn(reference), words, taken,
                reservedAndDiscs, metrics);
        List<Named> names = new ArrayList<>();
        for (OverlayRegistry.Owned owned : reference) {
            if (owned.geometry()
                    instanceof OverlayContribution.GreatCircle circle) {
                List<CurveRun> runs = GreatCirclePage.clip(projection,
                        mapping, region, circle.pole());
                PixelPoint anchor = labelAnchor(runs);
                if (anchor != null) {
                    CurveRun on = runCarrying(runs, anchor);
                    names.add(new Named(anchor,
                            circle.accessibleName(), owned.moduleId(),
                            on, startsAt(on, anchor), runs,
                            everyOtherCurve(reference, owned,
                                    projection, mapping, region)));
                }
            }
        }
        List<NamePlacement> placedNames = new ArrayList<>();
        for (Named named : names) {
            Rectangle2D box = boxAlong(paper, sky, named, metrics,
                    taken, discs);
            if (box != null) {
                taken.add(box);
                placedNames.add(new NamePlacement(named.moduleId(),
                        named.name(), box));
            }
        }
        return new Laid(List.copyOf(directions),
                List.copyOf(placedNames), taken);
    }

    /** The direction marks, in the order the page considers them. */
    private static List<OverlayContribution.DirectionMark>
            directionMarksIn(List<OverlayRegistry.Owned> reference) {
        List<OverlayContribution.DirectionMark> marks =
                new ArrayList<>();
        for (OverlayRegistry.Owned owned : reference) {
            if (owned.geometry()
                    instanceof OverlayContribution.DirectionMark mark) {
                marks.add(mark);
            }
        }
        return marks;
    }

    /**
     * One cardinal mark each, or nothing (#359).
     *
     * <p>The diamond stays at the exact computed horizon point - on
     * the limb itself when the horizon is the limb, which is the one
     * narrow widening of the sky rule here: {@code contains} is
     * false on its own boundary, so a bounded page also accepts a
     * point within a pixel and a half of it. Everything that is not
     * a cardinal mark keeps the strict rule.
     *
     * <p>The letter takes the first clean box {@link #letterBox}
     * offers - outward into the unused paper first for a mark on a
     * bounded page's limb, then the ordinary tiers. When no
     * candidate is clean the whole landmark is omitted: an
     * unexplained diamond and a letter through other ink are both
     * worse than absence, and the mark never slides to a friendlier
     * spot, because where it is IS what it says.
     *
     * <p>Accepted boxes and diamonds join {@code taken}, so the four
     * yield to one another in their stated order.
     */
    private static List<DirectionPlacement> decideDirections(
            Projection projection, ViewportMapping mapping,
            Rectangle2D paper, java.awt.Shape sky, boolean bounded,
            List<OverlayContribution.DirectionMark> marks,
            juranometria.project.PageWords words,
            List<Rectangle2D> taken, List<java.awt.Shape> reserved,
            FontMetrics metrics) {
        if (!marks.isEmpty() && words == null) {
            throw new IllegalArgumentException(
                    "a cardinal mark's words are the page's language:"
                            + " the page's PageWords are required to"
                            + " place " + marks.get(0).identity());
        }
        List<DirectionPlacement> placed = new ArrayList<>();
        for (OverlayContribution.DirectionMark mark : marks) {
            PixelPoint at = projection.project(mark.at())
                    .map(mapping::toPixel).orElse(null);
            if (at == null || !paper.contains(at.x(), at.y())) {
                continue;
            }
            boolean onSky = sky.contains(at.x(), at.y())
                    || (bounded && sky.intersects(at.x() - 1.5,
                            at.y() - 1.5, 3.0, 3.0));
            if (!onSky) {
                continue;
            }
            String letter = words.directionLetter(mark.direction());
            double w = metrics.stringWidth(letter);
            double h = metrics.getAscent() + metrics.getDescent();
            Rectangle2D box = letterBox(at, w, h, paper, sky, bounded,
                    taken, reserved);
            if (box == null) {
                continue;
            }
            taken.add(box);
            taken.add(new Rectangle2D.Double(at.x() - DIAMOND,
                    at.y() - DIAMOND, 2.0 * DIAMOND, 2.0 * DIAMOND));
            placed.add(new DirectionPlacement(mark.direction(),
                    letter, words.directionSpoken(mark.direction()),
                    at, box));
        }
        return List.copyOf(placed);
    }

    /**
     * The letter's box, or null (#359, owner rulings on its
     * completion).
     *
     * <p>Candidates come in fixed tiers around the exact point: the
     * four adjacent boxes, then the four diagonals, then the four
     * adjacent boxes at twice the distance, then the diagonals there.
     * A box is clean only when it is wholly on the paper and touches
     * nothing in {@code taken} or {@code reserved}.
     *
     * <p>An ordinary mark's letter must also lie wholly on the sky,
     * and the tiers are tried in order - the first tier is the four
     * boxes this mark always had, in the order it always tried them,
     * so a letter that had a place keeps it. On a bounded page the
     * boxes of each tier are tried nearest the page centre first.
     *
     * <p>A mark on the limb of a bounded page - the horizon of a
     * zenith-centred globe is the limb itself - looks outward first:
     * every tier, outermost box first, into the unused paper beyond
     * the sky, where a box must touch no sky at all. Only when paper
     * or furniture refuses every outward box does it fall back
     * inward, with the ordinary rule. The mark never moves: where it
     * is IS what it says.
     */
    private static Rectangle2D letterBox(PixelPoint at, double w,
            double h, Rectangle2D paper, java.awt.Shape sky,
            boolean bounded, List<Rectangle2D> taken,
            List<java.awt.Shape> reserved) {
        double gap = juranometria.render.CardinalLandmark.GAP;
        List<List<Rectangle2D>> tiers = List.of(
                adjacent(at, w, h, gap), diagonal(at, w, h, gap),
                adjacent(at, w, h, 2.0 * gap),
                diagonal(at, w, h, 2.0 * gap));
        double cx = paper.getCenterX();
        double cy = paper.getCenterY();
        java.util.Comparator<Rectangle2D> nearestCentre =
                java.util.Comparator.comparingDouble(box -> Math.hypot(
                        box.getCenterX() - cx, box.getCenterY() - cy));
        boolean onLimb = bounded && !sky.contains(at.x() - 1.5,
                at.y() - 1.5, 3.0, 3.0);
        if (onLimb) {
            for (List<Rectangle2D> tier : tiers) {
                List<Rectangle2D> outward = new ArrayList<>(tier);
                outward.sort(nearestCentre.reversed());
                for (Rectangle2D candidate : outward) {
                    if (paper.contains(candidate)
                            && !sky.intersects(candidate)
                            && clear(candidate, taken, reserved)) {
                        return candidate;
                    }
                }
            }
        }
        for (List<Rectangle2D> tier : tiers) {
            List<Rectangle2D> ordered = new ArrayList<>(tier);
            if (bounded) {
                ordered.sort(nearestCentre);
            }
            for (Rectangle2D candidate : ordered) {
                if (paper.contains(candidate) && sky.contains(candidate)
                        && clear(candidate, taken, reserved)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static List<Rectangle2D> adjacent(PixelPoint at, double w,
                                              double h, double gap) {
        return List.of(
                new Rectangle2D.Double(at.x() + gap, at.y() - h / 2.0,
                        w, h),
                new Rectangle2D.Double(at.x() - gap - w,
                        at.y() - h / 2.0, w, h),
                new Rectangle2D.Double(at.x() - w / 2.0, at.y() + gap,
                        w, h),
                new Rectangle2D.Double(at.x() - w / 2.0,
                        at.y() - gap - h, w, h));
    }

    /** The four corners, three quarters of the gap out on each axis. */
    private static List<Rectangle2D> diagonal(PixelPoint at, double w,
                                              double h, double gap) {
        double d = 0.75 * gap;
        return List.of(
                new Rectangle2D.Double(at.x() + d, at.y() + d, w, h),
                new Rectangle2D.Double(at.x() - d - w, at.y() + d, w, h),
                new Rectangle2D.Double(at.x() + d, at.y() - d - h, w, h),
                new Rectangle2D.Double(at.x() - d - w, at.y() - d - h,
                        w, h));
    }

    private static boolean clear(Rectangle2D box, List<Rectangle2D> taken,
                                 List<java.awt.Shape> reserved) {
        return !overlaps(box, taken) && !touchesAny(box, reserved);
    }

    private static boolean touchesAny(Rectangle2D box,
                                      List<java.awt.Shape> reserved) {
        for (java.awt.Shape shape : reserved) {
            if (shape.intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A line's name, the end of it the name belongs to, and the run
     * that end is on.
     *
     * <p>The run is carried so the name can be walked <em>along its
     * own curve</em> when it does not fit where it first asked. A
     * projected great circle is an ellipse; a straight line towards
     * the middle of the page is not on it, and a slide down the paper
     * is not either - so either would let a name drift off the thing
     * it names and, on a page carrying four reference lines, onto a
     * neighbour's (review of #331).
     */
    private record Named(PixelPoint anchor, String name,
                         String moduleId, CurveRun run,
                         boolean fromTheStart, List<CurveRun> own,
                         List<CurveRun> others) {

        /** A name with no curve to walk: a point's own. */
        Named(PixelPoint anchor, String name, String moduleId) {
            this(anchor, name, moduleId, null, false, List.of(),
                    List.of());
        }
    }

    private static void drawCircle(Graphics2D g,
                                   Projection projection,
                                   ViewportMapping mapping,
                                   PageRegion region,
                                   OverlayContribution.GreatCircle circle,
                                   juranometria.render.ChartPalette palette,
                                   java.util.function.Predicate<
                                           juranometria.render.ChartStructure>
                                           emphasized) {
        List<CurveRun> runs = GreatCirclePage.clip(projection, mapping,
                region, circle.pole());
        if (runs.isEmpty()) {
            // Silence. The circle does not cross this page, and a
            // line drawn anyway would be a promise the sky has not
            // made.
            return;
        }
        // The chart's central identity map decides whether this line
        // belongs to the emphasized structure; an unknown identity
        // keeps canonical ink (#361). The stroke's solid/dashed/
        // dash-dot identity survives emphasis by the resolver's rule.
        juranometria.render.StructureStyle.Style style =
                juranometria.render.ChartStructure
                        .ofIdentity(circle.identity())
                        .map(s -> juranometria.render.StructureStyle
                                .resolve(palette, s,
                                        emphasized.test(s),
                                        palette.figureInk(),
                                        strokeFor(circle.reference())))
                        .orElseGet(() ->
                                new juranometria.render.StructureStyle
                                        .Style(palette.figureInk(),
                                        strokeFor(circle.reference())));
        g.setColor(style.color());
        g.setStroke(style.stroke());
        for (CurveRun run : runs) {
            g.draw(shapeOf(run));
        }
    }

    /** One reference name, and the box it is written in. */
    public record NamePlacement(String moduleId, String name,
                                Rectangle2D box) {
    }

    /**
     * Where this page's reference names go, without drawing them.
     *
     * <p>The reference layer draws its curves and their names under
     * one reader switch, so nothing outside it could tell the two
     * apart - a study measuring what a name covered got the curve as
     * well (issue #310). This publishes the names' own decisions, the
     * way the star-label pass has published its since #154, and
     * {@link #paint} writes precisely this list.
     */
    private static List<OverlayRegistry.Owned> referenceOf(
            List<OverlayRegistry.Owned> contributions) {
        List<OverlayRegistry.Owned> reference = new ArrayList<>();
        for (OverlayRegistry.Owned owned : contributions) {
            if (owned.geometry().role() == InkRole.REFERENCE_LINE) {
                reference.add(owned);
            }
        }
        reference.sort(Comparator.comparing(OverlayRegistry.Owned::key));
        return reference;
    }

    /**
     * Where one name fits, or null when the page has no room left
     * below the ones already written. The decision, with no graphics
     * in it, so it can be published as well as drawn.
     */
    private static Rectangle2D boxFor(Rectangle2D paper, PixelPoint anchor,
                                      String name, FontMetrics metrics,
                                      List<Rectangle2D> taken) {
        return boxFor(paper, paper, anchor, name, metrics, taken,
                List.of());
    }

    /**
     * Where one name goes, on a page whose sky may not be its paper.
     *
     * <p>Candidates in a stated order, the first free one winning, as
     * everything else that places text on this atlas does: the end of
     * the line, then the same point walked in towards the middle of
     * the sky, and at each of those the existing slide downwards past
     * whatever is already written. A candidate counts only if the
     * <em>whole</em> box is on the sky - half a name is a false name
     * wherever the half is lost, at the paper's edge or at the limb.
     *
     * <p>Returns null when nothing fits, which is an omission and not
     * a silence: the page simply does not carry that name, the same
     * answer this has always given when the paper ran out.
     */
    private static Rectangle2D boxFor(Rectangle2D paper,
                                      java.awt.Shape sky,
                                      PixelPoint anchor,
                                      String name, FontMetrics metrics,
                                      List<Rectangle2D> taken,
                                      List<Shape> discs) {
        double line = metrics.getHeight();
        Rectangle2D box = labelBox(paper, anchor, name, metrics);
        while ((overlaps(box, taken) || touchesAny(box, discs)
                        || !sky.contains(box))
                && box.getMaxY() + line <= paper.getMaxY()) {
            box = new Rectangle2D.Double(box.getX(), box.getY() + line,
                    box.getWidth(), box.getHeight());
        }
        return !overlaps(box, taken) && !touchesAny(box, discs)
                && sky.contains(box) ? box : null;
    }

    /**
     * One name written where {@link #boxFor} says it goes, or not
     * written at all - the placement rule the curves' names publish,
     * used here for a point's name so there is one rule and not two.
     */
    private static Rectangle2D write(Graphics2D g, Rectangle2D paper,
                              java.awt.Shape sky,
                              PixelPoint anchor, String name,
                              List<Rectangle2D> taken,
                              List<java.awt.Shape> reserved,
                              List<Shape> discs,
                              juranometria.render.ChartPalette palette) {
        g.setColor(palette.gridLabelInk());
        g.setFont(EquatorialGrid.GRID_LABEL_FONT);
        FontMetrics metrics = g.getFontMetrics();
        Rectangle2D box = pointWordBox(paper, sky, anchor, name, metrics,
                taken, reserved, discs);
        if (box == null) {
            return null;
        }
        taken.add(box);
        g.drawString(name, (float) box.getMinX(),
                (float) (box.getMaxY() - metrics.getDescent()));
        return box;
    }

    /**
     * Where a point's word goes when a Solar System body is on the
     * page (#415 owner checkpoint): the disc is an obstacle, never
     * permission to paint over the word.
     *
     * <p>The released box first, unchanged, whenever no disc touches
     * it - so a page without a body, and every page whose body is
     * elsewhere, writes the word exactly where it always did. When a
     * disc covers that box, the word asks the deterministic
     * alternatives a cardinal letter is offered around the mark - the
     * four adjacent boxes, the four diagonals, then both again at
     * twice the distance, in that order - each wholly on the sky and
     * clear of every word already written, the catalogue's own ink
     * and every disc. Only when all of them are refused does it take
     * the released slide down the page, still clear of the discs; the
     * mark itself never moves.
     */
    private static Rectangle2D pointWordBox(Rectangle2D paper,
            java.awt.Shape sky, PixelPoint anchor, String name,
            FontMetrics metrics, List<Rectangle2D> taken,
            List<java.awt.Shape> reserved, List<Shape> discs) {
        Rectangle2D released = boxFor(paper, sky, anchor, name, metrics,
                taken, List.of());
        if (discs.isEmpty()
                || (released != null && !touchesAny(released, discs))) {
            return released;
        }
        double w = metrics.stringWidth(name);
        double h = metrics.getAscent() + metrics.getDescent();
        double gap = juranometria.render.CardinalLandmark.GAP;
        for (List<Rectangle2D> tier : List.of(
                adjacent(anchor, w, h, gap), diagonal(anchor, w, h, gap),
                adjacent(anchor, w, h, 2.0 * gap),
                diagonal(anchor, w, h, 2.0 * gap))) {
            for (Rectangle2D candidate : tier) {
                if (paper.contains(candidate) && sky.contains(candidate)
                        && clear(candidate, taken, reserved)
                        && !touchesAny(candidate, discs)) {
                    return candidate;
                }
            }
        }
        return boxFor(paper, sky, anchor, name, metrics, taken, discs);
    }

    private static boolean overlaps(Rectangle2D box,
                                    List<Rectangle2D> taken) {
        for (Rectangle2D each : taken) {
            if (each.intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A run, as something Java2D can draw.
     *
     * <p>The projection package says where the ink goes in numbers
     * and never in shapes: it draws nothing, and a scan of its
     * compiled classes holds it to that. Turning a run into a shape
     * is the chart's, which is the older rule as well - a module says
     * where, a projection says where that lands, and the chart
     * decides what it looks like.
     */
    public static Shape shapeOf(CurveRun run) {
        if (run instanceof CurveRun.Segment segment) {
            return new Line2D.Double(segment.start().x(),
                    segment.start().y(), segment.end().x(),
                    segment.end().y());
        }
        CurveRun.Arc arc = (CurveRun.Arc) run;
        // Java2D measures its arcs anticlockwise from the positive x
        // axis, and a page's y runs down, so the same angles arrive
        // negated.
        Arc2D.Double drawn = new Arc2D.Double(
                -arc.radiusAlong(), -arc.radiusAcross(),
                2.0 * arc.radiusAlong(), 2.0 * arc.radiusAcross(),
                Math.toDegrees(-arc.startRadians()),
                Math.toDegrees(-arc.spanRadians()), Arc2D.OPEN);
        AffineTransform onto = new AffineTransform();
        onto.translate(arc.centreX(), arc.centreY());
        onto.rotate(arc.tiltRadians());
        return onto.createTransformedShape(drawn);
    }

    /**
     * What each kind of reference line looks like.
     *
     * <p>The chart's decision, from the module's statement of what
     * the geometry is: a line across the sky, the boundary of what
     * can be seen of it, or a permanent circle of the sphere.
     */
    private static BasicStroke strokeFor(
            OverlayContribution.Reference reference) {
        return switch (reference) {
            case LINE -> SOLID;
            case BOUNDARY -> DASHED;
            case PERMANENT -> DASH_DOT;
        };
    }

    /**
     * A line's own name goes where the line leaves the paper.
     *
     * <p>At the <strong>upper</strong> end, and at the right one if
     * they are level. A rule, so that two runs of the same page put
     * the word in the same place and a reader learns where to look -
     * and the upper end rather than the lower because the lower edge
     * is where the grid writes its right-ascension notation and the
     * title block sits. Reference ink takes no part in the star-label
     * collision policy, which is a decision about not displacing the
     * sky; it is no argument for printing a word on top of the
     * chart's own furniture.
     */

    /**
     * The end a name hangs on: the upper one, and the right one if
     * they are level, across every run the page cut the curve into.
     *
     * <p>Null when the curve closed on itself and has no ends at all.
     */
    public static PixelPoint labelAnchor(List<CurveRun> runs) {
        PixelPoint best = null;
        for (CurveRun run : runs) {
            for (Optional<PixelPoint> end
                    : List.of(run.from(), run.to())) {
                if (end.isEmpty()) {
                    continue;
                }
                PixelPoint at = end.get();
                if (best == null || at.y() < best.y()
                        || (at.y() == best.y() && at.x() > best.x())) {
                    best = at;
                }
            }
        }
        return best;
    }

    /**
     * Where this arc's name lands on the page: at the upper end (the
     * right one if they are level), inset and clamped to the paper.
     *
     * <p>Public, because the journey that audits the page needs the
     * same truth the painter uses - a test that re-guessed the
     * layout would either drift from it or have to allow ink a broad
     * catchment around every anchor, and a review rightly refused
     * the catchment.
     */
    public static Rectangle2D labelBox(Rectangle2D paper,
                                       PixelPoint end,
                                       String name, FontMetrics metrics) {
        double width = metrics.stringWidth(name);
        double x = Math.min(Math.max(end.x() + LABEL_INSET,
                        paper.getMinX() + LABEL_INSET),
                paper.getMaxX() - LABEL_INSET - width);
        double baseline = Math.min(Math.max(end.y() + LABEL_INSET
                        + metrics.getAscent(),
                        paper.getMinY() + LABEL_INSET + metrics.getAscent()),
                paper.getMaxY() - LABEL_INSET);
        return new Rectangle2D.Double(x,
                baseline - metrics.getAscent(), width,
                metrics.getAscent() + metrics.getDescent());
    }

    /** Every reference curve on this page except this one's. */
    private static List<CurveRun> everyOtherCurve(
            List<OverlayRegistry.Owned> reference,
            OverlayRegistry.Owned mine, Projection projection,
            ViewportMapping mapping, PageRegion region) {
        List<CurveRun> others = new ArrayList<>();
        for (OverlayRegistry.Owned owned : reference) {
            if (owned == mine || !(owned.geometry()
                    instanceof OverlayContribution.GreatCircle circle)) {
                continue;
            }
            others.addAll(GreatCirclePage.clip(projection, mapping,
                    region, circle.pole()));
        }
        return others;
    }

    /**
     * How far this box lies from the nearest of these curves, or NaN
     * when there are none.
     *
     * <p>From the box rather than from its middle, so a long name is
     * not judged further from its own line than a short one written
     * in the same place.
     */
    private static double awayFrom(List<CurveRun> runs, Rectangle2D box) {
        // Against the chords between samples, not the samples alone.
        // Points seventeen pixels apart can miss a five-pixel
        // approach entirely, and near a crossing that mismeasurement
        // let a name pass the attribution test while a finer look -
        // the contract's own - put it nearer the rival (found by the
        // pan repair, whose corrected arcs re-anchored a horizon
        // name at such a crossing). A chord's distance to a box is
        // exact, and its sag from the true curve is under a tenth of
        // a pixel at this sampling.
        double nearest = Double.NaN;
        for (CurveRun run : runs) {
            PixelPoint previous = null;
            for (int at = 0; at <= ATTRIBUTION_SAMPLES; at++) {
                PixelPoint point = run.at(at / (double) ATTRIBUTION_SAMPLES);
                double away = previous == null
                        ? pointToBox(point, box)
                        : segmentToBox(previous, point, box);
                if (Double.isNaN(nearest) || away < nearest) {
                    nearest = away;
                }
                previous = point;
            }
        }
        return nearest;
    }

    private static double pointToBox(PixelPoint point, Rectangle2D box) {
        double dx = Math.max(0.0, Math.max(box.getMinX() - point.x(),
                point.x() - box.getMaxX()));
        double dy = Math.max(0.0, Math.max(box.getMinY() - point.y(),
                point.y() - box.getMaxY()));
        return Math.hypot(dx, dy);
    }

    /** Exact distance from a segment to a rectangle. */
    private static double segmentToBox(PixelPoint one, PixelPoint other,
                                       Rectangle2D box) {
        Line2D chord = new Line2D.Double(one.x(), one.y(), other.x(),
                other.y());
        if (chord.intersects(box)) {
            return 0.0;
        }
        double nearest = Math.min(pointToBox(one, box),
                pointToBox(other, box));
        for (double[] corner : new double[][] {
                {box.getMinX(), box.getMinY()},
                {box.getMaxX(), box.getMinY()},
                {box.getMaxX(), box.getMaxY()},
                {box.getMinX(), box.getMaxY()}}) {
            nearest = Math.min(nearest,
                    chord.ptSegDist(corner[0], corner[1]));
        }
        return nearest;
    }

    /** How finely a curve is sampled when asking what a name is beside. */
    private static final int ATTRIBUTION_SAMPLES = 120;

    /**
     * Whether a reader could tell which line this name belongs to:
     * its own curve is strictly nearer the box than any other
     * reference curve on the page.
     *
     * <p>Being inside the limb was never the whole requirement. Two
     * great circles on a hemisphere always cross, and near a crossing
     * a name can sit beside the wrong one while breaking no rule that
     * was being checked - which is a label that is placed correctly
     * and says something false (review of #331).
     */
    private static boolean attributable(Named named, Rectangle2D box) {
        double own = awayFrom(named.own(), box);
        double other = awayFrom(named.others(), box);
        return Double.isNaN(own) || Double.isNaN(other) || own < other;
    }

    /** Which of these runs carries this endpoint. */
    private static CurveRun runCarrying(List<CurveRun> runs,
                                        PixelPoint end) {
        for (CurveRun run : runs) {
            for (Optional<PixelPoint> at
                    : List.of(run.from(), run.to())) {
                if (at.isPresent() && at.get().equals(end)) {
                    return run;
                }
            }
        }
        return null;
    }

    /** Whether this endpoint is the run's start rather than its end. */
    private static boolean startsAt(CurveRun run, PixelPoint end) {
        return run != null && run.from().isPresent()
                && run.from().get().equals(end);
    }

    /**
     * Where one reference name goes, walked along its own curve.
     *
     * <p>Candidates in a stated order, the first free one winning, as
     * everything else that places text on this atlas does: the end of
     * the line where the name belongs, then points further along the
     * <strong>same run</strong>, and at each of those the existing
     * slide downwards past whatever is already written. A candidate
     * counts only if the whole box is on the sky.
     *
     * <p>Walking the curve rather than the page is the whole point. A
     * name that set off towards the middle of the disc would leave
     * the ellipse it names immediately, and on a page carrying a
     * meridian, a horizon and an ecliptic it could end up nearer a
     * line it says nothing about - a label inside the limb and still
     * a lie (review of #331).
     *
     * <p>A name with no run - a point's - does not walk at all. Its
     * anchor is the place it names, and a place has no curve to move
     * along; it takes the slide or it is not written.
     */
    private static Rectangle2D boxAlong(Rectangle2D paper,
                                        java.awt.Shape sky,
                                        Named named,
                                        FontMetrics metrics,
                                        List<Rectangle2D> taken,
                                        List<Shape> discs) {
        if (named.run() == null) {
            return boxFor(paper, sky, named.anchor(), named.name(),
                    metrics, taken, discs);
        }
        for (int step = 0; step <= ALONG_STEPS; step++) {
            double walked = step / (double) ALONG_STEPS * ALONG_MOST;
            double fraction = named.fromTheStart() ? walked
                    : 1.0 - walked;
            Rectangle2D box = boxFor(paper, sky,
                    named.run().at(fraction), named.name(), metrics,
                    taken, discs);
            if (box != null && attributable(named, box)) {
                return box;
            }
        }
        return null;
    }

    /**
     * How far along its own run a name may be walked, and in how many
     * steps: at most to the middle of the run, so a name stays in the
     * half of the curve whose end it belongs to and cannot wander
     * into the other end's territory.
     */
    private static final int ALONG_STEPS = 20;

    private static final double ALONG_MOST = 0.5;

    /**
     * The sky this page has, as a shape: the disc for a globe, and
     * the paper for every other page (Sprint 32, issue #331, step
     * four).
     */
    static java.awt.Shape skyOf(PageRegion region, Rectangle2D paper) {
        return region.bounded()
                ? new Ellipse2D.Double(
                        region.limbX() - region.limbRadius(),
                        region.limbY() - region.limbRadius(),
                        2.0 * region.limbRadius(),
                        2.0 * region.limbRadius())
                : paper;
    }


    private static Rectangle2D drawPoint(Graphics2D g,
                                  Projection projection,
                                  ViewportMapping mapping,
                                  Rectangle2D paper,
                                  java.awt.Shape sky,
                                  boolean bounded,
                                  OverlayContribution.Point point,
                                  List<Rectangle2D> taken,
                                  List<java.awt.Shape> reserved,
                                  List<Shape> discs,
                                  juranometria.render.ChartPalette palette,
                                  java.util.function.Predicate<
                                           juranometria.render.ChartStructure>
                                           emphasized) {
        PixelPoint at = projection.project(point.at())
                .map(mapping::toPixel).orElse(null);
        if (at == null || !sky.contains(at.x(), at.y())) {
            return null;
        }
        // A landmark takes its structure's accent with the line it
        // belongs to - the ecliptic's seasonal marks with the
        // ecliptic, a zenith with the meridian - and an unknown
        // identity keeps canonical ink (#361).
        juranometria.render.StructureStyle.Style style =
                juranometria.render.ChartStructure
                        .ofIdentity(point.identity())
                        .map(s -> juranometria.render.StructureStyle
                                .resolve(palette, s,
                                        emphasized.test(s),
                                        palette.figureInk(), SOLID))
                        .orElseGet(() ->
                                new juranometria.render.StructureStyle
                                        .Style(palette.figureInk(), SOLID));
        g.setColor(style.color());
        g.setStroke(style.stroke());
        switch (point.mark()) {
            case PLACE -> {
                g.draw(new Ellipse2D.Double(at.x() - RING, at.y() - RING,
                        2.0 * RING, 2.0 * RING));
                // Upward, because a place overhead has a direction
                // and a ring alone would read as an object.
                g.draw(new Line2D.Double(at.x(), at.y() - RING,
                        at.x(), at.y() - RING - TICK));
            }
            // An open diamond: no other mark on this chart is one.
            // Stars are filled discs, deep-sky objects are ellipses,
            // dotted and crossed circles, boxes and spoked squares,
            // a working mark is a gapped cross, and a place is the
            // ring and tick above. It reads as a marked position on
            // a line rather than as a place or an object.
            case LANDMARK -> g.draw(diamond(at));
        }
        // A landmark's name keeps clear of the lines' names as well
        // as of the other landmarks': the marks are drawn in their
        // own order and only the words are placed against what is
        // already written.
        //
        // Written outside the sky clip the mark was drawn under, and
        // kept on the sky by where it is put instead (#331, step
        // four). A name the limb cut in half would be the fault this
        // whole step exists to prevent.
        if (!bounded) {
            return write(g, paper, sky, at, point.accessibleName(), taken,
                    reserved, discs, palette);
        }
        Shape marksClip = g.getClip();
        g.setClip(paper);
        try {
            return write(g, paper, sky, at, point.accessibleName(), taken,
                    reserved, discs, palette);
        } finally {
            g.setClip(marksClip);
        }
    }

    /** A landmark's open diamond, about its position. */
    private static java.awt.geom.Path2D diamond(PixelPoint at) {
        java.awt.geom.Path2D shape = new java.awt.geom.Path2D.Double();
        shape.moveTo(at.x(), at.y() - DIAMOND);
        shape.lineTo(at.x() + DIAMOND, at.y());
        shape.lineTo(at.x(), at.y() + DIAMOND);
        shape.lineTo(at.x() - DIAMOND, at.y());
        shape.closePath();
        return shape;
    }

    // ---- Solar System bodies (Sprint 37, issue #415) -------------------

    /**
     * A body the page drew: which, where its centre landed, the disc's
     * opaque ink in page pixels, and the box its name took or null
     * when every candidate was refused (#414 ruling C4: the mark stays
     * where it is; the name may go).
     */
    public record BodyPlacement(String identity, PixelPoint centre,
                                Shape disc, String name, Rectangle2D box,
                                boolean belowHorizon, boolean symbol,
                                String state) {
        /**
         * Whether a page point lies on this body's opaque ink. A
         * cartographic symbol (#484) is no opaque ink: it hides nothing,
         * so a click through it reaches what is under it.
         */
        public boolean covers(double x, double y) {
            return !symbol && disc.contains(x, y);
        }
    }

    /**
     * Where an oblate body lands on a page (#484): its centre, its
     * outline - the larger of its true size and the minimum mark, turned
     * from a circle at the minimum to the true axis ratio at twice it,
     * the minor axis along the pole through the page's own north and
     * east at the body - its drawn equatorial radius, and whether it is
     * a cartographic symbol (its true disc below the minimum).
     */
    record OblateMark(PixelPoint centre, Shape outline, double radius,
                      boolean symbol, boolean onPage) {
    }

    /**
     * A satellite's mark on a page (#485): where it lands, its symbol, and
     * whether it is painted with its primary's opaque disc - in front of
     * it, or touching it - rather than beneath the stars.
     */
    record SatelliteMark(OverlayContribution.Satellite satellite, PixelPoint centre,
                         Ellipse2D mark, boolean withPrimary) {
    }

    /** An oblate body, its mark, and the satellites this page draws with it. */
    record JovianSystem(OverlayContribution.OblateBody primary, OblateMark mark,
                        List<SatelliteMark> satellites) {
    }

    /** Every oblate body's system on this page, each satellite decided on its own. */
    static List<JovianSystem> systemsOn(DrawnPage page,
            List<OverlayRegistry.Owned> contributions) {
        List<OverlayContribution> offered = new ArrayList<>();
        for (OverlayRegistry.Owned owned : contributions) {
            offered.add(owned.geometry());
        }
        List<JovianSystem> systems = new ArrayList<>();
        for (OverlayContribution c : offered) {
            if (!(c instanceof OverlayContribution.OblateBody primary)) {
                continue;
            }
            OblateMark mark = oblateOn(page, primary);
            if (mark == null) {
                continue;
            }
            systems.add(new JovianSystem(primary, mark,
                    satellitesOn(page, mark, SatelliteMarks.of(primary.identity(), offered))));
        }
        return systems;
    }

    private static List<SatelliteMark> satellitesOn(DrawnPage page, OblateMark primary,
            List<OverlayContribution.Satellite> satellites) {
        if (satellites.isEmpty()) {
            return List.of();
        }
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        Rectangle2D paper = ChartRenderer.paperOf(page.scene());
        PageRegion region = mapping.regionFor(page.scene().viewport(), projection);
        Shape sky = skyOf(region, paper);
        List<OverlayContribution.Satellite> placed = new ArrayList<>();
        List<PixelPoint> centres = new ArrayList<>();
        List<double[]> offsets = new ArrayList<>();
        for (OverlayContribution.Satellite s : satellites) {
            Optional<juranometria.project.PlanePoint> projected = projection.project(s.at());
            if (projected.isEmpty()) {
                continue;
            }
            PixelPoint c = mapping.toPixel(projected.get());
            placed.add(s);
            centres.add(c);
            offsets.add(new double[] {c.x() - primary.centre().x(), c.y() - primary.centre().y()});
        }
        // The chart's normal minimum field is read, never assumed to be
        // 1° (ruling 9 on #482).
        boolean atFloor = page.scene().viewport().fieldWidthDegrees()
                <= juranometria.chart.ChartViewState.normalMinimumFieldDegrees() + 1e-9;
        List<SatelliteMarks.Decision> decisions = SatelliteMarks.decide(atFloor,
                2.0 * primary.radius(), placed, offsets);
        List<SatelliteMark> out = new ArrayList<>();
        for (int i = 0; i < placed.size(); i++) {
            if (decisions.get(i) != SatelliteMarks.Decision.DRAWN) {
                continue;
            }
            OverlayContribution.Satellite s = placed.get(i);
            double r = s.markPx() / 2.0;
            Ellipse2D mark = discOn(centres.get(i), r, paper, sky, region.bounded());
            if (mark == null) {
                continue;
            }
            boolean withPrimary = primary.onPage() && !primary.symbol()
                    && (s.relation() == OverlayContribution.Satellite.Relation.IN_FRONT
                            || primary.outline().intersects(mark.getBounds2D()));
            out.add(new SatelliteMark(s, centres.get(i), mark, withPrimary));
        }
        // In front last, so a transit reads over everything else.
        out.sort(Comparator.comparingInt(m -> -SatelliteMarks.precedence(m.satellite())));
        return List.copyOf(out);
    }

    /**
     * A satellite's mark in state vocabulary A (ruling 5 on #482): clear,
     * a filled dot; in front of its primary, a filled dot ringed in the
     * page's ground; wholly or partly in shadow, a hollow ring. Behind is
     * never drawn. Dimmed below a drawn horizon.
     */
    private static void paintSatellite(Graphics2D g2, SatelliteMark m,
            juranometria.render.ChartPalette palette) {
        Color ink = palette.starInk();
        Color ground = palette.ground();
        if (m.satellite().belowHorizon()) {
            ink = ChartRenderer.quiet(ink, ground);
        }
        Ellipse2D dot = m.mark();
        if (m.satellite().relation() == OverlayContribution.Satellite.Relation.IN_FRONT) {
            g2.setColor(ground);
            g2.fill(new Ellipse2D.Double(dot.getX() - 1.0, dot.getY() - 1.0,
                    dot.getWidth() + 2.0, dot.getHeight() + 2.0));
            g2.setColor(ink);
            g2.fill(dot);
        } else if (m.satellite().shadowed()) {
            g2.setColor(ink);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(dot);
        } else {
            g2.setColor(ink);
            g2.fill(dot);
        }
    }

    /** The ink a satellite's mark keeps clear of words. */
    private static Shape inkOf(SatelliteMark m) {
        Ellipse2D d = m.mark();
        return new Ellipse2D.Double(d.getX() - 1.5, d.getY() - 1.5, d.getWidth() + 3.0,
                d.getHeight() + 3.0);
    }

    /** The oblate body's mark on this page, or null when none of it is there. */
    static OblateMark oblateOn(DrawnPage page, OverlayContribution.OblateBody body) {
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        Rectangle2D paper = ChartRenderer.paperOf(page.scene());
        PageRegion region = mapping.regionFor(page.scene().viewport(), projection);
        Shape sky = skyOf(region, paper);
        Optional<juranometria.project.PlanePoint> projected = projection.project(body.at());
        if (projected.isEmpty()) {
            return null;
        }
        PixelPoint centre = mapping.toPixel(projected.get());
        double trueEquatorial = 2.0 * radiusPx(projection, mapping, body.at(),
                body.equatorialDiameterArcseconds() / 3600.0 / 2.0);
        double minimum = body.minimumMarkPx();
        double drawnEquatorial = Math.max(trueEquatorial, minimum);
        double ratio = body.polarDiameterArcseconds() / body.equatorialDiameterArcseconds();
        double blend = Math.max(0.0, Math.min(1.0, (trueEquatorial - minimum) / minimum));
        double drawnPolar = drawnEquatorial * (1.0 - (1.0 - ratio) * blend);
        boolean onPage = discOn(centre, drawnEquatorial / 2.0, paper, sky,
                region.bounded()) != null;
        double[] up = PageBasis.at(page, body.at())
                .map(basis -> basis.direction(body.poleAngleDegrees()))
                .orElse(new double[] {0.0, -1.0});
        java.awt.geom.AffineTransform turn = new java.awt.geom.AffineTransform();
        turn.translate(centre.x(), centre.y());
        // Local x is the equator, local y the pole: turned so that y
        // lies along the pole's page direction.
        turn.rotate(Math.atan2(up[1], up[0]) + Math.PI / 2.0);
        Shape outline = turn.createTransformedShape(new Ellipse2D.Double(
                -drawnEquatorial / 2.0, -drawnPolar / 2.0, drawnEquatorial, drawnPolar));
        return new OblateMark(centre, outline, drawnEquatorial / 2.0,
                trueEquatorial < minimum, onPage);
    }

    /** An oblate body's ink: its lit face filled, its limb drawn; dimmed below a drawn horizon. */
    private static void paintOblate(Graphics2D g2, OblateMark mark,
            juranometria.render.ChartPalette palette, boolean belowHorizon) {
        Color face = brightness(palette.ground()) >= brightness(palette.starInk())
                ? palette.ground() : palette.starInk();
        Color limb = palette.starInk();
        if (belowHorizon) {
            face = ChartRenderer.quiet(face, palette.ground());
            limb = ChartRenderer.quiet(limb, palette.ground());
        }
        g2.setColor(face);
        g2.fill(mark.outline());
        g2.setColor(limb);
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(mark.outline());
    }

    /** The ink an oblate mark keeps clear of words: its outline grown by its stroke and a pixel. */
    private static Shape inkOf(OblateMark mark) {
        double grown = mark.radius() + 1.5;
        return new Ellipse2D.Double(mark.centre().x() - grown, mark.centre().y() - grown,
                2.0 * grown, 2.0 * grown);
    }

    /**
     * What an oblate body's system paints beneath the catalogue's marks
     * (#484, #485; ruling 7 on #482): the body itself while it is a
     * cartographic symbol, and every satellite mark not painted with an
     * opaque disc - so no symbol ever erases a real star.
     */
    private static void paintSymbols(Graphics2D g, DrawnPage page,
            List<OverlayRegistry.Owned> contributions,
            juranometria.render.ChartPalette palette) {
        List<JovianSystem> systems = systemsOn(page, contributions);
        if (systems.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            ViewportMapping mapping = new ViewportMapping(page);
            Rectangle2D paper = ChartRenderer.paperOf(page.scene());
            g2.clip(skyOf(mapping.regionFor(page.scene().viewport(), page.projection()),
                    paper));
            for (JovianSystem system : systems) {
                if (system.mark().symbol() && system.mark().onPage()) {
                    paintOblate(g2, system.mark(), palette, system.primary().belowHorizon());
                }
                for (SatelliteMark m : system.satellites()) {
                    if (!m.withPrimary()) {
                        paintSatellite(g2, m, palette);
                    }
                }
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints every {@link OverlayContribution.Body} at its true
     * angular size, after the catalogue's marks and labels and before
     * the furniture (the renderer's bodies layer), and returns what
     * was drawn so the page can answer a pointer and a screen reader.
     *
     * <p>The ruling of #414: the Sun is a ring with its centre dot, the
     * disc filled with the ground so what it covers is covered; the
     * Moon is a phased disc (#416) - its dark side inked, its lit side
     * turned to the bright limb through the page's own north and east
     * at the Moon ({@link PageBasis}), the terminator a half-ellipse
     * of axis ratio |cos i|. Below the drawn horizon the ink is dimmed
     * towards the ground and the name carries the language's status; a
     * body whose disc leaves the page entirely is not drawn at all - no
     * edge hint.
     *
     * <p>Three passes. Every disc is placed first; they are painted
     * farthest first, so the nearer covers the farther as it does in
     * the sky - the Moon over the Sun at a new Moon; and then they are
     * named, nearest first, each name in the first clean adjacent box
     * around its disc - clear of the reference layer's words, of the
     * catalogue's ink and of <em>every</em> disc, so no name is painted
     * over by another body or written across one - and refused, not
     * moved, when none is clean.
     */
    public static List<BodyPlacement> paintBodies(Graphics2D g,
            DrawnPage page, List<OverlayRegistry.Owned> contributions,
            juranometria.render.ChartPalette palette,
            juranometria.project.PageWords words, List<Shape> reserved,
            List<Rectangle2D> referenceBoxes) {
        List<OverlayContribution> bodies = new ArrayList<>();
        for (OverlayRegistry.Owned owned : contributions) {
            if (owned.geometry() instanceof OverlayContribution.Body
                    || owned.geometry() instanceof OverlayContribution.OblateBody) {
                bodies.add(owned.geometry());
            }
        }
        if (bodies.isEmpty()) {
            return List.of();
        }
        // Farthest first; identity breaks a tie, so the page never
        // depends on the order the modules happened to offer.
        bodies.sort(Comparator.comparingDouble(ReferenceInk::distanceOf)
                .reversed()
                .thenComparing(OverlayContribution::identity));
        ChartScene scene = page.scene();
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        Rectangle2D paper = ChartRenderer.paperOf(scene);
        PageRegion region = mapping.regionFor(scene.viewport(), projection);
        Shape sky = skyOf(region, paper);
        boolean bounded = region.bounded();

        // Pass one: where each disc lands, and how large it is.
        List<OverlayContribution> drawn = new ArrayList<>();
        List<PixelPoint> centres = new ArrayList<>();
        List<Shape> discs = new ArrayList<>();
        List<Shape> inks = new ArrayList<>();
        List<OblateMark> oblates = new ArrayList<>();
        for (OverlayContribution contribution : bodies) {
            if (contribution instanceof OverlayContribution.OblateBody oblate) {
                OblateMark mark = oblateOn(page, oblate);
                if (mark == null || !mark.onPage()) {
                    continue;
                }
                drawn.add(oblate);
                centres.add(mark.centre());
                discs.add(mark.outline());
                inks.add(inkOf(mark));
                oblates.add(mark);
                continue;
            }
            OverlayContribution.Body body = (OverlayContribution.Body) contribution;
            Optional<juranometria.project.PlanePoint> projected =
                    projection.project(body.at());
            if (projected.isEmpty()) {
                continue;
            }
            PixelPoint centre = mapping.toPixel(projected.get());
            double radius = radiusPx(projection, mapping, body.at(),
                    body.angularDiameterArcseconds() / 3600.0 / 2.0);
            Ellipse2D disc = discOn(centre, radius, paper, sky, bounded);
            if (disc == null) {
                continue;
            }
            drawn.add(body);
            centres.add(centre);
            discs.add(disc);
            inks.add(inkOf(disc, body.lit() != null));
            oblates.add(null);
        }
        String[] names = new String[drawn.size()];
        Rectangle2D[] boxes = new Rectangle2D[drawn.size()];
        // The Galilean moons (#485), decided moon by moon; their marks
        // keep words and names clear of them.
        List<JovianSystem> systems = systemsOn(page, contributions);
        List<SatelliteMark> moons = new ArrayList<>();
        for (JovianSystem system : systems) {
            moons.addAll(system.satellites());
        }
        for (SatelliteMark m : moons) {
            inks.add(inkOf(m));
        }
        java.util.Map<SatelliteMark, String> moonNames = new java.util.HashMap<>();
        java.util.Map<SatelliteMark, Rectangle2D> moonBoxes = new java.util.HashMap<>();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            // Pass two: the discs, farthest first. A symbol is not
            // painted here: the reference layer drew it beneath the
            // stars (#484).
            for (int i = 0; i < drawn.size(); i++) {
                if (drawn.get(i) instanceof OverlayContribution.OblateBody oblate) {
                    if (!oblates.get(i).symbol()) {
                        paintOblate(g2, oblates.get(i), palette, oblate.belowHorizon());
                        // A moon in front of the disc, or touching it, is
                        // painted with it, over it (ruling 4).
                        for (JovianSystem system : systems) {
                            if (system.primary() == oblate) {
                                for (SatelliteMark m : system.satellites()) {
                                    if (m.withPrimary()) {
                                        paintSatellite(g2, m, palette);
                                    }
                                }
                            }
                        }
                    }
                    continue;
                }
                OverlayContribution.Body body = (OverlayContribution.Body) drawn.get(i);
                Ellipse2D disc = (Ellipse2D) discs.get(i);
                double radius = disc.getWidth() / 2.0;
                if (body.lit() == null) {
                    paintSun(g2, disc, centres.get(i), radius, palette,
                            body.belowHorizon());
                } else {
                    PageBasis basis = PageBasis.at(page, body.at()).orElse(null);
                    paintMoon(g2, disc, centres.get(i), radius, basis,
                            body.lit(), palette, body.belowHorizon());
                }
            }
            // Pass three: the names, nearest first. A name with no clean
            // adjacent box is refused on its own; it never overlaps.
            g2.setFont(EquatorialGrid.GRID_LABEL_FONT);
            FontMetrics metrics = g2.getFontMetrics();
            List<Rectangle2D> taken = new ArrayList<>(referenceBoxes);
            List<Shape> obstacles = new ArrayList<>(reserved);
            obstacles.addAll(inks);
            for (int i = drawn.size() - 1; i >= 0; i--) {
                OverlayContribution body = drawn.get(i);
                boolean below = belowHorizonOf(body);
                String name = words.bodyName(body.identity());
                if (below) {
                    name = name + " " + words.bodyBelowHorizon();
                }
                names[i] = name;
                double w = metrics.stringWidth(name);
                double h = metrics.getAscent() + metrics.getDescent();
                Rectangle2D box = bodyNameBox(centres.get(i),
                        inks.get(i).getBounds2D().getWidth() / 2.0, w, h, paper,
                        sky, bounded, taken, obstacles);
                if (box != null) {
                    taken.add(box);
                    boxes[i] = box;
                    g2.setColor(below
                            ? ChartRenderer.quiet(palette.textInk(), palette.ground())
                            : palette.textInk());
                    g2.drawString(name, (float) box.getMinX(),
                            (float) (box.getMaxY() - metrics.getDescent()));
                }
            }
            // Pass four: the moons' names, decided separately from their
            // marks (ruling 6 on #482) - in precedence order, each through
            // the same adjacent boxes and refused on its own when none is
            // clean, so one crowded pair never takes every name with it.
            List<SatelliteMark> naming = new ArrayList<>(moons);
            naming.sort(Comparator.comparingInt(m -> SatelliteMarks.precedence(m.satellite())));
            for (SatelliteMark m : naming) {
                String name = words.bodyName(m.satellite().identity());
                double w = metrics.stringWidth(name);
                double h = metrics.getAscent() + metrics.getDescent();
                Rectangle2D box = bodyNameBox(m.centre(), m.mark().getWidth() / 2.0 + 1.5, w, h,
                        paper, sky, bounded, taken, obstacles);
                moonNames.put(m, name);
                if (box != null) {
                    taken.add(box);
                    moonBoxes.put(m, box);
                    g2.setColor(m.satellite().belowHorizon()
                            ? ChartRenderer.quiet(palette.textInk(), palette.ground())
                            : palette.textInk());
                    g2.drawString(name, (float) box.getMinX(),
                            (float) (box.getMaxY() - metrics.getDescent()));
                }
            }
        } finally {
            g2.dispose();
        }
        List<BodyPlacement> placed = new ArrayList<>();
        for (int i = 0; i < drawn.size(); i++) {
            placed.add(new BodyPlacement(drawn.get(i).identity(), centres.get(i),
                    discs.get(i), names[i], boxes[i], belowHorizonOf(drawn.get(i)),
                    oblates.get(i) != null && oblates.get(i).symbol(), null));
        }
        for (SatelliteMark m : moons) {
            OverlayContribution.Satellite s = m.satellite();
            String state = s.relation() == OverlayContribution.Satellite.Relation.IN_FRONT
                    ? "jovian.inFront" : s.shadowed() ? "jovian.shadowed" : null;
            placed.add(new BodyPlacement(s.identity(), m.centre(), m.mark(),
                    moonNames.get(m), moonBoxes.get(m), s.belowHorizon(), true, state));
        }
        return List.copyOf(placed);
    }

    private static double distanceOf(OverlayContribution body) {
        return body instanceof OverlayContribution.OblateBody oblate
                ? oblate.distanceKm() : ((OverlayContribution.Body) body).distanceKm();
    }

    private static boolean belowHorizonOf(OverlayContribution body) {
        return body instanceof OverlayContribution.OblateBody oblate
                ? oblate.belowHorizon() : ((OverlayContribution.Body) body).belowHorizon();
    }

    /** The Sun: the ground, then the ring and its centre dot in star ink. */
    private static void paintSun(Graphics2D g2, Ellipse2D disc, PixelPoint centre,
            double radius, juranometria.render.ChartPalette palette,
            boolean belowHorizon) {
        Color ink = belowHorizon
                ? ChartRenderer.quiet(palette.starInk(), palette.ground())
                : palette.starInk();
        // The ground first: whatever the disc covers is covered.
        g2.setColor(palette.ground());
        g2.fill(disc);
        g2.setColor(ink);
        g2.setStroke(new BasicStroke((float) ringWidth(radius)));
        g2.draw(disc);
        double dot = Math.max(1.5, radius / 6.0);
        g2.fill(new Ellipse2D.Double(centre.x() - dot, centre.y() - dot,
                2.0 * dot, 2.0 * dot));
    }

    /**
     * How far the Moon's dark side lies from the page's darker ink
     * towards its lighter: dark enough to read as unlit on both
     * palettes, light enough on paper that the disc still reads as a
     * disc and not a hole, and opaque on both, so the stars it hides
     * are seen to be hidden (#414 ruling C4).
     */
    static final double DARK_SIDE = 0.3;

    /** The Moon's dark-side share, for journeys outside this package. */
    public static double darkSide() {
        return DARK_SIDE;
    }

    /**
     * The Moon: the whole disc in the dark side's ink, the lit region
     * in the page's lighter ink, and the limb in star ink.
     */
    private static void paintMoon(Graphics2D g2, Ellipse2D disc, PixelPoint centre,
            double radius, PageBasis basis, OverlayContribution.Lit lit,
            juranometria.render.ChartPalette palette, boolean belowHorizon) {
        Color lighter = brightness(palette.ground()) >= brightness(palette.starInk())
                ? palette.ground() : palette.starInk();
        Color darker = lighter == palette.ground() ? palette.starInk() : palette.ground();
        Color dark = mix(darker, lighter, DARK_SIDE);
        Color limb = palette.starInk();
        if (belowHorizon) {
            lighter = ChartRenderer.quiet(lighter, palette.ground());
            dark = ChartRenderer.quiet(dark, palette.ground());
            limb = ChartRenderer.quiet(limb, palette.ground());
        }
        g2.setColor(dark);
        g2.fill(disc);
        if (basis != null) {
            g2.setColor(lighter);
            g2.fill(litRegion(centre, radius, basis, lit));
        }
        g2.setColor(limb);
        g2.setStroke(new BasicStroke((float) moonLimbWidth(radius)));
        g2.draw(disc);
    }

    /**
     * The lit part of a disc on the page (#414 ruling C3): in a frame
     * whose +x points at the bright limb - the page direction of χ
     * through the page's own north and east at the body - the half
     * disc towards the limb, with the half-ellipse of semi-axis
     * r·|cos i| across it added beyond the centre when gibbous
     * (cos i &gt; 0) and taken away from it when crescent. Its area
     * is k = (1 + cos i) / 2 of the disc's.
     */
    static java.awt.geom.Area litRegion(PixelPoint centre, double radius,
            PageBasis basis, OverlayContribution.Lit lit) {
        double cosI = Math.cos(Math.toRadians(lit.phaseAngleDegrees()));
        double r = radius;
        java.awt.geom.Area half = new java.awt.geom.Area(
                new Rectangle2D.Double(0.0, -r - 1.0, r + 1.0, 2.0 * r + 2.0));
        half.intersect(new java.awt.geom.Area(
                new Ellipse2D.Double(-r, -r, 2.0 * r, 2.0 * r)));
        double across = r * Math.abs(cosI);
        java.awt.geom.Area terminator = new java.awt.geom.Area(
                new Ellipse2D.Double(-across, -r, 2.0 * across, 2.0 * r));
        java.awt.geom.Area litArea = new java.awt.geom.Area(half);
        if (cosI >= 0.0) {
            terminator.intersect(new java.awt.geom.Area(new Rectangle2D.Double(
                    -r - 1.0, -r - 1.0, r + 1.0, 2.0 * r + 2.0)));
            litArea.add(terminator);
        } else {
            terminator.intersect(new java.awt.geom.Area(new Rectangle2D.Double(
                    0.0, -r - 1.0, r + 1.0, 2.0 * r + 2.0)));
            litArea.subtract(terminator);
        }
        double[] towards = basis.direction(lit.brightLimbAngleDegrees());
        AffineTransform to = new AffineTransform();
        to.translate(centre.x(), centre.y());
        to.rotate(Math.atan2(towards[1], towards[0]));
        litArea.transform(to);
        return litArea;
    }

    private static double brightness(Color c) {
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    private static Color mix(Color from, Color to, double t) {
        return new Color(
                (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t));
    }

    /** The Moon's limb stroke width for a disc of this radius. */
    private static double moonLimbWidth(double radius) {
        return Math.max(1.0, radius / 40.0);
    }

    /** The ring's stroke width for a disc of this radius. */
    private static double ringWidth(double radius) {
        return Math.max(1.0, radius / 12.0);
    }

    /**
     * Everything a disc's ink covers: the disc, half the ring's stroke
     * outside its limb, and the antialiased pixel beyond that.
     */
    private static Shape inkOf(Ellipse2D disc, boolean lit) {
        double radius = disc.getWidth() / 2.0;
        double stroke = lit ? moonLimbWidth(radius) : ringWidth(radius);
        double grown = radius + stroke / 2.0 + 1.0;
        return new Ellipse2D.Double(disc.getCenterX() - grown,
                disc.getCenterY() - grown, 2.0 * grown, 2.0 * grown);
    }

    /** The disc at this centre, or null when none of it is on the page. */
    private static Ellipse2D discOn(PixelPoint centre, double radius,
            Rectangle2D paper, Shape sky, boolean bounded) {
        Ellipse2D disc = new Ellipse2D.Double(centre.x() - radius,
                centre.y() - radius, 2.0 * radius, 2.0 * radius);
        if (!disc.intersects(paper)
                || (bounded && !sky.intersects(disc.getBounds2D()))) {
            return null;
        }
        return disc;
    }

    /**
     * The opaque discs the bodies layer will paint on this page, known
     * before it paints (#415 owner checkpoint), so the reference
     * layer's words can treat them as obstacles: the same position,
     * true size and page test {@link #paintBodies} uses. Empty on
     * every page with no body, which leaves that page as released.
     */
    static List<Shape> bodyDiscs(DrawnPage page,
            List<OverlayRegistry.Owned> contributions) {
        List<Shape> discs = new ArrayList<>();
        Projection projection = null;
        ViewportMapping mapping = null;
        Rectangle2D paper = null;
        Shape sky = null;
        boolean bounded = false;
        for (OverlayRegistry.Owned owned : contributions) {
            if (owned.geometry() instanceof OverlayContribution.OblateBody
                    || owned.geometry() instanceof OverlayContribution.Satellite) {
                continue;
            }
            if (!(owned.geometry() instanceof OverlayContribution.Body body)) {
                continue;
            }
            if (projection == null) {
                projection = page.projection();
                mapping = new ViewportMapping(page);
                paper = ChartRenderer.paperOf(page.scene());
                PageRegion region = mapping.regionFor(page.scene().viewport(),
                        projection);
                sky = skyOf(region, paper);
                bounded = region.bounded();
            }
            Optional<juranometria.project.PlanePoint> projected =
                    projection.project(body.at());
            if (projected.isEmpty()) {
                continue;
            }
            Ellipse2D disc = discOn(mapping.toPixel(projected.get()),
                    radiusPx(projection, mapping, body.at(),
                            body.angularDiameterArcseconds() / 3600.0 / 2.0),
                    paper, sky, bounded);
            if (disc != null) {
                discs.add(inkOf(disc, body.lit() != null));
            }
        }
        for (JovianSystem system : systemsOn(page, contributions)) {
            if (system.mark().onPage()) {
                discs.add(inkOf(system.mark()));
            }
            for (SatelliteMark m : system.satellites()) {
                discs.add(inkOf(m));
            }
        }
        return List.copyOf(discs);
    }

    /** What the page drew for the bodies, laid out without painting. */
    public static List<BodyPlacement> bodyPlacements(DrawnPage page,
            List<OverlayRegistry.Owned> contributions,
            juranometria.render.ChartPalette palette,
            juranometria.project.PageWords words, List<Shape> reserved) {
        java.awt.image.BufferedImage scratch = new java.awt.image.BufferedImage(
                1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scratch.createGraphics();
        try {
            return paintBodies(g, page, contributions, palette, words, reserved,
                    referenceBoxes(page, contributions, words, reserved,
                            structure -> false));
        } finally {
            g.dispose();
        }
    }

    /**
     * Every box the reference layer's names and letters take on this
     * page - the line names, the cardinal letters and the landmarks'
     * and places' words - laid out exactly as {@link #paint} lays them
     * out, without painting, so a later layer can stay clear of them.
     */
    public static List<Rectangle2D> referenceBoxes(DrawnPage page,
            List<OverlayRegistry.Owned> contributions,
            juranometria.project.PageWords words, List<Shape> reserved,
            java.util.function.Predicate<juranometria.render.ChartStructure>
                    emphasized) {
        return pointsLaidOut(page, contributions, words, reserved).taken();
    }

    /**
     * Where this page's landmarks' and places' words go, laid out
     * exactly as {@link #paint} writes them - after the line names and
     * cardinal letters, and clear of every Solar System disc (#415
     * owner checkpoint). A word the page has no room for is absent.
     */
    public static List<NamePlacement> pointWordPlacements(DrawnPage page,
            List<OverlayRegistry.Owned> contributions,
            juranometria.project.PageWords words, List<Shape> reserved) {
        return pointsLaidOut(page, contributions, words, reserved).names();
    }

    /** Every box the layer takes, and the points' words among them. */
    private record PointsLaid(List<NamePlacement> names,
            List<Rectangle2D> taken) {
    }

    private static PointsLaid pointsLaidOut(DrawnPage page,
            List<OverlayRegistry.Owned> contributions,
            juranometria.project.PageWords words, List<Shape> reserved) {
        List<OverlayRegistry.Owned> reference = referenceOf(contributions);
        if (reference.isEmpty()) {
            return new PointsLaid(List.of(), List.of());
        }
        ChartScene scene = page.scene();
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        Rectangle2D paper = ChartRenderer.paperOf(scene);
        PageRegion region = mapping.regionFor(scene.viewport(), projection);
        Shape sky = skyOf(region, paper);
        boolean bounded = region.bounded();
        Laid laid = laidOut(page, contributions, words, reserved);
        List<Shape> discs = bodyDiscs(page, contributions);
        List<Rectangle2D> taken = new ArrayList<>(laid.taken());
        List<NamePlacement> names = new ArrayList<>();
        java.awt.image.BufferedImage scratch = new java.awt.image.BufferedImage(
                1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scratch.createGraphics();
        try {
            g.setFont(EquatorialGrid.GRID_LABEL_FONT);
            g.setClip(paper);
            for (OverlayRegistry.Owned owned : reference) {
                if (owned.geometry() instanceof OverlayContribution.Point point) {
                    Rectangle2D box = drawPoint(g, projection, mapping, paper,
                            sky, bounded, point, taken, reserved, discs,
                            juranometria.render.ChartPalette.WHITE_PAPER,
                            structure -> false);
                    if (box != null) {
                        names.add(new NamePlacement(owned.moduleId(),
                                point.accessibleName(), box));
                    }
                }
            }
        } finally {
            g.dispose();
        }
        return new PointsLaid(List.copyOf(names), List.copyOf(taken));
    }

    /**
     * The disc's radius on the page, by projecting its northern limb:
     * true scale through the page's own projection, which stretches a
     * disc towards a tangent page's corners exactly as it stretches
     * the sky there.
     */
    private static double radiusPx(Projection projection,
                                   ViewportMapping mapping,
                                   juranometria.chart.SkyPosition at,
                                   double halfDegrees) {
        PixelPoint centre = mapping.toPixel(projection.project(at).orElseThrow());
        juranometria.chart.SkyPosition limb = new juranometria.chart.SkyPosition(
                at.raDegrees(), Math.min(89.999, at.decDegrees() + halfDegrees));
        PixelPoint edge = mapping.toPixel(projection.project(limb).orElseThrow());
        return Math.hypot(edge.x() - centre.x(), edge.y() - centre.y());
    }

    /**
     * The name's box: the same tiers a cardinal letter is offered,
     * measured from the edge of the disc's ink - its limb and the
     * outer half of its ring (#416: at 1° the Sun's ring reaches 11 px
     * beyond the limb) - rather than a point, and refused
     * when none is clean of the paper's edge, the sky's edge, other
     * names and the catalogue's own ink.
     */
    private static Rectangle2D bodyNameBox(PixelPoint centre, double radius,
            double w, double h, Rectangle2D paper, Shape sky,
            boolean bounded, List<Rectangle2D> taken, List<Shape> reserved) {
        double gap = juranometria.render.CardinalLandmark.GAP + radius;
        List<List<Rectangle2D>> tiers = List.of(
                adjacent(centre, w, h, gap), diagonal(centre, w, h, gap),
                adjacent(centre, w, h, gap + juranometria.render.CardinalLandmark.GAP),
                diagonal(centre, w, h, gap + juranometria.render.CardinalLandmark.GAP));
        for (List<Rectangle2D> tier : tiers) {
            for (Rectangle2D candidate : tier) {
                if (paper.contains(candidate)
                        && (!bounded || sky.contains(candidate))
                        && clear(candidate, taken, reserved)) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
