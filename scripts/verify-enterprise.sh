#!/usr/bin/env sh
set -eu

command -v docker >/dev/null 2>&1 || { echo 'Docker is required for the reproducible Java 21 gate.' >&2; exit 1; }

echo 'Running the authoritative build inside the pinned Java 21 Docker image...'
docker build --pull --tag payroll-payment-orchestrator:verification .
echo 'Enterprise build gate completed. The runtime image uses Java 21 and a non-root user.'
