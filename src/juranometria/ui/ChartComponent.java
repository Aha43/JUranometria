package juranometria.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartRenderer;

/**
 * The atlas page. Scenes are assembled by the {@link SceneAssembler} when
 * the view state or the component size changes; painting only renders the
 * current scene and performs no catalogue query. Repainting an unchanged
 * view reuses the assembled scene untouched.
 *
 * The page's height is capped by the assembler's coverage rule: in a
 * window taller than the bundled data can honestly fill, the page is
 * letterboxed on the theme's neutral surface instead of showing silently
 * sparse sky.
 */
public final class ChartComponent extends JComponent {

    private final juranometria.project.PageWords words;
    private final ChartRenderer renderer;
    private SceneAssembler assembler;
    private ChartViewState viewState = ChartViewState.DEFAULT;
    private juranometria.render.ChartOptions chartOptions =
            juranometria.render.ChartOptions.DEFAULTS;
    private ChartScene scene;
    /** The selected members, in membership order; ink only. */
    private List<String> selected = List.of();
    /** The lead identity, wearing the cross vocabulary's treatment. */
    private String highlighted;
    /**
     * The reader's transient emphasis (issue #361): at most one
     * semantic structure raised in ink. Presentation context beside
     * the options, never inside them and never persisted - it
     * survives panning and zooming and dies with the session. It is
     * cleared the moment its structure stops being available, so no
     * invisible latent mode outlives a hidden layer or a detached
     * module.
     */
    private final java.util.EnumSet<juranometria.render.ChartStructure>
            emphasized = java.util.EnumSet.noneOf(
                    juranometria.render.ChartStructure.class);
    private final java.util.List<Runnable> emphasisListeners =
            new java.util.ArrayList<>();
    private final java.util.List<Runnable> sceneListeners =
            new java.util.ArrayList<>();

