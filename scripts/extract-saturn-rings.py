#!/usr/bin/env python3
"""Extract the PDS Ring-Moon Systems Node "Rings of Saturn" table, whole,
from the retained page for the #574 Saturn system study. Approved by the
owner on 2026-10-19 for #574 measurement only.

The retained page's SHA-256 is verified first; the run stops on any
mismatch. Every row of the table is written as it stands - feature,
inner boundary, outer boundary, optical depth, type, associated moons,
footnotes and comments - with no value changed, merged or reduced; an
empty cell stays empty. A superscript is written as ^ followed by its
text (10<sup>-5</sup> becomes 10^-5). The table's numbered Sources list
is written beside it, so each footnote number resolves to its reference.

Nothing here is a build, test or runtime dependency; it is run by hand.
"""
import hashlib
import html.parser
import pathlib
import sys

RINGS = pathlib.Path(__file__).resolve().parent.parent / 'docs/studies/saturn-system/rings'
PAGE = RINGS / 'saturn_tables.html'
PAGE_SHA256 = '2dea8fcda149c94324353ccfb47e3df27f5dfef00ba8d62ab738a917378fc64c'


class RingsTable(html.parser.HTMLParser):
    """The first table after the 'rings-of-saturn' heading, and the first
    ordered list after it (the table's Sources)."""

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.state = 'before'
        self.header, self.rows, self.sources = [], [], []
        self.cell = None
        self.row = None

    def handle_starttag(self, tag, attrs):
        if self.state == 'before' and tag == 'h3' and ('id', 'rings-of-saturn') in attrs:
            self.state = 'heading'
        elif self.state == 'heading' and tag == 'table':
            self.state = 'table'
        elif self.state == 'table' and tag == 'tr':
            self.row = []
        elif self.state == 'table' and tag in ('td', 'th'):
            self.cell = ''
        elif self.state == 'sources-wait' and tag == 'ol':
            self.state = 'sources'
        elif self.state == 'sources' and tag == 'li':
            self.cell = ''
        elif self.cell is not None and tag == 'sup':
            self.cell += '^'

    def handle_endtag(self, tag):
        if self.state == 'table' and tag in ('td', 'th') and self.cell is not None:
            self.row.append(' '.join(self.cell.split()))
            self.cell = None
        elif self.state == 'table' and tag == 'tr':
            (self.rows if self.header else self.header).append(self.row) if self.header else self.header.extend(self.row)
            self.row = None
        elif self.state == 'table' and tag == 'table':
            self.state = 'sources-wait'
        elif self.state == 'sources' and tag == 'li':
            self.sources.append(' '.join(self.cell.split()))
            self.cell = None
        elif self.state == 'sources' and tag == 'ol':
            self.state = 'done'

    def handle_data(self, data):
        if self.cell is not None:
            self.cell += data


def main():
    body = PAGE.read_bytes()
    digest = hashlib.sha256(body).hexdigest()
    if digest != PAGE_SHA256:
        sys.exit(f'{PAGE.name}: SHA-256 {digest}, pinned {PAGE_SHA256} - stopping')
    parser = RingsTable()
    parser.feed(body.decode('utf-8'))
    if parser.state != 'done' or not parser.rows or not parser.sources:
        sys.exit(f'{PAGE.name}: the Rings of Saturn table or its Sources was not found whole - stopping')
    width = len(parser.header)
    for row in parser.rows:
        if len(row) != width:
            sys.exit(f'{PAGE.name}: row {row[0]!r} has {len(row)} cells, the header {width} - stopping')
    out = RINGS / 'pds-rings-table.tsv'
    with out.open('w', encoding='utf-8') as f:
        f.write(f'# Extracted by scripts/extract-saturn-rings.py from {PAGE.name} (SHA-256 {PAGE_SHA256}).\n')
        f.write('# Cells as they stand; an empty cell is empty; ^ marks a superscript.\n')
        f.write('\t'.join(parser.header) + '\n')
        for row in parser.rows:
            f.write('\t'.join(row) + '\n')
    src = RINGS / 'pds-rings-sources.tsv'
    with src.open('w', encoding='utf-8') as f:
        f.write(f'# The Rings of Saturn table\'s numbered Sources, from {PAGE.name} (SHA-256 {PAGE_SHA256}).\n')
        f.write('Footnote\tReference\n')
        for n, ref in enumerate(parser.sources, 1):
            f.write(f'{n}\t{ref}\n')
    print(f'{len(parser.rows)} rows, {len(parser.sources)} sources')


if __name__ == '__main__':
    main()
