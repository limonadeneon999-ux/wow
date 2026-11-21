#!/usr/bin/env bash
# Installs the prebuilt Morphe Gradle plugin + patcher into ~/.m2 so the
# project builds without access to maven.pkg.github.com/MorpheApp/registry.
#
# The GitHub Packages registry answers 401 for the patches plugin in this
# environment, so the plugin is served from mavenLocal() instead. The jars are
# the ones shipped with the toolchain; the POMs below declare the transitive
# dependencies the plugin needs (AGP, Kotlin plugin, patcher).
#
# Usage: bash tools/setup-mavenlocal.sh /path/to/toolchain/m2
set -euo pipefail

SRC="${1:-toolchain/m2}"
M2="$HOME/.m2/repository/app/morphe"

test -f "$SRC/plugin.jar"  || { echo "missing $SRC/plugin.jar" >&2; exit 1; }
test -f "$SRC/patcher.jar" || { echo "missing $SRC/patcher.jar" >&2; exit 1; }

mkdir -p "$M2/morphe-patches-gradle-plugin/2.0.0" \
         "$M2/patcher/2.0.0" \
         "$M2/patches/app.morphe.patches.gradle.plugin/2.0.0"

cp "$SRC/plugin.jar"  "$M2/morphe-patches-gradle-plugin/2.0.0/morphe-patches-gradle-plugin-2.0.0.jar"
cp "$SRC/patcher.jar" "$M2/patcher/2.0.0/patcher-2.0.0.jar"

cat > "$M2/morphe-patches-gradle-plugin/2.0.0/morphe-patches-gradle-plugin-2.0.0.pom" <<'POM'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>app.morphe</groupId>
  <artifactId>morphe-patches-gradle-plugin</artifactId>
  <version>2.0.0</version>
  <packaging>jar</packaging>
  <dependencies>
    <dependency>
      <groupId>com.android.tools.build</groupId>
      <artifactId>gradle</artifactId>
      <version>8.2.2</version>
    </dependency>
    <dependency>
      <groupId>app.morphe</groupId>
      <artifactId>patcher</artifactId>
      <version>2.0.0</version>
    </dependency>
    <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-gradle-plugin</artifactId>
      <version>1.9.22</version>
    </dependency>
  </dependencies>
</project>
POM

cat > "$M2/patcher/2.0.0/patcher-2.0.0.pom" <<'POM'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>app.morphe</groupId>
  <artifactId>patcher</artifactId>
  <version>2.0.0</version>
  <packaging>jar</packaging>
</project>
POM

cat > "$M2/patches/app.morphe.patches.gradle.plugin/2.0.0/app.morphe.patches.gradle.plugin-2.0.0.pom" <<'POM'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>app.morphe.patches</groupId>
  <artifactId>app.morphe.patches.gradle.plugin</artifactId>
  <version>2.0.0</version>
  <packaging>pom</packaging>
  <dependencies>
    <dependency>
      <groupId>app.morphe</groupId>
      <artifactId>morphe-patches-gradle-plugin</artifactId>
      <version>2.0.0</version>
    </dependency>
  </dependencies>
</project>
POM

echo "Installed Morphe plugin into $M2"
