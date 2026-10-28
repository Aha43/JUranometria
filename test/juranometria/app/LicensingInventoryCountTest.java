package juranometria.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The packaged licensing inventory counts its own list (#634, owner's
 * ruling 6). {@code build-app-image.sh} printed "13 resources" from #602
 * to #634 while its list held 14; a typed count drifts, a computed one
 * cannot. This holds the script to that, and its list to About's.
 */
class LicensingInventoryCountTest {

    private static final Path SCRIPT = Path.of("scripts/build-app-image.sh");

    @Test
    void theCountPrintedIsTheListsOwnNeverTyped() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);
        assertTrue(script.contains("packaged licensing inventory: complete ($checked resources"),
                "the count is the loop's counter");
        assertFalse(Pattern.compile("inventory: complete \\(\\d+ resources").matcher(script).find(),
                "and never a typed number");
    }

    @Test
    void theListCarriesEveryNoticeAboutShowsFifteenInAll() throws Exception {
        List<String> listed = inventory();
        assertEquals(15, listed.size(), "the summary, About's notices and both BSD texts: " + listed);
        String about = Files.readString(Path.of("src/juranometria/app/AboutDialog.java"),
                StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("new Notice\\(\"\\w+\",\\s*\"/(resources/[^\"]+)\"").matcher(about);
        int notices = 0;
        while (m.find()) {
            notices++;
            assertTrue(listed.contains(m.group(1)), "the packaged inventory checks " + m.group(1));
        }
        assertEquals(13, notices, "About's notices, as AboutTextTest counts them");
        assertTrue(listed.contains("resources/about/licensing-summary.txt"));
    }

    private static List<String> inventory() throws Exception {
        List<String> out = new ArrayList<>();
        boolean in = false;
        for (String line : Files.readAllLines(SCRIPT, StandardCharsets.UTF_8)) {
            String t = line.strip();
            if (t.startsWith("for resource in")) {
                in = true;
                continue;
            }
            if (in) {
                String entry = t.replace("\\", "").replace("; do", "").strip();
                if (!entry.isEmpty()) {
                    out.add(entry);
                }
                if (t.endsWith("; do")) {
                    break;
                }
            }
        }
        return out;
    }
}
