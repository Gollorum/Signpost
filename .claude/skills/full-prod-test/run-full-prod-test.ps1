<#
.SYNOPSIS
  Production runtime loading test for the Signpost mod.

.DESCRIPTION
  Builds the mod jars, then launches the game the way a PLAYER launches it - from an
  installed .minecraft profile, through launch-prod.py - and checks that each run loads a
  pristine save written by a PREVIOUS RELEASED VERSION.

  This is the production counterpart to full-runtime-test. That harness runs the Gradle run
  tasks, which on Fabric use NAMED mappings; players get INTERMEDIARY. Every mapping-sensitive
  bug is therefore invisible to it - see issue #118.

  Each run gets its own throwaway game directory and a fresh copy of the pristine save, so
  neither the corpus nor the user's real .minecraft is ever written to. Runs are strictly
  sequential: two clients would fight over the GPU, two servers over port 25565.

  Windows only - it uses CIM/WMI to find and stop the launched game JVMs.

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File .claude\skills\full-prod-test\run-full-prod-test.ps1

.EXAMPLE
  ... -Only fabric-client -SkipBuild
#>
[CmdletBinding()]
param(
    # Which testsaves/<version>/<mc>/world to load. Defaults to the highest-sorting version.
    [string] $SaveVersion,
    # Substring filter on run ids, e.g. -Only fabric-client,neoforge
    [string[]] $Only = @(),
    # Restrict to these loaders even if others are installed.
    [string[]] $Loaders = @(),
    # Reuse the jars already in <loader>/build/libs.
    [switch] $SkipBuild,
    # Download nothing; report whatever is missing as SKIP instead. Without this the harness
    # provisions what it needs - see "Provisioning" below.
    [switch] $NoDownload,
    [string] $MinecraftHome,
    # Java 21 for Minecraft 1.21+. Auto-detected when omitted.
    [string] $Java,
    [int] $ClientTimeoutSec = 600,
    [int] $ServerTimeoutSec = 420,
    # How long to keep reading the log after the success marker, before passing the run.
    [int] $SettleSec = 10
)

$ErrorActionPreference = 'Stop'
$RepoRoot  = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$SaveRoot  = Join-Path $RepoRoot 'testsaves'
$ProdRoot  = Join-Path $RepoRoot 'prodtest'
$Stamp     = Get-Date -Format 'yyyyMMdd-HHmmss'
$OutDir    = Join-Path $RepoRoot "build\prod-test\$Stamp"
$Launcher  = Join-Path $PSScriptRoot 'launch-prod.py'
$QuickPlayName = 'signpost-prod-test'
# Unique to jars this harness launches, so cleanup can never touch the user's own game.
$BrandArg  = 'signpost-prod-test'

if (-not $MinecraftHome) { $MinecraftHome = Join-Path $env:APPDATA '.minecraft' }

# powershell.exe -File passes "a,b,c" as ONE string rather than an array, so split the terms
# here - before anything reads them. Without this, -Only a,b silently matches nothing.
$Only = @($Only | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$Loaders = @($Loaders | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })

# Provisioning is expensive - a loader install is a multi-minute download - so it follows the
# same filters the runs do. Asking for -Only fabric-client must not install Forge.
function Test-LoaderInScope([string] $Loader) {
    if ($Loaders.Count -gt 0) { return $Loaders -contains $Loader }
    if ($Only.Count -eq 0) { return $true }
    $suffixes = @('client', 'client-mods', 'server', 'server-mods')
    $allIds = @(foreach ($l in $AllLoaders) { foreach ($s in $suffixes) { "$l-$s" } })
    foreach ($term in $Only) {
        if ($term -eq $Loader) { return $true }
        if ($allIds -contains $term) {
            # Exactly one loader owns an exact run id - the same rule the run filter uses.
            # Without this "forge-client" would also drag in neoforge, which is a multi-minute
            # install of something the user did not ask for.
            if ($term -like "$Loader-*") { return $true }
            continue
        }
        foreach ($s in $suffixes) { if ("$Loader-$s" -like "*$term*") { return $true } }
    }
    return $false
}

function Write-Head([string] $Text) {
    Write-Host ''
    Write-Host "=== $Text" -ForegroundColor Cyan
}

function Fail-Setup([string] $Message) {
    Write-Host $Message -ForegroundColor Red
    exit 2
}

# ---------------------------------------------------------------------------------------
# Lines that mean a run FAILED.
#
# Production logs differ from the dev ones in a way that matters: Fabric does NOT tag log
# lines with the mod id outside the dev environment, so full-runtime-test's
# "/ERROR] (signpost)" pattern can never match here. The net is cast on the severity instead,
# with the benign list below as the pressure valve - these runs load only Signpost and its
# dependencies, so the log is quiet enough for that to be practical.
# ---------------------------------------------------------------------------------------
$FailPatterns = @(
    # The #118 signature. Vanilla logs this from WorldOpenFlows when the world stem - datapack
    # registries included - fails to load, and then shows the "safe mode" screen.
    'Failed to load level data or datapacks',
    'Registry loading errors',
    'Failed to load registries',
    'Failed to load datapacks',
    'Failed to parse saved data',
    'NoClassDefFoundError',
    'ClassNotFoundException',
    'Mixin apply failed',
    'InvalidInjectionException',
    'Failed to start the minecraft server',
    'FatalStartupException',
    'Exception in thread "main"',
    '---- Minecraft Crash Report ----',
    'net\.minecraft\.ReportedException',
    'Incompatible mods found',
    'Some of your mods are incompatible',
    'Mod resolution encountered an incompatible mod set',
    'ModResolutionException',
    # Catch-all for anything logged at ERROR or FATAL that the benign list does not excuse.
    # Signpost's own failures land here: a codec that cannot decode a block entity logs one
    # line and the game carries on with the data silently dropped.
    '/(ERROR|FATAL)\]'
)

