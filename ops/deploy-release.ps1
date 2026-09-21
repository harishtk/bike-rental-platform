#Requires -Version 7.0

<#
.SYNOPSIS
Deploys a release manifest into the local bike-release-lab environment.

.EXAMPLE
pwsh -File ./ops/deploy-release.ps1 `
    -ManifestPath build/releases/release-b/release-manifest.json

.NOTES
Relative paths resolve from the repository root.
Requires Docker and prior GHCR authentication for private images.
Preserves database volumes. Does not automatically roll back on failure.
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$ManifestPath,

    [string]$ComposePath = 'ops/compose.release.yaml'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

$repoRoot = Split-Path -Parent $PSScriptRoot
$project = 'bike-release-lab'
$runId = '{0}-{1}' -f `
    [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'), `
    ([guid]::NewGuid().ToString('N').Substring(0, 8))

$runDirectory = Join-Path $repoRoot "build/deployments/$runId"
$composeArgs = $null
$exitCode = 1

function Invoke-DockerChecked {
    param([string[]]$Arguments)

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($Arguments -join ' ')"
    }
}

try {
    New-Item -ItemType Directory -Path $runDirectory -Force |
            Out-Null

    $manifestFile = [IO.Path]::GetFullPath($ManifestPath, $repoRoot)
    $composeFile = [IO.Path]::GetFullPath($ComposePath, $repoRoot)

    # Preserve the inputs used for this particular deployment.
    $savedManifest = Join-Path $runDirectory 'release-manifest.json'
    $savedCompose = Join-Path $runDirectory 'compose.base.yaml'
    $overrideFile = Join-Path $runDirectory 'compose.images.json'
    $resolvedFile = Join-Path $runDirectory 'compose.resolved.json'

    Copy-Item -LiteralPath $manifestFile -Destination $savedManifest
    Copy-Item -LiteralPath $composeFile -Destination $savedCompose

    # Run in a child process because prepare-release.ps1 uses exit.
    & pwsh -NoProfile -File "$PSScriptRoot/prepare-release.ps1" `
        -ManifestPath $savedManifest `
        -OutputPath $overrideFile

    if ($LASTEXITCODE -ne 0) {
        throw 'Manifest validation failed.'
    }

    # Resolve relative paths against the original Compose directory.
    $renderArgs = @(
        'compose'
        '--project-directory', (Split-Path -Parent $composeFile)
        '-p', $project
        '-f', $savedCompose
        '-f', $overrideFile
        'config', '--format', 'json'
    )

    $resolvedJson = & docker @renderArgs
    if ($LASTEXITCODE -ne 0) {
        throw 'Compose configuration validation failed.'
    }

    $resolvedJson |
            Set-Content -LiteralPath $resolvedFile -Encoding utf8

    $configuration = $resolvedJson |
            ConvertFrom-Json -AsHashtable

    # Require every service, including databases and Kafka, to use a digest.
    foreach ($service in $configuration.services.Keys) {
        $image = $configuration.services[$service].image

        if ($image -cnotmatch '^.+@sha256:[a-f0-9]{64}$') {
            throw "Service $service does not use a digest-pinned image."
        }
    }

    $composeArgs = @(
        'compose'
        '-p', $project
        '-f', $resolvedFile
    )

    Invoke-DockerChecked -Arguments ($composeArgs + @('pull'))

    Invoke-DockerChecked -Arguments (
    $composeArgs + @(
        'up', '-d', '--no-build', '--wait',
        '--wait-timeout', '300'
    )
    )

    # Verify every configured service against the resolved deployment input.
    foreach ($service in $configuration.services.Keys) {
        $containerIds = @(
            & docker @composeArgs ps -q $service
        )

        if ($LASTEXITCODE -ne 0 -or $containerIds.Count -ne 1) {
            throw "Expected one running container for $service."
        }

        $containerJson = & docker inspect $containerIds[0]
        if ($LASTEXITCODE -ne 0) {
            throw "Cannot inspect $service."
        }

        $container = @($containerJson | ConvertFrom-Json)[0]
        $expectedImage = $configuration.services[$service].image

        if ($container.Config.Image -cne $expectedImage) {
            throw "Image reference mismatch for $service."
        }

        if (
        -not $container.State.Running -or
                $container.State.Health.Status -ne 'healthy'
        ) {
            throw "Service $service is not healthy."
        }

        Write-Host "Verified $service"
    }

    $binding = & docker @composeArgs port api-gateway 8080
    if ($LASTEXITCODE -ne 0 -or -not $binding) {
        throw 'Cannot discover the gateway address.'
    }

    $manifest = Get-Content -LiteralPath $savedManifest -Raw |
            ConvertFrom-Json

    $record = [ordered]@{
        project = $project
        commit = $manifest.commit
        deployedAt = [DateTime]::UtcNow.ToString('o')
        gatewayUrl = "http://$($binding.Trim())"
        manifest = $savedManifest
        compose = $resolvedFile
    }

    $recordJson = $record | ConvertTo-Json -Depth 5

    $recordJson |
            Set-Content `
            -LiteralPath (Join-Path $runDirectory 'deployment.json') `
            -Encoding utf8

    # Only update this pointer after deployment and verification succeed.
    $recordJson |
            Set-Content `
            -LiteralPath (Join-Path $repoRoot 'build/deployments/current.json') `
            -Encoding utf8

    Write-Host "`nDeployment verified."
    Write-Host "Gateway: $($record.gatewayUrl)"
    $exitCode = 0
}
catch {
    [Console]::Error.WriteLine(
            "Deployment failed: $($_.Exception.Message)"
    )

    if (Test-Path -LiteralPath $runDirectory) {
        $_.Exception.Message |
                Set-Content `
                -LiteralPath (Join-Path $runDirectory 'failure.txt') `
                -Encoding utf8
    }
}
finally {
    # Diagnostics must not replace the deployment's original exit status.
    if ($null -ne $composeArgs) {
        try {
            & docker @composeArgs ps --all 2>&1 |
                    Out-File (Join-Path $runDirectory 'containers.txt')

            & docker @composeArgs logs --no-color --tail 200 2>&1 |
                    Out-File (Join-Path $runDirectory 'compose.log')
        }
        catch {
            Write-Warning "Could not save all diagnostics: $_"
        }
    }

    Write-Host "Deployment records: $runDirectory"
}

exit $exitCode