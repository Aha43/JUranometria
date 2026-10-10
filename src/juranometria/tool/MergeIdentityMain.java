package juranometria.tool;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Judges a facts file written by {@code scripts/verify-merge-identity.sh}
 * (issue #494) and says whether the landed merge is the tree its pull
 * request already qualified.
 *
 * <pre>
 *   MergeIdentityMain --facts build/merge-identity/facts.properties
 *                     [--github-output FILE] [--summary FILE]
 * </pre>
 *
 * <p>Prints the verdict with every proven fact and every reason it was not
 * proven. Writes {@code qualified=true} or {@code qualified=false} to the
 * GitHub output when asked; an unreadable facts file is
 * {@code qualified=false}. Exits 0 either way: a full route is an
 * answer, not an error.
 */
public final class MergeIdentityMain {

    private MergeIdentityMain() {
    }

    public static void main(String[] args) throws IOException {
        Path facts = null;
        Path output = null;
        Path summary = null;
        for (int i = 0; i + 1 < args.length; i += 2) {
            switch (args[i]) {
                case "--facts" -> facts = Path.of(args[i + 1]);
                case "--github-output" -> output = Path.of(args[i + 1]);
                case "--summary" -> summary = Path.of(args[i + 1]);
                default -> throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        Map<String, String> read = new LinkedHashMap<>();
        if (facts != null && Files.isReadable(facts)) {
            Properties p = new Properties();
            try (Reader in = Files.newBufferedReader(facts, StandardCharsets.UTF_8)) {
                p.load(in);
            }
            for (String key : p.stringPropertyNames()) {
                read.put(key, p.getProperty(key));
            }
        }
        MergeIdentity.Verdict verdict = MergeIdentity.judge(read);
        String text = render(verdict);
        System.out.print(text);
        if (output != null) {
            Files.writeString(output, "qualified=" + verdict.qualified() + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        if (summary != null) {
            Files.writeString(summary, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }
    }

    /** The verdict as the run summary and the console show it. */
    static String render(MergeIdentity.Verdict verdict) {
        StringBuilder out = new StringBuilder();
        out.append(verdict.qualified()
                ? "### Landed tree already qualified: structurally verified\n\n"
                : "### Landed tree not proven qualified: the full route runs\n\n");
        for (String fact : verdict.proven()) {
            out.append("- proven: ").append(fact).append('\n');
        }
        for (String reason : verdict.reasons()) {
            out.append("- not proven: ").append(reason).append('\n');
        }
        out.append('\n').append("Main-only workflows (pages, release) run on their own"
                + " triggers whatever this says.\n");
        return out.toString();
    }
}
