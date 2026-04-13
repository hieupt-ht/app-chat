#!/bin/bash
# Build the ChatApp project
echo "╔══════════════════════════════════╗"
echo "║    Building ChatApp...           ║"
echo "╚══════════════════════════════════╝"

cd "$(dirname "$0")"
mkdir -p out

javac -cp "lib/*" -d out -encoding UTF-8 $(find src -name "*.java")

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "To start the server:  ./run-server.sh"
    echo "To start a client:    ./run-client.sh"
    echo "To start a client:    ./run-client.sh <server-ip>"
else
    echo "❌ Build failed!"
    exit 1
fi
