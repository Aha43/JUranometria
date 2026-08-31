package juranometria.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 1.0 contract's loudest promise, held to mechanically (issue
 * #145): "the application makes no network requests at any time - no
 * telemetry, no update check, no remote lookup".
 *
 * A promise a reader cannot verify is worth little, and a reviewer
 * reading for absence is the weakest kind of evidence, so the check
 * reads the compiled classes: every class the application ships is
 * scanned for the constant-pool references that using a network
 * would require. Resource URLs are deliberately not on the list -
 * {@code Class.getResource} returns a {@code java.net.URL} and the
 * application uses it constantly to read its own bundled data; what
 * would betray a connection is opening one.
 */
class OfflinePromiseTest {

    /** Descriptors no offline application can do without reaching out. */
    private static final String[] CONNECTING = {
            "java/net/Socket", "java/net/ServerSocket",
            "java/net/DatagramSocket", "java/net/InetAddress",
            "java/net/URLConnection", "java/net/HttpURLConnection",
            "java/net/http", "java/nio/channels/SocketChannel",
            "openConnection", "openStream",
    };

    @Test
    void nothingTheApplicationShipsCanOpenAConnection() throws IOException {
        Path classes = Path.of("build/classes");
        assertTrue(Files.isDirectory(classes),
                "the compiled application must be present to scan");

        List<String> reaching = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> tree = Files.walk(classes)) {
            for (Path file : tree.filter(p -> p.toString().endsWith(".class"))
                    .toList()) {
                scanned++;
                String bytes = new String(Files.readAllBytes(file),
                        StandardCharsets.ISO_8859_1);
                for (String descriptor : CONNECTING) {
                    if (bytes.contains(descriptor)) {
                        reaching.add(classes.relativize(file) + " -> "
                                + descriptor);
                    }
                }
            }
        }

        assertTrue(scanned > 100,
                "the scan must actually cover the application: "
                        + scanned + " classes");
        assertEquals(List.of(), reaching,
                "no shipped class may reference a network connection;"
                        + " the atlas is offline permanently, by"
                        + " construction rather than by policy");
    }
}
