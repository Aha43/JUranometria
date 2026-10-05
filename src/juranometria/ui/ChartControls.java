package juranometria.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionMode;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.ShortcutText;

/**
 * The chart's controls, whoever holds them (Sprint 41, issue #450,
 * ruled on #449): zoom in and out, the zoom lock, fewer and more
 * stars, Home, the Inspector, Accumulate, Emphasis, a search field,
 * and the field-and-magnitude readout. The atlas toolbar holds one set
 * on its bar; the Controls companion holds one set in its compact
 * section. Neither is the reference the other copies - they are two
 * hosts of this one class, so they offer the same controls with the
 * same words, the same enablement and the same semantics.
 *
 * <p>Every control asks an authority and shows what it answers:
 * {@link ChartViewController} for the field and the magnitude,
 * {@link ZoomLock}, {@link InspectorToggle}, {@link SelectionMode},
 * and {@link ChartActions} for the two actions that are more than one
 * call. No control here keeps state of its own, so a change made
 * through either host, by the keyboard or by the menu, is shown by both
 * at once.
 *
 * <p>The search field is each host's own: what a reader types and the
 * list that opens belong to the field they typed into, while the
 * chart it moves and the selection it makes are shared. The host hands
 * its field in, built in the host's language; Home clears every field
 * the actions know.
 *
 * <p>The controls own no layout: how they are arranged - a bar, or
 * rows - is the host's.
 */
public final class ChartControls {

    /** Marks every host arrangement this class built, so a host can be checked. */
    public static final String MARK = "juranometria.chartControls";

    /** The component name the zoom lock carries, for tests (#428). */
    public static final String ZOOM_LOCK = "zoomLock";

    private final InterfaceText said;
    private final ShortcutText shortcuts;
    private final ChartViewController controller;
    private final ChartActions actions;
    private final SearchField search;
    private final JButton zoomIn;
    private final JButton zoomOut;
    private final JButton fewerStars;
    private final JButton moreStars;
    private final JButton home;
    private final JLabel readout = new JLabel();
    private JToggleButton inspectorButton;
    private JToggleButton accumulate;
    private JToggleButton zoomLock;
    private JButton emphasis;

    /**
     * @param inspector     the Inspector's switch, or null for a host without one
     * @param selectionMode the Accumulate switch, or null for a host without one
     * @param lock          the zoom lock, or null for a host without one
     * @param actions       Home and Emphasis, shared with every other host
     * @param said          the host's language, stated, never inherited
     */
    public ChartControls(ChartViewController controller, SearchField search,
                         InspectorToggle inspector, SelectionMode selectionMode,
                         ZoomLock lock, ChartActions actions, InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the controls have to say their words in some language");
        }
        if (actions == null || actions.controller() != controller) {
            throw new IllegalArgumentException(
                    "the actions are the same controller's");
        }
        this.said = said;
        this.shortcuts = ShortcutText.in(said);
        this.controller = controller;
        this.actions = actions;
        this.search = search;
        actions.clearing(search);

        // The keys are named from the one registry that binds them,
        // so a tooltip cannot promise a stroke the menu does not
        // answer, or spell a modifier this platform does not use.
        zoomIn = iconButton("zoom-in", said.say("toolbar.zoomIn.a11y"),
                shortcuts.withKeystroke(said.say("toolbar.zoomIn.hover"),
                        Shortcuts.ZOOM_IN),
                said.say("toolbar.zoomIn.explain"),
                controller::zoomIn);
        zoomOut = iconButton("zoom-out", said.say("toolbar.zoomOut.a11y"),
                shortcuts.withKeystroke(said.say("toolbar.zoomOut.hover"),
                        Shortcuts.ZOOM_OUT),
                said.say("toolbar.zoomOut.explain"),
                controller::zoomOut);
        fewerStars = iconButton("minus", said.say("toolbar.fewerStars.a11y"),
                said.say("toolbar.fewerStars.hover"),
                said.say("toolbar.fewerStars.explain"),
                controller::decreaseMagnitudeLimit);
        moreStars = iconButton("plus", said.say("toolbar.moreStars.a11y"),
                said.say("toolbar.moreStars.hover"),
                said.say("toolbar.moreStars.explain"),
                controller::increaseMagnitudeLimit);
        // Home is the one shared action: the first page, the target
        // gone, every search field cleared - through the actions, so
        // no host can clear only its own field.
        home = iconButton("zoom-reset", said.say("toolbar.reset.a11y"),
                said.say("toolbar.reset.hover"),
                said.say("toolbar.reset.explain"),
                actions::home);

