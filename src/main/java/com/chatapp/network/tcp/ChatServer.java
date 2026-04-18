package com.chatapp.network.tcp;

import com.chatapp.model.Group;
import com.chatapp.model.Message;
import com.chatapp.service.GroupService;
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
    private final GroupService groupService;
    private volatile boolean running = true;
    private long totalMessages = 0;

    public ChatServer() {
        this.threadPool = Executors.newCachedThreadPool();
        this.userService = new UserService();
        this.messageService = new MessageService();
        this.groupService = new GroupService();
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

        // Auto-join lobby
        groupService.joinGroup(Constants.LOBBY_GROUP_ID, username);

        broadcastUserList();
        sendGroupListToUser(username);

        // Broadcast join notification to lobby
        broadcastRoomEvent(username, Constants.TYPE_ROOM_JOIN);

        System.out.println("[Server] " + username + " logged in. Online: " + onlineClients.size());
    }

    public void unregisterClient(String username) {
        onlineClients.remove(username);
        userService.setOnline(username, false);

        // Broadcast leave notification to lobby before removing
        broadcastRoomEvent(username, Constants.TYPE_ROOM_LEAVE);

        // Auto-leave lobby
        groupService.leaveGroup(Constants.LOBBY_GROUP_ID, username);

        broadcastUserList();
        System.out.println("[Server] " + username + " logged out. Online: " + onlineClients.size());
    }

    /**
     * Broadcasts a join/leave system notification to all online lobby members.
     */
    public void broadcastRoomEvent(String username, String eventType) {
        String text;
        if (Constants.TYPE_ROOM_JOIN.equals(eventType)) text = username + " joined the room";
        else if (Constants.TYPE_ROOM_LEAVE.equals(eventType)) text = username + " left the room";
        else text = username + " was kicked out by Admin";

        JSONObject notification = new JSONObject();
        notification.put("type", eventType);
        notification.put("sender", "SYSTEM");
        notification.put("content", text);
        notification.put("groupId", Constants.LOBBY_GROUP_ID);
        notification.put("timestamp", System.currentTimeMillis());
        String json = notification.toString();

        com.chatapp.model.Group lobby = groupService.getGroup(Constants.LOBBY_GROUP_ID);
        if (lobby != null) {
            for (String member : lobby.getMembers()) {
                if (member.equals(username)) continue;
                ClientHandler h = onlineClients.get(member);
                if (h != null) {
                    h.sendMessage(json);
                }
            }
        }

        // Also persist this system event in the room's message history
        Message historyMessage = new Message(
                "SYSTEM",
                "",
                text,
                eventType,
                notification.optLong("timestamp"));
        historyMessage.setGroupId(Constants.LOBBY_GROUP_ID);
        messageService.saveMessage(historyMessage);
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

    public void routeGroupMessage(Message msg, String outboundJson) {
        Group group = groupService.getGroup(msg.getGroupId());
        if (group == null || !group.getMembers().contains(msg.getSender())) {
            return;
        }

        messageService.saveMessage(msg);
        totalMessages++;

        for (String member : group.getMembers()) {
            if (member.equals(msg.getSender())) {
                continue;
            }
            ClientHandler handler = onlineClients.get(member);
            if (handler != null) {
                handler.sendMessage(outboundJson);
            }
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

    public void broadcastGroupLists() {
        for (String username : onlineClients.keySet()) {
            sendGroupListToUser(username);
        }
    }

    public void sendGroupListToUser(String username) {
        ClientHandler handler = onlineClients.get(username);
        if (handler == null) {
            return;
        }

        JSONObject response = new JSONObject();
        response.put("type", Constants.TYPE_GROUP_LIST);
        response.put("groups", new JSONArray(JSONUtils.groupListToJSON(groupService.getGroupsForUser(username))));
        handler.sendMessage(response.toString());
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

    public GroupService getGroupService() {
        return groupService;
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
