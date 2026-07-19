#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/astra-sathi-parser-test"
rm -rf "$OUT"
mkdir -p "$OUT"
SOURCES=(
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/Command.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/BengaliText.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/LifeContextIntent.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/LifeContextParser.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/FuelEstimator.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/TextSimilarity.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/SensitiveDataFilter.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/CommandRouter.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/WorkflowStep.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/AutomationWorkflow.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/WorkflowPlanner.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/TradeOrder.java"
  "$ROOT/app/src/main/java/com/astratechnologies/astrasathi/TradeOrderParser.java"
  "$ROOT/tests/CommandRouterTest.java"
  "$ROOT/tests/WorkflowPlannerTest.java"
  "$ROOT/tests/TradeOrderParserTest.java"
  "$ROOT/tests/TextSimilarityTest.java"
  "$ROOT/tests/SensitiveDataFilterTest.java"
  "$ROOT/tests/LifeContextParserTest.java"
  "$ROOT/tests/FuelEstimatorTest.java"
)
if command -v javac >/dev/null 2>&1; then
  javac -encoding UTF-8 -d "$OUT" "${SOURCES[@]}"
else
  java --source 17 "$ROOT/scripts/CompileRunner.java" "$OUT" "${SOURCES[@]}"
fi
java -cp "$OUT" CommandRouterTest
java -cp "$OUT" WorkflowPlannerTest
java -cp "$OUT" TradeOrderParserTest
java -cp "$OUT" TextSimilarityTest
java -cp "$OUT" SensitiveDataFilterTest
java -cp "$OUT" LifeContextParserTest
java -cp "$OUT" FuelEstimatorTest
