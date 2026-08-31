<#
.SYNOPSIS
  Full runtime loading test for the Signpost mod.

.DESCRIPTION
  Launches every loader (fabric / neoforge / forge) on both sides (client / server) in
  both configurations (with / without other mods) - 12 runs - and checks that each one
  starts and loads a pristine save written by a PREVIOUS RELEASED VERSION.

  Each run gets a fresh copy of the pristine save, so Minecraft can never mutate the
  corpus. Runs are strictly sequential: two servers would fight over port 25565, and two
  clients would fight over the GPU.

  Windows only - it uses CIM/WMI to find and stop the forked game JVMs.

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File .claude\skills\full-runtime-test\run-full-runtime-test.ps1

.EXAMPLE
  ... -ServersOnly -Only fabric,neoforge
#>
[CmdletBinding()]
param(
    # Which testsaves/<version> to load. Defaults to the highest-sorting directory.
    [string] $SaveVersion,
    # Substring filter on run ids, e.g. -Only fabric-server,forge
    [string[]] $Only = @(),
    # Skip the 6 client runs (they open real windows). Servers still run.
    [switch] $ServersOnly,
    [int] $ServerTimeoutSec = 420,
    [int] $ClientTimeoutSec = 600
)

$ErrorActionPreference = 'Stop'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$SaveRoot = Join-Path $RepoRoot 'testsaves'
$Stamp    = Get-Date -Format 'yyyyMMdd-HHmmss'
$OutDir   = Join-Path $RepoRoot "build\runtime-test\$Stamp"
$QuickPlayName = 'signpost-runtime-test'

# ---------------------------------------------------------------------------------------
# The matrix. gameDir is relative to the repo root and must match the run configurations
# in the loader build scripts. Note forge's tasks have no "run" prefix.
# ---------------------------------------------------------------------------------------
$Matrix = @(
    @{ id = 'fabric-server';        task = ':fabric:runServer';         gameDir = 'fabric/runs/server';           side = 'server' }
    @{ id = 'fabric-server-mods';   task = ':fabric:runServerWithMods'; gameDir = 'fabric/runs/server_with_mods'; side = 'server' }
    @{ id = 'neoforge-server';      task = ':neoforge:runServer';       gameDir = 'neoforge/run';                 side = 'server' }
    @{ id = 'neoforge-server-mods'; task = ':neoforge:runServerWithMods'; gameDir = 'neoforge/run_with_mods';     side = 'server' }
    @{ id = 'forge-server';         task = ':forge:Server';             gameDir = 'forge/runs/server';            side = 'server' }
    @{ id = 'forge-server-mods';    task = ':forge:ServerWithMods';     gameDir = 'forge/runs/server_with_mods';  side = 'server' }
    @{ id = 'fabric-client';        task = ':fabric:runClient';         gameDir = 'fabric/runs/client';           side = 'client' }
    @{ id = 'fabric-client-mods';   task = ':fabric:runClientWithMods'; gameDir = 'fabric/runs/client_with_mods'; side = 'client' }
    @{ id = 'neoforge-client';      task = ':neoforge:runClient';       gameDir = 'neoforge/run';                 side = 'client' }
    @{ id = 'neoforge-client-mods'; task = ':neoforge:runClientWithMods'; gameDir = 'neoforge/run_with_mods';     side = 'client' }
    @{ id = 'forge-client';         task = ':forge:Client';             gameDir = 'forge/runs/client';            side = 'client' }
    @{ id = 'forge-client-mods';    task = ':forge:ClientWithMods';     gameDir = 'forge/runs/client_with_mods';  side = 'client' }
)

# Lines that mean the run FAILED.
$FailPatterns = @(
    'Failed to parse saved data',
    'NoClassDefFoundError',
    'ClassNotFoundException',
    'Mixin apply failed',
    'InvalidInjectionException',
    'Failed to start the minecraft server',
    'FatalStartupException',
    'Exception in thread "main"',
    '---- Minecraft Crash Report ----',
    'Failed to load datapacks',
    'net\.minecraft\.ReportedException',
    # Fabric mod-resolution failures. Without this the launcher process lingers after the
    # game aborts, so the run sits until the client timeout instead of failing in seconds.
    'Some of your mods are incompatible',
    'Mod resolution encountered an incompatible mod set',
    'ModResolutionException'
)

