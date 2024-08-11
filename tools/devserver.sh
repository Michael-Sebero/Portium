#!/bin/bash
# Runs the dev server with a console that commands can be written to (runServer doesn't forward stdin, and 1.12's
# RCON closes the socket after every packet).
#
#   tools/devserver.sh start [LEVEL [TYPE [SETTINGS]]]   start on run/LEVEL (default: world), waiting until it's up
#   tools/devserver.sh send CMD...                       run console commands and print the lines they logged
#   tools/devserver.sh stop                              stop the server and restore run/server.properties
#
# State lives in build/devserver/: the java command captured from Gradle's runServer, the console FIFO and the log.
# Build first; the captured command runs build/classes directly.
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
DIR=$ROOT/build/devserver
mkdir -p "$DIR"
PATTERN='^/usr/lib/jvm/[^ ]*/bin/java .*GradleStartServer'

capture() {
    [ -s "$DIR/cmdline" ] && return
    echo "Capturing the server command line from Gradle..."
    (cd "$ROOT" && JAVA_HOME=${JAVA_HOME:-$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2} ./gradlew --no-daemon runServer < /dev/null > "$DIR/gradle.log" 2>&1 &)
    for _ in $(seq 1 300); do
        PID=$(pgrep -f "$PATTERN" | head -1 || true)
        [ -n "$PID" ] && break
        sleep 1
    done
    [ -n "$PID" ] || { echo "runServer didn't start; see $DIR/gradle.log"; exit 1; }
    tr '\0' '\n' < /proc/$PID/cmdline | grep -v '^-Dmixin.debug.export' > "$DIR/cmdline"
    kill "$PID"
    sleep 5
}

case "$1" in
    start)
        capture
        LEVEL=${2:-world}
        cd "$ROOT/run"
        [ -f "$DIR/server.properties.orig" ] || cp server.properties "$DIR/server.properties.orig"
        python3 - "$LEVEL" "${3:-DEFAULT}" "${4:-}" <<'PY'
import sys
out = []
for line in open('server.properties').read().split('\n'):
    if line.startswith('level-name='): line = 'level-name=' + sys.argv[1]
    elif line.startswith('level-type='): line = 'level-type=' + sys.argv[2]
    elif line.startswith('generator-settings='): line = 'generator-settings=' + sys.argv[3].replace('\\', '\\\\')
    out.append(line)
open('server.properties', 'w').write('\n'.join(out))
PY
        [ -p "$DIR/console" ] || mkfifo "$DIR/console"
        mapfile -t ARGS < "$DIR/cmdline"
        nohup "${ARGS[@]}" <> "$DIR/console" > "$DIR/server.log" 2>&1 &
        for _ in $(seq 1 300); do
            grep -q 'Done (' "$DIR/server.log" 2>/dev/null && { grep 'Done (' "$DIR/server.log" | cut -c1-120; exit 0; }
            if ! pgrep -f "$PATTERN" > /dev/null; then echo "The server stopped; see $DIR/server.log"; tail -20 "$DIR/server.log"; exit 1; fi
            sleep 1
        done
        echo "Timed out waiting for the server"; exit 1
        ;;
    send)
        shift
        N=$(wc -l < "$DIR/server.log")
        for c in "$@"; do echo "$c" > "$DIR/console"; sleep "${GAP:-0.3}"; done
        sleep "${WAIT:-0.5}"
        tail -n +$((N + 1)) "$DIR/server.log" | sed 's/^\[[0-9:]*\] \[[^]]*\] \[[^]]*\]: //' | cut -c1-240
        ;;
    stop)
        if pgrep -f "$PATTERN" > /dev/null; then
            echo stop > "$DIR/console"
            for _ in $(seq 1 60); do pgrep -f "$PATTERN" > /dev/null || break; sleep 1; done
        fi
        [ -f "$DIR/server.properties.orig" ] && cp "$DIR/server.properties.orig" "$ROOT/run/server.properties" && rm "$DIR/server.properties.orig"
        echo stopped
        ;;
    *)
        sed -n '2,10p' "$0"
        ;;
esac
