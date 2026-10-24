#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
BT="$SDK/build-tools/35.0.0"
JAR="$SDK/platforms/android-28/android.jar"
mkdir -p build/classes build/dex build/outputs
rm -f build/unsigned.apk build/aligned.apk build/outputs/YPod-Y-Dino-Galaxy-Y.apk
"$BT/aapt" package -f -M AndroidManifest.xml -I "$JAR" -F build/unsigned.apk
javac --release 8 -cp "$JAR" -d build/classes src/main/java/com/ypod/ydino/GameActivity.java
jar cf build/classes.jar -C build/classes .
"$BT/d8" --release --min-api 10 --lib "$JAR" --output build/dex build/classes.jar
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
keytool -genkeypair -noprompt -keystore build/debug.jks -storepass android -keypass android -alias androiddebugkey -dname "CN=YPod Y-Dino,O=Personal,C=BR" -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1
"$BT/apksigner" sign --min-sdk-version 10 --v1-signing-enabled true --v2-signing-enabled false --v3-signing-enabled false --ks build/debug.jks --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out build/outputs/YPod-Y-Dino-Galaxy-Y.apk build/aligned.apk
"$BT/apksigner" verify --min-sdk-version 10 --verbose build/outputs/YPod-Y-Dino-Galaxy-Y.apk