# Lines that LOOK like failures but are known noise. Filtered out BEFORE the check above -
# note the Netty entries contain "NoClassDefFoundError", so order matters.
# Keep this list tight and justified; see AGENTS.md "Known-benign log noise".
$BenignPatterns = @(
    'io\.netty\.channel\.(kqueue|epoll)\.Native',  # Netty probing foreign native transports on Windows
    'No data fixer registered for',                # vanilla logs this for every modded block entity on Fabric
    "Reference map 'signpost\.refmap\.json'",      # NeoForge/Forge run on official mappings, no refmap needed
    'Object did not get ID it asked for',          # Forge registry renumbering at boot
    'Ambiguity between arguments',                 # vanilla brigadier noise
    'SERVER IS RUNNING IN OFFLINE/INSECURE MODE',
    'Unable to delete file .*logs',                # log4j losing a race on its own rotation
    # Third-party mods ship OPTIONAL mixins aimed at mods that are not installed; Mixin
    # probes for the target class, fails, and logs it at WARN. kuma_api (bundled inside
    # Balm) does this for the "Controlling" mod. The negative lookahead keeps this narrow:
    # the same message about one of OUR classes is still a real failure.
    'Error loading class: (?!.*signpost).*ClassNotFoundException',
    '@Mixin target (?!.*signpost).* was not found'
)

function Write-Head([string] $Text) {
    Write-Host ''
    Write-Host "=== $Text" -ForegroundColor Cyan
}

function Stop-GameJvms {
    # NeoForge -> fml.modFolders, Fabric -> fabricmc.loader, Forge -> ForgeBootstrap.
    # Gradle's daemon and wrapper are also java.exe, so never kill by process name alone.
    $procs = @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
               Where-Object { $_.CommandLine -match 'fml\.modFolders|fabricmc\.loader|ForgeBootstrap' })
    foreach ($p in $procs) {
        Write-Host "    stopping leftover game JVM PID $($p.ProcessId)" -ForegroundColor DarkYellow
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    }
    if ($procs.Count -gt 0) { Start-Sleep -Seconds 4 }
}

# A loader error window is a modal Swing dialog: the JVM sits on it forever waiting for a
# human to click Exit, so the run burns its whole timeout and leaves a window on screen.
# Match only *error dialog* titles - the running game's own window ("Minecraft 1.21.11")
# must not trip this. Fabric titles its dialog "Fabric Loader <version>".
$DialogTitlePattern = 'Incompatible mods found|Fabric Loader|Mod Loading Has Failed|Loading Error|Minecraft Crash|Failed to launch'

function Get-BlockingDialogTitle {
    $p = Get-Process java -ErrorAction SilentlyContinue |
         Where-Object { $_.MainWindowTitle -and $_.MainWindowTitle -match $DialogTitlePattern } |
         Select-Object -First 1
    if ($p) { return $p.MainWindowTitle }
    return $null
}

function Wait-PortFree([int] $Port, [int] $TimeoutSec = 30) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (-not (Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue)) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

$PlantMarker = '.signpost-runtime-test'

function Copy-PristineSave([string] $SourceWorld, [string] $DestPath) {
    # Never destroy a world the user created. A save this harness planted carries a marker
    # file, so it can be replaced freely; anything else gets moved aside once, keeping the
    # user's original rather than the previous test run.
    if (Test-Path $DestPath) {
        if (Test-Path (Join-Path $DestPath $PlantMarker)) {
            Remove-Item -Recurse -Force $DestPath
        }
        else {
            $aside = "$DestPath.preserved-$Stamp"
            Write-Host "    preserving existing world -> $(Split-Path -Leaf $aside)" -ForegroundColor DarkYellow
            Move-Item -Path $DestPath -Destination $aside
        }
    }
    New-Item -ItemType Directory -Force -Path $DestPath | Out-Null
    # robocopy handles large trees and long paths far better than Copy-Item.
    $null = robocopy $SourceWorld $DestPath /MIR /NFL /NDL /NJH /NJS /NP /R:1 /W:1
    $code = $LASTEXITCODE
    $global:LASTEXITCODE = 0
    if ($code -ge 8) { throw "robocopy failed ($code) copying $SourceWorld -> $DestPath" }
    Set-Content -Path (Join-Path $DestPath $PlantMarker) -Value $SaveVersion -Encoding ascii
}

