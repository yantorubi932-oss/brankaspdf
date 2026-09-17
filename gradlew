#!/usr/bin/env sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -f "$CLASSPATH" ]; then
  :
else
  echo "Gradle wrapper JAR not found at $CLASSPATH" >&2
  echo "Run the project in Android Studio or install Gradle wrapper jar manually." >&2
  exit 1
fi

if [ -n "$JAVA_HOME" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi

exec "$JAVA_CMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
