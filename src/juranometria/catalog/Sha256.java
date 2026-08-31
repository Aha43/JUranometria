package juranometria.catalog;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * The one place the atlas hashes bytes, so that every pack verifies
 * itself the same way (audit review, P2).
 *
 * <p>Three packs each carried their own copy of this, and the copies
 * diverged: two treated an absent SHA-256 as what it is - a broken
 * Java runtime - while the third reported it as a pack integrity
 * failure, which would have told a reader with a perfectly good
 * download to fetch it again. Duplication is what made the
 * divergence possible, so the duplication is gone rather than
 * merely policed.
 *
 * <p>A missing SHA-256 is therefore, in one place, an ordinary
 * unrecognised failure: {@link MessageDigest} guarantees the
 * algorithm on every conforming Java platform, so its absence says
 * something is wrong with the runtime, never with the data.
 */
public final class Sha256 {

    private Sha256() {
    }

    /** The lower-case hexadecimal SHA-256 digest of these bytes. */
    public static String hex(byte[] bytes) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // Deliberately NOT a PackIntegrityException: the data is
            // not in question, this Java runtime is.
            throw new IllegalStateException(
                    "SHA-256 is unavailable in this Java runtime", e);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest(bytes)) {
            hex.append(String.format(Locale.ROOT, "%02x", b));
        }
        return hex.toString();
    }
}