# ---------------------------------------------------------------------------------------
# Corpus resolution
# ---------------------------------------------------------------------------------------
$corpusHelp = @"
This harness loads a save written by a PREVIOUS RELEASED VERSION, so the corpus cannot be
generated from the current build. Populate it once per release:

  testsaves/<released-version>/world/    <- a complete save folder (level.dat, region/, data/, ...)

for example testsaves/2.03.0/world/, copied from a world last saved by Signpost 2.03.0.
The directory is gitignored on purpose; see testsaves/README.md.
"@

if (-not (Test-Path $SaveRoot)) {
    Write-Host "No test-save corpus found at: $SaveRoot" -ForegroundColor Red
    Write-Host $corpusHelp -ForegroundColor Red
    exit 2
}

if (-not $SaveVersion) {
    $candidates = @(Get-ChildItem -Directory $SaveRoot -ErrorAction SilentlyContinue |
                    Where-Object { Test-Path (Join-Path $_.FullName 'world') } |
                    Sort-Object Name)
    if ($candidates.Count -eq 0) {
        Write-Host "No testsaves/<version>/world/ directory found under $SaveRoot" -ForegroundColor Red
        Write-Host $corpusHelp -ForegroundColor Red
        exit 2
    }
    $SaveVersion = $candidates[-1].Name
}

$PristineWorld = Join-Path $SaveRoot "$SaveVersion\world"
if (-not (Test-Path (Join-Path $PristineWorld 'level.dat'))) {
    Write-Host "testsaves/$SaveVersion/world/level.dat is missing - that is not a save folder." -ForegroundColor Red
    exit 2
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
Write-Head 'Signpost full runtime loading test'
Write-Host "  repo        : $RepoRoot"
Write-Host "  save corpus : testsaves/$SaveVersion  (pristine, freshly copied per run)"
Write-Host "  logs        : $OutDir"

# Audit the corpus before spending an hour on it. A save from the wrong version, or one
# with no Signpost content in it, makes every run below meaningless.
$auditScript = Join-Path $PSScriptRoot 'audit-test-save.py'
if (Test-Path $auditScript) {
    $py = Get-Command python -ErrorAction SilentlyContinue
    if ($py) {
        Write-Head "Corpus audit"
        & $py.Source $auditScript (Join-Path $SaveRoot $SaveVersion)
        if ($LASTEXITCODE -ne 0) {
            $global:LASTEXITCODE = 0
            Write-Host 'The corpus audit found a blocking problem. Fix the save before running the matrix.' -ForegroundColor Red
            exit 2
        }
        $global:LASTEXITCODE = 0
    }
    else {
        Write-Host '  (python not on PATH - skipping the corpus audit)' -ForegroundColor DarkYellow
    }
}

$runs = @($Matrix)
if ($ServersOnly) { $runs = @($runs | Where-Object { $_.side -eq 'server' }) }
# powershell.exe -File passes "a,b,c" as ONE string rather than an array, so split the
# terms here. Without this, -Only a,b silently matches nothing and the run exits with
# "No runs matched the filter".
$Only = @($Only | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })

if ($Only.Count -gt 0) {
    # A term that names a run exactly matches only that run. Without this, "forge-server-mods"
    # would also select "neoforge-server-mods", since it is a substring of it.
    $allIds = $Matrix | ForEach-Object { $_.id }
    $runs = @($runs | Where-Object {
        $id = $_.id
        @($Only | Where-Object {
            if ($allIds -contains $_) { $id -eq $_ } else { $id -like "*$_*" }
        }).Count -gt 0
    })
}
if ($runs.Count -eq 0) { Write-Host 'No runs matched the filter.' -ForegroundColor Red; exit 2 }
Write-Host "  runs        : $($runs.Count) of $($Matrix.Count)"

$results = @()

