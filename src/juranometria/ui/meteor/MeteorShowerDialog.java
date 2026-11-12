package juranometria.ui.meteor;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import juranometria.ui.language.InterfaceText;

/**
 * Meteor showers... (#716): the dialog host of the one meteor-shower
 * session the Controller's group also shows. Modeless and singular;
 * opening it again brings it forward, and Escape closes it without
 * changing the selection, the chart or the time.
 */
public final class MeteorShowerDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static MeteorShowerDialog current;

    private final MeteorShowerControls controls;

    private MeteorShowerDialog(Frame owner, MeteorShowerSession session, InterfaceText said) {
        super(owner, said.say("meteor.title"), false);
        getAccessibleContext().setAccessibleName(said.say("meteor.title"));
        getAccessibleContext().setAccessibleDescription(said.say("meteor.explain"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        controls = new MeteorShowerControls(session, said);
        setContentPane(controls);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                controls.release();
            }
        });
        // Tall enough for a full night's facts, so selecting never resizes it.
        setSize(560, 520);
        setLocationRelativeTo(owner);
    }

    /** Opens the one dialog, or brings it to the front. */
    public static void open(Frame owner, MeteorShowerSession session, InterfaceText said) {
        if (current != null && current.isDisplayable()) {
            current.toFront();
            current.requestFocus();
            return;
        }
        current = new MeteorShowerDialog(owner, session, said);
        current.setVisible(true);
    }

    /** This dialog's controls. */
    public MeteorShowerControls controls() {
        return controls;
    }
}
