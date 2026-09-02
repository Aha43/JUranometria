package juranometria.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * What each module is offering the chart to ink (Sprint 24, issue
 * #215).
 *
 * <p>The first version of the seam took a bare list of geometry, and
 * that was wrong in two ways at once (review). A module could
 * replace what another module was showing, because nothing recorded
 * whose geometry was whose; and a module could not withdraw its own
 * on detach, because there was nothing to withdraw <em>from</em> - it
 * could only offer another list and hope.
 *
 * <p>So a contribution is <strong>owned</strong>. A module registers
 * once, under its own name, and is handed the one thing that
 * withdraws it. Removing a module removes its ink and nothing else,
 * which is what "removable" has to mean if the word is to survive
 * the second module.
 *
 * <p>Geometry is pulled rather than pushed: the chart asks when it
 * paints, so a module never has to guess when the page will next be
 * drawn, and a module that has nothing to show contributes nothing
 * by returning nothing.
 */
public final class OverlayRegistry {

    /** One piece of contributed geometry, and whose it is. */
    public record Owned(String moduleId, OverlayContribution geometry) {

        public Owned {
            if (moduleId == null || moduleId.isBlank()) {
                throw new IllegalArgumentException(
                        "contributed geometry has an owner");
            }
            if (geometry == null) {
                throw new IllegalArgumentException(
                        "an owner contributes geometry: " + moduleId);
            }
        }

        /**
         * The key the chart hit-tests by.
         *
         * <p>Two modules may both call something "m31" without
         * either having to know the other exists, so an identity is
         * only unique <em>within</em> its module.
         */
        public String key() {
            return moduleId + "/" + geometry.identity();
        }
    }

    private final Map<String, Supplier<List<OverlayContribution>>> sources =
            new LinkedHashMap<>();

    /**
     * Registers a module's geometry and returns the handle that
     * withdraws it.
     *
     * @throws IllegalStateException if this module is already
     *     contributing - two registrations under one name is a
     *     module that has lost track of its own lifecycle, and
     *     silently replacing the first is how the earlier seam let
     *     modules overwrite each other
     */
    public Runnable offer(String moduleId,
                          Supplier<List<OverlayContribution>> geometry) {
        if (moduleId == null || moduleId.isBlank()) {
            throw new IllegalArgumentException(
                    "a contributing module has a name");
        }
        if (geometry == null) {
            throw new IllegalArgumentException(
                    "a module contributes geometry: " + moduleId);
        }
        if (sources.containsKey(moduleId)) {
            throw new IllegalStateException(
                    "already contributing: " + moduleId);
        }
        sources.put(moduleId, geometry);
        return () -> sources.remove(moduleId, geometry);
    }

    /** Whether this module is currently contributing. */
    public boolean holds(String moduleId) {
        return sources.containsKey(moduleId);
    }

    /**
     * Everything on offer, in the order modules registered - a
     * defined order, so a page does not change because two modules
     * were attached in a different sequence.
     */
    public List<Owned> collect() {
        List<Owned> all = new ArrayList<>();
        for (Map.Entry<String, Supplier<List<OverlayContribution>>> entry
                : sources.entrySet()) {
            List<OverlayContribution> offered = entry.getValue().get();
            if (offered == null) {
                continue;
            }
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (OverlayContribution geometry : offered) {
                if (!seen.add(geometry.identity())) {
                    // A stable key that is not unique is not a key
                    // (review). Two pieces of ink a reader cannot
                    // tell apart, and a hit test that cannot say
                    // which was pointed at, is a defect in the
                    // module - reported as one rather than drawn.
                    throw new IllegalStateException(String.format(
                            "%s contributes the identity \"%s\" more"
                                    + " than once; contributed"
                                    + " identities are unique within"
                                    + " a module, because the chart"
                                    + " hit-tests by them",
                            entry.getKey(), geometry.identity()));
                }
                all.add(new Owned(entry.getKey(), geometry));
            }
        }
        return List.copyOf(all);
    }
}
