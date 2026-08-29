package juranometria.chart;

/**
 * Deep-sky object types the catalogue can carry, mapped from the OpenNGC
 * type tokens. The renderer currently draws a symbol only for
 * {@link #GALAXY}; other types load, search, and recenter, and receive
 * their chart symbols from docs/chart-conventions.md in a later issue.
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
