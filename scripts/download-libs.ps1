$ErrorActionPreference = "Stop"

# Versions come from lib-versions.env, the single authority shared
# with download-libs.sh and the Makefile.
$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$Versions = @{}
Get-Content (Join-Path $ScriptDir "lib-versions.env") | ForEach-Object {
    if ($_ -match '^([A-Z0-9_]+)=(.+)$') { $Versions[$Matches[1]] = $Matches[2] }
}
$FlatLafVersion = $Versions["FLATLAF_VERSION"]
$JsvgVersion    = $Versions["JSVG_VERSION"]
$JUnitVersion   = $Versions["JUNIT_VERSION"]
$RootDir    = Resolve-Path (Join-Path $ScriptDir "..")
$LibDir     = Join-Path $RootDir "lib"
$TestLibDir = Join-Path $RootDir "lib/test"

New-Item -ItemType Directory -Force $LibDir     | Out-Null
New-Item -ItemType Directory -Force $TestLibDir | Out-Null

$Maven = "https://repo1.maven.org/maven2"

$Dependencies = @(
    @{ File = "flatlaf-$FlatLafVersion.jar";        GroupPath = "com/formdev/flatlaf/$FlatLafVersion";        Dir = $LibDir;     Sha = $Versions["FLATLAF_SHA256"] },
    @{ File = "flatlaf-extras-$FlatLafVersion.jar"; GroupPath = "com/formdev/flatlaf-extras/$FlatLafVersion"; Dir = $LibDir;     Sha = $Versions["FLATLAF_EXTRAS_SHA256"] },
    @{ File = "jsvg-$JsvgVersion.jar";              GroupPath = "com/github/weisj/jsvg/$JsvgVersion";         Dir = $LibDir;     Sha = $Versions["JSVG_SHA256"] },
    @{ File = "junit-platform-console-standalone-$JUnitVersion.jar";
       GroupPath = "org/junit/platform/junit-platform-console-standalone/$JUnitVersion"; Dir = $TestLibDir; Sha = $Versions["JUNIT_SHA256"] }
)

function Get-Sha256([string]$Path) {
    (Get-FileHash -Algorithm SHA256 -Path $Path).Hash.ToLowerInvariant()
}

foreach ($Dep in $Dependencies) {
    $TargetPath = Join-Path $Dep.Dir $Dep.File
    if (Test-Path $TargetPath) {
        if ((Get-Sha256 $TargetPath) -eq $Dep.Sha) {
            Write-Host "Already exists: $($Dep.File)"
            continue
        }
        Write-Host "Checksum mismatch for existing $($Dep.File) (partial or corrupt); re-downloading..."
        Remove-Item $TargetPath
    }
    $Url = "$Maven/$($Dep.GroupPath)/$($Dep.File)"
    Write-Host "Downloading $($Dep.File)..."
    # Download to a temporary name and move into place only after the
    # checksum verifies, so an interrupted transfer can never pose as
    # a cached dependency.
    $TempPath = "$TargetPath.download"
    Invoke-WebRequest -Uri $Url -OutFile $TempPath
    $Actual = Get-Sha256 $TempPath
    if ($Actual -ne $Dep.Sha) {
        Remove-Item $TempPath
        throw "$($Dep.File) failed its pinned SHA-256 (expected $($Dep.Sha), got $Actual). Check the network and scripts/lib-versions.env, then rerun this script."
    }
    Move-Item $TempPath $TargetPath
}

Write-Host "Done."
Write-Host "  Runtime libs : $LibDir"
Write-Host "  Test libs    : $TestLibDir"
