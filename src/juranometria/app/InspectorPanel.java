package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import juranometria.chart.ChartScene;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.Selection;
import juranometria.chart.SelectionDetails;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;

/**
 * The chart inspector (Sprint 19, issue #170): what the thing the
 * reader pointed at actually is.
 *
 * <p>An <strong>observer</strong> of {@link SelectionModel}, never
 * its owner. It can be closed, or absent altogether, without
 * changing how selection is produced - the chart goes on answering
 * questions whether or not anything is listening.
 *
 * <p>It states only what the catalogue records, and says so when the
 * catalogue records nothing: measured over the bundled pack, 19.4%
 * of deep-sky objects have no position angle and 68.1% no visual
 * magnitude, so "not recorded" is the common case rather than the
 * exception. A blank is never allowed to read as a zero.
 *
 * <p>Nothing here moves the chart except {@code Center here}, which
 * the reader presses deliberately.
 */
public final class InspectorPanel extends JPanel {

    private final Supplier<ChartScene> currentScene;
    private final SelectionModel selection;
    private final Consumer<Selection> centreOn;

    private final JLabel heading = new JLabel();
    private final JPanel facts = new JPanel();
    private final DefaultListModel<String> candidateNames =
            new DefaultListModel<>();
    private final JList<String> candidates = new JList<>(candidateNames);
    private final JScrollPane candidateScroll = new JScrollPane(candidates);
    private final JButton centreHere = new JButton("Center here");
    /**
     * The pane's own dismissal (issue #197). The toolbar toggle
     * remains the obvious way back, and this is where a reader looks
     * once the pane is open: its upper-right corner, as side panes
     * everywhere else are dismissed.
     */
    private final JButton close = new JButton(
            new com.formdev.flatlaf.extras.FlatSVGIcon(
                    "resources/icons/x.svg", 16, 16));
    private final Runnable unsubscribe;
    /** True while the panel is writing the list, so echoes are ignored. */
    private boolean updating;
    private boolean requested;
    private boolean fitsHere = true;
    private java.util.function.Consumer<Boolean> onVisibilityChange;
    private Runnable returnFocus = () -> { };
    private final Supplier<juranometria.render.ChartOptions> options;

    public InspectorPanel(SelectionModel selection,
                          Supplier<ChartScene> currentScene,
                          Supplier<juranometria.render.ChartOptions> options,
                          Consumer<Selection> centreOn) {
        if (selection == null || currentScene == null || options == null
                || centreOn == null) {
            throw new IllegalArgumentException(
                    "the inspector needs a selection model, the current"
                            + " page, what that page draws, and a way"
                            + " to centre the chart");
        }
        this.options = options;
        this.selection = selection;
        this.currentScene = currentScene;
        this.centreOn = centreOn;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(PREFERRED_PANEL_WIDTH, 400));
        setMinimumSize(new Dimension(240, 200));
        getAccessibleContext().setAccessibleName("Inspector");
        getAccessibleContext().setAccessibleDescription(
                "What the selected chart mark is");

        heading.putClientProperty("FlatLaf.styleClass", "h3");
        heading.setAlignmentX(0.0f);
        heading.getAccessibleContext().setAccessibleName("Selected object");

        facts.setLayout(new BoxLayout(facts, BoxLayout.Y_AXIS));
        facts.setAlignmentX(0.0f);
        facts.setFocusable(true);
        facts.getAccessibleContext().setAccessibleName(
                "Details of the selected object");

        candidates.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        candidates.getAccessibleContext().setAccessibleName(
                "Objects at this point");
        candidates.getAccessibleContext().setAccessibleDescription(
                "Several objects lie within reach of that point;"
                        + " choose one to inspect it. Choosing does not"
                        + " move the chart.");
        candidates.addListSelectionListener(event -> {
            if (updating || event.getValueIsAdjusting()) {
                return;
            }
            int index = candidates.getSelectedIndex();
            if (index >= 0 && index < selection.candidates().size()) {
                selection.chooseCandidate(index);
            }
        });
        candidateScroll.setAlignmentX(0.0f);
        candidateScroll.setPreferredSize(new Dimension(280, 96));

