#!/usr/bin/env bash

##############################################################################
##
##  SpecScript CLI wrapper script for Linux and Macos
##
##############################################################################

die ( ) {
    echo "$*"
    exit 1
}

# Check if Java is installed
command -v java &> /dev/null || die "Please install Java"

# Detecting program dir
PROG_DIR="$(cd "$(dirname "${0:-$PWD}")" 2>/dev/null 1>&2 && pwd)"

# Get version and download URL
source "${PROG_DIR}/specscript.conf" || die "Could not read specscript.conf"

# Find target location
SPECSCRIPT_HOME="${HOME}/.specscript/lib"
if [ ! -d "${SPECSCRIPT_HOME}" ] ; then
  mkdir -p "${SPECSCRIPT_HOME}" || die "Could not create SpecScript configuration directory at: $SPECSCRIPT_HOME"
fi

# Download SpecScript jar if it does not exist
SPECSCRIPT_JAR="${SPECSCRIPT_HOME}/specscript-${SPECSCRIPT_VERSION}-full.jar"
if [ ! -f "${SPECSCRIPT_JAR}" ] ; then
  echo "Downloading specscript to ${SPECSCRIPT_JAR}"
  DOWNLOAD_URL="${SPECSCRIPT_BASE_URL}/${SPECSCRIPT_VERSION}/specscript-${SPECSCRIPT_VERSION}-full.jar"
  curl -fLsSo "${SPECSCRIPT_JAR}.$$" "${DOWNLOAD_URL}" || die "Could not download SpecScript jar from: $DOWNLOAD_URL"
  mv "${SPECSCRIPT_JAR}.$$" "${SPECSCRIPT_JAR}" || die "Could not move SpecScript jar to home folder"
fi

# Run SpecScript
exec java -jar ${SPECSCRIPT_JAR} "$@"
