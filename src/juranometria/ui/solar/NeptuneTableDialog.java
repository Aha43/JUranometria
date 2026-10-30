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
 * The Neptune table as a window of its own (issue #656), kept beside the
 * Controller's Neptune group during dogfooding as Mars's dialog is: one
 * host of {@link NeptuneTableControls} over the shared
 * {@link NeptuneTableSession}. Modeless and singular; Escape closes it.
 * Opening it computes nothing: it shows what the session already holds,
 * and computes only when the reader asks - Compute, a view or Enter. What
 * is the dialog's own is its layout, the time note, Close and the access
 * letters, and since #659 Centre on chart.
 */
public final class NeptuneTableDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static NeptuneTableDialog current;

    private final Content content;

    private NeptuneTableDialog(Frame owner, NeptuneTableSession session, InterfaceText said,
                               CentreOnChart centre, BodyOnChart layer) {
        super(owner, new SolarTableWords(said, "neptunetable").say("title"), false);
        SolarTableWords words = new SolarTableWords(said, "neptunetable");
        getAccessibleContext().setAccessibleName(words.say("a11y"));
        getAccessibleContext().setAccessibleDescription(words.say("explain"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        content = new Content(session, said, this::dispose, centre, layer);
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

    /** Opens the one Neptune table over the shared session, or brings it to the front. */
    public static void open(Frame owner, NeptuneTableSession session, InterfaceText said) {
        open(owner, session, said, null, null);
    }

    /**
     * With Centre on chart (#659): the dialog's button turns {@code layer},
     * the Solar System module's Neptune switch, on and centres the chart on Neptune.
     */
    public static void open(Frame owner, NeptuneTableSession session, InterfaceText said,
                            CentreOnChart centre, BodyOnChart layer) {
        if (current != null && current.isDisplayable()) {
            current.toFront();
            current.requestFocus();
            return;
        }
        current = new NeptuneTableDialog(owner, session, said, centre, layer);
        current.setVisible(true);
    }

    /** The content over a session, headless-constructible, for tests and studies. */
    public static Content content(NeptuneTableSession session, InterfaceText said) {
        return new Content(session, said, () -> { }, null, null);
    }

    /** The same, with Centre on chart (#659). */
    public static Content content(NeptuneTableSession session, InterfaceText said,
                                  CentreOnChart centre, BodyOnChart layer) {
        return new Content(session, said, () -> { }, centre, layer);
    }

    /** This dialog's content. */
    public Content content() {
        return content;
    }

    /** Everything inside the dialog, with no window of its own. */
    public static final class Content extends JPanel {

        private static final long serialVersionUID = 1L;

        private final NeptuneTableControls controls;
        public final JLabel timeNote = new JLabel();
        public final JButton close;

        Content(NeptuneTableSession session, InterfaceText language, Runnable closeAction,
                CentreOnChart centre, BodyOnChart layer) {
            controls = new NeptuneTableControls(session, language, true, null, centre, layer);
            SolarTableWords said = controls.words();
            MnemonicText letters = MnemonicText.in(language);
            setLayout(new BorderLayout(0, 8));
            setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
            add(controls.inDialog(), BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
            timeNote.setName("neptuneTimeNote");
            timeNote.setAlignmentX(0f);
            timeNote.setText("<html><body style='width: 640px'>"
                    + said.say("time.note", session.timeScales().exactFrom().toString(),
                            session.timeScales().exactUntil().toString())
                    + "</body></html>");
            bottom.add(timeNote);
            bottom.add(Box.createVerticalStrut(8));
            close = new JButton(said.say("close.label"));
            close.setName("neptuneClose");
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
            if (controls.centreOnChart != null) {
                buttons.add(controls.centreOnChart);
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
        public NeptuneTableControls controls() {
            return controls;
        }
    }
}
