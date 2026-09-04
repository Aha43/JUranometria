# The Andromeda mark as the Mac application identity

Issue #245. A reader reported JUranometria appearing on macOS with
generic Java branding — the Java figure instead of the application's
own mark — in places where an application's identity is expected:
Finder, the Dock, the application switcher. The build looked
correctly wired on paper, so the issue's first demand was to
establish the failing launch route from the public downloads before
changing anything.

## What was reproduced, and from what

Everything below was gathered from the **published 1.6.0 release
artifacts**, not a working-tree build, on macOS (Darwin 25.5.0,
Apple silicon/arm64, 3024x1964 retina display):

- `JUranometria-1.6.0-macos-arm64.zip`, fetched with
  `gh release download v1.6.0` and verified against the published
  `SHA256SUMS.txt` before opening. Because `gh` downloads carry no
  quarantine attribute, Gatekeeper's first-launch prompt did not
  arise; that affects launch friction, not icon identity.
- `JUranometria-1.6.0-portable.zip`, fetched and verified the same
  way.

The evidence is the `screenshot-*` captures beside this document's
study directory, `docs/studies/mac-identity/` — the
**captured-evidence** class of `docs/decisions/test-evidence.md`:
operating-system screenshots no command of ours can regenerate,
each digest-pinned in the evidence verifier so a substitution or a
silent swap is a named breach. Captured 2026-09-04 with macOS's own
`screencapture`; the Dock tile was located through the Dock's
accessibility position rather than aimed by eye.

## The route matrix

| route | surface | what showed | capture |
|---|---|---|---|
| `JUranometria.app`, unpacked | Finder, before any launch | the Andromeda mark | `screenshot-finder-prelaunch.png` |
| `JUranometria.app`, running | Dock tile | the Andromeda mark | `screenshot-dock-app.png` |
| `JUranometria.app`, running | application switcher (Cmd-Tab) | the Andromeda mark | `screenshot-switcher.png` |
| copy in `/Applications` | Finder's Applications view | the Andromeda mark | `screenshot-applications-folder.png` |
| alias on the Desktop | Desktop | the Andromeda mark, alias badge and all | `screenshot-desktop-alias.png` |
| portable, `java -jar JUranometria.jar` | Dock tile | **the generic Java figure** | `screenshot-dock-jar.png` |
| candidate `.app` built from this branch | Dock tile | the Andromeda mark | `screenshot-dock-candidate.png` |

The portable flavour's `juranometria` launcher script `exec`s
`java -jar` after its version check, so a shell launch of the
portable download is mechanically the same route as the portable
`java -jar` row above - named by route, not by number, so an
inserted row cannot make this reference stale.
Launching the `.app`'s embedded binary from a shell stays inside
the bundle and keeps its identity.

The installed bundle itself was inspected, not inferred:
`Info.plist` carries `CFBundleIconFile = JUranometria.icns` and
`CFBundleIdentifier = juranometria.app`, both bundle version fields
read 1.6.0, and the installed `JUranometria.icns` (76,828 bytes)
decoded with `iconutil -c iconset` back to the reviewed mark at
16@2x, 32@2x, 128, 256, 512 and 512@2x.

No fresh-filename, fresh-account or Launch Services experiment was
run, because there was no `.app` failure to chase: the mark
appeared on every `.app` surface at first arrival, with no cache
touched. The Java figure on the JAR route is not a cache artifact
either — it is Java's default Dock icon, shown because the process
sets no Dock image of its own through the runtime's Taskbar API.

## The decision

This is the issue's second branch: **only the portable JAR route
shows Java branding, and no packaging change reaches it.** The
measured claim, stated no wider than the measurement: a JAR is not
a bundle, so Finder identity is out of reach for it entirely; and
its running Dock tile shows Java's generic icon because the
current launcher executes a plain `java -jar` with no runtime
Taskbar-icon integration. A runtime API could brand that one tile
— the follow-up below — so the Dock half of this is a property of
today's launcher, not a law of the operating system. So:

- the native `JUranometria.app` is the unmistakable reader route,
  and the README now says so plainly where the downloads are named,
  including what the portable flavour will look like in the Dock —
  we do not claim a raw JAR is a branded `.app`;
- the packaging check closed its one genuine gap. The build already
  refused a missing icon, regenerated the container from the
  reviewed `ApplicationMark` geometry (`verify-icons.sh`), and
  compared the installed copy byte-for-byte — so missing,
  substituted and malformed were caught. **Unreferenced** was not:
  a bundle can carry a perfect icns that `Info.plist` never names,
  and every check passed while macOS showed the default. The macOS
  cell of `build-app-image.sh` now reads `CFBundleIconFile` back
  from the built image and fails unless it names the installed
  container. Rehearsed by mutation on a copy of the built image:
  the key deleted and the key pointed at an absent container each
  fail; the real image passes.

## Out of scope, recorded rather than done

- The icns carries no plain 16 or 32 point variant (and no 128@2x
  or 256@2x): every surface inspected rendered correctly from what
  is there, so this is an observation about `ApplicationIconMain`'s
  container, not a defect — one geometry, more sizes, if a small
  rendition ever looks soft.
- `java.awt.Taskbar.setIconImage` could brand the JAR route's Dock
  tile at runtime from inside the process. That would soften the
  limitation recorded above, not remove it (Finder would still show
  a plain JAR), and it is runtime behaviour rather than packaging —
  left as a possible follow-up issue, deliberately not folded into
  this fix.
- Signing and notarisation, and anything that would promise macOS
  decorates a portable JAR as a native application.

Windows and Linux icon packaging are untouched: the evidence found
no corresponding defect, and the issue directs leaving them alone
unless it had.
