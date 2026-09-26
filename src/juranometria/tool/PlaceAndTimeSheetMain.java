package juranometria.tool;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import juranometria.meridian.MeridianModule;
import juranometria.sky.Observer;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.placeandtime.PlaceAndTimeDialog;
import juranometria.ui.placeandtime.PlaceStore;

/**
 * Every word the Place and Time dialog says, in both languages
 * (Sprint 33, issue #350).
 *
 * <p><strong>The real packed dialog</strong>, built through the
 * factory that requires a language - not its controls rearranged in a
 * stand-in panel, and not its content pane alone. A window has
 * channels of its own: its title and the description a screen reader
 * reads when it opens. Photographing the content pane answers for
 * neither, and that is exactly the gap this issue found at the export
 * dialog and then repeated here before it was caught.
 *
 * <p>It installs its own look and feel before any component exists
 * (#362). A generator that inherits whatever a peer left behind is
 * photographing the peer.
 *
 * <p>The five states are reached by setting the module's observer and
 * its switches - the same values a reader's own typing produces -
 * rather than by drawing a picture of a state nothing can be in.
 */
public final class PlaceAndTimeSheetMain {

    /**
     * What this photographer holds still: the application states this window’s size, and packing
     * it would photograph a width no reader meets.
     *
     * <p>Read by the display evidence gate, which refuses a
     * generator that declares one kind and asks the capture
     * coordinator for another.
     */
    public static final SheetCapture.Kind CAPTURE_KIND =
            SheetCapture.Kind.APPLICATION_SIZED;

    private PlaceAndTimeSheetMain() {
    }

    /**
     * Where the sheets go: the committed directory by default, or a
     * directory a caller names as {@code args[0]}.
     *
     * <p>`InterfaceEvidenceGateTest` runs every one of these into a
     * scratch directory and compares the result with what is
     * committed. It can only do that if a generator can be told
     * where to write; one that always writes over the evidence
     * cannot be used to check it.
     */
    private static Path out =
            Path.of("docs/studies/interface-language");

    /** The frozen moment every sheet is taken at. */
    private static final Instant WHEN =
            Instant.parse("2026-03-20T21:33:00Z");

    /** One arrangement of the dialog, and why it is here. */
    private record State(String name, String title, double latitude,
                         double eastLongitude, boolean meridian,
                         boolean horizon, boolean zenith, String note) {
    }

