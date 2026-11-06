#!/bin/sh

# Gradle wrapper start-up script (POSIX). Le jar gradle-wrapper.jar est embarqué
# (gradle/wrapper/) et télécharge la distribution déclarée dans
# gradle/wrapper/gradle-wrapper.properties au premier lancement.

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
    echo "gradle-wrapper.jar absent : lancez 'gradle wrapper' une fois (voir README.md)" >&2
    exit 1
fi

# Xcode exécute les Run Script avec un PATH minimal (/usr/bin:/bin:...) : si
# 'java' n'est pas trouvé (JDK Homebrew/Toolbox/SDKMAN hors PATH), on le
# résout via /usr/libexec/java_home (mécanisme Apple, scanne les JVMs installées).
if ! command -v java >/dev/null 2>&1 && [ -x /usr/libexec/java_home ]; then
    JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null) && export JAVA_HOME
    [ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH"
fi

exec java ${JAVA_OPTS:-} -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
