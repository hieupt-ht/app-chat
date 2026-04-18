package com.chatapp.network.tcp;

import com.chatapp.model.Message;
import com.chatapp.util.Constants;
import com.chatapp.util.JSONUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;

/**
 * Handles a single client connection on the server side.
 * Reads JSON messages from the client and processes them.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String line;
            while (running && (line = reader.readLine()) != null) {
                processMessage(line);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[ClientHandler] Connection lost: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    private void processMessage(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String type = obj.optString("type", "");

            switch (type) {
                case Constants.TYPE_LOGIN:
                    handleLogin(obj);
                    break;
                case Constants.TYPE_REGISTER:
                    handleRegister(obj);
                    break;
                case Constants.TYPE_PRIVATE:
                case Constants.TYPE_TEXT:
                case Constants.TYPE_FILE:
                    handlePrivateMessage(json);
                    break;
                case Constants.TYPE_GROUP_MESSAGE:
                    handleGroupMessage(json);
                    break;
                case Constants.TYPE_CREATE_GROUP:
                    handleCreateGroup(obj);
                    break;
                case Constants.TYPE_JOIN_GROUP:
                    handleJoinGroup(obj);
                    break;
                case Constants.TYPE_LEAVE_GROUP:
                    handleLeaveGroup(obj);
                    break;
                case Constants.TYPE_REMOVE_GROUP_MEMBER:
                    handleRemoveGroupMember(obj);
                    break;
                case Constants.TYPE_DELETE_GROUP:
                    handleDeleteGroup(obj);
                    break;
                case Constants.TYPE_LOGOUT:
                    disconnect();
                    break;
                case Constants.TYPE_BAN_USER:
                    handleBanUser(obj);
                    break;
                case Constants.TYPE_DELETE_USER:
                    handleDeleteUser(obj);
                    break;
                case Constants.TYPE_FORCE_LOGOUT:
                    handleForceLogout(obj);
                    break;
                case Constants.TYPE_DELETE_MESSAGE:
                    handleDeleteMessage(obj);
                    break;
                case Constants.TYPE_UNSEND:
                    handleUnsendMessage(obj, json);
                    break;
                case Constants.TYPE_GET_STATS:
                    handleGetStats();
                    break;
                default:
                    System.err.println("[ClientHandler] Unknown type: " + type);
            }
        } catch (Exception e) {
            System.err.println("[ClientHandler] Error parsing: " + e.getMessage());
        }
    }

    private void handleLogin(JSONObject obj) {
        String user = obj.optString("sender", "");
        String pass = obj.optString("content", "");

        JSONObject response = new JSONObject();
        response.put("type", Constants.TYPE_AUTH_RESULT);

        com.chatapp.model.User userObj = server.getUserService().getUser(user);
        
        if (userObj != null && userObj.isBanned()) {
            response.put("content", "LOGIN_FAILED");
            response.put("reason", "BANNED");
            response.put("sender", "SERVER");
        } else if (server.getUserService().authenticate(user, pass)) {
            this.username = user;
            server.registerClient(username, this);
            response.put("content", "LOGIN_SUCCESS");
            response.put("role", userObj.getRole());
            response.put("sender", "SERVER");

            // Send message history
            sendMessageHistory(user);
        } else {
            response.put("content", "LOGIN_FAILED");
            response.put("reason", "INVALID");
            response.put("sender", "SERVER");
        }
        sendMessage(response.toString());
    }

    private void handleRegister(JSONObject obj) {
        String user = obj.optString("sender", "");
        String pass = obj.optString("content", "");

        JSONObject response = new JSONObject();
        response.put("type", Constants.TYPE_AUTH_RESULT);

        if (server.getUserService().register(user, pass)) {
            response.put("content", "REGISTER_SUCCESS");
        } else {
            response.put("content", "REGISTER_FAILED");
        }
        response.put("sender", "SERVER");
        sendMessage(response.toString());
    }

    private void handlePrivateMessage(String json) {
        Message msg = JSONUtils.jsonToMessage(json);
        if (Constants.TYPE_TEXT.equals(msg.getType())) {
            msg.setType(Constants.TYPE_PRIVATE);
        }
        if (msg.getGroupId() != null && !msg.getGroupId().isEmpty()) {
            handleGroupPayload(msg, json);
            return;
        }
        Message historyMessage = new Message(
                msg.getSender(),
                msg.getReceiver(),
                Constants.TYPE_FILE.equals(msg.getType()) && msg.getFileName() != null && !msg.getFileName().isEmpty()
                        ? msg.getFileName()
                        : msg.getContent(),
                msg.getType(),
                msg.getTimestamp());
        historyMessage.setFileName(msg.getFileName());
        server.routeMessage(historyMessage, json);
    }

    private void handleGroupMessage(String json) {
        Message msg = JSONUtils.jsonToMessage(json);
        msg.setType(Constants.TYPE_GROUP_MESSAGE);
        handleGroupPayload(msg, json);
    }

    private void handleGroupPayload(Message msg, String outboundJson) {
        Message historyMessage = new Message(
                msg.getSender(),
                "",
                Constants.TYPE_FILE.equals(msg.getType()) && msg.getFileName() != null && !msg.getFileName().isEmpty()
                        ? msg.getFileName()
                        : msg.getContent(),
                msg.getType(),
                msg.getTimestamp());
        historyMessage.setGroupId(msg.getGroupId());
        historyMessage.setFileName(msg.getFileName());
        server.routeGroupMessage(historyMessage, outboundJson);
    }

    private void handleCreateGroup(JSONObject obj) {
        String groupName = obj.optString("groupName", "").trim();
        org.json.JSONArray memberArr = obj.optJSONArray("members");
        java.util.List<String> members = new java.util.ArrayList<>();
        if (memberArr != null) {
            for (int i = 0; i < memberArr.length(); i++) {
                String member = memberArr.optString(i, "").trim();
                if (!member.isEmpty() && !members.contains(member) && server.getUserService().getUser(member) != null) {
                    members.add(member);
                }
            }
        }
        if (username != null && !members.contains(username)) {
            members.add(username);
        }
        server.getGroupService().createGroup(groupName, members);
        server.broadcastGroupLists();
    }

    private void handleJoinGroup(JSONObject obj) {
        String groupId = obj.optString("groupId", "");
        String targetUser = obj.optString("target", username);
        if (server.getUserService().getUser(targetUser) != null && server.getGroupService().joinGroup(groupId, targetUser)) {
            server.broadcastGroupLists();
        }
    }

    private void handleLeaveGroup(JSONObject obj) {
        String groupId = obj.optString("groupId", "");
        String targetUser = obj.optString("target", username);
        if (server.getGroupService().leaveGroup(groupId, targetUser)) {
            server.broadcastGroupLists();
        }
    }

    private void handleRemoveGroupMember(JSONObject obj) {
        String groupId = obj.optString("groupId", "");
        String targetUser = obj.optString("target", "");
        if (server.getGroupService().removeMember(groupId, targetUser)) {
            server.broadcastGroupLists();
            
            // If it's a lobby kick, broadcast to everyone so they see the kick message
            if (Constants.LOBBY_GROUP_ID.equals(groupId)) {
                server.broadcastRoomEvent(targetUser, "ROOM_KICK");
            }
        }
    }

    private void handleDeleteGroup(JSONObject obj) {
        String groupId = obj.optString("groupId", "");
        if (Constants.LOBBY_GROUP_ID.equals(groupId)) {
            return; // Prevent deleting the Lobby group
        }
        if (server.getGroupService().deleteGroup(groupId)) {
            server.getMessageService().deleteGroupMessages(groupId);
            server.broadcastGroupLists();
        }
    }

    private void handleBanUser(JSONObject obj) {
        String target = obj.optString("target", "");
        boolean ban = obj.optBoolean("ban", true);
        server.getUserService().banUser(target, ban);
        if (ban) {
            server.forceLogout(target);
        }
    }

    private void handleDeleteUser(JSONObject obj) {
        String target = obj.optString("target", "");
        server.getUserService().deleteUser(target);
        server.forceLogout(target);
    }

    private void handleForceLogout(JSONObject obj) {
        String target = obj.optString("target", "");
        server.forceLogout(target);
    }

    private void handleDeleteMessage(JSONObject obj) {
        long timestamp = obj.optLong("timestamp", 0);
        String sender = obj.optString("msgSender", "");
        String receiver = obj.optString("msgReceiver", "");
        String groupId = obj.optString("groupId", "");
        server.getMessageService().deleteMessage(timestamp, sender, receiver, groupId);
    }

    private void handleUnsendMessage(JSONObject obj, String originalJson) {
        long timestamp = obj.optLong("timestamp", 0);
        String sender = obj.optString("msgSender", "");
        String receiver = obj.optString("msgReceiver", "");
        String groupId = obj.optString("groupId", "");
        // Update database
        server.getMessageService().unsendMessage(timestamp, sender, receiver, groupId);
        
        // Broadcast the update to receiver or group members for live UI update
        Message historyMessage = new Message(sender, receiver, "Tin nhắn đã bị thu hồi", Constants.TYPE_UNSEND, timestamp);
        historyMessage.setUnsent(true);
        historyMessage.setGroupId(groupId);
        if (groupId != null && !groupId.isEmpty()) {
            server.routeGroupMessage(historyMessage, JSONUtils.messageToJSON(historyMessage));
        } else {
            server.routeMessage(historyMessage, JSONUtils.messageToJSON(historyMessage));
        }
    }

    private void handleGetStats() {
        JSONObject response = new JSONObject();
        response.put("type", Constants.TYPE_STATS_RESULT);
        response.put("onlineCount", server.getOnlineCount());
        response.put("totalUsers", server.getUserService().getAllUsers().size());
        response.put("totalMessages", server.getTotalMessages());
        
        // Also attach the full user list and message list for the admin panels
        response.put("users", new org.json.JSONArray(JSONUtils.userListToJSON(server.getUserService().getAllUsers())));
        response.put("messages", new org.json.JSONArray(JSONUtils.messageListToJSON(server.getMessageService().getAllMessages())));
        response.put("groups", new org.json.JSONArray(JSONUtils.groupListToJSON(server.getGroupService().getAllGroups())));
        
        sendMessage(response.toString());
    }

    private void sendMessageHistory(String user) {
        // Send lobby (public room) chat history
        java.util.List<com.chatapp.model.Message> lobbyHistory =
                server.getMessageService().getGroupHistory(Constants.LOBBY_GROUP_ID);
        for (com.chatapp.model.Message msg : lobbyHistory) {
            sendMessage(JSONUtils.messageToJSON(msg));
        }
    }

    /**
     * Sends a JSON string to this client.
     */
    public synchronized void sendMessage(String json) {
        if (writer != null) {
            writer.println(json);
        }
    }

    public void disconnect() {
        running = false;
        if (username != null) {
            server.unregisterClient(username);
        }
        try {
            if (reader != null)
                reader.close();
        } catch (IOException ignored) {
        }
        try {
            if (writer != null)
                writer.close();
        } catch (Exception ignored) {
        }
        try {
            if (socket != null)
                socket.close();
        } catch (IOException ignored) {
        }
    }

    public String getUsername() {
        return username;
    }
}
