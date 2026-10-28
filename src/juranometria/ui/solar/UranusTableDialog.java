package juranometria.ui.solar;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import juranometria.ui.Explain;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.MnemonicText;

/**
 * The Uranus table as a window of its own (issue #635), kept beside the
 * Controller's Uranus group during dogfooding as Mars's dialog is: one
 * host of {@link UranusTableControls} over the shared
 * {@link UranusTableSession}. Modeless and singular; Escape closes it.
 * Opening it computes nothing: it shows what the session already holds,
 * and computes only when the reader asks - Compute, a view or Enter. What
 * is the dialog's own is its layout, the time note, Close and the access
 * letters. No Centre on chart: no Uranus is drawn yet.
 */
public final class UranusTableDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static UranusTableDialog current;

    private final Content content;

    private UranusTableDialog(Frame owner, UranusTableSession session, InterfaceText said) {
        super(owner, new SolarTableWords(said, "uranustable").say("title"), false);
        SolarTableWords words = new SolarTableWords(said, "uranustable");
        getAccessibleContext().setAccessibleName(words.say("a11y"));
        getAccessibleContext().setAccessibleDescription(words.say("explain"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        content = new Content(session, said, this::dispose);
        setContentPane(content);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                content.controls().release();
            }
        });
        pack();
        setLocationRelativeTo(owner);
    }

    /** Opens the one Uranus table over the shared session, or brings it to the front. */
    public static void open(Frame owner, UranusTableSession session, InterfaceText said) {
        if (current != null && current.isDisplayable()) {
            current.toFront();
            current.requestFocus();
            return;
        }
        current = new UranusTableDialog(owner, session, said);
        current.setVisible(true);
    }

    /** The content over a session, headless-constructible, for tests and studies. */
    public static Content content(UranusTableSession session, InterfaceText said) {
        return new Content(session, said, () -> { });
    }

    /** This dialog's content. */
    public Content content() {
        return content;
    }

    /** Everything inside the dialog, with no window of its own. */
    public static final class Content extends JPanel {

        private static final long serialVersionUID = 1L;

        private final UranusTableControls controls;
        public final JLabel timeNote = new JLabel();
        public final JButton close;

        Content(UranusTableSession session, InterfaceText language, Runnable closeAction) {
            controls = new UranusTableControls(session, language, true);
            SolarTableWords said = controls.words();
            MnemonicText letters = MnemonicText.in(language);
            setLayout(new BorderLayout(0, 8));
            setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
            add(controls.inDialog(), BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
            timeNote.setName("uranusTimeNote");
            timeNote.setAlignmentX(0f);
            timeNote.setText("<html><body style='width: 640px'>"
                    + said.say("time.note", session.timeScales().exactFrom().toString(),
                            session.timeScales().exactUntil().toString())
                    + "</body></html>");
            bottom.add(timeNote);
            bottom.add(Box.createVerticalStrut(8));
            close = new JButton(said.say("close.label"));
            close.setName("uranusClose");
            close.getAccessibleContext().setAccessibleName(said.say("close.a11y"));
            letters.apply(close, said.key("close.mnemonic"));
            Explain.selfExplanatory(close, said.say("close.explain"));
            close.addActionListener(e -> closeAction.run());
            JPanel buttons = new JPanel();
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
            buttons.setAlignmentX(0f);
            if (controls.setChartTime != null) {
                buttons.add(controls.setChartTime);
                buttons.add(Box.createHorizontalStrut(8));
            }
            buttons.add(Box.createHorizontalGlue());
            buttons.add(close);
            bottom.add(buttons);
            add(bottom, BorderLayout.SOUTH);
        }

        /** Applies this content's draft at the chart's context now. */
        public void update() {
            controls.apply();
        }

        /** The controls this content hosts. */
        public UranusTableControls controls() {
            return controls;
        }
    }
}
