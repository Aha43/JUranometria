package juranometria.app;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;

/** Application entry point. */
public final class JUranometriaMain {

    private JUranometriaMain() {
    }

    public static void main(String[] args) {
        // macOS integration properties must be set before any AWT class loads.
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", AppInfo.NAME);
        boolean darkOverride = java.util.Arrays.asList(args).contains("--dark");
        SwingUtilities.invokeLater(() -> {
            // Launch is the one place a failure has no reader-visible
            // consequence of its own: the packaged application has no
            // console, and an exception here would otherwise leave a
            // live process with no window (issue #145).
            try {
                start(darkOverride);
            } catch (Throwable failure) {
                StartupFailure.reportAndExit(failure);
            }
        });
    }

    /**
     * The inspector's one navigating action (issue #170): explicit,
     * pressed by the reader, and using the same recentre path search
     * uses - so coverage and titling behave exactly as they always
     * have. Selecting alone never does this.
     */
    private static void centreOn(ChartViewController navigation,
                                 juranometria.chart.Selection chosen) {
        if (chosen == null || chosen.position() == null) {
            return;
        }
        navigation.recenter(chosen.position());
    }

    private static void start(boolean darkOverride) {
        // The appearance session policy: the saved preference decides an
        // ordinary launch; an active --dark override keeps this whole
        // session dark and can never be converted into a stored choice
        // by merely confirming Settings (contract on AppearanceSession).
        AppearanceSession appearance = new AppearanceSession(
                AppearanceStore.user(), darkOverride);
        UiTheme.apply(appearance.startupDark());
        ChartOptionsController chartOptions =
                new ChartOptionsController(ChartOptionsStore.user());
        // The catalogues verify themselves as they load, so they are
        // loaded before any window exists: a damaged download should
        // be explained, not half-drawn behind a frame that will never
        // be usable.
        ChartViewController controller =
                new ChartViewController(Atlas.assembler()::fits);
        JFrame frame = new JFrame(AppInfo.NAME + " " + AppInfo.version());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ChartComponent chart = new ChartComponent(Atlas.assembler());
        controller.onChange(chart::setViewState);
        // Hiding the family a searched target belongs to retires the
        // target (issue #196): the explicit hide is the later and
        // equally explicit request, so it wins. Ordinary family
        // hiding stays exactly what it was - a repaint - and only
        // this conflict becomes a navigation transition, which is
        // why the decision is asked here rather than folded into
        // options state that owns no navigation.
        TargetRetirement.connect(chartOptions, chart, controller);
        juranometria.ui.PanInteraction.install(chart, controller);
        juranometria.ui.ZoomInteraction.install(chart, controller);

        // Point and identify (issue #170). The selection is shared
        // state; the chart produces it, the inspector consumes it,
        // and neither knows about the other.
        juranometria.chart.SelectionModel selection =
                new juranometria.chart.SelectionModel();
        juranometria.ui.SelectInteraction.install(chart, selection);
        InspectorPanel inspector = new InspectorPanel(selection,
                chart::currentScene, chartOptions::options,
                chosen -> centreOn(controller, chosen));
        // A second consumer of the same state, marking the chart:
        // proof in the running application that the seam carries
        // more than one reader.
        selection.onChange(change -> chart.setHighlightedObject(
                change.selection()
                        instanceof juranometria.chart.Selection.Object object
                        ? object.catalogueId() : null));
        // A page the reader navigated to may no longer draw what they
        // selected; the panel re-reads it rather than going on
        // describing something that has gone.
        chart.onSceneChange(inspector::refresh);
        inspector.onClose(chart::requestFocusInWindow);
        // One switch, three ways to reach it: the toolbar button, the
        // View menu, and the window's own width (issue #180).
        juranometria.ui.InspectorToggle inspectorToggle =
                new juranometria.ui.InspectorToggle();
        inspectorToggle.bind(
                () -> inspector.setRequestedVisible(
                        !inspector.isRequestedVisible()),
                inspector::canShow);
        inspector.onVisibilityChange(inspectorToggle::report);

        frame.setJMenuBar(AppMenuBar.create(controller,
                () -> SettingsDialog.open(frame, appearance,
                        effectiveDark -> {
                            UiTheme.apply(effectiveDark);
                            com.formdev.flatlaf.FlatLaf.updateUI();
                        }),
                () -> ChartOptionsDialog.open(frame, chartOptions),
                () -> AboutDialog.open(frame),
                () -> {
                    inspectorToggle.toggle();
                    frame.revalidate();
                    frame.repaint();
                }));
        javax.swing.JCheckBoxMenuItem inspectorItem =
                AppMenuBar.inspectorItem(frame.getJMenuBar());
        if (inspectorItem != null) {
            // The item shows what is actually on screen, including
            // when a narrow window has closed the panel for the
            // reader rather than at their asking - the same state the
            // toolbar button shows, from the same switch.
            inspectorToggle.onChange(state -> {
                inspectorItem.setSelected(state.showing());
                inspectorItem.setEnabled(state.available());
            });
        }
        // The reviewed layout rule: below 640 px of window the
        // inspector yields, and a window that widens again restores
        // what the reader asked for.
        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                inspector.setAvailableWidth(frame.getWidth());
                frame.revalidate();
            }
        });
        AppMenuBar.installZoomShortcuts(frame.getRootPane(), controller);
        juranometria.ui.SearchField searchField = new juranometria.ui.SearchField(
                Atlas.search(), Atlas.assembler(), controller);
        // Finding an object by name selects it, so a reader with no
        // pointer can reach the inspector at all.
        searchField.setSelectionModel(selection);

        frame.setLayout(new BorderLayout());
        frame.add(new AtlasToolbar(controller, searchField, inspectorToggle),
                BorderLayout.NORTH);
        frame.add(chart, BorderLayout.CENTER);
        frame.add(inspector, BorderLayout.EAST);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
