#!/usr/bin/env bash
set -e

echo "==============================================="
echo "AoTD Industry Patch Installer"
echo "MAKE SURE YOU BACKUP starfarer.api.jar MANUALLY"
echo "(Copy it to a safe backup folder before running)"
echo "==============================================="
echo

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

PATCHER_JAR="$SCRIPT_DIR/IndustryPatcher.jar"
ASM_JAR="$SCRIPT_DIR/asm-9.1.jar"
API_JAR="$SCRIPT_DIR/starfarer.api.jar"

for f in "$PATCHER_JAR" "$ASM_JAR" "$API_JAR"; do
  if [[ ! -f "$f" ]]; then
    echo "Missing $(basename "$f")"
    read -p "Press ENTER to exit"
    exit 1
  fi
done

cd "$SCRIPT_DIR"

echo "Running patcher..."
java -cp "$ASM_JAR:$PATCHER_JAR:$API_JAR" IndustryPatch

if [[ ! -f starfarer.api.jar.tmp ]]; then
  echo "Temporary patched jar not found!"
  read -p "Press ENTER to exit"
  exit 1
fi

echo "Replacing original jar..."
rm -f starfarer.api.jar
mv starfarer.api.jar.tmp starfarer.api.jar

echo
echo "Patch completed successfully."
read -p "Press ENTER to exit"
