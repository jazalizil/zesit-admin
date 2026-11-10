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
# le framework binaire nous-mêmes selon la plateforme demandée par Xcode, puis on le
# copie à l'endroit attendu par le projet Xcode :
#   composeApp/build/xcode-frameworks/$PLATFORM_NAME/$CONFIGURATION/ComposeApp.framework
#
# Simulateur : framework FAT (arm64 + x86_64). Une destination générique d'Xcode
# (« Any iOS Simulator Device », deuxième build d'une PR…) lie les DEUX slices :
# un framework mono-arch produit alors « ignoring file 'ComposeApp': found
# architecture 'arm64', required architecture 'x86_64' » -> Undefined symbols.
# Les liens sont mis en cache par Gradle : le coût des slices superflues est nul
# après le premier build.
CONFIGURATION="${CONFIGURATION:-Debug}"
PLATFORM_NAME="${PLATFORM_NAME:-iphonesimulator}"
LOWER_CONFIG=$(echo "$CONFIGURATION" | tr '[:upper:]' '[:lower:]')

DST_DIR="composeApp/build/xcode-frameworks/${PLATFORM_NAME}/${CONFIGURATION}"
DST_FW="$DST_DIR/ComposeApp.framework"

if [ "$PLATFORM_NAME" = "iphoneos" ]; then
  ./gradlew ":composeApp:link${CONFIGURATION}FrameworkIosArm64"
  SRC_FW="composeApp/build/bin/iosArm64/${LOWER_CONFIG}Framework/ComposeApp.framework"
  rm -rf "$DST_FW"
  mkdir -p "$DST_DIR"
  cp -R "$SRC_FW" "$DST_FW"
else
  ./gradlew \
    ":composeApp:link${CONFIGURATION}FrameworkIosSimulatorArm64" \
    ":composeApp:link${CONFIGURATION}FrameworkIosX64"
  SRC_FW="composeApp/build/bin/iosSimulatorArm64/${LOWER_CONFIG}Framework/ComposeApp.framework"
  SRC_FW_X64="composeApp/build/bin/iosX64/${LOWER_CONFIG}Framework/ComposeApp.framework"
  rm -rf "$DST_FW"
  mkdir -p "$DST_DIR"
  cp -R "$SRC_FW" "$DST_FW"
  # Fusion des deux slices en un binaire fat (structure du framework identique).
  xcrun lipo -create \
    "$SRC_FW/ComposeApp" \
    "$SRC_FW_X64/ComposeApp" \
    -output "$DST_FW/ComposeApp"
fi
# Ressources Compose (polices Montserrat) : assemblage du dossier de la
# « folder reference » compose-resources déclarée dans la phase Resources de
# la cible iosApp (iosApp.xcodeproj). Xcode la copie dans le .app comme une
# ressource ordinaire, donc elle est SUIVIE par le graphe de build — requis :
# l'installation incrémentale d'Xcode sur simulateur ne copie QUE les fichiers
# suivis, zap ceux plaqués dans le .app par un script (sinon au lancement :
# « Missing resource … composeResources/fr.zesit.admin/font/montserrat_*.ttf »)
echo "ComposeApp.framework copié dans $DST_FW ($(file -b "$DST_FW/ComposeApp" | sed 's/, for*.*//'))"

STAGED_DIR="composeApp/build/ios-app-resources/compose-resources"
rm -rf "$STAGED_DIR"
# Le runtime lit <app>/compose-resources/composeResources/<package>/<type>/<fichier>
# (package = compose.resources.packageOfResClass du composeApp/build.gradle.kts) ;
# les ressources préparées sont mises à plat par Gradle — on reforme l'arborescence.
STAGED_PKG="$STAGED_DIR/composeResources/fr.zesit.admin"
mkdir -p "$STAGED_PKG"
for PREPARED in composeApp/build/generated/compose/resourceGenerator/preparedResources/*/; do
  [ -d "${PREPARED}composeResources" ] || continue
  cp -R "${PREPARED}composeResources/." "$STAGED_PKG/"
done
echo "compose-resources assemblés dans $STAGED_DIR (copiés dans le .app par la phase Resources)"
