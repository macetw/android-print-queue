#!/bin/sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

GRADLE_VERSION=8.7
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}"

if [ ! -f "$GRADLE_HOME/gradle-${GRADLE_VERSION}/bin/gradle" ]; then
  echo "Downloading Gradle $GRADLE_VERSION..."
  mkdir -p "$GRADLE_HOME"
  cd "$GRADLE_HOME"
  curl -L "$GRADLE_URL" -o gradle.zip
  unzip -q gradle.zip
  rm gradle.zip
  cd "$SCRIPT_DIR"
fi

JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export JAVA_HOME

"$GRADLE_HOME/gradle-${GRADLE_VERSION}/bin/gradle" "$@"