foreach ($r in $runs) {
    Write-Head "$($r.id)   [$($r.task)]"
    $log     = Join-Path $OutDir "$($r.id).log"
    $gameDir = Join-Path $RepoRoot ($r.gameDir -replace '/', '\')
    $verdict = 'FAIL'
    $reason  = 'did not start'
    $proc    = $null

    try {
        Stop-GameJvms
        if ($r.side -eq 'server' -and -not (Wait-PortFree 25565)) {
            $results += [pscustomobject]@{ Run = $r.id; Result = 'FAIL'; Detail = 'port 25565 still in use'; Log = $log }
            Write-Host '  FAIL - port 25565 still in use' -ForegroundColor Red
            continue
        }

        New-Item -ItemType Directory -Force -Path $gameDir | Out-Null

        if ($r.side -eq 'server') {
            Copy-PristineSave $PristineWorld (Join-Path $gameDir 'world')
            Set-Content -Path (Join-Path $gameDir 'eula.txt') -Value 'eula=true' -Encoding ascii
        }
        else {
            Copy-PristineSave $PristineWorld (Join-Path $gameDir "saves\$QuickPlayName")
        }
        Write-Host "  planted save from testsaves/$SaveVersion"

        $gradleArgs = @($r.task, '--console=plain')
        if ($r.side -eq 'client') { $gradleArgs += "-PsignpostQuickPlay=$QuickPlayName" }

        $proc = Start-Process -FilePath (Join-Path $RepoRoot 'gradlew.bat') `
                              -ArgumentList $gradleArgs `
                              -WorkingDirectory $RepoRoot `
                              -RedirectStandardOutput $log `
                              -RedirectStandardError "$log.err" `
                              -PassThru -NoNewWindow

        $timeout  = if ($r.side -eq 'server') { $ServerTimeoutSec } else { $ClientTimeoutSec }
        $deadline = (Get-Date).AddSeconds($timeout)
        # Server: "Done (" means started AND finished loading the world.
        # Client: quickPlay has actually entered the world once advancements load.
        $successRe = if ($r.side -eq 'server') { 'Done \(' } else { 'Loaded \d+ advancements' }

        while ((Get-Date) -lt $deadline) {
            Start-Sleep -Seconds 5
            if (-not (Test-Path $log)) { continue }
            # Gradle prints task failures to stderr, so both streams must be inspected -
            # looking only at stdout reports a useless "exited early" with no cause.
            $text = (Get-Content $log -Raw -ErrorAction SilentlyContinue)
            $errText = (Get-Content "$log.err" -Raw -ErrorAction SilentlyContinue)
            if ($errText) { $text = "$text`n$errText" }
            if (-not $text) { continue }

            $clean = ($text -split "`n" | Where-Object {
                          $line = $_
                          @($BenignPatterns | Where-Object { $line -match $_ }).Count -eq 0
                      }) -join "`n"

            $dlg = Get-BlockingDialogTitle
            if ($dlg) {
                $verdict = 'FAIL'
                $reason  = "blocking error dialog: `"$dlg`""
                break
            }

            $hit = $FailPatterns | Where-Object { $clean -match $_ } | Select-Object -First 1
            if ($hit)                       { $verdict = 'FAIL'; $reason = "matched /$hit/"; break }
            if ($text -match $successRe)    { $verdict = 'PASS'; $reason = 'started and loaded the save'; break }
            if ($text -match 'BUILD FAILED'){ $verdict = 'FAIL'; $reason = 'gradle BUILD FAILED before launch'; break }
            if ($proc.HasExited) {
                $verdict = 'FAIL'
                $why = ($text -split "`n" | Select-String -Pattern '^> ' | Select-Object -First 1).ToString().Trim()
                $reason = if ($why) { "exited early: $why" } else { 'process exited before loading the save' }
                break
            }
        }
        if ($verdict -eq 'FAIL' -and $reason -eq 'did not start') { $reason = "timed out after ${timeout}s" }
    }
    catch {
        $verdict = 'FAIL'
        $reason  = $_.Exception.Message
    }
    finally {
        if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue }
        Stop-GameJvms
    }

    $color = if ($verdict -eq 'PASS') { 'Green' } else { 'Red' }
    Write-Host "  $verdict - $reason" -ForegroundColor $color
    $results += [pscustomobject]@{ Run = $r.id; Result = $verdict; Detail = $reason; Log = $log }
}

Write-Head 'Summary'
$table = $results | Format-Table -AutoSize Run, Result, Detail | Out-String
Write-Host $table
Set-Content -Path (Join-Path $OutDir 'summary.txt') -Value $table -Encoding utf8

$failed = @($results | Where-Object { $_.Result -ne 'PASS' })
Write-Host "Logs: $OutDir"
if ($failed.Count -gt 0) {
    Write-Host "$($failed.Count) of $($results.Count) runs FAILED." -ForegroundColor Red
    foreach ($f in $failed) { Write-Host "  $($f.Run): $($f.Detail)  ->  $($f.Log)" -ForegroundColor Red }
    exit 1
}
Write-Host "All $($results.Count) runs passed." -ForegroundColor Green
exit 0
