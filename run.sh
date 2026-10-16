#!/bin/bash
# Compiles and runs the AWT Donation Management System (Linux / macOS / Git Bash)
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java") || exit 1
java -Dfile.encoding=UTF-8 -cp out com.awt.donation.Main
