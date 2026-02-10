package com.example.perf;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.csv;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.incrementConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.repeat;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.core.CoreDsl.uniformRandomSwitch;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * High blast-radius CI burst workload:
 *  - mixed dependency resolution traffic from non-admin users
 *  - stresses repository resolution, permission checks, and metadata lookups
 *  - supports staircase concurrency to identify p95 tipping points
 */
public class ConcurrentCiJobLoadSimulation extends Simulation {

  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8081");

  private static final int START_CONCURRENT_JOBS = intProp("startConcurrentJobs", 500);
  private static final int CONCURRENCY_STEP = intProp("concurrencyStep", 100);
  private static final int CONCURRENCY_LEVELS = intProp("concurrencyLevels", 5);
  private static final Duration START_LEVEL_DURATION =
      Duration.ofSeconds(intProp("startLevelDurationSeconds", 180));
  private static final Duration LEVEL_DURATION =
      Duration.ofSeconds(intProp("levelDurationSeconds", 300));
  private static final Duration RAMP_DURATION =
      Duration.ofSeconds(intProp("rampDurationSeconds", 45));

  private static final int DEPENDENCY_REQUESTS_PER_JOB = intProp("dependenciesPerJob", 500);
  private static final Duration INTER_DEPENDENCY_PAUSE =
      Duration.ofMillis(intProp("interDependencyPauseMs", 30));

  private static final int ARTIFACT_P95_UNACCEPTABLE_MS =
      intProp("artifactP95UnacceptableMs", 1500);
  private static final int PERMISSION_P95_UNACCEPTABLE_MS =
      intProp("permissionP95UnacceptableMs", 1200);
  private static final int CANARY_P95_UNACCEPTABLE_MS =
      intProp("canaryP95UnacceptableMs", 800);
  private static final double FAILED_REQUESTS_UNACCEPTABLE_PCT =
      doubleProp("failedRequestsUnacceptablePct", 2.0d);

  private static final int CANARY_CONCURRENT_USERS = intProp("canaryConcurrentUsers", 10);
  private static final Duration CANARY_POLL_INTERVAL =
      Duration.ofSeconds(intProp("canaryPollIntervalSeconds", 5));
  private static final Duration CANARY_DURATION = totalTestDuration();

  private final FeederBuilder.Batchable<String> nonAdminUsers =
      csv("non_admin_users.csv").circular();
  private final FeederBuilder.Batchable<String> virtualRepos =
      csv("virtual_repos.csv").circular();

  private final HttpProtocolBuilder httpProtocol =
      http.baseUrl(BASE_URL)
          .acceptHeader("*/*")
          .contentTypeHeader("application/json")
          .disableCaching()
          .shareConnections()
          .userAgentHeader("gatling-ci-build-start/1.0");

  private final ChainBuilder permissionProbe =
      exec(
          http("Permission Check - Metadata List")
              .get("/artifactory/api/storage/#{metadataVirtualRepo}")
              .basicAuth("#{username}", "#{password}")
              .queryParam("list", "1")
              .queryParam("deep", "0")
              .check(status().in(200, 401, 403, 404)));

  private final ChainBuilder mavenInstall =
      exec(
              http("Artifact Resolution - Maven Metadata")
                  .get("/artifactory/api/storage/#{mavenVirtualRepo}/org/apache/commons/commons-lang3/3.14.0")
                  .basicAuth("#{username}", "#{password}")
                  .check(status().in(200, 401, 403, 404)))
          .exec(
              http("Artifact Resolution - Maven Binary")
                  .get("/artifactory/#{mavenVirtualRepo}/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar")
                  .basicAuth("#{username}", "#{password}")
                  .check(status().in(200, 302, 304, 401, 403, 404)));

  private final ChainBuilder npmInstall =
      exec(
              http("Artifact Resolution - NPM Metadata")
                  .get("/artifactory/api/npm/#{npmVirtualRepo}/lodash")
                  .basicAuth("#{username}", "#{password}")
                  .check(status().in(200, 401, 403, 404)))
          .exec(
              http("Artifact Resolution - NPM Tarball")
                  .get("/artifactory/#{npmVirtualRepo}/lodash/-/lodash-4.17.21.tgz")
                  .basicAuth("#{username}", "#{password}")
                  .check(status().in(200, 302, 304, 401, 403, 404)));

