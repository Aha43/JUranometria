# Meteor showers: sources, terms and how the study's inputs were made (#715)

Sprint 60, issue #715. The study reads four raw sources, each fetched once on
2026-11-11 after the owner approved it by name (D1-D4), and three
transcriptions made from two of them. All live under
`imports/raw/meteor-showers/` in the main checkout, which git ignores; the
study's tools hold each to the SHA-256 printed in `measurements.md`,
section 1, and stop when one is absent or changed. Nothing here is shipped.

## What was fetched

| # | file | from | what it is | terms as read on 2026-11-11 |
|---|---|---|---|---|
| D1 | `imo-cal2027.pdf` | `https://www.imo.net/files/data/calendar/cal2027.pdf` | IMO 2027 Meteor Shower Calendar, ed. J. Rendtel, IMO INFO(3-26), 28 pages, DOI 10.13140/RG.2.2.16856.46082 | © International Meteor Organization, 2026. The IMO's data policy (below). |
| D2 | `imo-cal2026.pdf` | `https://www.imo.net/files/meteor-shower/cal2026.pdf` | IMO 2026 Meteor Shower Calendar, ed. J. Rendtel, 28 pages | as D1 |
| D3 | `iaumdc-streamestablisheddata.txt` | `https://ceresiaumdc.ta3.sk/downloads/lists_shw_data/streamestablisheddata.txt` | IAU Meteor Data Center, established meteor showers, V.2; "Last update: Nov 10, 20:00:00 UTC, 2026" | No licence or terms of use found on the MDC's pages. The file and the site ask for citation (below). |
| D4 | `gmn_shower_table_20230518.txt` | `https://raw.githubusercontent.com/wmpg/WesternMeteorPyLib/master/wmpl/share/gmn_shower_table_20230518.txt` | "Shower lookup table measured using GMN data up to May 18, 2023" | Global Meteor Network data: CC BY 4.0 (`globalmeteornetwork.org/data/`). The repository that carries the table is MIT (© 2019 Denis Vida); the table states no licence of its own. |

### The IMO's data policy

`https://www.imo.net/about/using-imo-data/`, read 2026-11-11. In the IMO's
words: data published by the IMO, "such as the Meteor Shower Calendar, the
Working List of Visual Meteor Showers and observation results", may be used,
reproduced and shared "for any purpose, without asking us first", including
"in commercial apps and services", on two conditions:

- credit the IMO, with the line **Source: International Meteor Organization
  (IMO) – www.imo.net**;
- keep the data honest: values a user computes are marked as the user's own.

The IMO asks to be contacted first before "reproducing the Meteor Shower
Calendar or other IMO publications in full or in substantial part" and before
"republishing an IMO dataset in full as a standalone file, database or API".

The site's general legal notice (`https://www.imo.net/legal-notice/`) reserves
the website's own structure, design and text.

### The IAU Meteor Data Center's request

The file's own header: the database "is maintained on an unpaid voluntary
basis", and "the correct references are" Jenniskens et al. 2020, *Planetary
and Space Science* 182, 104821; Jopek & Kaňuchová 2017, *Planetary and Space
Science* 143, 3-6; Jopek & Kaňuchová 2014 (Meteoroids 2013, 353-364); Jopek &
Jenniskens 2011 (NASA/CP-2011-216469, 7-13). The MDC's references page
(`https://ceresiaumdc.ta3.sk/references`) lists eleven papers, the first
being Jopek et al. 2024, *A&A* 682, A159.

### The Global Meteor Network's request

`https://globalmeteornetwork.org/data/`: "The data are released under the CC
BY 4.0 license"; reference the website and Vida et al. 2020, *MNRAS* 491,
2688-2705, and Vida et al. 2021, *MNRAS* 506, 5046-5074.