# Lines that LOOK like failures but are known noise. Filtered out BEFORE the check above -
# note the Netty entries contain "NoClassDefFoundError", so order matters.
# Keep this list tight and justified; every entry is a hole in the net above.
$BenignPatterns = @(
    'io\.netty\.channel\.(kqueue|epoll)\.Native',   # Netty probing foreign native transports
    # Vanilla logs this at ERROR for every modded block entity and entity, because
    # SharedConstants.CHECK_DATA_FIXER_SCHEMA is true in release builds too. Signpost's own
    # post/waystone/waystone_generator appear here on every clean run.
    'No data fixer registered for',
    'Could not read metadata of .* for resource pack detection',
    'Unable to delete file .*logs',                 # log4j losing a race on its own rotation
    'SERVER IS RUNNING IN OFFLINE/INSECURE MODE',
    'Ambiguity between arguments',
    'Shader .* could not find sampler named',
    # The offline access token this harness launches with cannot talk to Mojang's services,
    # so the client logs these at ERROR on every run. Singleplayer needs neither.
    'Failed to fetch user properties',
    'Failed to fetch Realms feature flags',
    # Third-party mods ship OPTIONAL mixins aimed at mods that are not installed; Mixin probes
    # for the target class, fails, and logs it. kuma_api (bundled inside Balm) does this for
    # the "Controlling" mod, which fails every "-mods" run. The negative lookahead keeps this
    # narrow: the same message about one of OUR classes is still a real failure.
    'Error loading class: (?!.*signpost).*ClassNotFoundException',
    '@Mixin target (?!.*signpost).* was not found',
    # Vanilla logs this at ERROR the first time a server starts in a directory, then writes
    # the defaults and carries on. provision.py writes the file up front so this should not
    # appear, but a hand-made server install would still hit it.
    'Failed to load properties from file: server\.properties'
)

$DialogTitlePattern = 'Incompatible mods found|Fabric Loader|Mod Loading Has Failed|Loading Error|Minecraft Crash|Failed to launch'

function Stop-ProdGameJvms {
    # Matched on this harness's own brand, so a game the user started by hand - and Gradle's
    # daemon, which is also java.exe - can never be caught by this. Clients carry the string
    # through -Dminecraft.launcher.brand, servers through -Dsignpost.prodtest, which
    # provision.py writes into start.cmd. A server that is missed keeps its library jars open
    # and breaks the next install, so both must match.
    $procs = @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
               Where-Object { $_.CommandLine -match [regex]::Escape($BrandArg) })
    foreach ($p in $procs) {
        Write-Host "    stopping leftover prod game JVM PID $($p.ProcessId)" -ForegroundColor DarkYellow
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    }
    if ($procs.Count -gt 0) { Start-Sleep -Seconds 4 }
}

function Get-BlockingDialogTitle {
    # A loader error dialog is modal: the JVM waits forever for a human to click Exit, so the
    # run would burn its whole timeout and leave a window on the user's screen.
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

function Disable-AccessibilityOnboarding([string] $GameDir) {
    # On a game directory it has never run in, the client opens the accessibility onboarding
    # screen ("Would you like to enable the Narrator...") and waits for a human to click
    # Continue. Minecraft.java gates that on Options.onboardAccessibility, which defaults to
    # true, so quickPlay never happens and the run burns its whole timeout at the menu.
    # Existing settings are preserved - only this one key is set.
    $optionsFile = Join-Path $GameDir 'options.txt'
    $setting = 'onboardAccessibility:false'
    if (-not (Test-Path $optionsFile)) {
        New-Item -ItemType Directory -Force -Path $GameDir | Out-Null
        Set-Content -Path $optionsFile -Value $setting -Encoding ascii
        return
    }
    $lines = @(Get-Content $optionsFile)
    if ($lines -match '^onboardAccessibility:') {
        $lines = $lines -replace '^onboardAccessibility:.*$', $setting
    }
    else {
        $lines += $setting
    }
    Set-Content -Path $optionsFile -Value $lines -Encoding ascii
}

function Invoke-Provision {
    param([string[]] $Arguments, [string] $What)
    $quoted = @(@($Provisioner) + $Arguments | ForEach-Object { '"' + $_ + '"' })
    $logFile = Join-Path $OutDir ("provision-" + ($What -replace '[^\w.-]', '-') + ".log")
    $p = Start-Process -FilePath $python.Source -ArgumentList $quoted -WorkingDirectory $RepoRoot `
                       -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err" `
                       -PassThru -NoNewWindow -Wait
    Get-Content $logFile -ErrorAction SilentlyContinue | ForEach-Object { Write-Host $_ }
    if ($p.ExitCode -ne 0) {
        Get-Content "$logFile.err" -ErrorAction SilentlyContinue |
            ForEach-Object { Write-Host $_ -ForegroundColor Red }
        return $false
    }
    return $true
}

Add-Type -Namespace SignpostProdTest -Name Win -MemberDefinition @'
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);
'@