  private final ChainBuilder pypiInstall =
      exec(
              http("Artifact Resolution - PyPI Simple Index")
                  .get("/artifactory/api/pypi/#{pypiVirtualRepo}/simple/requests/")
                  .basicAuth("#{username}", "#{password}")
                  .check(status().in(200, 401, 403, 404)))
          .exec(
              http("Artifact Resolution - PyPI Wheel")
                  .get("/artifactory/#{pypiVirtualRepo}/packages/requests-2.32.5-py3-none-any.whl")
                  .basicAuth("#{username}", "#{password}")
                  .check(status().in(200, 302, 304, 401, 403, 404)));

  private final ChainBuilder dockerPull =
      exec(
              http("Artifact Resolution - Docker Tags")
                  .get("/artifactory/api/docker/#{dockerVirtualRepo}/v2/library/busybox/tags/list")
                  .basicAuth("#{username}", "#{password}")
                  .check(status().in(200, 401, 403, 404)))
          .exec(
              http("Artifact Resolution - Docker Manifest")
                  .get("/artifactory/api/docker/#{dockerVirtualRepo}/v2/library/busybox/manifests/latest")
                  .header("Accept", "application/vnd.docker.distribution.manifest.v2+json")
                  .basicAuth("#{username}", "#{password}")
                  .check(status().in(200, 401, 403, 404)));

  private final ScenarioBuilder ciBuildStart =
      scenario("CI Build Start - Mixed Dependency Resolution")
          .feed(nonAdminUsers)
          .feed(virtualRepos)
          .repeat(DEPENDENCY_REQUESTS_PER_JOB).on(
              exec(permissionProbe)
                  .exec(
                      uniformRandomSwitch()
                          .on(
                              exec(mavenInstall),
                              exec(npmInstall),
                              exec(pypiInstall),
                              exec(dockerPull)))
                  .exec(pause(INTER_DEPENDENCY_PAUSE)));

  private final ScenarioBuilder canaryTraffic =
      scenario("Canary - Cross-Service Impact")
          .feed(nonAdminUsers)
          .forever()
          .on(
              exec(
                      http("Canary - System Ping")
                          .get("/artifactory/api/system/ping")
                          .basicAuth("#{username}", "#{password}")
                          .check(status().in(200, 401, 403)))
                  .exec(pause(CANARY_POLL_INTERVAL)));

  public ConcurrentCiJobLoadSimulation() {
    setUp(
            ciBuildStart.injectClosed(
                constantConcurrentUsers(START_CONCURRENT_JOBS).during(START_LEVEL_DURATION),
                incrementConcurrentUsers(CONCURRENCY_STEP)
                    .times(CONCURRENCY_LEVELS)
                    .eachLevelLasting(LEVEL_DURATION)
                    .separatedByRampsLasting(RAMP_DURATION)
                    .startingFrom(START_CONCURRENT_JOBS)),
            canaryTraffic.injectClosed(
                constantConcurrentUsers(CANARY_CONCURRENT_USERS).during(CANARY_DURATION)))
        .protocols(httpProtocol)
        .assertions(
            details("Artifact Resolution - Maven Binary")
                .responseTime()
                .percentile3()
                .lte(ARTIFACT_P95_UNACCEPTABLE_MS),
            details("Artifact Resolution - NPM Tarball")
                .responseTime()
                .percentile3()
                .lte(ARTIFACT_P95_UNACCEPTABLE_MS),
            details("Artifact Resolution - PyPI Wheel")
                .responseTime()
                .percentile3()
                .lte(ARTIFACT_P95_UNACCEPTABLE_MS),
            details("Artifact Resolution - Docker Manifest")
                .responseTime()
                .percentile3()
                .lte(ARTIFACT_P95_UNACCEPTABLE_MS),
            details("Permission Check - Metadata List")
                .responseTime()
                .percentile3()
                .lte(PERMISSION_P95_UNACCEPTABLE_MS),
            details("Canary - System Ping")
                .responseTime()
                .percentile3()
                .lte(CANARY_P95_UNACCEPTABLE_MS),
            global().failedRequests().percent().lte(FAILED_REQUESTS_UNACCEPTABLE_PCT));
  }

  private static int intProp(String key, int defaultValue) {
    return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
  }

  private static double doubleProp(String key, double defaultValue) {
    return Double.parseDouble(System.getProperty(key, String.valueOf(defaultValue)));
  }

  private static Duration totalTestDuration() {
    long start = START_LEVEL_DURATION.getSeconds();
    long stepped = (LEVEL_DURATION.getSeconds() + RAMP_DURATION.getSeconds()) * CONCURRENCY_LEVELS;
    return Duration.ofSeconds(start + stepped);
  }
}
