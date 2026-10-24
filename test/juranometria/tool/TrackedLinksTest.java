package juranometria.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tree tracks no link to another checkout (issue #619).
 *
 * <p>A worktree used to be set up with {@code lib} and {@code imports}
 * as symbolic links to the main checkout's directories, so its builds
 * could share the pinned jars and the imported sources. One
 * {@code git add -A} in such a worktree committed both links - absolute,
 * pointing at one machine's path, and in the main checkout pointing at
 * themselves - and the landing's {@code make run} from a clean clone
 * failed with "Too many levels of symbolic links". Nothing refused it:
 * in the worktree the links resolved, so every gate built and ran; on
 * CI the dependency cache restored a real {@code lib/} over the dangling
 * link before the bootstrap ran, so the downloader found its jars
 * "already existing" and never saw the link; {@code imports} is read by
 * no job. This test reads the index, where the fault lives.
 *
 * <p>Three holds: {@code lib} and {@code imports} are tracked as nothing -
 * not as a link, not as a directory, not as a file; no tracked symbolic
 * link anywhere points outside the tree or at an absolute path; and the
 * ignore rules refuse both names, so {@code git add -A} cannot do it
 * again without {@code -f}.
 */
class TrackedLinksTest {

    private static final Path ROOT = Path.of(".");

    private record Run(int status, String output) {
    }

    private static Run git(String... arguments) throws Exception {
        String[] command = new String[arguments.length + 3];
        command[0] = "git";
        command[1] = "-C";
        command[2] = ROOT.toAbsolutePath().toString();
        System.arraycopy(arguments, 0, command, 3, arguments.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        return new Run(process.waitFor(), output.trim());
    }

    @Test
    void libAndImportsAreTrackedAsNothing() throws Exception {
        Run entries = git("ls-files", "-s", "--", "lib", "imports");
        assertEquals(0, entries.status(), entries.output());
        assertEquals("", entries.output(), "the dependencies and the imported sources"
                + " are downloaded per checkout, never tracked - not even as a link"
                + " (#619):\n" + entries.output());
    }

    @Test
    void noTrackedSymbolicLinkLeavesTheTreeOrNamesAnAbsolutePath() throws Exception {
        Run entries = git("ls-files", "-s");
        assertEquals(0, entries.status(), entries.output());
        List<String> offenders = new ArrayList<>();
        for (String line : entries.output().split("\n")) {
            if (!line.startsWith("120000 ")) {
                continue;
            }
            // "120000 <blob> <stage>\t<path>"
            String[] head = line.split("\t", 2);
            String blob = head[0].split(" ")[1];
            String path = head[1];
            String target = git("cat-file", "-p", blob).output();
            Path from = Path.of(path).getParent();
            Path resolved = (from == null ? Path.of(target) : from.resolve(target)).normalize();
            if (Path.of(target).isAbsolute() || resolved.startsWith("..")) {
                offenders.add(path + " -> " + target);
            }
        }
        assertEquals(List.of(), offenders, "a tracked link that names an absolute path or"
                + " leaves the tree exists on one machine only (#619)");
    }

    @Test
    void theIgnoreRulesRefuseBothNamesWhateverTheirKind() throws Exception {
        for (String name : List.of("lib", "imports", "lib/flatlaf.jar", "imports/a.csv")) {
            Run ignored = git("check-ignore", "-q", "--no-index", name);
            assertEquals(0, ignored.status(), name + " is ignored, so git add -A cannot"
                    + " track it again as a link, a directory or a file (#619): "
                    + ignored.output());
        }
        assertFalse(git("check-ignore", "-q", "--no-index", "src/juranometria").status() == 0,
                "the source tree is not ignored");
        assertTrue(git("check-ignore", "-q", "--no-index", "lib").status() == 0);
    }
}