        centreHere.getAccessibleContext().setAccessibleName("Center here");
        centreHere.getAccessibleContext().setAccessibleDescription(
                "Move the chart to put the selected object at the"
                        + " centre of the page");
        centreHere.setAlignmentX(0.0f);
        centreHere.addActionListener(event -> centreOn.accept(
                selection.selection()));

        close.setToolTipText("Close Inspector");
        close.getAccessibleContext().setAccessibleName("Close Inspector");
        close.getAccessibleContext().setAccessibleDescription(
                "Hide the Inspector pane. The chart, the selection and"
                        + " the page are unchanged.");
        // Quiet: an icon and its hover, not a bordered button
        // competing with the heading beside it.
        close.putClientProperty("JButton.buttonType", "toolBarButton");
        close.setFocusable(true);
        // Straight to the requested-visibility the toolbar toggle
        // writes, so there is one wish and one switch. Not
        // setVisible: that would hide the pane behind the toggle's
        // back and leave the toolbar and the menu claiming a panel
        // that is not there.
        close.addActionListener(event -> dismiss());

        // The heading and its dismissal on one line, the button
        // pushed to the trailing edge.
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setAlignmentX(0.0f);
        header.add(heading);
        header.add(Box.createHorizontalGlue());
        header.add(close);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                close.getPreferredSize().height));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(header);
        body.add(Box.createVerticalStrut(10));
        body.add(candidateScroll);
        body.add(Box.createVerticalStrut(10));
        body.add(facts);
        body.add(Box.createVerticalGlue());
        body.add(centreHere);
        add(body, BorderLayout.CENTER);

        // Escape closes the inspector, as it closes every dialog.
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getActionMap().put("close", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                dismiss();
            }
        });

        // Enter settles on the candidate the reader walked to with
        // the arrow keys and moves focus into the facts, which is
        // where the answer they were after is written.
        candidates.getInputMap(WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "settle");
        candidates.getActionMap().put("settle",
                new javax.swing.AbstractAction() {
                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent event) {
                        focusTarget().requestFocusInWindow();
                    }
                });

        // Closed until the reader asks for it, and its own state
        // says so from the start: a panel that is visible by Swing's
        // default while reporting that nobody asked for it would
        // announce its first appearance to nobody.
        setVisible(false);
        this.unsubscribe = selection.onChange(this::show);
    }

    /**
     * The width the reviewed layout needs beside the panel: the
     * chart keeps at least 400 px of page, the panel at least 240.
     * Below that the panel yields - the chart never becomes a sliver
     * so that a panel can keep its width.
     */
    public static final int MINIMUM_CHART_WIDTH = 400;
    public static final int MINIMUM_PANEL_WIDTH = 240;
    public static final int PREFERRED_PANEL_WIDTH = 320;

    /** Whether a window this wide can show both (issue #170). */
    public static boolean fitsBeside(int windowWidth) {
        return windowWidth >= MINIMUM_CHART_WIDTH + MINIMUM_PANEL_WIDTH;
    }

    /**
     * What the reader asked for, which is not always what fits.
     * Toggling records the wish; the window's width decides whether
     * it can be honoured, and a window that widens again restores
     * what the reader wanted rather than making them ask twice.
     */
    public void setRequestedVisible(boolean wanted) {
        this.requested = wanted;
        applyVisibility();
    }

    /** Whether the reader currently wants the inspector. */
    public boolean isRequestedVisible() {
        return requested;
    }

    /**
     * Whether the window is wide enough to show the panel at all -
     * what a control must know before claiming the panel is there
     * (issue #180).
     */
    public boolean canShow() {
        return fitsHere;
    }

    /**
     * The width the panel may take beside a window this wide: what
     * is left once the chart has its 400 px, never more than the
     * preferred 320 and never less than the floor of 240.
     *
     * <p>Without this the rule was only half kept (review): 640 px
     * passed {@link #fitsBeside}, and then the panel took its full
     * preferred 320, leaving the chart 320 - the very squeeze the
     * decision said the panel would absorb.
     */
    public static int widthBeside(int windowWidth) {
        int spare = windowWidth - MINIMUM_CHART_WIDTH;
        return Math.max(MINIMUM_PANEL_WIDTH,
                Math.min(PREFERRED_PANEL_WIDTH, spare));
    }

    /** Tells the panel how much room the window has. */
    public void setAvailableWidth(int windowWidth) {
        boolean fits = fitsBeside(windowWidth);
        if (fits) {
            // Take the leftovers, not the preference: the chart's
            // 400 px comes first.
            int width = widthBeside(windowWidth);
            setPreferredSize(new Dimension(width, getPreferredSize().height));
            setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
        }
        if (fits != fitsHere) {
            this.fitsHere = fits;
        }
        applyVisibility();
    }

    private void applyVisibility() {
        boolean shown = requested && fitsHere;
        if (shown != isVisible()) {
            setVisible(shown);
        }
        // Announced whenever it might have changed, not only when it
        // did (review): a checkbox menu item flips itself when it is
        // clicked, so a toggle that a narrow window refuses must
        // still be told what actually happened - otherwise the menu
        // shows a panel that is not there.
        if (onVisibilityChange != null) {
            onVisibilityChange.accept(shown);
        }
    }

    /** Told whenever the panel appears or disappears, for the menu. */
    public void onVisibilityChange(java.util.function.Consumer<Boolean> sink) {
        this.onVisibilityChange = sink;
    }

    /**
     * Re-reads the page. Called when the chart has been navigated:
     * the selection has not changed, but what the page can say about
     * it has - an object the reader panned away from is no longer
     * described as though it were still in front of them.
     */
    public void refresh() {
        show(new SelectionModel.Change(selection.selection(),
                selection.candidates(), selection.currentIndex()));
    }

    /**
     * Where Enter takes the reader from the candidate list: into the
     * facts, which is where the answer they were walking towards is
     * written - not onward to the button that would move the chart
     * (review).
     */
    public JComponent focusTarget() {
        return facts;
    }

    /**
     * Closes the pane the way both of its own controls close it -
     * Escape, and the heading's button - through the requested
     * visibility the toolbar toggle writes, handing the reader back
     * to the chart rather than leaving focus in a panel that is no
     * longer there.
     *
     * <p>The pane only stops being shown. Nothing here clears the
     * selection, moves the chart, changes the target, queries the
     * catalogue or assembles a page.
     */
    private void dismiss() {
        setRequestedVisible(false);
        returnFocus.run();
    }

    /** The pane's own close control; for tests. */
    public JButton closeButton() {
        return close;
    }

    /** Where focus goes when the inspector closes: the chart. */
    public void onClose(Runnable focusChart) {
        this.returnFocus = focusChart == null ? () -> { } : focusChart;
    }

    /** Stops observing - the panel can be discarded safely. */
    public void dispose() {
        unsubscribe.run();
    }

    /** The panel's current text, top to bottom; for tests. */
    public List<String> lines() {
        List<String> lines = new java.util.ArrayList<>();
        lines.add(heading.getText());
        for (java.awt.Component component : facts.getComponents()) {
            if (component instanceof JLabel label) {
                lines.add(label.getText());
            }
        }
        return List.copyOf(lines);
    }

    /** The candidate list as the reader sees it; for tests. */
    public List<String> candidateLines() {
        List<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < candidateNames.size(); i++) {
            lines.add(candidateNames.get(i));
        }
        return List.copyOf(lines);
    }

    private void show(SelectionModel.Change change) {
        ChartScene scene = currentScene.get();
        facts.removeAll();
        updating = true;
        candidateNames.clear();
        for (Selection.Object candidate : change.candidates()) {
            candidateNames.addElement(nameOf(scene, candidate));
        }
        candidateScroll.setVisible(change.isAmbiguous());
        if (change.currentIndex() >= 0) {
            candidates.setSelectedIndex(change.currentIndex());
        }
        updating = false;

        Selection current = change.selection();
        centreHere.setEnabled(!(current instanceof Selection.None));
        if (current instanceof Selection.None) {
            heading.setText("Nothing selected");
            fact("Click a star or a deep-sky symbol to see what it is.");
        } else if (current instanceof Selection.EmptySky empty) {
            heading.setText("Empty sky");
            fact(coordinates(empty.position()));
            fact("ICRS J2000");
            fact("");
            fact("No catalogued object within reach of that point.");
        } else {
            Selection.Object object = (Selection.Object) current;
            if (change.isAmbiguous()) {
                heading.setText(change.candidates().size()
                        + " objects here");
            }
            SelectionDetails.star(scene, current)
                    .ifPresentOrElse(star -> describeStar(star, change),
                            () -> SelectionDetails.deepSky(scene, current)
                                    .ifPresentOrElse(
                                            dso -> describeDeepSkyIfDrawn(
                                                    scene, dso, object, change),
                                            () -> describeAbsent(object)));
        }
        revalidate();
        repaint();
    }

    private void describeStar(Star star, SelectionModel.Change change) {
        StarIdentity identity = star.identity();
        if (!change.isAmbiguous()) {
            // The best name this star has: its own, then its Bayer
            // letter, then its Flamsteed number, then its catalogue
            // identifier. Heading a lettered star merely "Star"
            // withholds something the atlas knows.
            heading.setText(bestName(identity, star.id()));
        }
        fact(identity != null && identity.bayer() != null
                ? identity.bayer() : "no Bayer designation");
        fact(identity != null && identity.flamsteed() != null
                ? identity.flamsteed() + " (Flamsteed)"
                : "no Flamsteed number");
        fact(star.id());
        fact("");
        fact(String.format(Locale.ROOT, "V %.2f  (visual magnitude)",
                star.magnitude()));
        fact(coordinates(star.position()));
        fact("ICRS J2000");
    }

    private void describeDeepSky(DeepSkyObject dso,
                                 SelectionModel.Change change) {
        if (!change.isAmbiguous()) {
            heading.setText(messierName(dso).orElse(dso.id()));
        }
        // Say each name once: an object headed by its own catalogue
        // id should not then list that id as a fact about itself.
        if (!heading.getText().equals(dso.id())) {
            fact(dso.id());
        }
        String others = dso.aliases().stream()
                .filter(alias -> !alias.equals(heading.getText()))
                .collect(java.util.stream.Collectors.joining(", "));
        if (!others.isEmpty()) {
            fact(others);
        }
        fact(readableType(dso));
        fact("");
        fact(magnitudeLine(dso));
        fact(sizeLine(dso));
        fact(coordinates(dso.position()));
        fact("ICRS J2000");
    }

    /**
     * The selection names something this page no longer draws - the
     * reader panned away. The panel says exactly that instead of
     * inventing facts or going quiet.
     */
    /**
     * A selected object the page no longer draws is reported as
     * absent, not described as though it were there (issue #196).
     *
     * <p>Selection outlives presentation deliberately - it is
     * UI-independent state a future module will read - so switching
     * a family off does not clear what the reader chose. But the
     * panel must not go on reciting the facts of a symbol that is no
     * longer on the paper. The question is asked of production's own
     * rule, {@link juranometria.render.ChartRenderer#permitted}, so
     * the panel and the drawing cannot come to disagree about what is
     * on the page.
     */
    private void describeDeepSkyIfDrawn(ChartScene scene,
                                        juranometria.chart.DeepSkyObject dso,
                                        Selection.Object object,
                                        SelectionModel.Change change) {
        if (juranometria.render.ChartRenderer.permitted(scene, dso,
                options.get())) {
            describeDeepSky(dso, change);
        } else {
            describeAbsent(object);
        }
    }

    private void describeAbsent(Selection.Object object) {
        heading.setText(object.catalogueId());
        fact(coordinates(object.position()));
        fact("ICRS J2000");
        fact("");
        fact("Not on this page any more.");
    }

    /**
     * The magnitude with its band named - never a blue magnitude
     * labelled visual, and never a blank where the catalogue is
     * silent.
     */
    static String magnitudeLine(DeepSkyObject dso) {
        return switch (dso.recorded().band()) {
            case VISUAL -> String.format(Locale.ROOT,
                    "V %.2f  (visual magnitude)", dso.magnitude());
            case BLUE -> String.format(Locale.ROOT,
                    "B %.2f  (blue magnitude; no V recorded)",
                    dso.magnitude());
            case NONE -> "magnitude not recorded";
        };
    }

    /** The extent and orientation, or the fact that neither is known. */
    static String sizeLine(DeepSkyObject dso) {
        if (!dso.recorded().hasSize()) {
            return "size not recorded";
        }
        String extent = dso.recorded().minorAxisArcmin() != null
                ? String.format(Locale.ROOT, "%.1f′ × %.1f′",
                        dso.recorded().majorAxisArcmin(),
                        dso.recorded().minorAxisArcmin())
                : String.format(Locale.ROOT, "%.1f′ across",
                        dso.recorded().majorAxisArcmin());
        return dso.recorded().hasPositionAngle()
                ? extent + String.format(Locale.ROOT, "  at PA %.0f°",
                        dso.recorded().positionAngleDegrees())
                : extent + "  (orientation not recorded)";
    }

    /** The catalogue's type in words, not as an enum constant. */
    static String readableType(DeepSkyObject dso) {
        String name = dso.type().name().toLowerCase(Locale.ROOT)
                .replace('_', ' ');
        if (name.equals("other")) {
            return "type not classified";
        }
        // One type does not survive lowercasing: an H II region is
        // named after a spectroscopic notation, not a word, and
        // "hii region" reads as a typo (Sprint 21 gate, #184).
        return name.equals("hii region") ? "H II region" : name;
    }

    /** The most telling name a star has, never less than its id. */
    static String bestName(StarIdentity identity, String catalogueId) {
        if (identity == null) {
            return catalogueId;
        }
        if (identity.name() != null) {
            return identity.name();
        }
        if (identity.bayer() != null) {
            return identity.bayer() + "  (Bayer)";
        }
        if (identity.flamsteed() != null) {
            return identity.flamsteed() + "  (Flamsteed)";
        }
        return catalogueId;
    }

    private static java.util.Optional<String> messierName(DeepSkyObject dso) {
        return dso.aliases().stream().filter(a -> a.startsWith("M "))
                .findFirst();
    }

    private String nameOf(ChartScene scene, Selection.Object candidate) {
        if (candidate.kind() == Selection.Object.Kind.STAR) {
            return SelectionDetails.star(scene, candidate)
                    .map(star -> {
                        StarIdentity identity = star.identity();
                        return String.format(Locale.ROOT, "%s   V %.1f",
                                bestName(identity, star.id()),
                                star.magnitude());
                    })
                    .orElse(candidate.catalogueId());
        }
        return SelectionDetails.deepSky(scene, candidate)
                .map(dso -> messierName(dso)
                        .map(m -> m + "   " + dso.id()).orElse(dso.id()))
                .orElse(candidate.catalogueId());
    }

    private void fact(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(0.0f);
        if (!text.isEmpty()) {
            label.getAccessibleContext().setAccessibleName(text);
        }
        facts.add(label);
    }

    private static String coordinates(SkyPosition position) {
        double hours = position.raDegrees() / 15.0;
        int h = (int) hours;
        double minutes = (hours - h) * 60.0;
        char sign = position.decDegrees() < 0 ? '−' : '+';
        double absolute = Math.abs(position.decDegrees());
        int d = (int) absolute;
        int m = (int) Math.round((absolute - d) * 60.0);
        return String.format(Locale.ROOT, "%dh %04.1fm   %c%d° %02d′",
                h, minutes, sign, d, m);
    }

    /** The panel, wrapped so callers need not know its type. */
    public JComponent component() {
        return this;
    }
}
