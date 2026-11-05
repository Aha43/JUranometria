package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import juranometria.app.Atlas;
import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;
import juranometria.meridian.MeridianModule;
import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.Furniture;
import juranometria.page.PageEntry;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartFurniture;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.SymbolFamily;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.ChartViewController;
import juranometria.ui.Shortcuts;
import juranometria.ui.SheetInk;
import juranometria.ui.companion.CompanionSection;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * The measured study for the 6.3.0 road (issue #690, road #689): guide
 * the chart precisely, then keep the result understandable on the page.
 *
 * <p><b>Pointer navigation.</b> The four proposed drag modes and the
 * one-drag modifier ({@link PointerDragSketch}) are run through the
 * production grab solver ({@code PanSolver.solveCentre}) on the ordinary
 * chart, a polar page, the stereographic overview and the celestial
 * globe, along two recorded gestures, waypoint by waypoint as the
 * production drag solves them. What is measured is what the reader would
 * see: how the centre moves, whether the grabbed sky stands exactly
 * under the constrained pixel, and how far a star off the press row
 * moves vertically during a horizontal-only drag - which is what makes
 * the constraint a statement about the screen, not about the sky.
 *
 * <p><b>Selected-object furniture.</b> A box describing the last selected
 * object is offered to production pages through the chart's own typed
 * furniture seam ({@code ChartServices.furnish}) by a study-only module,
 * so its corner, ink, reservation, label yielding, speech and paper are
 * the production chart's; its words are the study's proposal, built from
 * facts the page already holds. Two sketches, marked as such, show what
 * production cannot yet do: the box preferring the upper left, and a
 * mark for a selected body.
 *
 * <p>No production behaviour changes. Every proposed word in both
 * languages is the study's and is listed in the report for the owner's
 * review. Portable facts go to {@code measurements.md}; sizes that
 * depend on this desktop's fonts go to {@code platform.md}.
 * Regenerate with {@code make guide-and-explain-study}.
 */
public final class GuideAndExplainStudyMain {

    private static final Path DIR = Path.of("docs/studies/guide-and-explain");
    private static final Path VENUS_EVENTS = Path.of("docs/studies/venus/venus-events.txt");
    private static final double OSLO_LAT = 59.91;
    private static final double OSLO_LON = 10.75;
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");

    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();

    private GuideAndExplainStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        // The contract runs a report's main twice in one process.
        REPORT.setLength(0);
        PLATFORM.setLength(0);
        Files.createDirectories(DIR);
        javax.swing.LookAndFeel before = UIManager.getLookAndFeel();
        boolean hadFont = UIManager.getDefaults().containsKey("defaultFont");
        Object font = hadFont ? UIManager.get("defaultFont") : null;
        // A main run before this one in the contract's process may have left
        // its own default font; the sketches are drawn from none, every time.
        SwingUtilities.invokeAndWait(() -> UIManager.put("defaultFont", null));
        try {
            study();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                UIManager.put("defaultFont", font);
                try {
                    UIManager.setLookAndFeel(before);
                } catch (javax.swing.UnsupportedLookAndFeelException gone) {
                    throw new IllegalStateException(gone);
                }
            });
        }
        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        System.out.flush();
        PlatformEvidence.write(PLATFORM, DIR.resolve("platform.md").toString());
    }

    private static void study() throws Exception {
        PlatformEvidence.preface(PLATFORM,
                "Guide the chart and explain the selection, as this desktop's fonts size it",
                "Sprint 57, issue #690.");
        p("# Guide the chart and explain the selection: the measurements");
        p("");
        p("Sprint 57, issue #690, road #689. A measured study: it changes no production"
                + " behaviour. Every pan below is solved by the production grab solver and"
                + " every page is drawn by the production chart; the drag modes and the box's"
                + " words are the study's proposal (`docs/decisions/guide-and-explain.md`)."
                + " Sizes that depend on this desktop's fonts are in `platform.md`."
                + " Regenerate with `make guide-and-explain-study`.");
        p("");
        pointerNavigation();
        shortcuts();
        projections();
        panelSketches();
        selectedObjectFurniture();
    }

    // ------------------------------------------------------------------
    // Part 1: pointer navigation

    private record PanCase(String name, String caption, SkyPosition centre, double field) {
    }

    private record Gesture(String name, PixelPoint press, PixelPoint end, boolean wobbleAcross) {
    }

    /** The outcome of one gesture under one mode. */
    private record Outcome(SkyPosition centre, double grabOffTargetPx, double probeVerticalPx,
                           double probeHorizontalPx, int solved, int constrained,
                           int refused, String axis) {
    }

    private static final List<PanCase> PAN_CASES = List.of(
            new PanCase("m42-8", "M 42 at 8° (gnomonic)",
                    new SkyPosition(83.818667, -5.389667), 8.0),
            new PanCase("dec60-8", "declination +60° at 8° (gnomonic)",
                    new SkyPosition(83.818667, 60.0), 8.0),
            new PanCase("polar-8", "declination +85° at 8° (gnomonic)",
                    new SkyPosition(0.0, 85.0), 8.0),
            new PanCase("overview-60", "the 60° overview (stereographic)",
                    new SkyPosition(30.0, 40.0), 60.0),
            new PanCase("globe-180", "the celestial globe (orthographic)",
                    new SkyPosition(30.0, 40.0), 180.0));

    /** Two recorded gestures from the page centre, each with a hand's sideways wobble. */
    private static final List<Gesture> GESTURES = List.of(
            new Gesture("mostly across", new PixelPoint(450, 350), new PixelPoint(650, 380), false),
            new Gesture("mostly up", new PixelPoint(450, 350), new PixelPoint(470, 170), true));

    private static final int WAYPOINTS = 20;
    private static final double WOBBLE_PX = 12.0;
    /** Production's click-or-drag threshold ({@code PanInteraction.DRAG_THRESHOLD_PX}). */
    private static final double THRESHOLD_PX = 4.0;
    /** A probe star 200 px above the press, off the row a horizontal drag keeps. */
    private static final double PROBE_OFFSET_PX = 200.0;

    private static void pointerNavigation() {
        p("## 1. Pointer navigation, through the production solver");
        p("");
        p("Each gesture starts at the page centre of a " + WIDTH + " × " + HEIGHT + " px page"
                + " and passes " + WAYPOINTS + " waypoints - the straight line to its end with"
                + " a hand's wobble of ±" + fmt0(WOBBLE_PX) + " px across it - solved one by"
                + " one against the press-time grab exactly as production's drag solves them."
                + " *Grab off target* is how far the grabbed sky ends from the pixel the mode"
                + " asked for, to the hundredth of a pixel. *Probe* is a star that stood " + fmt0(PROBE_OFFSET_PX) + " px"
                + " above the press: how far it ends up moved vertically, and horizontally, on"
                + " the page. *Modifier* is a free drag with the proposed modifier held: the"
                + " axis is decided at the first movement of at least " + fmt0(THRESHOLD_PX)
                + " px, from its larger component, and kept for that drag.");
        p("");
        p("| page | gesture | mode | Δα (°) | Δδ (°) | grab off target (px) | probe ↕ (px) |"
                + " probe ↔ (px) | solves | held at a pole | refused |");
        p("|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|");
        for (PanCase c : PAN_CASES) {
            for (Gesture g : GESTURES) {
                for (String mode : List.of("FREE", "HORIZONTAL", "VERTICAL", "LOCKED",
                        "MODIFIER")) {
                    Outcome o = run(c, g, mode);
                    double dRa = wrap(o.centre().raDegrees() - c.centre().raDegrees());
                    double dDec = o.centre().decDegrees() - c.centre().decDegrees();
                    String name = mode.equals("MODIFIER") ? "modifier (" + o.axis() + ")"
                            : mode.toLowerCase(Locale.ROOT);
                    p("| " + c.name() + " | " + g.name() + " | " + name + " | " + fmt3(dRa)
                            + " | " + fmt3(dDec) + " | " + small(o.grabOffTargetPx()) + " | "
                            + fmt1(o.probeVerticalPx()) + " | " + fmt1(o.probeHorizontalPx())
                            + " | " + o.solved() + " | " + o.constrained() + " | "
                            + o.refused() + " |");
                }
            }
        }
        p("");
        p("Read across a row: under *horizontal* the grabbed sky stays exactly on the press"
                + " row (the wobble is gone), yet the probe star 200 px above it still moves"
                + " up or down the page wherever the page's north-up grid is not parallel to"
                + " the screen's rows - most at the polar page, the overview and the globe."
                + " A horizontal-only drag keeps a *screen row*, never a parallel of"
                + " declination, and never the ecliptic or the equator. *Locked* never pans."
                + " *Held at a pole* counts waypoints the solver held to its feasibility"
                + " boundary near a pole - the production rule, unchanged by any mode.");
        p("");
        p("### How the modifier decides its axis");
        p("");
        p("| first movement past the threshold (px) | axis |");
        p("|---|---|");
        PixelPoint press = new PixelPoint(0, 0);
        int[][] firsts = {{5, 4}, {4, 5}, {3, 3}, {-6, 2}, {1, -7}, {-4, -4}};
        for (int[] f : firsts) {
            PixelPoint first = new PixelPoint(f[0], f[1]);
            String axis = PointerDragSketch.beyond(press, first, THRESHOLD_PX)
                    ? PointerDragSketch.dominant(press, first).name().toLowerCase(Locale.ROOT)
                    : "still a click";
            p("| (" + f[0] + ", " + f[1] + ") | " + axis + " |");
        }
        p("");
        p("A tie goes to horizontal. The axis is decided once; a drag that turns later keeps"
                + " the axis it started with, so the reader is never surprised mid-gesture.");
        p("");
    }

    private static Outcome run(PanCase c, Gesture g, String mode) {
        ChartViewport viewport = new ChartViewport(c.centre(), c.field(), WIDTH, HEIGHT);
        ChartProjection kind = viewport.projection();
        SkyPosition grabbed = PanSolver.skyFromPlane(kind, c.centre(),
                PanSolver.planeFromPixel(viewport, g.press()));
        PixelPoint probePixel = new PixelPoint(g.press().x(), g.press().y() - PROBE_OFFSET_PX);
        SkyPosition probe = PanSolver.skyAt(viewport, PanSolver.planeFromPixel(viewport,
                probePixel)).orElseThrow();
        SkyPosition current = c.centre();
        PixelPoint lastTarget = g.press();
        int solved = 0;
        int constrained = 0;
        int refused = 0;
        PointerDragSketch.Mode fixed = mode.equals("MODIFIER") ? null
                : PointerDragSketch.Mode.valueOf(mode);
        PointerDragSketch.Axis axis = null;
        boolean dragging = false;
        for (int i = 1; i <= WAYPOINTS; i++) {
            PixelPoint pointer = waypoint(g, i);
            if (!dragging) {
                if (!PointerDragSketch.beyond(g.press(), pointer, THRESHOLD_PX)) {
                    continue;
                }
                dragging = true;
                if (fixed == null) {
                    axis = PointerDragSketch.dominant(g.press(), pointer);
                }
            }
            PointerDragSketch.Mode now = fixed != null ? fixed : PointerDragSketch.modeFor(axis);
            Optional<PixelPoint> target = PointerDragSketch.target(now, g.press(), pointer);
            if (target.isEmpty()) {
                continue;
            }
            PlanePoint plane = PanSolver.planeFromPixel(viewport, target.get());
            PanSolver.PanSolution solution = PanSolver.solveCentre(kind, grabbed, plane, current);
            if (solution.constrained()) {
                constrained++;
            }
            if (solution.centre().isEmpty()) {
                refused++;
                continue;
            }
            solved++;
            current = solution.centre().get();
            lastTarget = target.get();
        }
        ChartViewport after = new ChartViewport(current, c.field(), WIDTH, HEIGHT);
        PixelPoint grabNow = pixelOf(after, grabbed);
        PixelPoint probeNow = pixelOf(after, probe);
        double off = Math.hypot(grabNow.x() - lastTarget.x(), grabNow.y() - lastTarget.y());
        return new Outcome(current, solved == 0 ? 0.0 : off, probeNow.y() - probePixel.y(),
                probeNow.x() - probePixel.x(), solved, constrained, refused,
                axis == null ? "" : axis.name().toLowerCase(Locale.ROOT));
    }

    private static PixelPoint waypoint(Gesture g, int i) {
        double t = i / (double) WAYPOINTS;
        double x = g.press().x() + t * (g.end().x() - g.press().x());
        double y = g.press().y() + t * (g.end().y() - g.press().y());
        double wobble = WOBBLE_PX * Math.sin(t * Math.PI * 3.0);
        return g.wobbleAcross() ? new PixelPoint(x + wobble, y) : new PixelPoint(x, y + wobble);
    }

    private static PixelPoint pixelOf(ChartViewport viewport, SkyPosition sky) {
        PlanePoint plane = Projections.of(viewport.projection(), viewport.centre())
                .project(sky).orElseThrow();
        return new ViewportMapping(viewport, Projections.of(viewport.projection(),
                viewport.centre())).toPixel(plane);
    }

    // ------------------------------------------------------------------
    // Part 2: what the keyboard and the mouse already use

    private static void shortcuts() {
        p("## 2. The shortcut and modifier inventory");
        p("");
        p("Every keystroke the application binds (`Shortcuts.all()`), written portably:"
                + " *Menu* is Command on macOS and Ctrl elsewhere.");
        p("");
        p("| id | label | keys |");
        p("|---|---|---|");
        for (Shortcuts.Shortcut s : Shortcuts.all()) {
            p("| " + s.id() + " | " + s.label() + " | " + portable(s.stroke()) + " |");
        }
        p("");
        TreeSet<Character> used = new TreeSet<>();
        for (var t : juranometria.app.ChartKeys.toggles()) {
            used.add(Character.toUpperCase(t.key()));
        }
        for (var c : juranometria.app.ChartKeys.commands()) {
            used.add(Character.toUpperCase(c.key()));
        }
        TreeSet<Character> free = new TreeSet<>();
        for (char ch = 'A'; ch <= 'Z'; ch++) {
            if (!used.contains(ch)) {
                free.add(ch);
            }
        }
        p("The chart keyboard (" + portable(juranometria.app.ChartKeys.prefix()) + ", then one"
                + " letter) uses " + letters(used) + "; the letters still free are "
                + letters(free) + ".");
        p("");
        p("On the chart itself, as read from the code: a press with the menu modifier adds"
                + " to the selection (`SelectInteraction.toggleModifierMask`); the drag ignores"
                + " every modifier, so a menu-modifier drag pans today; Shift and Alt (Option)"
                + " have no meaning on the chart, the secondary button pans nothing and opens"
                + " nothing, and the wheel ignores modifiers. There is no keyboard panning at"
                + " all, so a pointer-only rule leaves no keyboard path to reconcile.");
        p("");
    }

    private static String portable(KeyStroke stroke) {
        StringBuilder out = new StringBuilder();
        int mods = stroke.getModifiers();
        if ((mods & Shortcuts.menuMask()) != 0) {
            out.append("Menu+");
        }
        if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) {
            out.append("Shift+");
        }
        if ((mods & InputEvent.ALT_DOWN_MASK) != 0) {
            out.append("Alt+");
        }
        int code = stroke.getKeyCode();
        if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z) {
            out.append((char) code);
        } else if (code == KeyEvent.VK_EQUALS) {
            out.append('=');
        } else if (code == KeyEvent.VK_MINUS) {
            out.append('-');
        } else {
            out.append("key ").append(code);
        }
        return out.toString();
    }

    private static String letters(TreeSet<Character> set) {
        StringBuilder out = new StringBuilder();
        for (char c : set) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(c);
        }
        return out.toString();
    }

    // ------------------------------------------------------------------
    // Part 3: which projection each field draws in

    private static void projections() {
        p("## 3. The field ladder and its projections");
        p("");
        p("Read from the production controller by zooming out from its narrowest ordinary"
                + " field to its widest; the projection is a property of the field"
                + " (`ChartProjection.forField`). Every field pans through the one drag path,"
                + " so a drag mode applies to all of them unless a ruling says otherwise.");
        p("");
        ChartViewController controller = new ChartViewController();
        for (int i = 0; i < 40; i++) {
            controller.zoomIn();
        }
        List<Double> fields = new ArrayList<>();
        double last = Double.NaN;
        for (int i = 0; i < 40; i++) {
            double field = controller.state().fieldWidthDegrees();
            if (field == last) {
                break;
            }
            fields.add(field);
            last = field;
            controller.zoomOut();
        }
        Map<String, List<String>> byProjection = new LinkedHashMap<>();
        for (double field : fields) {
            byProjection.computeIfAbsent(ChartProjection.forField(field).name()
                    .toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(fmt(field) + "°");
        }
        p("| projection | fields |");
        p("|---|---|");
        byProjection.forEach((k, v) -> p("| " + k + " | " + String.join(", ", v) + " |"));
        p("");
    }

    // ------------------------------------------------------------------
    // Part 4: the proposed Pointer navigation section, sketched

    /** The study's proposed words, English then Norwegian - for the owner's review. */
    private static final Map<String, String[]> WORDS = new LinkedHashMap<>();

    static {
        WORDS.put("section", new String[] {"Pointer navigation", "Navigering med pekeren"});
        WORDS.put("free", new String[] {"Free", "Fri"});
        WORDS.put("horizontal", new String[] {"Horizontal only", "Bare vannrett"});
        WORDS.put("vertical", new String[] {"Vertical only", "Bare loddrett"});
        WORDS.put("locked", new String[] {"Locked", "Låst"});
        WORDS.put("hint", new String[] {
                "Dragging moves the page across or up and down the screen, not along a line in"
                        + " the sky. Hold Shift while you drag to keep that one drag to its"
                        + " main direction.",
                "Når du drar, flyttes siden bortover eller opp og ned på skjermen, ikke langs en"
                        + " linje på himmelen. Hold Skift mens du drar for å holde det ene"
                        + " draget i hovedretningen."});
        WORDS.put("locked.explain", new String[] {
                "Dragging does not move the chart; clicking still selects.",
                "Å dra flytter ikke kartet; et klikk velger fortsatt."});
        WORDS.put("time", new String[] {"for {0} UTC", "for {0} UTC"});
        WORDS.put("magnitude.visual", new String[] {"V {0}", "V {0}"});
        WORDS.put("magnitude.blue", new String[] {"B {0}", "B {0}"});
        WORDS.put("family.GALAXIES", new String[] {"galaxy", "galakse"});
        WORDS.put("family.OPEN_CLUSTERS", new String[] {"open cluster", "åpen stjernehop"});
        WORDS.put("family.GLOBULAR_CLUSTERS", new String[] {"globular cluster", "kulehop"});
        WORDS.put("family.NEBULAE", new String[] {"nebula", "tåke"});
        WORDS.put("family.PLANETARY_NEBULAE", new String[] {"planetary nebula",
                "planetarisk tåke"});
        WORDS.put("spoken", new String[] {"Selected: {0}", "Valgt: {0}"});
    }

    private static String word(String key, InterfaceText said) {
        return WORDS.get(key)[said == NB ? 1 : 0];
    }

    private static void panelSketches() throws Exception {
        p("## 4. The proposed Pointer navigation section");
        p("");
        p("A sketch, not production: the Controller's own collapsible section"
                + " (`CompanionSection`) holding four exclusive choices and one sentence,"
                + " at the companion's default width of 360 px. *Free* is chosen, as it would"
                + " be at every start.");
        p("");
        p("| words | English | Norwegian (for the owner's review) |");
        p("|---|---|---|");
        for (var e : WORDS.entrySet()) {
            p("| " + e.getKey() + " | " + e.getValue()[0] + " | " + e.getValue()[1] + " |");
        }
        p("");
        q("## The Pointer navigation sketches");
        q("");
        for (InterfaceText said : List.of(EN, NB)) {
            for (boolean dark : List.of(false, true)) {
                String name = "pointer-navigation-" + (said == NB ? "nb-NO" : "en") + "-"
                        + (dark ? "dark" : "light") + ".png";
                String size = onEdt(() -> draw(() -> panel(said), name, dark));
                p("![](" + name + ")");
                p("");
                q("- " + size + " px");
            }
        }
        q("");
    }

    private static JComponent panel(InterfaceText said) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        ButtonGroup group = new ButtonGroup();
        for (String key : List.of("free", "horizontal", "vertical", "locked")) {
            JRadioButton choice = new JRadioButton(word(key, said), key.equals("free"));
            group.add(choice);
            body.add(choice);
        }
        JLabel hint = new JLabel("<html><body style='width:250px'>" + word("hint", said)
                + "</body></html>");
        hint.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 0));
        body.add(hint);
        CompanionSection section = CompanionSection.forStudy("pointernavigation",
                word("section", said), body, said);
        JPanel holder = new JPanel(new java.awt.BorderLayout());
        holder.add(section, java.awt.BorderLayout.NORTH);
        holder.setPreferredSize(new Dimension(360, holder.getPreferredSize().height));
        return holder;
    }

    // ------------------------------------------------------------------
    // Part 5: the box describing the last selected object

    /** A study-only module offering one box through the chart's furniture seam. */
    private static final class SelectedObjectBox implements ChartModule {
        private final Supplier<Optional<Furniture>> box;
        private Runnable withdraw = () -> { };

        SelectedObjectBox(Supplier<Optional<Furniture>> box) {
            this.box = box;
        }

        @Override
        public String name() {
            return "selected-object (study)";
        }

        @Override
        public void attach(ChartServices services) {
            withdraw = services.furnish("selected-object",
                    () -> box.get().map(List::of).orElse(List.of()), action -> { });
        }

        @Override
        public void detach() {
            withdraw.run();
        }
    }

    /** One page with one selection. */
    private record BoxCase(String name, String caption, ChartViewState state, int width,
                           int height, InterfaceText said, ChartPalette palette,
                           boolean key, String subject, boolean bodies, boolean horizon) {
    }

    private static void selectedObjectFurniture() throws Exception {
        Instant instant = elongation();
        Observer oslo = new Observer(OSLO_LAT, OSLO_LON, instant);
        SolarSystemService service = SolarSystemService.load();
        p("## 5. The box for the last selected object, on production pages");
        p("");
        p("Each page is a production `ChartComponent` under a production `ChartModuleHost`,"
                + " the subject selected through the host's working selection (so the"
                + " ordinary selection mark is production's), and the box offered by a"
                + " study-only module through `ChartServices.furnish`. The corner is"
                + " production's ruled order (ruling 3 on #556: lower right, upper right while"
                + " the magnitude key does not hold it, upper left); the words are the study's."
                + " Bodies are drawn for Venus's cited 2026 greatest eastern elongation ("
                + juranometria.ui.placeandtime.PlaceAndTimeFurniture.shown(instant)
                + " UTC, `docs/studies/venus/venus-events.txt`), seen from Oslo.");
        p("");
        p("The rule each page follows, as proposed: a box appears exactly when the subject"
                + " is on the page - drawn, or marked with the working cross because a chart"
                + " option or the magnitude limit keeps it from being drawn - and never for"
                + " empty sky.");
        p("");
        q("## The box on each page");
        q("");
        q("| page | box (px) | corner | names yielded to it |");
        q("|---|---|---|---:|");
        SkyPosition betelgeuse = find("Betelgeuse").position();
        SkyPosition m42 = find("M 42").position();
        List<BoxCase> cases = new ArrayList<>();
        cases.add(new BoxCase("star-named-8", "A named star: Betelgeuse at 8°.",
                new ChartViewState(betelgeuse, 8.0, 8.0), WIDTH, HEIGHT, EN,
                ChartPalette.WHITE_PAPER, false, "Betelgeuse", false, false));
        cases.add(new BoxCase("star-named-8-nb", "The same in Norwegian.",
                new ChartViewState(betelgeuse, 8.0, 8.0), WIDTH, HEIGHT, NB,
                ChartPalette.WHITE_PAPER, false, "Betelgeuse", false, false));
        cases.add(new BoxCase("star-catalogue-4", "A star with a catalogue identity only:"
                + " the brightest such star on Betelgeuse's 4° page.",
                new ChartViewState(betelgeuse, 4.0, 8.0), WIDTH, HEIGHT, EN,
                ChartPalette.WHITE_PAPER, false, "?catalogue-only", false, false));
        cases.add(new BoxCase("deep-sky-8", "A deep-sky object: M 42 at 8°.",
                new ChartViewState(m42, 8.0, 8.0), WIDTH, HEIGHT, EN,
                ChartPalette.WHITE_PAPER, false, "M 42", false, false));
        cases.add(new BoxCase("deep-sky-8-nb", "The same in Norwegian.",
                new ChartViewState(m42, 8.0, 8.0), WIDTH, HEIGHT, NB,
                ChartPalette.WHITE_PAPER, false, "M 42", false, false));
        cases.add(new BoxCase("deep-sky-8-black", "The same on the black sky.",
                new ChartViewState(m42, 8.0, 8.0), WIDTH, HEIGHT, EN,
                ChartPalette.BLACK_SKY, false, "M 42", false, false));
        ChartViewState refusing = refusingPage();
        cases.add(new BoxCase("refused-label", "A deep-sky object whose label the page refuses (star labels are almost never refused; deep-sky labels often are): the"
                + " first page of " + REFUSING_TRIES.size() + " tried (" + REFUSING_NAMES
                + ") with such an object - " + fmt(refusing.fieldWidthDegrees()) + "° at RA "
                + fmt3(refusing.centre().raDegrees()) + "°, Dec "
                + fmt3(refusing.centre().decDegrees()) + "°.",
                refusing, WIDTH, HEIGHT, EN, ChartPalette.WHITE_PAPER, false, "?refused",
                false, false));
        cases.add(new BoxCase("narrow-8", "Narrow: M 42 in a 520 px window.",
                new ChartViewState(m42, 8.0, 8.0), 520, HEIGHT, EN,
                ChartPalette.WHITE_PAPER, false, "M 42", false, false));
        cases.add(new BoxCase("narrow-8-nb", "The same in Norwegian.",
                new ChartViewState(m42, 8.0, 8.0), 520, HEIGHT, NB,
                ChartPalette.WHITE_PAPER, false, "M 42", false, false));
        cases.add(new BoxCase("narrow-key-8", "Narrow with the magnitude key on.",
                new ChartViewState(m42, 8.0, 8.0), 520, HEIGHT, EN,
                ChartPalette.WHITE_PAPER, true, "M 42", false, false));
        cases.add(new BoxCase("wide-60", "Wide: M 42 on the 60° overview.",
                new ChartViewState(m42, 60.0, 5.0), WIDTH, HEIGHT, EN,
                ChartPalette.WHITE_PAPER, false, "M 42", false, false));
        cases.add(new BoxCase("globe-180", "The celestial globe, M 42 selected.",
                new ChartViewState(m42, 180.0, 4.0), WIDTH, HEIGHT, EN,
                ChartPalette.WHITE_PAPER, false, "M 42", false, false));
        cases.add(new BoxCase("letterbox-120", "Letterboxed: a 120° page in a 600 × 1900"
                + " window.", new ChartViewState(m42, 120.0, 4.0), 600, 1900, EN,
                ChartPalette.WHITE_PAPER, false, "M 42", false, false));
        cases.add(new BoxCase("letterbox-120-nb", "The same in Norwegian.",
                new ChartViewState(m42, 120.0, 4.0), 600, 1900, NB,
                ChartPalette.WHITE_PAPER, false, "M 42", false, false));
        cases.add(new BoxCase("off-page-8", "M 42 selected, then the page panned to RA 120°:"
                + " the subject has left the page, so has the box.",
                new ChartViewState(new SkyPosition(120.0, -5.0), 8.0, 8.0), WIDTH, HEIGHT, EN,
                ChartPalette.WHITE_PAPER, false, "M 42", false, false));
        cases.add(new BoxCase("back-on-page-8", "Panned back: the box returns with the"
                + " subject.", new ChartViewState(m42, 8.0, 8.0), WIDTH, HEIGHT, EN,
                ChartPalette.WHITE_PAPER, false, "M 42", false, false));
        cases.add(new BoxCase("moon-chart-time", "The Moon at chart time, at 3°.",
                null, WIDTH, HEIGHT, EN, ChartPalette.WHITE_PAPER, false, "moon", true,
                false));
        cases.add(new BoxCase("venus-chart-time", "Venus at chart time, at 3°.",
                null, WIDTH, HEIGHT, EN, ChartPalette.WHITE_PAPER, false, "venus", true,
                false));
        String below = belowHorizonBody(oslo, service);
        cases.add(new BoxCase("below-horizon", "A body below the drawn horizon, with the"
                + " mathematical horizon on: the first of " + String.join(", ", BELOW_TRIES)
                + " standing below it at the instant - " + below + ".", null, WIDTH, HEIGHT,
                EN, ChartPalette.WHITE_PAPER, false, below, true, true));
        for (BoxCase c : cases) {
            page(c, oslo, service);
        }
        q("");
        p("### What the selection model admits");
        p("");
        p("A constellation is not a selection: `Selection` holds stars, deep-sky objects,"
                + " empty sky and Solar System occurrences, and no constellation kind, so no"
                + " constellation box is drawn. Planetary moons are drawn but not selectable,"
                + " so none is described either.");
        p("");
        sketches(oslo, service);
        paper(oslo, service);
        cleanChart();
    }


    /** Pages tried, in order, for a named star whose name the page refuses. */
    private static final List<Object[]> REFUSING_TRIES = List.of(
            new Object[] {"Pleiades", 2.0}, new Object[] {"Pleiades", 4.0},
            new Object[] {"Betelgeuse", 18.0}, new Object[] {"Antares", 18.0},
            new Object[] {"Deneb", 18.0}, new Object[] {"Betelgeuse", 36.0},
            new Object[] {"Antares", 36.0}, new Object[] {"Deneb", 36.0});
    private static final String REFUSING_NAMES = "the Pleiades at 2° and 4°, then Betelgeuse,"
            + " Antares and Deneb at 18° and at 36°";

    private static ChartViewState refusingPage() throws Exception {
        for (Object[] t : REFUSING_TRIES) {
            ChartViewState state = new ChartViewState(find((String) t[0]).position(),
                    (Double) t[1], 8.0);
            boolean found = onEdt(() -> {
                ChartComponent chart = new ChartComponent(Atlas.assembler(), PageText.in(EN));
                chart.setSize(WIDTH, HEIGHT);
                chart.setViewState(state);
                paintOnce(chart);
                return refusedNamedStar(chart).isPresent();
            });
            if (found) {
                return state;
            }
        }
        throw new IllegalStateException("no page tried refuses a deep-sky label");
    }

    private static Optional<String> refusedNamedStar(ChartComponent chart) {
        ChartScene scene = chart.currentScene();
        ChartRenderer renderer = new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT,
                PageText.in(EN));
        List<String> omitted = new ArrayList<>();
        for (var text : renderer.textPlacements(ChartRenderer.TextMetrics.offscreen(),
                scene, chart.drawnOptions())) {
            if (text.omitted()) {
                omitted.add(text.request().id());
            }
        }
        return scene.deepSkyObjects().stream()
                .filter(d -> omitted.contains(d.id()))
                .min(java.util.Comparator.comparingDouble(DeepSkyObject::magnitude))
                .map(DeepSkyObject::id);
    }



    /** Bodies tried, in order, for one standing below Oslo's horizon at the instant. */
    private static final List<String> BELOW_TRIES = List.of("sun", "moon", "venus", "mercury",
            "mars");

    private static String belowHorizonBody(Observer oslo, SolarSystemService service)
            throws Exception {
        for (String body : BELOW_TRIES) {
            BoxCase probe = new BoxCase("probe", "", null, WIDTH, HEIGHT, EN,
                    ChartPalette.WHITE_PAPER, false, body, true, true);
            Object[] made = onEdt(() -> build(probe, oslo, service));
            ChartComponent chart = (ChartComponent) made[0];
            boolean isBelow = onEdt(() -> chart.renderedBodies().stream()
                    .anyMatch(b -> b.identity().equals(body) && b.belowHorizon()));
            if (isBelow) {
                return body;
            }
        }
        throw new IllegalStateException("none of " + BELOW_TRIES + " is below the horizon");
    }

    private static void page(BoxCase c, Observer oslo, SolarSystemService service)
            throws Exception {
        Object[] made = onEdt(() -> build(c, oslo, service));
        ChartComponent chart = (ChartComponent) made[0];
        String subject = (String) made[1];
        String fallbackNote = (String) made[2];
        BufferedImage image = new BufferedImage(c.width(), c.height(),
                BufferedImage.TYPE_INT_RGB);
        onEdt(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
            return null;
        });
        ImageIO.write(image, "png", DIR.resolve(c.name() + ".png").toFile());
        List<ChartComponent.DrawnFurniture> drawn = chart.renderedFurniture();
        Optional<ChartComponent.DrawnFurniture> box = drawn.stream()
                .filter(d -> d.placed().furniture().key().endsWith("/selected-object"))
                .findFirst();
        p("### " + c.name());
        p("");
        p(c.caption() + (fallbackNote.isEmpty() ? "" : " " + fallbackNote)
                + (box.isEmpty() ? " No box." : " A box is drawn; its corner depends on"
                        + " this desktop's fonts and stands in `platform.md`.")
                + (chart.pageOffsetY() > 0 ? " The page is "
                        + chart.currentScene().viewport().heightPx() + " px tall in a "
                        + c.height() + " px window." : ""));
        p("");
        p("![](" + c.name() + ".png)");
        p("");
        if (box.isPresent()) {
            ChartFurniture f = box.get().placed().furniture();
            p("> **" + f.heading() + "**  ");
            for (String line : f.lines()) {
                p("> " + line + "  ");
            }
            p("");
            Rectangle covers = box.get().placed().covers();
            q("| " + c.name() + " | " + covers.width + "×" + covers.height + " | "
                    + box.get().placed().corner().token() + " | "
                    + yielded(chart, box.get().placed()) + " |");
        } else if (subject != null) {
            p("The subject " + subject + " is not on this page.");
            p("");
        }
    }

    /** Builds the page on the event thread: the chart, the subject's identity, a note. */
    private static Object[] build(BoxCase c, Observer oslo, SolarSystemService service) {
        InterfaceText said = c.said();
        ChartComponent chart = new ChartComponent(Atlas.assembler(), PageText.in(said));
        chart.setSize(c.width(), c.height());
        chart.setChartOptions(withKey(c.key()).withPalette(c.palette()));
        ChartModuleHost host = new ChartModuleHost(chart, new SelectionModel(), r -> { });
        MeridianModule meridian = host.attach(new MeridianModule(oslo));
        meridian.showing(false, c.horizon(), false);
        String note = "";
        ChartViewState state = c.state();
        if (c.bodies()) {
            SolarSystemModule solar = host.attach(new SolarSystemModule(
                    () -> meridian.observer(), () -> service, () -> c.horizon()));
            switch (c.subject()) {
                case "moon" -> solar.moonShowing(true);
                case "venus" -> solar.venusShowing(true);
                case "sun" -> solar.sunShowing(true);
                case "mercury" -> solar.mercuryShowing(true);
                case "mars" -> solar.marsShowing(true);
                default -> throw new IllegalStateException(c.subject());
            }
            // Where the body is, read from the page's own inventory after one
            // wide paint - the box then speaks the page's entry, no second solve.
            PageEntry.Body entry = null;
            for (SkyPosition look : List.of(new SkyPosition(0.0, 0.0),
                    new SkyPosition(180.0, 0.0), new SkyPosition(0.0, 89.0),
                    new SkyPosition(0.0, -89.0))) {
                chart.setViewState(new ChartViewState(look, 180.0, 4.0));
                paintOnce(chart);
                entry = host.inventory().bodies().stream()
                        .filter(b -> b.identity().equals(c.subject())).findFirst()
                        .orElse(null);
                if (entry != null) {
                    break;
                }
            }
            if (entry == null) {
                throw new IllegalStateException("no " + c.subject() + " on any hemisphere");
            }
            state = new ChartViewState(entry.position(), c.horizon() ? 8.0 : 3.0,
                    8.0);
        }
        chart.setViewState(state);
        paintOnce(chart);
        String identity = subjectIdentity(c, chart, host);
        if (c.subject().startsWith("?")) {
            note = "(" + identity + ".)";
        }
        if (c.name().equals("off-page-8")) {
            // Selected on its own page first, then the page moves away.
            chart.setViewState(new ChartViewState(find("M 42").position(), 8.0, 8.0));
            paintOnce(chart);
            host.workingSelection().replaceWith(List.of(identity), identity);
            chart.setViewState(state);
            paintOnce(chart);
        } else {
            host.workingSelection().replaceWith(List.of(identity), identity);
        }
        String id = identity;
        if (!c.name().startsWith("sketch-")) {
            host.attach(new SelectedObjectBox(() -> boxFor(id, chart, host, said)));
        }
        paintOnce(chart);
        return new Object[] {chart, identity, note, host};
    }

    private static void paintOnce(ChartComponent chart) {
        BufferedImage scratch = new BufferedImage(Math.max(1, chart.getWidth()),
                Math.max(1, chart.getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scratch.createGraphics();
        try {
            chart.paint(g);
        } finally {
            g.dispose();
        }
    }

    /** The subject's identity: a searched name, a body, or one the page itself offers. */
    private static String subjectIdentity(BoxCase c, ChartComponent chart, ChartModuleHost host) {
        ChartScene scene = chart.currentScene();
        switch (c.subject()) {
            case "moon", "venus", "sun", "mercury", "mars" -> {
                return c.subject();
            }
            case "?catalogue-only" -> {
                return scene.stars().stream()
                        .filter(s -> s.identity() == null || (s.identity().name() == null
                                && s.identity().bayer() == null
                                && s.identity().flamsteed() == null))
                        .filter(s -> host.inventory().entries().stream().anyMatch(e ->
                                e.identity().equals(s.id())
                                        && e.visibility() == juranometria.page.PageVisibility.DRAWN))
                        .min(java.util.Comparator.comparingDouble(Star::magnitude))
                        .map(Star::id).orElseThrow();
            }
            case "?refused" -> {
                return refusedNamedStar(chart).orElseThrow();
            }
            default -> {
                return find(c.subject()).identity();
            }
        }
    }

    /**
     * The proposed box for the lead of the working selection: present only
     * while the subject is on this page, built from facts the page holds.
     */
    private static Optional<Furniture> boxFor(String identity, ChartComponent chart,
                                              ChartModuleHost host, InterfaceText said) {
        Optional<PageEntry> entry = host.inventory().entries().stream()
                .filter(e -> e.identity().equals(identity)).findFirst();
        if (entry.isEmpty()) {
            return Optional.empty();
        }
        String heading;
        List<String> lines = new ArrayList<>();
        switch (entry.get()) {
            case PageEntry.StarEntry s -> {
                Star star = s.star();
                StarIdentity id = star.identity();
                heading = bestName(id, star.id(), said);
                List<String> names = new ArrayList<>();
                if (id != null && id.name() != null && id.bayer() != null) {
                    names.add(withConstellation(id.bayer(), id));
                } else if (id != null && id.name() != null && id.flamsteed() != null) {
                    names.add(withConstellation(id.flamsteed(), id));
                }
                if (!heading.equals(star.id())) {
                    names.add(star.id());
                }
                if (!names.isEmpty()) {
                    lines.add(String.join(" · ", names));
                }
                lines.add(say(word("magnitude.visual", said), mag(star.magnitude())));
                lines.add(coordinates(star.position()));
            }
            case PageEntry.DeepSky d -> {
                DeepSkyObject dso = d.object();
                heading = dso.aliases().stream().filter(a -> a.startsWith("M "))
                        .findFirst().orElse(dso.id());
                List<String> first = new ArrayList<>();
                if (!heading.equals(dso.id())) {
                    first.add(dso.id());
                }
                first.add(word("family." + SymbolFamily.of(dso.type()).name(), said));
                lines.add(String.join(" · ", first));
                String magnitude = switch (dso.recorded().band()) {
                    case VISUAL -> say(word("magnitude.visual", said), mag(dso.magnitude()));
                    case BLUE -> say(word("magnitude.blue", said), mag(dso.magnitude()));
                    case NONE -> said.say("inspector.magnitude.none");
                };
                lines.add(magnitude);
                lines.add(coordinates(dso.position()));
            }
            case PageEntry.Body b -> {
                heading = b.name();
                lines.add(say(word("time", said), INSTANT.format(
                        b.moment().instant().atOffset(java.time.ZoneOffset.UTC))));
                lines.add(coordinates(b.position()));
                boolean below = chart.renderedBodies().stream()
                        .anyMatch(p -> p.identity().equals(b.identity()) && p.belowHorizon());
                if (below) {
                    lines.add(said.say("page.body.belowHorizon"));
                }
            }
            default -> {
                return Optional.empty();
            }
        }
        String all = heading + ", " + String.join(", ", lines);
        String spoken = say(word("spoken", said), all);
        return Optional.of(new Furniture("selected-object", heading, lines, null, spoken,
                spoken));
    }

    private static final java.time.format.DateTimeFormatter INSTANT =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    private static int yielded(ChartComponent chart, ChartFurniture.Placed placed) {
        ChartScene scene = chart.currentScene();
        ChartOptions options = chart.drawnOptions();
        ChartRenderer without = new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT,
                PageText.in(EN));
        ChartRenderer with = new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT,
                PageText.in(EN));
        with.setFurniture(() -> List.of(placed.furniture()));
        return placedNames(without, scene, options) - placedNames(with, scene, options);
    }

    private static int placedNames(ChartRenderer renderer, ChartScene scene,
                                   ChartOptions options) {
        int placed = 0;
        for (var text : renderer.textPlacements(ChartRenderer.TextMetrics.offscreen(), scene,
                options)) {
            if (!text.omitted()) {
                placed++;
            }
        }
        return placed;
    }

    // ------------------------------------------------------------------
    // Part 6: two sketches of what production cannot do yet

    private static void sketches(Observer oslo, SolarSystemService service) throws Exception {
        p("## 6. Two sketches of what the proposal adds (built since in #692)");
        p("");
        p("Marked as sketches: each is a production page carrying no box of the study's,"
                + " with the study's own drawing over it, so the labels beneath do not yield as"
                + " production's would.");
        p("");
        // The upper left first, as the road prefers.
        for (BoxCase c : List.of(
                new BoxCase("sketch-upper-left-8", "Sketch: the box preferring the upper"
                        + " left, Betelgeuse at 8°.", new ChartViewState(
                        find("Betelgeuse").position(), 8.0, 8.0), WIDTH, HEIGHT, EN,
                        ChartPalette.WHITE_PAPER, false, "Betelgeuse", false, false),
                new BoxCase("sketch-body-mark", "Sketch: the box in the upper left beside a"
                        + " selected body - Venus at 3°. The selection ring around the body,"
                        + " which this sketch once drew itself, is production's own since #692"
                        + " (ruling C-10).", null, WIDTH, HEIGHT, EN,
                        ChartPalette.WHITE_PAPER, false, "venus", true, false))) {
            Object[] made = onEdt(() -> build(c, oslo, service));
            ChartComponent chart = (ChartComponent) made[0];
            String identity = (String) made[1];
            BufferedImage image = new BufferedImage(c.width(), c.height(),
                    BufferedImage.TYPE_INT_RGB);
            ChartModuleHost host = (ChartModuleHost) made[3];
            Furniture[] box = new Furniture[1];
            onEdt(() -> {
                // The page carries no box of the study's; the sketch draws the one it would.
                box[0] = boxFor(identity, chart, host, c.said()).orElseThrow();
                Graphics2D g = image.createGraphics();
                try {
                    chart.paint(g);
                    drawSketchBox(g, chart, box[0], c.palette());
                    // Since #692 production rings a selected body itself; the
                    // sketch adds no second ring.
                } finally {
                    g.dispose();
                }
                return null;
            });
            ImageIO.write(image, "png", DIR.resolve(c.name() + ".png").toFile());
            p("### " + c.name());
            p("");
            p(c.caption());
            p("");
            p("![](" + c.name() + ".png)");
            p("");
        }
    }

    /**
     * The box drawn in the upper left by the sketch, in the furniture's own
     * face, frame and padding (ChartRenderer: 11-point SansSerif, bold
     * heading, 12 px margin, 8 px padding), on a page that carries no other box of
     * the study's.
     */
    private static void drawSketchBox(Graphics2D g, ChartComponent chart, Furniture box,
                                      ChartPalette palette) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(chart.pageOffsetX(), chart.pageOffsetY());
            Font label = new Font("SansSerif", Font.PLAIN, 11);
            Font bold = new Font("SansSerif", Font.BOLD, 11);
            FontMetrics metrics = g2.getFontMetrics(label);
            int width = g2.getFontMetrics(bold).stringWidth(box.heading());
            for (String line : box.lines()) {
                width = Math.max(width, metrics.stringWidth(line));
            }
            int lineHeight = metrics.getHeight();
            Rectangle r = new Rectangle(12, 12, width + 16,
                    (1 + box.lines().size()) * lineHeight + 16);
            g2.setColor(palette.ground());
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.setColor(palette.frameInk());
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRect(r.x, r.y, r.width, r.height);
            g2.setColor(palette.textInk());
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int baseline = r.y + 8 + metrics.getAscent();
            g2.setFont(bold);
            g2.drawString(box.heading(), r.x + 8, baseline);
            g2.setFont(label);
            for (String line : box.lines()) {
                baseline += lineHeight;
                g2.drawString(line, r.x + 8, baseline);
            }
        } finally {
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Part 7: paper

    private static void paper(Observer oslo, SolarSystemService service) throws Exception {
        p("## 7. On paper");
        p("");
        Object[] made = onEdt(() -> build(new BoxCase("paper", "", new ChartViewState(
                find("M 42").position(), 8.0, 8.0), WIDTH, HEIGHT, EN, ChartPalette.WHITE_PAPER,
                false, "M 42", false, false), oslo, service));
        ChartComponent chart = (ChartComponent) made[0];
        String identity = (String) made[1];
        ChartViewState state = new ChartViewState(find("M 42").position(), 8.0, 8.0);
        Map<String, byte[]> written = new LinkedHashMap<>();
        for (boolean marks : List.of(true, false)) {
            byte[][] out = onEdt(() -> {
                juranometria.sheet.SheetRecording sheet = juranometria.sheet.ChartSheet.record(
                        Atlas.assembler()::assemble, state, ChartOptions.DEFAULTS,
                        SheetInk.reference(chart), SheetInk.bodies(chart),
                        marks ? SheetInk.working(chart, List.of(identity), identity,
                                ChartOptions.DEFAULTS) : ChartRenderer.ReferenceLayer.NONE,
                        juranometria.sheet.PaperSize.A4, PageText.in(EN), java.util.Set.of(),
                        chart.paperFurniture());
                return new byte[][] {
                        juranometria.sheet.SheetWriters.write(sheet,
                                juranometria.sheet.SheetFormat.PNG, 96),
                        juranometria.sheet.SheetWriters.write(sheet,
                                juranometria.sheet.SheetFormat.SVG, 96),
                        juranometria.sheet.SheetWriters.write(sheet,
                                juranometria.sheet.SheetFormat.PDF, 96)};
            });
            written.put(marks ? "with" : "without", out[0]);
            if (marks) {
                Files.write(DIR.resolve("sheet-a4.png"), out[0]);
                String svg = new String(out[1], StandardCharsets.UTF_8);
                ChartFurniture f = chart.renderedFurniture().get(0).placed().furniture();
                p("M 42 at 8° exported to A4 with the working selection included. The box"
                        + " travels to paper through the chart's own furniture path (no"
                        + " control on it); the SVG carries its heading as text: "
                        + (svg.contains(">" + f.heading() + "<") ? "yes" : "no") + "; the PDF"
                        + " and the PNG are replays of the same recording.");
                p("");
                p("![](sheet-a4.png)");
                p("");
            }
        }
        BufferedImage with = ImageIO.read(new java.io.ByteArrayInputStream(written.get("with")));
        BufferedImage without = ImageIO.read(new java.io.ByteArrayInputStream(
                written.get("without")));
        int differ = 0;
        for (int y = 0; y < with.getHeight(); y++) {
            for (int x = 0; x < with.getWidth(); x++) {
                if (with.getRGB(x, y) != without.getRGB(x, y)) {
                    differ++;
                }
            }
        }
        p("Exported **without** the working selection - today's default"
                + " (`ExportSheetSession`) - the sheet still carries the box, but not the"
                + " ring: the two PNG sheets differ in " + differ + " pixels, all of them the"
                + " mark. Box and mark part company on paper unless the export rule changes.");
        p("");
    }

    private static void cleanChart() {
        p("## 8. Clean chart");
        p("");
        p("Clean chart today hides the chart toolbar and nothing else"
                + " (`ChartChromeStore`, key `chartToolbarShown`; ruled in"
                + " `docs/decisions/clean-chart-controls.md`: it \"touches nothing else\")."
                + " Furniture is not chrome: the title block and Place and Time's box stay on a"
                + " clean chart. Hiding the selection box with Clean chart would be the first"
                + " page furniture the clean-chart switch reaches.");
        p("");
    }

    // ------------------------------------------------------------------

    private static SearchHit find(String query) {
        var results = Atlas.search().search(query);
        if (results.isEmpty()) {
            throw new IllegalStateException("nothing found for " + query);
        }
        var r = results.get(0);
        return new SearchHit(r.identity(), r.position());
    }

    private record SearchHit(String identity, SkyPosition position) {
    }

    private static String bestName(StarIdentity identity, String catalogueId,
                                   InterfaceText said) {
        if (identity == null) {
            return catalogueId;
        }
        if (identity.name() != null) {
            return identity.name();
        }
        if (identity.bayer() != null) {
            return withConstellation(identity.bayer(), identity);
        }
        if (identity.flamsteed() != null) {
            return withConstellation(identity.flamsteed(), identity);
        }
        return catalogueId;
    }

    /** A Bayer letter or Flamsteed number with its constellation: "α Ori", "58 Ori". */
    private static String withConstellation(String designation, StarIdentity identity) {
        return identity.constellation() == null ? designation
                : designation + " " + identity.constellation();
    }

    /** As the Inspector writes coordinates ({@code InspectorPanel.coordinates}). */
    private static String coordinates(SkyPosition p) {
        double ra = p.raDegrees() / 15.0;
        int h = (int) Math.floor(ra);
        double m = (ra - h) * 60.0;
        if (Math.round(m * 10.0) >= 600) {
            h = (h + 1) % 24;
            m = 0.0;
        }
        double dec = Math.abs(p.decDegrees());
        int d = (int) Math.floor(dec);
        int am = (int) Math.round((dec - d) * 60.0);
        if (am == 60) {
            d++;
            am = 0;
        }
        return String.format(Locale.ROOT, "%dh %04.1fm   %c%d° %02d′", h, m,
                p.decDegrees() < 0 ? '−' : '+', d, am);
    }

    private static String say(String pattern, Object... args) {
        return java.text.MessageFormat.format(pattern, args);
    }

    private static String mag(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static ChartOptions withKey(boolean key) {
        ChartOptions o = ChartOptions.DEFAULTS;
        return new ChartOptions(o.deepSkyObjects(), o.deepSkyLabels(),
                o.constellationFigures(), o.constellationBoundaries(),
                o.constellationNames(), o.starNames(), o.bayerLetters(),
                o.flamsteedNumbers(), o.equatorialGrid(), o.titleBlock(), key,
                o.galaxies(), o.openClusters(), o.globularClusters(), o.nebulae(),
                o.planetaryNebulae(), o.palette());
    }

    /** Venus's cited 2026 greatest eastern elongation, read as #556's study reads it. */
    private static Instant elongation() throws IOException {
        for (String line : Files.readAllLines(VENUS_EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            if (f[0].equals("2026-greatest-elongation-east")) {
                String iso = f[1].length() == 17 ? f[1].substring(0, 16) + ":00Z" : f[1];
                return Instant.parse(iso);
            }
        }
        throw new IllegalStateException("no 2026-greatest-elongation-east in " + VENUS_EVENTS);
    }

    /**
     * Applies the theme first and only then builds the sketch, so every run
     * builds the same components under the same look (as the bookmark study).
     */
    private static String draw(Supplier<JComponent> build, String name, boolean dark)
            throws IOException {
        juranometria.app.UiTheme.apply(dark);
        JComponent content = build.get();
        Dimension size = content.getPreferredSize();
        content.setSize(size);
        layOut(content);
        BufferedImage image = new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(UIManager.getColor("Panel.background"));
            g.fillRect(0, 0, size.width, size.height);
            content.paint(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        return name + " " + size.width + " × " + size.height;
    }

    private static void layOut(Component at) {
        at.invalidate();
        if (at instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layOut(child);
            }
        }
    }

    private static <T> T onEdt(Callable<T> work) throws Exception {
        Object[] out = new Object[1];
        Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                out[0] = work.call();
            } catch (Exception e) {
                failed[0] = e;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        @SuppressWarnings("unchecked")
        T result = (T) out[0];
        return result;
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    private static void q(String line) {
        PLATFORM.append(line).append('\n');
    }

    private static double wrap(double degrees) {
        double d = ((degrees % 360.0) + 540.0) % 360.0 - 180.0;
        return d == -180.0 ? 180.0 : d;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
    }

    private static String fmt0(double v) {
        return String.format(Locale.ROOT, "%.0f", v);
    }

    private static String fmt1(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return s.equals("-0.0") ? "0.0" : s;
    }

    private static String fmt3(double v) {
        String s = String.format(Locale.ROOT, "%.3f", v);
        return s.equals("-0.000") ? "0.000" : s;
    }

    /**
     * A residual that should be nothing, to the hundredth of a pixel: its
     * last digits are the platform's floating point (CI's Linux runner
     * read 1e-4 px where this desktop read nothing), not a fact of the rule.
     */
    private static String small(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.equals("-0.00") ? "0.00" : s;
    }
}
