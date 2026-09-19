# Bike Rental Platform

Java 21 / Spring Boot microservices for bike inventory, stations, reservations,
authentication, and rentals. The repository includes PostgreSQL integration tests,
a REST Assured end-to-end workflow, and local delivery pipeline scripts.

## Repository layout

| Directory | Responsibility |
| --- | --- |
| `services/auth-service` | Registration, login, and JWT issuance |
| `services/bike-service` | Bikes, stations, and bike state transitions |
| `services/reservation-service` | Reservations, expiration, and reservation events |
| `services/rental-service` | Rental lifecycle and daily pricing |
| `platform/discovery-service` | Eureka service discovery |
| `platform/api-gateway` | API routing and JWT authentication |
| `e2e-tests` | Full reservation and rental workflow through the gateway |
| `ops` | PowerShell/Bash pipeline scripts and isolated CI Compose configuration |

Each application has its own Gradle wrapper and Dockerfile. GitHub Actions uses
the Bash pipeline for verification and isolated E2E testing. Registry publishing,
deployment automation, and infrastructure provisioning are planned next.

## Prerequisites

- Java 21 on `PATH` with `JAVA_HOME` pointing to the JDK.
- Docker with Linux containers and Docker Compose supporting `up --wait`.
- PowerShell 7 (`pwsh`) for the PowerShell pipeline.
- Bash for the shell pipeline; Git Bash works with Windows Java and Docker Desktop.
- `curl` and `jq` for Bash's `e2e` and `all` stages.
- Network access for the first Gradle dependency downloads and container pulls.

Use LF line endings for `.sh` files and Gradle's Unix wrappers. WSL needs its own
Java installation and Docker integration. No global Gradle installation is needed.
The E2E stack runs ten containers; allow enough Docker memory and disk space.

## Run the local pipeline

Run these commands from the repository root. Choose either script.

### PowerShell

```powershell
pwsh -File ./ops/pipeline.ps1 -Stage verify
pwsh -File ./ops/pipeline.ps1 -Stage images -ImageTag local
pwsh -File ./ops/pipeline.ps1 -Stage e2e -ImageTag local
pwsh -File ./ops/pipeline.ps1 -Stage all -ImageTag local
$LASTEXITCODE
```

### Bash

```bash
bash ops/pipeline.sh verify
bash ops/pipeline.sh images local
bash ops/pipeline.sh e2e local
bash ops/pipeline.sh all local
echo $?
```

Both default to `verify` and image tag `local`. Stages run sequentially and stop
on failure. A successful run exits with zero; failures return a nonzero status.
Read the exit code immediately after the pipeline command.

| Stage | What it does |
| --- | --- |
| `verify` | Runs `check bootJar` through each application's wrapper: tests, configured coverage checks, and executable JAR packaging |
| `images` | Builds six local `bike-rental/<service>:<tag>` images using the existing Dockerfiles |
| `e2e` | Starts an isolated stack using existing images, checks readiness, runs E2E tests, captures diagnostics, and cleans up |
| `all` | Runs `verify`, then `images`, then `e2e` |

`images` alone does not run verification. `e2e` does not build application images;
run `images` first with the same tag, or use `all`. Dockerfiles currently compile
the applications again inside the image build. Nothing is pushed to a registry.
Services with no test sources may report `NO-SOURCE`; that is not test coverage.

## How the E2E environment works

[ops/compose.ci.yaml](ops/compose.ci.yaml) is separate from the development
[docker-compose.yaml](docker-compose.yaml).

1. The script creates a unique `bike-ci-*` Compose project and log directory.
2. `IMAGE_TAG` selects prebuilt local application images; missing images fail.
3. Compose starts six applications, three PostgreSQL databases, and Kafka.
4. Health checks wait for startup, with a 300-second Compose wait timeout.
5. Docker assigns a loopback host port to the gateway; the script discovers it.
6. A disposable user registration obtains a token for authenticated route probes.
7. The script checks station/reservation GET routes and the rental OPTIONS route.
   These readiness probes retry for roughly 120 seconds each; test assertions do not.
8. `API_GATEWAY_URL` points REST Assured at this gateway. Gradle's `--rerun-tasks`
   ensures tests execute against the fresh environment.
9. The script captures container logs/status before removing that project's
   containers, network, and volumes on success or ordinary failure.

