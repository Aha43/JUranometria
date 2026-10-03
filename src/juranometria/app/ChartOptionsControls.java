package juranometria.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.SymbolFamily;

/**
 * Chart Options' controls, whoever holds them (#443, ruled on #442):
 * the seventeen boxes in their four subjects, and Restore Defaults.
 * The Chart Options dialog holds one set in its tabs; the Controls
 * companion holds one set in its collapsible groups. Neither is the
 * reference the other copies - they are two hosts of this one class,
 * so they offer the same controls with the same semantics.
 *
 * <p><strong>Shared immediate.</strong> Every box writes the
 * controller's <em>current</em> value with its own one field changed,
 * and that is saved at once - the chart keyboard's gesture exactly.
 * The controls follow the controller through one subscription, so a
 * change made anywhere is shown here at once, ticks and enabled states
 * both, with a child's remembered choice kept while its master is
 * off. Restore Defaults asks first; confirmed, it is one accepted
 * change. The subscription is released when the component holding the
 * controls is disposed.
 *
 * <p>The controls own no layout beyond each subject's own column: how
 * the subjects are arranged - tabs, or groups - is the host's.
 */
public final class ChartOptionsControls {

    /** Marks every subject column this class built, so a host can be checked. */
    public static final String MARK = "juranometria.chartOptionsControls";

    /** One subject: a stable id, its title in the host's language, its column. */
    public record Subject(String id, String title, JComponent column) {
    }

    private final List<Subject> subjects;
    private final JButton restoreDefaults;
    private final ChartOptionsController.Subscription following;

