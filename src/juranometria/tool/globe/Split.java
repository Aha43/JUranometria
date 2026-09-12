package juranometria.tool.globe;

import java.io.IOException;
import java.util.Locale;

import juranometria.tool.PlatformEvidence;

/**
 * A globe study's two documents: what the atlas answers, and what
 * this machine answers (Sprint 32, issue #301; classified as #315
 * requires).
 *
 * <p>The gate measured a great deal of ink, and ink is the desktop's
 * answer as much as the atlas's - a font drawn a fraction wider
 * covers more pixels without anything being wrong. Counting objects,
 * projecting a footprint, ranking what a layer recovers and deciding
 * whether a name found a home are the atlas's own answers and
 * reproduce anywhere.
 *
 * <p>So each study writes both: the portable half on stdout, where
 * the Makefile pins it to bytes under {@code docs/studies}, and the
 * machine's half beside it in {@code platform.md}, held to
 * reproducing within one environment. A report that mixed them could
 * be held to neither.
 */
final class Split {

    private final StringBuilder machine = new StringBuilder();

    private final String path;

    /**
     * @param slug the study's directory under {@code docs/studies}
     * @param title the platform record's heading
     * @param sprint the issue line every study document carries
     */
    Split(String slug, String title, String sprint) {
        this.path = "docs/studies/" + slug + "/platform.md";
        PlatformEvidence.preface(machine, title, sprint);
    }

    /** A sentence saying which document carries what. */
    void beside(String whatIsPortable) {
        machine.append(whatIsPortable).append("\n\n");
    }

    void portable(String line) {
        System.out.println(line);
    }

    void portablef(String format, Object... arguments) {
        System.out.printf(Locale.ROOT, format, arguments);
    }

    void machine(String line) {
        machine.append(line).append('\n');
    }

    void machinef(String format, Object... arguments) {
        machine.append(String.format(Locale.ROOT, format, arguments));
    }

    /** Writes the machine's half where the contract looks for it. */
    void write() throws IOException {
        PlatformEvidence.write(machine, path);
    }
}
