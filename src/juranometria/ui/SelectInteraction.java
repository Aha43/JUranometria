package juranometria.ui;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import juranometria.chart.ChartScene;
import juranometria.chart.Selection;
import juranometria.chart.SelectionMode;
import juranometria.chart.SelectionModel;
import juranometria.chart.WorkingSelection;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartRenderer;
import juranometria.chart.StarSizePolicy;

/**
 * Asking the chart what something is (Sprint 19, issue #170), and
 * building the working selection with the same click (Sprint 27,
 * issue #261, semantics from docs/decisions/working-selection.md).
 *
 * <p>A <strong>click</strong> - press and release without travelling
 * - asks; a drag pans, as it always has. The two never compete: the
 * threshold here is the same four pixels
 * {@link PanInteraction#DRAG_THRESHOLD_PX} uses to decide that a
 * gesture became a drag, so a movement is either a pan or a question
 * and never both.
 *
 * <p>What the click <em>hits</em> is decided entirely by
 * {@link ChartHitTest}. What it <em>means</em> is the decided gesture
 * table: an ordinary click replaces the working selection, an
 * additive click - Accumulate on, or the platform's add-to-selection
 * modifier, which always works - toggles membership, and an empty-sky
 * additive click answers the place without editing anything. The
 * answering model is then given the hit's own answer, candidates and
 * all, so the Inspector describes exactly what was under the pointer
 * - an unnamed star included, which no inventory lookup could
 * resolve.
 *
 * <p>The ambiguous additive click is the decision's first
 * <strong>captured transaction</strong>: toggle exactly one candidate
 * - the one the reader settles on - against the pre-click set.
 * Cycling the chooser retracts the transaction's own effect and
 * replays the single toggle against the snapshot, one whole
 * transition per step, so cycling over members and non-members alike
 * can neither accumulate members nor shed extra ones. Any membership
 * transition from another gesture ends the transaction.
 */
public final class SelectInteraction extends MouseAdapter {

    private final ChartComponent chart;
    private final SelectionModel selection;
    private final WorkingSelection working;
    private final SelectionMode mode;
    private final ChartHitTest hitTest;
    private Point pressedAt;
    private boolean pressedAdditive;

    /** The open ambiguous transaction, or null candidates when none. */
    private List<String> snapshotMembers;
    private String snapshotLead;
    private List<Selection.Object> transactionCandidates;
    private boolean transactionAdditive;
    /** True while this class is the author of a model change. */
    private boolean writing;

    private SelectInteraction(ChartComponent chart, SelectionModel selection,
                              WorkingSelection working, SelectionMode mode) {
        this.chart = chart;
        this.selection = selection;
        this.working = working;
        this.mode = mode;
        this.hitTest = new ChartHitTest(
                new ChartRenderer(StarSizePolicy.DEFAULT));
    }

    /** Installs point-and-identify-and-select on the chart. */
    public static SelectInteraction install(ChartComponent chart,
                                            SelectionModel selection,
                                            WorkingSelection working,
                                            SelectionMode mode) {
        if (chart == null || selection == null || working == null
                || mode == null) {
            throw new IllegalArgumentException(
                    "chart, selection model, working selection and"
                            + " selection mode are required");
        }
        SelectInteraction interaction =
                new SelectInteraction(chart, selection, working, mode);
        chart.addMouseListener(interaction);
        // The transaction's two ending conditions, subscribed once:
        // a membership transition from any other gesture, and an
        // answering-model change that is not the chooser cycling.
        working.onChange(change -> {
            if (!interaction.writing) {
                interaction.endTransaction();
            }
        });
        selection.onChange(interaction::selectionChanged);
        return interaction;
    }

