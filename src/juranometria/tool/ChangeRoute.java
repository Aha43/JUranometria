package juranometria.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Which route a change takes through CI: narrow, when nothing in it
 * can alter rendering or the evidence of rendering, or wide, when
 * the whole rendering regime applies.
 *
 * <p>Issue #398's rule, as the owner ruled it: the narrow route is
 * allowed only when a guard proves that the change touches no
 * renderer, chart contribution, rendering or evidence generator,
 * committed generated image or provenance row. Anything else - or
 * any doubt - is wide. A path is wide for one of two reasons:
 *
 * <ul>
 *   <li>it compiles into a class the {@link RenderingClosure}
 *   reaches from a root, which is the mechanical half; or</li>
 *   <li>it is an input the rendering regime consumes without
 *   compiling it - committed evidence, bundled resources, packaging,
 *   the build, the workflows, the packaged application's own entry
 *   points, or this guard - which is the named half.</li>
 * </ul>
 *
 * <p>The named half is deliberately coarse. A resource change is wide
 * whether or not a renderer reads that resource, because which
 * resources a class reads is not something a class file states, and
 * a rule that guessed would be a rule that one day guessed wrong.
 * Wide costs an hour of CI; narrow-by-mistake costs a committed
 * picture nobody proved.
 */
public final class ChangeRoute {

    /** The two routes. */
    public enum Route {
        /** Unit and display suites only. */
        NARROW,
        /** Everything: evidence, native images, portable archive. */
        WIDE
    }

    /** One changed path and what was decided about it. */
    public record Finding(String path, Route route, String reason) {
    }

    /** A named rule: paths it matches are wide for this reason. */
    record NamedRule(String reason, Predicate<String> matches) {
    }

    /**
     * The inputs the rendering regime consumes without compiling
     * them. Each is a prefix or an exact path, relative to the
     * repository root.
     */
    static final List<NamedRule> ALWAYS_WIDE = List.of(
            new NamedRule("committed evidence: docs/studies is the"
                    + " tree the evidence contract regenerates and"
                    + " holds to its bytes, provenance rows included",
                    under("docs/studies/")),
            new NamedRule("committed reference image compared by the"
                    + " image workflow", under("docs/reference/")),
            new NamedRule("committed gallery pages, generated and"
                    + " published", under("docs/gallery/")),
            new NamedRule("bundled resource: read by renderers and"
                    + " generators, and shipped in every package",
                    under("src/resources/")),
            new NamedRule("packaging input", under("packaging/")),
            new NamedRule("build or packaging script", under("scripts/")),
            new NamedRule("workflow: the gates themselves",
                    under(".github/workflows/")),
            new NamedRule("build input", exactly("Makefile")),
            new NamedRule("VERSION feeds committed artifacts",
                    exactly("VERSION")),
            new NamedRule("shipped in every package by make dist",
                    exactly("LICENSING.md")),
            new NamedRule("the packaged application's entry point",
                    exactly("src/juranometria/app/JUranometriaMain.java")),
            new NamedRule("the packaged application's qualification",
                    exactly("src/juranometria/app/PackagedAcceptanceMain.java")),
            new NamedRule("the guard itself",
                    exactly("src/juranometria/tool/RenderingClosure.java")),
            new NamedRule("the guard itself",
                    exactly("src/juranometria/tool/ChangeRoute.java")),
            new NamedRule("the guard itself",
                    exactly("src/juranometria/tool/ChangeClassifierMain.java")));

    private ChangeRoute() {
    }

    /**
     * Judges every changed path.
     *
     * @param changed   paths relative to the repository root
     * @param closure   the source files the rendering closure reaches
     * @param chain     how a closure source is reached, for the reason
     * @param present   whether a path still exists in the tree; a
     *                  removed source cannot be traced to a class, so
     *                  it is wide by caution
     */
    public static List<Finding> classify(Collection<String> changed,
                                         Set<String> closure,
                                         Function<String, Optional<List<String>>> chain,
                                         Predicate<String> present) {
        List<Finding> findings = new ArrayList<>();
        for (String path : changed) {
            findings.add(judge(path, closure, chain, present));
        }
        return findings;
    }

    private static Finding judge(String path, Set<String> closure,
                                 Function<String, Optional<List<String>>> chain,
                                 Predicate<String> present) {
        for (NamedRule rule : ALWAYS_WIDE) {
            if (rule.matches().test(path)) {
                return new Finding(path, Route.WIDE, rule.reason());
            }
        }
        if (closure.contains(path)) {
            String via = chain.apply(path)
                    .map(steps -> String.join(" -> ", steps))
                    .orElse("(chain not recorded)");
            return new Finding(path, Route.WIDE,
                    "in the rendering closure: " + via);
        }
        if (path.startsWith("src/") && !present.test(path)) {
            return new Finding(path, Route.WIDE,
                    "removed source: what depended on it at the base"
                            + " cannot be read from the build at the head");
        }
        return new Finding(path, Route.NARROW,
                path.startsWith("src/")
                        ? "source outside the rendering closure"
                        : "not an input of the rendering regime");
    }

    /** The route of the whole change: wide if any path is. */
    public static Route routeOf(List<Finding> findings) {
        return findings.stream().anyMatch(f -> f.route() == Route.WIDE)
                ? Route.WIDE : Route.NARROW;
    }

    /** Whether a path exists as a regular file in the working tree. */
    public static boolean existsInTree(String path) {
        return Files.isRegularFile(Path.of(path));
    }

    private static Predicate<String> under(String prefix) {
        return path -> path.startsWith(prefix);
    }

    private static Predicate<String> exactly(String exact) {
        return exact::equals;
    }
}
