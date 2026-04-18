package com.chatapp.controller;

import com.chatapp.model.Group;
import com.chatapp.model.Message;
import com.chatapp.network.tcp.ChatClient;
import com.chatapp.network.udp.UDPBroadcaster;
import com.chatapp.network.udp.UDPListener;
import com.chatapp.util.Constants;
import com.chatapp.util.JSONUtils;
import com.chatapp.view.ChatView;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatController {

    private final ChatView chatView;
    private final ChatClient chatClient;
    private final boolean isLanMode;
    private final Runnable logoutAction;

    private UDPBroadcaster broadcaster;
    private UDPListener udpListener;
    private int lanTcpPort;
    private ServerSocket lanServerSocket;
    private final ConcurrentHashMap<String, PrintWriter> lanConnections = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();
    private final Map<String, Group> groupCache = new ConcurrentHashMap<>();
    private String selectedUser;
    private String selectedGroupId;
    private boolean suppressSelectionEvents;

    public ChatController(ChatView chatView, ChatClient chatClient, boolean isLanMode) {
        this(chatView, chatClient, isLanMode, null);
    }

    public ChatController(ChatView chatView, ChatClient chatClient, boolean isLanMode, Runnable logoutAction) {
        this.chatView = chatView;
        this.chatClient = chatClient;
        this.isLanMode = isLanMode;
        this.logoutAction = logoutAction;

        initListeners();
        if (isLanMode) {
            startLanMode();
        } else {
            startServerMode();
        }
    }

    private void initListeners() {
        chatView.addSendListener(e -> sendMessage());
        chatView.addAttachListener(e -> sendAttachment());
        chatView.addLogoutListener(e -> logout());
        chatView.addRoomButtonListener(e -> switchToRoom());
        chatView.addEmojiListener();

        chatView.addUserSelectionListener(e -> {
            if (e.getValueIsAdjusting() || suppressSelectionEvents) {
                return;
            }
            String user = chatView.getSelectedUser();
            if (user != null) {
                selectedUser = user;
                selectedGroupId = null;
                clearGroupSelection();
                chatView.setPrivateChatHeader(user);
                loadConversationHistory(getPrivateHistoryKey(chatView.getCurrentUser(), user));
            }
        });

        chatView.addGroupSelectionListener(e -> {
            if (e.getValueIsAdjusting() || suppressSelectionEvents) {
                return;
            }
            String groupId = chatView.getSelectedGroupId();
            if (groupId != null) {
                selectedGroupId = groupId;
                selectedUser = null;
                clearUserSelection();
                chatView.setGroupChatHeader(chatView.getSelectedGroupName(), chatView.getSelectedGroupMemberCount());
                loadConversationHistory(getGroupHistoryKey(groupId));
            }
        });

        chatView.addCreateGroupListener(e -> createGroup());
        chatView.addJoinGroupListener(e -> joinGroup());
        chatView.addLeaveGroupListener(e -> leaveGroup());

        chatView.addWindowCloseListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
                chatView.dispose();
                System.exit(0);
            }
        });
        
        chatView.setMessageActionCallback(new ChatView.MessageActionCallback() {
            @Override
            public void onReply(Message msg) {
                chatView.startReply(msg);
            }

            @Override
            public void onUnsend(Message msg) {
                if (!isLanMode) {
                    JSONObject obj = new JSONObject();
                    obj.put("type", Constants.TYPE_UNSEND);
                    obj.put("timestamp", msg.getTimestamp());
                    obj.put("msgSender", msg.getSender());
                    obj.put("msgReceiver", msg.getReceiver() == null ? "" : msg.getReceiver());
                    if (msg.getGroupId() != null && !msg.getGroupId().isEmpty()) {
                        obj.put("groupId", msg.getGroupId());
                    }
                    chatClient.send(obj.toString());
                } else {
                    chatView.addSystemMessage("Gỡ tin nhắn không hỗ trợ trong chế độ LAN.");
                }
            }
        });
    }

    private void sendMessage() {
        String text = chatView.getMessageText();
        if (text.isEmpty()) {
            return;
        }

        Message message;
        if (selectedUser != null) {
            // Private chat has priority when a user is selected
            message = new Message(chatView.getCurrentUser(), selectedUser, text, Constants.TYPE_PRIVATE);
        } else if (selectedGroupId != null) {
            message = new Message(chatView.getCurrentUser(), "", text, Constants.TYPE_GROUP_MESSAGE);
            message.setGroupId(selectedGroupId);
        } else {
            chatView.addSystemMessage("Select a user or a group first.");
            return;
        }
        
        Message replyingMsg = chatView.getReplyingToMessage();
        if (replyingMsg != null) {
            message.setReplySender(replyingMsg.getSender());
            String snippet = Constants.TYPE_FILE.equals(replyingMsg.getType()) ? 
                             "[Đính kèm] " + replyingMsg.getFileName() : 
                             replyingMsg.getContent();
            message.setReplySnippet(snippet);
            chatView.cancelReply();
        }

        deliverMessage(message, true);
    }

    private void sendAttachment() {
        if (selectedUser == null && selectedGroupId == null) {
            chatView.addSystemMessage("Select a conversation before sending a file.");
            chatView.focusMessageField();
            return;
        }

        File file = chatView.chooseAttachment();
        if (file == null) {
            return;
        }
        if (!file.isFile()) {
            chatView.addSystemMessage("Selected path is not a file.");
            return;
        }
        if (file.length() > Constants.MAX_FILE_SIZE_BYTES) {
            chatView.addSystemMessage("File is too large. Max size is 5 MB.");
            return;
        }

        String encodedData;
        try {
            encodedData = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            chatView.addSystemMessage("Cannot read file: " + e.getMessage());
            return;
        }

        Message fileMessage = new Message(
                chatView.getCurrentUser(),
                selectedUser == null ? "" : selectedUser,
                file.getName(),
                Constants.TYPE_FILE);
        fileMessage.setFileName(file.getName());
        fileMessage.setFileData(encodedData);
        if (selectedGroupId != null) {
            fileMessage.setGroupId(selectedGroupId);
        }
        
        Message replyingMsg = chatView.getReplyingToMessage();
        if (replyingMsg != null) {
            fileMessage.setReplySender(replyingMsg.getSender());
            String snippet = Constants.TYPE_FILE.equals(replyingMsg.getType()) ? 
                             "[Đính kèm] " + replyingMsg.getFileName() : 
                             replyingMsg.getContent();
            fileMessage.setReplySnippet(snippet);
            chatView.cancelReply();
        }
        
        deliverMessage(fileMessage, true);
    }

    private void deliverMessage(Message msg, boolean sentByCurrentUser) {
        if (isLanMode) {
            sendLanMessage(msg);
        } else {
            chatClient.sendMessage(msg);
        }

        Message historyMsg = sanitizeForHistory(msg);
        saveToHistory(historyMsg);
        chatView.addMessage(
                historyMsg.getSender(),
                getDisplayContent(historyMsg),
                historyMsg.getTimestamp(),
                sentByCurrentUser,
                Constants.TYPE_FILE.equals(historyMsg.getType()),
                historyMsg.getGroupId() != null && !historyMsg.getGroupId().isEmpty());
    }

    private void startLanMode() {
        String nickname = chatView.getCurrentUser();
        try {
            lanServerSocket = new ServerSocket(0);
            lanTcpPort = lanServerSocket.getLocalPort();
        } catch (IOException e) {
            chatView.addSystemMessage("Cannot create LAN server: " + e.getMessage());
            return;
        }

        Thread lanServerThread = new Thread(() -> {
            while (!lanServerSocket.isClosed()) {
                try {
                    Socket peerSocket = lanServerSocket.accept();
                    Thread peerThread = new Thread(() -> handleLanPeer(peerSocket));
                    peerThread.setDaemon(true);
                    peerThread.start();
                } catch (IOException e) {
                    if (!lanServerSocket.isClosed()) {
                        System.err.println("[ChatController] LAN server error: " + e.getMessage());
                    }
                }
            }
        }, "LAN-Server");
        lanServerThread.setDaemon(true);
        lanServerThread.start();

        broadcaster = new UDPBroadcaster(nickname, lanTcpPort);
        Thread broadcasterThread = new Thread(broadcaster, "UDP-Broadcaster");
        broadcasterThread.setDaemon(true);
        broadcasterThread.start();

        udpListener = new UDPListener(nickname);
        udpListener.setPeerDiscoveryListener(peers -> {
            List<String> names = new ArrayList<>(peers.keySet());
            SwingUtilities.invokeLater(() -> chatView.updateUserList(names));
        });
        Thread listenerThread = new Thread(udpListener, "UDP-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Auto-select the lobby (Public Room) as default conversation
        selectedGroupId = Constants.LOBBY_GROUP_ID;
        selectedUser = null;
        chatView.setGroupChatHeader(Constants.LOBBY_GROUP_NAME, 0);

        chatView.addSystemMessage("LAN mode active. Welcome to the Public Room!");
    }

    private void handleLanPeer(Socket peerSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Message msg = JSONUtils.jsonToMessage(line);
                SwingUtilities.invokeLater(() -> handleIncomingMessage(msg));
            }
        } catch (IOException ignored) {
        }
    }

    private void sendLanMessage(Message msg) {
        // If it's a lobby/room message in LAN mode, broadcast to all peers
        if (Constants.LOBBY_GROUP_ID.equals(msg.getGroupId())) {
            Map<String, UDPListener.PeerInfo> peers = udpListener.getPeers();
            String json = JSONUtils.messageToJSON(msg);
            for (Map.Entry<String, UDPListener.PeerInfo> entry : peers.entrySet()) {
                try {
                    PrintWriter writer = lanConnections.get(entry.getKey());
                    if (writer == null) {
                        Socket socket = new Socket(entry.getValue().ipAddress, entry.getValue().tcpPort);
                        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                        lanConnections.put(entry.getKey(), writer);
                        final Socket finalSocket = socket;
                        Thread readThread = new Thread(() -> handleLanPeer(finalSocket));
                        readThread.setDaemon(true);
                        readThread.start();
                    }
                    writer.println(json);
                } catch (IOException e) {
                    lanConnections.remove(entry.getKey());
                }
            }
            return;
        }
        if (msg.getGroupId() != null && !msg.getGroupId().isEmpty()) {
            chatView.addSystemMessage("Group chat is available in server mode.");
            return;
        }

        Map<String, UDPListener.PeerInfo> peers = udpListener.getPeers();
        UDPListener.PeerInfo info = peers.get(msg.getReceiver());
        if (info == null) {
            chatView.addSystemMessage("User " + msg.getReceiver() + " not found on LAN.");
            return;
        }

        try {
            PrintWriter writer = lanConnections.get(msg.getReceiver());
            if (writer == null) {
                Socket socket = new Socket(info.ipAddress, info.tcpPort);
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                lanConnections.put(msg.getReceiver(), writer);

                final Socket finalSocket = socket;
                Thread readThread = new Thread(() -> handleLanPeer(finalSocket));
                readThread.setDaemon(true);
                readThread.start();
            }
            writer.println(JSONUtils.messageToJSON(msg));
        } catch (IOException e) {
            chatView.addSystemMessage("Failed to send to " + msg.getReceiver() + ": " + e.getMessage());
            lanConnections.remove(msg.getReceiver());
        }
    }

    private void startServerMode() {
        chatClient.setServerEventListener(new ChatClient.ServerEventListener() {
            @Override
            public void onMessageReceived(String json) {
                SwingUtilities.invokeLater(() -> handleServerMessage(json));
            }

            @Override
            public void onDisconnected() {
                SwingUtilities.invokeLater(() -> chatView.addSystemMessage("Disconnected from server."));
            }
        });

        // Auto-select the lobby (Public Room) as default conversation
        selectedGroupId = Constants.LOBBY_GROUP_ID;
        selectedUser = null;
        chatView.setGroupChatHeader(Constants.LOBBY_GROUP_NAME, 0);

        requestInitialState();
        chatView.addSystemMessage("Welcome to the Public Room!");
    }

    private void requestInitialState() {
        JSONObject obj = new JSONObject();
        obj.put("type", Constants.TYPE_GET_STATS);
        chatClient.send(obj.toString());
    }

    private void handleServerMessage(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String type = obj.optString("type", "");

            switch (type) {
                case Constants.TYPE_USER_LIST: {
                    JSONArray usersArr = obj.optJSONArray("users");
                    List<String> users = new ArrayList<>();
                    if (usersArr != null) {
                        for (int i = 0; i < usersArr.length(); i++) {
                            users.add(usersArr.optString(i));
                        }
                    }
                    chatView.updateUserList(users);
                    break;
                }

                case Constants.TYPE_GROUP_LIST: {
                    JSONArray groupsArr = obj.optJSONArray("groups");
                    List<Group> groups = groupsArr == null ? new ArrayList<>() : JSONUtils.jsonToGroupList(groupsArr.toString());
                    updateGroups(groups);
                    break;
                }

                case Constants.TYPE_STATS_RESULT: {
                    JSONArray usersArr = obj.optJSONArray("users");
                    List<String> users = new ArrayList<>();
                    if (usersArr != null) {
                        for (int i = 0; i < usersArr.length(); i++) {
                            JSONObject userObj = usersArr.optJSONObject(i);
                            if (userObj == null) {
                                continue;
                            }
                            String username = userObj.optString("username", "");
                            boolean online = userObj.optBoolean("online", false);
                            if (online && !username.isEmpty()) {
                                users.add(username);
                            }
                        }
                    }
                    chatView.updateUserList(users);

                    JSONArray statsGroupsArr = obj.optJSONArray("groups");
                    List<Group> statsGroups = statsGroupsArr == null ? new ArrayList<>() : JSONUtils.jsonToGroupList(statsGroupsArr.toString());
                    updateGroups(statsGroups);
                    break;
                }

                case Constants.TYPE_PRIVATE:
                case Constants.TYPE_TEXT:
                case Constants.TYPE_FILE:
                case Constants.TYPE_GROUP_MESSAGE:
                case Constants.TYPE_ROOM_JOIN:
                case Constants.TYPE_ROOM_LEAVE:
                case "ROOM_KICK":
                    handleIncomingMessage(JSONUtils.jsonToMessage(json));
                    break;

                case Constants.TYPE_UNSEND: {
                    Message unsendMsg = JSONUtils.jsonToMessage(json);
                    String key = getConversationKey(unsendMsg);
                    if (conversationHistory.containsKey(key)) {
                        for (Message msg : conversationHistory.get(key)) {
                            if (msg.getTimestamp() == unsendMsg.getTimestamp() && msg.getSender().equals(unsendMsg.getSender())) {
                                msg.setUnsent(true);
                                msg.setContent("Tin nhắn đã bị thu hồi");
                                break;
                            }
                        }
                        if (isCurrentConversation(unsendMsg)) {
                            loadConversationHistory(key); // Refresh UI
                        }
                    }
                    break;
                }

                default:
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ChatController] Error: " + e.getMessage());
        }
    }

    private void handleIncomingMessage(Message msg) {
        Message historyMsg = prepareIncomingMessage(msg);
        saveToHistory(historyMsg);

        if (isCurrentConversation(historyMsg)) {
            if (isSystemType(historyMsg.getType())) {
                chatView.addSystemMessage(getDisplayContent(historyMsg));
            } else {
                chatView.addBubbleMessage(historyMsg, false);
            }
        }
    }

    private boolean isSystemType(String type) {
        return Constants.TYPE_ROOM_JOIN.equals(type) || 
               Constants.TYPE_ROOM_LEAVE.equals(type) || 
               "ROOM_KICK".equals(type);
    }

    private void saveToHistory(Message msg) {
        conversationHistory
                .computeIfAbsent(getConversationKey(msg), key -> new ArrayList<>())
                .add(msg);
    }

    private void loadConversationHistory(String key) {
        chatView.clearChat();
        List<Message> history = conversationHistory.getOrDefault(key, new ArrayList<>());
        for (Message msg : history) {
            if (isSystemType(msg.getType())) {
                chatView.addSystemMessage(getDisplayContent(msg));
            } else {
                boolean sentByCurrentUser = chatView.getCurrentUser().equals(msg.getSender());
                chatView.addBubbleMessage(msg, sentByCurrentUser);
            }
        }
    }

    private String getConversationKey(Message msg) {
        return msg.getGroupId() != null && !msg.getGroupId().isEmpty()
                ? getGroupHistoryKey(msg.getGroupId())
                : getPrivateHistoryKey(msg.getSender(), msg.getReceiver());
    }

    private String getPrivateHistoryKey(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? "private:" + user1 + "_" + user2 : "private:" + user2 + "_" + user1;
    }

    private String getGroupHistoryKey(String groupId) {
        return "group:" + groupId;
    }

    private Message prepareIncomingMessage(Message msg) {
        Message historyMsg = sanitizeForHistory(msg);
        if (Constants.TYPE_FILE.equals(msg.getType())) {
            String savedPath = saveIncomingFile(msg);
            if (savedPath != null) {
                chatView.addSystemMessage("Received file saved to: " + savedPath);
            }
        }
        return historyMsg;
    }

    private Message sanitizeForHistory(Message msg) {
        Message historyMsg = new Message(
                msg.getSender(),
                msg.getReceiver(),
                getDisplayContent(msg),
                msg.getType(),
                msg.getTimestamp());
        historyMsg.setGroupId(msg.getGroupId());
        historyMsg.setFileName(msg.getFileName());
        return historyMsg;
    }

    private String getDisplayContent(Message msg) {
        if (Constants.TYPE_FILE.equals(msg.getType()) && msg.getFileName() != null && !msg.getFileName().isEmpty()) {
            return msg.getFileName();
        }
        return msg.getContent();
    }

    private String saveIncomingFile(Message msg) {
        if (msg.getFileData() == null || msg.getFileData().isEmpty()) {
            return null;
        }

        String fileName = msg.getFileName() != null && !msg.getFileName().isEmpty() ? msg.getFileName() : "attachment.bin";
        try {
            byte[] data = Base64.getDecoder().decode(msg.getFileData());
            Path userDownloadDir = Path.of(Constants.DOWNLOADS_DIR, chatView.getCurrentUser());
            Files.createDirectories(userDownloadDir);

            Path target = userDownloadDir.resolve(fileName);
            if (Files.exists(target)) {
                target = userDownloadDir.resolve(System.currentTimeMillis() + "_" + fileName);
            }
            Files.write(target, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return target.toAbsolutePath().toString();
        } catch (Exception e) {
            System.err.println("[ChatController] Cannot save incoming file: " + e.getMessage());
            return null;
        }
    }

    private boolean isCurrentConversation(Message msg) {
        if (msg.getGroupId() != null && !msg.getGroupId().isEmpty()) {
            return msg.getGroupId().equals(selectedGroupId);
        }
        if (selectedUser == null) {
            return false;
        }
        return selectedUser.equals(msg.getSender()) || selectedUser.equals(msg.getReceiver());
    }

    private void createGroup() {
        if (isLanMode) {
            chatView.addSystemMessage("Group chat is only available in server mode.");
            return;
        }
        String groupName = JOptionPane.showInputDialog(chatView, "Group name:");
        if (groupName == null || groupName.trim().isEmpty()) {
            return;
        }
        String membersText = JOptionPane.showInputDialog(chatView, "Additional members (comma separated usernames):");
        JSONArray members = new JSONArray();
        members.put(chatView.getCurrentUser());
        if (membersText != null && !membersText.trim().isEmpty()) {
            for (String value : membersText.split(",")) {
                String member = value.trim();
                if (!member.isEmpty() && !chatView.getCurrentUser().equals(member)) {
                    members.put(member);
                }
            }
        }

        JSONObject request = new JSONObject();
        request.put("type", Constants.TYPE_CREATE_GROUP);
        request.put("groupName", groupName.trim());
        request.put("members", members);
        chatClient.send(request.toString());
        chatView.addSystemMessage("Group creation request sent.");
    }

    private void joinGroup() {
        if (isLanMode) {
            chatView.addSystemMessage("Group chat is only available in server mode.");
            return;
        }
        String groupId = JOptionPane.showInputDialog(chatView, "Group ID:");
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }
        JSONObject request = new JSONObject();
        request.put("type", Constants.TYPE_JOIN_GROUP);
        request.put("groupId", groupId.trim());
        request.put("target", chatView.getCurrentUser());
        chatClient.send(request.toString());
        chatView.addSystemMessage("Join group request sent for " + groupId.trim() + ".");
    }

    private void leaveGroup() {
        if (selectedGroupId == null) {
            chatView.addSystemMessage("Select a group first.");
            return;
        }
        JSONObject request = new JSONObject();
        request.put("type", Constants.TYPE_LEAVE_GROUP);
        request.put("groupId", selectedGroupId);
        request.put("target", chatView.getCurrentUser());
        chatClient.send(request.toString());

        chatView.clearChat();
        chatView.clearGroupSelection();
        selectedGroupId = null;
        chatView.resetConversationState();
        chatView.addSystemMessage("Leave group request sent.");
    }

    private void updateGroups(List<Group> groups) {
        groupCache.clear();
        for (Group group : groups) {
            groupCache.put(group.getGroupId(), group);
        }
        chatView.updateGroupList(groups);

        if (selectedGroupId != null && !groupCache.containsKey(selectedGroupId)) {
            selectedGroupId = null;
            chatView.clearGroupSelection();
            chatView.clearChat();
            chatView.resetConversationState();
            chatView.addSystemMessage("Current group is no longer available.");
        }
    }

    private void clearUserSelection() {
        suppressSelectionEvents = true;
        chatView.clearUserSelection();
        suppressSelectionEvents = false;
    }

    private void clearGroupSelection() {
        suppressSelectionEvents = true;
        chatView.clearGroupSelection();
        suppressSelectionEvents = false;
    }

    private void shutdown() {
        if (isLanMode) {
            if (broadcaster != null) {
                broadcaster.stop();
            }
            if (udpListener != null) {
                udpListener.stop();
            }
            try {
                if (lanServerSocket != null) {
                    lanServerSocket.close();
                }
            } catch (IOException ignored) {
            }
            for (PrintWriter writer : lanConnections.values()) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        } else if (chatClient != null) {
            chatClient.disconnect();
        }
    }

    private void logout() {
        shutdown();
        chatView.dispose();
        if (logoutAction != null) {
            SwingUtilities.invokeLater(logoutAction);
        }
    }

    private void switchToRoom() {
        selectedUser = null;
        selectedGroupId = Constants.LOBBY_GROUP_ID;
        clearUserSelection();
        clearGroupSelection();
        chatView.setGroupChatHeader(Constants.LOBBY_GROUP_NAME, 0);
        loadConversationHistory(getGroupHistoryKey(Constants.LOBBY_GROUP_ID));
    }
}
