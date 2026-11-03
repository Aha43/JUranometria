package juranometria.bookmark;

/** A bookmark's place on the Earth, as the chart context holds one. */
public record Place(double latitudeDegrees, double eastLongitudeDegrees) {

    public Place {
        if (!(latitudeDegrees >= -90 && latitudeDegrees <= 90)
                || !(eastLongitudeDegrees >= -180 && eastLongitudeDegrees <= 180)) {
            throw new IllegalArgumentException("a place on the Earth: "
                    + latitudeDegrees + ", " + eastLongitudeDegrees);
        }
    }
}