        if (lock != null) {
            // The toggle asks the shared lock and shows what it says,
            // so the remembered choice and the control cannot disagree.
            zoomLock = new JToggleButton(said.say("toolbar.zoomLock.label"),
                    lock.locked());
            zoomLock.setName(ZOOM_LOCK);
            zoomLock.setFocusable(true);
            zoomLock.getAccessibleContext().setAccessibleName(
                    said.say("toolbar.zoomLock.a11y"));
            Explain.control(zoomLock,
                    said.say("toolbar.zoomLock.hover"),
                    said.say("toolbar.zoomLock.explain"));
            zoomLock.addActionListener(event -> {
                lock.lock(zoomLock.isSelected());
                zoomLock.setSelected(lock.locked());
            });
            lock.onChange(zoomLock::setSelected);
        }
        if (inspector != null) {
            inspectorButton = new JToggleButton(
                    new FlatSVGIcon("resources/icons/list-details.svg", 16, 16));
            inspectorButton.setFocusable(true);
            inspectorButton.getAccessibleContext().setAccessibleName(
                    said.say("toolbar.inspector.a11y"));
            inspectorButton.addActionListener(event -> {
                // Ask, then let the answer come back through the
                // shared switch: pressing does not decide the state.
                inspector.toggle();
                syncInspector(inspector.state());
            });
            inspector.onChange(this::syncInspector);
        }
        if (selectionMode != null) {
            accumulate = new JToggleButton(said.say("toolbar.accumulate.label"));
            accumulate.setFocusable(true);
            accumulate.getAccessibleContext().setAccessibleName(
                    said.say("toolbar.accumulate.a11y"));
            Explain.control(accumulate,
                    said.say("toolbar.accumulate.hover"),
                    said.say("toolbar.accumulate.explain"));
            accumulate.addActionListener(event ->
                    selectionMode.accumulate(accumulate.isSelected()));
            // The mode is the truth; the button says what it holds,
            // however it was changed.
            selectionMode.onChange(accumulate::setSelected);
        }

