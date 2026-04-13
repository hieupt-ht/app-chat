#!/bin/bash
# Start the ChatApp TCP Server
cd "$(dirname "$0")"
java -cp "out:lib/*" com.chatapp.network.tcp.ChatServer
