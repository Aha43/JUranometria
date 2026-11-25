# Hands-on test — #734 rendering and interface audit on Windows

**Status:** DRAFT  
**Tester:**  
**Date:**  
**Issue:** [#734 — Establish a clean Windows installation and rendering baseline](https://github.com/Aha43/JUranometria/issues/734)  
**Artifact:** Published `JUranometria-7.0.0-windows-x64.zip`, as installed in [subject A](734-windows-A-reader.md)  
**Archive SHA-256:** `19d5354f0ac0fbe174b1e2ce5abb0af16b3d472d880a28caf1d7a2d9d8d8968d`  
**Platform and displays:** as recorded in subject A, section 1  

Tick a box only after observing the expected behaviour. Add a short note beneath a failed or incomplete check, with a screenshot and the exact steps. Do not troubleshoot during the test. A picture that merely looks different from the Mac is not a failure: note it, and say what a reader would lose, if anything.

## 1. Controller

- [ ] Open the Controller and resize it narrower and wider.
- [ ] Controls wrap without overlapping, disappearing or forcing a surprising window size.
- [ ] Open several sections and use a few switches; the section behaviour remains normal.
- [ ] *Keep one section open* behaves as expected.

**Observation:**


## 2. Dialogs

- [ ] *Place and Time*: all fields, labels and buttons fit without clipping; *Now* or an applied time updates the chart; it closes normally.
- [ ] *Export Sheet*: contents fit without clipping or unexpected shrinking.
- [ ] *Chart Options*, *Settings* and *About*: text fits, nothing is cut off.

**Observation:**


## 3. Display scaling and displays

- [ ] At the display's current scaling (note the percentage), chart text and lines are sharp, not blurred or doubled.
- [ ] Change scaling (for example 100 % ↔ 150 %), restart the application: windows and dialogs still fit and text is sharp.
- [ ] If a second display is available: move the main window and the Controller across; they redraw correctly and keep a sensible size. (Otherwise list it under *Not tested*.)

**Observation:**


## 4. Languages and grounds

- [ ] Switch the interface to Norwegian and back to English: menus, Controller and dialogs change, nothing is clipped in either.
- [ ] Switch between the two grounds (light and dark sky): chart, labels and furniture remain legible on both.

**Observation:**


## 5. Chart text and selection

- [ ] Star names, constellation labels and the chart title are readable and do not collide with the furniture more than expected.
- [ ] Click an object: the selected-object box appears, fits its text and sits clear of the object.
- [ ] Enlarged text (Settings): labels grow, dialogs still fit.
- [ ] Keyboard access: the chart keyboard commands (pan, zoom) work, and menus open with Alt and the Ctrl shortcuts (for example Ctrl-Shift-B for bookmarks).

**Observation:**


## 6. Whole sky and ecliptic notation

- [ ] Enter *Whole sky* and show the ecliptic.
- [ ] Enable *Longitude scale* and *Solar longitude marker* in Chart Options.
- [ ] The ruler is readable and the marker is visible with its numeric longitude.
- [ ] Switch ground; the notation remains legible and correctly placed.

**Observation:**


## 7. Export

- [ ] Export one PNG and one PDF or SVG (whichever the sheet offers) into a folder whose path contains a space.
- [ ] Open each: it contains the expected chart, title and furniture, with the same text as on screen.

**Observation:**


## 8. Saturn close view

- [ ] Set chart time to 2034-01-08, centre Saturn and choose the 0.25° field.
- [ ] No isolated dark or light dots near the Cassini gap resemble extra moons.
- [ ] Real moon marks and labels remain distinct where drawn.
- [ ] Take a screenshot of this view whatever the result.

**Observation:**


## 9. Bookmarks and persistence

- [ ] Add a bookmark, change the view, return through the bookmark: object, time and place come back.
- [ ] *Library → Export Bookmarks…* writes a file; *Restore from File…* reads it back.
- [ ] Quit and restart: window size and position, language, ground and bookmarks are as left.

**Observation:**


## Not tested in this record

List here anything above that was skipped, and why (for example: no second display).

## Verdict

Replace `DRAFT` below and at the top with exactly one of `PASS`, `FAIL` or `INCOMPLETE`.

**DRAFT**

**Overall observation:**

