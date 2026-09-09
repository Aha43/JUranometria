package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Where a promoted image came from (Sprint 31, issue #315).
 *
 * <p>A promoted study page is a picture of what the atlas drew when
 * somebody looked at it and agreed. Whether it has fallen behind the
 * atlas is a question about <em>cartography</em>, and the only way
 * the evidence gate could ask it was to draw the page again and
 * compare the pixels - which answers a different question on a
 * different machine, where text rasterises differently and nothing
 * has gone stale at all.
 *
 * <p>So the picture carries its own account of itself instead: when
 * it was recorded, on what machine, by which generator, and the hash
 * of the bytes that were agreed. Anywhere can check that the file in
 * the repository is the file that was recorded. Only the machine that
 * promotes reference images can check whether the atlas has moved
 * under it, and that check keeps its own command.
 *
 * <p>Run by {@code make evidence-provenance} on the machine that
 * promotes them, after a reviewed regeneration - never automatically,
 * because a record that rewrites itself whenever the pixels move
 * records nothing.
 */
public final class EvidenceProvenanceMain {

    private EvidenceProvenanceMain() {
    }

    /** Where the record lives. */
    public static final Path RECORD =
            Path.of("docs/studies/PROVENANCE.md");

    /** One promoted artifact and the account it carries. */
    public record Entry(String path, String sha256, String recorded,
                        String environment, String generator) {
    }

    public static void main(String[] args) throws Exception {
        List<Entry> entries = new ArrayList<>();
        String recorded = args.length > 0 ? args[0]
                : java.time.LocalDate.now().toString();
        for (Path file : rendered()) {
            entries.add(new Entry(file.toString().replace('\\', '/'),
                    sha256(Files.readAllBytes(file)), recorded,
                    PlatformEvidence.environment(),
                    generatorOf(file)));
        }
        Files.writeString(RECORD, document(entries),
                StandardCharsets.UTF_8);
        System.out.println("provenance recorded for " + entries.size()
                + " promoted artifacts in " + RECORD);
    }

    /** Every committed artifact whose bytes a renderer drew. */
    public static List<Path> rendered() throws IOException {
        List<Path> found = new ArrayList<>();
        try (var walk = Files.walk(Path.of("docs/studies"))) {
            walk.filter(Files::isRegularFile)
                    .filter(file -> "renderer-drawn".equals(
                            TestEvidenceScan.artifactClass(
                                    file.getFileName().toString())))
                    .sorted()
                    .forEach(found::add);
        }
        return found;
    }

    /** The reader of the record, for the contract to check against. */
    public static List<Entry> recorded() throws IOException {
        List<Entry> entries = new ArrayList<>();
        if (!Files.exists(RECORD)) {
            return entries;
        }
        for (String line : Files.readAllLines(RECORD,
                StandardCharsets.UTF_8)) {
            if (!line.startsWith("| `docs/studies/")) {
                continue;
            }
            String[] cells = line.split("\\|");
            if (cells.length < 6) {
                continue;
            }
            entries.add(new Entry(strip(cells[1]), strip(cells[2]),
                    strip(cells[3]), strip(cells[4]), strip(cells[5])));
        }
        return entries;
    }

    private static String strip(String cell) {
        return cell.trim().replace("`", "");
    }

    private static String document(List<Entry> entries) {
        StringBuilder out = new StringBuilder();
        out.append("# Where the promoted images came from\n\n");
        out.append("Sprint 31, issue #315. Written by"
                + " `make evidence-provenance` on the machine that\n"
                + "promotes reference images, after a reviewed"
                + " regeneration.\n\n");
        out.append("A promoted page is a picture of what the atlas"
                + " drew when somebody looked at\nit and agreed. This"
                + " says when, on what, and by which generator, and"
                + " the hash of\nthe bytes that were agreed - so any"
                + " machine can check that the file in the\n"
                + "repository is the file that was recorded.\n\n");
        out.append("**It cannot say whether the atlas has moved since,"
                + " and neither can a rerender\non another machine:"
                + " text rasterises differently there and nothing has"
                + " gone\nstale. Cartographic freshness is carried by"
                + " the ink and semantic evidence, and\nby reviewed"
                + " regeneration.**\n\n");
        out.append(String.format(Locale.ROOT,
                "**%d promoted artifacts.**%n%n", entries.size()));
        out.append("| artifact | sha256 | recorded | environment |"
                + " generator |\n|---|---|---|---|---|\n");
        for (Entry entry : entries) {
            out.append("| `").append(entry.path()).append("` | `")
                    .append(entry.sha256()).append("` | ")
                    .append(entry.recorded()).append(" | ")
                    .append(entry.environment()).append(" | ")
                    .append(entry.generator()).append(" |\n");
        }
        return out.toString();
    }

    /** Which generator writes this directory, as far as anything says. */
    private static String generatorOf(Path file) {
        String directory = file.getParent().toString()
                .replace('\\', '/') + "/";
        for (var writer : EvidenceContractMain.buildWriters().entrySet()) {
            if (EvidenceContractMain.promotedDirectories()
                    .getOrDefault(directory, "").equals(
                            writer.getValue())) {
                return simple(writer.getKey());
            }
        }
        for (String main : EvidenceContractMain.imageMains()) {
            return simple(main).equals("") ? "unknown" : "various";
        }
        return "unknown";
    }

    private static String simple(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    /** The hash a reader can check by hand. */
    public static String sha256(byte[] bytes) throws Exception {
        java.security.MessageDigest digest =
                java.security.MessageDigest.getInstance("SHA-256");
        StringBuilder hex = new StringBuilder();
        for (byte one : digest.digest(bytes)) {
            hex.append(String.format(Locale.ROOT, "%02x", one));
        }
        return hex.toString();
    }
}