    /**
     * The platform's add-to-selection modifier, as a modifier mask -
     * the one answer every gesture surface reads, which is why it
     * lives here rather than being asked of the toolkit at each
     * press. A headless toolkit refuses the question (it has no
     * menus and no pointer), so headless runs - where every gesture
     * is a dispatched test event - use Ctrl, and the tests dispatch
     * with the same answer they read from here.
     */
    public static int toggleModifierMask() {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return java.awt.event.InputEvent.CTRL_DOWN_MASK;
        }
        return java.awt.Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMaskEx();
    }

    @Override
    public void mousePressed(MouseEvent event) {
        pressedAt = SwingUtilities.isLeftMouseButton(event)
                ? event.getPoint() : null;
        // Read at the press, not the release: the reader may let the
        // modifier go while the button is still down.
        pressedAdditive = (event.getModifiersEx()
                & toggleModifierMask()) != 0;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        Point pressed = pressedAt;
        pressedAt = null;
        if (pressed == null
                || event.getPoint().distance(pressed)
                        >= PanInteraction.DRAG_THRESHOLD_PX) {
            // The hand travelled: that was a pan, and a pan is not a
            // question about what lies under the pointer.
            return;
        }
        ask(pressed, mode.accumulate() || pressedAdditive);
    }

    /** Puts the reader's question to the page, at a component point. */
    private void ask(Point point, boolean additive) {
        ChartScene scene = chart.scene();
        if (scene == null) {
            return;
        }
        // Component coordinates to page coordinates: the letterbox is
        // chrome, and the hit test is defined on the paper.
        double x = point.x;
        double y = point.y - chart.pageOffsetY();
        ChartHitTest.Hit hit =
                hitTest.at(scene, chart.chartOptions(), x, y);
        if (hit == null) {
            // Not on the paper at all. The reader clicked the frame
            // around the page; nothing was asked, so nothing changes.
            return;
        }
        if (hit.isEmptySky()) {
            emptySky(hit, additive);
        } else if (hit.isAmbiguous()) {
            ambiguous(hit, additive);
        } else {
            single(hit.candidates().get(0), additive);
        }
    }

    /**
     * Empty sky: ordinary clicks replace the set with the empty set;
     * an additive click leaves membership untouched - the Inspector
     * still answers the place, because it was a question, not an
     * edit.
     */
    private void emptySky(ChartHitTest.Hit hit, boolean additive) {
        endTransaction();
        writing = true;
        try {
            if (!additive) {
                working.clear();
            }
            selection.selectEmptySky(hit.selection().position());
        } finally {
            writing = false;
        }
    }

    /** One object hit: replace, or toggle it. */
    private void single(Selection.Object object, boolean additive) {
        endTransaction();
        String identity = object.catalogueId();
        writing = true;
        try {
            boolean removed = additive && working.isMember(identity);
            if (additive) {
                working.toggle(identity);
            } else {
                working.replaceWith(List.of(identity), identity);
            }
            if (!removed) {
                // The hit's own answer, which resolves what no
                // inventory can: an unnamed star has no page entry,
                // but the click knows exactly what it hit.
                selection.select(object);
            }
            // A toggle that removed the member leaves the answer to
            // the lead bridge: the reader unselected the object, and
            // the answering model moves on to what still leads.
        } finally {
            writing = false;
        }
    }

    /**
     * Several candidates: the working selection takes the current
     * candidate - replaced in, or toggled against the pre-click
     * snapshot - and the answering model offers the whole choice, as
     * it always has.
     */
    private void ambiguous(ChartHitTest.Hit hit, boolean additive) {
        snapshotMembers = working.members();
        snapshotLead = working.lead();
        transactionCandidates = hit.candidates();
        transactionAdditive = additive;
        writing = true;
        try {
            applyTransaction(hit.candidates().get(0));
            selection.selectAmong(hit.candidates());
        } finally {
            writing = false;
        }
    }

    /**
     * The chooser cycling: the answering model changed while its
     * candidate list is still this transaction's. Retract the
     * transaction's own effect and replay it for the newly chosen
     * candidate - each step is the snapshot with one toggle, never
     * the previous step with another. Anything else ends the
     * transaction.
     */
    private void selectionChanged(SelectionModel.Change change) {
        if (writing || transactionCandidates == null) {
            return;
        }
        if (!change.candidates().equals(transactionCandidates)
                || !(change.selection() instanceof Selection.Object chosen)) {
            endTransaction();
            return;
        }
        writing = true;
        try {
            applyTransaction(chosen);
        } finally {
            writing = false;
        }
    }

    /** One whole transition for the currently chosen candidate. */
    private void applyTransaction(Selection.Object chosen) {
        String identity = chosen.catalogueId();
        if (!transactionAdditive) {
            // Replace semantics: choosing another candidate retargets
            // member and lead in one transition.
            working.replaceWith(List.of(identity), identity);
            return;
        }
        List<String> members = new ArrayList<>(snapshotMembers);
        String lead;
        if (members.contains(identity)) {
            // Present in the snapshot: shown removed, the lead by the
            // removal rule on (snapshot - candidate).
            members.remove(identity);
            lead = identity.equals(snapshotLead)
                    ? (members.isEmpty() ? null
                            : members.get(members.size() - 1))
                    : snapshotLead;
        } else {
            members.add(identity);
            lead = identity;
        }
        working.replaceWith(members, lead);
    }

    private void endTransaction() {
        snapshotMembers = null;
        snapshotLead = null;
        transactionCandidates = null;
    }
}
