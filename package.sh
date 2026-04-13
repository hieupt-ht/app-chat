#!/bin/bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$ROOT_DIR/dist"
TMP_DIR="$ROOT_DIR/.package-tmp"

echo "╔══════════════════════════════════╗"
echo "║   Packaging ChatApp JAR files   ║"
echo "╚══════════════════════════════════╝"

"$ROOT_DIR/build.sh"

rm -rf "$DIST_DIR" "$TMP_DIR"
mkdir -p "$DIST_DIR/client" "$DIST_DIR/server" "$TMP_DIR/deps"

cp -R "$ROOT_DIR/out/." "$DIST_DIR/client/"
cp -R "$ROOT_DIR/out/." "$DIST_DIR/server/"

for dep in "$ROOT_DIR"/lib/*.jar; do
    [ -f "$dep" ] || continue
    dep_name="$(basename "$dep" .jar)"
    dep_dir="$TMP_DIR/deps/$dep_name"
    mkdir -p "$dep_dir"
    (
        cd "$dep_dir"
        jar xf "$dep"
    )
    rm -rf "$dep_dir/META-INF"
    cp -R "$dep_dir/." "$DIST_DIR/client/"
    cp -R "$dep_dir/." "$DIST_DIR/server/"
done

(
    cd "$DIST_DIR/client"
    jar cfe "$DIST_DIR/ChatApp-client.jar" com.chatapp.App .
)

(
    cd "$DIST_DIR/server"
    jar cfe "$DIST_DIR/ChatApp-server.jar" com.chatapp.network.tcp.ChatServer .
)

rm -rf "$DIST_DIR/client" "$DIST_DIR/server" "$TMP_DIR"

echo ""
echo "Created:"
echo "  $DIST_DIR/ChatApp-client.jar"
echo "  $DIST_DIR/ChatApp-server.jar"
