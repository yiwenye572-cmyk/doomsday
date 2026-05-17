# PowerShell script to run docker-based tests
# Usage: .\scripts\run-docker-tests.ps1
docker compose up -d postgres redis
# wait for DB healthy
Write-Host "Waiting for Postgres to become ready..."
Start-Sleep -s 5
docker compose run --rm test-runner
# Bring down services
docker compose down --remove-orphans
