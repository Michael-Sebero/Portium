#!/bin/bash
# Runs each argument as a chat command in a dev client on $DISPLAY (default :0.0), then screenshots the game window.
# Usage: tools/chat.sh "/time set day" "/tp @s 0 100 0"   (WAIT=seconds before the screenshot, SHOT=output png)
# Needs tools/Bot.class: javac --release 8 tools/Bot.java
TOOLS=$(cd "$(dirname "$0")" && pwd)
export DISPLAY=${DISPLAY:-:0.0}
for c in "$@"; do
  java -cp "$TOOLS" Bot key T wait 300 type "$c" key ENTER wait 250
done
sleep "${WAIT:-1}"
# The game window sits at 530,290 on a 1920x1080 screen; adjust CROP for other layouts.
import -window root -crop "${CROP:-860x500+530+290}" +repage "${SHOT:-shot.png}"
