package juranometria.tool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.ApplicationIcon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The committed application-icon containers (Sprint 23, issue #202).
 *
 * <p>That an icon file exists catches a deletion and nothing else.
 * It was measured: eight bytes of rubbish named
 * {@code JUranometria.icns} built a complete, passing application
 * image, because jpackage copies the container verbatim and every
 * check compared that copy against the same rubbish. So the question
 * asked here is the only one that catches it - is what we ship still
 * the mark that was reviewed?
 */
class ApplicationIconAssetsTest {

    private static final Path ICONS = Path.of("packaging/icon");

    @Test
    void everyCommittedContainerIsWhatTheGeometryDraws() throws Exception {
        for (int size : new int[] {16, 24, 32, 48, 64, 128, 256, 512, 1024}) {
            Path committed = ICONS.resolve("JUranometria-" + size + ".png");
            assertTrue(Files.exists(committed), committed + " is committed");
            assertArrayEquals(ApplicationIconMain.png(size),
                    Files.readAllBytes(committed),
                    committed + " is exactly what the chosen geometry"
                            + " draws at " + size + " px");
        }
        assertArrayEquals(ApplicationIconMain.ico(),
                Files.readAllBytes(ICONS.resolve("JUranometria.ico")),
                "the Windows container is the reviewed mark");
        assertArrayEquals(ApplicationIconMain.icns(),
                Files.readAllBytes(ICONS.resolve("JUranometria.icns")),
                "the macOS container is the reviewed mark");
    }

    @Test
    void theWindowsContainerCarriesTheSizesWindowsSelects()
            throws Exception {
        byte[] ico = ApplicationIconMain.ico();
        ByteBuffer buffer = ByteBuffer.wrap(ico).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, buffer.getShort(0), "reserved");
        assertEquals(1, buffer.getShort(2), "an icon, not a cursor");
        int count = buffer.getShort(4);
        assertEquals(7, count, "seven representations");

        List<Integer> declared = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int at = 6 + 16 * i;
            int width = ico[at] & 0xff;
            // The format says 256 with a zero, which is the entry an
            // incomplete icon is usually missing.
            declared.add(width == 0 ? 256 : width);
            int length = buffer.getInt(at + 8);
            int offset = buffer.getInt(at + 12);
            assertTrue(offset + length <= ico.length,
                    "entry " + i + " points inside the file");
            assertEquals((byte) 0x89, ico[offset],
                    "entry " + i + " is a PNG payload");
        }
        assertEquals(List.of(16, 24, 32, 48, 64, 128, 256), declared,
                "the sizes Windows actually selects between");
    }

    @Test
    void theMacContainerCarriesTheRepresentationsMacOsReads()
            throws Exception {
        byte[] icns = ApplicationIconMain.icns();
        assertEquals("icns", new String(icns, 0, 4, StandardCharsets.US_ASCII));
        ByteBuffer buffer = ByteBuffer.wrap(icns).order(ByteOrder.BIG_ENDIAN);
        assertEquals(icns.length, buffer.getInt(4),
                "the declared length is the real length - a truncated"
                        + " container is exactly what this catches");

        List<String> types = new ArrayList<>();
        int at = 8;
        while (at < icns.length) {
            types.add(new String(icns, at, 4, StandardCharsets.US_ASCII));
            int length = buffer.getInt(at + 4);
            assertTrue(length > 8 && at + length <= icns.length,
                    "entry " + types.get(types.size() - 1) + " fits");
            at += length;
        }
        assertEquals(List.of("ic11", "ic12", "ic07", "ic08", "ic09", "ic10"),
                types, "the representations macOS reads");
    }

    @Test
    void theWindowIconSetIsTheSameMarkAtEverySize() {
        List<java.awt.Image> icons = ApplicationIcon.windowIcons();
        assertEquals(ApplicationIcon.WINDOW_SIZES.length, icons.size(),
                "one image per size a window manager chooses between");
        for (int i = 0; i < icons.size(); i++) {
            int size = ApplicationIcon.WINDOW_SIZES[i];
            java.awt.image.BufferedImage drawn =
                    (java.awt.image.BufferedImage) icons.get(i);
            assertEquals(size, drawn.getWidth(), "size " + size);
            // The running application and the installed containers
            // are the same drawing, not two that happen to agree.
            java.awt.image.BufferedImage direct = ApplicationIcon.at(size);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    assertEquals(direct.getRGB(x, y), drawn.getRGB(x, y),
                            "the window icon at " + size + " is the mark");
                }
            }
        }
    }
}
