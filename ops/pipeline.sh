#!/usr/bin/env bash

# Local pipeline: bash ops/pipeline.sh [verify|images|e2e|all] [image-tag]
# See README.md for prerequisites, artifacts, and the failure-cleanup exercise.

# Propagate ERR traps, reject unset variables, and detect failed pipeline commands.
set -Eeuo pipefail

stage="${1:-verify}"
image_tag="${2:-local}"

# Resolve paths relative to this file, not the caller's working directory.
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

services=(
    "services/auth-service"
    "services/bike-service"
    "services/reservation-service"
    "services/rental-service"
    "platform/discovery-service"
    "platform/api-gateway"
)

die() {
    printf '\nERROR: %s\n' "$*" >&2
    exit 1
}

on_error() {
    local exit_code="$1"
    local line="$2"

    printf '\nPipeline failed at line %s (exit code %s).\n' \
        "$line" "$exit_code" >&2

    exit "$exit_code"
}

trap 'on_error "$?" "$LINENO"' ERR

require_command() {
    command -v "$1" >/dev/null 2>&1 ||
        die "Required command not found: $1"
}

check_docker() {
    require_command docker

    local docker_os
    docker_os="$(docker info --format '{{.OSType}}')"
    docker_os="${docker_os//$'\r'/}"

    [[ "$docker_os" == "linux" ]] ||
        die "Docker must be running Linux containers."
}

# The subshell restores the caller's directory automatically, including on failure.
verify_service() (
    local service_path="$1"
    local service_directory="$repo_root/$service_path"

    [[ -f "$service_directory/gradlew" ]] ||
        die "Gradle wrapper not found in $service_directory"

    printf '\n=== Verifying %s ===\n' "$service_path"

    cd -- "$service_directory"
    bash ./gradlew check bootJar --console=plain --no-daemon
)

verify_all() {
    require_command java
    check_docker

    local service
    for service in "${services[@]}"; do
        verify_service "$service"
    done
}

build_image() {
    local service_path="$1"
    local service_directory="$repo_root/$service_path"
    local service_name="${service_path##*/}"
    local image="bike-rental/${service_name}:${image_tag}"

    [[ -f "$service_directory/Dockerfile" ]] ||
        die "Dockerfile not found in $service_directory"

    printf '\n=== Building %s ===\n' "$image"

    # Build only; verification and registry publication are separate concerns.
    docker build \
        --file "$service_directory/Dockerfile" \
        --tag "$image" \
        "$service_directory"

    printf 'Built %s\n' "$image"
}

build_all_images() {
    check_docker

    local service
    for service in "${services[@]}"; do
        build_image "$service"
    done
}

check_e2e_tools() {
    require_command java
    require_command curl
    require_command jq
    check_docker
    docker compose version >/dev/null
}

# Only the token goes to stdout: callers capture it with command substitution.
# Progress goes to stderr, and neither credentials nor response bodies are logged.
wait_for_auth() {
    local gateway_url="$1"
    local deadline=$((SECONDS + 120))
    local attempt=0
    local username body response token

    printf 'Waiting for gateway-to-auth routing...\n' >&2

    while (( SECONDS < deadline )); do
        attempt=$((attempt + 1))
        # A timed-out registration may have succeeded; avoid reusing its username.
        username="probe-$(date +%s)-$$-${RANDOM}-${attempt}"

        body="$(jq -nc \
            --arg username "$username" \
            --arg password "Readiness-Test-123!" \
            '{username: $username, password: $password}')"

        if response="$(curl \
            --silent --show-error --fail \
            --connect-timeout 2 --max-time 5 \
            --request POST \
            --header 'Content-Type: application/json' \
            --data "$body" \
            "$gateway_url/api/v1/auth/register" 2>/dev/null)"
        then
            if token="$(printf '%s' "$response" |
                jq -er '.accessToken | strings | select(length > 0)' \
                    2>/dev/null)"
            then
                printf '%s' "$token"
                return 0
            fi
        fi

        sleep 2
    done

    printf 'Timed out waiting for auth registration through the gateway.\n' >&2
    return 1
}

# Retry discovery/readiness delays here, never business-test assertion failures.
wait_for_route() {
    local gateway_url="$1"
    local token="$2"
    local method="$3"
    local path="$4"

    local deadline=$((SECONDS + 120))
    local status="unavailable"

    printf 'Waiting for %s %s...\n' "$method" "$path"

    while (( SECONDS < deadline )); do
        if status="$(curl \
            --silent --show-error \
            --connect-timeout 2 --max-time 5 \
            --output /dev/null \
            --write-out '%{http_code}' \
            --request "$method" \
            --header "Authorization: Bearer $token" \
            "$gateway_url$path" 2>/dev/null)"
        then
            if [[ "$status" == "200" ]]; then
                return 0
            fi
        fi

        sleep 2
    done

    printf 'Timed out waiting for %s %s; last HTTP status: %s\n' \
        "$method" "$path" "$status" >&2

    return 1
}

