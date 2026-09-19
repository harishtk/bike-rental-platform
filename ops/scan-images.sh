#!/usr/bin/env bash

# Usage: bash ops/scan-images.sh [image-tag]
# Scan every application image and retain reports even when a scan fails.
set -Eeuo pipefail

image_tag="${1:-local}"
trivy_image="aquasec/trivy:0.74.0"

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
run_id="$(date -u +%Y%m%dT%H%M%SZ)-$$"
report_directory="$repo_root/build/security/$run_id"

services=(
    auth-service
    bike-service
    reservation-service
    rental-service
    discovery-service
    api-gateway
)

die() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

(( $# <= 1 )) || die "Usage: bash ops/scan-images.sh [image-tag]"

[[ "$image_tag" =~ ^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$ ]] ||
    die "Invalid image tag: $image_tag"

command -v docker >/dev/null 2>&1 ||
    die "Docker is required."

docker_os="$(docker info --format '{{.OSType}}')"
docker_os="${docker_os//$'\r'/}"

[[ "$docker_os" == "linux" ]] ||
    die "Docker must be running Linux containers."

mkdir -p "$report_directory"

# Docker Desktop needs a Windows host path when invoked from Git Bash.
report_mount="$report_directory"
if command -v cygpath >/dev/null 2>&1; then
    report_mount="$(cygpath -m "$report_directory")"
fi

# Prevent Git Bash from rewriting container paths such as /reports.
docker_command() {
    MSYS_NO_PATHCONV=1 docker "$@"
}

summary_file="$report_directory/summary.txt"
printf 'Image tag: %s\nScanner: %s\n\n' \
    "$image_tag" "$trivy_image" > "$summary_file"

failed=0

for service in "${services[@]}"; do
    image="bike-rental/$service:$image_tag"
    log_file="$report_directory/$service.log"

    printf '\n=== Scanning %s ===\n' "$image"

    # Fail explicitly for missing local images instead of pulling another image.
    if ! docker image inspect "$image" >/dev/null 2>&1; then
        printf 'Missing local image: %s\n' "$image" | tee "$log_file"
        printf 'FAIL %s: image missing\n' "$image" >> "$summary_file"
        failed=1
        continue
    fi

    # Exit 1 for matching vulnerabilities. Operational errors also fail the scan.
    # pipefail ensures tee cannot hide the scanner's exit status.
    if docker_command run --rm \
        --mount "type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock" \
        --mount "type=volume,source=bike-trivy-cache,target=/root/.cache/trivy" \
        --mount "type=bind,source=$report_mount,target=/reports" \
        "$trivy_image" image \
        --image-src docker \
        --scanners vuln \
        --severity HIGH,CRITICAL \
        --exit-code 1 \
        --timeout 15m \
        --no-progress \
        --format json \
        --output "/reports/$service.json" \
        "$image" 2>&1 | tee "$log_file"
    then
        printf 'PASS %s\n' "$image" | tee -a "$summary_file"
    else
        scan_exit=$?
        printf 'FAIL %s: exit %s; inspect JSON and log\n' \
            "$image" "$scan_exit" | tee -a "$summary_file"
        failed=1
    fi
done

printf '\nReports: %s\n' "$report_directory"

if (( failed != 0 )); then
    printf 'Image scanning failed. See summary.txt and individual reports.\n' >&2
    exit 1
fi

printf 'All six images passed the HIGH/CRITICAL vulnerability policy.\n'