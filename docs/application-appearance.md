# Application appearance

## Baseline libraries

JUranometria will reuse the appearance stack proven in NamDesktop:

- **FlatLaf** for the Swing look and feel, including coherent light and dark
  application themes;
- **Tabler Icons** for a restrained, consistent icon language;
- **JSVG / FlatLaf Extras** for rendering the SVG icons cleanly at different
  display scales.

Pin exact versions in the dependency download script. Store the selected SVG
files as application resources so normal use never depends on a network
connection. Preserve the source project, license, selected icon names, and
download version beside the resources.

## Separation between application and chart

The application frame and the atlas page serve different visual roles.

FlatLaf controls menus, search, buttons, dialogs, layer controls, and other
application chrome. The chart renderer owns its own explicit palette,
typography, line weights, and symbols. A dark application theme must not
silently turn the atlas into a dark planetarium display.

The default chart remains white paper with black and grey ink in both
application themes. A purpose-built red-light or inverted chart mode may be
considered later, but it must be a deliberate chart style with printable
behaviour, not an accidental consequence of Swing theme colors.

## Visual character

The interface should feel like a precise desktop instrument:

- compact but not crowded;
- quiet neutral surfaces around the page;
- controls revealed where they are useful rather than permanent toolbars full
  of astronomy functions;
- text labels alongside unfamiliar actions instead of unexplained icons;
- standard platform behaviour for menus, keyboard focus, and dialogs;
- no gradients, glowing celestial effects, decorative animation, or glass-like
  panels.

The chart receives most of the window. Controls should frame it rather than
compete with it.

## Icon conventions

- Use Tabler's outline icons at a consistent optical size and stroke weight.
- Load icons from classpath resources rather than filesystem paths.
- Let the application theme recolor monochrome icon strokes for contrast.
- Do not recolor symbols inside the celestial chart through the UI theme.
- Add an icon only when it improves recognition or scan speed; icons are not
  decoration.
- Pair an icon with text in menus and for actions that are not immediately
  obvious. Icon-only controls require an accessible name and tooltip.

Likely early icons include search, zoom in, zoom out, reset view, layers,
print, and export. Final choices should be made when those actions are added,
not downloaded as a speculative icon set.

## Accessibility and platform fit

- Preserve keyboard navigation and visible focus indication supplied by Swing
  and FlatLaf.
- Keep control contrast valid in both application themes.
- Provide accessible names for custom components and icon-only buttons.
- Respect display scaling; SVG icons and Java2D drawing must remain crisp on
  HiDPI screens.
- Use the macOS screen menu bar when running on macOS, following NamDesktop's
  established application setup.

## Menus, About, and the appearance setting

The menu bar is deliberately restrained: a File menu carrying
Settings, a View menu carrying Chart Options, and a Help menu
carrying About - no placeholder items. (File
rather than an app-named menu: the macOS screen menu bar already
provides the application menu, and a duplicate name reads wrongly.) The
About dialog identifies the application from the packaged version,
describes it in the product language, and presents licensing from the
packaged summary and notice resources (never duplicated legal prose);
the full notices are readable offline. The appearance setting is
Light or Dark, persisted through a tiny JDK-preferences boundary,
applied live on OK and only on OK - Cancel and Escape change nothing.
`--dark` remains a session-only override that never rewrites the
saved choice. Appearance is application state: the chart's own paper
and ink never follow the theme.

