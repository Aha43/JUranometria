package juranometria.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * What furniture each module is asking the chart to carry (Sprint 51,
 * issue #259).
 *
 * <p>Owned and pulled, as geometry is ({@link OverlayRegistry}): a
 * module registers once under its own name and is handed the one thing
 * that withdraws it; the chart asks what each module offers when it
 * paints, so a module whose text changes - another place, another
 * instant - needs only to ask for a redraw. A module offering nothing
 * returns nothing, and the page is the page without it.
 *
 * <p>Registration order is the order the chart allocates corners in, so
 * later modules join the chart's corner order without knowing one
 * another.
 */
public final class FurnitureRegistry {

    /** One box of furniture, whose it is, and who answers its action. */
    public record Owned(String moduleId, Furniture furniture,
                        Consumer<String> actions) {

        public Owned {
            if (moduleId == null || moduleId.isBlank() || furniture == null
                    || actions == null) {
                throw new IllegalArgumentException(
                        "furniture has an owner who answers its action");
            }
        }

        /** The key the chart names and hit-tests the box by. */
        public String key() {
            return moduleId + "/" + furniture.identity();
        }
    }

    private record Source(Supplier<List<Furniture>> furniture,
                          Consumer<String> actions) {
    }

    private final Map<String, Source> sources = new LinkedHashMap<>();

    /**
     * Registers a module's furniture and the handler its actions are
     * handed back to, and returns the handle that withdraws both.
     *
     * @throws IllegalStateException if this module is already offering
     *     furniture - a module that registers twice has lost track of
     *     its own lifecycle
     */
    public Runnable offer(String moduleId, Supplier<List<Furniture>> furniture,
                          Consumer<String> actions) {
        if (moduleId == null || moduleId.isBlank()) {
            throw new IllegalArgumentException(
                    "a module offering furniture has a name");
        }
        if (furniture == null || actions == null) {
            throw new IllegalArgumentException(
                    "a module offers furniture and answers its actions: "
                            + moduleId);
        }
        if (sources.containsKey(moduleId)) {
            throw new IllegalStateException(
                    "already offering furniture: " + moduleId);
        }
        Source source = new Source(furniture, actions);
        sources.put(moduleId, source);
        return () -> sources.remove(moduleId, source);
    }

    /** Whether this module is offering furniture right now. */
    public boolean holds(String moduleId) {
        return sources.containsKey(moduleId);
    }

    /** Everything on offer now, in the order modules registered. */
    public List<Owned> collect() {
        List<Owned> all = new ArrayList<>();
        for (Map.Entry<String, Source> entry : sources.entrySet()) {
            List<Furniture> offered = entry.getValue().furniture().get();
            if (offered == null) {
                continue;
            }
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Furniture box : offered) {
                if (!seen.add(box.identity())) {
                    throw new IllegalStateException(entry.getKey()
                            + " offers the furniture \"" + box.identity()
                            + "\" more than once; the chart names and"
                            + " hit-tests furniture by its identity");
                }
                all.add(new Owned(entry.getKey(), box,
                        entry.getValue().actions()));
            }
        }
        return List.copyOf(all);
    }
}
