package juranometria.ui.placeandtime;

import java.time.Instant;

import juranometria.meridian.MeridianModule;
import juranometria.ui.ChartModuleHost;

/**
 * How a session begins with the meridian module (Sprint 25, issue
 * #229).
 *
 * <p>One place, because the policy was living in three: the
 * application's main, the journey and the packaged acceptance each
 * hand-copied "load the remembered place, state the moment, switch
 * everything off" - and three copies of a policy is how one of them
 * quietly changes (sprint review). What a reader's next evening
 * looks like is production behaviour, so production owns it and the
 * evidence exercises this seam rather than its own transcription.
 *
 * <p>The policy itself, as the gate decided it:
 *
 * <ul>
 *   <li>the <strong>place</strong> is the remembered one - latitude
 *       and east-positive longitude from the reader's stored
 *       choices, the equator at Greenwich until a reader first
 *       says otherwise;</li>
 *   <li>the <strong>moment</strong> is stated by the caller, read
 *       from the clock once at startup and frozen - never stored,
 *       so no stale saved clock can masquerade as now;</li>
 *   <li>every <strong>switch is off</strong>: the switches are not
 *       persisted, and each session begins on the ordinary chart
 *       with the reference lines a choice a reader makes each
 *       time.</li>
 * </ul>
 */
public final class PlaceAndTimeSession {

    private PlaceAndTimeSession() {
    }

    /**
     * Attaches the meridian module the way a session starts it, and
     * returns it for the dialog to control.
     */
    public static MeridianModule begin(ChartModuleHost modules,
                                       PlaceStore store,
                                       Instant sessionMoment) {
        MeridianModule module = modules.attach(
                new MeridianModule(store.load(sessionMoment)));
        module.showing(false, false, false);
        return module;
    }
}
