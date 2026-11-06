#!/bin/sh
# Appelé par la phase "Gradle build" du projet Xcode (iosApp.xcodeproj).
#
# Xcode exécute les Run Script avec un PATH minimal (/usr/bin:/bin:/usr/sbin:/sbin,
# sans le .zshrc) : un JDK installé via Homebrew (/opt/homebrew), JetBrains Toolbox
# ou SDKMAN est alors invisible -> "java: command not found". On résout donc un JDK
# avant d'appeler Gradle :
#   1. java déjà dans le PATH
#   2. /usr/libexec/java_home (mécanisme Apple : scanne les JVMs installées)
#   3. préfixes Homebrew usuels (openjdk)
# Si toujours rien : renseigner org.gradle.java.home=<chemin JDK> dans
# zesit-admin/gradle.properties (le wrapper utilisera ce JDK).
set -e

if ! command -v java >/dev/null 2>&1 && [ -x /usr/libexec/java_home ]; then
    JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || true)
    if [ -n "$JAVA_HOME" ]; then
        export JAVA_HOME
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

if ! command -v java >/dev/null 2>&1; then
    for j in /opt/homebrew/opt/openjdk /usr/local/opt/openjdk /opt/homebrew/opt/openjdk@17 /usr/local/opt/openjdk@17; do
        if [ -x "$j/bin/java" ]; then
            export PATH="$j/bin:$PATH"
            break
        fi
    done
fi

# JDKs installés par .dmg / installateur (hors PATH de Xcode)
if ! command -v java >/dev/null 2>&1; then
    for j in /Library/Java/JavaVirtualMachines/*/Contents/Home "$HOME"/Library/Java/JavaVirtualMachines/*/Contents/Home; do
        if [ -x "$j/bin/java" ]; then
            export PATH="$j/bin:$PATH"
            break
        fi
    done
fi

if ! command -v java >/dev/null 2>&1; then
    echo "ERREUR: aucun JDK trouve dans le PATH minimal de Xcode."
    echo "  - installez un JDK 17+ (ex: brew install openjdk@17, puis 'sudo ln -sfn \$(brew --prefix openjdk@17)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk')"
    echo "  - ou renseignez org.gradle.java.home=/Library/Java/JavaVirtualMachines/<jdk>.jdk/Contents/Home dans zesit-admin/gradle.properties"
    exit 1
fi

cd "${SRCROOT}/.."
./gradlew -p "${SRCROOT}/.." :composeApp:embedAndSignAppleFrameworkForXcode
