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
        SwingUtilities.invokeLater(() -> start(darkOverride));
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
        JFrame frame = new JFrame(AppInfo.NAME + " " + AppInfo.version());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ChartViewController controller =
                new ChartViewController(Atlas.assembler()::fits);
        frame.setJMenuBar(AppMenuBar.create(controller,
                () -> SettingsDialog.open(frame, appearance,
                        effectiveDark -> {
                            UiTheme.apply(effectiveDark);
                            com.formdev.flatlaf.FlatLaf.updateUI();
                        }),
                () -> ChartOptionsDialog.open(frame, chartOptions),
                () -> AboutDialog.open(frame)));

        ChartComponent chart = new ChartComponent(Atlas.assembler());
        controller.onChange(chart::setViewState);
        chartOptions.onChange(chart::setChartOptions);
        juranometria.ui.PanInteraction.install(chart, controller);
        juranometria.ui.ZoomInteraction.install(chart, controller);
        AppMenuBar.installZoomShortcuts(frame.getRootPane(), controller);
        juranometria.ui.SearchField searchField = new juranometria.ui.SearchField(
                Atlas.search(), Atlas.assembler(), controller);

        frame.setLayout(new BorderLayout());
        frame.add(new AtlasToolbar(controller, searchField), BorderLayout.NORTH);
        frame.add(chart, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
