#!/bin/bash
# Start the ChatApp Client
# Usage: ./run-client.sh [server-ip]
# Default server: localhost
cd "$(dirname "$0")"
java -cp "out:lib/*" com.chatapp.App ${1:-localhost}
