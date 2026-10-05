package juranometria.ui;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import juranometria.ui.language.InterfaceText;

/**
 * The compact atlas toolbar: zoom, magnitude-limit, and reset controls
 * with a combined field/limit readout. It frames the chart rather than
 * competing with it; controls disable themselves at the fixture bounds
 * through the view state's can-queries, so the toolbar can never promise
 * data the fixture does not hold.
 *
 * <p>Since Sprint 41 (issue #450, ruled on #449) the bar is one
 * <strong>host</strong> of {@link ChartControls}: the controls, their
 * words, their enablement and their following of the authorities live
 * there, and the Controls companion holds a second set of the same
 * class. What is the bar's own is the arrangement, the version, the way
 * out, and the responsive rule below.
 */
public final class AtlasToolbar extends JToolBar {

    private final InterfaceText said;
    private final ChartControls controls;
    private final JLabel readout;
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
    private final SearchField searchFieldComponent;

    AtlasToolbar(ChartViewController controller,
                 SearchField searchField) {
        this(controller, searchField, (InspectorToggle) null);
    }

    /**
     * The toolbar with the Inspector toggle (issue #180). The toggle
     * is a shared switch, not a panel: the toolbar asks it to flip
     * and is told what happened, so it never learns anything about
     * windows, widths, or the inspector's lifecycle.
     */
    AtlasToolbar(ChartViewController controller,
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
    AtlasToolbar(ChartViewController controller,
                 SearchField searchField,
                 InspectorToggle inspector,
                 String versionText,
                 Runnable requestExit) {
        this(controller, searchField, inspector, versionText, requestExit,
                (juranometria.chart.SelectionMode) null);
    }

    /**
     * The bar with a language, and without the optional parts.
     *
     * <p>These exist so that the thing a caller outside this package
     * may not omit is the LANGUAGE. An inspector toggle, a version, a
     * way out and a selection mode are all genuinely optional; which
     * words the bar says is not (#350).
     */
    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField,
                        InterfaceText said) {
        this(controller, searchField, null, null, null, null, said);
    }

    /** The bar with everything but the selection mode, in a language. */
    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField,
                        InspectorToggle inspector,
                        String versionText,
                        Runnable requestExit,
                        InterfaceText said) {
        this(controller, searchField, inspector, versionText, requestExit,
                null, said);
    }

    // Package-private: these default to English, and a production
    // caller that wanted English by accident is exactly the defect
    // #350 found in the toolbar itself. Harnesses in this package
    // may use them; anything outside states its language.
    AtlasToolbar(ChartViewController controller,
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
        this(controller, searchField, inspector, versionText, requestExit,
                selectionMode, null, said);
    }

    /**
     * The bar with the zoom lock beside the zoom buttons (Sprint 38,
     * issue #428). A null lock leaves the toggle out, which is every
     * earlier caller's bar. The bar's own actions, shared with no other
     * host: a harness's bar.
     */
    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField,
                        InspectorToggle inspector,
                        String versionText,
                        Runnable requestExit,
                        juranometria.chart.SelectionMode selectionMode,
                        ZoomLock lock,
                        InterfaceText said) {
        this(controller, searchField, inspector, versionText, requestExit,
                selectionMode, lock, new ChartActions(controller), said);
    }

    /**
     * The bar over actions another host shares (Sprint 41, issue
     * #450): Home and Emphasis are the actions', so the bar and the
     * companion cannot come to do different things.
     */
    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField,
                        InspectorToggle inspector,
                        String versionText,
                        Runnable requestExit,
                        juranometria.chart.SelectionMode selectionMode,
                        ZoomLock lock,
                        ChartActions actions,
                        InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the bar has to say its words in some language");
        }
        this.said = said;
        this.searchFieldComponent = searchField;
        setFloatable(false);
        controls = new ChartControls(controller, searchField, inspector,
                selectionMode, lock, actions, said);
        readout = controls.readout();

        add(controls.zoomIn());
        add(controls.zoomOut());
        if (controls.zoomLock() != null) {
            // Beside the controls it leaves working: a deliberate
            // press of either still zooms while the wheel is locked.
            add(controls.zoomLock());
        }
        addSeparator();
        add(controls.fewerStars());
        add(controls.moreStars());
        addSeparator();
        add(controls.home());
        addSeparator();
        if (controls.inspector() != null) {
            add(controls.inspector());
            addSeparator();
        }
        if (controls.accumulate() != null) {
            add(controls.accumulate());
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
            // The bar's own, not the controls': the door is
            // application furniture, and the companion has none
            // (ruled on #449).
            exit = new JButton(new com.formdev.flatlaf.extras.FlatSVGIcon(
                    "resources/icons/door-exit.svg", 16, 16));
            exit.getAccessibleContext().setAccessibleName(
                    said.say("toolbar.exit.a11y"));
            exit.setFocusable(true);
            exit.addActionListener(e -> requestExit.run());
            Explain.control(exit, said.say("toolbar.exit.hover"),
                    said.say("toolbar.exit.explain"));
            add(exit);
        }

        // Every button here is built asking to be focusable, and the
        // look and feel takes it away again when a button joins a
        // toolbar. Re-asserted after everything is added, so the order
        // of construction cannot decide it (#203 review).
        ChartControls.keepReachableByKeyboard(this);

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

    /** The component name the zoom lock carries, for tests (#428). */
    public static final String ZOOM_LOCK = ChartControls.ZOOM_LOCK;

    /** The zoom lock, or null on a bar built without one (#428). */
    public javax.swing.JToggleButton zoomLockButton() {
        return controls.zoomLock();
    }

    /** The Accumulate control, for tests that drive it as a reader would. */
    public javax.swing.JToggleButton accumulateButton() {
        return controls.accumulate();
    }

    /** The controls this bar hosts, for a host that checks what it shares. */
    public ChartControls controls() {
        return controls;
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
     * Installs the compact Emphasis control (issue #361), before the
     * search field: the controls' button, over the one menu the shared
     * actions build. Re-asserted focusable after it is added, because
     * the look and feel takes focusability away from a button joining
     * a toolbar, and this one joined after the bar had re-asserted its
     * others - the one control a keyboard could not reach (#449).
     */
    public void attachEmphasis(ChartComponent chart) {
        if (chart == null) {
            throw new IllegalArgumentException("a chart is required");
        }
        JButton emphasis = controls.attachEmphasis(chart);
        int before = getComponentIndex(searchFieldComponent);
        add(emphasis, before);
        add(new javax.swing.JToolBar.Separator(), before + 1);
        emphasis.setFocusable(true);
    }

    /** The installed control; package-visible for its contracts. */
    JButton emphasisButton() {
        return controls.emphasis();
    }

    /** The menu, reading the chart at the moment it opens. */
    javax.swing.JPopupMenu emphasisMenu(ChartComponent chart) {
        return ChartActions.emphasisMenu(chart, said);
    }
}
