#!/bin/sh

# Gradle wrapper start-up script (POSIX). Le jar gradle-wrapper.jar est généré
# par `gradle wrapper` (voir README.md) ou copié depuis une distribution Gradle :
#   cp $(dirname $(readlink -f $(which gradle)))/../lib/gradle-wrapper.jar gradle/wrapper/
# Il télécharge ensuite automatiquement la distribution déclarée dans
# gradle/wrapper/gradle-wrapper.properties.

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
    echo "gradle-wrapper.jar absent : lancez 'gradle wrapper' une fois (voir README.md)" >&2
    exit 1
fi

exec java ${JAVA_OPTS:-} -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