    /**
     * @param restoreConfirmed asked before Restore Defaults is applied;
     *     nothing takes it back, so the reader confirms it first
     */
    public ChartOptionsControls(ChartOptionsController controller,
                                BooleanSupplier restoreConfirmed,
                                juranometria.ui.language.InterfaceText said) {
        ChartOptions initial = controller.options();

        JCheckBox dsos = ChartOptionsDialog.checkBox(
                said.say("chartoptions.deepSkyObjects.label"), 'D',
                initial.deepSkyObjects(),
                said.say("chartoptions.deepSkyObjects.a11y"),
                said.say("chartoptions.deepSkyObjects.explain"),
                ChartKeys.DEEP_SKY, said);
        JCheckBox labels = ChartOptionsDialog.checkBox(said.say("chartoptions.deepSkyLabels.label"), 'l', initial.deepSkyLabels(),
                said.say("chartoptions.deepSkyLabels.a11y"),
                said.say("chartoptions.deepSkyLabels.explain"),
                "chart.deepSkyLabels", said);
        List<JCheckBox> families = new ArrayList<>();
        for (SymbolFamily family : SymbolFamily.values()) {
            // Through the shared family-text seam (owner ruling,
            // #350): Chart Options, the Inspector, the legends and
            // exported furniture all ask the same question, and a
            // translation written twice is one that drifts.
            juranometria.ui.language.SymbolFamilyText families_ =
                    juranometria.ui.language.SymbolFamilyText.in(said);
            families.add(ChartOptionsDialog.checkBox(families_.label(family),
                    family.mnemonic(), initial.family(family),
                    families_.accessibleName(family),
                    families_.description(family),
                    ChartOptionsDialog.familyKey(family), said));
        }

        JCheckBox figures = ChartOptionsDialog.checkBox(
                said.say("chartoptions.constellationFigures.label"), 'f',
                initial.constellationFigures(),
                said.say("chartoptions.constellationFigures.a11y"),
                said.say("chartoptions.constellationFigures.explain"),
                ChartKeys.FIGURES, said);
        JCheckBox boundaries = ChartOptionsDialog.checkBox(said.say("chartoptions.constellationBoundaries.label"), 'b', initial.constellationBoundaries(),
                said.say("chartoptions.constellationBoundaries.a11y"),
                said.say("chartoptions.constellationBoundaries.explain"),
                "chart.constellationBoundaries", said);
        JCheckBox names = ChartOptionsDialog.checkBox(said.say("chartoptions.constellationNames.label"), 'n', initial.constellationNames(),
                said.say("chartoptions.constellationNames.a11y"),
                said.say("chartoptions.constellationNames.explain"),
                "chart.constellationNames", said);
        JCheckBox starNames = ChartOptionsDialog.checkBox(said.say("chartoptions.starNames.label"), 'S', initial.starNames(),
                said.say("chartoptions.starNames.a11y"),
                said.say("chartoptions.starNames.explain"),
                "chart.starNames", said);
        JCheckBox bayerLetters = ChartOptionsDialog.checkBox(said.say("chartoptions.bayerLetters.label"), 'y', initial.bayerLetters(),
                said.say("chartoptions.bayerLetters.a11y"),
                said.say("chartoptions.bayerLetters.explain"),
                "chart.bayerLetters", said);
        JCheckBox flamsteedNumbers = ChartOptionsDialog.checkBox(said.say("chartoptions.flamsteedNumbers.label"), 'F', initial.flamsteedNumbers(),
                said.say("chartoptions.flamsteedNumbers.a11y"),
                said.say("chartoptions.flamsteedNumbers.explain"),
                "chart.flamsteedNumbers", said);
        JCheckBox grid = ChartOptionsDialog.checkBox(
                said.say("chartoptions.equatorialGrid.label"), 'E',
                initial.equatorialGrid(),
                said.say("chartoptions.equatorialGrid.a11y"),
                said.say("chartoptions.equatorialGrid.explain"),
                "chart.equatorialGrid", said);
        JCheckBox titleBlock = ChartOptionsDialog.checkBox(
                said.say("chartoptions.titleBlock.label"), 'T',
                initial.titleBlock(),
                said.say("chartoptions.titleBlock.a11y"),
                said.say("chartoptions.titleBlock.explain"),
                "chart.titleBlock", said);
        JCheckBox magnitudeKey = ChartOptionsDialog.checkBox(
                said.say("chartoptions.magnitudeKey.label"), 'k',
                initial.magnitudeKey(),
                said.say("chartoptions.magnitudeKey.a11y"),
                said.say("chartoptions.magnitudeKey.explain"),
                "chart.magnitudeKey", said);
        JCheckBox blackSky = ChartOptionsDialog.checkBox(
                said.say("chartoptions.blackSky.label"), 'B',
                initial.palette() == ChartPalette.BLACK_SKY,
                said.say("chartoptions.blackSky.a11y"),
                said.say("chartoptions.blackSky.explain"),
                "chart.blackSky", said);

        // Each box and the switch it is: its keyboard id, the one
        // registry both routes read.
        java.util.Map<JCheckBox, String> switches = new java.util.LinkedHashMap<>();
        switches.put(dsos, ChartKeys.DEEP_SKY);
        for (int i = 0; i < families.size(); i++) {
            switches.put(families.get(i),
                    ChartOptionsDialog.familyKey(SymbolFamily.values()[i]));
        }
        switches.put(labels, "chart.deepSkyLabels");
        switches.put(starNames, "chart.starNames");
        switches.put(bayerLetters, "chart.bayerLetters");
        switches.put(flamsteedNumbers, "chart.flamsteedNumbers");
        switches.put(figures, ChartKeys.FIGURES);
        switches.put(boundaries, "chart.constellationBoundaries");
        switches.put(names, "chart.constellationNames");
        switches.put(grid, "chart.equatorialGrid");
        switches.put(titleBlock, "chart.titleBlock");
        switches.put(magnitudeKey, "chart.magnitudeKey");
        switches.put(blackSky, "chart.blackSky");
        for (java.util.Map.Entry<JCheckBox, String> each : switches.entrySet()) {
            JCheckBox box = each.getKey();
            String id = each.getValue();
            // One field, onto the controller's current value: never a
            // whole value assembled from these boxes, which may have
            // been built before a change made elsewhere.
            box.addActionListener(event -> controller.accept(
                    ChartSwitches.withChart(controller.options(), id,
                            box.isSelected())));
        }

        // Following the controller: the ticks, and the two decided
        // dependencies with the five families, which the master
        // governs while they remember. setSelected fires no action,
        // so following never writes.
        following = controller.onChange(current -> {
            for (java.util.Map.Entry<JCheckBox, String> each
                    : switches.entrySet()) {
                each.getKey().setSelected(ChartSwitches.isOn(current,
                        each.getValue()));
            }
            labels.setEnabled(current.deepSkyObjects());
            names.setEnabled(current.constellationFigures());
            for (JCheckBox family : families) {
                family.setEnabled(current.deepSkyObjects());
            }
        });

        subjects = List.of(
                new Subject("deepsky", said.say("chartoptions.tab.deepsky"),
                        marked(ChartOptionsDialog.deepSkyTab(dsos, families,
                                labels, said))),
                new Subject("stars", said.say("chartoptions.tab.stars"),
                        marked(ChartOptionsDialog.column(starNames,
                                bayerLetters, flamsteedNumbers))),
                new Subject("constellations",
                        said.say("chartoptions.tab.constellations"),
                        marked(ChartOptionsDialog.column(figures, boundaries,
                                names))),
                new Subject("chart", said.say("chartoptions.tab.chart"),
                        marked(ChartOptionsDialog.column(grid, titleBlock,
                                magnitudeKey, blackSky))));

        restoreDefaults = new JButton(said.say("chartoptions.defaults.label"));
        restoreDefaults.setMnemonic('R');
        restoreDefaults.getAccessibleContext().setAccessibleName(
                said.say("chartoptions.defaults.a11y"));
        juranometria.ui.Explain.control(restoreDefaults,
                said.say("chartoptions.defaults.hover"),
                said.say("chartoptions.defaults.explain"));
        // Asked first, with Cancel the safe answer (ruled on #442):
        // confirmed, it is one accepted change, saved, and every
        // presentation shows it through its subscription.
        restoreDefaults.addActionListener(event -> {
            if (restoreConfirmed.getAsBoolean()) {
                controller.restoreDefaults();
            }
        });
    }

