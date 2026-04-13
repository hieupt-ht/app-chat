package com.chatapp.service;

import com.chatapp.model.User;
import com.chatapp.util.Constants;
import com.chatapp.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for user management: registration, authentication, online status.
 * Uses file-based JSON storage.
 */
public class UserService {

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    public UserService() {
        loadUsers();
    }

    /**
     * Registers a new user. Password is hashed with SHA-256 before storage.
     * 
     * @return true if registration successful, false if username already exists.
     */
    public synchronized boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty())
            return false;
        if (password == null || password.trim().isEmpty())
            return false;
        if (users.containsKey(username))
            return false;

        User user = new User(username, hashPassword(password));
        if ("admin".equalsIgnoreCase(username)) {
            user.setRole(Constants.ROLE_ADMIN);
        }
        users.put(username, user);
        saveUsers();
        return true;
    }

    /**
     * Authenticates a user by checking hashed password.
     */
    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        if (user == null)
            return false;
        return user.getPassword().equals(hashPassword(password));
    }

    public void setOnline(String username, boolean online) {
        User user = users.get(username);
        if (user != null) {
            user.setOnline(online);
        }
    }

    public List<String> getOnlineUsernames() {
        List<String> online = new ArrayList<>();
        for (User u : users.values()) {
            if (u.isOnline()) {
                online.add(u.getUsername());
            }
        }
        return online;
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized void banUser(String username, boolean ban) {
        User user = users.get(username);
        if (user != null) {
            user.setBanned(ban);
            saveUsers();
        }
    }

    public synchronized void deleteUser(String username) {
        if (users.remove(username) != null) {
            saveUsers();
        }
    }

    // ───── Persistence ─────

    private void loadUsers() {
        try {
            File file = new File(Constants.USERS_FILE);
            if (!file.exists())
                return;

            String content = new String(Files.readAllBytes(Paths.get(Constants.USERS_FILE)), StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(content);
            for (int i = 0; i < arr.length(); i++) {
                User user = JSONUtils.jsonToUser(arr.getJSONObject(i).toString());
                user.setOnline(false); // Everyone starts offline on server restart
                users.put(user.getUsername(), user);
            }
            System.out.println("[UserService] Loaded " + users.size() + " users.");
        } catch (Exception e) {
            System.err.println("[UserService] Error loading users: " + e.getMessage());
        }
    }

    private void saveUsers() {
        try {
            File dir = new File(Constants.DATA_DIR);
            if (!dir.exists())
                dir.mkdirs();

            JSONArray arr = new JSONArray();
            for (User u : users.values()) {
                arr.put(JSONUtils.userToJSONObject(u));
            }

            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(Constants.USERS_FILE), StandardCharsets.UTF_8)) {
                writer.write(arr.toString(2));
            }
        } catch (Exception e) {
            System.err.println("[UserService] Error saving users: " + e.getMessage());
        }
    }

    // ───── Password hashing ─────

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
