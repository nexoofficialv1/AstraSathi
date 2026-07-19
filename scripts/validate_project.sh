#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
java --source 17 "$ROOT/scripts/ValidateProject.java" "$ROOT"
