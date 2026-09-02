package juranometria.page;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What is on one page, in the decided order.
 *
 * <p>A snapshot: it answers for the page it was built from and is
 * never mutated afterwards. When the page changes, a new one is
 * built - which is what makes "painting performs no catalogue query"
 * a property of the design rather than a promise.
 */
public record PageContents(List<PageEntry> entries) {

    public PageContents {
        entries = List.copyOf(entries);
    }

    /** Nothing catalogued is on this page. */
    public static final PageContents EMPTY = new PageContents(List.of());

    /** The deep-sky objects, in order. */
    public List<PageEntry.DeepSky> deepSky() {
        List<PageEntry.DeepSky> found = new ArrayList<>();
        for (PageEntry entry : entries) {
            if (entry instanceof PageEntry.DeepSky deepSky) {
                found.add(deepSky);
            }
        }
        return List.copyOf(found);
    }

    /** The stars the catalogue names, in order. */
    public List<PageEntry.StarEntry> namedStars() {
        List<PageEntry.StarEntry> found = new ArrayList<>();
        for (PageEntry entry : entries) {
            if (entry instanceof PageEntry.StarEntry star && star.named()) {
                found.add(star);
            }
        }
        return List.copyOf(found);
    }

    /**
     * How many stars are here that the catalogue does not name.
     *
     * <p>The number behind the counted line: a page with 600
     * anonymous stars says so once rather than six hundred times.
     */
    public int anonymousStarCount() {
        int count = 0;
        for (PageEntry entry : entries) {
            if (entry instanceof PageEntry.StarEntry star && !star.named()) {
                count++;
            }
        }
        return count;
    }

    /** The entry with this identity, if it is on the page. */
    public Optional<PageEntry> find(String identity) {
        for (PageEntry entry : entries) {
            if (entry.identity().equals(identity)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /** Whether this identity is on the page at all. */
    public boolean holds(String identity) {
        return find(identity).isPresent();
    }

    /**
     * How many entries carry each visibility - the shape of a page
     * at a glance, and what the gate measured pages by.
     */
    public Map<PageVisibility, Integer> tally() {
        Map<PageVisibility, Integer> counts = new LinkedHashMap<>();
        for (PageVisibility state : PageVisibility.values()) {
            counts.put(state, 0);
        }
        for (PageEntry entry : entries) {
            counts.merge(entry.visibility(), 1, Integer::sum);
        }
        return Map.copyOf(counts);
    }
}
