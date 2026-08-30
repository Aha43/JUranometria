package juranometria.chart;

/**
 * A star's immutable structured identity from the bundled identity
 * pack (docs/decisions/star-identity.md): the traditional proper
 * name, Bayer designation (Greek letter or post-omega Latin letter,
 * component digits verbatim), Flamsteed number, and IAU constellation
 * abbreviation - designations as separate facts, never a
 * renderer-ready label string. A null field is honestly unknown; a
 * designation never floats without its constellation.
 */
public record StarIdentity(String name, String bayer, String flamsteed,
                           String constellation) {

    public StarIdentity {
        for (String field : new String[] {name, bayer, flamsteed,
                constellation}) {
            if (field != null && field.isBlank()) {
                throw new IllegalArgumentException(
                        "identity fields are null when unknown, never blank");
            }
        }
        if (name == null && bayer == null && flamsteed == null
                && constellation == null) {
            throw new IllegalArgumentException(
                    "an identity with no fields is no identity");
        }
        if ((bayer != null || flamsteed != null) && constellation == null) {
            throw new IllegalArgumentException("a Bayer or Flamsteed"
                    + " designation is meaningless without its constellation");
        }
    }
}
