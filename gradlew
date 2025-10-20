#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$WRAPPER_JAR" ]; then
  java -jar "$WRAPPER_JAR" "$@"
else
  if command -v gradle >/dev/null 2>&1; then
    gradle "$@"
  else
    echo "Gradle wrapper jar missing and Gradle is not installed." >&2
    exit 1
  fi
fi
