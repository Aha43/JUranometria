package juranometria.app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.ui.ChartComponent;

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
        frame.setContentPane(new ChartComponent(
                M31Chart.CENTRE, M31Chart.FIELD_WIDTH_DEGREES,
                M31Chart.TITLE, M31Chart.LIMITING_MAGNITUDE,
                M31Chart.loadStars(), M31Chart.loadDeepSkyObjects()));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
