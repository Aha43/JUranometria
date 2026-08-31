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
 * A guard on the 1.0 contract's loudest promise (issue #145): "the
 * application makes no network requests at any time - no telemetry,
 * no update check, no remote lookup".
 *
 * <p><strong>What this proves, exactly.</strong> Every class the
 * application ships is scanned for the constant-pool references that
 * the listed connection mechanisms require: sockets and channels,
 * URL connections, the HTTP client, name resolution, SSL factories,
 * RMI, JNDI, and starting a subprocess (which could reach the
 * network on the application's behalf). If any appears, this fails
 * with the class named.
 *
 * <p><strong>What it does not prove</strong> (audit review, P3): it
 * is a guard against reintroduction, not a proof of impossibility.
 * A connection opened through reflection, a method handle, native
 * code, or a mechanism absent from the list below would not be
 * caught here. The promise rests on the contract, the code review of
 * every change, and the fact that nothing in the application has any
 * reason to connect; this test's job is to make a reintroduction
 * loud rather than to make one inconceivable.
 *
 * <p>Resource URLs are deliberately not on the list -
 * {@code Class.getResource} reads the application's own bundled data
 * and connects to nothing; what would betray a connection is opening
 * one.
 */
class OfflinePromiseTest {

    /** Mechanisms a shipped class would need to reach the network. */
    private static final String[] CONNECTING = {
            "java/net/Socket", "java/net/ServerSocket",
            "java/net/DatagramSocket", "java/net/InetAddress",
            "java/net/URLConnection", "java/net/HttpURLConnection",
            "java/net/ProxySelector", "java/net/http",
            "java/nio/channels/SocketChannel",
            "java/nio/channels/DatagramChannel",
            "java/nio/channels/AsynchronousSocketChannel",
            "javax/net/", "java/rmi/", "javax/naming/",
            "jdk/internal/net",
            // A subprocess is the practical way around all of the
            // above: curl, powershell, open-a-URL. Both the builder
            // and the Process type it returns are listed, so
            // Runtime.exec is covered with it.
            "java/lang/ProcessBuilder", "java/lang/Process",
            "openConnection", "openStream",
    };

    /**
     * Present in the compiled application, and so proof that the
     * scan can see a reference at all - without it, a scan that
     * silently matched nothing would pass looking identical to a
     * clean one.
     */
    private static final String POSITIVE_CONTROL = "java/lang/Runtime";

    @Test
    void noShippedClassReferencesAnyKnownWayToOpenAConnection()
            throws IOException {
        Path classes = Path.of("build/classes");
        assertTrue(Files.isDirectory(classes),
                "the compiled application must be present to scan");

        List<String> reaching = new ArrayList<>();
        List<Path> scanned;
        boolean controlSeen = false;
        try (Stream<Path> tree = Files.walk(classes)) {
            scanned = tree.filter(p -> p.toString().endsWith(".class"))
                    .toList();
        }
        for (Path file : scanned) {
            String bytes = new String(Files.readAllBytes(file),
                    StandardCharsets.ISO_8859_1);
            controlSeen |= bytes.contains(POSITIVE_CONTROL);
            for (String descriptor : CONNECTING) {
                if (bytes.contains(descriptor)) {
                    reaching.add(classes.relativize(file) + " -> "
                            + descriptor);
                }
            }
        }

        assertTrue(scanned.size() > 50,
                "the scan must cover the application: " + scanned.size()
                        + " classes");
        assertTrue(controlSeen,
                "the scan must be able to find a reference that is"
                        + " really there (" + POSITIVE_CONTROL + "),"
                        + " or its silence means nothing");
        assertEquals(List.of(), reaching,
                "no shipped class may reference a way to open a"
                        + " connection; the atlas is offline by"
                        + " construction, and this guard keeps a"
                        + " reintroduction loud");
    }
}