function Send-KeyToGame([int] $WindowHandle, [byte[]] $VirtualKeys) {
    # Minecraft draws its screens inside the GL window, so there is no OS dialog to click -
    # the only way in is synthesized input through the window's own message queue, which is
    # what keybd_event produces. The window has to be in the foreground to receive it.
    [SignpostProdTest.Win]::ShowWindow([IntPtr]$WindowHandle, 9) | Out-Null   # SW_RESTORE
    [SignpostProdTest.Win]::SetForegroundWindow([IntPtr]$WindowHandle) | Out-Null
    Start-Sleep -Milliseconds 400
    foreach ($vk in $VirtualKeys) {
        [SignpostProdTest.Win]::keybd_event($vk, 0, 0, [UIntPtr]::Zero)        # down
        Start-Sleep -Milliseconds 60
        [SignpostProdTest.Win]::keybd_event($vk, 0, 2, [UIntPtr]::Zero)        # up
        Start-Sleep -Milliseconds 180
    }
}

function Confirm-BlockingGameDialog {
    <#
      Dismisses a vanilla confirmation screen that is holding a client run hostage.

      The one that actually happens is "Worlds using Experimental Settings are not supported",
      shown by WorldOpenFlows when worldGenSettingsLifecycle() is not stable - which a modded
      world often is not. It is a BackupConfirmScreen: the buttons are, in order, "Create
      Backup and Load", "I know what I'm doing!" and "Cancel". Tab moves focus onto the first,
      a second Tab onto the second, and Enter activates the focused one. Both of the first two
      proceed to load the world, so landing on either is fine and only Cancel would be wrong -
      which is why this sends exactly two Tabs and never a third.

      Called only once a run has loaded its datapacks and then failed to enter the world, so
      idle keystrokes are never sent into a game that is merely still loading.
    #>
    param([string] $LogPath)
    $game = Get-Process java -ErrorAction SilentlyContinue |
            Where-Object { $_.MainWindowTitle -and $_.MainWindowTitle -like 'Minecraft*' } |
            Select-Object -First 1
    if (-not $game) { return $false }
    Write-Host "    a confirmation screen is blocking the run; confirming it" -ForegroundColor DarkYellow
    Send-KeyToGame $game.MainWindowHandle @([byte]0x09, [byte]0x09, [byte]0x0D)   # Tab Tab Enter
    return $true
}

function Copy-Tree([string] $Source, [string] $Dest) {
    New-Item -ItemType Directory -Force -Path $Dest | Out-Null
    $null = robocopy $Source $Dest /MIR /NFL /NDL /NJH /NJS /NP /R:1 /W:1
    $code = $LASTEXITCODE
    $global:LASTEXITCODE = 0
    if ($code -ge 8) { throw "robocopy failed ($code) copying $Source -> $Dest" }
}

$javaMajorCache = @{}
function Get-JavaMajor([string] $Exe) {
    # 21.0.5 -> 21, 1.8.0_351 -> 8. Read it from the JDK's own "release" file rather than
    # from "java -version": that prints to stderr, and under $ErrorActionPreference='Stop'
    # redirecting a native command's stderr throws NativeCommandError, so every probe would
    # come back 0 and no JDK would ever match. Mojang's bundled runtimes ship the file too.
    if ($javaMajorCache.ContainsKey($Exe)) { return $javaMajorCache[$Exe] }
    $major = 0
    $javaHome = Split-Path (Split-Path $Exe -Parent) -Parent
    $release = Join-Path $javaHome 'release'
    if (Test-Path $release) {
        $line = Select-String -Path $release -Pattern '^JAVA_VERSION="(1\.)?(\d+)' -ErrorAction SilentlyContinue |
                Select-Object -First 1
        if ($line) { $major = [int]$line.Matches[0].Groups[2].Value }
    }
    if ($major -eq 0) {
        # No release file (a stripped runtime): ask the JVM, with the stderr redirect made
        # safe for the duration of the call.
        $prev = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            $out = (& $Exe -version 2>&1 | Out-String)
            if ($out -match 'version "1\.(\d+)')  { $major = [int]$Matches[1] }
            elseif ($out -match 'version "(\d+)') { $major = [int]$Matches[1] }
        }
        catch { $major = 0 }
        finally { $ErrorActionPreference = $prev; $global:LASTEXITCODE = 0 }
    }
    $javaMajorCache[$Exe] = $major
    return $major
}

function Get-JavaCandidates {
    $c = @()
    # Mojang's bundled runtime first - it is literally the JVM the player's game runs on.
    $runtimeRoots = @(
        (Join-Path $MinecraftHome 'runtime'),
        (Join-Path $env:LOCALAPPDATA 'Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime'),
        (Join-Path ${env:ProgramFiles(x86)} 'Minecraft Launcher\runtime')
    )
    foreach ($root in $runtimeRoots) {
        if (Test-Path $root) {
            $c += @(Get-ChildItem -Path $root -Recurse -Filter 'java.exe' -ErrorAction SilentlyContinue |
                    ForEach-Object { $_.FullName })
        }
    }
    foreach ($root in @((Join-Path $env:ProgramFiles 'Java'),
                        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
                        (Join-Path $env:ProgramFiles 'Microsoft\jdk'),
                        (Join-Path $env:USERPROFILE '.jdks'))) {
        if (Test-Path $root) {
            $c += @(Get-ChildItem -Directory $root -ErrorAction SilentlyContinue |
                    ForEach-Object { Join-Path $_.FullName 'bin\java.exe' })
        }
    }
    if ($env:JAVA_HOME) { $c += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
    $onPath = Get-Command java -ErrorAction SilentlyContinue
    if ($onPath) { $c += $onPath.Source }
    return @($c | Where-Object { Test-Path $_ } | Select-Object -Unique)
}

function Resolve-GameJava([int] $WantMajor) {
    # The game must run on the Java version the profile asks for. Taking JAVA_HOME on faith
    # is how this harness ends up testing a JVM no player has - on this machine JAVA_HOME is
    # a JDK 25, four majors past what Minecraft 1.21.1 ships.
    if ($Java) { return $Java }
    foreach ($c in Get-JavaCandidates) {
        if ((Get-JavaMajor $c) -eq $WantMajor) { return $c }
    }
    return $null
}

function Resolve-BuildJavaHome([int] $WantMajor) {
    # Gradle needs a JDK, not the JRE Mojang bundles - so javac has to be there too.
    foreach ($c in Get-JavaCandidates) {
        # Not $home - that is a read-only automatic variable in PowerShell.
        $jdkHome = Split-Path (Split-Path $c -Parent) -Parent
        if ((Test-Path (Join-Path $jdkHome 'bin\javac.exe')) -and (Get-JavaMajor $c) -eq $WantMajor) {
            return $jdkHome
        }
    }
    return $null
}

# ---------------------------------------------------------------------------------------
# Project versions
# ---------------------------------------------------------------------------------------
$props = @{}
Get-Content (Join-Path $RepoRoot 'gradle.properties') | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z0-9_.]+)\s*=\s*(.+?)\s*$') { $props[$Matches[1]] = $Matches[2] }
}
$McVersion  = $props['minecraft_version']
$ModVersion = $props['version']
if (-not $McVersion -or -not $ModVersion) { Fail-Setup 'gradle.properties has no minecraft_version / version.' }

