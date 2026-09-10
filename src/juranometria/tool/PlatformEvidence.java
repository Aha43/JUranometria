package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Evidence that is one machine's answer, said so (Sprint 31, issue
 * #315).
 *
 * <p>Some of what a study measures is the same everywhere: how many
 * switches there are, which letter reaches each, what a scheme would
 * collide with. Some of it is what the desktop in front of you calls
 * things - {@code ⌃K} on a Mac and {@code Ctrl+K} on Linux for the
 * same modifier - or what its look and feel binds into a text field,
 * or how wide a font draws a word.
 *
 * <p>Holding the second kind to the same bytes on two machines is not
 * a strict contract but a false one, and the project already decided
 * against it: the 1.0 contract records pixel equality
 * <em>per environment</em> and does not require it across
 * environments. A report that mixes both kinds cannot be held to
 * either, so the studies split them - the portable half printed where
 * the contract pins its bytes, the platform half written beside it
 * with the machine that produced it named.
 *
 * <p>The platform half is still evidence, and still checked: it must
 * reproduce byte for byte <strong>within one environment</strong>, and
 * what it says must be true of that environment now. What it may not
 * do is claim to be true of every environment.
 */
public final class PlatformEvidence {

    private PlatformEvidence() {
    }

    /**
     * The token a portable report uses where this desktop's own word
     * for the menu modifier would otherwise appear.
     */
    public static final String MENU_MODIFIER = "<menu>";

    /**
     * The same text with this platform's modifier spelling replaced
     * by a token.
     *
     * <p>Not a normalisation of everything - that would hide the
     * difference rather than classify it. One substitution, for the
     * one thing the toolkit spells differently on each desktop, so
     * that what remains is the atlas's own answer and reproduces
     * anywhere.
     */
    public static String portable(String text) {
        // Longest first, and the whole keystroke rather than only its
        // modifier: a desktop spells the key as well as the modifier,
        // and `⌘=` against `Ctrl+Equals` differs in both halves. What
        // is left behind is which action was quoted, which is the
        // atlas's own answer (#315).
        String portable = text.replace(
                juranometria.app.ChartKeys.prefixText(), PREFIX);
        List<juranometria.ui.Shortcuts.Shortcut> byLength =
                new java.util.ArrayList<>(
                        juranometria.ui.Shortcuts.all());
        byLength.sort(java.util.Comparator.comparingInt(
                (juranometria.ui.Shortcuts.Shortcut one)
                        -> one.text().length()).reversed());
        for (juranometria.ui.Shortcuts.Shortcut shortcut : byLength) {
            portable = portable.replace(shortcut.text(),
                    "<key:" + shortcut.id() + ">");
        }
        String modifier = juranometria.ui.Shortcuts.menuModifierText();
        return modifier.isEmpty() ? portable
                : portable.replace(modifier, MENU_MODIFIER);
    }

    /** The token standing in for the chart keyboard's own prefix. */
    public static final String PREFIX = "<prefix>";

    /**
     * The marker a report prints when its numbers are this machine's.
     *
     * <p>A study that measures ink, font extents or encoded sizes is
     * measuring the desktop as much as the atlas. Saying so in the
     * document is what lets the contract hold it to the right thing -
     * reproducing here, not matching a recording made elsewhere - and
     * lets a reader of the document know which kind of number they
     * are reading.
     */
    public static final String OBSERVED_MARK = "Platform observation";

    /** The banner, with the machine that took the measurements. */
    public static String observed(String whatIsPortable) {
        return "> **" + OBSERVED_MARK + ".** The measurements below"
                + " are this machine's:\n> ink counts, font extents"
                + " and encoded sizes depend on the fonts and the\n>"
                + " rasteriser in front of them, and another machine"
                + " measures differently\n> without anything being"
                + " wrong. Held to reproducing here, never to"
                + " matching\n> another machine's recording. "
                + whatIsPortable + "\n>\n> Recorded on: `"
                + machine() + "`\n\n";
    }

    /** The machine, in one line. */
    public static String machine() {
        return System.getProperty("os.name") + " "
                + System.getProperty("os.version") + "/"
                + System.getProperty("os.arch") + "/"
                + System.getProperty("java.vendor") + " "
                + System.getProperty("java.version");
    }

    /** The heading every platform record carries. */
    public static void preface(StringBuilder out, String title,
                               String sprint) {
        out.append("# ").append(title).append("\n\n");
        out.append(sprint).append("\n\n");
        out.append("**This is one machine's answer.** What is written"
                + " here is what the desktop\nthis was generated on"
                + " calls things and binds; another desktop answers"
                + " differently\nand is not wrong. The contract holds"
                + " it to reproducing within an environment,\nnever"
                + " across two - the portable half of this study is"
                + " the document beside it.\n\n");
        out.append("| the machine | |\n|---|---|\n");
        out.append("| operating system | ")
                .append(System.getProperty("os.name")).append(" |\n");
        out.append("| architecture | ")
                .append(System.getProperty("os.arch")).append(" |\n");
        out.append("| Java | ")
                .append(System.getProperty("java.version")).append(" |\n");
        out.append("| look and feel | ")
                .append(javax.swing.UIManager.getLookAndFeel() == null
                        ? "none"
                        : javax.swing.UIManager.getLookAndFeel().getName())
                .append(" |\n");
        out.append("| headless | ")
                .append(java.awt.GraphicsEnvironment.isHeadless())
                .append(" |\n\n");
    }

    /** Writes a platform record where the contract looks for it. */
    public static void write(StringBuilder out, String path)
            throws IOException {
        Path file = Path.of(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    /** A line naming the environment, for a provenance record. */
    public static String environment() {
        return String.format(Locale.ROOT, "%s %s, Java %s",
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                System.getProperty("java.version"));
    }
}