Only the gateway publishes a host port. Databases and Kafka communicate over the
project network. Disposable credentials and probe accounts are for this test
environment. Development volumes are separate. Images remain available afterward.
Avoid overlapping runs in the same checkout: Gradle build/report directories are
shared even though Compose resources are isolated.

Readiness proves that routes respond, not every business invariant. The current
E2E workflow reserves one bike and rents a different bike; it does not test
reservation-to-rental conversion.

## Reports and troubleshooting

Both scripts print `build/pipeline/<project>/` at the end of an E2E run.

| Output | PowerShell | Bash |
| --- | --- | --- |
| `compose.log`, `containers.txt` under the run directory | Yes | Yes |
| `e2e.log` under the run directory | No; Gradle output is shown in the terminal | Yes |
| E2E HTML/XML copied into the run directory | No | Yes, when available |

Standard reports remain under each module's `build/reports/tests/test/` and
`build/test-results/test/`. Coverage output is under `build/jacocoHtml/` and
`build/reports/jacoco/` when generated. PowerShell's E2E reports remain in
`e2e-tests/build/`; later runs can overwrite them. Build outputs are Git-ignored.

- **Docker unavailable:** start Docker Desktop and select Linux containers.
- **Application image missing:** run `images` with the tag passed to `e2e`.
- **Startup/routing timeout:** inspect `compose.log` and `containers.txt` for the
  failed run. Healthy containers can still be waiting for discovery propagation.
- **E2E assertion failure:** inspect the test report. Do not automatically retry
  assertions to make the pipeline green.
- **Script fails to parse:** check LF endings and run `bash -n ops/pipeline.sh`.
- **Cleanup interrupted:** a terminated shell, Docker outage, or machine shutdown
  can leave resources behind. Inspect the exact run's project before removal.

For example, replace the project value below with the failed run's printed ID:

```bash
docker compose -p bike-ci-YOUR-RUN-ID -f ops/compose.ci.yaml ps --all
docker compose -p bike-ci-YOUR-RUN-ID -f ops/compose.ci.yaml down --volumes
```

The second command deletes that project's disposable database volumes. Do not use
the development project name or global Docker pruning for pipeline cleanup.

## Validate changes to the scripts

1. Run `all` and confirm a zero exit code and saved diagnostics.
2. Temporarily change the registration expectation in `SystemEndToEndTest` from
   HTTP `201` to `418`, then run `e2e`.
3. Confirm a nonzero exit code, retained logs, and removal of the run's Docker
   resources. Restore `201` immediately afterward; do not commit the failing assertion.

No application image rebuild is needed for that assertion experiment: E2E tests
compile and run on the host. The pipeline scripts have been exercised locally for
both success and intentional assertion failure; future CI runs validate the
GitHub runner environment separately.

## GitHub Actions CI

[.github/workflows/ci.yml](.github/workflows/ci.yml) runs on pull requests targeting
`main`, pushes to `main`, and manual dispatch. Both jobs use Ubuntu 24.04 and Java
21, with a 30-minute timeout per job. New runs cancel older runs for the same ref.

| Check name | Work performed |
| --- | --- |
| `Verify services` | Checks Bash syntax and runs `bash ops/pipeline.sh verify` |
| `End-to-End Tests` | After verification passes, builds images tagged with the run's commit SHA and runs `e2e` with that same tag |

Image builds and E2E execution share a runner within the second job, so the test
stack can use those local images without a registry. Gradle caching is read-only
outside `main`.

Report uploads run even after a preceding step fails, when files are available:

- `verification-reports-<attempt>` contains the verification log and application
  test/coverage reports.
- `e2e-reports-<attempt>` contains the per-run E2E diagnostics and copied test
  reports.

Artifacts are retained for seven days and can be downloaded from the workflow
run's summary page. Failures before report creation may leave no artifact.

For branch protection on `main`, require both check names shown above. Branch
protection is configured in GitHub repository settings, outside this workflow.
Confirm both checks complete successfully on a pull request before relying on
the hosted pipeline. If a merge queue is enabled later, add a `merge_group`
trigger so required checks also run for queued merges.

## Delivery roadmap

1. Confirm the GitHub-hosted workflow and required checks on a pull request.
2. Image scanning, registry publication, and immutable release manifests.
3. Staging/production deployment configuration and rollback procedures.
4. Infrastructure-as-code after choosing the deployment target.