# ---------------------------------------------------------------------------------------
# Which production profiles are installed?
#
# The loader version must be the one the mod is built against, otherwise the run proves
# nothing about what a player gets. Fabric is the exception: its loader is a floor, not a
# pin, so any fabric-loader-*-<mc> profile will do and the newest is used.
# ---------------------------------------------------------------------------------------
$VersionsDir = Join-Path $MinecraftHome 'versions'
if (-not (Test-Path $VersionsDir)) { Fail-Setup "No Minecraft installation at $MinecraftHome (looked for versions/)." }

$AllLoaders = @('fabric', 'neoforge', 'forge')

# The loader version the mod is built against. Fabric's is a floor rather than a pin - any
# fabric-loader-*-<mc> profile runs the same jars - so its glob is wider than the other two.
$LoaderVersions = @{
    fabric   = $props['fabric_loader_version']
    neoforge = $props['neoforge_version']
    forge    = $props['forge_version']
}
$ProfileGlobs = @{
    fabric   = "fabric-loader-*-$McVersion"
    neoforge = "neoforge-$($props['neoforge_version'])"
    forge    = "$McVersion-forge-$($props['forge_version'])"
}

function Find-Profiles {
    $found = @{}
    foreach ($loader in $AllLoaders) {
        $hit = Get-ChildItem -Directory $VersionsDir -ErrorAction SilentlyContinue |
               Where-Object { $_.Name -like $ProfileGlobs[$loader] } |
               Sort-Object Name | Select-Object -Last 1
        if ($hit) { $found[$loader] = $hit.Name }
    }
    return $found
}

$profiles = Find-Profiles
$missingLoaders = @($AllLoaders | Where-Object { -not $profiles.ContainsKey($_) })

# ---------------------------------------------------------------------------------------
# Corpus
# ---------------------------------------------------------------------------------------
if (-not (Test-Path $SaveRoot)) { Fail-Setup "No test-save corpus at $SaveRoot - see testsaves/README.md." }

if (-not $SaveVersion) {
    $candidates = @(Get-ChildItem -Directory $SaveRoot -ErrorAction SilentlyContinue |
                    Where-Object { Test-Path (Join-Path $_.FullName "$McVersion\world") } |
                    Sort-Object Name)
    if ($candidates.Count -eq 0) {
        Fail-Setup @"
No testsaves/<version>/$McVersion/world/ found under $SaveRoot.

This harness loads a save written by a PREVIOUS RELEASED VERSION on this Minecraft version,
so the corpus cannot be generated from the current build. See testsaves/README.md.
"@
    }
    $SaveVersion = $candidates[-1].Name
}
$PristineWorld = Join-Path $SaveRoot "$SaveVersion\$McVersion\world"
if (-not (Test-Path (Join-Path $PristineWorld 'level.dat'))) {
    Fail-Setup "testsaves/$SaveVersion/$McVersion/world/level.dat is missing - that is not a save folder."
}
if ($SaveVersion -eq $ModVersion) {
    Fail-Setup "The corpus at testsaves/$SaveVersion was written by the version being built ($ModVersion). That tests nothing."
}

# The Minecraft version itself says which Java it wants; do not guess it.
$vanillaJson = Get-Content (Join-Path $VersionsDir "$McVersion\$McVersion.json") -Raw | ConvertFrom-Json
$JavaMajor = 21
if ($vanillaJson.javaVersion -and $vanillaJson.javaVersion.majorVersion) {
    $JavaMajor = [int]$vanillaJson.javaVersion.majorVersion
}

