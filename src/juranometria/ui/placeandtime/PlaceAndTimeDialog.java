package juranometria.ui.placeandtime;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import juranometria.meridian.MeridianModule;

/**
 * Where the reader sets a place and an instant (Sprint 25, issue
 * #228).
 *
 * <p>A dialog opened from the View menu, as Chart Options is: a
 * place and an instant are settings, not readings, and the gate
 * measured the Inspector alternative to death at 240 px. The surface
 * is the gate's, drawn in the reviewed mock-up: three fields, three
 * visibility switches, and exactly two actions.
 *
 * <p><strong>Nothing ticks and nothing moves.</strong> The chart is
 * drawn for one frozen instant. Typing changes nothing until the
 * field is committed - Enter, or leaving the field - and committing
 * redraws the reference lines and leaves the page exactly where the
 * reader put it. The two deliberate actions are <em>Now</em>, which
 * re-freezes on the moment it is pressed (read once; it is a button,
 * not a state), and <em>Centre on zenith</em>, the one thing here
 * that moves the chart, because the reader asked.
 *
 * <p>There is no Apply and no Cancel, because the gate decided two
 * actions and no others: a committed field has already spoken, and a
 * dialog that could take it back would be holding state the module
 * does not have. Escape and the close box simply close.
 *
 * <p><strong>East-positive is stated in the label</strong>, not
 * assumed: the sign convention is the single easiest thing to get
 * wrong here, and a chart drawn for the wrong hemisphere looks
 * entirely plausible.
 *
 * <p>The place is remembered through {@link PlaceStore}; the instant
 * and the switches are not. An entry that does not parse, or is out
 * of range, is put back as it was and applies nothing - the module
 * never sees it - and a line under the controls says why.
 *
 * <p>Since #434 the content is a {@link PlaceAndTimePanel}, the same
 * class the companion window holds: one presentation, two hosts.
 */
public final class PlaceAndTimeDialog extends JDialog {

    /** The one live instance; guarded on the EDT. */
    private static PlaceAndTimeDialog current;

    /**
     * The size this dialog gives itself, in one place.
     *
     * <p>Packing asks the layout what it would like; this dialog
     * then raises the width to the reviewed {@link #ORDINARY_WIDTH}
     * floor, so its size is a <strong>policy</strong> rather than a
     * preference. A reader never meets the narrower packed width.
     *
     * <p>Public so the photographer can re-apply it rather than
     * guess at it. It used to be inlined in the constructor, and the
     * study kept its own copy of the same arithmetic to undo a pack
     * the coordinator had done - which failed under load and
     * produced a 326 px picture of a dialog no reader has seen.
     * One definition, applied by whoever needs it.
     */
    public void applySizePolicy() {
        pack();
        restateSizePolicy();
    }

    /**
     * The content size this dialog's policy gives it, as a value.
     *
     * <p>The same rule {@link #applySizePolicy()} applies - the
     * packed size, with the width raised to {@link #ORDINARY_WIDTH} -
     * computed from the layout's preference and the window's insets
     * instead of read back from the window. A photographer holds the
     * capture to this (#380): an unshown window's size is the native
     * peer's to change, even straight after {@code setSize}, so the
     * window cannot say what the policy asked for.
     */
    public java.awt.Dimension sizePolicyContent() {
        addNotify();
        java.awt.Insets chrome = getInsets();
        java.awt.Dimension packed = getPreferredSize();
        return new java.awt.Dimension(
                Math.max(packed.width, ORDINARY_WIDTH)
                        - chrome.left - chrome.right,
                packed.height - chrome.top - chrome.bottom);
    }

