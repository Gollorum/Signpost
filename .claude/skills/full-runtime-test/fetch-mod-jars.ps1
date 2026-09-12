<#
.SYNOPSIS
  Populate the *WithMods run folders from a persistent, SHA-1 verified jar cache.

.DESCRIPTION
  The run folders are gitignored, so every fresh clone, every new worktree and every Minecraft
  upgrade starts with empty mods/ directories - and a *WithMods run with an empty mods/ folder
  does not fail, it just silently becomes a second copy of the plain run. Half the matrix then
  proves nothing while still reporting PASS.

  This downloads what mod-jars.json says each folder needs into

      <repo>/../mod-jar-cache/<minecraft_version>/

  and copies it into place. The cache sits BESIDE the repo on purpose: it survives `git clean`,
  branch switches and worktree deletion, and one download serves every checkout on the machine.
  Jars already cached with the right SHA-1 are not re-downloaded.

  Nothing here is required by run-full-runtime-test.ps1 - it is the way to get the six *WithMods
  runs into a state worth testing before you invoke it.

.EXAMPLE
  # See what is available for the current minecraft_version without downloading anything
  powershell -NoProfile -ExecutionPolicy Bypass -File .claude\skills\full-runtime-test\fetch-mod-jars.ps1 -List

.EXAMPLE
  # Fetch, verify and install the newest release of each
  powershell -NoProfile -ExecutionPolicy Bypass -File .claude\skills\full-runtime-test\fetch-mod-jars.ps1

.EXAMPLE
  # Hold one project back - see the Sodium/Iris note below
  ... fetch-mod-jars.ps1 -Pin sodium=mc1.21.11-0.8.12-fabric
#>
[CmdletBinding()]
param(
    # Defaults to minecraft_version from gradle.properties.
    [string] $McVersion,
    # Hold a project at an exact Modrinth version_number: -Pin sodium=mc1.21.11-0.8.12-fabric
    # Repeatable. Use this whenever "newest of everything" does not resolve.
    [string[]] $Pin = @(),
    # Show candidate versions and exit. No downloads, no writes.
    [switch] $List,
    # Download and verify into the cache, but leave the run folders alone.
    [switch] $NoInstall,
    # Include pre-releases when picking "newest". Off by default.
    [switch] $AllowBeta
)

$ErrorActionPreference = 'Stop'
$RepoRoot  = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$Manifest  = Get-Content (Join-Path $PSScriptRoot 'mod-jars.json') -Raw | ConvertFrom-Json
$Headers   = @{ 'User-Agent' = 'Gollorum/signpost-runtime-test (+https://modrinth.com/mod/signpost)' }

if (-not $McVersion) {
    $line = Select-String -Path (Join-Path $RepoRoot 'gradle.properties') -Pattern '^minecraft_version=(.+)$'
    if (-not $line) { throw 'minecraft_version not found in gradle.properties' }
    $McVersion = $line.Matches[0].Groups[1].Value.Trim()
}
$CacheDir = Join-Path (Split-Path -Parent $RepoRoot) "mod-jar-cache\$McVersion"

$pins = @{}
foreach ($p in ($Pin | ForEach-Object { $_ -split ',' })) {
    $kv = $p -split '=', 2
    if ($kv.Count -eq 2) { $pins[$kv[0].Trim()] = $kv[1].Trim() }
}

function Get-VersionsFor([string] $slug, [string] $loader, [string] $gameVersion) {
    # PowerShell 5.1 collapses a JSON array of objects into ONE object whose properties are
    # parallel arrays, so `$resp | Where-Object { $_.version_number -eq $x }` matches the whole
    # collection and `$_.files | Select -First 1` then hands back the NEWEST file regardless of
    # what was asked for. That silently downloads the wrong version - it cost us a Sodium that
    # was incompatible with the pinned Iris, caught only by reading the filename.
    # Reading the parallel arrays by index is correct whether or not the collapse happened.
    $u = 'https://api.modrinth.com/v2/project/{0}/version?game_versions=%5B%22{1}%22%5D&loaders=%5B%22{2}%22%5D' -f $slug, $gameVersion, $loader
    $resp  = Invoke-RestMethod -Uri $u -Headers $Headers
    $ids   = @($resp.id); $nums = @($resp.version_number); $types = @($resp.version_type)
    $out = @()
    for ($i = 0; $i -lt $ids.Count; $i++) {
        # An EMPTY response is the same trap wearing a different hat: `@($resp.id)` on a `[]`
        # body yields one $null element, so a naive .Count says 1 and "no builds published"
        # reads as "one build published" - which suppressed the fallback below and dropped
        # Repurposed Structures from the run folders entirely.
        if (-not $ids[$i]) { continue }
        $out += [pscustomobject]@{ id = $ids[$i]; number = $nums[$i]; type = $types[$i] }
    }
    return $out
}