$JavaExe = Resolve-GameJava $JavaMajor
if (-not $JavaExe) {
    Fail-Setup @"
No Java $JavaMajor found, which is what Minecraft $McVersion asks for.

Looked in Mojang's bundled runtimes, Program Files\Java, Eclipse Adoptium, Microsoft\jdk,
~/.jdks, JAVA_HOME and PATH. Install a Java $JavaMajor, or pass -Java <path to java.exe>.
"@
}
$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) { Fail-Setup 'python is not on PATH; it is needed to build the launcher command.' }

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
Write-Head 'Signpost production runtime test'
Write-Host "  repo        : $RepoRoot"
Write-Host "  minecraft   : $McVersion   (.minecraft at $MinecraftHome)"
Write-Host "  mod version : $ModVersion"
Write-Host "  save corpus : testsaves/$SaveVersion/$McVersion  (pristine, freshly copied per run)"
Write-Host "  java        : $JavaExe"
Write-Host "  logs        : $OutDir"
foreach ($l in $AllLoaders) {
    if ($profiles.ContainsKey($l)) { Write-Host "  profile     : $l -> $($profiles[$l])" }
}
foreach ($l in $missingLoaders) {
    $fate = if ($NoDownload) { 'runs skipped, -NoDownload' }
            elseif (Test-LoaderInScope $l) { 'will be installed' }
            else { 'not in this run''s filter' }
    Write-Host "  profile     : $l -> NOT INSTALLED ($fate)" -ForegroundColor DarkYellow
}
if ($profiles.Count -eq 0) {
    Fail-Setup @"
None of the three loaders is installed for $McVersion in $MinecraftHome.

Install the matching profile once, from each loader's own installer:
  fabric   : any fabric-loader-*-$McVersion
  neoforge : neoforge-$($props['neoforge_version'])
  forge    : $McVersion-forge-$($props['forge_version'])
Then launch it once from the official launcher so its libraries and assets are downloaded.
"@
}

# Audit the corpus before spending an hour on it - same auditor the dev harness uses. It is
# handed the world folder itself rather than the version directory: given a directory it
# appends "world" only one level down, and this corpus nests the Minecraft version in between.
$auditScript = Join-Path $PSScriptRoot '..\full-runtime-test\audit-test-save.py'
if (Test-Path $auditScript) {
    Write-Head 'Corpus audit'
    & $python.Source $auditScript $PristineWorld
    if ($LASTEXITCODE -ne 0) {
        $global:LASTEXITCODE = 0
        Fail-Setup 'The corpus audit found a blocking problem. Fix the save before running the matrix.'
    }
    $global:LASTEXITCODE = 0
}

# ---------------------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------------------
# A loader with no installed profile cannot run at all, and its jar would not be built below
# either - so install it first. This writes into the user's real .minecraft, alongside the
# profiles the official launcher puts there; -NoDownload leaves it alone.
$Provisioner = Join-Path $PSScriptRoot 'provision.py'
$provisionFailed = @{}

if (-not $NoDownload -and $missingLoaders.Count -gt 0) {
    Write-Head 'Provisioning loader profiles'
    Write-Host "  installing into $MinecraftHome (use -NoDownload to disable all downloading)"
    foreach ($loader in $missingLoaders) {
        if (-not (Test-LoaderInScope $loader)) {
            Write-Host "  $loader : not in this run's filter - not installing it" -ForegroundColor DarkGray
            continue
        }
        if (-not $LoaderVersions[$loader]) {
            Write-Host "  $loader : no version in gradle.properties - cannot install" -ForegroundColor DarkYellow
            continue
        }
        Write-Host "  $loader : profile missing, installing $($LoaderVersions[$loader])"
        $ok = Invoke-Provision @('client', '--loader', $loader, '--mc', $McVersion,
                                 '--loader-version', $LoaderVersions[$loader],
                                 '--mc-home', $MinecraftHome, '--java', $JavaExe) "client-$loader"
        if ($ok) {
            # Re-discover rather than assume: the installer names the profile, not us.
            $profiles = Find-Profiles
            if ($profiles.ContainsKey($loader)) {
                Write-Host "  $loader : installed $($profiles[$loader])" -ForegroundColor Green
            }
            else {
                $provisionFailed[$loader] = 'installer succeeded but no profile appeared'
            }
        }
        else { $provisionFailed[$loader] = 'loader installation failed' }
    }
    $missingLoaders = @($AllLoaders | Where-Object { -not $profiles.ContainsKey($_) })
}

if (-not $SkipBuild) {
    Write-Head 'Building mod jars'
    # Gradle takes its JDK from JAVA_HOME, and a JDK newer than the project's Java 21
    # toolchain dies in buildSrc with "Unsupported class file major version". An inherited
    # JAVA_HOME pointing at a newer JDK is the normal case on a dev machine, so check it
    # rather than trusting it.
    $currentHomeJava = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { '' }
    if (-not ($currentHomeJava -and (Test-Path $currentHomeJava) -and (Get-JavaMajor $currentHomeJava) -eq $JavaMajor)) {
        $buildHome = Resolve-BuildJavaHome $JavaMajor
        if (-not $buildHome) {
            Fail-Setup "Gradle needs a JDK $JavaMajor (with javac). None found - install one, or run with -SkipBuild."
        }
        Write-Host "  JAVA_HOME -> $buildHome (was: $(if ($env:JAVA_HOME) { $env:JAVA_HOME } else { '<unset>' }))"
        $env:JAVA_HOME = $buildHome
    }
    $tasks = @($profiles.Keys | ForEach-Object { ":${_}:build" })
    $buildLog = Join-Path $OutDir 'build.log'
    $bp = Start-Process -FilePath (Join-Path $RepoRoot 'gradlew.bat') `
                        -ArgumentList (@($tasks) + @('-x', 'test', '--console=plain')) `
                        -WorkingDirectory $RepoRoot `
                        -RedirectStandardOutput $buildLog -RedirectStandardError "$buildLog.err" `
                        -PassThru -NoNewWindow -Wait
    if ($bp.ExitCode -ne 0) {
        Write-Host (Get-Content "$buildLog.err" -Raw -ErrorAction SilentlyContinue) -ForegroundColor Red
        Fail-Setup "Build failed (exit $($bp.ExitCode)). See $buildLog"
    }
    Write-Host '  ok'
}