    private static final List<State> STATES = List.of(
            new State("default", "Nothing remembered", 0.0, 0.0,
                    true, true, false,
                    "The released default: the equator at Greenwich."
                            + " Neutral, and honest about being"
                            + " nobody's home. Both coordinates read"
                            + " zero, which is the one place a sign"
                            + " convention cannot be seen - which is"
                            + " why the next two states exist."),
            new State("oslo", "Oslo — north and east", 59.913, 10.752,
                    true, true, false,
                    "East longitude is positive. The number and its"
                            + " sign are notation and are identical in"
                            + " both languages; only the words around"
                            + " them change."),
            new State("santiago", "Santiago — south and west",
                    -33.4489, -70.6693, true, true, false,
                    "South latitude and west longitude are negative."
                            + " A translation that rendered a minus"
                            + " sign differently would put a reader on"
                            + " the wrong side of the planet, so it"
                            + " does not: the field round-trips"
                            + " through Locale.ROOT."),
            new State("lines-off", "Every line off", 59.913, 10.752,
                    false, false, false,
                    "All three switches clear. The words do not"
                            + " change with the switch - a checkbox"
                            + " says what it would draw, not what it"
                            + " is drawing."),
            new State("lines-on", "Every line on", 59.913, 10.752,
                    true, true, true,
                    "All three set, the zenith included. The zenith"
                            + " has no keystroke of its own and its"
                            + " hover does not promise one: the chart"
                            + " keyboard refuses it deliberately"
                            + " (#312), and a tooltip offering a key"
                            + " would be a promise that decision does"
                            + " not make."));

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && !args[0].isBlank()) {
            out = Path.of(args[0]);
        }
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.err.println("a packed dialog needs a display, and a"
                    + " stand-in panel is what the review rejected");
            System.exit(1);
        }
        Files.createDirectories(out);
        StringBuilder said = new StringBuilder();
        said.append("""
                # Every word Place and Time says

                Issue #350. Generated by
                `juranometria.tool.PlaceAndTimeSheetMain` from compiled
                application classes and their classpath resources.
                Packaged acceptance separately verifies that the
                interface language resources ship in the application
                image.

                %s

                ## The window, not a panel

                Each capture is the production dialog built through
                `PlaceAndTimeDialog.packedForStudy`, which has no
                "English if omitted" overload: a caller that forgets a
                language does not compile. The title and the window
                description below are read from the `JDialog` itself,
                because a window says things no walk of its content
                pane will find - the gap this issue met at the export
                dialog and then repeated here.

                Each is photographed at the reviewed 420 px width,
                which is the width a reader gets. An unshown packed
                dialog is pulled back to 326 px one event cycle after
                it is built, because the peer never received the
                floor - there is no window on screen to resize. A
                shown dialog holds 420 px in both languages, measured
                before concluding this was the photographer's problem
                and not the reader's, and the generator restores the
                floor before painting. Without that, three runs
                produced three different sets of images.

                ## What is translated here, and what is not

                Coordinates, the instant, `UTC` and the keystroke
                glyphs are canonical. The degrees and the instant
                round-trip through `Locale.ROOT` because a reader
                edits them; a decimal comma in a field that must parse
                back is a lost place, not a translation. `UTC` is
                handed to the instant's label as an argument rather
                than written inside it.

                Access letters ARE language. A mnemonic marks a letter
                in the item's own displayed word, and the displayed
                word is translated: `L` is in "Latitude" and nowhere
                in "Breddegrad". All eight are listed per language
                below, and the application refuses a letter that is
                not in the label it marks rather than guessing one.

                """.formatted(juranometria.tool.InterfaceLanguageStatus
                        .statement("nb-NO")));

        int sheet = 1;
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText text = InterfaceText.forLanguage(language);
            said.append("## ").append(language.equals("en") ? "English"
                            : "Norsk bokmål").append(" (`")
                    .append(language).append("`)\n\n");
            for (State state : STATES) {
                said.append("### ").append(state.title()).append("\n\n")
                        .append(state.note()).append("\n\n")
                        .append(draw(language, text, state, false,
                                out.resolve("placeandtime-" + language
                                        + "-" + (sheet++) + "-"
                                        + state.name() + ".png")))
                        .append('\n');
            }
            said.append("### The frozen note, dark\n\n")
                    .append("""
                            The one sentence here a reader can only
                            read. It is quiet **visually** and not
                            quiet to a screen reader: `setEnabled(false)`
                            announced it as unavailable, so the weight
                            now comes from the theme's own subdued
                            colour, which is resolved per theme. A
                            colour written down once would be right in
                            one theme and wrong in the other, and the
                            Norwegian sentence is longer than the
                            English one - so it is worth seeing wrapped
                            in the palette where low contrast is
                            easiest to get wrong.

                            """)
                    .append(draw(language, text, STATES.get(1), true,
                            out.resolve("placeandtime-" + language + "-"
                                    + (sheet++) + "-dark.png")))
                    .append('\n');
            said.append(letters(text)).append(deferred());
        }
        Files.writeString(out.resolve("placeandtime-strings.md"),
                said.toString(), StandardCharsets.UTF_8);
        System.out.println("place and time sheets: 12 images and "
                + out.resolve("placeandtime-strings.md"));
    }

    /** The eight access letters, and the words they mark. */
    private static String letters(InterfaceText said) {
        StringBuilder out = new StringBuilder(
                "#### The eight access letters\n\n"
                        + "| control | word shown | letter |\n"
                        + "|---|---|---|\n");
        for (String stem : List.of("placeandtime.latitude",
                "placeandtime.longitude", "placeandtime.instant",
                "placeandtime.meridian", "placeandtime.horizon",
                "placeandtime.zenith", "placeandtime.now",
                "placeandtime.centre")) {
            String label = stem.equals("placeandtime.instant")
                    ? said.say(stem + ".label", "UTC")
                    : said.say(stem + ".label");
            out.append("| `").append(stem).append("` | ")
                    .append(label.replace("|", "\\|")).append(" | ")
                    .append(said.say(stem + ".mnemonic")).append(" |\n");
        }
        return out.append('\n').toString();
    }

    /** What this surface does not yet say, stated rather than staged. */
    private static String deferred() {
        return """
                #### Not shown, because it does not exist

                A coordinate field that cannot parse what was typed
                **reverts to its last good value and says nothing**.
                No picture of a refusal appears above, because the
                application does not draw one; staging a mock-up of a
                message nobody has written would put a promise in the
                evidence that is not in the atlas. It is recorded as a
                separate issue, not resolved here.

                """;
    }

    /** Draws one state in one language and returns its strings. */
    private static String draw(String language, InterfaceText said,
                               State state, boolean dark, Path to)
            throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-study-placeandtime-"
                        + System.nanoTime());
        JFrame[] owner = new JFrame[1];
        PlaceAndTimeDialog[] dialog = new PlaceAndTimeDialog[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                // Stated before any component exists (#362).
                juranometria.app.UiTheme.apply(dark);

                MeridianModule module = new MeridianModule(new Observer(
                        state.latitude(), state.eastLongitude(), WHEN));
                module.showing(state.meridian(), state.horizon(),
                        state.zenith());
                PlaceStore store = PlaceStore.forNode(node);
                store.save(state.latitude(), state.eastLongitude());

                owner[0] = new JFrame("study");
                dialog[0] = PlaceAndTimeDialog.packedForStudy(owner[0],
                        module, store, said);
            });
            return capture(dialog[0], language, to);
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (dialog[0] != null) {
                    dialog[0].dispose();
                }
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
            try {
                // A photograph is not a session and leaves none.
                node.removeNode();
            } catch (java.util.prefs.BackingStoreException leaving) {
                // An empty node is a blemish, not a failure.
            }
        }
    }

    /**
     * The dialog painted, and every channel of it written down.
     *
     * <p>The window's own two channels are read from the dialog
     * before its content is walked, because nothing inside a content
     * pane knows the window's title.
     *
     * <p><strong>Called from the task that built the dialog, with no
     * event-thread flush between.</strong> The constructor packs,
     * raises the width to the reviewed 420 px floor, and lays out
     * again; one event cycle later an unshown window is pulled back
     * to its packed 326 px by the peer, which never received the
     * larger size because there is no window on screen to resize.
     * Measured, not guessed: twelve builds reported 420 px at
     * construction and 326 px after a flush, and this generator
     * produced a different set of images on each of three runs
     * before the flush was removed.
     *
     * <p>A reader never meets that width. {@code open} shows the
     * dialog, and a shown one holds 420 px in both languages -
     * checked before deciding this was a photographer's problem
     * rather than theirs. The picture here is the reader's.
     */
    private static String capture(PlaceAndTimeDialog dialog,
                                  String language, Path to)
            throws Exception {
        // Application-sized: this dialog raises its packed width to
        // its own reviewed floor, so its size is a policy - and the
        // policy says what it asks for as a value (#380). The
        // capture is held to that declared content, not to whatever
        // the unshown window reports, because the peer can pull the
        // window back to its packed width at any moment.
        // Names this sheet in the capture trace. Without it a
        // retained pair can only be mapped by counting lines and
        // knowing the order sheets are written in - which is how the
        // first retained Place and Time pair could not be read at
        // all, because two runs' line N are not necessarily the same
        // sheet.
        SheetCapture.tracing(to.getFileName() + " language=" + language);
        SheetCapture.Sizing sizing =
                SheetCapture.applicationSized(
                        "PlaceAndTimeDialog.applySizePolicy",
                        new SheetCapture.ApplicationPolicy() {

                            // Both are the dialog's own methods. A
                            // photographer that copied the arithmetic
                            // instead is how the width drifted in the
                            // first place.
                            @Override
                            public java.awt.Dimension declaredContent() {
                                return dialog.sizePolicyContent();
                            }

                            @Override
                            public void apply() {
                                dialog.applySizePolicy();
                            }
                        });
        Set<String> shown = new LinkedHashSet<>();
        Set<String> spoken = new LinkedHashSet<>();
        List<String> letters = new ArrayList<>();
        BufferedImage[] image = new BufferedImage[1];
        String[] window = new String[2];
        // The policy is held, and the geometry recorded, where
        // nothing can undo them - by the coordinator, in the block
        // that paints. This generator did both by hand, correctly
        // and alone; #364 showed that eight others did neither, so
        // the sequence belongs in one place rather than in the one
        // photographer that had learned it.
        image[0] = SheetCapture.take(dialog,
                (javax.swing.JComponent) dialog.getContentPane(),
                sizing, SheetCapture.Premise.none(), () -> {
            Container content = dialog.getContentPane();
            window[0] = dialog.getTitle();
            window[1] = dialog.getAccessibleContext()
                    .getAccessibleDescription();
            BufferedImage drawn = new BufferedImage(
                    Math.max(1, content.getWidth()),
                    Math.max(1, content.getHeight()),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = drawn.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(content.getBackground() == null
                        ? Color.WHITE : content.getBackground());
                g.fillRect(0, 0, drawn.getWidth(), drawn.getHeight());
                content.paint(g);
            } finally {
                g.dispose();
            }
            collect(content, shown, spoken, letters);
            return drawn;
        });
        ImageIO.write(image[0], "png", to.toFile());

        StringBuilder out = new StringBuilder();
        out.append("![](").append(to.getFileName()).append(")\n\n")
                .append("Packed ").append(image[0].getWidth())
                .append(" × ").append(image[0].getHeight())
                .append(" px, `").append(language).append("`.\n\n")
                .append("| channel | words |\n|---|---|\n");
        row(out, "window title", window[0]);
        row(out, "window, spoken", window[1]);
        for (String one : shown) {
            row(out, "shown or hovered", one);
        }
        for (String one : spoken) {
            row(out, "spoken", one);
        }
        row(out, "access letters", String.join(" ", letters));
        return out.append('\n').toString();
    }

    private static void row(StringBuilder out, String what, String words) {
        out.append("| ").append(what).append(" | ")
                .append(words == null ? "—"
                        : words.replace("|", "\\|").replace("\n", " / "))
                .append(" |\n");
    }

    private static void collect(Container from, Set<String> shown,
                                Set<String> spoken, List<String> letters) {
        for (Component child : from.getComponents()) {
            if (child instanceof JComponent widget) {
                if (widget instanceof AbstractButton button) {
                    add(shown, button.getText());
                    letter(letters, button.getMnemonic());
                }
                if (widget instanceof JLabel label) {
                    add(shown, label.getText());
                    letter(letters, label.getDisplayedMnemonic());
                }
                if (widget instanceof javax.swing.JTextField field) {
                    add(shown, field.getText());
                }
                add(shown, widget.getToolTipText());
                if (widget.getAccessibleContext() != null) {
                    add(spoken, widget.getAccessibleContext()
                            .getAccessibleName());
                    add(spoken, widget.getAccessibleContext()
                            .getAccessibleDescription());
                }
            }
            if (child instanceof Container nested) {
                collect(nested, shown, spoken, letters);
            }
        }
    }

    private static void letter(List<String> letters, int keyCode) {
        if (keyCode != 0) {
            letters.add(String.valueOf((char) keyCode));
        }
    }

    private static void add(Set<String> said, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        // A wrap becomes a space and is never deleted: Chart Options
        // once published "angitt ikatalogen" by deleting one.
        said.add(text.replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ").trim());
    }
}
