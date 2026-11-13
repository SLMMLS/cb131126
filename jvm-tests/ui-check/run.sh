#!/usr/bin/env bash
# Type-checks the Compose screens (app/src/main/kotlin/.../ui/*.kt) with kotlinc, without Gradle and without the real
# Compose libraries (which cannot be downloaded in the place where this was written): the screens are compiled against
# small hand-written stubs of Compose, Material 3, Navigation, Activity, CameraX and ML Kit (./stubs), with the real
# signatures of the versions the project uses (Compose BOM 2024.09.00, Material 3 1.3.0, Navigation 2.8.0, Activity 1.9.2),
# and against the real sources of the non-UI code they call (names, the transfer list, the queue...).
#
# What this catches: a mistyped name, a missing import, a wrong type or argument of anything the screens call in
# the app's own code, a missing @OptIn, a composable called with parameters that do not exist. What it cannot catch:
# anything the stubs got wrong about the real libraries (they are written from the published API, not generated from
# it), the rules that only the Compose compiler enforces (a composable called from a non-composable lambda), and how
# the screens look and behave. Gradle and a phone remain the final word.
#
# A second step proves that the check is not a rubber stamp: a file with known mistakes must be rejected.
#
# Needs the same environment variables as ../run.sh.
set -euo pipefail

: "${KOTLINC:?path to the kotlinc script, e.g. ~/kotlinc/bin/kotlinc}"
: "${ANDROID_JAR:?path to android.jar of API 34}"
: "${BCPROV_JAR:?path to bcprov-jdk18on-*.jar}"
: "${ORG_JSON_JAR:?path to a real org.json jar}"
KOTLIN_LIB="${KOTLIN_LIB:-$(dirname "$KOTLINC")/../lib}"

HERE="$(cd "$(dirname "$0")" && pwd)"
APP="$HERE/../../app/src/main/kotlin/com/clipsync/app"
OUT="$(mktemp -d)"
CP="$KOTLIN_LIB/kotlin-stdlib.jar:$KOTLIN_LIB/kotlinx-coroutines-core-jvm.jar:$BCPROV_JAR:$ORG_JSON_JAR"

# The real, non-UI sources the screens depend on (the same set the JVM tests compile), plus the pure ones next to the UI.
REAL=(
  "$APP"/files/FileNames.kt "$APP"/files/FileTypes.kt "$APP"/files/ThumbMath.kt "$APP"/files/FileProtocol.kt
  "$APP"/files/TransferModels.kt "$APP"/files/FileReceiver.kt "$APP"/files/FileSender.kt "$APP"/files/TransferManager.kt
  "$APP"/files/FileSources.kt "$APP"/crypto/CryptoUtils.kt "$APP"/crypto/Identity.kt
  "$APP"/network/NetworkErrors.kt "$APP"/network/PinnedHttp.kt "$APP"/network/Protocol.kt "$APP"/network/HelloState.kt
  "$HERE"/../stubs/SyncPort.kt
)
STUBS=("$HERE"/stubs/*.kt)
SCREENS=("$APP"/ui/*.kt)
# The same global opt-in as app/build.gradle.kts.
FLAGS=(-no-stdlib -cp "$CP:$ANDROID_JAR" -jvm-target 17 -opt-in=androidx.compose.material3.ExperimentalMaterial3Api)

echo "Compiling the screens against the stubs..."
"$KOTLINC" "${FLAGS[@]}" -d "$OUT/real" "${SCREENS[@]}" "${REAL[@]}" "${STUBS[@]}" 2>&1 | grep -v "^warning:" || true
if [ -n "$(find "$OUT/real" -name '*.class' 2>/dev/null | head -1)" ]; then
  echo "OK: ${#SCREENS[@]} screen files type-check ($(find "$OUT/real/com/clipsync/app/ui" -name '*.class' | wc -l) classes)."
else
  echo "FAILED: the screens do not type-check (see the errors above)."
  exit 1
fi

echo "Negative control: a file with known mistakes must be rejected..."
if "$KOTLINC" "${FLAGS[@]}" -d "$OUT/negative" "${SCREENS[@]}" "${REAL[@]}" "${STUBS[@]}" "$HERE"/negative/Broken.kt > "$OUT/negative.log" 2>&1; then
  echo "FAILED: the broken file was accepted, the check proves nothing."
  exit 1
fi
EXPECTED=4
FOUND=$(grep -c "Broken.kt.*error:" "$OUT/negative.log" || true)
if [ "$FOUND" -lt "$EXPECTED" ]; then
  echo "FAILED: only $FOUND of the $EXPECTED planted mistakes were reported:"
  grep "error:" "$OUT/negative.log" | head
  exit 1
fi
echo "OK: all $EXPECTED planted mistakes were reported."
