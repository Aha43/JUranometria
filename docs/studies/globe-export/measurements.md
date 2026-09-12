# What a globe looks like after it is written to a file

Each sheet written to disk and read back. The page is measured in memory and the
file on disk, so that a wrong page can be told from a writer that lost a right one.

## Where the ink outside the limb comes from

These are marginal costs and do not sum: ink overlaps, so removing two layers
recovers less than the two rows together.

Each of these layers was taken away in turn and the ink beyond the limb
recounted, so what a removal recovers is what that layer put there:
  without star names
  without the grid
  without figures
  without star marks
  without deep-sky symbols
  without constellation names

## The writer, measured rather than assumed

Layer by layer, because asking whether a differing pixel has any ink near it in the
other rendering credits whatever happens to be nearby: outside a globe's limb the
figures, names and grid lie across one another, so a displaced shape could be
excused by an unrelated glyph. One layer at a time, the ink in the comparison
belongs to the shape being compared.

  constellation figures  every difference is an edge within the bound
  the grid               every difference is an edge within the bound
  deep-sky symbols       every difference is an edge within the bound
  star marks             every difference is an edge within the bound

The two text layers are not in that list, and the reason is a measurement:
the CI runner draws a glyph stem four pixels left of where this machine draws
it, and two pixels wider, in the written file against the page - the same
stroke, hinted differently under the two paths' transforms. That is the
desktop's answer, not the atlas's, so the text layers are compared in the
platform record beside this and held to reproducing within one environment.

  with one outside-limb patch struck from the page, the comparison fails, as it must
  every vector layer holds and the oracle can fail: the writer keeps the page's geometry.

## The core globe, no modules

  SVG  identity: orthographic  ** text anchored outside the limb **
  PDF  identity: orthographic  geometry not read back from this format
  PNG  identity: orthographic  ** ink beyond the limb **

## The same globe with modules (carries #331's known overrun)

  SVG  identity: orthographic  ** text anchored outside the limb **
  PDF  identity: orthographic  geometry not read back from this format
  PNG  identity: orthographic  ** ink beyond the limb **

The counts behind all of this - ink beyond the limb by layer, the two
rasterisations compared, file sizes and text-element counts - are this
machine's and are in docs/studies/globe-export/platform.md.
