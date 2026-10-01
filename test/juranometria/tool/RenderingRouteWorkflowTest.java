package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The workflows consult the rendering-neutral gate where the owner
 * ruled, and nowhere else (issue #398).
 *
 * <p>Read from the workflow files themselves, as
 * {@code CiFailureEvidenceTest} does, so the rule cannot drift from
 * what runs. Three things have to hold. The expensive jobs - the
 * evidence contract, the native images, the portable archive - run
 * only on the wide route. The unit and display suites never consult
 * the route, because they are the required checks and a change that
 * skipped them would have skipped everything. And the release
 * workflow does not gate on it at all: it calls the packaging
 * workflows on a tag, which the classifier answers wide by event.
 */
class RenderingRouteWorkflowTest {

    private static final Path WORKFLOWS = Path.of(".github", "workflows");

    private static final String CALL =
            "uses: ./.github/workflows/classify.yml";

    private static final String GATE =
            "if: needs.classify.outputs.route == 'wide'";

    @Test
    void theClassifierWorkflowRunsTheClassifierAgainstTheBase()
            throws IOException {
        String classify = read("classify.yml");
        List<String> missing = new ArrayList<>();
        for (String required : List.of(
                "workflow_call:",
                "route:",
                "value: ${{ jobs.classify.outputs.route }}",
                "fetch-depth: 0",
                "run: make classes",
                "juranometria.tool.ChangeClassifierMain",
                "git merge-base \"${{ github.event.pull_request.base.sha }}\" HEAD",
                "git ls-files --others --exclude-standard",
                "--event \"${{ github.event_name }}\"",
                "--changed build/changed-paths.txt",
                "--github-output \"$GITHUB_OUTPUT\"",
                "--summary \"$GITHUB_STEP_SUMMARY\"")) {
            if (!classify.contains(required)) {
                missing.add(required);
            }
        }
        assertEquals(List.of(), missing,
                "the reusable workflow compiles the head, lists the"
                        + " change with git, classifies it with the event"
                        + " named, and hands the route out");
        assertFalse(classify.contains("pull_request:")
                || classify.contains("push:"),
                "it is called, never triggered on its own");
    }

    @Test
    void theEvidenceJobRunsOnlyOnTheWideRoute() throws IOException {
        String test = read("test.yml");
        assertTrue(job(test, "classify").contains(CALL),
                "test.yml has a classify job that calls the gate");
        String evidence = job(test, "evidence");
        assertTrue(evidence.contains("needs: classify"),
                "evidence needs the classification");
        assertTrue(evidence.contains(GATE),
                "and runs only when it is wide");
    }

    @Test
    void theRequiredSuitesNeverConsultTheRoute() throws IOException {
        String test = read("test.yml");
        for (String name : List.of("test", "display")) {
            String block = job(test, name);
            assertFalse(block.contains("needs:"),
                    name + " depends on nothing");
            assertFalse(block.contains("needs.classify"),
                    name + " never reads the route");
        }
    }

    @Test
    void theNativeImagesAreBuiltOnlyOnTheWideRoute() throws IOException {
        String images = read("app-image.yml");
        assertTrue(job(images, "classify").contains(CALL));
        String image = job(images, "image");
        assertTrue(image.contains("needs: [jar, classify]"),
                "the image matrix needs the JAR and the classification");
        assertTrue(image.contains(GATE), "and runs only when wide");
        assertFalse(job(images, "jar").contains("needs:"),
                "the JAR is cheap and always builds");
    }

    @Test
    void theArchiveIsBuiltAndVerifiedOnlyOnTheWideRoute()
            throws IOException {
        String dist = read("dist.yml");
        assertTrue(job(dist, "classify").contains(CALL));
        String build = job(dist, "build");
        assertTrue(build.contains("needs: classify"));
        assertTrue(build.contains(GATE));
        assertTrue(job(dist, "verify").contains("needs: build"),
                "verify follows build, so it follows the route");
    }

    @Test
    void theReleaseWorkflowDoesNotGateOnTheRoute() throws IOException {
        assertFalse(read("release.yml").contains("classify"),
                "a release is wide by being a tag; the packaging"
                        + " workflows it calls classify the event and"
                        + " answer wide themselves");
    }

    @Test
    void theClassifierIsWideOnEveryEventButAPullRequest()
            throws IOException {
        String main = Files.readString(
                Path.of("src/juranometria/tool/ChangeClassifierMain.java"),
                StandardCharsets.UTF_8);
        assertTrue(main.contains("if (!\"pull_request\".equals(event))")
                && main.contains("route = Route.WIDE;"),
                "only a pull request may be narrow; every other event"
                        + " is wide before any path is read");
    }

    private static String read(String name) throws IOException {
        return Files.readString(WORKFLOWS.resolve(name),
                StandardCharsets.UTF_8);
    }

    /** The block of one top-level job, by its two-space-indented key. */
    private static String job(String workflow, String name) {
        Matcher start = Pattern.compile("\n  " + name + ":\n")
                .matcher(workflow);
        assertTrue(start.find(), "job " + name + " exists");
        Matcher next = Pattern.compile("\n  [a-z][a-z-]*:\n")
                .matcher(workflow);
        int end = workflow.length();
        if (next.find(start.end())) {
            end = next.start();
        }
        return workflow.substring(start.start(), end);
    }
}
