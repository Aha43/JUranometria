package juranometria.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every CI job that can fail keeps the interface gate's evidence (#393).
 *
 * <p>The runner is gone once a job ends, so whatever a failure left
 * in {@code build/} and CI did not upload survives only as log text.
 * #378 made a failed generator run keep its output, trace and summary
 * in {@code build/interface-gate-failures}; a disagreement between two
 * runs is kept in {@code build/interface-gate-evidence}. PR #388's
 * second run failed on exactly such a disagreement - a chooser
 * photograph - and CI uploaded only the first directory, so the two
 * runs that disagreed were lost with the runner.
 *
 * <p>Read from the workflow itself, so the rule cannot drift from the
 * file that runs: each of the three suite jobs has a step that runs
 * only on failure, uploads both directories under a name of its own,
 * and does not fail when they are absent.
 */
class CiFailureEvidenceTest {

    private static final Path WORKFLOW =
            Path.of(".github", "workflows", "test.yml");

    @Test
    void everyJobThatCanFailUploadsBothEvidenceDirectories()
            throws Exception {
        String workflow = Files.readString(WORKFLOW,
                StandardCharsets.UTF_8);
        List<String> missing = new ArrayList<>();
        for (String job : List.of("test", "display", "evidence")) {
            String block = job(workflow, job);
            String step = uploadStep(block);
            if (step == null) {
                missing.add(job + ": no failure-only upload step");
                continue;
            }
            for (String required : List.of(
                    "if: failure()",
                    "uses: actions/upload-artifact@",
                    "name: interface-gate-evidence-" + job,
                    "build/interface-gate-failures",
                    "build/interface-gate-evidence",
                    "if-no-files-found: ignore")) {
                if (!step.contains(required)) {
                    missing.add(job + ": its upload step lacks `"
                            + required + "`");
                }
            }
        }
        assertEquals(List.of(), missing,
                "a job that can fail must keep what the failure left,"
                        + " or the next disagreement is a log line and"
                        + " nothing to inspect");
    }

    /** One top-level job's block, from its key to the next job's. */
    private static String job(String workflow, String name) {
        String key = "\n  " + name + ":\n";
        int start = workflow.indexOf(key);
        if (start < 0) {
            return "";
        }
        int from = start + key.length();
        int end = workflow.length();
        java.util.regex.Matcher next = java.util.regex.Pattern
                .compile("\n  [a-z][a-z-]*:\n").matcher(workflow);
        if (next.find(from)) {
            end = next.start();
        }
        return workflow.substring(start, end);
    }

    /**
     * The step that uploads on failure, as its non-comment lines, or
     * null. Comments are dropped first: a comment that names a
     * directory uploads nothing, and the first version of this check
     * was satisfied by one.
     */
    private static String uploadStep(String block) {
        for (String step : block.split("\n      - ")) {
            String said = step.lines()
                    .filter(line -> !line.strip().startsWith("#"))
                    .collect(java.util.stream.Collectors.joining("\n"));
            if (said.contains("upload-artifact")
                    && said.contains("if: failure()")) {
                return said;
            }
        }
        return null;
    }
}
