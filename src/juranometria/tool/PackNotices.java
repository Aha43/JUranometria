package juranometria.tool;

import juranometria.tool.AllSkyPackMain.PackCounts;

/**
 * The generated provenance and notice texts shipped inside the bright-sky
 * pack. Content is deterministic: it depends only on the pinned inputs
 * and the import counts, never on the clock or machine.
 */
final class PackNotices {

    /** The audit date of the pinned inputs, not a run timestamp. */
    private static final String AUDIT_DATE = "2026-08-29";

    private PackNotices() {
    }

    static String provenance(PackCounts counts, int tileCount) {
        StringBuilder types = new StringBuilder();
        counts.dsoTypes.forEach((type, count) ->
                types.append("| - type ").append(type).append(" | ").append(count).append(" |\n"));
        return """
                # Bright-sky pack provenance

                Generated resources - do not edit by hand. Regenerate with:

                ```sh
                scripts/download-catalogue-sources.sh
                make import-allsky
                ```

                The pack implements docs/decisions/all-sky-tiling.md over the
                sources decided in docs/decisions/catalogue-sources.md. Raw
                inputs were audited %s; the import tool verifies their SHA-256
                checksums (pinned in `PinnedInputs`) before transforming
                anything, and `manifest.properties` carries the SHA-256 of
                every generated tile file.

                ## Sources

                - Stars: The Tycho-2 Catalogue (Hog E. et al., 2000, A&A 355,
                  L27), CDS I/259, main catalogue and supplement-1. License
                  CC BY-NC 3.0 IGO - see `NOTICE-tycho2.md`.
                - Deep-sky objects: OpenNGC release v20260501 (Mattia Verga).
                  License CC-BY-SA-4.0 - see `NOTICE-openngc.md` and
                  `LICENSE-CC-BY-SA-4.0.txt`.

                ## Coverage and rules

                - Coverage: the complete sky, partitioned into %d populated
                  radec-grid-30 tiles; every object lives in exactly one home
                  tile chosen by its centre position. The manifest declares
                  the largest object semi-extent (the Large Magellanic Cloud)
                  so queries can never omit an object whose symbol reaches
                  into a chart.
                - Stars: Johnson V <= %.1f, derived per the I/259 ReadMe as
                  V = VT - 0.090 * (BT - VT); VT (or Hp in supplement-1) used
                  unchanged when BT is absent. Supplement-1 positions are at
                  epoch J1991.25 without proper-motion propagation. Identifier
                  collisions follow the main-catalogue-wins component policy.
                - Deep-sky objects: every OpenNGC object with a position,
                  including those without photometry or dimensions; the type
                  column carries the OpenNGC token. Unknown values stay
                  explicitly empty - the pack preserves facts and never
                  invents dimensions, angles, or magnitudes. V and B
                  magnitudes are stored in their own columns.

                ## Row counts and normalizations

                | Fact | Count |
                |---|---|
                | Stars written | %d |
                | - from the main catalogue | %d |
                | - from supplement-1 | %d |
                | - using the observed (fallback) position | %d |
                | - V taken from VT alone (no BT) | %d |
                | - V taken from an Hp magnitude | %d |
                | - supplement components skipped for an existing TYC id | %d |
                | Records dropped for missing VT (whole sky) | %d |
                | Deep-sky objects written | %d |
                %s| Dup/NonEx entries skipped | %d |
                | - dropped for missing position | %d |
                | - without any magnitude (kept, fields empty) | %d |
                | - without V but with B (kept, vmag empty) | %d |
                | - without dimensions (kept, fields empty) | %d |
                | - without a position angle (kept, field empty) | %d |

                Stars are ordered by (vmag, id) within each tile; deep-sky
                objects by id. Identical pinned inputs reproduce every file
                byte-identically.

                ## Relation to the M31 regional resource

                The Sprint 3 regional resource retired when the application
                switched to this pack (Sprint 5, issue #43); this pack is
                the single source of bundled catalogue data, and the M31
                reference chart reproduces from it byte-identically.
                """.formatted(AUDIT_DATE, tileCount, AllSkyPackMain.STAR_LIMIT_V,
                counts.starsWritten, counts.mainStars, counts.supplementStars,
                counts.fallbackPositions, counts.vtWithoutBt, counts.hpMagnitudes,
                counts.supplementComponentsSkipped, counts.droppedNoVt,
                counts.dsosWritten, types.toString(), counts.skippedDupNonEx,
                counts.droppedNoPosition, counts.dsosWithoutAnyMagnitude,
                counts.dsosWithoutVMagnitude, counts.dsosWithoutDimensions,
                counts.dsosWithoutPositionAngle);
    }

    static String tycho2() {
        return """
                # Notice: Tycho-2 derived data

                The star tiles in this pack are derived from the Tycho-2
                Catalogue:

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

                The deep-sky tiles in this pack are derived from OpenNGC,
                release v20260501:

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
