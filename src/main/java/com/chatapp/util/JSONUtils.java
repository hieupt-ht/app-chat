package com.chatapp.util;

import com.chatapp.model.Message;
import com.chatapp.model.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JSONUtils {

    // ───── Message ↔ JSON ─────

    public static String messageToJSON(Message msg) {
        JSONObject obj = new JSONObject();
        obj.put("type", msg.getType());
        obj.put("sender", msg.getSender());
        obj.put("receiver", msg.getReceiver());
        obj.put("content", msg.getContent());
        obj.put("timestamp", msg.getTimestamp());
        if (msg.getFileName() != null && !msg.getFileName().isEmpty()) {
            obj.put("fileName", msg.getFileName());
        }
        if (msg.getFileData() != null && !msg.getFileData().isEmpty()) {
            obj.put("fileData", msg.getFileData());
        }
        return obj.toString();
    }

    public static Message jsonToMessage(String json) {
        JSONObject obj = new JSONObject(json);
        Message message = new Message(
            obj.optString("sender", ""),
            obj.optString("receiver", ""),
            obj.optString("content", ""),
            obj.optString("type", Constants.TYPE_TEXT),
            obj.optLong("timestamp", System.currentTimeMillis())
        );
        message.setFileName(obj.optString("fileName", ""));
        message.setFileData(obj.optString("fileData", ""));
        return message;
    }

    // ───── User ↔ JSON ─────

    public static String userToJSON(User user) {
        JSONObject obj = new JSONObject();
        obj.put("username", user.getUsername());
        obj.put("password", user.getPassword());
        obj.put("ipAddress", user.getIpAddress());
        obj.put("port", user.getPort());
        obj.put("online", user.isOnline());
        obj.put("role", user.getRole());
        obj.put("isBanned", user.isBanned());
        return obj.toString();
    }

    public static User jsonToUser(String json) {
        JSONObject obj = new JSONObject(json);
        User user = new User(
            obj.optString("username", ""),
            obj.optString("password", "")
        );
        user.setIpAddress(obj.optString("ipAddress", ""));
        user.setPort(obj.optInt("port", 0));
        user.setOnline(obj.optBoolean("online", false));
        user.setRole(obj.optString("role", com.chatapp.util.Constants.ROLE_USER));
        user.setBanned(obj.optBoolean("isBanned", false));
        return user;
    }

    public static JSONObject userToJSONObject(User user) {
        return new JSONObject(userToJSON(user));
    }

    // ───── List helpers ─────

    public static String userListToJSON(List<User> users) {
        JSONArray arr = new JSONArray();
        for (User u : users) {
            arr.put(new JSONObject(userToJSON(u)));
        }
        return arr.toString();
    }

    public static List<User> jsonToUserList(String json) {
        List<User> users = new ArrayList<>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            users.add(jsonToUser(arr.getJSONObject(i).toString()));
        }
        return users;
    }

    public static String messageListToJSON(List<Message> messages) {
        JSONArray arr = new JSONArray();
        for (Message m : messages) {
            arr.put(new JSONObject(messageToJSON(m)));
        }
        return arr.toString();
    }

    public static List<Message> jsonToMessageList(String json) {
        List<Message> messages = new ArrayList<>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            messages.add(jsonToMessage(arr.getJSONObject(i).toString()));
        }
        return messages;
    }
}
