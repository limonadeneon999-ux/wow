#!/usr/bin/env bash
# baksmali/smali round trip for classes10.dex, using only public Maven Central artifacts.
#
# The MorpheApp smali fork lives on a private registry (401 here) and the SDK ships
# baksmali but not the assembler, so this pins org.smali 2.5.2, which is self-consistent
# and reads/writes the same dex 035 format as the APK.
#
# usage: dex-toolchain.sh d -o <outdir> <in.dex>      # disassemble
#        dex-toolchain.sh a -o <out.dex> <indir>      # assemble
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JARS="${MORPHE_DEX_JARS:-$HERE/../.dex-toolchain}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

download() { # url dest
  [ -s "$2" ] || curl -fsSL --retry 3 -o "$2" "$1"
}

mkdir -p "$JARS"
M=https://repo1.maven.org/maven2
download "$M/org/smali/smali/2.5.2/smali-2.5.2.jar"        "$JARS/smali.jar"
download "$M/org/smali/baksmali/2.5.2/baksmali-2.5.2.jar"  "$JARS/baksmali.jar"
download "$M/org/smali/dexlib2/2.5.2/dexlib2-2.5.2.jar"    "$JARS/dexlib2.jar"
download "$M/org/smali/util/2.5.2/util-2.5.2.jar"          "$JARS/util.jar"
download "$M/org/antlr/antlr-runtime/3.5.2/antlr-runtime-3.5.2.jar" "$JARS/antlr.jar"
download "$M/com/beust/jcommander/1.78/jcommander-1.78.jar" "$JARS/jcommander.jar"
download "$M/com/google/guava/guava/31.1-jre/guava-31.1-jre.jar" "$JARS/guava.jar"

CP="$JARS/smali.jar:$JARS/baksmali.jar:$JARS/dexlib2.jar:$JARS/util.jar:$JARS/antlr.jar:$JARS/jcommander.jar:$JARS/guava.jar"

case "${1:-}" in
  d) exec "$JAVA" -Xmx3g -cp "$CP" org.jf.baksmali.Main "$@" ;;
  a) exec "$JAVA" -Xmx3g -cp "$CP" org.jf.smali.Main    "$@" ;;
  *) echo "usage: $0 {d|a} -o <target> <source>" >&2; exit 2 ;;
esac
