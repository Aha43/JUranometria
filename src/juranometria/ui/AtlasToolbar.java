package juranometria.ui;

import java.util.Locale;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import juranometria.chart.ChartViewState;

/**
 * The compact atlas toolbar: zoom, magnitude-limit, and reset controls
 * with a combined field/limit readout. It frames the chart rather than
 * competing with it; controls disable themselves at the fixture bounds
 * through the view state's can-queries, so the toolbar can never promise
 * data the fixture does not hold.
 */
public final class AtlasToolbar extends JToolBar {

    private final JButton zoomIn;
    private final JButton zoomOut;
    private final JButton fewerStars;
    private final JButton moreStars;
    private final JButton resetView;
    private final JLabel readout = new JLabel();
    private javax.swing.JToggleButton inspectorButton;
    private javax.swing.JToggleButton accumulate;
    /**
     * The running version, as status text (issue #198). The toolbar
     * is handed the string rather than looking it up, so it holds no
     * second copy and no second way to format one. Never focusable -
     * it is something the toolbar says, not something a reader
     * operates.
     */
    private JLabel version;
    private JButton exit;
    private javax.swing.JComponent versionGap;

    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField) {
        this(controller, searchField, null);
    }

    /**
     * The toolbar with the Inspector toggle (issue #180). The toggle
     * is a shared switch, not a panel: the toolbar asks it to flip
     * and is told what happened, so it never learns anything about
     * windows, widths, or the inspector's lifecycle.
     */
    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField,
                        InspectorToggle inspector) {
        this(controller, searchField, inspector, null, null);
    }

    /**
     * The toolbar with the version and the way out (issue #198).
     * Exit is the rightmost control, the version immediately before
     * it, and both are separated from the chart's own controls: a
     * reader reaching for the door should not find zoom.
     *
     * <p>The toolbar is <strong>told</strong> the version and
     * <strong>told</strong> how to ask for the exit; it looks neither
     * up. That is the same seam the Inspector toggle uses, and it is
     * what keeps the toolbar from growing application knowledge - it
     * cannot format a version of its own, nor terminate anything, so
     * it cannot come to disagree with About or with the window's
     * close box.
     */
    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField,
                        InspectorToggle inspector,
                        String versionText,
                        Runnable requestExit) {
        this(controller, searchField, inspector, versionText, requestExit,
                null);
    }

    /**
     * The toolbar with the visible <strong>Accumulate</strong>
     * control (issue #261, decided by the #258 gate): the switch
     * that makes gestures add and remove instead of replace, placed
     * beside search so it serves chart and table gestures alike. The
     * control exists so the operation is discoverable and accessible
     * without remembering a modifier - the platform's
     * add-to-selection modifier always works regardless.
     *
     * <p>The toolbar reads and writes the shared mode and holds no
     * state of its own, the same seam as every other control here.
     */
    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField,
                        InspectorToggle inspector,
                        String versionText,
                        Runnable requestExit,
                        juranometria.chart.SelectionMode selectionMode) {
        setFloatable(false);

        // The keys are named from the one registry that binds them,
        // so a tooltip cannot promise a stroke the menu does not
        // answer, or spell a modifier this platform does not use.
        zoomIn = iconButton("zoom-in", "Zoom in",
                Shortcuts.saying("Zoom in", Shortcuts.ZOOM_IN),
                "Shows a narrower field, with fainter stars on it",
                controller::zoomIn);
        zoomOut = iconButton("zoom-out", "Zoom out",
                Shortcuts.saying("Zoom out", Shortcuts.ZOOM_OUT),
                "Shows a wider field, with fewer stars on it",
                controller::zoomOut);
        fewerStars = iconButton("minus", "Fewer stars",
                "Fewer stars (brighter magnitude limit)",
                "Draws only the brighter stars, one step at a time",
                controller::decreaseMagnitudeLimit);
        moreStars = iconButton("plus", "More stars",
                "More stars (fainter magnitude limit)",
                "Draws fainter stars as well, one step at a time",
                controller::increaseMagnitudeLimit);
        resetView = iconButton("zoom-reset", "Reset view",
                "Reset view: back to the atlas's first page",
                "Returns the chart to where every reader begins, and"
                        + " clears the search; what the chart draws is"
                        + " left as you chose it",
                () -> {
                    controller.reset();
                    searchField.clearSearch();
                });

        add(zoomIn);
        add(zoomOut);
        addSeparator();
        add(fewerStars);
        add(moreStars);
        addSeparator();
        add(resetView);
        addSeparator();
        if (inspector != null) {
            inspectorButton = new javax.swing.JToggleButton(
                    new FlatSVGIcon("resources/icons/list-details.svg", 16, 16));
            inspectorButton.setFocusable(true);
            inspectorButton.getAccessibleContext().setAccessibleName(
                    "Inspector");
            inspectorButton.addActionListener(event -> {
                // Ask, then let the answer come back through the
                // shared switch: pressing does not decide the state.
                inspector.toggle();
                syncInspector(inspector.state());
            });
            inspector.onChange(this::syncInspector);
            add(inspectorButton);
            addSeparator();
        }
        if (selectionMode != null) {
            accumulate = new javax.swing.JToggleButton("Accumulate");
            accumulate.setFocusable(true);
            accumulate.getAccessibleContext().setAccessibleName(
                    "Accumulate selection");
            Explain.control(accumulate,
                    "When on, choosing objects adds them to the working"
                            + " selection and choosing them again"
                            + " removes them, instead of replacing the"
                            + " selection. The platform's"
                            + " add-to-selection modifier always works.",
                    "Off, each object you choose replaces the working"
                            + " selection; on, it is added to it, and"
                            + " choosing it again takes it out. Holding"
                            + " the platform's add-to-selection"
                            + " modifier does the same whether this is"
                            + " on or off.");
            accumulate.addActionListener(event ->
                    selectionMode.accumulate(accumulate.isSelected()));
            // The mode is the truth; the button says what it holds,
            // however it was changed.
            selectionMode.onChange(accumulate::setSelected);
            add(accumulate);
            addSeparator();
        }
        add(searchField);
        add(Box.createHorizontalGlue());
        add(readout);
        readout.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 8));

        if (versionText != null) {
            versionGap = (javax.swing.JComponent)
                    Box.createHorizontalStrut(12);
            add(versionGap);
            version = new JLabel("v" + versionText);
            // Quiet, and beneath the controls in the hierarchy: an
            // identifier the reader can find when they need it, not
            // something competing with the chart's own readout.
            version.putClientProperty("FlatLaf.styleClass", "small");
            version.setEnabled(false);
            version.setFocusable(false);
            version.getAccessibleContext().setAccessibleName(
                    "JUranometria version " + versionText);
            version.setBorder(javax.swing.BorderFactory
                    .createEmptyBorder(0, 0, 0, 8));
            add(version);

        }
        if (requestExit != null) {
            exit = iconButton("door-exit", "Exit JUranometria",
                    "Exit JUranometria",
                    "Closes the atlas; what you chose is remembered",
                    requestExit);
            add(exit);
        }

        keepButtonsReachableByKeyboard();

        // The bar watches its own width. The rule used to be wired
        // by the application, which meant every other window that
        // built a toolbar - a journey, a harness - silently had no
        // responsive behaviour at all, and a test could assert the
        // rule by calling it directly and never notice (#203
        // review). A component that knows when it is resized does
        // not need anyone to remember.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                setAvailableWidth(getWidth());
            }
        });

        // Enablement asks the controller, whose can-queries include the
        // coverage predicate, so a zoom that would leave the bundled data
        // is disabled rather than refused after the click.
        controller.onChange(state -> sync(controller, state));
    }

    /**
     * Every button here is built asking to be focusable, and the look
     * and feel takes it away again: FlatLaf's toolbars make their
     * buttons unfocusable by convention, which it applies when a
     * button is added. The effect was that <em>no</em> control on
     * this bar could be reached by keyboard, though the code had
     * said it should be since the toolbar was written.
     *
     * <p>Re-asserted here, after everything is added, so the order of
     * construction cannot decide it. Local to this toolbar rather
     * than a change to the look and feel's defaults: what is claimed
     * is that the atlas's own controls are reachable, not that every
     * toolbar everywhere should be.
     */
    private void keepButtonsReachableByKeyboard() {
        for (java.awt.Component child : getComponents()) {
            if (child instanceof javax.swing.AbstractButton) {
                child.setFocusable(true);
            }
        }
    }

    /**
     * How much room the toolbar has, told the way the Inspector pane
     * is told (issue #198), so the rule can be driven in a test
     * without a window manager.
     *
     * <p>When the toolbar is squeezed, <strong>status text yields
     * and controls do not</strong>. The version goes first: it is
     * the one thing here a reader can find in Help - About. If that
     * is not enough - and with enlarged application text it is not -
     * the field-and-magnitude readout follows. Each is hidden whole
     * rather than truncated, because "v1.3" that is really 1.3.0 is
     * worse than no version at all. Nothing that does something is
     * ever given up for something that says something.
     */
    public void setAvailableWidth(int width) {
        if (version == null) {
            return;
        }
        // Status text yields, in the order of how easily a reader
        // can find it elsewhere; controls never do. The version goes
        // first - it is in Help > About. If the bar still does not
        // fit, the field-and-magnitude readout goes too, because the
        // alternative is pushing Exit off the end, and a control a
        // reader cannot reach is worse than a number they can read
        // from the chart's own title block.
        //
        // The second step was found by asking what enlarged
        // application text does (#203 review): at 24 pt the bar
        // overflowed a 560 px window even with the version already
        // hidden, and Exit was the control that fell off.
        boolean showVersion = width >= requiredWidth(true, true);
        boolean showReadout = showVersion
                || width >= requiredWidth(false, true);
        if (showVersion != version.isVisible()
                || showReadout != readout.isVisible()) {
            version.setVisible(showVersion);
            versionGap.setVisible(showVersion);
            readout.setVisible(showReadout);
            revalidate();
            repaint();
        }
    }

    /**
     * The narrowest the bar can be and still hold everything it
     * refuses to give up: every control's own preferred width, with
     * neither piece of status text.
     *
     * <p>There is a floor, and pretending otherwise is how a
     * responsive rule becomes a lie (#203 review). Below this width
     * the bar has nothing left to yield - the version and the
     * readout are already gone - and the controls must overflow,
     * because a button cannot be narrower than a button. The number
     * is not a constant: it moves with the application's text size,
     * which is exactly why it is computed rather than written down.
     *
     * <p>At or above it, nothing clips. That is the promise, and it
     * is the one worth testing.
     */
    public int minimumWidthForControls() {
        return version == null ? 0 : requiredWidth(false, false);
    }

    /** Whether the toolbar is currently showing the version. */
    public boolean isVersionShowing() {
        return version != null && version.isVisible();
    }

    /** The version the toolbar displays, or null when it shows none. */
    public String versionText() {
        return version == null ? null : version.getText();
    }

    /** The way out, for tests that drive it as a reader would. */
    public JButton exitButton() {
        return exit;
    }

    /** The Accumulate control, for tests that drive it as a reader would. */
    public javax.swing.JToggleButton accumulateButton() {
        return accumulate;
    }

    /**
     * What the toolbar needs to show everything, version included:
     * every component's own preferred width, with the glue counted
     * at nothing because it is what gives way first.
     */
    private int requiredWidth(boolean withVersion, boolean withReadout) {
        int needed = 0;
        for (java.awt.Component component : getComponents()) {
            if (component == version || component == versionGap) {
                if (withVersion) {
                    needed += component.getPreferredSize().width;
                }
            } else if (component == readout) {
                if (withReadout) {
                    needed += component.getPreferredSize().width;
                }
            } else if (!(component instanceof Box.Filler)) {
                needed += component.getPreferredSize().width;
            }
        }
        return needed;
    }

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
                    "The window is too narrow to show the Inspector",
                    "Unavailable: widen the window and the Inspector"
                            + " comes back with whatever it was"
                            + " showing");
        } else if (state.showing()) {
            Explain.dynamic(inspectorButton,
                    Shortcuts.saying("Hide the Inspector",
                            Shortcuts.INSPECTOR),
                    "Showing; press to give the whole window back to"
                            + " the chart");
        } else {
            Explain.dynamic(inspectorButton,
                    Shortcuts.saying("Show the Inspector: what the"
                            + " selected mark is", Shortcuts.INSPECTOR),
                    "Hidden; press to open the panel that names what"
                            + " you have chosen and what is on this"
                            + " page");
        }
    }

    private void sync(ChartViewController controller, ChartViewState state) {
        zoomIn.setEnabled(controller.canZoomIn());
        zoomOut.setEnabled(controller.canZoomOut());
        fewerStars.setEnabled(controller.canDecreaseMagnitudeLimit());
        moreStars.setEnabled(controller.canIncreaseMagnitudeLimit());
        readout.setText(String.format(Locale.ROOT,
                "Field %.0f° · Stars to V %.1f",
                state.fieldWidthDegrees(), state.limitingMagnitude()));
        say(zoomOut, "Zoom out", Shortcuts.ZOOM_OUT,
                "Shows a wider field, with fewer stars on it",
                controller.canZoomOut() ? state.zoomOut() : null, state);
        say(zoomIn, "Zoom in", Shortcuts.ZOOM_IN,
                "Shows a narrower field, with fainter stars on it",
                controller.canZoomIn() ? state.zoomIn() : null, state);
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
    private static void say(JButton button, String plain, String id,
                            String spoken, ChartViewState next,
                            ChartViewState state) {
        String hovered = Shortcuts.saying(plain, id);
        String heard = spoken;
        if (next == null) {
            heard = "As far as the ladder goes in that direction;"
                    + " " + plain.toLowerCase(Locale.ROOT)
                    + " is unavailable here";
        } else if (next.overview() != state.overview()) {
            hovered = Shortcuts.saying(next.overview()
                    ? "Zoom out to the overview: the whole sky's shape,"
                            + " wider than a detailed page"
                    : "Zoom in to the detailed atlas", id);
            heard = next.overview()
                    ? "The next step out leaves the detailed atlas for"
                            + " the overview, which shows the sky's"
                            + " shape rather than a page to point a"
                            + " telescope at"
                    : "The next step in returns to the detailed atlas";
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
