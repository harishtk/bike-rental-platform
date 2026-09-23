# Local pipeline: pwsh -File ./ops/pipeline.ps1 -Stage all -ImageTag local
# See README.md for prerequisites, artifacts, and the failure-cleanup exercise.
param(
    [Parameter(Position = 0)]
    [ValidateSet("verify", "images", "e2e", "all")]
    [string]$Stage = "verify",

    [ValidatePattern('^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$')]
    [string]$ImageTag = "local"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Resolve paths relative to this file, not the caller's working directory.
$repoRoot = Split-Path -Parent $PSScriptRoot

$services = @(
    "services/auth-service"
    "services/bike-service"
    "services/reservation-service"
    "services/rental-service"
    "platform/discovery-service"
    "platform/api-gateway"
)

function Invoke-ServiceVerification
{
    param(
        [Parameter(Mandatory)]
        [string]$ServicePath
    )

    $serviceDirectory = Join-Path $repoRoot $ServicePath

    $wrapperName = if ($IsWindows) {
        "gradlew.bat"
    } else {
        "gradlew"
    }

    $wrapperPath = Join-Path $serviceDirectory $wrapperName

    if (-not (Test-Path -LiteralPath $wrapperPath)) {
        throw "Gradle wrapper not found: $wrapperPath"
    }

    Write-Host ""
    Write-Host "=== Verifying $ServicePath ===" -ForegroundColor Cyan

    # Always restore the caller's directory, even when Gradle fails.
    Push-Location $serviceDirectory
    try {
        if ($IsWindows) {
            & $wrapperPath check bootJar --console=plain --no-daemon
        } else {
            & bash $wrapperPath check bootJar --console=plain --no-daemon
        }

        # Native tools report failure through their exit code, not a PS exception.
        if ($LASTEXITCODE -ne 0) {
            throw "Verification failed for $ServicePath (exit code $LASTEXITCODE)."
        }
    }
    finally {
        Pop-Location
    }
}

function Invoke-ServiceImageBuild {
    param(
        [Parameter(Mandatory)]
        [string]$ServicePath,

        [Parameter(Mandatory)]
        [string]$Tag
    )

    $serviceDirectory = Join-Path $repoRoot $ServicePath
    $serviceName = Split-Path -Leaf $ServicePath
    $dockerfile = Join-Path $serviceDirectory "Dockerfile"
    $image = "bike-rental/${serviceName}:${Tag}"

    if (-not (Test-Path -LiteralPath $dockerfile)) {
        throw "Dockerfile not found: $dockerfile"
    }

    Write-Host ""
    Write-Host "=== Building $image ===" -ForegroundColor Cyan

    # Build only; verification and registry publication are separate concerns.
    & docker build `
        --file $dockerfile `
        --tag $image `
        $serviceDirectory

    if ($LASTEXITCODE -ne 0) {
        throw "Image build failed for $serviceName (exit code $LASTEXITCODE)."
    }

    Write-Host "Built $image" -ForegroundColor Green
}

function Invoke-EndToEndTests {
    param(
        [Parameter(Mandatory)]
        [string]$Tag
    )

    # Project-scoped names isolate containers, networks, and disposable volumes.
    $project = "bike-ci-" + [guid]::NewGuid().ToString("N").Substring(0, 12)
    $composeFile = Join-Path $repoRoot "ops\compose.ci.yaml"
    $artifactDirectory = Join-Path $repoRoot "build\pipeline\$project"

    New-Item -ItemType Directory -Force -Path $artifactDirectory |
        Out-Null

    # Use identical project/file arguments for startup, diagnostics, and cleanup.
    $composeArgs = @(
        "compose", "-p", $project, "-f", $composeFile
    )

    # Local runs may use the ignored root .env; CI supplies process environment values.
    $serviceEnvFile = Join-Path $repoRoot ".env"
    if (Test-Path -LiteralPath $serviceEnvFile -PathType Leaf) {
        $composeArgs += @("--env-file", $serviceEnvFile)
    }

    # Save caller settings and restore them even if startup or tests fail.
    $previousImageTag = $env:IMAGE_TAG
    $previousGatewayUrl = $env:API_GATEWAY_URL
    $cleanupNeeded = $false
    $failure = $null

    try
    {
        $env:IMAGE_TAG = $Tag

        & docker @composeArgs config --quiet
        if ($LASTEXITCODE -ne 0)
        {
            throw "CI Compose configuration is invalid."
        }

        Write-Host "Starting E2E stack: $project" -ForegroundColor Cyan

        # Cleanup is needed even if startup only partially succeeds.
        $cleanupNeeded = $true

        & docker @composeArgs up --detach --no-build --wait --wait-timeout 300
        if ($LASTEXITCODE -ne 0)
        {
            throw "E2E stack failed to become healthy."
        }

        # Discover the assigned loopback port instead of assuming host port 8080.
        $binding = & docker @composeArgs port api-gateway 8080
        if ($LASTEXITCODE -ne 0)
        {
            throw "Could not discover the gateway port."
        }

        $binding = ($binding | Out-String).Trim()

        if ($binding -notmatch '^127\.0\.0\.1:(\d+)$')
        {
            throw "Unexpected gateway port binding: $binding"
        }

        $gatewayurl = "http://127.0.0.1:$($Matches[1])"
        $env:API_GATEWAY_URL = $gatewayurl

        Write-Host "Gateway: $gatewayurl"

        # A disposable account used only to check authenticated routes.
        # A fresh username per attempt avoids conflicts after a timeout.
        $registration = Wait-ForEndpoint `
            -Description "gateway-to-auth routing" `
            -Probe {
                $body = @{
                    username = "probe-" + [guid]::NewGuid().ToString("N")
                    password = "Readiness-Test-123!"
                } | ConvertTo-Json

                $result = Invoke-RestMethod `
                        -Uri "$gatewayUrl/api/v1/auth/register" `
                        -Method Post `
                        -ContentType "application/json" `
                        -Body $body `
                        -TimeoutSec 5

                if ( [string]::IsNullOrWhiteSpace($result.accessToken))
                {
                    throw "Registration returned no access token."
                }

                $result
            }

        $headers = @{
            Authorization = "Bearer $($registration.accessToken)"
        }

        # OPTIONS checks rental routing without creating a rental.
        $routes = @(
            @{ Path = "/api/v1/stations"; Method = "Get" }
            @{ Path = "/api/v1/reservations"; Method = "Get" }
            @{ Path = "/api/v1/rentals"; Method = "Options" }
        )

        foreach ($route in $routes) {
            Wait-ForEndpoint `
                -Description "$($route.Method) $($route.Path)" `
                -Probe {
                $response = Invoke-WebRequest `
                        -Uri "$gatewayUrl$($route.Path)" `
                        -Method $route.Method `
                        -Headers $headers `
                        -TimeoutSec 5 `

                if ($response.StatusCode -ne 200) {
                    throw "Unexpected status: $($response.StatusCode)"
                }
            }
        }

        Write-Host "Gateway routes are ready. Running E2E tests." `
            -ForegroundColor Cyan

        # Force execution against fresh data; never retry failed test assertions.
        # Gradle reports remain in e2e-tests/build rather than the run log directory.
        Push-Location (Join-Path $repoRoot "e2e-tests")
        try {
            if ($IsWindows) {
                & .\gradlew.bat test --rerun-tasks --console=plain --no-daemon
            } else {
                & ./gradlew test --rerun-tasks --console=plain --no-daemon
            }

            if ($LASTEXITCODE -ne 0) {
                throw "E2E tests failed."
            }
        }
        finally
        {
            Pop-Location
        }
    }
    catch {
        # Retain the original error while the finally block collects diagnostics.
        $failure = $_
    }
    finally {
        try
        {
            if ($cleanupNeeded) {
                # Preserve diagnostics before deleting the test stack.
                try {
                    & docker @composeArgs logs --no-color 2>&1 |
                        Out-File (Join-Path $artifactDirectory "compose.log") `
                            -Encoding utf8

                    & docker @composeArgs ps --all 2>&1 |
                        Out-File (Join-Path $artifactDirectory "containers.txt") `
                            -Encoding utf8
                }
                catch {
                    Write-Warning "Could not collect all diagnostics: $_"
                }
                finally {
                    # Remove only this run's resources, including its test databases.
                    # Abrupt process/host termination can still require manual cleanup.
                    & docker @composeArgs down --volumes --remove-orphans

                    if ($LASTEXITCODE -ne 0) {
                        if ($null -eq $failure) {
                            $failure = "Cleanup failed for compose project $project."
                        }
                        else {
                            Write-Warning "Cleanup also failed for $project."
                        }
                    }
                }
            }
        }
        finally {
            $env:IMAGE_TAG = $previousImageTag
            $env:API_GATEWAY_URL = $previousGatewayUrl
        }

        Write-Host "Pipeline logs: $artifactDirectory"
    }

    if ($null -ne $failure) {
        throw $failure
    }
}

# Retry readiness failures only. Successful probe output is returned to the caller
# (registration returns a token-bearing object); do not log that response object.
function Wait-ForEndpoint {
    param(
        [Parameter(Mandatory)]
        [string]$Description,

        [Parameter(Mandatory)]
        [scriptblock]$Probe,

        [int]$TimeoutSeconds = 120
    )

    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    $lastError = ""

    while ($timer.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
        try {
            & $Probe
            return
        }
        catch {
            $lastError = $_.Exception.Message
            Start-Sleep -Seconds 2
        }
    }

    throw "Timed out waiting for ${Description}. Last error: $lastError"
}

$stopWatch = [System.Diagnostics.Stopwatch]::StartNew()

try {
    switch ($Stage) {
        "verify" {
            foreach ($servie in $services) {
                Invoke-ServiceVerification -ServicePath $servie
            }
        }

        "images" {
            if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
                throw "Docker CLI was not found. Install Docker Desktop."
            }

            & docker info --format '{{.OSType}}'

            if ($LASTEXITCODE -ne 0) {
                throw "Docker is unavailable. Start Docker Desktop and retry."
            }

            foreach ($service in $services) {
                Invoke-ServiceImageBuild `
                    -ServicePath $service `
                    -Tag $ImageTag
            }
        }

        "e2e" {
            Invoke-EndToEndTests -Tag $ImageTag
        }

        # Standalone stages are independent; all enforces the complete stage order.
        "all" {
            if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
                throw "Docker CLI was not found. Install Docker Desktop."
            }

            & docker info --format '{{.OSType}}'

            if ($LASTEXITCODE -ne 0) {
                throw "Docker is unavailable. Start Docker Desktop and retry."
            }

            foreach ($service in $services) {
                Invoke-ServiceVerification -ServicePath $service
            }

            foreach ($service in $services) {
                Invoke-ServiceImageBuild `
                    -ServicePath $service `
                    -Tag $ImageTag
            }

            Invoke-EndToEndTests -Tag $ImageTag
        }
    }

    $stopWatch.Stop()
    Write-Host ""
    Write-Host (
        "Stage '{0}' completed successfully in {1:N1} seconds." -f
        $Stage, $stopWatch.Elapsed.TotalSeconds
    ) -ForegroundColor Green
}
catch {
    Write-Host ""
    Write-Host $_.Exception.Message -ForegroundColor Red

    if ($_.ScriptStackTrace) {
        Write-Host $_.ScriptStackTrace -ForegroundColor DarkGray
    }

    exit 1
}