# Projects do not agree on how precisely they tag a Minecraft version. On 26.1.2 most publish
# against "26.1.2", but Repurposed Structures tags its builds "26.1" - querying only the exact
# version reports it as unpublished and quietly drops it from the run folders. So try the exact
# version first and fall back to the <major>.<minor> prefix, announcing it when that happens:
# a looser match is a thing to eyeball, not to apply silently.
$script:LooseMatched = @{}
function Get-Versions([string] $slug, [string] $loader) {
    $vs = @(Get-VersionsFor $slug $loader $McVersion)
    if ($vs.Count -gt 0) { return $vs }
    $parts = $McVersion -split '\.'
    if ($parts.Count -lt 3) { return @() }
    $short = "$($parts[0]).$($parts[1])"
    $vs = @(Get-VersionsFor $slug $loader $short)
    if ($vs.Count -gt 0) { $script:LooseMatched["$slug|$loader"] = $short }
    return $vs
}

function Get-JarMeta([string] $path) {
    # depends/breaks live in fabric.mod.json; NeoForge and Forge use TOML we do not parse here.
    Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
    $zip = [System.IO.Compression.ZipFile]::OpenRead($path)
    try {
        $e = $zip.Entries | Where-Object { $_.FullName -eq 'fabric.mod.json' } | Select-Object -First 1
        if (-not $e) { return $null }
        $sr = New-Object System.IO.StreamReader($e.Open())
        try { return $sr.ReadToEnd() | ConvertFrom-Json } finally { $sr.Dispose() }
    } finally { $zip.Dispose() }
}

# Which (project, loader) pairs are actually needed, derived from the folder layout.
$needed = @{}
foreach ($f in $Manifest.folders) {
    foreach ($k in $f.projects) { $needed["$k|$($f.loader)"] = $true }
}

Write-Host ''
Write-Host "=== mod jars for Minecraft $McVersion" -ForegroundColor Cyan
Write-Host "  repo  : $RepoRoot"
Write-Host "  cache : $CacheDir"

if ($List) {
    foreach ($key in ($needed.Keys | Sort-Object)) {
        $k, $loader = $key -split '\|'
        $slug = $Manifest.projects.$k.slug
        $vs = @(Get-Versions $slug $loader | Where-Object { $AllowBeta -or $_.type -eq 'release' })
        if (-not $vs.Count) { Write-Host ("  {0,-13} {1,-9} (nothing published for {2})" -f $k, $loader, $McVersion) -ForegroundColor DarkYellow; continue }
        foreach ($v in ($vs | Select-Object -First 3)) {
            Write-Host ("  {0,-13} {1,-9} {2,-36} {3}" -f $k, $loader, $v.number, $v.type)
        }
    }
    exit 0
}

New-Item -ItemType Directory -Force -Path $CacheDir | Out-Null
$resolved = @{}
$missing  = @()

