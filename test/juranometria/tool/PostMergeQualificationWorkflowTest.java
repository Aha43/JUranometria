package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The workflow half of the exact-tree post-merge rule (issue #494,
 * docs/decisions/post-merge-qualification.md): every pull-request run
 * records the trial merge it qualified; a push to main verifies the
 * landed tree against it first, and skips the suites only on a proof;
 * main-only deployments and the release never consult it.
 */
class PostMergeQualificationWorkflowTest {

    private static String read(String name) throws IOException {
        return Files.readString(Path.of(".github", "workflows", name));
    }

    /** One job's block in a workflow, from its key to the next top-level job. */
    private static String job(String workflow, String name) {
        int at = workflow.indexOf("\n  " + name + ":\n");
        assertTrue(at >= 0, "the job " + name + " exists");
        int end = workflow.length();
        java.util.regex.Matcher next = java.util.regex.Pattern.compile("\n  [a-z][a-z-]*:\n")
                .matcher(workflow);
        if (next.find(at + 1)) {
            end = next.start();
        }
        return workflow.substring(at, end);
    }

    @Test
    void everyPullRequestRunRecordsTheTrialMergeItQualifies() throws IOException {
        String classify = read("classify.yml");
        assertTrue(classify.contains("- name: Record the trial merge this run qualifies\n"
                + "        if: github.event_name == 'pull_request'"), "on pull requests only");
        assertTrue(classify.contains("echo \"merge=$(git rev-parse HEAD)\""),
                "the commit the run checked out, which is the trial merge");
        assertTrue(classify.contains("echo \"route=${{ steps.route.outputs.route }}\""),
                "and the route it was qualified on");
        assertTrue(classify.contains("name: qualified-merge"), "kept as the run's artifact");
    }

    @Test
    void aPushToMainVerifiesFirstAndSkipsOnlyOnAProof() throws IOException {
        String test = read("test.yml");
        String landing = job(test, "landing");
        assertTrue(landing.contains("if: github.event_name == 'push'"),
                "the landing check runs on a push to main and never on a pull request");
        assertTrue(landing.contains("scripts/verify-merge-identity.sh \"${{ github.sha }}\""),
                "it verifies the pushed commit itself");
        assertTrue(landing.contains("continue-on-error: true"),
                "a verifier that cannot answer leaves the output empty: the full route runs");
        assertTrue(landing.contains("actions: read") && !landing.contains("write"),
                "it reads runs and artifacts and writes nothing");
        for (String name : new String[] {"test", "display", "classify"}) {
            String block = job(test, name);
            assertTrue(block.contains("needs: landing\n"), name + " follows the landing check");
            assertTrue(block.contains(
                    "if: ${{ !cancelled() && needs.landing.outputs.qualified != 'true' }}"),
                    name + " is skipped only when the landing is proved: anything else runs it");
        }
        // PR #496's first CI run: with `landing` skipped on a pull request,
        // GitHub skipped `evidence` too, because a job downstream of a
        // skipped one is skipped unless its condition says otherwise. Every
        // job below classify must therefore name classify's own result.
        for (String name : new String[] {"evidence", "interaction-evidence"}) {
            String block = job(test, name);
            assertTrue(block.contains("needs: classify"), name + " follows the classification");
            assertTrue(block.contains("if: ${{ !cancelled() && needs.classify.result == 'success'"),
                    name + " runs whenever the classification succeeded, never dropped because"
                            + " the skipped landing check sits above it");
        }
    }

    @Test
    void mainOnlyDeploymentsAndTheReleaseNeverConsultIt() throws IOException {
        for (String name : new String[] {"pages.yml", "release.yml", "app-image.yml",
                "dist.yml"}) {
            String workflow = read(name);
            assertFalse(workflow.contains("landing") || workflow.contains("verify-merge-identity"),
                    name + " runs on its own triggers, whatever the landing check says");
        }
    }

    @Test
    void theVerifierIsAScriptOverThePureJudge() throws IOException {
        Path script = Path.of("scripts", "verify-merge-identity.sh");
        assertTrue(Files.isExecutable(script), "the documented command exists");
        String text = Files.readString(script);
        assertTrue(text.contains("src/juranometria/tool/MergeIdentity.java"),
                "it judges with MergeIdentity, the class the contract tests hold");
        assertTrue(text.contains("git rev-list --parents -n 1") && text.contains("^{tree}"),
                "parents and trees come from git objects, not from recorded prose");
    }
}
