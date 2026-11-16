package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No test class keeps a planetary service, pack or kernel in a static
 * field of its own (#708). The suite runs in one process, so a static
 * field lives until the last test has run; seventy-five classes each
 * keeping their own copy of a pack's kernel bytes once held about 3 GB
 * of a 4 GB heap. Computational tests take the suite's one instance
 * from {@code juranometria.solar.SharedServices}; this inventory of
 * the test sources refuses a new static holder anywhere else, apart
 * from the reviewed few below whose subject needs a reading of its
 * own. A holder there is named with its reason, and a name that no
 * longer matches a field is refused too, so the list cannot go stale.
 */
class StaticServiceHolderTest {

    private static final Path TESTS = Path.of("test");

    /** The types that hold kernel bytes, directly or through the packs they keep. */
    private static final String HELD = "SolarSystemService|JovianSystemService"
            + "|SaturnSystemService|MarsService|UranusService|NeptuneService"
            + "|VenusService|MercuryService|SolarSystemPack|PlanetsPack|JovianPack"
            + "|SaturnPack|MarsSystemPack|UranusSystemPack|NeptuneSystemPack"
            + "|SpkKernel|WholeSkyComposition\\.Services";

    /** A static field whose declared type names one of them, generics and arrays included. */
    private static final Pattern HOLDER = Pattern.compile(
            "^\\s*(?:(?:private|protected|public|final|volatile|transient)\\s+)*static\\s+"
            + "(?:(?:final|volatile|transient)\\s+)*"
            + "([^=;(]*(?<![\\w.])(?:juranometria\\.\\w+\\.)?(?:" + HELD + ")(?!\\w)[^=;(]*?)"
            + "\\s+(\\w+)\\s*(?:=.*|;.*)$");

    /** The fixture every computational test reads from. */
    private static final String FIXTURE = "juranometria/solar/SharedServices.java";

    /** Reviewed: the tests whose subject needs its own reading, and why. */
    private static final Map<String, String> REVIEWED = reviewed();

    private static Map<String, String> reviewed() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("juranometria/solar/spk/SpkKernelTest.java#kernel",
                "the kernel reader is the subject: the bundled kernel, read by the test itself");
        m.put("juranometria/solar/spk/SpkSegmentSelectionTest.java#fixture",
                "segment selection over a small committed multi-segment fixture, not a pack");
        m.put("juranometria/solar/spk/SpkExcerptTest.java#fixture",
                "the excerpt writer's source: the same small committed fixture");
        m.put("juranometria/ui/WholeSkyPageTest.java#SERVICES",
                "the whole-sky composition with the services it reads for itself;"
                + " released when the class is done");
        return m;
    }

    private record Holder(String file, String type, String field) {
        String key() {
            return file + "#" + field;
        }
    }

    private static List<Holder> holders() throws IOException {
        List<Holder> found = new ArrayList<>();
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(TESTS)) {
            sources = walk.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        for (Path source : sources) {
            String file = TESTS.relativize(source).toString().replace('\\', '/');
            boolean inBlockComment = false;
            for (String line : Files.readAllLines(source)) {
                String s = line.strip();
                if (inBlockComment) {
                    inBlockComment = !s.contains("*/");
                    continue;
                }
                if (s.startsWith("/*")) {
                    inBlockComment = !s.contains("*/");
                    continue;
                }
                if (s.startsWith("//") || s.startsWith("*")) {
                    continue;
                }
                Matcher m = HOLDER.matcher(line);
                if (m.matches()) {
                    found.add(new Holder(file, m.group(1).strip(), m.group(2)));
                }
            }
        }
        return found;
    }

    @Test
    void theInventoryReadsADeclarationAsAHolderAndAMethodAsNone() {
        assertTrue(HOLDER.matcher("    private static SolarSystemService service;").matches());
        assertTrue(HOLDER.matcher(
                "    private static final MarsService MARS = MarsService.load();").matches());
        assertTrue(HOLDER.matcher("    static SpkKernel fixture;").matches());
        assertTrue(HOLDER.matcher(
                "    static final java.util.List<JovianPack> PACKS = new ArrayList<>();").matches());
        assertTrue(HOLDER.matcher("    private static UranusService[] both;").matches());
        assertTrue(HOLDER.matcher(
                "    private static WholeSkyComposition.Services SERVICES;").matches());
        assertTrue(HOLDER.matcher(
                "    static juranometria.solar.NeptuneService neptune;").matches());
        assertTrue(!HOLDER.matcher("    static SolarSystemService service() {").matches());
        assertTrue(!HOLDER.matcher("    private SolarSystemService service;").matches());
        assertTrue(!HOLDER.matcher(
                "    private static final Observer OSLO = at(SolarSystemService.FIRST_DAY);")
                .matches());
        assertTrue(!HOLDER.matcher("    private static MeteorShowerPack PACK;").matches());
    }

    @Test
    void noTestClassKeepsAServiceAPackOrAKernelInAStaticFieldOfItsOwn() throws IOException {
        List<String> refused = new ArrayList<>();
        for (Holder h : holders()) {
            if (h.file().equals(FIXTURE) || REVIEWED.containsKey(h.key())) {
                continue;
            }
            refused.add("test/" + h.file() + ": static " + h.type() + " " + h.field());
        }
        assertEquals(List.of(), refused, "a static field keeps its kernel bytes for the rest of"
                + " the one-process suite (#708): take the service from"
                + " juranometria.solar.SharedServices, or, where the test's subject needs a"
                + " reading of its own, keep it in a local and say why on the reviewed list");
    }

    @Test
    void everyReviewedHolderStillExists() throws IOException {
        TreeSet<String> present = new TreeSet<>();
        for (Holder h : holders()) {
            present.add(h.key());
        }
        List<String> stale = new ArrayList<>();
        for (String key : REVIEWED.keySet()) {
            if (!present.contains(key)) {
                stale.add(key);
            }
        }
        assertEquals(List.of(), stale, "the reviewed list names holders that are gone");
    }

    @Test
    void theFixtureIsWhereTheSharedServicesAreKept() throws IOException {
        long kept = holders().stream().filter(h -> h.file().equals(FIXTURE)).count();
        assertEquals(10, kept, "two packs and eight services, one of each");
    }
}