# ---------------------------------------------------------------------------------------
# Provisioning
#
# The harness launches the game the way a player does, so there is no dependency graph to
# pull mods from: whatever is missing has to be fetched. provision.py does the downloading
# and is idempotent, so this costs one API round trip per project when everything is present.
#
# Where things land:
#   prodtest/deps|extras/<loader>/<mc>/   mod jars                 (inside the repo)
#   prodtest/servers/<loader>/<mc>/        a server install         (inside the repo)
#
# Everything is scoped by Minecraft version. A server install is built for one exact
# Minecraft and loader version, and reusing one across branches fails at mod load with
# "Missing or unsupported mandatory dependencies" - which reads like a Signpost bug and is
# not. Scoping by version also means switching branches back and forth re-downloads nothing.
#   <MinecraftHome>/versions/        a loader profile               (OUTSIDE the repo)
#
# -NoDownload turns all of this off and restores plain SKIP reporting.
# ---------------------------------------------------------------------------------------

if (-not $NoDownload) {
    Write-Head 'Provisioning mods and servers'

    foreach ($loader in @($profiles.Keys)) {
        if (-not (Test-LoaderInScope $loader)) { continue }
        $jar = Join-Path $RepoRoot "$loader\build\libs\signpost-$loader-$McVersion-$ModVersion.jar"
        $modArgs = @('mods', '--loader', $loader, '--mc', $McVersion,
                     '--deps-dir', (Join-Path $ProdRoot "deps\$loader\$McVersion"),
                     '--extras-dir', (Join-Path $ProdRoot "extras\$loader\$McVersion"))
        if (Test-Path $jar) { $modArgs += @('--jar', $jar) }
        if (-not (Invoke-Provision $modArgs "mods-$loader")) {
            $provisionFailed[$loader] = 'mod download failed'
        }

        $serverDir = Join-Path $ProdRoot "servers\$loader\$McVersion"
        if (-not (Test-Path (Join-Path $serverDir 'start.cmd'))) {
            Write-Host "  $loader : no server install, creating one"
            if (-not (Invoke-Provision @('server', '--loader', $loader, '--mc', $McVersion,
                                         '--loader-version', $LoaderVersions[$loader],
                                         '--server-dir', $serverDir, '--java', $JavaExe) "server-$loader")) {
                Write-Host "  $loader : server install failed; its server runs will SKIP" -ForegroundColor DarkYellow
            }
        }
    }
}

# ---------------------------------------------------------------------------------------
# The matrix. After provisioning, a run is only SKIPped when its pieces genuinely could not
# be obtained - never merely because they were not there to begin with.
# ---------------------------------------------------------------------------------------
$Matrix = @()
foreach ($loader in $AllLoaders) {
    if (-not $profiles.ContainsKey($loader)) { continue }
    $extras = Join-Path $ProdRoot "extras\$loader\$McVersion"
    $hasExtras = (Test-Path $extras) -and @(Get-ChildItem $extras -Filter *.jar -ErrorAction SilentlyContinue).Count -gt 0
    $serverStart = Join-Path $ProdRoot "servers\$loader\$McVersion\start.cmd"

    # After provisioning these should all be present, so say why they are not. A missing piece
    # with -NoDownload is a choice; without it, it is a failure worth naming.
    $why = if ($NoDownload) { 'not present and -NoDownload was given' } else { 'could not be provisioned' }
    if ($provisionFailed.ContainsKey($loader)) { $why = $provisionFailed[$loader] }

    $noExtras = $null
    if (-not $hasExtras) { $noExtras = "no companion mods for $loader - $why" }
    $noServer = $null
    if (-not (Test-Path $serverStart)) { $noServer = "no server install for $loader - $why" }
    $noServerMods = $noServer
    if (-not $noServerMods) { $noServerMods = $noExtras }

    $Matrix += @{ id = "$loader-client";      loader = $loader; side = 'client'; extras = $false; skip = $null }
    $Matrix += @{ id = "$loader-client-mods"; loader = $loader; side = 'client'; extras = $true;  skip = $noExtras }
    $Matrix += @{ id = "$loader-server";      loader = $loader; side = 'server'; extras = $false; skip = $noServer;     start = $serverStart }
    $Matrix += @{ id = "$loader-server-mods"; loader = $loader; side = 'server'; extras = $true;  skip = $noServerMods; start = $serverStart }
}