**In the repository (#716, owner's ruling of 2026-11-14):**
`test/resources/meteor-showers/gmn-radiant-samples.tsv`, 44 lines of the
table for the nine showers, verbatim, with the source's address, its
SHA-256, the licence and this attribution in its heading. It is written by
`make import-meteor-showers`, read by `MeteorShowerAccuracyTest`, and not
shipped in any archive or image.

### Not used

The American Meteor Society's calendar: its terms grant no reuse, it names no
source, and its text repeats the IMO's in places. Wikipedia: a starting point
for reading only.

## What was transcribed, and how far to trust it

The two calendars are PDFs. Their text was extracted with macOS PDFKit and
kept beside the PDFs (`transcribed/imo-cal2026.pdfkit.txt`,
`transcribed/imo-cal2027.pdfkit.txt`).

- **`transcribed/imo-2026-table5.tsv`, `imo-2027-table5.tsv`** - Table 5,
  the Working List of Visual Meteor Showers (p. 25), parsed from the
  extracted text. The 2027 table was also keyed by hand from the same page;
  the two agree in all 38 rows and 10 fields.
- **`transcribed/imo-2027-table6.tsv`** - Table 6, the radiant positions
  (p. 26). The extraction loses the page's columns, so each entry was
  assigned to its shower by continuity with the shower's listed maximum, and
  three assignments were corrected by reading the page (the Southern and
  Northern Taurids' rows that fell under the Antihelion Source, and one
  κ-Cygnid row that fell under the July γ-Draconids). All 72 date rows and
  all 234 entries are assigned; none is left over. The 2026 edition's table
  is identical except one entry (the Draconids on 10 October).

This is a study transcription. It has one independent check - Table 6 read
at each maximum against Table 5 (`measurements.md`, section 6) - and no
second keying. A shipped pack would need a reviewed transcription with its
own check (decision record, R-3).

### The second keying (#716, 2026-11-12)

For the nine showers of the first pack, Table 5 and Table 6 were keyed a
second time from pages 25 and 26 rendered as images (PDFKit, three times
enlarged), independently of the extracted text:
`transcribed/imo-2027-second-keying-table5.tsv` and
`imo-2027-second-keying-table6.tsv`, in the same gitignored folder. The
pack's importer compares the two keyings field by field and writes nothing
unless they agree; they agree in all 307 fields. The second keying was made
by the coding agent reading the page image, not by a second person
(decision record, section 8).

## What the repository carries of the IMO's data

By the owner's ruling (decision record, C-1) the study does not publish most
of the working list. `measurements.md` prints IMO values, with the credit
line, for #564's nine candidate showers and for the two others listed at a
ZHR of 10 or more; the rest of the working list appears as counts, as
differences from other sources, and as the handful of fields that changed
between the two editions. The full transcriptions stay in the gitignored raw
folder. The owner contacts the IMO before #716 ships any derived offline
pack.

**2026-11-14.** The owner wrote to `info@imo.net`; no reply had come by
2026-11-14, and the owner ruled that the wait is over (decision record,
section 9): the IMO's data policy permits use, reproduction and sharing for
any purpose without prior permission, and the pack is a derived nine-shower
subset, neither the Calendar in substantial part nor the IMO's dataset as a
standalone file. The pack `src/resources/meteor-showers/` is in the
repository from #716 on, with the credit line, the edition's name and its
notice. The full transcriptions, both keyings and the calendar PDFs stay in
the gitignored raw folder. A newer edition of the calendar is a new import:
fetch the PDF into `imports/raw/meteor-showers/`, key the nine showers'
rows of Tables 5 and 6 twice, pin the checksums and the edition in
`MeteorShowerPackMain`, and run `make import-meteor-showers`, which refuses
unless the keyings agree. A later reply from the IMO is recorded here and
in the decision record.

## A process deviation

Preparing the study worktree, `scripts/download-libs.sh` fetched
`junit-platform-console-standalone-6.1.3.jar` from the network without the
owner's approval (the test library had not been copied from the main
checkout with the runtime jars). The script verified its SHA-256. Recorded
in the decision record, section 6.
