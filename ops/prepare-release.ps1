#Requires -Version 7.0

<#
.SYNOPSIS
Validates a release manifest and generates a Compose image override.
.EXAMPLE
pwsh -File ./ops/prepare-release.ps1
.EXAMPLE
pwsh -File ./ops/prepare-release.ps1 -ManifestPath ./build/release/release-manifest.json -OutputPath ./build/release/compose.images.json
.NOTES
Relative paths are resolved from the repository root. Does not pull or start images.
#>
[CmdletBinding()]
param(
    [string]$ManifestPath = 'build/release/release-manifest.json',
    [string]$OutputPath = 'build/release/compose.images.json'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

try {
    $manifestFile = [IO.Path]::GetFullPath($ManifestPath, $repoRoot)
    $outputFile = [IO.Path]::GetFullPath($OutputPath, $repoRoot)

    if ($manifestFile -eq $outputFile) {
        throw 'The output path must differ from the manifest path.'
    }

    $manifest = Get-Content -LiteralPath $manifestFile -Raw | ConvertFrom-Json
    if ($manifest.schemaVersion -ne 1) {
        throw 'Unsupported manifest schema version; expected 1.'
    }

    if ($manifest.repository -notmatch '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$') {
        throw 'The manifest repository must have the form owner/repository.'
    }

    $expectedServices = @(
        'auth-service', 'bike-service', 'reservation-service',
        'rental-service', 'discovery-service', 'api-gateway'
    )
    $images = @($manifest.images)
    $actualServices = @($images | ForEach-Object { $_.service })
    if (
        $images.Count -ne 6 -or
        @($actualServices | Select-Object -Unique).Count -ne 6 -or
        @(Compare-Object $expectedServices $actualServices).Count -ne 0
    ) {
        throw 'The manifest must contain exactly the six expected services.'
    }

    $composeServices = [ordered]@{}
    foreach ($entry in $images) {
        $prefix = "ghcr.io/$($manifest.repository.ToLowerInvariant())/$($entry.service)"
        $pattern = '^' + [regex]::Escape($prefix) + '@sha256:[a-f0-9]{64}$'
        if ($entry.image -cnotmatch $pattern) {
            throw "Invalid digest-pinned image reference for $($entry.service)."
        }

        # Override CI's local images and its pull_policy: never.
        $composeServices[$entry.service] = [ordered]@{
            image = $entry.image
            pull_policy = 'missing'
        }
    }

    # Validate the complete input before creating or replacing the output.
    $json = @{ services = $composeServices } | ConvertTo-Json -Depth 5
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($outputFile)) | Out-Null
    Set-Content -LiteralPath $outputFile -Value $json -Encoding utf8
    Write-Host "Compose image override: $outputFile"
    exit 0
}
catch {
    [Console]::Error.WriteLine("Release preparation failed: $($_.Exception.Message)")
    exit 1
}
