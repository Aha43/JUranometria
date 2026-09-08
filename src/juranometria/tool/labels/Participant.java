package juranometria.tool.labels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import juranometria.chart.ChartScene;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SceneGeography;
import juranometria.chart.Star;
import juranometria.geo.GeoSegment;
import juranometria.render.ChartOptions;

/**
 * One piece of ink the page draws, and how to take it away again
 * (Sprint 31, issue #310).
 *
 * <p>A collision has two participants and this study will not report
 * one it cannot name both of. Naming them means being able to withhold
 * each independently, which is the whole of this class: every
 * participant is a small, honest surgery on the scene or the reader's
 * options, after which the production renderer draws the page again
 * and the difference is that participant.
 *
 * <p>The surgeries that matter are the two that separate a star's
 * <em>name</em> from a star's <em>disc</em>. Removing the star takes
 * both; setting its identity to null takes only the name, because the
 * label policy has nothing to say about a star the identity pack does
 * not know, while the mark is drawn from position and magnitude
 * alone. That pair is what lets the Nunki fixture be reported as three
 * pieces rather than as overlapping ink.
 */
public record Participant(Family family, String id, String description,
                          java.awt.geom.Rectangle2D where,
                          Participant alsoTaken) {

    /** A participant whose withholding takes it and nothing else. */
    public Participant(Family family, String id, String description) {
        this(family, id, description, null, null);
    }

    @Override
    public String toString() {
        return family + ":" + id;
    }

    /** The text and non-text families a page draws. */
    public enum Family {
        STAR_LABEL("star name, letter or number"),
        STAR_DISC("a star's own mark"),
        CONSTELLATION_NAME("constellation name"),
        DEEP_SKY_LABEL("deep-sky label"),
        DEEP_SKY_SYMBOL("deep-sky symbol"),
        FIGURE_LINE("constellation figure line"),
        BOUNDARY_LINE("constellation boundary"),
        // The grid draws its lines and its edge notation under one
        // switch, and the reference layer its curves and their names
        // under another, so neither can be withheld apart from the
        // other and this study will not pretend otherwise. Naming
        // them as one participant each is the honest report, and
        // publishing their decisions the way star labels are
        // published (issue #154) is a contract for #313.
        SELECTION_RING("a working selection's ring"),
        GRID_INK("equatorial grid line or its edge notation"),
        TITLE_BLOCK("title block"),
        MAGNITUDE_KEY("magnitude key"),
        REFERENCE_INK("meridian, horizon or ecliptic line or name");

        private final String prose;

        Family(String prose) {
            this.prose = prose;
        }

        public String prose() {
            return prose;
        }

        /** Whether this family is text a placement policy could move. */
        public boolean isText() {
            return this == STAR_LABEL || this == CONSTELLATION_NAME
                    || this == DEEP_SKY_LABEL;
        }
    }

    /**
     * What withholding this participant actually changes.
     *
     * <p>A star's label or disc is that star's alone, but a family the
     * reader switches - deep-sky labels, the grid, the reference layer
     * - comes off in one piece, and a page painted without it is the
     * same page for every member. Naming the surgery rather than the
     * participant is what stops a wide chart repainting itself once
     * per deep-sky object.
     */
    public String withholdKey() {
        return switch (family) {
            case DEEP_SKY_LABEL, BOUNDARY_LINE, GRID_INK, TITLE_BLOCK,
                    MAGNITUDE_KEY, REFERENCE_INK -> family.name();
            default -> family.name() + ":" + id;
        };
    }

    /** This participant, localised to the box its family draws in. */
    public Participant within(java.awt.geom.Rectangle2D box) {
        return new Participant(family, id, description, box, alsoTaken);
    }

    /**
     * This participant, knowing that withholding it takes something
     * else with it that must be subtracted back out.
     *
     * <p>Removing a star from the scene removes its <em>name</em> as
     * well as its mark, and this study found that out the hard way:
     * the paint-order check reported a disc covering three times more
     * of a label than the label covered of it, which is impossible for
     * ink drawn underneath. What the disc's ink actually held was that
     * star's own name. A collision reported as "one star's name across
     * another star's disc" would sometimes have been one name across
     * another <em>name</em> - a different defect, wearing the
     * fixture's clothes.
     *
     * <p>So a mark's ink is measured as everything its removal takes,
     * less everything its label's removal takes. Two withholdings, one
     * subtraction, and the two pieces the gate insists on separating
     * are separated.
     */
    public Participant alsoTaking(Participant other) {
        return new Participant(family, id, description, where, other);
    }

    /** A page with this participant withheld and nothing else changed. */
    public Page withheldFrom(Page page) {
        if (page.isCandidate() && family.isText()) {
            // On a candidate page the text is the candidate's, so
            // withholding it is leaving it out rather than taking the
            // reader's identity pack away from the star.
            return page.withoutPlaced(family, id);
        }
        return switch (family) {
            case STAR_LABEL -> page.withScene(anonymous(page.scene(), id));
            case STAR_DISC -> page.withScene(withoutStar(page.scene(), id));
            case CONSTELLATION_NAME ->
                    page.withScene(withoutName(page.scene(), id));
            case DEEP_SKY_SYMBOL ->
                    page.withScene(withoutDeepSky(page.scene(), id));
            case FIGURE_LINE ->
                    page.withScene(withoutFigure(page.scene(), id));
            // The families the reader switches, which is also the only
            // handle production offers for them. A label of one of
            // these is localised afterwards by the box the renderer
            // publishes for it, so the ink is still one participant's.
            case DEEP_SKY_LABEL -> page.withOptions(
                    Options.deepSkyLabels(page.options(), false));
            case BOUNDARY_LINE -> page.withOptions(
                    Options.boundaries(page.options(), false));
            case GRID_INK -> page.withOptions(
                    Options.grid(page.options(), false));
            case TITLE_BLOCK -> page.withOptions(
                    Options.titleBlock(page.options(), false));
            case MAGNITUDE_KEY -> page.withOptions(
                    Options.magnitudeKey(page.options(), false));
            case REFERENCE_INK -> page.withoutOverlays();
            case SELECTION_RING -> page.withoutSelected(id);
        };
    }

    /** The same scene with one star's identity forgotten. */
    private static ChartScene anonymous(ChartScene scene, String starId) {
        List<Star> stars = new ArrayList<>();
        for (Star star : scene.stars()) {
            stars.add(star.id().equals(starId)
                    ? new Star(star.id(), star.position(), star.magnitude())
                    : star);
        }
        return withStars(scene, stars);
    }

    private static ChartScene withoutStar(ChartScene scene, String starId) {
        List<Star> stars = new ArrayList<>();
        for (Star star : scene.stars()) {
            if (!star.id().equals(starId)) {
                stars.add(star);
            }
        }
        return withStars(scene, stars);
    }

    private static ChartScene withStars(ChartScene scene, List<Star> stars) {
        return new ChartScene(scene.viewport(), stars,
                scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), scene.targetIdentity(),
                scene.geography());
    }

    private static ChartScene withoutDeepSky(ChartScene scene, String dsoId) {
        List<DeepSkyObject> kept = new ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (!dso.id().equals(dsoId)) {
                kept.add(dso);
            }
        }
        return new ChartScene(scene.viewport(), scene.stars(), kept,
                scene.title(), scene.limitingMagnitude(),
                scene.targetIdentity(), scene.geography());
    }

    private static ChartScene withoutName(ChartScene scene,
                                          String constellationId) {
        Map<String, String> names =
                new LinkedHashMap<>(scene.geography().latinNames());
        names.remove(constellationId);
        return withGeography(scene, new SceneGeography(
                scene.geography().figureSegments(),
                scene.geography().boundarySegments(), names));
    }

    private static ChartScene withoutFigure(ChartScene scene,
                                            String constellationId) {
        List<GeoSegment> kept = new ArrayList<>();
        for (GeoSegment segment : scene.geography().figureSegments()) {
            if (!segment.constellationId().equals(constellationId)) {
                kept.add(segment);
            }
        }
        return withGeography(scene, new SceneGeography(kept,
                scene.geography().boundarySegments(),
                scene.geography().latinNames()));
    }

    private static ChartScene withGeography(ChartScene scene,
                                            SceneGeography geography) {
        return new ChartScene(scene.viewport(), scene.stars(),
                scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), scene.targetIdentity(),
                geography);
    }

    /** One switch of the reader's chart options, nothing else moved. */
    static final class Options {

        private Options() {
        }

        static ChartOptions deepSkyLabels(ChartOptions on, boolean value) {
            return new ChartOptions(on.deepSkyObjects(), value,
                    on.constellationFigures(), on.constellationBoundaries(),
                    on.constellationNames(), on.starNames(),
                    on.bayerLetters(), on.flamsteedNumbers(),
                    on.equatorialGrid(), on.titleBlock(), on.magnitudeKey(),
                    on.galaxies(), on.openClusters(), on.globularClusters(),
                    on.nebulae(), on.planetaryNebulae(), on.palette());
        }

        static ChartOptions deepSkyObjects(ChartOptions on, boolean value) {
            return new ChartOptions(value, on.deepSkyLabels(),
                    on.constellationFigures(), on.constellationBoundaries(),
                    on.constellationNames(), on.starNames(),
                    on.bayerLetters(), on.flamsteedNumbers(),
                    on.equatorialGrid(), on.titleBlock(), on.magnitudeKey(),
                    on.galaxies(), on.openClusters(), on.globularClusters(),
                    on.nebulae(), on.planetaryNebulae(), on.palette());
        }

        static ChartOptions boundaries(ChartOptions on, boolean value) {
            return new ChartOptions(on.deepSkyObjects(), on.deepSkyLabels(),
                    on.constellationFigures(), value,
                    on.constellationNames(), on.starNames(),
                    on.bayerLetters(), on.flamsteedNumbers(),
                    on.equatorialGrid(), on.titleBlock(), on.magnitudeKey(),
                    on.galaxies(), on.openClusters(), on.globularClusters(),
                    on.nebulae(), on.planetaryNebulae(), on.palette());
        }

        static ChartOptions grid(ChartOptions on, boolean value) {
            return new ChartOptions(on.deepSkyObjects(), on.deepSkyLabels(),
                    on.constellationFigures(), on.constellationBoundaries(),
                    on.constellationNames(), on.starNames(),
                    on.bayerLetters(), on.flamsteedNumbers(), value,
                    on.titleBlock(), on.magnitudeKey(), on.galaxies(),
                    on.openClusters(), on.globularClusters(), on.nebulae(),
                    on.planetaryNebulae(), on.palette());
        }

        static ChartOptions titleBlock(ChartOptions on, boolean value) {
            return new ChartOptions(on.deepSkyObjects(), on.deepSkyLabels(),
                    on.constellationFigures(), on.constellationBoundaries(),
                    on.constellationNames(), on.starNames(),
                    on.bayerLetters(), on.flamsteedNumbers(),
                    on.equatorialGrid(), value, on.magnitudeKey(),
                    on.galaxies(), on.openClusters(), on.globularClusters(),
                    on.nebulae(), on.planetaryNebulae(), on.palette());
        }

        static ChartOptions magnitudeKey(ChartOptions on, boolean value) {
            return new ChartOptions(on.deepSkyObjects(), on.deepSkyLabels(),
                    on.constellationFigures(), on.constellationBoundaries(),
                    on.constellationNames(), on.starNames(),
                    on.bayerLetters(), on.flamsteedNumbers(),
                    on.equatorialGrid(), on.titleBlock(), value,
                    on.galaxies(), on.openClusters(), on.globularClusters(),
                    on.nebulae(), on.planetaryNebulae(), on.palette());
        }

        static ChartOptions starLabels(ChartOptions on, boolean value) {
            return new ChartOptions(on.deepSkyObjects(), on.deepSkyLabels(),
                    on.constellationFigures(), on.constellationBoundaries(),
                    on.constellationNames(), value, value, value,
                    on.equatorialGrid(), on.titleBlock(), on.magnitudeKey(),
                    on.galaxies(), on.openClusters(), on.globularClusters(),
                    on.nebulae(), on.planetaryNebulae(), on.palette());
        }

        static ChartOptions constellationNames(ChartOptions on,
                                               boolean value) {
            return new ChartOptions(on.deepSkyObjects(), on.deepSkyLabels(),
                    on.constellationFigures(), on.constellationBoundaries(),
                    value, on.starNames(), on.bayerLetters(),
                    on.flamsteedNumbers(), on.equatorialGrid(),
                    on.titleBlock(), on.magnitudeKey(), on.galaxies(),
                    on.openClusters(), on.globularClusters(), on.nebulae(),
                    on.planetaryNebulae(), on.palette());
        }
    }
}
