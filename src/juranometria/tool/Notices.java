package juranometria.tool;

import java.util.Locale;

import juranometria.tool.CatalogueImportMain.Counts;

/**
 * The generated provenance and notice texts shipped beside the imported
 * catalogue resources. Content is deterministic: it depends only on the
 * pinned inputs and the import counts, never on the clock or machine.
 */
final class Notices {

    /** The audit date of the pinned inputs, not a run timestamp. */
    private static final String AUDIT_DATE = "2026-08-29";

    private Notices() {
    }

    static String provenance(Counts counts, int starCount, int dsoCount) {
        return """
                # M31-region catalogue provenance

                Generated resources - do not edit by hand. Regenerate with:

                ```sh
                scripts/download-catalogue-sources.sh
                make import-catalogue
                ```

                The sources, licensing, magnitude semantics, and import contract
                are decided in `docs/decisions/catalogue-sources.md`. Raw inputs
                were audited %s; the import tool verifies their SHA-256
                checksums (pinned in `CatalogueImportMain`) before transforming
                anything.

                ## Sources

                - Stars: The Tycho-2 Catalogue (Hog E. et al., 2000, A&A 355,
                  L27), CDS I/259, main catalogue and supplement-1, from
                  `https://cdsarc.cds.unistra.fr/ftp/I/259/`. License
                  CC BY-NC 3.0 IGO - see `NOTICE-tycho2.md`.
                - Deep-sky objects: OpenNGC release v20260501 (Mattia Verga),
                  `NGC.csv` and `addendum.csv`, from
                  `https://github.com/mattiaverga/OpenNGC`. License
                  CC-BY-SA-4.0 - see `NOTICE-openngc.md` and
                  `LICENSE-CC-BY-SA-4.0.txt`.

                ## Coverage and limits

                - Region: cone of radius %.1f degrees around the M31 centre
                  (%.6f, %+.6f ICRS).
                - Stars: Johnson V <= %.1f, derived per the I/259 ReadMe as
                  V = VT - 0.090 * (BT - VT); VT (or Hp in supplement-1) used
                  unchanged when BT is absent. Supplement-1 positions are at
                  their catalogue epoch J1991.25 without proper-motion
                  propagation (out of scope; sub-arcsecond at chart scales).
                - Deep-sky objects: every OpenNGC galaxy (type G) in the
                  region; other types wait for their chart symbols.

                ## Row counts and normalizations

                | Fact | Count |
                |---|---|
                | Stars written | %d |
                | - from the main catalogue | %d |
                | - from supplement-1 | %d |
                | - using the observed (fallback) position | %d |
                | - V taken from VT alone (no BT) | %d |
                | - V taken from an Hp magnitude | %d |
                | Records dropped for missing VT (whole sky) | %d |
                | Galaxies written | %d |
                | In-region objects of other types, skipped | %d |
                | Dup/NonEx entries skipped (whole sky) | %d |
                | - position angle absent, recorded as 0.0 | %d |
                | - minor axis absent, set to major | %d |
                | - V magnitude taken from B | %d |
                | - galaxies dropped for missing V and B | %d |

                Stars are ordered by (vmag, id); deep-sky objects by id.
                Identical pinned inputs reproduce these files byte-identically.
                """.formatted(AUDIT_DATE,
                CatalogueImportMain.REGION_RADIUS_DEGREES,
                CatalogueImportMain.M31_CENTRE.raDegrees(),
                CatalogueImportMain.M31_CENTRE.decDegrees(),
                CatalogueImportMain.STAR_LIMIT_V,
                starCount, counts.mainStars, counts.supplementStars,
                counts.fallbackPositions, counts.vtWithoutBt, counts.hpMagnitudes,
                counts.droppedNoVt,
                dsoCount, counts.skippedOtherTypes, counts.skippedDupNonEx,
                counts.missingPositionAngle, counts.missingMinorAxis,
                counts.vmagFromB, counts.droppedNoMagnitude);
    }

    static String tycho2() {
        return """
                # Notice: Tycho-2 derived data

                The file `stars.csv` in this directory is derived from the
                Tycho-2 Catalogue:

                > The Tycho-2 Catalogue of the 2.5 Million Brightest Stars,
                > Hog E., Fabricius C., Makarov V.V., Urban S., Corbin T.,
                > Wycoff G., Bastian U., Schwekendiek P., Wicenec A.,
                > Astron. Astrophys. 355, L27 (2000). CDS catalogue I/259.

                This work has made use of data from the Tycho-2 Catalogue.
                This research has made use of the VizieR catalogue access
                tool, CDS, Strasbourg, France (DOI: 10.26093/cds/vizier).

                ## License: CC BY-NC 3.0 IGO

                The Tycho-2 data are distributed under the Creative Commons
                Attribution-NonCommercial 3.0 IGO license:
                https://creativecommons.org/licenses/by-nc/3.0/igo/

                **This resource may not be used commercially.** Commercial
                redistribution or use of the packaged application therefore
                requires removing or replacing this Tycho-2-derived
                catalogue. It is restricted data inside an otherwise
                MIT-licensed project; see LICENSING.md at the repository
                root.
                """;
    }

    static String openNgc() {
        return """
                # Notice: OpenNGC derived data

                The file `dsos.csv` in this directory is derived from
                OpenNGC, release v20260501:

                > OpenNGC, Mattia Verga,
                > https://github.com/mattiaverga/OpenNGC
                > DOI: 10.21938/y.1ejWUD_MQ6b_eDFoVbbw

                ## License: CC-BY-SA-4.0

                OpenNGC is released under the Creative Commons
                Attribution-ShareAlike 4.0 license; the complete text ships
                beside this notice as `LICENSE-CC-BY-SA-4.0.txt`
                (https://creativecommons.org/licenses/by-sa/4.0/).

                OpenNGC is itself built from these services, whose
                acknowledgments it requests:

                - The NASA/IPAC Extragalactic Database (NED), operated by
                  the Jet Propulsion Laboratory, California Institute of
                  Technology, under contract with NASA.
                - The SIMBAD database, operated at CDS, Strasbourg, France.
                - The HyperLeda database (http://leda.univ-lyon1.fr).
                """;
    }
}
