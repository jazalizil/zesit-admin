#!/bin/sh
# Appelé par la phase "Gradle build" du projet Xcode (iosApp.xcodeproj) :
#   shellScript = "\"${SRCROOT}/gradle-build.sh\"\n"
#
# Xcode exécute les Run Script avec un PATH minimal (/usr/bin:/bin:... sans le
# .zshrc) : un JDK installé via SDKMAN, Android Studio (JBR) ou java_home est alors
# invisible -> "java: command not found". On résout donc JAVA_HOME avant ./gradlew
# (sdkman -> JBR Android Studio -> /usr/libexec/java_home), comme dans ToyotaObd.
set -e

if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
  exit 0
fi

cd "$SRCROOT/.."

# Aligner le xcodebuild utilisé par Gradle sur le Xcode qui lance le build
# (Xcode exporte DEVELOPER_DIR dans les Run Script).
if [ -n "$DEVELOPER_DIR" ] && [ -x "$DEVELOPER_DIR/usr/bin/xcodebuild" ]; then
  export PATH="$DEVELOPER_DIR/usr/bin:$PATH"
fi

if [ -z "$JAVA_HOME" ]; then
  if [ -x "$HOME/.sdkman/candidates/java/current/bin/java" ]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
  elif [ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  else
    export JAVA_HOME="$(/usr/libexec/java_home)"
  fi
fi

# Kotlin 2.2 ne fournit plus la tâche embedAndSignAppleFrameworkForXcode : on linke
# le framework binaire nous-mêmes selon la plateforme/arch demandées par Xcode,
# puis on le copie à l'endroit attendu par le projet Xcode :
#   composeApp/build/xcode-frameworks/$PLATFORM_NAME/$CONFIGURATION/ComposeApp.framework
CONFIGURATION="${CONFIGURATION:-Debug}"
PLATFORM_NAME="${PLATFORM_NAME:-iphonesimulator}"

case "$PLATFORM_NAME:$ARCHS" in
  iphoneos:*)
    TARGET=iosArm64 ;;
  *:x86_64)
    TARGET=iosX64 ;;
  *)
    TARGET=iosSimulatorArm64 ;;
esac

LOWER_CONFIG=$(echo "$CONFIGURATION" | tr '[:upper:]' '[:lower:]')
./gradlew ":composeApp:link${CONFIGURATION}Framework${TARGET}"

SRC_FW="composeApp/build/bin/${TARGET}/${LOWER_CONFIG}Framework/ComposeApp.framework"
DST_DIR="composeApp/build/xcode-frameworks/${PLATFORM_NAME}/${CONFIGURATION}"
DST_FW="$DST_DIR/ComposeApp.framework"

rm -rf "$DST_FW"
mkdir -p "$DST_DIR"
cp -R "$SRC_FW" "$DST_FW"
echo "ComposeApp.framework copié dans $DST_FW"
