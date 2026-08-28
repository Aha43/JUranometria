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
        SwingUtilities.invokeLater(JUranometriaMain::start);
    }

    private static void start() {
        UiTheme.apply();
        JFrame frame = new JFrame(AppInfo.NAME + " " + AppInfo.version());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new ChartComponent(
                M31Chart.CENTRE, M31Chart.FIELD_WIDTH_DEGREES, M31Chart.loadStars()));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
