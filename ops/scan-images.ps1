#Requires -Version 7.0

<#
.SYNOPSIS
Scans local application images for HIGH/CRITICAL vulnerabilities.

.EXAMPLE
pwsh -File ./ops/scan-images.ps1 -ImageTag local
#>

[CmdletBinding()]
param(
    [ValidatePattern('^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$')]
    [string]$ImageTag = 'local'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Handle native exit codes explicitly so every image gets scanned.
$PSNativeCommandUseErrorActionPreference = $false

$trivyImage = 'aquasec/trivy:0.74.0'
$repoRoot = Split-Path -Parent $PSScriptRoot
$runId = '{0}-{1}-{2}' -f `
    [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'), `
    $PID, `
    ([guid]::NewGuid().ToString('N').Substring(0, 8))

$reportDirectory = Join-Path $repoRoot "build/security/$runId"

$services = @(
    'auth-service'
    'bike-service'
    'reservation-service'
    'rental-service'
    'discovery-service'
    'api-gateway'
)

try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker is required.'
    }

    $dockerOsOutput = & docker info --format '{{.OSType}}'
    $dockerExit = $LASTEXITCODE

    if ($dockerExit -ne 0) {
        throw 'Cannot connect to Docker.'
    }

    $dockerOs = ($dockerOsOutput -join '').Trim()

    if ($dockerOs -ne 'linux') {
        throw 'Docker must be running Linux containers.'
    }

    New-Item -ItemType Directory -Path $reportDirectory -Force |
            Out-Null

    $summaryFile = Join-Path $reportDirectory 'summary.txt'

    @(
        "Image tag: $ImageTag"
        "Scanner: $trivyImage"
        ''
    ) | Set-Content -LiteralPath $summaryFile -Encoding utf8

    $failed = $false

    foreach ($service in $services) {
        $image = "bike-rental/${service}:$ImageTag"
        $logFile = Join-Path $reportDirectory "$service.log"

        Write-Host "`n=== Scanning $image ==="

        & docker image inspect $image *> $null
        $inspectExit = $LASTEXITCODE

        # Require the existing local image; do not pull a substitute.
        if ($inspectExit -ne 0) {
            "Missing local image: $image" |
                    Tee-Object -FilePath $logFile |
                    Out-Host

            "FAIL ${image}: image missing" |
                    Add-Content -LiteralPath $summaryFile

            $failed = $true
            continue
        }

        $dockerArguments = @(
            'run'
            '--rm'
            '--mount'
            'type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock'
            '--mount'
            'type=volume,source=bike-trivy-cache,target=/root/.cache/trivy'
            '--mount'
            "type=bind,source=$reportDirectory,target=/reports"
            $trivyImage
            'image'
            '--image-src'
            'docker'
            '--scanners'
            'vuln'
            '--severity'
            'HIGH,CRITICAL'
            '--exit-code'
            '1'
            '--timeout'
            '15m'
            '--no-progress'
            '--format'
            'json'
            '--output'
            "/reports/$service.json"
            $image
        )

        # Save diagnostics while displaying them in the terminal.
        & docker @dockerArguments 2>&1 |
                Tee-Object -FilePath $logFile |
                Out-Host

        $scanExit = $LASTEXITCODE

        if ($scanExit -eq 0) {
            $result = "PASS $image"
        }
        else {
            $result = "FAIL ${image}: exit $scanExit; inspect JSON and log"
            $failed = $true
        }

        Write-Host $result
        Add-Content -LiteralPath $summaryFile -Value $result
    }

    Write-Host "`nReports: $reportDirectory"

    if ($failed) {
        [Console]::Error.WriteLine(
                'Image scanning failed. See summary.txt and individual reports.'
        )
        exit 1
    }

    Write-Host 'All six images passed the HIGH/CRITICAL vulnerability policy.'
    exit 0
}
catch {
    [Console]::Error.WriteLine("Image scanning failed: $($_.Exception.Message)")
    [Console]::Error.WriteLine("Report location: $reportDirectory")
    exit 1
}