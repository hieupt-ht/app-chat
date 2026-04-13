package com.chatapp.network.tcp;

import com.chatapp.model.Message;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import com.chatapp.util.Constants;
import com.chatapp.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-threaded TCP chat server.
 * Handles client connections, authentication, and message routing.
 */
public class ChatServer {

    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final ConcurrentHashMap<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();
    private final UserService userService;
    private final MessageService messageService;
    private volatile boolean running = true;
    private long totalMessages = 0;

    public ChatServer() {
        this.threadPool = Executors.newCachedThreadPool();
        this.userService = new UserService();
        this.messageService = new MessageService();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(Constants.TCP_PORT);
            System.out.println("[Server] ChatServer started on port " + Constants.TCP_PORT);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] New connection from " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[Server] Error: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            // ignore
        }
        threadPool.shutdownNow();
    }

    // ───── Client management ─────

    public void registerClient(String username, ClientHandler handler) {
        onlineClients.put(username, handler);
        userService.setOnline(username, true);
        broadcastUserList();
        System.out.println("[Server] " + username + " logged in. Online: " + onlineClients.size());
    }

    public void unregisterClient(String username) {
        onlineClients.remove(username);
        userService.setOnline(username, false);
        broadcastUserList();
        System.out.println("[Server] " + username + " logged out. Online: " + onlineClients.size());
    }

    /**
     * Routes a private message to the target client.
     */
    public void routeMessage(Message msg) {
        routeMessage(msg, JSONUtils.messageToJSON(msg));
    }

    public void routeMessage(Message msg, String outboundJson) {
        // Save to history
        messageService.saveMessage(msg);
        totalMessages++;

        // Forward to recipient if online
        ClientHandler recipient = onlineClients.get(msg.getReceiver());
        if (recipient != null) {
            recipient.sendMessage(outboundJson);
        }
    }

    /**
     * Sends the current online user list to all connected clients.
     */
    private void broadcastUserList() {
        JSONObject listMsg = new JSONObject();
        listMsg.put("type", Constants.TYPE_USER_LIST);
        JSONArray users = new JSONArray();
        for (String username : onlineClients.keySet()) {
            users.put(username);
        }
        listMsg.put("users", users);
        String json = listMsg.toString();

        for (ClientHandler handler : onlineClients.values()) {
            handler.sendMessage(json);
        }
    }

    public void forceLogout(String username) {
        ClientHandler handler = onlineClients.get(username);
        if (handler != null) {
            handler.disconnect();
        }
    }

    public int getOnlineCount() {
        return onlineClients.size();
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    // ───── Service access ─────

    public UserService getUserService() {
        return userService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    // ───── Main entry point for standalone server ─────

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║       ChatApp Server v1.0        ║");
        System.out.println("╚══════════════════════════════════╝");
        ChatServer server = new ChatServer();

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Server] Shutting down...");
            server.stop();
        }));

        server.start();
    }
}