foreach ($key in ($needed.Keys | Sort-Object)) {
    $k, $loader = $key -split '\|'
    $slug = $Manifest.projects.$k.slug
    $vs = @(Get-Versions $slug $loader)
    $pick = $null
    if ($pins.ContainsKey($k)) {
        $pick = $vs | Where-Object { $_.number -eq $pins[$k] } | Select-Object -First 1
        if (-not $pick) { throw "pin $k=$($pins[$k]) matches no $loader version for $McVersion" }
    } else {
        $pick = $vs | Where-Object { $AllowBeta -or $_.type -eq 'release' } | Select-Object -First 1
    }
    if (-not $pick) {
        Write-Host ("  {0,-13} {1,-9} NOT PUBLISHED for {2}" -f $k, $loader, $McVersion) -ForegroundColor DarkYellow
        $missing += "$k ($loader)"
        continue
    }
    # Fetch the single version by id: one object, so no array-collapse ambiguity about its files.
    $ver  = Invoke-RestMethod -Uri "https://api.modrinth.com/v2/version/$($pick.id)" -Headers $Headers
    $file = @($ver.files | Where-Object { $_.primary })[0]
    if (-not $file) { $file = @($ver.files)[0] }

    $dest = Join-Path $CacheDir $file.filename
    $have = (Test-Path $dest) -and ((Get-FileHash -Algorithm SHA1 $dest).Hash.ToLower() -eq $file.hashes.sha1)
    if (-not $have) { Invoke-WebRequest -Uri $file.url -Headers $Headers -OutFile $dest }

    $got = (Get-FileHash -Algorithm SHA1 $dest).Hash.ToLower()
    if ($got -ne $file.hashes.sha1) { throw "SHA-1 mismatch for $($file.filename): expected $($file.hashes.sha1), got $got" }

    $resolved["$k|$loader"] = $file.filename
    $note = if ($pins.ContainsKey($k)) { 'PINNED' } elseif ($have) { 'cached' } else { 'downloaded' }
    $loose = $script:LooseMatched["$slug|$loader"]
    if ($loose) { $note += " (tagged $loose, not $McVersion)" }
    Write-Host ("  {0,-13} {1,-9} {2,-46} {3}" -f $k, $loader, $file.filename, $note)
}

# Read back what the jars themselves declare. AGENTS.md is explicit that `depends` is not enough
# and `breaks` is where the Sodium/Iris incompatibility hides - so print it rather than trusting
# a table that was written for the previous Minecraft version.
Write-Host ''
Write-Host '=== declared depends / breaks (fabric jars)' -ForegroundColor Cyan
foreach ($key in ($resolved.Keys | Sort-Object)) {
    if (-not $key.EndsWith('|fabric')) { continue }
    $meta = Get-JarMeta (Join-Path $CacheDir $resolved[$key])
    if (-not $meta) { continue }
    Write-Host ("  {0} {1}" -f $meta.id, $meta.version)
    foreach ($field in 'depends', 'breaks') {
        if (-not $meta.$field) { continue }
        foreach ($p in $meta.$field.PSObject.Properties) {
            if ($p.Name -eq 'minecraft' -or $p.Name -eq 'java') { continue }
            Write-Host ("      {0,-7} {1} {2}" -f $field, $p.Name, ($p.Value -join ' || '))
        }
    }
}
Write-Host '  ^ check every `breaks` line against what is installed above before trusting a run.' -ForegroundColor DarkYellow

if ($NoInstall) { Write-Host ''; Write-Host 'cache populated; run folders left untouched (-NoInstall).'; exit 0 }

Write-Host ''
Write-Host '=== installing into run folders' -ForegroundColor Cyan
foreach ($f in $Manifest.folders) {
    $dir = Join-Path $RepoRoot ($f.dir -replace '/', '\')
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    # Clear first: a jar left over from the previous Minecraft version is worse than none,
    # because the loader may still accept it and the run then tests the wrong thing.
    Get-ChildItem $dir -Filter *.jar -ErrorAction SilentlyContinue | Remove-Item -Force
    $n = 0
    foreach ($k in $f.projects) {
        $name = $resolved["$k|$($f.loader)"]
        if (-not $name) { continue }
        Copy-Item (Join-Path $CacheDir $name) (Join-Path $dir $name) -Force
        $n++
    }
    Write-Host ("  {0,-42} {1} jars" -f $f.dir, $n)
}

if ($missing.Count -gt 0) {
    Write-Host ''
    Write-Host "No build published for $McVersion : $($missing -join ', ')" -ForegroundColor DarkYellow
    Write-Host 'Say so explicitly in the report - that loader loses that coverage until it ships.' -ForegroundColor DarkYellow
}
Write-Host ''
Write-Host 'Done. The plain client/server folders are deliberately left empty.' -ForegroundColor Green
