package juranometria.catalog;

/**
 * Thrown when a bundled pack is not what was published: a manifest
 * or declared file missing, a checksum that does not match, a pack
 * name or format version the build does not expect, a malformed or
 * dishonest row.
 *
 * <p>This exists to be a <strong>closed signal</strong> (audit
 * review, P1). The launch surface tells a reader with a damaged
 * download to fetch it again and check its checksum, and that advice
 * is only honest when the failure really is damaged data. Deciding
 * that from the package a stack frame came from would sweep in every
 * programming defect in the same packages and give those readers the
 * same wrong instruction; deciding it from this type cannot, because
 * only the verification paths throw it.
 *
 * <p>It extends {@link IllegalStateException} so that existing
 * callers and tests that expect the loaders' established failure
 * type keep working - the type is narrowed here, never widened.
 */
public class PackIntegrityException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public PackIntegrityException(String message) {
        super(message);
    }

    public PackIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
