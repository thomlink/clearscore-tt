#!/usr/bin/env bash
set -eu

echo "Started running at: $(date '+%Y-%m-%d %H:%M:%S %:z')"

sbt run

echo "Finished running at: $(date '+%Y-%m-%d %H:%M:%S %:z')"
