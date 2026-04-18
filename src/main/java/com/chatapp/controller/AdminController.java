package com.chatapp.controller;

import com.chatapp.network.tcp.ChatClient;
import com.chatapp.util.Constants;
import com.chatapp.view.admin.AdminDashboard;
import com.chatapp.view.admin.ChatManagementPanel;
import com.chatapp.view.admin.StatisticsPanel;
import com.chatapp.view.admin.UserManagementPanel;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;

public class AdminController {

    private final AdminDashboard dashboard;
    private final ChatClient chatClient;
    private final Runnable logoutAction;

    public AdminController(AdminDashboard dashboard, ChatClient chatClient) {
        this(dashboard, chatClient, null);
    }

    public AdminController(AdminDashboard dashboard, ChatClient chatClient, Runnable logoutAction) {
        this.dashboard = dashboard;
        this.chatClient = chatClient;
        this.logoutAction = logoutAction;

        initListeners();
        requestStats(); // Load initial data
    }

    private void initListeners() {
        UserManagementPanel up = dashboard.getUserPanel();
        ChatManagementPanel cp = dashboard.getChatPanel();
        StatisticsPanel sp = dashboard.getStatsPanel();

        // User Panel Listeners
        up.addRefreshListener(e -> requestStats());
        
        up.addBanListener(e -> {
            String user = up.getSelectedUser();
            if (user != null) sendAdminCommand(Constants.TYPE_BAN_USER, user, true);
        });

        up.addUnbanListener(e -> {
            String user = up.getSelectedUser();
            if (user != null) sendAdminCommand(Constants.TYPE_BAN_USER, user, false);
        });

        up.addDeleteListener(e -> {
            String user = up.getSelectedUser();
            if (user != null) sendAdminCommand(Constants.TYPE_DELETE_USER, user, true);
        });

        up.addForceLogoutListener(e -> {
            String user = up.getSelectedUser();
            if (user != null) sendAdminCommand(Constants.TYPE_FORCE_LOGOUT, user, true);
        });

        // Chat Panel Listeners
        cp.addRefreshListener(e -> requestStats());
        
        cp.addDeleteListener(e -> {
            JSONObject msgInfo = cp.getSelectedMessage();
            if (msgInfo != null) {
                JSONObject obj = new JSONObject();
                obj.put("type", Constants.TYPE_DELETE_MESSAGE);
                obj.put("timestamp", msgInfo.optLong("timestamp"));
                obj.put("msgSender", msgInfo.optString("sender"));
                obj.put("msgReceiver", msgInfo.optString("receiver"));
                obj.put("groupId", msgInfo.optString("groupId", ""));
                chatClient.send(obj.toString());
                JOptionPane.showMessageDialog(dashboard, "Delete command sent.");
                requestStats();
            }
        });

        cp.addRemoveMemberListener(e -> {
            String groupId = cp.getSelectedGroupId();
            String member = cp.getSelectedGroupMember();
            if (groupId != null && member != null) {
                JSONObject obj = new JSONObject();
                obj.put("type", Constants.TYPE_REMOVE_GROUP_MEMBER);
                obj.put("groupId", groupId);
                obj.put("target", member);
                chatClient.send(obj.toString());
                requestStats();
            }
        });

        cp.addDeleteGroupListener(e -> {
            String groupId = cp.getSelectedGroupId();
            if (groupId != null) {
                JSONObject obj = new JSONObject();
                obj.put("type", Constants.TYPE_DELETE_GROUP);
                obj.put("groupId", groupId);
                chatClient.send(obj.toString());
                requestStats();
            }
        });

        // Stats Panel Listeners
        sp.addRefreshListener(e -> requestStats());

        dashboard.addLogoutListener(e -> logout());

        // Server Response Listener
        chatClient.setServerEventListener(new ChatClient.ServerEventListener() {
            @Override
            public void onMessageReceived(String json) {
                SwingUtilities.invokeLater(() -> handleServerResponse(json));
            }

            @Override
            public void onDisconnected() {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(dashboard, "Disconnected from server.", "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void requestStats() {
        JSONObject obj = new JSONObject();
        obj.put("type", Constants.TYPE_GET_STATS);
        chatClient.send(obj.toString());
    }

    private void sendAdminCommand(String type, String targetUser, boolean banStatus) {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("target", targetUser);
        if (Constants.TYPE_BAN_USER.equals(type)) {
            obj.put("ban", banStatus);
        }
        chatClient.send(obj.toString());
        JOptionPane.showMessageDialog(dashboard, "Command sent for user: " + targetUser);
        requestStats(); // Refresh
    }

    private void handleServerResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            if (Constants.TYPE_STATS_RESULT.equals(obj.optString("type"))) {
                int online = obj.optInt("onlineCount", 0);
                int totalUsers = obj.optInt("totalUsers", 0);
                long totalMessages = obj.optLong("totalMessages", 0);
                
                dashboard.getStatsPanel().updateStats(online, totalUsers, totalMessages);

                JSONArray usersArr = obj.optJSONArray("users");
                if (usersArr != null) {
                    dashboard.getUserPanel().updateUsers(usersArr);
                }

                JSONArray msgArr = obj.optJSONArray("messages");
                if (msgArr != null) {
                    dashboard.getChatPanel().updateMessages(msgArr);
                }

                JSONArray groupsArr = obj.optJSONArray("groups");
                if (groupsArr != null) {
                    dashboard.getChatPanel().updateGroups(groupsArr);
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminController] Error processing response: " + e.getMessage());
        }
    }

    private void logout() {
        chatClient.disconnect();
        dashboard.dispose();
        if (logoutAction != null) {
            SwingUtilities.invokeLater(logoutAction);
        }
    }
}
