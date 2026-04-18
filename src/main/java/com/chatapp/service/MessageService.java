package com.chatapp.service;

import com.chatapp.model.Message;
import com.chatapp.util.Constants;
import com.chatapp.util.JSONUtils;
import org.json.JSONArray;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for message persistence.
 * Stores message history in per-conversation JSON files.
 */
public class MessageService {

    public MessageService() {
        File dir = new File(Constants.MESSAGES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Saves a message to the conversation file between sender and receiver.
     */
    public synchronized void saveMessage(Message msg) {
        String filename = getStorageFilename(msg);
        List<Message> history = loadMessages(filename);
        history.add(msg);

        try {
            JSONArray arr = new JSONArray();
            for (Message m : history) {
                arr.put(new org.json.JSONObject(JSONUtils.messageToJSON(m)));
            }

            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(filename), StandardCharsets.UTF_8)) {
                writer.write(arr.toString(2));
            }
        } catch (Exception e) {
            System.err.println("[MessageService] Error saving message: " + e.getMessage());
        }
    }

    /**
     * Loads message history between two users.
     */
    public List<Message> getHistory(String user1, String user2) {
        String filename = getConversationFilename(user1, user2);
        return loadMessages(filename);
    }

    public List<Message> getGroupHistory(String groupId) {
        return loadMessages(getGroupFilename(groupId));
    }

    public synchronized void deleteMessage(long timestamp, String sender, String receiver) {
        deleteMessage(timestamp, sender, receiver, null);
    }

    public synchronized void unsendMessage(long timestamp, String sender, String receiver, String groupId) {
        String filename = groupId != null && !groupId.isEmpty()
                ? getGroupFilename(groupId)
                : getConversationFilename(sender, receiver);
        List<Message> history = loadMessages(filename);
        
        boolean updated = false;
        for (Message m : history) {
            if (m.getTimestamp() == timestamp && m.getSender().equals(sender)) {
                m.setUnsent(true);
                m.setContent("Tin nhắn đã bị thu hồi"); // Localized unsend message
                updated = true;
                break;
            }
        }
        
        if (updated) {
            try {
                JSONArray arr = new JSONArray();
                for (Message m : history) {
                    arr.put(new org.json.JSONObject(JSONUtils.messageToJSON(m)));
                }

                try (Writer writer = new OutputStreamWriter(
                        new FileOutputStream(filename), StandardCharsets.UTF_8)) {
                    writer.write(arr.toString(2));
                }
            } catch (Exception e) {
                System.err.println("[MessageService] Error unsending message: " + e.getMessage());
            }
        }
    }

    public synchronized void deleteMessage(long timestamp, String sender, String receiver, String groupId) {
        String filename = groupId != null && !groupId.isEmpty()
                ? getGroupFilename(groupId)
                : getConversationFilename(sender, receiver);
        List<Message> history = loadMessages(filename);
        boolean removed = history.removeIf(m -> m.getTimestamp() == timestamp && m.getSender().equals(sender));
        
        if (removed) {
            try {
                JSONArray arr = new JSONArray();
                for (Message m : history) {
                    arr.put(new org.json.JSONObject(JSONUtils.messageToJSON(m)));
                }

                try (Writer writer = new OutputStreamWriter(
                        new FileOutputStream(filename), StandardCharsets.UTF_8)) {
                    writer.write(arr.toString(2));
                }
            } catch (Exception e) {
                System.err.println("[MessageService] Error deleting message: " + e.getMessage());
            }
        }
    }

    public synchronized void deleteGroupMessages(String groupId) {
        File file = new File(getGroupFilename(groupId));
        if (file.exists() && !file.delete()) {
            System.err.println("[MessageService] Could not delete group history for " + groupId);
        }
    }

    public List<Message> getAllMessages() {
        List<Message> allMessages = new ArrayList<>();
        File dir = new File(Constants.MESSAGES_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    allMessages.addAll(loadMessages(file.getAbsolutePath()));
                }
            }
        }
        allMessages.sort((m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
        return allMessages;
    }

    private List<Message> loadMessages(String filename) {
        List<Message> messages = new ArrayList<>();
        try {
            File file = new File(filename);
            if (!file.exists())
                return messages;

            String content = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
            if (content.trim().isEmpty())
                return messages;

            return JSONUtils.jsonToMessageList(content);
        } catch (Exception e) {
            System.err.println("[MessageService] Error loading messages: " + e.getMessage());
        }
        return messages;
    }

    /**
     * Creates a consistent filename for a conversation between two users.
     * Sorts names alphabetically so the file is the same regardless of who sends.
     */
    private String getConversationFilename(String user1, String user2) {
        String a = user1.compareTo(user2) < 0 ? user1 : user2;
        String b = user1.compareTo(user2) < 0 ? user2 : user1;
        return Constants.MESSAGES_DIR + "/" + a + "_" + b + ".json";
    }

    private String getGroupFilename(String groupId) {
        return Constants.MESSAGES_DIR + "/group_" + groupId + ".json";
    }

    private String getStorageFilename(Message msg) {
        if (msg.getGroupId() != null && !msg.getGroupId().isEmpty()) {
            return getGroupFilename(msg.getGroupId());
        }
        return getConversationFilename(msg.getSender(), msg.getReceiver());
    }
}
