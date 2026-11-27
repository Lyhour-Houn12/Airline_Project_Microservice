#!/usr/bin/env bash
# Builds the Docker images of the services with Jib (no Dockerfile).
#
# Usage (from anywhere):
#   scripts/build-images.sh                                  all services, one after another, into the local Docker
#   scripts/build-images.sh booking-service seat-service     only the named services
#   scripts/build-images.sh --parallel                       all services at the same time (logs in target/build-logs)
#   scripts/build-images.sh -p booking-service seat-service  named services at the same time
#   IMAGE_PREFIX=<docker-hub-user> scripts/build-images.sh --push
#                                                            build, then `docker push` each image (needs `docker login`)
#   scripts/build-images.sh -p --push booking-service        flags and service names combine freely
#
# Images: <prefix>/<service>:latest. The prefix is <docker.image.prefix> in the root pom.xml
# ("lyhour121"), or IMAGE_PREFIX. The CPU type is arm64; JIB_ARCH=amd64 builds for ordinary servers.
#
# Works with the Bash 3.2 that ships with macOS.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOG_DIR="${PROJECT_ROOT}/target/build-logs"      # target/ is git-ignored
DEFAULT_SERVICES=(discovery-server api-gateway user-service airline-core-service location-service
                  flight-ops-service pricing-service seat-service ancillary-service booking-service payment-service)

PARALLEL=false
PUSH=false
SERVICES=()

for arg in "$@"; do
  case "$arg" in
    --parallel|-p) PARALLEL=true ;;
    --push)        PUSH=true ;;
    -h|--help)     sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    -*)            echo "Unknown option: $arg (see --help)" >&2; exit 2 ;;
    *)             SERVICES+=("$arg") ;;
  esac
done
if [ ${#SERVICES[@]} -eq 0 ]; then
  SERVICES=("${DEFAULT_SERVICES[@]}")
fi

# services/<name> or cloud/<name>
module_path() {
  local service="$1"
  if   [ -f "${PROJECT_ROOT}/services/${service}/pom.xml" ]; then echo "services/${service}"
  elif [ -f "${PROJECT_ROOT}/cloud/${service}/pom.xml" ];    then echo "cloud/${service}"
  else return 1
  fi
}

# Image name, read from the root pom.xml (where Jib is configured), so a push always matches the build.
PREFIX="${IMAGE_PREFIX:-$(grep -m1 -oE '<docker.image.prefix>[^<]*' "${PROJECT_ROOT}/pom.xml" | sed 's/.*>//')}"
image_ref() { echo "${PREFIX}/$1"; }

MVN_FLAGS=(-B -q -DskipTests "-Ddocker.image.prefix=${PREFIX}")
if [ -n "${JIB_ARCH:-}" ]; then
  MVN_FLAGS+=("-Djib.arch=${JIB_ARCH}")
fi

for service in "${SERVICES[@]}"; do
  module_path "$service" >/dev/null || { echo "Unknown service: ${service}" >&2; echo "Known: ${DEFAULT_SERVICES[*]} config-server" >&2; exit 2; }
done

build_service() {
  local service="$1" module
  module="$(module_path "$service")"
  ( cd "$PROJECT_ROOT" && mvn "${MVN_FLAGS[@]}" -pl "$module" package jib:dockerBuild ) || return 1
  if [ "$PUSH" = true ]; then
    docker push --all-tags "$(image_ref "$service")" || return 1
  fi
}

ACTION="built"
[ "$PUSH" = true ] && ACTION="built and pushed"

# Every service depends on common-lib: build and install it (and the parent poms) once, first,
# so the service builds don't each rebuild it (in parallel they would overwrite each other's files).
echo "==> Building common-lib"
( cd "$PROJECT_ROOT" && mvn -B -q -DskipTests -pl common-lib -am install ) || { echo "==> common-lib failed to build" >&2; exit 1; }

START=$(date +%s)
if [ "$PARALLEL" = true ]; then
  mkdir -p "$LOG_DIR"
  pids=(); names=()
  for service in "${SERVICES[@]}"; do
    echo "==> Starting build for ${service} (log: target/build-logs/${service}.log)"
    build_service "$service" > "${LOG_DIR}/${service}.log" 2>&1 &
    pids+=("$!"); names+=("$service")
  done

  failed=()
  for i in "${!pids[@]}"; do
    if wait "${pids[$i]}"; then
      echo "    ${names[$i]}: ok"
    else
      echo "    ${names[$i]}: FAILED"
      failed+=("${names[$i]}")
    fi
  done

  echo
  if [ ${#failed[@]} -eq 0 ]; then
    echo "==> All images ${ACTION} successfully in $(( $(date +%s) - START ))s: ${SERVICES[*]}"
  else
    echo "==> Build FAILED for: ${failed[*]}"
    echo "    See logs in ${LOG_DIR}"
    exit 1
  fi
else
  for service in "${SERVICES[@]}"; do
    echo "==> Building image for ${service}"
    build_service "$service" || { echo "==> Build failed for ${service}" >&2; exit 1; }
  done
  echo "==> All images ${ACTION} in $(( $(date +%s) - START ))s: ${SERVICES[*]}"
fi
echo "    docker images '${PREFIX}/*'"
