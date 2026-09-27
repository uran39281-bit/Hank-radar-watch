#!/usr/bin/env bash
set -euo pipefail
# SDK paths must contain Android platform 35 and build-tools 35.0.0.
: "${RADAR_ANDROID_JAR:?Set RADAR_ANDROID_JAR to android.jar}"
: "${RADAR_BUILD_TOOLS:?Set RADAR_BUILD_TOOLS to build-tools directory}"
: "${RADAR_KEYSTORE:?Set RADAR_KEYSTORE to signing keystore}"
: "${RADAR_KEY_PASSWORD:?Set RADAR_KEY_PASSWORD to keystore password}"
cd "$(dirname "$0")"
rm -rf build
mkdir -p build/classes build/dex
"$RADAR_BUILD_TOOLS/aapt2" compile --dir res -o build/res.zip
"$RADAR_BUILD_TOOLS/aapt2" link -o build/unsigned.apk -I "$RADAR_ANDROID_JAR" --manifest AndroidManifest.xml -A assets build/res.zip
if [ -n "${RADAR_ECJ:-}" ]; then
 java -jar "$RADAR_ECJ" -8 -classpath "$RADAR_ANDROID_JAR" -d build/classes src/com/jb/radar/*.java
else
 javac --release 8 -classpath "$RADAR_ANDROID_JAR" -d build/classes src/com/jb/radar/*.java
fi
(cd build/classes && zip -qr ../classes.jar .)
"$RADAR_BUILD_TOOLS/d8" --lib "$RADAR_ANDROID_JAR" --min-api 26 --output build/dex build/classes.jar
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$RADAR_BUILD_TOOLS/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$RADAR_BUILD_TOOLS/apksigner" sign --ks "$RADAR_KEYSTORE" --ks-key-alias radar --ks-pass env:RADAR_KEY_PASSWORD --out build/HAWK-Radar-Watch-v0.3.apk build/aligned.apk
"$RADAR_BUILD_TOOLS/apksigner" verify --verbose build/HAWK-Radar-Watch-v0.3.apk
