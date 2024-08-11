#!/bin/bash
# Generates a world with Mojang's 1.13.2 server, for testing how Portium loads 1.13 worlds.
#   tools/vanilla113.sh LEVEL [TYPE [SETTINGS]] [-- CONSOLE COMMANDS...]   ("sleep N" between commands waits N seconds)
# Needs build/mc113/server.jar (1.13.2's official server; ask before downloading it), or else runs the server inside
# the 1.13.2 client jar Portium already downloads (run/portium/forge-1.13.2/), with libraries from Gradle's cache and
# fastutil 8.2.1 in build/mc113/libs. Its EULA must be accepted in build/mc113/eula.txt. Runs on port 25566 so Portium's dev
# server can stay up, then stops and leaves build/mc113/LEVEL behind.
set -e
DIR=$(cd "$(dirname "$0")/.." && pwd)/build/mc113
cd "$DIR"
LEVEL=$1
TYPE=${2:-default}
SETTINGS=${3:-}
shift $(( $# < 3 ? $# : 3 ))
[ "$1" = "--" ] && shift
python3 -I - "$LEVEL" "$TYPE" "$SETTINGS" <<'PY'
import os, sys
keep = ('online-mode=', 'spawn-protection=', 'enable-command-block=', 'max-tick-time=', 'spawn-monsters=', 'difficulty=')
lines = [l for l in open('server.properties').read().split('\n') if l.startswith(keep)] if os.path.exists('server.properties') else []
lines += ['server-port=25566', 'level-name=' + sys.argv[1], 'level-type=' + sys.argv[2], 'generator-settings=' + sys.argv[3]]
open('server.properties', 'w').write('\n'.join(lines) + '\n')
PY
[ -p console ] || mkfifo console
: > server.log
if [ -f server.jar ]; then
    nohup java -Xmx2G -jar server.jar nogui <> console > server.log 2>&1 &
else
    RUNTIME=$DIR/../../run/portium/forge-1.13.2
    CACHE=$HOME/.gradle/caches/modules-2/files-2.1
    lib() { find "$CACHE/$1" -name "$2" ! -name '*sources*' | head -1; }
    # 1.13.2 needs fastutil 8.2.1 (build/mc113/libs, from Maven Central); fastutil 7 can't stand in.
    CP=$DIR/libs/fastutil-8.2.1.jar
    CP=$CP:$RUNTIME/minecraft-1.13.2-client.jar:$RUNTIME/brigadier-1.0.14.jar:$RUNTIME/datafixerupper-1.0.19.jar
    for spec in "com.mojang/authlib authlib-1.5.25.jar" "net.sf.jopt-simple/jopt-simple jopt-simple-5.0.3.jar" \
            "io.netty/netty-all netty-all-4.1.9.Final.jar" "commons-io/commons-io commons-io-2.5.jar" \
            "com.google.guava/guava guava-21.0.jar" "org.apache.commons/commons-lang3 commons-lang3-3.5.jar" "com.google.code.gson/gson gson-2.8.0.jar" \
            "org.apache.logging.log4j/log4j-api log4j-api-2.17.1.jar" "org.apache.logging.log4j/log4j-core log4j-core-2.17.1.jar" \
            "com.google.code.findbugs/jsr305 jsr305-3.0.2.jar"; do
        read -r group jar <<< "$spec"
        CP=$CP:$(lib "$group" "$jar")
    done
    nohup java -Xmx2G -cp "$CP" net.minecraft.server.MinecraftServer nogui <> console > server.log 2>&1 &
fi
PID=$!
for _ in $(seq 1 240); do
    grep -q 'Done (' server.log && break
    kill -0 $PID 2>/dev/null || { tail -20 server.log; exit 1; }
    sleep 1
done
# "sleep N" waits (for force-loaded chunks to generate); anything else is a console command.
for c in "$@"; do
    case "$c" in
        "sleep "*) sleep "${c#sleep }" ;;
        *) echo "$c" > console; sleep 0.3 ;;
    esac
done
echo "save-all flush" > console
sleep 2
echo stop > console
for _ in $(seq 1 60); do kill -0 $PID 2>/dev/null || break; sleep 1; done
grep -v 'Preparing spawn area\|Saving chunks\|ThreadedAnvilChunkStorage' server.log | sed 's/^\[[0-9:]*\] \[[^]]*\]: //' | tail -n +4 | cut -c1-200
