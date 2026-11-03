# Bundled icons

Source: [Tabler Icons](https://github.com/tabler/tabler-icons), outline
style, release **v3.46.0**, downloaded 2026-08-28 with
`scripts/download-icons.sh` (the pinned version lives in that script).

License: MIT (Tabler Icons, Paweł Kuna). The complete upstream
copyright and permission notice is bundled beside the icons in
[`LICENSE`](LICENSE) and ships as an application resource, as the MIT
license requires. The SVG files are bundled unmodified; FlatLaf
recolors the monochrome strokes at runtime for theme contrast, which
does not alter the files.

Icons bundled:

| File | Used for |
|---|---|
| `zoom-in.svg` | Toolbar: zoom in |
| `zoom-out.svg` | Toolbar: zoom out |
| `zoom-reset.svg` | Toolbar: reset view |
| `minus.svg` | Toolbar: fewer stars (brighter magnitude limit) |
| `plus.svg` | Toolbar: more stars (fainter magnitude limit) |
| `list-details.svg` | Toolbar: show or hide the Inspector |
| `x.svg` | Inspector: close the pane from its own heading |
| `door-exit.svg` | Toolbar: exit JUranometria |
| `clock-pin.svg` | Toolbar: Place and Time (#675) |
| `adjustments-horizontal.svg` | Toolbar: show or hide the Controller (#675) |
| `stack-2.svg` | Toolbar and Controller: Accumulate selection (#675) |
| `sparkles.svg` | Toolbar and Controller: Emphasis (#675) |
| `bookmark-plus.svg` | Toolbar: Add Bookmark… (#681; fetched 2026-11-02, 469 bytes, sha256 71fea9dc55178bd5d454288552fb3f31b87aa38c86d21b8094ee209c8d9dfa68) |

Add an icon only when a real action needs it: add the name to the
script's `ICONS` list, re-run the script, and record it here.
