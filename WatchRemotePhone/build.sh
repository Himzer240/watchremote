#!/bin/bash
set -e
cd "$(dirname "$0")"

SDK=/home/user/tools/android/sdk
BT=$SDK/build-tools/34.0.0
PLAT=$SDK/platforms/android-34/android.jar
JAVA_HOME=/home/user/tools/android/jdk-17.0.21.1+2
export PATH=$JAVA_HOME/bin:$PATH

rm -rf build
mkdir -p build/gen build/classes build/dex build/apk_stage

echo "[1/5] aapt2 compile"
$BT/aapt2 compile --dir res -o build/res.zip

echo "[2/5] aapt2 link"
$BT/aapt2 link -o build/base.apk \
  -I "$PLAT" \
  --manifest AndroidManifest.xml \
  -R build/res.zip \
  -A assets \
  --java build/gen \
  --auto-add-overlay \
  --min-sdk-version 24 --target-sdk-version 33 \
  --version-code 1 --version-name 1.0

echo "[3/5] javac"
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options \
  -bootclasspath "$PLAT" \
  -cp libs/xms.jar \
  -d build/classes \
  $(find src build/gen -name "*.java")

echo "[4/5] d8"
$BT/d8 --min-api 24 --lib "$PLAT" --output build/dex \
  $(find build/classes -name "*.class") libs/xms.jar

( cd build/dex && zip -q ../base.apk classes.dex )
$BT/zipalign -f -p 4 build/base.apk build/aligned.apk

echo "[5/5] sign"
KS=medreminder.keystore
if [ ! -f "$KS" ]; then
  keytool -genkeypair -v -keystore "$KS" -alias medreminder \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass medreminder -keypass medreminder \
    -dname "CN=MedReminder, OU=App, O=Doubao, L=Beijing, C=CN" > /dev/null 2>&1
fi
$BT/apksigner sign --ks "$KS" --ks-pass pass:medreminder --key-pass pass:medreminder \
  --out WatchRemote-1.0.apk build/aligned.apk

echo "done: $(pwd)/WatchRemote-1.0.apk"
ls -lh WatchRemote-1.0.apk
