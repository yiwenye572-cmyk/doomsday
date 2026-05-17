#!/usr/bin/env bash
# Run docker-based tests (Linux / WSL)
docker compose up -d postgres redis
echo "Waiting for Postgres to become ready..."
sleep 5
docker compose run --rm test-runner
# cleanup
docker compose down --remove-orphans
