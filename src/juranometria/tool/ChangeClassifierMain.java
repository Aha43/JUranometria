package juranometria.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import juranometria.tool.ChangeRoute.Finding;
import juranometria.tool.ChangeRoute.Route;

/**
 * Says which route a change takes through CI, and why, path by path.
 *
 * <pre>
 *   java juranometria.tool.ChangeClassifierMain --changed FILE
 *       [--event pull_request] [--github-output FILE] [--summary FILE]
 * </pre>
 *
 * <p>{@code --changed} names a file with one changed path per line,
 * relative to the repository root. Producing it is git's job, not
 * this program's: the Makefile and the workflow list everything that
 * differs between the merge base and the working tree, untracked
 * files included, and hand the list over. So the same command
 * answers on a clean CI checkout and on a developer's machine before
 * a push - and this class starts no process, which is what
 * {@code OfflinePromiseTest} requires of every class that ships.
 *
 * <p>Every path gets a finding; the route of the whole change is wide
 * if any path is (issue #398). Only a pull request can be narrow. A
 * push to {@code main}, a tag and a manual dispatch are wide by
 * event: what lands on main, what is released and what somebody asked
 * for by hand all get the whole regime, whatever they changed.
 *
 * <p>The exit status is 0 for either route. This program classifies;
 * it does not judge. A missing build or a missing list is status 2,
 * because a boundary that cannot be computed proves nothing and the
 * workflow must not read silence as narrow.
 */
public final class ChangeClassifierMain {

    private static final Path CLASSES = Path.of("build/classes");

    private ChangeClassifierMain() {
    }

    public static void main(String[] args) throws Exception {
        Path changedList = null;
        String event = "pull_request";
        Path githubOutput = null;
        Path summary = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--changed" -> changedList = Path.of(args[++i]);
                case "--event" -> event = args[++i];
                case "--github-output" -> githubOutput = Path.of(args[++i]);
                case "--summary" -> summary = Path.of(args[++i]);
                default -> {
                    System.err.println("unknown argument " + args[i]);
                    System.exit(2);
                }
            }
        }
        if (!Files.isDirectory(CLASSES)) {
            System.err.println("no compiled classes under " + CLASSES
                    + "; run make classes first");
            System.exit(2);
        }

        StringBuilder report = new StringBuilder();
        Route route;
        List<Finding> findings = new ArrayList<>();
        if (!"pull_request".equals(event)) {
            route = Route.WIDE;
            report.append("route: wide - the event is `").append(event)
                    .append("`, and only a pull request may be narrow\n");
        } else {
            if (changedList == null || !Files.isRegularFile(changedList)) {
                System.err.println("--changed FILE is required for a pull"
                        + " request: the list of changed paths, one per"
                        + " line, as git reports them");
                System.exit(2);
            }
            List<String> changed = read(changedList);
            RenderingClosure closure = RenderingClosure.of(CLASSES);
            findings = ChangeRoute.classify(changed, closure.sources(),
                    closure::chainTo, ChangeRoute::existsInTree);
            route = ChangeRoute.routeOf(findings);
            report.append("route: ").append(route.name().toLowerCase())
                    .append(" - ").append(changed.size())
                    .append(" changed path(s)\n");
            report.append("rendering closure: ")
                    .append(closure.reached().size())
                    .append(" classes in ").append(closure.sources().size())
                    .append(" source files, from ")
                    .append(closure.roots().size()).append(" roots\n");
            for (Finding finding : findings) {
                report.append(finding.route() == Route.WIDE ? "  WIDE   "
                        : "  narrow ").append(finding.path())
                        .append("  (").append(finding.reason()).append(")\n");
            }
            if (changed.isEmpty()) {
                report.append("  (nothing changed)\n");
            }
        }
        System.out.print(report);

        if (githubOutput != null) {
            Files.writeString(githubOutput,
                    "route=" + route.name().toLowerCase() + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }
        if (summary != null) {
            Files.writeString(summary, markdown(route, report, findings),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }
    }

    private static String markdown(Route route, CharSequence report,
                                   List<Finding> findings) {
        StringBuilder md = new StringBuilder();
        md.append("## Rendering-neutral gate: ").append(
                route == Route.WIDE ? "wide" : "narrow").append("\n\n");
        md.append(route == Route.WIDE
                ? "The whole rendering regime applies: evidence, native"
                        + " images and the portable archive run.\n\n"
                : "No path in this change can reach a renderer, a chart"
                        + " contribution, an evidence generator, a committed"
                        + " image or a provenance row, so the evidence,"
                        + " native-image and portable-archive jobs are"
                        + " skipped by this verdict. The unit and display"
                        + " suites run regardless.\n\n");
        long wide = findings.stream()
                .filter(f -> f.route() == Route.WIDE).count();
        if (!findings.isEmpty()) {
            md.append(wide).append(" wide, ").append(findings.size() - wide)
                    .append(" narrow.\n\n");
        }
        md.append("```\n").append(report).append("```\n");
        return md.toString();
    }

    /** One path per line, blank lines ignored, sorted and deduplicated. */
    private static List<String> read(Path list) throws java.io.IOException {
        TreeSet<String> paths = new TreeSet<>();
        for (String line : Files.readAllLines(list, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                paths.add(line.strip());
            }
        }
        return new ArrayList<>(paths);
    }
}
