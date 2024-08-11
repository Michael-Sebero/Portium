#!/bin/bash
# Generates a world with Mojang's 1.16.5 server, for comparing Portium's 1.16.5 generation against (tools/nethercompare.py).
#   tools/vanilla116.sh LEVEL SEED [-- CONSOLE COMMANDS...]   ("sleep N" between commands waits N seconds)
# Needs build/mc116/server.jar (1.16.5's official server, sha1 1b557e7b033b583cd9f66746b7a9ab1ec1673ced; ask before
# downloading it) and its EULA accepted in build/mc116/eula.txt. Runs on port 25566 so Portium's dev server can stay up,
# then saves, stops and leaves build/mc116/LEVEL behind. Example, a Nether area:
#   tools/vanilla116.sh nether116 12345 -- "execute in minecraft:the_nether run forceload add -96 -96 95 95" "sleep 120"
set -e
DIR=$(cd "$(dirname "$0")/.." && pwd)/build/mc116
cd "$DIR"
[ -f server.jar ] || { echo "build/mc116/server.jar is missing"; exit 1; }
LEVEL=$1
SEED=$2
shift 2
[ "$1" = "--" ] && shift
printf 'server-port=25566\nonline-mode=false\nlevel-name=%s\nlevel-seed=%s\nspawn-protection=0\nmax-tick-time=-1\n' "$LEVEL" "$SEED" > server.properties
[ -p console ] || mkfifo console
: > server.log
nohup java -Xmx2G -jar server.jar nogui <> console > server.log 2>&1 &
PID=$!
for _ in $(seq 1 240); do
    grep -q 'Done (' server.log && break
    kill -0 $PID 2>/dev/null || { tail -20 server.log; exit 1; }
    sleep 1
done
for c in "$@"; do
    case "$c" in
        "sleep "*) sleep "${c#sleep }" ;;
        *) echo "$c" > console; sleep 0.3 ;;
    esac
done
echo "save-all flush" > console
sleep 10
echo stop > console
for _ in $(seq 1 60); do kill -0 $PID 2>/dev/null || break; sleep 1; done
grep -v 'Preparing spawn area\|ThreadedAnvilChunkStorage' server.log | sed 's/^\[[0-9:]*\] \[[^]]*\]: //' | tail -n +4 | cut -c1-200