    /**
     * The chart, drawn and described in a stated language (#350).
     *
     * @param words what this page is called. Held only to pass on -
     *     the component does not read a preference, and the language
     *     it was given is the one the exported sheet will use,
     *     because both come from the same resolution.
     */
    public ChartComponent(SceneAssembler assembler,
                          juranometria.project.PageWords words) {
        if (assembler == null) {
            throw new IllegalArgumentException("scene assembler must not be null");
        }
        if (words == null) {
            throw new IllegalArgumentException("a chart says what it"
                    + " is in some language (#350)");
        }
        this.words = words;
        this.renderer = new ChartRenderer(StarSizePolicy.DEFAULT, words);
        this.assembler = assembler;
        setOpaque(true);
        setPreferredSize(new Dimension(900, 700));
        setBackground(javax.swing.UIManager.getColor("Panel.background") != null
                ? javax.swing.UIManager.getColor("Panel.background")
                : java.awt.Color.LIGHT_GRAY);
        getAccessibleContext().setAccessibleName(words.chartName());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                assembleScene();
            }
        });
    }

    /** Adopts a new view state, assembles its scene, and repaints. */
    public void setViewState(ChartViewState viewState) {
        if (viewState == null) {
            throw new IllegalArgumentException("view state must not be null");
        }
        this.viewState = viewState;
        assembleScene();
    }

    public ChartViewState viewState() {
        return viewState;
    }

    /**
     * What assembled this page, for whoever must reproduce it.
     *
     * <p>Export exists to put on paper the page the reader is looking
     * at. It used to build its own scene from the application's
     * assembler, which was the same object while there was only one
     * language - and would quietly stop being the same object the
     * moment a reader chose a different sky language, printing Latin
     * names under a Norwegian screen.
     *
     * <p>Asking the chart what drew it makes screen and paper share
     * one language state by construction, rather than by two call
     * sites being careful to consult the same source (#348).
     */
    public SceneAssembler assembler() {
        return assembler;
    }

    /**
     * Draws this page again, from an assembler that names the sky
     * differently (#348).
     *
     * <p>Reconstruction, not repaint. Names are resolved into a scene
     * when it is assembled, so a page already built cannot be made to
     * show a language it was not built in - asking for a repaint
     * would leave the old names on screen and look like the setting
     * had been ignored.
     *
     * <p>The view state is untouched: choosing a language is not
     * navigation. The reader stays exactly where they were and the
     * sky is relabelled around them.
     */
    public void setAssembler(SceneAssembler assembler) {
        if (assembler == null) {
            throw new IllegalArgumentException(
                    "scene assembler must not be null");
        }
        this.assembler = assembler;
        assembleScene();
    }

    /**
     * Adopts the reader's chart options and repaints. Options are
     * presentation state consumed by the renderer: no scene assembly,
     * no catalogue or geography query, no navigation change
     * (docs/decisions/chart-options.md).
     */
    public void setChartOptions(juranometria.render.ChartOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("chart options must not be null");
        }
        this.chartOptions = options;
        for (Runnable listener : List.copyOf(optionsListeners)) {
            listener.run();
        }
        revalidateEmphasis();
        repaint();
    }

    private final java.util.List<Runnable> optionsListeners =
            new java.util.ArrayList<>();

    /**
     * Told when the reader changes what the chart draws.
     *
     * <p>Separate from {@link #onSceneChange}: the options do not
     * change the assembled scene, only what is made of it. But they
     * do change what can be <em>seen</em>, and anything that reports
     * on visibility - the page inventory above all - is stale the
     * moment a family is switched off. Without this the table went
     * on calling a hidden galaxy "drawn" (issue #217).
     */
    public void onChartOptionsChange(Runnable listener) {
        if (listener != null) {
            optionsListeners.add(listener);
        }
    }

    public juranometria.render.ChartOptions chartOptions() {
        return chartOptions;
    }

    /**
     * The options this page is actually drawn with (Sprint 32, issue
     * #329).
     *
     * <p>The reader's own, as the page has them: a globe turns
     * constellation boundaries off, because the gate measured them
     * costing more ink than any other layer at a limb carrying half
     * the sky (docs/decisions/celestial-globe.md). Nothing is stored,
     * so the reader's switches are exactly as they left them.
     *
     * <p>Kept apart from {@link #chartOptions()} on purpose. That one
     * is what the reader chose and what the dialog edits; this one is
     * what the page shows, and the inventory has to agree with the
     * ink rather than with the switches - an inventory listing a
     * boundary the page does not draw is the fault #217 was about,
     * the other way round.
     */
    public juranometria.render.ChartOptions drawnOptions() {
        return scene == null ? chartOptions
                : chartOptions.onPage(
                        juranometria.project.DrawnPage.of(scene));
    }

    ChartScene scene() {
        return scene;
    }

    /** Told whenever a new page has been assembled. */
    public void onSceneChange(Runnable listener) {
        if (listener != null) {
            sceneListeners.add(listener);
        }
    }

    /** The page as assembled, for consumers outside this package. */
    public ChartScene currentScene() {
        return scene;
    }

    /**
     * The object the reader has selected, marked on the page (issue
     * #170). Presentation only: it changes no view state, assembles
     * no scene, and asks the catalogue nothing.
     */
    public void setHighlightedObject(String catalogueId) {
        setWorkingSelection(catalogueId == null ? List.of()
                : List.of(catalogueId), catalogueId);
    }

    /**
     * The reader's working selection, as ink (issue #261,
     * docs/decisions/working-selection.md): the chart's existing
     * selection ring around each selected member whose symbol is
     * drawn - the renderer already answers "is it drawn" per
     * identity, so an undrawn or off-page member simply leaves no
     * ring - and the lead treatment for the cross vocabulary.
     * Presentation only, exactly as the single highlight was: no
     * view state, no scene, no catalogue query.
     */
    public void setWorkingSelection(List<String> members, String lead) {
        List<String> next = members == null ? List.of()
                : List.copyOf(members);
        if (selected.equals(next)
                && java.util.Objects.equals(highlighted, lead)) {
            return;
        }
        this.selected = next;
        this.highlighted = lead;
        repaint();
    }

    /** The selected members, in membership order; package-visible
     * so the emphasis contracts can hold the independence rule. */
    List<String> selectedMembers() {
        return selected;
    }

    private void assembleScene() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        int pageHeight = Math.min(getHeight(), assembler.maxPageHeightPx(
                viewState.projection(), viewState.centre(),
                viewState.fieldWidthDegrees(), getWidth()));
        scene = assembler.assemble(viewState, getWidth(), pageHeight);
        getAccessibleContext().setAccessibleDescription(describe(scene));
        // Consumers that describe the page - the inspector - need to
        // know it changed, because what the page can say about the
        // selection changes with it (issue #170).
        revalidateEmphasis();
        for (Runnable listener : java.util.List.copyOf(sceneListeners)) {
            listener.run();
        }
        repaint();
    }

    /**
     * What this page is, for a reader who cannot see it (Sprint 30,
     * issue #300).
     *
     * <p>The same four things the title block states and the exported
     * sheet's metadata states, in the same order: what part of the
     * sky, where its centre is, how wide it is, and
     * <strong>which projection drew it</strong>. A page had an
     * accessible name and nothing else before this - "Star chart",
     * which is true of every page the atlas can draw and therefore
     * says nothing about the one being read.
     *
     * <p>The projection is named on every page, not only the wide
     * ones. A description is read aloud rather than looked at, so
     * there is no page whose silence a reader could compare against
     * another page's word.
     */
    /**
     * The words this chart was given, for what draws over it.
     *
     * <p>Selection ink and sheet ink build renderers of their own and
     * must not resolve a language to do it: they take the one the
     * chart they are drawing on already has, so nothing downstream
     * can end up speaking differently from the page underneath
     * (#350).
     */
    public juranometria.project.PageWords words() {
        return words;
    }

    /** The directions the last paint accepted, for the description. */
    private java.util.List<ReferenceInk.DirectionPlacement>
            spokenDirections = java.util.List.of();

    /** The directions the last paint accepted; package-visible so
     * the landmark journeys can hold letter and position. */
    java.util.List<ReferenceInk.DirectionPlacement> renderedDirections() {
        return spokenDirections;
    }

    /**
     * The accessible description: the page, then its rendered
     * cardinal directions in the page's own language (#359).
     */
    static String withDirections(String base,
            java.util.List<ReferenceInk.DirectionPlacement> directions) {
        if (directions.isEmpty()) {
            return base;
        }
        StringBuilder said = new StringBuilder(base);
        for (ReferenceInk.DirectionPlacement placed : directions) {
            said.append(' ').append(placed.spokenName()).append('.');
        }
        return said.toString();
    }

    private String describe(ChartScene scene) {
        // One source for what a page says it is, shared with the
        // title block and the exported sheet (#301), and now in one
        // language with them too (#350).
        return juranometria.project.DrawnPage.of(scene).describe(words);
    }

    /** Top of the paper page inside the (possibly letterboxed) canvas. */
    /**
     * What the modules are offering to ink. Empty until one attaches,
     * which is how the chart draws its ordinary page with every
     * module absent.
     */
    private final juranometria.module.OverlayRegistry overlays =
            new juranometria.module.OverlayRegistry();

    /** The registry a module contributes its geometry to. */
    public juranometria.module.OverlayRegistry overlays() {
        return overlays;
    }

    /**
     * Toggles one semantic structure's membership in the raised set
     * (multiple-emphasis ruling): choosing a structure toggles only
     * that structure, and an unavailable structure cannot enter.
     * Selecting and emphasizing stay independent: this touches no
     * selection, and no selection touches this.
     */
    public void toggleEmphasis(
            juranometria.render.ChartStructure structure) {
        if (structure == null) {
            return;
        }
        if (emphasized.contains(structure)) {
            emphasized.remove(structure);
        } else if (emphasisAvailable(structure)) {
            emphasized.add(structure);
        } else {
            return;
        }
        emphasisChanged();
    }

    /** Normal: the whole set settles at once. */
    public void clearEmphasis() {
        if (emphasized.isEmpty()) {
            return;
        }
        emphasized.clear();
        emphasisChanged();
    }

    /** The raised structures, in the enum's own order; never null. */
    public java.util.Set<juranometria.render.ChartStructure>
            emphasizedSet() {
        return java.util.Collections.unmodifiableSet(emphasized);
    }

    private void emphasisChanged() {
        for (Runnable listener
                : java.util.List.copyOf(emphasisListeners)) {
            listener.run();
        }
        repaint();
    }

    /** Told when emphasis changes, including a forced settle. */
    public void onEmphasisChange(Runnable listener) {
        if (listener != null) {
            emphasisListeners.add(listener);
        }
    }

    /**
     * Whether this structure could take emphasis right now: its
     * layer is switched on and drawn at this field, or some module
     * is contributing geometry the chart maps to it.
     */
    public boolean emphasisAvailable(
            juranometria.render.ChartStructure structure) {
        if (structure == null) {
            return false;
        }
        juranometria.render.ChartOptions drawn = drawnOptions();
        var policy = new juranometria.render.GeographyDetailPolicy(
                viewState.fieldWidthDegrees());
        return switch (structure) {
            case EQUATORIAL_GRID -> drawn.equatorialGrid();
            case CONSTELLATION_BOUNDARIES ->
                    drawn.constellationBoundaries()
                            && policy.boundariesDrawn();
            case CONSTELLATION_FIGURES ->
                    drawn.constellationFigures() && policy.figuresDrawn();
            default -> overlays.collect().stream().anyMatch(owned ->
                    juranometria.render.ChartStructure
                            .ofIdentity(owned.geometry().identity())
                            .filter(s -> s == structure).isPresent());
        };
    }

    /**
     * Settles the page if the emphasized structure has become
     * unavailable. Called wherever availability can change - an
     * options change, a scene change, a module contributing or
     * withdrawing - and never during painting.
     */
    void revalidateEmphasis() {
        boolean changed = emphasized.removeIf(
                structure -> !emphasisAvailable(structure));
        if (changed) {
            emphasisChanged();
        }
    }

    public int pageOffsetY() {
        return scene == null ? 0
                : (getHeight() - scene.viewport().heightPx()) / 2;
    }

    /** Whether a component point lies on the paper page, not the chrome. */
    boolean isOnPaper(java.awt.Point point) {
        if (scene == null) {
            return false;
        }
        int top = pageOffsetY();
        return point.x >= 0 && point.x < scene.viewport().widthPx()
                && point.y >= top
                && point.y < top + scene.viewport().heightPx();
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        // Plain JComponent subclasses supply no accessible context of
        // their own; provide one so the chart is exposed as a canvas.
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.CANVAS;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (scene == null || scene.viewport().widthPx() != getWidth()
                || scene.viewport().heightPx() != Math.min(getHeight(),
                        assembler.maxPageHeightPx(viewState.projection(),
                                viewState.centre(),
                                viewState.fieldWidthDegrees(),
                                getWidth()))) {
            // A resize event is already on its way for this geometry; skip
            // the stale frame rather than querying inside painting.
            return;
        }
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(0, pageOffsetY());
            // The reference layer is handed to the renderer rather
            // than painted after it: a line of reference belongs
            // above the grid and below every mark, and that is the
            // only moment it can be laid down (#227). With no module
            // contributing, the layer is empty and the page is the
            // released page.
            juranometria.render.ChartOptions drawn = drawnOptions();
            renderer.render(g2, scene, drawn,
                    (layerG, layerScene, reserved) ->
                            spokenDirections = ReferenceInk.paint(
                                    layerG, layerScene,
                                    overlays.collect(),
                                    drawn.palette(), words, reserved,
                                    emphasized),
                    null, emphasized);
            // What a reader who cannot see the page is told follows
            // what the page actually rendered (#359): the base
            // description, and then every cardinal direction this
            // paint accepted, each by its full localized spoken name.
            // A direction the page omitted is not spoken, because a
            // description of ink that is not there would be the lie
            // accessibility exists to prevent.
            getAccessibleContext().setAccessibleDescription(
                    withDirections(describe(scene), spokenDirections));
            // One ring per selected drawn member (issue #261): the
            // renderer draws nothing for an identity the page does
            // not draw, so an on-page undrawn member is left to its
            // cross and an off-page member leaves no ink at all -
            // never both treatments for one object.
            for (String member : selected) {
                renderer.drawSelectionHighlight(g2, scene, drawn, member);
            }
            // After the chart, never inside it: working crosses are
            // an interaction overlay and not catalogue symbols, so
            // ordinary and reference rendering are untouched by them
            // - and identical when nothing is marked, because a
            // module with nothing to say contributes nothing.
            WorkingCrossInk.paint(g2, juranometria.project.DrawnPage.of(scene), overlays.collect(), highlighted,
                    chartOptions.palette());
        } finally {
            g2.dispose();
        }
    }
}
