package juranometria.tool.allsky;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * The production interface text with the study's draft reader names for
 * the three candidate projections appended (#686, study only).
 *
 * <p>The page words refuse a projection that has no reader name, which
 * is right; the study supplies draft names through the existing
 * resource route rather than changing a language file, so no
 * production word moves. The words here are drafts for the owner's
 * ruling, not proposals already accepted.
 */
public final class AllSkyStudyWords {

    private AllSkyStudyWords() {
    }

    private static final String EN = String.join("\n",
            "",
            "page.projection.allsky-hammer = Hammer whole-sky",
            "page.projection.allsky-mollweide = Mollweide whole-sky",
            "page.projection.allsky-equirectangular = equirectangular whole-sky",
            "");

    private static final String NB = String.join("\n",
            "",
            "page.projection.allsky-hammer = Hammer helhimmel",
            "page.projection.allsky-mollweide = Mollweide helhimmel",
            "page.projection.allsky-equirectangular = ekvirektangulær helhimmel",
            "");

    public static PageText in(String languageTag) {
        return PageText.in(InterfaceText.forLanguage(languageTag, AllSkyStudyWords::open));
    }

    private static InputStream open(String path) {
        InputStream base = InterfaceText.class.getResourceAsStream(path);
        if (base == null) {
            return null;
        }
        String extra = path.endsWith("/en.properties") ? EN
                : path.endsWith("/nb-NO.properties") ? NB : "";
        try (base) {
            byte[] bytes = base.readAllBytes();
            byte[] add = extra.getBytes(StandardCharsets.UTF_8);
            byte[] all = new byte[bytes.length + add.length];
            System.arraycopy(bytes, 0, all, 0, bytes.length);
            System.arraycopy(add, 0, all, bytes.length, add.length);
            return new ByteArrayInputStream(all);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
