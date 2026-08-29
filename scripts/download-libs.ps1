$ErrorActionPreference = "Stop"

# Versions come from lib-versions.env, the single authority shared
# with download-libs.sh and the Makefile.
$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$Versions = @{}
Get-Content (Join-Path $ScriptDir "lib-versions.env") | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.+)$') { $Versions[$Matches[1]] = $Matches[2] }
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
    @{ File = "flatlaf-$FlatLafVersion.jar";        GroupPath = "com/formdev/flatlaf/$FlatLafVersion";        Dir = $LibDir },
    @{ File = "flatlaf-extras-$FlatLafVersion.jar"; GroupPath = "com/formdev/flatlaf-extras/$FlatLafVersion"; Dir = $LibDir },
    @{ File = "jsvg-$JsvgVersion.jar";              GroupPath = "com/github/weisj/jsvg/$JsvgVersion";         Dir = $LibDir },
    @{ File = "junit-platform-console-standalone-$JUnitVersion.jar";
       GroupPath = "org/junit/platform/junit-platform-console-standalone/$JUnitVersion"; Dir = $TestLibDir }
)

foreach ($Dep in $Dependencies) {
    $TargetPath = Join-Path $Dep.Dir $Dep.File
    if (Test-Path $TargetPath) { Write-Host "Already exists: $($Dep.File)"; continue }
    $Url = "$Maven/$($Dep.GroupPath)/$($Dep.File)"
    Write-Host "Downloading $($Dep.File)..."
    Invoke-WebRequest -Uri $Url -OutFile $TargetPath
}

Write-Host "Done."
Write-Host "  Runtime libs : $LibDir"
Write-Host "  Test libs    : $TestLibDir"
