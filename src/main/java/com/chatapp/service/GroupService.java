package com.chatapp.service;

import com.chatapp.model.Group;
import com.chatapp.util.Constants;
import com.chatapp.util.JSONUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GroupService {

    private final ConcurrentHashMap<String, Group> groups = new ConcurrentHashMap<>();

    public GroupService() {
        loadGroups();
        ensureLobby();
    }

    /**
     * Ensures the lobby (public room) group exists.
     */
    public synchronized void ensureLobby() {
        if (!groups.containsKey(Constants.LOBBY_GROUP_ID)) {
            Group lobby = new Group(Constants.LOBBY_GROUP_ID, Constants.LOBBY_GROUP_NAME, new ArrayList<>());
            groups.put(Constants.LOBBY_GROUP_ID, lobby);
            saveGroups();
        }
    }

    public synchronized Group createGroup(String groupName, List<String> members) {
        String normalizedName = groupName == null ? "" : groupName.trim();
        if (normalizedName.isEmpty()) {
            return null;
        }

        String groupId = "g" + System.currentTimeMillis();
        List<String> normalizedMembers = new ArrayList<>();
        if (members != null) {
            for (String member : members) {
                if (member != null) {
                    String trimmed = member.trim();
                    if (!trimmed.isEmpty() && !normalizedMembers.contains(trimmed)) {
                        normalizedMembers.add(trimmed);
                    }
                }
            }
        }
        Group group = new Group(groupId, normalizedName, normalizedMembers);
        groups.put(groupId, group);
        saveGroups();
        return group;
    }

    public synchronized boolean joinGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        if (group == null || username == null || username.trim().isEmpty()) {
            return false;
        }
        if (!group.getMembers().contains(username)) {
            group.getMembers().add(username);
            saveGroups();
        }
        return true;
    }

    public synchronized boolean leaveGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        if (group == null) {
            return false;
        }
        boolean removed = group.getMembers().remove(username);
        if (removed) {
            saveGroups();
        }
        return removed;
    }

    public synchronized boolean removeMember(String groupId, String username) {
        return leaveGroup(groupId, username);
    }

    public synchronized boolean deleteGroup(String groupId) {
        if (Constants.LOBBY_GROUP_ID.equals(groupId)) return false;
        if (groups.remove(groupId) != null) {
            saveGroups();
            return true;
        }
        return false;
    }

    public Group getGroup(String groupId) {
        Group group = groups.get(groupId);
        return group == null ? null : new Group(group.getGroupId(), group.getGroupName(), group.getMembers());
    }

    public List<Group> getAllGroups() {
        List<Group> result = new ArrayList<>();
        for (Group group : groups.values()) {
            result.add(new Group(group.getGroupId(), group.getGroupName(), group.getMembers()));
        }
        return result;
    }

    public List<Group> getGroupsForUser(String username) {
        List<Group> result = new ArrayList<>();
        for (Group group : groups.values()) {
            if (group.getMembers().contains(username)) {
                result.add(new Group(group.getGroupId(), group.getGroupName(), group.getMembers()));
            }
        }
        return result;
    }

    private void loadGroups() {
        try {
            File file = new File(Constants.GROUPS_FILE);
            if (!file.exists()) {
                return;
            }

            String content = new String(Files.readAllBytes(Paths.get(Constants.GROUPS_FILE)), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                return;
            }

            JSONArray arr = new JSONArray(content);
            for (int i = 0; i < arr.length(); i++) {
                Group group = JSONUtils.jsonToGroup(arr.getJSONObject(i).toString());
                groups.put(group.getGroupId(), group);
            }
        } catch (Exception e) {
            System.err.println("[GroupService] Error loading groups: " + e.getMessage());
        }
    }

    private synchronized void saveGroups() {
        try {
            File dir = new File(Constants.DATA_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            JSONArray arr = new JSONArray();
            for (Group group : groups.values()) {
                arr.put(JSONUtils.groupToJSONObject(group));
            }

            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(Constants.GROUPS_FILE), StandardCharsets.UTF_8)) {
                writer.write(arr.toString(2));
            }
        } catch (Exception e) {
            System.err.println("[GroupService] Error saving groups: " + e.getMessage());
        }
    }
}
