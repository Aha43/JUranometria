package juranometria.catalog;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackManifestTest {

    static final String VALID = """
            format.version=1
            pack.name=bright-sky
            coverage.type=all-sky
            stars.limit.vmag=8.0
            tiling.scheme=radec-grid-30
            sources.tycho2.catalogue=I/259
            sources.openngc.release=v20260501
            license.stars=CC BY-NC 3.0 IGO
            license.dsos=CC-BY-SA-4.0
            checksum.tiles/r00-d4/stars.csv=abc123
            """;

    private static PackManifest parse(String text) {
        return PackManifest.parse(new StringReader(text), "test-manifest");
    }

    @Test
    void aValidManifestParsesCompletely() {
        PackManifest manifest = parse(VALID);
        assertEquals(1, manifest.formatVersion());
        assertEquals("bright-sky", manifest.packName());
        assertEquals("all-sky", manifest.coverage());
        assertEquals(8.0, manifest.starLimitVmag());
        assertEquals("radec-grid-30", manifest.tilingScheme());
        assertEquals("abc123", manifest.entries().get("checksum.tiles/r00-d4/stars.csv"),
                "checksum entries survive as raw entries");
    }

    @Test
    void missingRequiredKeysFailClearly() {
        String withoutLicense = VALID.replace("license.stars=CC BY-NC 3.0 IGO\n", "");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> parse(withoutLicense));
        assertTrue(failure.getMessage().contains("license.stars"));
    }

    @Test
    void unsupportedVersionsAndSchemesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> parse(VALID.replace("format.version=1", "format.version=2")));
        assertThrows(IllegalArgumentException.class,
                () -> parse(VALID.replace("format.version=1", "format.version=one")));
        assertThrows(IllegalArgumentException.class,
                () -> parse(VALID.replace("tiling.scheme=radec-grid-30",
                        "tiling.scheme=healpix-64")));
        assertThrows(IllegalArgumentException.class,
                () -> parse(VALID.replace("stars.limit.vmag=8.0", "stars.limit.vmag=bright")));
    }
}
