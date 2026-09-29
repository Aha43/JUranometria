package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The published instants of the seasonal events, read from the one
 * cited fixture (issue #399, owner checkpoint).
 *
 * <p>The first Sun table named two events at instants that were
 * supplied from memory and were hours wrong. The repair is that no
 * seasonal instant is ever typed again: the fixture
 * {@code docs/studies/solar-system/seasons-2026.txt} quotes the
 * IMCCE's published tables, with their URLs and digests, and
 * everything that names an equinox or a solstice - the study, the
 * Horizons request script, the contract test - reads it from here.
 */
public final class SeasonalEventsFixture {

    public static final Path FIXTURE =
            Path.of("docs/studies/solar-system/seasons-2026.txt");

    /** The four events, in the year's order. */
    public static final List<String> EVENTS = List.of("march-equinox",
            "june-solstice", "september-equinox", "december-solstice");

    /** One published event. */
    public record Event(String name, Instant instant, String asPublished) {
    }

    private SeasonalEventsFixture() {
    }

    /** The events by name, in the fixture's order. */
    public static Map<String, Event> read() throws IOException {
        return read(FIXTURE);
    }

    static Map<String, Event> read(Path fixture) throws IOException {
        Map<String, Event> events = new LinkedHashMap<>();
        for (String line : Files.readAllLines(fixture, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            if (f.length != 3) {
                throw new IOException("a fixture row is event, instant and"
                        + " the published text, tab-separated: " + line);
            }
            events.put(f[0].strip(), new Event(f[0].strip(),
                    Instant.parse(f[1].strip()), f[2].strip()));
        }
        if (!events.keySet().equals(new java.util.LinkedHashSet<>(EVENTS))) {
            throw new IOException("the fixture names exactly " + EVENTS
                    + " in order; it names " + events.keySet());
        }
        return Collections.unmodifiableMap(events);
    }
}
