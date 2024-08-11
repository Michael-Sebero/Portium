#!/bin/bash
# Generates areas of a world with Portium's dev server, for comparing with vanilla 1.13.2 (tools/vanilla113.sh and
# tools/oceancompare.py):
#   tools/portiumgen.sh LEVEL SEED "X1 Z1 X2 Z2" ...
# Force-loads each area (block corners), waits for generation, saves and stops. Keeps an existing level (delete
# run/LEVEL first for a fresh one). Sets run/server.properties' level-seed for the run and empties it afterwards.
cd "$(dirname "$0")/.." || exit 1
LEVEL=$1; SEED=$2; shift 2
sed -i "s/^level-seed=.*/level-seed=$SEED/" run/server.properties
timeout 500 tools/devserver.sh start "$LEVEL" | tail -1
sleep 5
for a in "$@"; do timeout 120 tools/devserver.sh send "forceload add $a" | tail -1 | cut -c1-120; sleep 2; done
sleep 150
timeout 120 tools/devserver.sh send "save-all flush" | tail -1
timeout 60 tools/devserver.sh stop | tail -1
sed -i 's/^level-seed=.*/level-seed=/' run/server.properties
