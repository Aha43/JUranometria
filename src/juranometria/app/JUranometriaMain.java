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
        boolean dark = java.util.Arrays.asList(args).contains("--dark");
        SwingUtilities.invokeLater(() -> start(dark));
    }

    private static void start(boolean dark) {
        UiTheme.apply(dark);
        JFrame frame = new JFrame(AppInfo.NAME + " " + AppInfo.version());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ChartComponent chart = new ChartComponent(M31Chart.assembler());
        ChartViewController controller =
                new ChartViewController(M31Chart.assembler()::fits);
        controller.onChange(chart::setViewState);
        juranometria.ui.SearchField searchField = new juranometria.ui.SearchField(
                M31Chart.search(), M31Chart.assembler(), controller);

        frame.setLayout(new BorderLayout());
        frame.add(new AtlasToolbar(controller, searchField), BorderLayout.NORTH);
        frame.add(chart, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
