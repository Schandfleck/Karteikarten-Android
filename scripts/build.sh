#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
BUILD_VARIANT="${1:-release}"
case "$BUILD_VARIANT" in
    debug) TASK_VARIANT=Debug; APK_SOURCE=app/build/outputs/apk/debug/app-debug.apk; APK_TARGET=APK/Karteikarten-debug.apk ;;
    release)
        TASK_VARIANT=Release; APK_SOURCE=app/build/outputs/apk/release/app-release.apk; APK_TARGET=APK/Karteikarten.apk
        if [ ! -f signing.properties ]; then
            printf '%s\n' 'Release benötigt signing.properties und deinen Signierschlüssel. Siehe docs/BUILDING.md; alternativ: ./build.sh debug' >&2
            exit 1
        fi
        ;;
    *) printf '%s\n' 'Verwendung: ./build.sh [debug|release]' >&2; exit 2 ;;
esac
if [ -n "${GRADLE_BIN:-}" ]; then
    "$GRADLE_BIN" --no-daemon ":app:assemble$TASK_VARIANT" ":app:lint$TASK_VARIANT"
else
    ./gradlew --no-daemon ":app:assemble$TASK_VARIANT" ":app:lint$TASK_VARIANT"
fi
mkdir -p APK
if [ ! -f "$APK_SOURCE" ]; then
    printf '%s\n' 'Keine signierte APK gefunden. Bitte signing.properties prüfen (docs/BUILDING.md).' >&2
    exit 1
fi
if [ "$BUILD_VARIANT" = release ]; then
    SDK_PATH="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [ -z "$SDK_PATH" ] && [ -f local.properties ]; then
        SDK_PATH="$(sed -n 's/^sdk.dir=//p' local.properties)"
    fi
    if [ ! -x "$SDK_PATH/build-tools/36.0.0/apksigner" ]; then
        printf '%s\n' 'Signaturprüfung benötigt ANDROID_HOME mit Build Tools 36.0.0.' >&2
        exit 1
    fi
    "$SDK_PATH/build-tools/36.0.0/apksigner" verify --verbose "$APK_SOURCE"
fi
cp "$APK_SOURCE" "$APK_TARGET"
printf 'APK: %s\n' "$APK_TARGET"