$runs = @($Matrix)
if ($Loaders.Count -gt 0) {
    $runs = @($runs | Where-Object { $Loaders -contains $_.loader })
}
# powershell.exe -File passes "a,b,c" as ONE string rather than an array, so split the terms.
$Only = @($Only | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
if ($Only.Count -gt 0) {
    # A term that names a run exactly matches only that run; without this "forge-client"
    # would also select "neoforge-client", since it is a substring of it.
    $allIds = $Matrix | ForEach-Object { $_.id }
    $runs = @($runs | Where-Object {
        $id = $_.id
        @($Only | Where-Object { if ($allIds -contains $_) { $id -eq $_ } else { $id -like "*$_*" } }).Count -gt 0
    })
}
if ($runs.Count -eq 0) { Fail-Setup 'No runs matched the filter.' }
Write-Host "  runs        : $($runs.Count) of $($Matrix.Count)"

# ---------------------------------------------------------------------------------------
# Run
# ---------------------------------------------------------------------------------------
$results = @()

foreach ($r in $runs) {
    Write-Head "$($r.id)"

    if ($r.skip) {
        Write-Host "  SKIP - $($r.skip)" -ForegroundColor DarkYellow
        $results += [pscustomobject]@{ Run = $r.id; Result = 'SKIP'; Detail = $r.skip; Log = '' }
        continue
    }

    $gameDir = Join-Path $OutDir $r.id
    $log     = Join-Path $OutDir "$($r.id).launcher.log"
    $gameLog = Join-Path $gameDir 'logs\latest.log'
    $verdict = 'FAIL'
    $reason  = 'did not start'
    $proc    = $null

    try {
        Stop-ProdGameJvms
        if ($r.side -eq 'server' -and -not (Wait-PortFree 25565)) {
            $results += [pscustomobject]@{ Run = $r.id; Result = 'FAIL'; Detail = 'port 25565 still in use'; Log = $log }
            Write-Host '  FAIL - port 25565 still in use' -ForegroundColor Red
            continue
        }

        # Mods: the jar under test, plus anything the loader needs to load it, plus the
        # optional companions. Names are read off the build output rather than guessed.
        $modsDir = Join-Path $gameDir 'mods'
        New-Item -ItemType Directory -Force -Path $modsDir | Out-Null

        $built = @(Get-ChildItem (Join-Path $RepoRoot "$($r.loader)\build\libs") -Filter "signpost-$($r.loader)-$McVersion-$ModVersion.jar" -ErrorAction SilentlyContinue)
        if ($built.Count -eq 0) {
            throw "no signpost-$($r.loader)-$McVersion-$ModVersion.jar in $($r.loader)/build/libs (build first, or drop -SkipBuild)"
        }
        Copy-Item $built[0].FullName $modsDir

        $deps = Join-Path $ProdRoot "deps\$($r.loader)\$McVersion"
        if (Test-Path $deps) { Get-ChildItem $deps -Filter *.jar | Copy-Item -Destination $modsDir }
        if ($r.extras) {
            Get-ChildItem (Join-Path $ProdRoot "extras\$($r.loader)\$McVersion") -Filter *.jar | Copy-Item -Destination $modsDir
        }
        Write-Host "  mods: $(@(Get-ChildItem $modsDir -Filter *.jar).Count) jar(s)"

        if ($r.side -eq 'server') {
            # A prepared server install is reused in place; only its mods and world are ours.
            $serverDir = Split-Path $r.start -Parent
            $gameDir = $serverDir
            $gameLog = Join-Path $serverDir 'logs\latest.log'
            $srvMods = Join-Path $serverDir 'mods'
            if (Test-Path $srvMods) { Remove-Item -Recurse -Force $srvMods }
            Copy-Tree $modsDir $srvMods
            Copy-Tree $PristineWorld (Join-Path $serverDir 'world')
            Set-Content -Path (Join-Path $serverDir 'eula.txt') -Value 'eula=true' -Encoding ascii
            Remove-Item -Force $gameLog -ErrorAction SilentlyContinue

            $proc = Start-Process -FilePath $r.start -WorkingDirectory $serverDir `
                                  -RedirectStandardOutput $log -RedirectStandardError "$log.err" `
                                  -PassThru -NoNewWindow
        }
        else {
            Copy-Tree $PristineWorld (Join-Path $gameDir "saves\$QuickPlayName")
            Disable-AccessibilityOnboarding $gameDir
            # Never name this $args - that is an automatic variable in PowerShell.
            # Every element is quoted below: Start-Process joins -ArgumentList with spaces and
            # quotes nothing, so an unquoted repo path containing spaces reaches python as
            # several arguments and it opens the first fragment as the script.
            $launchArgs = @(
                $Launcher,
                '--version', $profiles[$r.loader],
                '--game-dir', $gameDir,
                '--mc-home', $MinecraftHome,
                '--java', $JavaExe,
                '--quick-play-world', $QuickPlayName
            )
            $quoted = @($launchArgs | ForEach-Object { '"' + $_ + '"' })
            $proc = Start-Process -FilePath $python.Source -ArgumentList $quoted `
                                  -WorkingDirectory $RepoRoot `
                                  -RedirectStandardOutput $log -RedirectStandardError "$log.err" `
                                  -PassThru -NoNewWindow
        }
        Write-Host "  planted save from testsaves/$SaveVersion/$McVersion"

        $timeout  = if ($r.side -eq 'server') { $ServerTimeoutSec } else { $ClientTimeoutSec }
        $deadline = (Get-Date).AddSeconds($timeout)
        # Server: "Done (" means started AND finished loading the world.
        # Client: "<name> joined the game", logged by the integrated server, is the only line
        # that means the player is actually IN the world. "Loaded <n> advancements" looks
        # tempting but fires twice - the first time while the world stem's datapacks load,
        # which is BEFORE vanilla can still stop on a confirmation screen. Using it passed a
        # client that was sitting on "Worlds using Experimental Settings are not supported".
        $successRe = if ($r.side -eq 'server') { 'Done \(' } else { 'joined the game' }
        # Reached the datapack load but never joined: the signature of a modal screen waiting
        # for a human. $DatapacksLoadedRe is what tells Confirm-BlockingGameDialog to act.
        $datapacksRe = 'Loaded \d+ advancements'
        $datapacksSeenAt = $null
        $confirmAttempts = 0

        while ((Get-Date) -lt $deadline) {
            Start-Sleep -Seconds 5

            # The game's own log is the evidence; the launcher's streams only catch a crash
            # that happens before logging starts.
            $text = ''
            foreach ($f in @($gameLog, $log, "$log.err")) {
                $c = Get-Content $f -Raw -ErrorAction SilentlyContinue
                if ($c) { $text = "$text`n$c" }
            }
            if (-not $text.Trim()) {
                if ($proc.HasExited) { $verdict = 'FAIL'; $reason = 'launcher exited before the game logged anything'; break }
                continue
            }

            $clean = ($text -split "`n" | Where-Object {
                          $line = $_
                          @($BenignPatterns | Where-Object { $line -match $_ }).Count -eq 0
                      }) -join "`n"

            $dlg = Get-BlockingDialogTitle
            if ($dlg) { $verdict = 'FAIL'; $reason = "blocking error dialog: `"$dlg`""; break }

            # An in-game confirmation screen looks exactly like a slow load from out here, so
            # it is identified by its shape: datapacks loaded, world never entered. Give it a
            # grace period first, then confirm, and only try a bounded number of times so a
            # genuinely stuck client still fails instead of being typed at forever.
            if ($r.side -eq 'client' -and $text -match $datapacksRe -and $confirmAttempts -lt 3) {
                if (-not $datapacksSeenAt) { $datapacksSeenAt = Get-Date }
                elseif (((Get-Date) - $datapacksSeenAt).TotalSeconds -gt 20) {
                    if (Confirm-BlockingGameDialog $gameLog) { $confirmAttempts++ }
                    $datapacksSeenAt = Get-Date
                }
            }

            $hit = $FailPatterns | Where-Object { $clean -match $_ } | Select-Object -First 1
            if ($hit) {
                $verdict = 'FAIL'
                $first = ($clean -split "`n" | Select-String -Pattern $hit | Select-Object -First 1)
                $reason = "matched [$hit]: $(($first -replace '\s+', ' ').ToString().Trim())"
                if ($reason.Length -gt 240) { $reason = $reason.Substring(0, 240) + '...' }
                break
            }
            if ($text -match $successRe) {
                # The success marker is not the end of the log. Signpost logs its own
                # world-load failures in the same second the advancements land, and whichever
                # of the two a poll happens to read first would otherwise decide the verdict.
                # Let the log settle, then re-check the failure patterns before passing.
                Start-Sleep -Seconds $SettleSec
                $settled = ''
                foreach ($f in @($gameLog, $log, "$log.err")) {
                    $c = Get-Content $f -Raw -ErrorAction SilentlyContinue
                    if ($c) { $settled = "$settled`n$c" }
                }
                $settledClean = ($settled -split "`n" | Where-Object {
                                     $line = $_
                                     @($BenignPatterns | Where-Object { $line -match $_ }).Count -eq 0
                                 }) -join "`n"
                $lateHit = $FailPatterns | Where-Object { $settledClean -match $_ } | Select-Object -First 1
                if ($lateHit) {
                    $verdict = 'FAIL'
                    $first = ($settledClean -split "`n" | Select-String -Pattern $lateHit | Select-Object -First 1)
                    $reason = "loaded the save, then matched [$lateHit]: $(($first -replace '\s+', ' ').ToString().Trim())"
                    if ($reason.Length -gt 240) { $reason = $reason.Substring(0, 240) + '...' }
                    break
                }
                $verdict = 'PASS'; $reason = 'started and loaded the save'
                break
            }
            if ($proc.HasExited)         { $verdict = 'FAIL'; $reason = 'process exited before loading the save'; break }
        }
        if ($verdict -eq 'FAIL' -and $reason -eq 'did not start') { $reason = "timed out after ${timeout}s" }

        # Keep the game's log next to the launcher log; the game dir may be reused or cleaned.
        if (Test-Path $gameLog) { Copy-Item $gameLog (Join-Path $OutDir "$($r.id).game.log") -Force }
    }
    catch {
        $verdict = 'FAIL'
        $reason  = $_.Exception.Message
    }
    finally {
        if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue }
        Stop-ProdGameJvms
    }

    $color = if ($verdict -eq 'PASS') { 'Green' } else { 'Red' }
    Write-Host "  $verdict - $reason" -ForegroundColor $color
    $results += [pscustomobject]@{ Run = $r.id; Result = $verdict; Detail = $reason; Log = $log }
}

