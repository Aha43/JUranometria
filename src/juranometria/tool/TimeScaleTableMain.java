package juranometria.tool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import juranometria.solar.SolarSystemPack;
import juranometria.solar.time.TimeScales;

/**
 * The atlas's own reading of civil instants, for study scripts that
 * compare against another implementation (issue #500): one ISO-8601
 * UTC instant per line in, the TT Julian date the atlas's
 * {@link TimeScales} gives it and the confidence out, so a comparison
 * measures the accepted time-scale model rather than the comparing
 * library's own. Study tooling only: nothing in the application reads
 * this.
 */
public final class TimeScaleTableMain {

    private TimeScaleTableMain() {
    }

    public static void main(String[] args) throws Exception {
        TimeScales scales = new TimeScales(SolarSystemPack.load().leapSeconds());
        PrintStream out = new PrintStream(System.out, false, StandardCharsets.UTF_8);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Instant civil = Instant.parse(line.strip());
                TimeScales.Epoch epoch = scales.tt(civil);
                out.println(String.format(Locale.ROOT, "%s %.12f %s", line.strip(), epoch.jdTt(),
                        epoch.confidence()));
            }
        }
        out.flush();
    }
}
