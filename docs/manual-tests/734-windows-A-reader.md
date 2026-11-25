# Hands-on test — #734 subject A: clean reader installation on Windows

**Status:** DRAFT  
**Tester:**  
**Date:**  
**Issue:** [#734 — Establish a clean Windows installation and rendering baseline](https://github.com/Aha43/JUranometria/issues/734)  
**Artifact:** Published `JUranometria-7.0.0-windows-x64.zip` from the [v7.0.0 release](https://github.com/Aha43/JUranometria/releases/tag/v7.0.0)  
**Archive SHA-256:** `19d5354f0ac0fbe174b1e2ce5abb0af16b3d472d880a28caf1d7a2d9d8d8968d`  
**Platform:** the owner's private Windows PC (fill in section 1)  

This is an owner-operated check of the published application, as a reader would meet it. Tick a box only after observing the expected behaviour. Add a short note beneath a failed or incomplete check. Do not troubleshoot during the test: preserve what happened, with a screenshot and the exact steps for every mismatch. Use an ordinary non-administrator account if practical.

Screenshots: attach them to a comment on #734, or commit them under `docs/manual-tests/734-screenshots/`, and name them in the observation.

## 1. Environment

Paste the output of these PowerShell lines under the block, or fill the table by hand.

```powershell
Get-ComputerInfo | Select-Object OsName, OsVersion, OsBuildNumber, OsArchitecture, CsModel, CsProcessors
Get-CimInstance Win32_VideoController | Select-Object Name, CurrentHorizontalResolution, CurrentVerticalResolution
Get-Culture; Get-WinUserLanguageList | Select-Object LanguageTag
where.exe java
```

| | |
|---|---|
| Windows edition, version, build | |
| Machine, processor, architecture | |
| Graphics | |
| Displays: resolution and scaling of each | |
| Locale and display language | |
| Theme (light/dark), text size or other accessibility settings | |
| Account type (administrator or standard) | |
| Antivirus or security product | |
| Is a Java found on the PATH? (`where.exe java`) | |

## 2. Download and verify

- [ ] Download `JUranometria-7.0.0-windows-x64.zip` with a browser from the release page.
- [ ] `Get-FileHash .\JUranometria-7.0.0-windows-x64.zip -Algorithm SHA256` prints the SHA-256 above (letter case aside).
- [ ] Neither the browser nor the security product blocked, quarantined or warned about the download.

**Observation:**


## 3. Unpack and first launch

- [ ] Unpack with Explorer's *Extract All* into a folder whose path contains a space (for example `C:\Users\<you>\Star Atlas\`).
- [ ] Launch `JUranometria\JUranometria.exe` by double-click.
- [ ] If SmartScreen shows "Windows protected your PC", *More info* then *Run anyway* starts it. Note whether the dialog appeared.
- [ ] The application window opens with a chart; no console window stays open and no error dialog appears.
- [ ] The window, taskbar button and Alt-Tab entry carry the application's own name and icon.
- [ ] No Java was needed: either no Java is installed, or the application runs from the bundled runtime (Task Manager → Details → `JUranometria.exe`, *Open file location* is the unpacked folder).

**Observation:**


## 4. The packaged acceptance

- [ ] In PowerShell, from the unpacked `JUranometria` folder: `.\juranometria-acceptance.exe` ends with `PACKAGED ACCEPTANCE OK`. (If it prints nothing in the window, run `cmd /c "juranometria-acceptance.exe > acceptance.log 2>&1"` and read the last line of `acceptance.log`.)

**Observation (paste the last five lines):**


## 5. Where things live

- [ ] Add one bookmark (*Bookmarks → Add Bookmark…*). A file appears under `%APPDATA%\JUranometria`.
- [ ] Change one Chart Options switch. Preferences exist in the registry at `HKEY_CURRENT_USER\Software\JavaSoft\Prefs\juranometria` (`reg query HKCU\Software\JavaSoft\Prefs\juranometria`).
- [ ] Export one PNG from *Export Sheet*; the dialog proposes a sensible folder and the file is written where chosen.

**Observation (the folders actually used):**


## 6. Restart, remove, reinstall

- [ ] Quit and launch again: the bookmark and the changed switch are still there.
- [ ] Delete the unpacked folder, unpack the archive again into a different folder, launch: the application starts, and the bookmark and switch are still there (state lives outside the application folder).
- [ ] Remove the state by hand (delete `%APPDATA%\JUranometria`; `reg delete HKCU\Software\JavaSoft\Prefs\juranometria /f`), launch: the application starts as on a first run, with no error.

**Observation:**


## Not tested in this record

List here anything above that was skipped, and why.

## Verdict

Replace `DRAFT` below and at the top with exactly one of `PASS`, `FAIL` or `INCOMPLETE`.

**DRAFT**

**Overall observation:**

