package juranometria.geo;

/**
 * One of the 88 IAU constellations: stable identity (the IAU
 * abbreviation), canonical Latin name, genitive form, and the
 * prominence rank (1 = most prominent) carried from the bundled
 * geography source. Names are Latin by the atlas convention - the
 * model is deliberately not coupled to any UI language.
 */
public record Constellation(String id, String latinName, String genitive,
                            int rank) {

    public Constellation {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("constellation id must not be blank");
        }
        if (latinName == null || latinName.isBlank()) {
            throw new IllegalArgumentException(
                    "constellation Latin name must not be blank");
        }
        if (genitive == null || genitive.isBlank()) {
            throw new IllegalArgumentException(
                    "constellation genitive must not be blank");
        }
        if (rank < 1 || rank > 3) {
            throw new IllegalArgumentException(
                    "constellation rank must be 1..3: " + rank);
        }
    }
}
