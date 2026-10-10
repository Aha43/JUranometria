package juranometria.ui;

import java.util.ArrayList;
import java.util.List;

import juranometria.module.OverlayContribution;
import juranometria.module.OverlayContribution.Satellite;

/**
 * Which of a primary's satellites a page draws (issue #485, the owner's
 * ruling 4 on #482), each decided on its own - so one crowded pair never
 * suppresses the whole system:
 *
 * <ul>
 *   <li>a moon behind its primary is omitted;</li>
 *   <li>at the chart's normal minimum field (1° today, read from the
 *       chart and never assumed) every other moon is drawn at its exact
 *       place, its mark overlapping the primary's or another's where the
 *       real configuration is that close - a moon in front of the
 *       primary overlaps it on purpose;</li>
 *   <li>above that field a moon is drawn only where its own mark is
 *       distinguishable: in front of the primary only when the primary's
 *       drawn disc can hold a mark with room ({@value #IN_FRONT_ROOM_PX}
 *       px across); clear or shadowed only when its mark stands
 *       {@value #DISTINCT_PX} px clear of the primary's drawn edge and of
 *       every higher-precedence moon already drawn.</li>
 * </ul>
 *
 * <p>Precedence: in front, then clear, then shadowed; then the module's
 * own order (the larger moon first). A collision suppresses only the
 * lower-precedence mark. The decision is the study's, measured over 2026
 * in docs/studies/jovian-cartography (section B); labels are decided
 * separately, after the marks.
 */
final class SatelliteMarks {

    /** Two marks this far apart, centre to centre, read as two. */
    static final double DISTINCT_PX = 6.0;
    /** A primary's disc this wide can hold a moon's mark in front of it. */
    static final double IN_FRONT_ROOM_PX = 2.0 * DISTINCT_PX;

    /** Why a satellite is or is not drawn on a page. */
    enum Decision {
        DRAWN, BEHIND, AT_PRIMARY, COLLIDES, IN_FRONT_UNRESOLVED
    }

    private SatelliteMarks() {
    }

    /** The order collisions are settled in: in front, clear, shadowed; then the module's. */
    static int precedence(Satellite s) {
        int state = s.relation() == Satellite.Relation.IN_FRONT ? 0 : s.shadowed() ? 2 : 1;
        return state * 1000 + s.precedence();
    }

    /**
     * Each satellite's decision, in the order given.
     *
     * @param atFloor whether the page's field is the chart's normal minimum or finer
     * @param primaryDrawnPx the primary's drawn equatorial width in pixels
     * @param offsetsPx each satellite's page offset from the primary's centre
     */
    static List<Decision> decide(boolean atFloor, double primaryDrawnPx,
                                 List<Satellite> satellites, List<double[]> offsetsPx) {
        Decision[] out = new Decision[satellites.size()];
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < satellites.size(); i++) {
            order.add(i);
        }
        order.sort((x, y) -> Integer.compare(precedence(satellites.get(x)),
                precedence(satellites.get(y))));
        List<double[]> drawn = new ArrayList<>();
        for (int i : order) {
            Satellite s = satellites.get(i);
            double[] at = offsetsPx.get(i);
            double r = s.markPx() / 2.0;
            if (s.relation() == Satellite.Relation.BEHIND) {
                out[i] = Decision.BEHIND;
                continue;
            }
            if (atFloor) {
                out[i] = Decision.DRAWN;
            } else if (s.relation() == Satellite.Relation.IN_FRONT) {
                out[i] = primaryDrawnPx >= IN_FRONT_ROOM_PX ? Decision.DRAWN
                        : Decision.IN_FRONT_UNRESOLVED;
            } else if (Math.hypot(at[0], at[1]) - primaryDrawnPx / 2.0 < DISTINCT_PX / 2.0 + r) {
                out[i] = Decision.AT_PRIMARY;
            } else {
                out[i] = Decision.DRAWN;
                for (double[] other : drawn) {
                    if (Math.hypot(at[0] - other[0], at[1] - other[1]) < DISTINCT_PX) {
                        out[i] = Decision.COLLIDES;
                        break;
                    }
                }
            }
            if (out[i] == Decision.DRAWN) {
                drawn.add(at);
            }
        }
        return List.of(out);
    }

    /** The satellites of a primary among the page's contributions, in offered order. */
    static List<Satellite> of(String primary, List<OverlayContribution> contributions) {
        List<Satellite> out = new ArrayList<>();
        for (OverlayContribution c : contributions) {
            if (c instanceof Satellite s && s.primary().equals(primary)) {
                out.add(s);
            }
        }
        return out;
    }
}