Write-Head 'Summary'
$table = $results | Format-Table -AutoSize Run, Result, Detail | Out-String
Write-Host $table
Set-Content -Path (Join-Path $OutDir 'summary.txt') -Value $table -Encoding utf8

$failed  = @($results | Where-Object { $_.Result -eq 'FAIL' })
$skipped = @($results | Where-Object { $_.Result -eq 'SKIP' })
$passed  = @($results | Where-Object { $_.Result -eq 'PASS' })
Write-Host "Logs: $OutDir"
if ($missingLoaders.Count -gt 0) {
    Write-Host "Loaders not installed for ${McVersion}: $($missingLoaders -join ', ') - those runs did not happen." -ForegroundColor DarkYellow
}
if ($skipped.Count -gt 0) {
    Write-Host "$($skipped.Count) run(s) SKIPPED - not verified:" -ForegroundColor DarkYellow
    foreach ($s in $skipped) { Write-Host "  $($s.Run): $($s.Detail)" -ForegroundColor DarkYellow }
}
if ($failed.Count -gt 0) {
    Write-Host "$($failed.Count) of $($results.Count) runs FAILED." -ForegroundColor Red
    foreach ($f in $failed) { Write-Host "  $($f.Run): $($f.Detail)  ->  $($f.Log)" -ForegroundColor Red }
    exit 1
}
Write-Host "$($passed.Count) run(s) passed, $($skipped.Count) skipped." -ForegroundColor Green
exit 0
