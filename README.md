# Concurrent CI Job Load Test (Gatling)

This project implements a **high blast-radius** performance scenario for the CI build start window:

- many non-admin CI users start builds concurrently
- mixed dependency-resolution workload:
  - `mvn install` style traffic
  - `npm install` style traffic
  - `pip install` style traffic
  - `docker pull` style traffic
- additional metadata listing request on virtual repos to amplify permission calculations

The goal is to identify:

1. the concurrent job level where artifact-resolution **p95 latency becomes unacceptable**
2. the point where permission/metadata overhead starts affecting DB or app-thread health (visible as latency/failure degradation, and via external telemetry)

## Scenario Design

### Main scenario: `CI Build Start - Mixed Dependency Resolution`

- Closed-model concurrency (simulates fixed active CI jobs)
- Staircase profile:
  - start at a baseline concurrent job count
  - increase by steps per level
  - hold each level long enough for steady-state
- Each virtual "job" performs repeated dependency requests (`dependenciesPerJob`) and randomly switches between Maven/NPM/PyPI/Docker flows
- All requests use **non-admin credentials** from feeder data

### Canary scenario: `Canary - Cross-Service Impact`

- Small constant non-admin traffic hitting `api/system/ping`
- Helps detect if build-storm load is impacting unrelated operations

## Files

- `pom.xml`: Maven + Gatling configuration
- `src/test/java/com/example/perf/ConcurrentCiJobLoadSimulation.java`: simulation implementation
- `src/test/resources/non_admin_users.csv`: non-admin credentials feeder
- `src/test/resources/virtual_repos.csv`: target virtual repo names feeder

## Run

1. Update feeders with real non-admin users and repository names.
2. Run Gatling:

```bash
mvn gatling:test \
  -DbaseUrl=http://your-artifactory-host:8081 \
  -DstartConcurrentJobs=500 \
  -DconcurrencyStep=100 \
  -DconcurrencyLevels=4 \
  -DstartLevelDurationSeconds=180 \
  -DlevelDurationSeconds=300 \
  -DrampDurationSeconds=45 \
  -DdependenciesPerJob=500 \
  -DartifactP95UnacceptableMs=1500 \
  -DpermissionP95UnacceptableMs=1200 \
  -DcanaryP95UnacceptableMs=800 \
  -DfailedRequestsUnacceptablePct=2.0
```

## Key Tunables

- `startConcurrentJobs`: first concurrency level
- `concurrencyStep`: additional concurrent jobs per level
- `concurrencyLevels`: number of increments
- `dependenciesPerJob`: dependency requests per build
- `artifactP95UnacceptableMs`: p95 SLA threshold for artifact requests
- `permissionP95UnacceptableMs`: p95 SLA threshold for metadata/permission probe
- `failedRequestsUnacceptablePct`: failure-rate guardrail

## How to Answer the Jira Questions

### 1) When does artifact-resolution p95 become unacceptable?

In Gatling report/request stats, monitor p95 for:

- `Artifact Resolution - Maven Binary`
- `Artifact Resolution - NPM Tarball`
- `Artifact Resolution - PyPI Wheel`
- `Artifact Resolution - Docker Manifest`

The first staircase level where p95 breaches your threshold is your practical concurrency limit for build starts.

### 2) When does permission overhead affect DB or Tomcat threads?

Correlate these with external telemetry at each load level:

- `Permission Check - Metadata List` p95/p99 and errors
- DB: active connections, wait time, slow queries
- Tomcat: busy threads, queued requests, saturation
- `Canary - System Ping` p95 increase (cross-service impact signal)

The tipping point is where permission-probe latency and DB/thread indicators rise together, often followed by canary degradation.