    private static JComponent marked(JComponent column) {
        column.putClientProperty(MARK, Boolean.TRUE);
        return column;
    }

    /** The four subjects, in order: Deep sky, Stars, Constellations, Chart. */
    public List<Subject> subjects() {
        return subjects;
    }

    /** Restore Defaults, which asks first. */
    public JButton restoreDefaults() {
        return restoreDefaults;
    }

    /**
     * Releases the subscription when the holder is disposed - the
     * window it is in going away - so reopening adds no listener.
     */
    public void releaseWith(JComponent holder) {
        boolean[] shown = new boolean[1];
        holder.addHierarchyListener(event -> {
            if ((event.getChangeFlags()
                    & java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED) == 0) {
                return;
            }
            if (holder.isDisplayable()) {
                shown[0] = true;
            } else if (shown[0]) {
                following.cancel();
            }
        });
    }

    /**
     * These controls as the Controls companion holds them (#443, ruled
     * on #442): the four subjects as groups that each remember their
     * own collapse - Deep sky introduced collapsed, the rest open - and
     * Restore Defaults below them. The groups' collapse is the
     * companion's presentation, kept in its store; what the boxes show
     * is the controller's. The descriptions are wrapped narrow before
     * the companion knows its width, and re-wrapped to the room their
     * columns get whenever it changes.
     */
    public JComponent inCompanion(juranometria.ui.companion.CompanionStore store,
                                  juranometria.ui.language.InterfaceText said) {
        javax.swing.JPanel held = new javax.swing.JPanel();
        held.setLayout(new javax.swing.BoxLayout(held,
                javax.swing.BoxLayout.Y_AXIS));
        for (Subject subject : subjects) {
            held.add(juranometria.ui.companion.CompanionSection.remembered(
                    "chartoptions." + subject.id(), subject.title(),
                    subject.column(), said, store,
                    subject.id().equals("deepsky")));
        }
        javax.swing.JPanel actions = new javax.swing.JPanel(
                new java.awt.BorderLayout());
        actions.setBorder(javax.swing.BorderFactory.createEmptyBorder(
                10, 14, 12, 14));
        actions.add(restoreDefaults, java.awt.BorderLayout.WEST);
        held.add(actions);
        ChartOptionsDialog.wrapDescriptions(held, NARROW_WRAP);
        held.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                ChartOptionsDialog.rewrapToColumns(held);
            }
        });
        releaseWith(held);
        return held;
    }

    /**
     * The width descriptions are first wrapped at in the companion, before
     * it is laid out: narrow enough never to make the companion wider
     * than its own default asks.
     */
    static final int NARROW_WRAP = 200;

    /** Releases the subscription now. */
    public void release() {
        following.cancel();
    }
}
