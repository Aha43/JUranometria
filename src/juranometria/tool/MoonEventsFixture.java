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
import java.util.OptionalDouble;

/**
 * The published instants of the named lunar events of 2026, read from
 * the one cited fixture (issue #406, ruling M8; issue #407).
 *
 * <p>As with the seasons, no lunar event is ever typed: the fixture
 * {@code docs/studies/solar-system/moon-events-2026.txt} quotes Fred
 * Espenak's published phase and perigee/apogee catalogues, with their
 * URLs, retrieval date and digests, and everything that names a new
 * Moon or a perigee - the study, the contract test - reads it from
 * here. The rows name instants at the catalogues' minute precision;
 * they are not the numerical authority, which stays DE440 and Horizons.
 */
public final class MoonEventsFixture {

    public static final Path FIXTURE =
            Path.of("docs/studies/solar-system/moon-events-2026.txt");

    /** The events, in the fixture's order. */
    public static final List<String> EVENTS = List.of("new-moon-june",
            "first-quarter-june", "full-moon-june", "last-quarter-july",
            "perigee-january", "apogee-february", "perigee-nearest",
            "apogee-farthest");

    /**
     * One published event.
     *
     * @param distanceKm centre to centre, as the perigee/apogee table
     *                   states it; empty for a phase
     */
    public record Event(String name, Instant instant, OptionalDouble distanceKm,
                        String asPublished) {
    }

    private MoonEventsFixture() {
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
            if (f.length != 4) {
                throw new IOException("a fixture row is event, instant, distance"
                        + " or - and the published text, tab-separated: " + line);
            }
            OptionalDouble distance = f[2].strip().equals("-")
                    ? OptionalDouble.empty()
                    : OptionalDouble.of(Double.parseDouble(f[2].strip()));
            events.put(f[0].strip(), new Event(f[0].strip(),
                    Instant.parse(f[1].strip()), distance, f[3].strip()));
        }
        if (!events.keySet().equals(new java.util.LinkedHashSet<>(EVENTS))) {
            throw new IOException("the fixture names exactly " + EVENTS
                    + " in order; it names " + events.keySet());
        }
        return Collections.unmodifiableMap(events);
    }
}