        // Enablement asks the controller, whose can-queries include the
        // coverage predicate, so a zoom that would leave the bundled data
        // is disabled rather than refused after the click.
        controller.onChange(this::sync);
    }

    /**
     * The compact Emphasis control (issue #361): one button opening the
     * one menu the actions build, which reads the chart each time it
     * opens. Built once, when a host attaches the chart; before that
     * the host has no Emphasis, as the toolbar had none before the
     * application attached it.
     *
     * <p><strong>Reachable by keyboard</strong> (ruled on #449): the
     * look and feel makes a button unfocusable when it joins a toolbar,
     * so the host re-asserts focusability after adding it, and a test
     * holds it in both hosts.
     */
    public JButton attachEmphasis(ChartComponent chart) {
        if (emphasis != null) {
            throw new IllegalStateException(
                    "the emphasis control is attached once");
        }
        actions.attachEmphasis(chart);
        emphasis = new JButton(said.say("toolbar.emphasis.label"));
        emphasis.setFocusable(true);
        emphasis.getAccessibleContext().setAccessibleName(
                said.say("toolbar.emphasis.a11y"));
        Explain.control(emphasis, said.say("toolbar.emphasis.hover"),
                said.say("toolbar.emphasis.explain"));
        emphasis.addActionListener(event -> actions.emphasisMenu(said)
                .show(emphasis, 0, emphasis.getHeight()));
        return emphasis;
    }

    // ---- what each host lays out --------------------------------------

    public JButton zoomIn() {
        return zoomIn;
    }

    public JButton zoomOut() {
        return zoomOut;
    }

    public JButton fewerStars() {
        return fewerStars;
    }

    public JButton moreStars() {
        return moreStars;
    }

    /** Home, which the toolbar calls Reset view. */
    public JButton home() {
        return home;
    }

    /** The zoom lock, or null on controls built without one. */
    public JToggleButton zoomLock() {
        return zoomLock;
    }

    /** The Inspector toggle, or null on controls built without one. */
    public JToggleButton inspector() {
        return inspectorButton;
    }

    /** Accumulate, or null on controls built without one. */
    public JToggleButton accumulate() {
        return accumulate;
    }

    /** Emphasis, or null until a chart is attached. */
    public JButton emphasis() {
        return emphasis;
    }

    /** This host's own search field. */
    public SearchField search() {
        return search;
    }

    /** The field-and-magnitude readout, following the controller. */
    public JLabel readout() {
        return readout;
    }

    /** The shared actions, for a host that checks what it shares. */
    public ChartActions actions() {
        return actions;
    }

    /**
     * These controls as the Controls companion holds them (ruled on
     * #449): two compact rows - the navigation, then the Inspector,
     * Accumulate and Emphasis - the search field as wide as the
     * section, and the readout as a quiet line beneath. The version
     * and Exit are the application's and are not here.
     */
    public JComponent inCompanion() {
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBorder(BorderFactory.createEmptyBorder(4, 4, 2, 4));
        rows.putClientProperty(MARK, Boolean.TRUE);

        JToolBar navigation = row();
        navigation.add(zoomIn);
        navigation.add(zoomOut);
        if (zoomLock != null) {
            navigation.add(zoomLock);
        }
        navigation.addSeparator();
        navigation.add(fewerStars);
        navigation.add(moreStars);
        navigation.addSeparator();
        navigation.add(home);
        rows.add(leading(navigation));

        JToolBar work = row();
        if (inspectorButton != null) {
            work.add(inspectorButton);
            work.addSeparator();
        }
        if (accumulate != null) {
            work.add(accumulate);
            work.addSeparator();
        }
        if (emphasis != null) {
            work.add(emphasis);
        }
        if (work.getComponentCount() > 0) {
            rows.add(leading(work));
        }
        keepReachableByKeyboard(navigation);
        keepReachableByKeyboard(work);

        JPanel field = new JPanel(new BorderLayout());
        field.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        field.add(search, BorderLayout.CENTER);
        rows.add(field);

        JPanel quiet = new JPanel(new BorderLayout());
        quiet.setBorder(BorderFactory.createEmptyBorder(2, 12, 6, 10));
        // Quiet visually, as the toolbar's version is: the theme's own
        // subdued colour, never setEnabled(false), which a screen
        // reader would announce as unavailable (#350).
        readout.putClientProperty("FlatLaf.styleClass", "small");
        readout.putClientProperty("FlatLaf.style",
                "foreground: $Label.disabledForeground");
        readout.setFocusable(false);
        quiet.add(readout, BorderLayout.WEST);
        rows.add(quiet);
        return rows;
    }

    private static JToolBar row() {
        JToolBar row = new JToolBar();
        row.setFloatable(false);
        row.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return row;
    }

    /** A row kept to its own preferred width, against the leading edge. */
    private static JComponent leading(JComponent row) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(row, BorderLayout.WEST);
        return holder;
    }

    /**
     * Every button here is built asking to be focusable, and the look
     * and feel takes it away again when a button joins a toolbar.
     * Re-asserted after everything is added, so the order of
     * construction cannot decide it - in every host, and for Emphasis
     * too, which the toolbar attached after re-asserting (#449).
     */
    public static void keepReachableByKeyboard(java.awt.Container host) {
        for (java.awt.Component child : host.getComponents()) {
            if (child instanceof javax.swing.AbstractButton) {
                child.setFocusable(true);
            }
        }
    }

    // ---- following the authorities -------------------------------------

    /**
     * The button says what is true: selected when the panel is
     * showing, and disabled - never selected - when the window is too
     * narrow to show it, so it cannot claim a panel that is not
     * there.
     */
    private void syncInspector(InspectorToggle.State state) {
        if (inspectorButton == null) {
            return;
        }
        inspectorButton.setSelected(state.showing());
        inspectorButton.setEnabled(state.available());
        // Disabled is the case worth writing: a control that has gone
        // grey and says nothing leaves a reader to guess whether the
        // atlas is broken or the window is small.
        if (!state.available()) {
            Explain.dynamic(inspectorButton,
                    said.say("toolbar.inspector.unavailable.hover"),
                    said.say("toolbar.inspector.unavailable.explain"));
        } else if (state.showing()) {
            Explain.dynamic(inspectorButton,
                    shortcuts.withKeystroke(
                            said.say("toolbar.inspector.showing.hover"),
                            Shortcuts.INSPECTOR),
                    said.say("toolbar.inspector.showing.explain"));
        } else {
            Explain.dynamic(inspectorButton,
                    shortcuts.withKeystroke(
                            said.say("toolbar.inspector.hidden.hover"),
                            Shortcuts.INSPECTOR),
                    said.say("toolbar.inspector.hidden.explain"));
        }
    }

    private void sync(ChartViewState state) {
        zoomIn.setEnabled(controller.canZoomIn());
        zoomOut.setEnabled(controller.canZoomOut());
        fewerStars.setEnabled(controller.canDecreaseMagnitudeLimit());
        moreStars.setEnabled(controller.canIncreaseMagnitudeLimit());

        // The values are notation and are spelled once, in ROOT, then
        // handed over already written. A language places them; none
        // re-formats them, so a decimal point cannot become a comma
        // in a magnitude (#350).
        String field = String.format(Locale.ROOT, "%.0f",
                state.fieldWidthDegrees());
        String limit = String.format(Locale.ROOT, "%.1f",
                state.limitingMagnitude());
        readout.setText(said.say("toolbar.readout", field, limit));

        say(zoomOut, "toolbar.zoomOut", Shortcuts.ZOOM_OUT,
                controller.canZoomOut() ? state.zoomOut() : null, state);
        say(zoomIn, "toolbar.zoomIn", Shortcuts.ZOOM_IN,
                controller.canZoomIn() ? state.zoomIn() : null, state);

        // A step the ladder will not take must say so. Zoom has said
        // it since #203; the magnitude controls went grey and went on
        // describing what they would do, which told a reader at the
        // end of the ladder that the control does something it will
        // not (#350 inventory).
        sayLimit(fewerStars, "toolbar.fewerStars",
                controller.canDecreaseMagnitudeLimit(), limit);
        sayLimit(moreStars, "toolbar.moreStars",
                controller.canIncreaseMagnitudeLimit(), limit);
    }

    /** A magnitude step, and what it says at the end of its ladder. */
    private void sayLimit(JButton button, String stem, boolean canStep,
                          String limit) {
        Explain.dynamic(button,
                canStep ? said.say(stem + ".hover")
                        : said.say(stem + ".end.hover", limit),
                canStep ? said.say(stem + ".explain")
                        : said.say(stem + ".end.explain", limit));
    }

    /**
     * What a zoom control leads to, when it leads somewhere new.
     *
     * <p>The overview is another rung of the same ladder rather than
     * a mode with a switch, which is the gate's decision and not a
     * shortcut: a reader who had to be told which projection was
     * drawing would be a reader being told about a problem they do
     * not have (docs/decisions/overview-projection.md). But the
     * <em>step</em> that leaves the detailed atlas is worth
     * announcing, because it is the one step of the ladder where a
     * reader gets a different kind of chart - wide, whole, and not
     * for pointing a telescope at.
     *
     * <p>So the control says where it goes, and only at that step. A
     * button that renamed itself at every rung would be a readout
     * pretending to be a control.
     */
    private void say(JButton button, String stem, String id,
                     ChartViewState next, ChartViewState state) {
        // Four whole forms, one per situation, and no sentence built
        // from pieces (#350).
        String hovered = shortcuts.withKeystroke(
                said.say(stem + ".hover"), id);
        String heard = said.say(stem + ".explain");
        if (next == null) {
            heard = said.say(stem + ".end");
        } else if (next.overview() != state.overview()) {
            // Whole keys, not a stem with a fragment glued on, so that
            // every one of them can be found by searching for it.
            hovered = shortcuts.withKeystroke(said.say(next.overview()
                    ? "toolbar.zoomOut.overview.hover"
                    : "toolbar.zoomIn.overview.hover"), id);
            heard = said.say(next.overview()
                    ? "toolbar.zoomOut.overview.explain"
                    : "toolbar.zoomIn.overview.explain");
        }
        Explain.dynamic(button, hovered, heard);
    }

    /**
     * An icon-only control, which is the kind that most needs both
     * sentences: there are no visible words at all, so the tooltip
     * is the only thing a sighted reader has and the description is
     * the only thing anyone else has.
     */
    private static JButton iconButton(String icon, String name,
                                      String hovered, String spoken,
                                      Runnable action) {
        JButton button = new JButton(
                new FlatSVGIcon("resources/icons/" + icon + ".svg", 16, 16));
        button.getAccessibleContext().setAccessibleName(name);
        button.setFocusable(true);
        button.addActionListener(e -> action.run());
        return Explain.control(button, hovered, spoken);
    }
}
