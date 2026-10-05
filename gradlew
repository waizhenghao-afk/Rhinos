#!/usr/bin/env bash
# Build wrapper: uses the local Gradle 8.10.2 + JDK 21 toolchain set up for this project.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLS="$(cd "$SCRIPT_DIR/.." && pwd)/.tools"
export JAVA_HOME="$TOOLS/jdk/jdk21"
export ANDROID_HOME="$TOOLS/android-sdk"
export PATH="$JAVA_HOME/bin:$TOOLS/gradle/gradle-8.10.2/bin:$PATH"
cd "$SCRIPT_DIR"
exec gradle "$@"
