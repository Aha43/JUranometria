package juranometria.chart;

/**
 * Deep-sky object types the catalogue can carry, mapped from the OpenNGC
 * type tokens.
 *
 * <p>Fourteen of them draw one of the renderer's five symbols, in the
 * five reader-facing families of
 * docs/decisions/deep-sky-vocabulary.md; the other five - novae,
 * stellar and double-star entries, associations, and OpenNGC's own
 * unclassified rows - are deliberately drawn nothing, and still load,
 * search and recenter. {@code ChartRenderer.symbolForType} is the one
 * place that mapping lives.
 */
public enum DsoType {
    GALAXY("G"),
    GALAXY_PAIR("GPair"),
    GALAXY_TRIPLET("GTrpl"),
    GALAXY_GROUP("GGroup"),
    OPEN_CLUSTER("OCl"),
    GLOBULAR_CLUSTER("GCl"),
    CLUSTER_WITH_NEBULA("Cl+N"),
    PLANETARY_NEBULA("PN"),
    HII_REGION("HII"),
    NEBULA("Neb"),
    EMISSION_NEBULA("EmN"),
    REFLECTION_NEBULA("RfN"),
    DARK_NEBULA("DrkN"),
    SUPERNOVA_REMNANT("SNR"),
    NOVA("Nova"),
    STAR("*"),
    DOUBLE_STAR("**"),
    STELLAR_ASSOCIATION("*Ass"),
    OTHER("Other");

    private final String openNgcToken;

    DsoType(String openNgcToken) {
        this.openNgcToken = openNgcToken;
    }

    public String openNgcToken() {
        return openNgcToken;
    }

    /** Maps an OpenNGC type token; unknown tokens fail clearly. */
    public static DsoType fromOpenNgcToken(String token) {
        for (DsoType type : values()) {
            if (type.openNgcToken.equals(token)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown OpenNGC type token: " + token);
    }
}
