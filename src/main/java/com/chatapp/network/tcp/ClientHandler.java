package com.chatapp.network.tcp;

import com.chatapp.model.Message;
import com.chatapp.util.Constants;
import com.chatapp.util.JSONUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.List;

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
        server.getMessageService().deleteMessage(timestamp, sender, receiver);
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
        
        sendMessage(response.toString());
    }

    private void sendMessageHistory(String user) {
        // We don't know who the user will chat with yet,
        // so history is loaded on demand when a conversation is selected.
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
