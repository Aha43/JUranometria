package juranometria.bookmark;

import java.util.Optional;

/**
 * A bookmark's subject: a kind and the atlas's own language-independent
 * identity for it - the catalogue's for a star ({@code TYC 1800-2202-1})
 * or a deep-sky object ({@code NGC 598}), the module's for a Solar System
 * body or moon ({@code moon}, {@code jovian.io}). Display text is never
 * stored; it is looked up in the interface's language at each reading.
 */
public record Subject(Kind kind, String id) {

    /** What a subject identity names. */
    public enum Kind {
        STAR("star"),
        DEEP_SKY("deep-sky"),
        BODY("body");

        private final String token;

        Kind(String token) {
            this.token = token;
        }

        /** The document's word for the kind. */
        public String token() {
            return token;
        }

        static Optional<Kind> of(String token) {
            for (Kind k : values()) {
                if (k.token.equals(token)) {
                    return Optional.of(k);
                }
            }
            return Optional.empty();
        }
    }

    public Subject {
        if (kind == null || id == null || id.isBlank()) {
            throw new IllegalArgumentException("a subject is a kind and an identity");
        }
    }
}
