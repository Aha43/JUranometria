package juranometria.tool;

import java.nio.charset.StandardCharsets;

/** Exposes the tool's hash helper to tests in other packages. */
public final class TestHashes {

    private TestHashes() {
    }

    public static String sha256(String content) {
        return PinnedInputs.sha256Hex(content.getBytes(StandardCharsets.UTF_8));
    }
}
