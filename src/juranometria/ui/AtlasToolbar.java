package juranometria.ui;

import java.util.Locale;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import juranometria.chart.ChartViewState;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.ShortcutText;

/**
 * The compact atlas toolbar: zoom, magnitude-limit, and reset controls
 * with a combined field/limit readout. It frames the chart rather than
 * competing with it; controls disable themselves at the fixture bounds
 * through the view state's can-queries, so the toolbar can never promise
 * data the fixture does not hold.
 */
public final class AtlasToolbar extends JToolBar {

    private final InterfaceText said;
    private final ShortcutText shortcuts;
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
        this(controller, searchField, inspector, versionText, requestExit,
                selectionMode, InterfaceText.forLanguage("en"));
    }

    /**
     * The toolbar in a language a caller states (issue #350).
     *
     * <p>Stated, never inherited: a bar that asked the default locale
     * would speak whichever language the machine was set to rather
     * than the one the reader chose. {@code SearchField} is added
     * here but owns its own words and is not translated from this
     * constructor.
     */
    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField,
                        InspectorToggle inspector,
                        String versionText,
                        Runnable requestExit,
                        juranometria.chart.SelectionMode selectionMode,
                        InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the bar has to say its words in some language");
        }
        this.said = said;
        this.shortcuts = ShortcutText.in(said);
        setFloatable(false);

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
        resetView = iconButton("zoom-reset", said.say("toolbar.reset.a11y"),
                said.say("toolbar.reset.hover"),
                said.say("toolbar.reset.explain"),
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
                    said.say("toolbar.inspector.a11y"));
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
            accumulate = new javax.swing.JToggleButton(
                    said.say("toolbar.accumulate.label"));
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
            //
            // Quiet VISUALLY. It used to be quietened with
            // setEnabled(false), which is a different claim: a screen
            // reader announces a disabled control as unavailable, so
            // the one thing here that is purely informative was
            // spoken as something the reader could not use (#350
            // inventory). The weight now comes from the theme's own
            // subdued colour, resolved per theme so the dark palette
            // gets the dark answer rather than a colour written down
            // once in light.
            version.putClientProperty("FlatLaf.styleClass", "small");
            version.putClientProperty("FlatLaf.style",
                    "foreground: $Label.disabledForeground");
            version.setFocusable(false);
            version.getAccessibleContext().setAccessibleName(
                    said.say("toolbar.version.a11y", versionText));
            version.setBorder(javax.swing.BorderFactory
                    .createEmptyBorder(0, 0, 0, 8));
            add(version);

        }
        if (requestExit != null) {
            exit = iconButton("door-exit", said.say("toolbar.exit.a11y"),
                    said.say("toolbar.exit.hover"),
                    said.say("toolbar.exit.explain"),
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

    private void sync(ChartViewController controller, ChartViewState state) {
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
        // from pieces. The old end-of-ladder line was assembled by
        // lowercasing the button's English label with Locale.ROOT -
        // an English rule about English words, offered as though it
        // were universal, and the same defect Chart Options had in
        // its dependency clause (#350).
        String hovered = shortcuts.withKeystroke(
                said.say(stem + ".hover"), id);
        String heard = said.say(stem + ".explain");
        if (next == null) {
            heard = said.say(stem + ".end");
        } else if (next.overview() != state.overview()) {
            // Whole keys, not a stem with a fragment glued on. The
            // schema is explicit keys so that every one of them can
            // be found by searching for it; a key assembled at
            // runtime is a key no grep will ever locate.
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
