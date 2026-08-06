$ErrorActionPreference = 'Stop'
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is required for the reproducible Java 21 gate.'
}
Write-Host 'Running the authoritative build inside the pinned Java 21 Docker image...'
docker build --pull --tag payroll-payment-orchestrator:verification .
if ($LASTEXITCODE -ne 0) { throw 'Docker build verification failed.' }
Write-Host 'Enterprise build gate completed. The runtime image uses Java 21 and a non-root user.'