# Scope environment variables and EXIT/signal traps to this E2E run.
run_e2e() (
    check_e2e_tools

    local project
    project="bike-ci-$(date +%s)-$$-${RANDOM}"

    local compose_file="$repo_root/ops/compose.ci.yaml"
    local artifact_directory="$repo_root/build/pipeline/$project"
    local cleanup_needed=false

    mkdir -p -- "$artifact_directory"

    # Reuse the exact project/file arguments for startup, diagnostics, and cleanup.
    local -a compose=(
        docker compose
        -p "$project"
        -f "$compose_file"
    )

    # These exports are confined to this function's subshell.
    export IMAGE_TAG="$image_tag"

    cleanup_e2e() {
        local exit_code="$1"

        # Cleanup must continue even if diagnostics cannot be collected.
        trap - EXIT ERR
        set +e

        if [[ "$cleanup_needed" == true ]]; then
            printf '\nCollecting diagnostics for %s...\n' "$project"

            "${compose[@]}" logs --no-color \
                >"$artifact_directory/compose.log" 2>&1

            if (( $? != 0 )); then
                printf 'WARNING: Could not collect all container logs.\n' >&2
            fi

            "${compose[@]}" ps --all \
                >"$artifact_directory/containers.txt" 2>&1

            if (( $? != 0 )); then
                printf 'WARNING: Could not collect container status.\n' >&2
            fi

            printf 'Removing E2E stack: %s\n' "$project"

            # Delete only this run's disposable resources; keep application images.
            "${compose[@]}" down --volumes --remove-orphans

            if (( $? != 0 )); then
                printf 'ERROR: Cleanup failed for %s.\n' "$project" >&2

                # Preserve the original failure, or fail a successful test on cleanup.
                if (( exit_code == 0 )); then
                    exit_code=1
                fi
            fi
        fi

        printf 'Pipeline logs: %s\n' "$artifact_directory"
        exit "$exit_code"
    }

    # EXIT handles normal completion/errors; catchable signals retain nonzero status.
    # Forced termination (SIGKILL, host shutdown) cannot guarantee cleanup.
    trap 'cleanup_e2e "$?"' EXIT
    trap 'exit 130' INT
    trap 'exit 143' TERM

    "${compose[@]}" config --quiet

    printf '\nStarting E2E stack: %s\n' "$project"

    # Partial startup failures also require cleanup.
    cleanup_needed=true

    "${compose[@]}" up \
        --detach \
        --no-build \
        --wait \
        --wait-timeout 300

    # Discover the dynamically assigned loopback port instead of assuming 8080.
    local binding
    binding="$("${compose[@]}" port api-gateway 8080)"
    binding="${binding//$'\r'/}"

    if [[ ! "$binding" =~ ^127\.0\.0\.1:([0-9]+)$ ]]; then
        die "Unexpected gateway port binding: $binding"
    fi

    local gateway_url="http://127.0.0.1:${BASH_REMATCH[1]}"
    export API_GATEWAY_URL="$gateway_url"

    printf 'Gateway: %s\n' "$gateway_url"

    local token
    token="$(wait_for_auth "$gateway_url")"

    wait_for_route "$gateway_url" "$token" GET "/api/v1/stations"
    wait_for_route "$gateway_url" "$token" GET "/api/v1/reservations"
    # OPTIONS checks rental routing without creating a business resource.
    wait_for_route "$gateway_url" "$token" OPTIONS "/api/v1/rentals"

    unset token

    printf '\nGateway routes are ready. Running E2E tests.\n'

    cd -- "$repo_root/e2e-tests"

    local test_exit=0

    # pipefail prevents tee from hiding a Gradle failure. Capture the failure so
    # test reports can be copied before exiting; rerun against the fresh database.
    bash ./gradlew test \
        --rerun-tasks \
        --console=plain \
        --no-daemon \
        2>&1 | tee "$artifact_directory/e2e.log" ||
        test_exit=$?

    # Preserve reports per run instead of relying on the latest build.
    if [[ -d build/reports/tests/test ]]; then
        cp -R build/reports/tests/test \
            "$artifact_directory/test-report"
    fi

    if [[ -d build/test-results/test ]]; then
        cp -R build/test-results/test \
            "$artifact_directory/test-results"
    fi

    if (( test_exit != 0 )); then
        printf '\nE2E tests failed (exit code %s).\n' "$test_exit" >&2
        exit "$test_exit"
    fi

    # The EXIT trap collects container logs and cleans up.
)

if (( $# > 2 )); then
    die "Usage: bash ops/pipeline.sh [verify|images|e2e|all] [image-tag]"
fi

case "$stage" in
    verify|images|e2e|all) ;;
    *)
        die "Unknown stage '$stage'. Choose verify, images, e2e, or all."
        ;;
esac

if [[ ! "$image_tag" =~ ^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$ ]]; then
    die "Invalid Docker image tag: $image_tag"
fi

started_at=$SECONDS

case "$stage" in
    verify)
        verify_all
        ;;
    images)
        build_all_images
        ;;
    e2e)
        run_e2e
        ;;
    all)
        # Check E2E prerequisites before spending time on builds.
        check_e2e_tools
        verify_all
        build_all_images
        run_e2e
        ;;
esac

printf "\nStage '%s' completed successfully in %s seconds.\n" \
    "$stage" "$((SECONDS - started_at))"
