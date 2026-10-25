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

    /**
     * The size the current scene was assembled at, so a resize event
     * for that same size is recognised as no change at all (#389).
     */
    private int assembledWidth = -1;
    private int assembledHeight = -1;
    /** The selected members, in membership order; ink only. */
    private List<String> selected = List.of();
    /** The lead identity, wearing the cross vocabulary's treatment. */
    private String highlighted;
    /**
     * The reader's transient emphasis (issue #361): the semantic
     * structures raised in ink - since #526 the Solar System labels as
     * one class among them. Presentation context beside
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
    /** Told whenever what may be emphasized could have changed (#525). */
    private final java.util.List<Runnable> availabilityListeners =
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
        // A closer page with no Mercury, Venus, Jupiter or Saturn on it says why zoom in
        // offers nothing more (#544); every other page carries no note.
        this.renderer.setTitleNote(this::closerFieldNote);
        // The modules' furniture (#259): none until a module offers
        // some, which is the released page.
        this.renderer.setFurniture(this::screenFurniture);
        this.assembler = assembler;
        installFurnitureInteraction();
        setOpaque(true);
        setPreferredSize(new Dimension(900, 700));
        setBackground(javax.swing.UIManager.getColor("Panel.background") != null
                ? javax.swing.UIManager.getColor("Panel.background")
                : java.awt.Color.LIGHT_GRAY);
        getAccessibleContext().setAccessibleName(words.chartName());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                // setSize queues this event; setViewState assembles at
                // once. A chart sized and then given a view therefore
                // receives, afterwards, a resize for the size its page
                // was just assembled at. Reassembling then asked the
                // catalogue again for an identical page and replaced
                // the scene object whenever the event thread got here
                // (#389). A real change of size still reassembles.
                if (scene != null && getWidth() == assembledWidth
                        && getHeight() == assembledHeight) {
                    return;
                }
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
        assembledWidth = getWidth();
        assembledHeight = getHeight();
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
    /** The Solar System bodies the last paint drew (#415). */
    private java.util.List<ReferenceInk.BodyPlacement>
            drawnBodies = java.util.List.of();

    /** The bodies the last paint drew, for journeys and the packaged image. */
    public java.util.List<ReferenceInk.BodyPlacement> renderedBodies() {
        return drawnBodies;
    }

    /** Told after a paint whose drawn bodies differ from the last paint's (#513). */
    private final java.util.List<Runnable> bodyListeners =
            new java.util.ArrayList<>();
    /** What the last paint's bodies were, for telling a change from a repaint. */
    private java.util.List<Object> bodySignature = java.util.List.of();

    /**
     * Told whenever a paint draws a different set of bodies, or the
     * same bodies for a different moment or at different places - the
     * seam a selected occurrence re-resolves through (#513).
     */
    public void onBodiesChange(Runnable listener) {
        if (listener != null) {
            bodyListeners.add(listener);
        }
    }

    /**
     * The selectable body a reader points at, if any (#513): on its
     * drawn ink or within the hit tolerance of its centre, the nearest
     * first. A body offered without a moment, or not selectable, is
     * never answered here - see {@link #bodyAt} for what its opaque ink
     * does to a click.
     */
    public java.util.Optional<ReferenceInk.BodyPlacement> selectableBodyAt(
            double x, double y) {
        ReferenceInk.BodyPlacement nearest = null;
        double best = Double.MAX_VALUE;
        for (ReferenceInk.BodyPlacement body : drawnBodies) {
            if (body.pointedAt(x, y, juranometria.render.ChartHitTest.TOLERANCE_PX)) {
                double d = Math.hypot(body.centre().x() - x, body.centre().y() - y);
                if (d < best) {
                    best = d;
                    nearest = body;
                }
            }
        }
        return java.util.Optional.ofNullable(nearest);
    }

    /**
     * The opaque body under a page point, if any (#414 ruling C4 and
     * C6): what it covers is covered, for the pointer as for the eye.
     */
    public java.util.Optional<ReferenceInk.BodyPlacement> bodyAt(double x,
                                                                double y) {
        for (ReferenceInk.BodyPlacement body : drawnBodies) {
            if (body.covers(x, y)) {
                return java.util.Optional.of(body);
            }
        }
        return java.util.Optional.empty();
    }

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

    /** The description, then each drawn body by its name on the page (#415). */
    static String withBodies(String base,
            java.util.List<ReferenceInk.BodyPlacement> bodies,
            juranometria.project.PageWords words) {
        if (bodies.isEmpty()) {
            return base;
        }
        StringBuilder said = new StringBuilder(base);
        for (ReferenceInk.BodyPlacement body : bodies) {
            said.append(' ').append(body.name() != null ? body.name()
                    : words.bodyName(body.identity()));
            // A moon says where it stands (#485): in front of Jupiter, or
            // in its shadow; a clear moon needs no word.
            if (body.state() != null) {
                said.append(", ").append(words.bodyState(body.state()));
            }
            // A mark drawn at its minimum says so (#484): it is a
            // cartographic symbol, never offered as the body's size.
            if (body.symbol()) {
                said.append(' ').append(words.bodySymbol(body.identity()));
            }
            said.append('.');
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
    /** The chart's one time and place (#612), held across every paint; null for a chart without one. */
    private juranometria.context.ChartContextHolder context;

    /**
     * Gives the chart the holder of its time and place (#612): every
     * paint holds it, so an installation during a paint is refused and
     * one paint reads one snapshot throughout.
     */
    /** The observer's zenith while Subdue below horizon is on, else null (#615). */
    private java.util.function.Supplier<juranometria.chart.SkyPosition> subdueZenith = () -> null;
    private juranometria.render.BelowHorizon subdueCached;
    private ChartScene subdueScene;
    private juranometria.chart.SkyPosition subdueAt;

    /**
     * Where Subdue below horizon reads the observer's zenith (#615):
     * null while the switch is off. Read once per page, from the
     * snapshot the paint or export holds.
     */
    public void setSubdueBelowHorizon(
            java.util.function.Supplier<juranometria.chart.SkyPosition> zenith) {
        this.subdueZenith = zenith == null ? () -> null : zenith;
        subdueCached = null;
        repaint();
    }

    /** The rule for this page: one instance per page and zenith, shared by screen and paper's layers. */
    public juranometria.render.BelowHorizon belowHorizon(ChartScene page) {
        juranometria.chart.SkyPosition zenith = subdueZenith.get();
        if (zenith == null || page == null) {
            return juranometria.render.BelowHorizon.NONE;
        }
        if (subdueCached == null || subdueScene != page || !zenith.equals(subdueAt)) {
            subdueCached = juranometria.render.BelowHorizon.of(zenith, page);
            subdueScene = page;
            subdueAt = zenith;
        }
        return subdueCached;
    }

    public void setContext(juranometria.context.ChartContextHolder context) {
        this.context = context;
    }

    /** The holder of the chart's time and place, or null. */
    public juranometria.context.ChartContextHolder context() {
        return context;
    }

    /** Runs an export or another whole-page reading over one snapshot of the context. */
    public <T> T holdingContext(java.util.function.Supplier<T> work) {
        return context == null ? work.get() : context.hold(work);
    }

    public juranometria.module.OverlayRegistry overlays() {
        return overlays;
    }

    // ---- the modules' furniture (#259, the rulings on #556) ----------

    /**
     * What furniture the modules are asking the page to carry. Empty
     * until one offers some, which is how the page stays the released
     * page with every module absent.
     */
    private final juranometria.module.FurnitureRegistry furniture =
            new juranometria.module.FurnitureRegistry();

    /** The registry a module offers its furniture to. */
    public juranometria.module.FurnitureRegistry furniture() {
        return furniture;
    }

    /**
     * A box the last paint drew: whose it is, and where the page put it.
     *
     * @param owned the module's offer, with the handler its action is
     *     handed back to
     * @param placed the corner, the box and the framed control
     */
    public record DrawnFurniture(juranometria.module.FurnitureRegistry.Owned owned,
                                 juranometria.render.ChartFurniture.Placed placed) {

        /** How the view report names it: the box's identity and its corner. */
        public String report() {
            return owned.furniture().identity() + " " + placed.corner().token();
        }
    }

    /** The furniture the last paint drew, in the order the page allocated it. */
    private java.util.List<DrawnFurniture> drawnFurniture = java.util.List.of();
    /** The key of the box whose control holds the keyboard, or null. */
    private String focusedAction;
    /** The key of the box whose control the pointer is over, or null. */
    private String hoveredAction;
    private final java.util.List<Runnable> furnitureListeners =
            new java.util.ArrayList<>();

    /** The furniture the last paint drew; empty on a page carrying none. */
    public java.util.List<DrawnFurniture> renderedFurniture() {
        return drawnFurniture;
    }

    /**
     * Told when a module offers or withdraws furniture - never during
     * painting. The page repaints, and whatever reports the furniture
     * asks again.
     */
    public void furnitureChanged() {
        boolean actions = furniture.collect().stream()
                .anyMatch(owned -> owned.furniture().action() != null);
        // The control's explanation on hover, from the toolkit's own
        // tooltip: registered only while some box offers a control, so
        // a chart carrying none is the chart it always was.
        if (actions) {
            javax.swing.ToolTipManager.sharedInstance().registerComponent(this);
        }
        for (Runnable listener : java.util.List.copyOf(furnitureListeners)) {
            listener.run();
        }
        repaint();
    }

    /** Told whenever a module offers or withdraws furniture. */
    public void onFurnitureChange(Runnable listener) {
        if (listener != null) {
            furnitureListeners.add(listener);
        }
    }

    /** The furniture as the screen draws it: each box with its framed control. */
    private java.util.List<juranometria.render.ChartFurniture> screenFurniture() {
        java.util.List<juranometria.render.ChartFurniture> boxes =
                new java.util.ArrayList<>();
        for (juranometria.module.FurnitureRegistry.Owned owned : furniture.collect()) {
            juranometria.module.Furniture box = owned.furniture();
            boxes.add(new juranometria.render.ChartFurniture(owned.key(),
                    box.heading(), box.lines(),
                    box.action() == null ? null : box.action().label()));
        }
        return java.util.List.copyOf(boxes);
    }

    /**
     * The furniture as paper carries it (ruling 6 on #556): no control,
     * since a control on paper cannot be pressed, and its honest
     * sentence in the control's place; with the sentence the sheet's
     * own description gains while the box is drawn.
     */
    public java.util.List<juranometria.sheet.ChartSheet.Furnished> paperFurniture() {
        java.util.List<juranometria.sheet.ChartSheet.Furnished> boxes =
                new java.util.ArrayList<>();
        for (juranometria.module.FurnitureRegistry.Owned owned : furniture.collect()) {
            juranometria.module.Furniture box = owned.furniture();
            java.util.List<String> lines = new java.util.ArrayList<>(box.lines());
            if (box.action() != null) {
                lines.add(box.action().paperLine());
            }
            boxes.add(new juranometria.sheet.ChartSheet.Furnished(
                    new juranometria.render.ChartFurniture(owned.key(), box.heading(),
                            lines, null),
                    box.described()));
        }
        return java.util.List.copyOf(boxes);
    }

    /** The drawn box whose control lies under a page point, if any. */
    public java.util.Optional<DrawnFurniture> furnitureActionAt(double x, double y) {
        for (DrawnFurniture drawn : drawnFurniture) {
            java.awt.Rectangle action = drawn.placed().action();
            if (action != null && x >= action.x && x <= action.x + action.width
                    && y >= action.y && y <= action.y + action.height) {
                return java.util.Optional.of(drawn);
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * Activates a drawn box's control: hands its identity back to the
     * module that offered it, and does nothing else. Every route - the
     * pointer, the keyboard, assistive technology - ends here.
     */
    public void activateFurniture(DrawnFurniture drawn) {
        juranometria.module.Furniture.Action action = drawn.owned().furniture().action();
        if (action != null) {
            drawn.owned().actions().accept(action.identity());
        }
    }

    /** The drawn box with this key, if the last paint drew it. */
    private java.util.Optional<DrawnFurniture> drawnFurniture(String key) {
        if (key == null) {
            return java.util.Optional.empty();
        }
        return drawnFurniture.stream()
                .filter(drawn -> drawn.owned().key().equals(key)).findFirst();
    }

    /** The first drawn box that offers a control, if any. */
    private java.util.Optional<DrawnFurniture> firstAction() {
        return drawnFurniture.stream()
                .filter(drawn -> drawn.placed().action() != null).findFirst();
    }

    /** Whether a drawn control holds the keyboard - for the journeys. */
    public boolean furnitureActionFocused() {
        return isFocusOwner() && drawnFurniture(focusedAction).isPresent();
    }

    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        return furnitureActionAt(event.getX(), event.getY() - pageOffsetY())
                .map(drawn -> drawn.owned().furniture().action().explanation())
                .orElse(null);
    }

    /**
     * The pointer and the keyboard reach a drawn control (ruling 7 on
     * #556). A click on it activates it; hovering marks it and explains
     * it; Tab from the chart gives it the keyboard - a focus ring drawn
     * by the chart - and Enter or Space activates it, and a second Tab
     * goes on to the next component as Tab always has. With no control
     * drawn, Tab and Shift Tab are the ordinary focus traversal.
     */
    private void installFurnitureInteraction() {
        java.awt.event.MouseAdapter pointer = new java.awt.event.MouseAdapter() {
            private java.awt.Point pressed;

            @Override
            public void mousePressed(java.awt.event.MouseEvent event) {
                pressed = javax.swing.SwingUtilities.isLeftMouseButton(event)
                        ? event.getPoint() : null;
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent event) {
                java.awt.Point at = pressed;
                pressed = null;
                if (at == null || event.getPoint().distance(at)
                        >= PanInteraction.DRAG_THRESHOLD_PX) {
                    return;
                }
                furnitureActionAt(at.x, at.y - pageOffsetY())
                        .ifPresent(ChartComponent.this::activateFurniture);
            }

            @Override
            public void mouseMoved(java.awt.event.MouseEvent event) {
                hover(furnitureActionAt(event.getX(), event.getY() - pageOffsetY())
                        .map(drawn -> drawn.owned().key()).orElse(null));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                hover(null);
            }
        };
        addMouseListener(pointer);
        addMouseMotionListener(pointer);
        setFocusTraversalKeysEnabled(false);
        javax.swing.InputMap keys = getInputMap(WHEN_FOCUSED);
        keys.put(javax.swing.KeyStroke.getKeyStroke("TAB"), "furniture.forward");
        keys.put(javax.swing.KeyStroke.getKeyStroke("ctrl TAB"), "furniture.leave");
        keys.put(javax.swing.KeyStroke.getKeyStroke("shift TAB"), "furniture.backward");
        keys.put(javax.swing.KeyStroke.getKeyStroke("ctrl shift TAB"),
                "furniture.leaveBackward");
        keys.put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "furniture.activate");
        keys.put(javax.swing.KeyStroke.getKeyStroke("SPACE"), "furniture.activate");
        getActionMap().put("furniture.forward", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                java.util.Optional<DrawnFurniture> first = firstAction();
                if (focusedAction == null && first.isPresent()) {
                    focus(first.get().owned().key());
                } else {
                    focus(null);
                    transferFocus();
                }
            }
        });
        getActionMap().put("furniture.leave", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                focus(null);
                transferFocus();
            }
        });
        getActionMap().put("furniture.backward", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (focusedAction != null) {
                    focus(null);
                } else {
                    transferFocusBackward();
                }
            }
        });
        getActionMap().put("furniture.leaveBackward", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                focus(null);
                transferFocusBackward();
            }
        });
        getActionMap().put("furniture.activate", new javax.swing.AbstractAction() {
            // Enabled only while a control holds the keyboard, so Enter
            // and Space reach whatever else answers them otherwise.
            @Override
            public boolean isEnabled() {
                return drawnFurniture(focusedAction).isPresent();
            }

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                drawnFurniture(focusedAction).ifPresent(
                        ChartComponent.this::activateFurniture);
            }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent event) {
                focus(null);
            }
        });
    }

    private void hover(String key) {
        if (!java.util.Objects.equals(hoveredAction, key)) {
            hoveredAction = key;
            repaint();
        }
    }

    private void focus(String key) {
        if (!java.util.Objects.equals(focusedAction, key)) {
            focusedAction = key;
            repaint();
        }
    }

    /**
     * The control's states, drawn by the chart over the drawn box: a
     * heavier frame under the pointer, and a focus ring around it while
     * it holds the keyboard. Neither is drawn on a page with no control.
     */
    private void paintFurnitureStates(Graphics2D g, juranometria.render.ChartPalette palette) {
        for (DrawnFurniture drawn : drawnFurniture) {
            java.awt.Rectangle action = drawn.placed().action();
            if (action == null) {
                continue;
            }
            String key = drawn.owned().key();
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (key.equals(hoveredAction)) {
                    g2.setColor(palette.textInk());
                    g2.setStroke(new java.awt.BasicStroke(2.0f));
                    g2.drawRoundRect(action.x, action.y, action.width, action.height,
                            5, 5);
                }
                if (key.equals(focusedAction) && isFocusOwner()) {
                    g2.setColor(palette.selectionInk());
                    g2.setStroke(new java.awt.BasicStroke(1.5f));
                    g2.drawRoundRect(action.x - 3, action.y - 3, action.width + 6,
                            action.height + 6, 9, 9);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** The drawn furniture, appended to what a reader who cannot see the page is told. */
    static String withFurniture(String base, java.util.List<DrawnFurniture> drawn) {
        if (drawn.isEmpty()) {
            return base;
        }
        StringBuilder said = new StringBuilder(base);
        for (DrawnFurniture box : drawn) {
            said.append(' ').append(box.owned().furniture().spoken());
        }
        return said.toString();
    }

    /**
     * The bodies whose presence on a page offers a closer field (#544,
     * ruled on #539, rulings 5 and 6, Jupiter added by the owner at the
     * #544 checkpoint): Mercury, Venus and Jupiter, named by the
     * application, which knows the module's identities; none until it
     * does, which is a chart that never offers one.
     */
    private java.util.Set<String> closerFieldBodies = java.util.Set.of();

    public void setCloserFieldBodies(java.util.Set<String> identities) {
        closerFieldBodies = java.util.Set.copyOf(identities);
        pageContentsChanged();
    }

    /**
     * Whether a closer page may be offered: a body named in
     * {@link #setCloserFieldBodies}, as the modules contribute it now,
     * lies on the page the candidate state would display at this
     * chart's page size. The question a closer step asks of the page
     * it would show, about its centre or about a pointer (#544).
     */
    public boolean offersCloserField(ChartViewState candidate) {
        if (candidate == null || !candidate.closerField()) {
            return false;
        }
        return closerBodyOn(candidate);
    }

    private boolean closerBodyOn(ChartViewState page) {
        if (closerFieldBodies.isEmpty()) {
            return false;
        }
        int width = scene != null ? scene.viewport().widthPx() : getWidth();
        int height = scene != null ? scene.viewport().heightPx() : getHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        juranometria.chart.ChartViewport viewport =
                new juranometria.chart.ChartViewport(page.centre(),
                        page.fieldWidthDegrees(), width, height);
        juranometria.project.DrawnPage drawn = juranometria.project.DrawnPage.of(
                new juranometria.chart.ChartScene(viewport, List.of(), List.of(),
                        "closer-field offer", page.limitingMagnitude(), null));
        juranometria.project.ViewportMapping mapping =
                new juranometria.project.ViewportMapping(drawn);
        for (juranometria.module.OverlayRegistry.Owned owned : overlays.collect()) {
            // Mercury and Venus are round bodies, Jupiter an oblate one, Saturn a ringed one.
            juranometria.chart.SkyPosition at = null;
            if (owned.geometry() instanceof juranometria.module.OverlayContribution.Body body
                    && closerFieldBodies.contains(body.identity())) {
                at = body.at();
            } else if (owned.geometry()
                    instanceof juranometria.module.OverlayContribution.OblateBody oblate
                    && closerFieldBodies.contains(oblate.identity())) {
                at = oblate.at();
            } else if (owned.geometry()
                    instanceof juranometria.module.OverlayContribution.RingedBody ringed
                    && closerFieldBodies.contains(ringed.identity())) {
                // Saturn, a ringed figure (#579; ruling 6 on #578).
                at = ringed.at();
            }
            if (at != null) {
                var plane = drawn.projection().project(at);
                if (plane.isEmpty()) {
                    continue;
                }
                juranometria.project.PixelPoint pixel = mapping.toPixel(plane.get());
                if (pixel.x() >= 0 && pixel.x() < width
                        && pixel.y() >= 0 && pixel.y() < height) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The title block's note for this page (#544), or null. */
    public String closerFieldNote(juranometria.chart.ChartScene drawnScene) {
        if (!viewState.closerField() || closerFieldBodies.isEmpty()
                || drawnScene.viewport().fieldWidthDegrees()
                        != viewState.fieldWidthDegrees()) {
            return null;
        }
        return closerBodyOn(viewState) ? null : words.closerFieldWithoutBody();
    }

    private final java.util.List<Runnable> pageContentsListeners =
            new java.util.ArrayList<>();

    /**
     * Told whenever what the page carries could have changed - a scene,
     * a module contributing, redrawing or withdrawing - so whatever
     * depends on which bodies stand on it, such as the closer step's
     * offer, asks again (#544). Never during painting.
     */
    public void onPageContentsChange(Runnable listener) {
        if (listener != null) {
            pageContentsListeners.add(listener);
        }
    }

    /** Announces that the page's contents may have changed (#544). */
    public void pageContentsChanged() {
        for (Runnable listener : java.util.List.copyOf(pageContentsListeners)) {
            listener.run();
        }
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
     * Whether this structure could take emphasis right now: the
     * reader has its layer switched on, or some module is contributing
     * geometry the chart maps to it.
     *
     * <p><strong>Navigation is not a choice (#537, amending #361).</strong>
     * Availability follows the reader's own switches, never what the
     * page in view happens to draw: zooming past the field where the
     * detail policy stops drawing figures or boundaries, panning, or a
     * globe that turns boundaries off for its own page leaves the
     * emphasis raised and still toggleable, and its ink returns when the
     * structure is drawn again. Switching the layer itself off, or a
     * module stopping its geometry, is an explicit choice and still
     * clears the emphasis at once - #361's point stands: no layer is
     * hidden with a forgotten emphasis attached to it.
     */
    public boolean emphasisAvailable(
            juranometria.render.ChartStructure structure) {
        if (structure == null) {
            return false;
        }
        juranometria.render.ChartOptions chosen = chartOptions;
        return switch (structure) {
            case EQUATORIAL_GRID -> chosen.equatorialGrid();
            case CONSTELLATION_BOUNDARIES -> chosen.constellationBoundaries();
            case CONSTELLATION_FIGURES -> chosen.constellationFigures();
            // The label class (#526) is choosable while some module
            // contributes a body or a satellite - the bodies layer's own
            // classification, so a later planet joins by contributing.
            case SOLAR_SYSTEM_LABELS -> overlays.collect().stream().anyMatch(owned ->
                    owned.geometry() instanceof juranometria.module.OverlayContribution.Body
                            || owned.geometry()
                                    instanceof juranometria.module.OverlayContribution.OblateBody
                            || owned.geometry()
                                    instanceof juranometria.module.OverlayContribution.Satellite);
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
        // Wherever availability can change, a host showing it is told
        // (#525): the Controller's boxes follow the chart, not a copy.
        for (Runnable listener : java.util.List.copyOf(availabilityListeners)) {
            listener.run();
        }
        // The same moments change which bodies stand on the page.
        pageContentsChanged();
    }

    /**
     * Told whenever what may be emphasized could have changed - an
     * options change, a scene change, a module contributing or
     * withdrawing (#525). Never during painting.
     */
    public void onEmphasisAvailabilityChange(Runnable listener) {
        if (listener != null) {
            availabilityListeners.add(listener);
        }
    }

    /**
     * How far right of the component's left edge the page is drawn:
     * the page always spans the component's width, so none - stated
     * here, and used by the paint, so a report of it is the offset
     * actually drawn (#372).
     */
    public int pageOffsetX() {
        return 0;
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

                /**
                 * The drawn furniture's controls, as actions of the
                 * chart (#259, ruling 7 on #556): named as the page
                 * names them, and reaching the same activation the
                 * pointer and the keyboard reach. None while no control
                 * is drawn.
                 */
                @Override
                public javax.accessibility.AccessibleAction getAccessibleAction() {
                    java.util.List<DrawnFurniture> controls = drawnFurniture.stream()
                            .filter(drawn -> drawn.placed().action() != null).toList();
                    if (controls.isEmpty()) {
                        return null;
                    }
                    return new javax.accessibility.AccessibleAction() {
                        @Override
                        public int getAccessibleActionCount() {
                            return controls.size();
                        }

                        @Override
                        public String getAccessibleActionDescription(int i) {
                            return i < 0 || i >= controls.size() ? null
                                    : controls.get(i).owned().furniture().action().label();
                        }

                        @Override
                        public boolean doAccessibleAction(int i) {
                            if (i < 0 || i >= controls.size()) {
                                return false;
                            }
                            activateFurniture(controls.get(i));
                            return true;
                        }
                    };
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
        // One paint, one snapshot of the chart's time and place (#612):
        // the holder refuses an installation until this paint returns.
        holdingContext(() -> {
            paintPage(g);
            return null;
        });
    }

    private void paintPage(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(pageOffsetX(), pageOffsetY());
            // The reference layer is handed to the renderer rather
            // than painted after it: a line of reference belongs
            // above the grid and below every mark, and that is the
            // only moment it can be laid down (#227). With no module
            // contributing, the layer is empty and the page is the
            // released page.
            juranometria.render.ChartOptions drawn = drawnOptions();
            renderer.render(g2, scene, drawn,
                    ChartRenderer.ReferenceLayer.subduing(
                            (layerG, layerScene, reserved) ->
                                    spokenDirections = ReferenceInk.paint(
                                            layerG, layerScene,
                                            overlays.collect(),
                                            drawn.palette(), words, reserved,
                                            emphasized, belowHorizon(layerScene)),
                            this::belowHorizon),
                    // The Solar System bodies (#415), above every
                    // catalogue mark and label: opaque, at true scale.
                    (layerG, layerScene, reserved) -> {
                        juranometria.project.DrawnPage page =
                                juranometria.project.DrawnPage.of(layerScene);
                        java.util.List<juranometria.module.OverlayRegistry.Owned>
                                offered = overlays.collect();
                        drawnBodies = ReferenceInk.paintBodies(layerG, page,
                                offered, drawn.palette(), words, reserved,
                                ReferenceInk.referenceBoxes(page, offered,
                                        words, reserved,
                                        juranometria.render.ChartStructure
                                                .membersOf(emphasized)),
                                juranometria.render.ChartStructure
                                        .membersOf(emphasized));
                    },
                    null, emphasized);
            // The furniture this paint drew (#259), exactly as the
            // renderer allocated it: what the pointer, the keyboard,
            // the description and the report all read.
            java.util.Map<String, juranometria.module.FurnitureRegistry.Owned> offered =
                    new java.util.HashMap<>();
            for (juranometria.module.FurnitureRegistry.Owned owned : furniture.collect()) {
                offered.put(owned.key(), owned);
            }
            java.util.List<DrawnFurniture> drew = new java.util.ArrayList<>();
            for (juranometria.render.ChartFurniture.Placed placed
                    : renderer.furnitureLayout(
                            ChartRenderer.TextMetrics.of(g2).labels(), scene, drawn)) {
                juranometria.module.FurnitureRegistry.Owned owned =
                        offered.get(placed.furniture().key());
                if (owned != null) {
                    drew.add(new DrawnFurniture(owned, placed));
                }
            }
            drawnFurniture = java.util.List.copyOf(drew);
            if (drawnFurniture(focusedAction).isEmpty()) {
                focusedAction = null;
            }
            paintFurnitureStates(g2, drawn.palette());
            // A paint that drew other bodies, or the same for another
            // moment or place, is told to whoever resolves a selected
            // occurrence against the page (#513); an unchanged repaint
            // is not.
            java.util.List<Object> signature = new java.util.ArrayList<>();
            for (ReferenceInk.BodyPlacement body : drawnBodies) {
                signature.add(java.util.List.of(body.identity(),
                        java.util.Objects.requireNonNullElse(body.moment(), ""),
                        body.selectable(), body.centre()));
            }
            if (!signature.equals(bodySignature)) {
                bodySignature = signature;
                for (Runnable listener : java.util.List.copyOf(bodyListeners)) {
                    listener.run();
                }
            }
            // What a reader who cannot see the page is told follows
            // what the page actually rendered (#359): the base
            // description, and then every cardinal direction this
            // paint accepted, each by its full localized spoken name.
            // A direction the page omitted is not spoken, because a
            // description of ink that is not there would be the lie
            // accessibility exists to prevent.
            getAccessibleContext().setAccessibleDescription(withFurniture(
                    withBodies(withDirections(describe(scene), spokenDirections),
                            drawnBodies, words), drawnFurniture));
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
