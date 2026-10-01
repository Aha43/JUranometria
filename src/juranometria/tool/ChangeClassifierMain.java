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
 *       [--base DIR] [--event pull_request]
 *       [--github-output FILE] [--summary FILE]
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
 * <p>{@code --base} names a directory holding the merge base's copy of
 * each changed file that existed there, at its own relative path -
 * again git's job - so a language file can be judged key by key and
 * the provenance record row by row (#428). Without it every key and
 * row reads as new, which can only make a change wider.
 *
 * <p>Every path gets a finding; the route of the whole change is the
 * widest any path takes - narrow, interaction or wide (issue #398,
 * extended by #427/#428). Only a pull request can be less than wide. A
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
        Path baseDir = null;
        String event = "pull_request";
        Path githubOutput = null;
        Path summary = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--changed" -> changedList = Path.of(args[++i]);
                case "--base" -> baseDir = Path.of(args[++i]);
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
                    .append("`, and only a pull request may take a cheaper"
                            + " route\n");
        } else {
            if (changedList == null || !Files.isRegularFile(changedList)) {
                System.err.println("--changed FILE is required for a pull"
                        + " request: the list of changed paths, one per"
                        + " line, as git reports them");
                System.exit(2);
            }
            List<String> changed = read(changedList);
            RenderingClosure closure = RenderingClosure.of(CLASSES);
            ChangeBoundary boundary = ChangeBoundary.of(closure, Path.of("."),
                    baseDir);
            findings = ChangeRoute.classify(changed, boundary,
                    ChangeRoute::existsInTree);
            route = ChangeRoute.routeOf(findings);
            report.append("route: ").append(route.name().toLowerCase())
                    .append(" - ").append(changed.size())
                    .append(" changed path(s)\n");
            report.append("closures: ").append(boundary.sizes().get("chart"))
                    .append(" classes the chart producers reach, ")
                    .append(boundary.sizes().get("interaction"))
                    .append(" the interface reaches\n");
            for (Finding finding : findings) {
                report.append(switch (finding.route()) {
                    case WIDE -> "  WIDE        ";
                    case INTERACTION -> "  interaction ";
                    case NARROW -> "  narrow      ";
                }).append(finding.path())
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
        md.append("## CI route: ").append(route.name().toLowerCase())
                .append("\n\n");
        md.append(switch (route) {
            case WIDE -> "The whole regime applies: every chart generator"
                    + " reproduced, the native images and the portable"
                    + " archive.\n\n";
            case INTERACTION -> "Interface work that cannot reach chart ink:"
                    + " the interface and report evidence is reproduced,"
                    + " the native images with packaged acceptance and the"
                    + " portable archive run, and no chart producer runs -"
                    + " the run proves no chart picture or provenance row"
                    + " moved.\n\n";
            case NARROW -> "Nothing in this change reaches the application or"
                    + " its evidence: the unit and display suites run, and"
                    + " the rest is skipped by this verdict.\n\n";
        });
        if (!findings.isEmpty()) {
            for (Route each : Route.values()) {
                long count = findings.stream()
                        .filter(f -> f.route() == each).count();
                md.append(count).append(' ')
                        .append(each.name().toLowerCase()).append(each
                                == Route.WIDE ? ".\n\n" : ", ");
            }
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