    /**
     * States the reviewed width again, without asking the layout
     * anything.
     *
     * <p>The half of the policy that may be repeated. Packing is how
     * the height is <em>discovered</em>, and discovery has more than
     * one answer here: this dialog's wrapped label reports 324 px on
     * some runs and 326 on others, so a pack repeated later can
     * return a width the first one did not choose - and on an
     * unshown window the peer can answer the pack before the floor
     * is applied, leaving the dialog at its packed width inside the
     * very call meant to raise it. That was photographed: 324x263
     * where 420x263 had been settled on moments earlier.
     *
     * <p>So re-stating the size does not pack. It applies the floor
     * to the width the window already has and lays the controls out
     * at it.
     *
     * <p>Used twice, both times while a size is being established:
     * by {@link #applySizePolicy()} after its pack, and by a study
     * photographing this dialog, which states the width once more
     * as the layout settles so that the geometry it records is this
     * policy's answer. Nothing calls it while anything is being
     * painted.
     */
    public void restateSizePolicy() {
        setSize(Math.max(getWidth(), ORDINARY_WIDTH), getHeight());
        // And laid out again at that size - invalidate first,
        // because setSize leaves the tree marked valid and a bare
        // validate() is then a no-op: the floor was applied to the
        // window but not to the controls inside it, which the
        // study's dark photograph showed at 344 px.
        invalidate();
        validate();
    }

    /** What the reviewed mock-up was drawn at. */
    public static final int ORDINARY_WIDTH = 420;

    private PlaceAndTimeDialog(Frame owner, MeridianModule module,
                               PlaceStore store, Supplier<Instant> clock,
                               juranometria.ui.language.InterfaceText said) {
        super(owner, said.say("placeandtime.title"), false);
        getAccessibleContext().setAccessibleName(
                said.say("placeandtime.a11y"));
        getAccessibleContext().setAccessibleDescription(
                said.say("placeandtime.explain"));
        // One closing mechanism, not two: an earlier version also
        // disposed from a window listener, and the redundancy made
        // the close box unbreakable by mutation - either half could
        // rot and the other would cover for it.
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        PlaceAndTimePanel panel =
                new PlaceAndTimePanel(module, store, clock, said);
        setContentPane(panel);
        getRootPane().registerKeyboardAction(event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        applySizePolicy();
        // A refusal line appearing or going changes the height the
        // policy discovers; re-applied so it is never clipped.
        panel.onLayoutChange(this::applySizePolicy);
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog, or brings the existing one forward. */
    public static void open(Frame owner, MeridianModule module,
                            PlaceStore store, Supplier<Instant> clock,
                            juranometria.ui.language.InterfaceText said){
        if (current != null && current.isDisplayable()) {
            current.toFront();
            current.requestFocus();
            return;
        }
        current = new PlaceAndTimeDialog(owner, module, store, clock, said);
        current.setVisible(true);
    }

    /**
     * The packed production dialog, unshown, for the study that
     * photographs it. The whole dialog and not its content in a
     * stand-in panel: a photograph of an artificial arrangement can
     * hide a clipped control the packed geometry would show
     * (review). Its clock answers the frozen instant, because a
     * photograph is not a session. Needs a display, as any dialog
     * does; the caller owns disposing it.
     */
    public static PlaceAndTimeDialog packedForStudy(Frame owner,
                                                    MeridianModule module,
                                                    PlaceStore store,
                                                    juranometria.ui.language.InterfaceText said) {
        return new PlaceAndTimeDialog(owner, module, store,
                () -> module.observer().instant(), said);
    }

    /**
     * The dialog content; headless-constructible for the tests that
     * hold every claim above.
     *
     * @param clock read once each time <em>Now</em> is pressed, and
     *     at no other moment - passed in, because a clock the dialog
     *     owned could not be held still by a test
     */
    /**
     * The dialog's content, for the audit that reads what every
     * control says (#311). The clock is the module's own frozen
     * instant, because an inventory is not a session.
     */
    /**
     * The dialog's content, in a language a caller states.
     *
     * <p>There is no overload that means "English if omitted" (#350).
     * A door like that is how a production-shaped caller forgets a
     * language and still compiles, which is exactly how a translated
     * toolbar shipped in English.
     */
    public static JComponent contentForStudy(MeridianModule module,
                                             PlaceStore store,
                                             juranometria.ui.language.InterfaceText said) {
        return content(module, store, () -> module.observer().instant(),
                said);
    }

    static JComponent content(MeridianModule module, PlaceStore store,
                              Supplier<Instant> clock,
                              juranometria.ui.language.InterfaceText said) {
        return new PlaceAndTimePanel(module, store, clock, said);
    }
}
