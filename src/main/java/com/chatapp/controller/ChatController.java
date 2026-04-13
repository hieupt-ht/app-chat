package com.chatapp.controller;

import com.chatapp.model.Message;
import com.chatapp.network.tcp.ChatClient;
import com.chatapp.network.udp.UDPBroadcaster;
import com.chatapp.network.udp.UDPListener;
import com.chatapp.util.Constants;
import com.chatapp.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Base64;

import com.chatapp.view.ChatView;

/**
 * Controller for the main chat functionality.
 * Handles both LAN (UDP) and Server (TCP) modes.
 */
public class ChatController {

    private final ChatView chatView;
    private final ChatClient chatClient;
    private final boolean isLanMode;

    // LAN mode components
    private UDPBroadcaster broadcaster;
    private UDPListener udpListener;
    private Thread broadcasterThread;
    private Thread listenerThread;
    private int lanTcpPort;
    private ServerSocket lanServerSocket;
    private Thread lanServerThread;
    private final ConcurrentHashMap<String, PrintWriter> lanConnections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Message>> lanChatHistory = new ConcurrentHashMap<>();

    private String selectedUser = null;
    private final Runnable logoutAction;

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
        // Send message
        chatView.addSendListener(e -> sendMessage());
        chatView.addAttachListener(e -> sendAttachment());
        chatView.addLogoutListener(e -> logout());

        // User selection
        chatView.addUserSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = chatView.getSelectedUser();
                if (selected != null) {
                    selectedUser = selected;
                    chatView.setChatHeader(selected);
                    loadChatHistory(selected);
                }
            }
        });

        // Emoji
        chatView.addEmojiListener();

        // Window close
        chatView.addWindowCloseListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
                chatView.dispose();
                System.exit(0);
            }
        });
    }

    // ═══════════ Send Message ═══════════

    private void sendMessage() {
        String text = chatView.getMessageText();
        if (text.isEmpty() || selectedUser == null)
            return;

        Message msg = new Message(chatView.getCurrentUser(), selectedUser, text, Constants.TYPE_PRIVATE);
        deliverMessage(msg, true);
    }

    private void sendAttachment() {
        if (selectedUser == null) {
            chatView.addSystemMessage("Select a user before sending a file.");
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
                selectedUser,
                file.getName(),
                Constants.TYPE_FILE);
        fileMessage.setFileName(file.getName());
        fileMessage.setFileData(encodedData);
        deliverMessage(fileMessage, true);
    }

    private void deliverMessage(Message msg, boolean sentByCurrentUser) {
        if (isLanMode) {
            sendLanMessage(msg);
        } else {
            chatClient.sendMessage(msg);
        }

        chatView.addMessage(
                msg.getSender(),
                getDisplayContent(msg),
                msg.getTimestamp(),
                sentByCurrentUser,
                Constants.TYPE_FILE.equals(msg.getType()));
        saveToChatHistory(sanitizeForHistory(msg));
    }

    // ═══════════ LAN Mode ═══════════

    private void startLanMode() {
        String nickname = chatView.getCurrentUser();

        // Start a local TCP server for receiving LAN messages
        try {
            lanServerSocket = new ServerSocket(0); // random available port
            lanTcpPort = lanServerSocket.getLocalPort();
        } catch (IOException e) {
            System.err.println("[ChatController] Cannot create LAN server: " + e.getMessage());
            return;
        }

        // LAN TCP server thread (accepts connections from peers)
        lanServerThread = new Thread(() -> {
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

        // Start UDP broadcaster
        broadcaster = new UDPBroadcaster(nickname, lanTcpPort);
        broadcasterThread = new Thread(broadcaster, "UDP-Broadcaster");
        broadcasterThread.setDaemon(true);
        broadcasterThread.start();

        // Start UDP listener
        udpListener = new UDPListener(nickname);
        udpListener.setPeerDiscoveryListener(peers -> {
            List<String> names = new ArrayList<>(peers.keySet());
            SwingUtilities.invokeLater(() -> chatView.updateUserList(names));
        });
        listenerThread = new Thread(udpListener, "UDP-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        chatView.addSystemMessage("LAN mode active. Discovering users...");
    }

    private void handleLanPeer(Socket peerSocket) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(peerSocket.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                Message msg = JSONUtils.jsonToMessage(line);
                SwingUtilities.invokeLater(() -> {
                    Message historyMsg = prepareIncomingMessage(msg);
                    saveToChatHistory(historyMsg);
                    if (msg.getSender().equals(selectedUser)) {
                        chatView.addMessage(
                                historyMsg.getSender(),
                                getDisplayContent(historyMsg),
                                historyMsg.getTimestamp(),
                                false,
                                Constants.TYPE_FILE.equals(historyMsg.getType()));
                    }
                });
            }
        } catch (IOException e) {
            // Peer disconnected
        }
    }

    private void sendLanMessage(Message msg) {
        String targetNick = msg.getReceiver();
        Map<String, UDPListener.PeerInfo> peers = udpListener.getPeers();
        UDPListener.PeerInfo info = peers.get(targetNick);

        if (info == null) {
            chatView.addSystemMessage("User " + targetNick + " not found on LAN.");
            return;
        }

        // Get or create TCP connection to peer
        try {
            PrintWriter pw = lanConnections.get(targetNick);
            if (pw == null) {
                Socket s = new Socket(info.ipAddress, info.tcpPort);
                pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
                lanConnections.put(targetNick, pw);

                // Start reading from this connection too
                final Socket finalSocket = s;
                Thread readThread = new Thread(() -> handleLanPeer(finalSocket));
                readThread.setDaemon(true);
                readThread.start();
            }
            pw.println(JSONUtils.messageToJSON(msg));
        } catch (IOException e) {
            chatView.addSystemMessage("Failed to send to " + targetNick + ": " + e.getMessage());
            lanConnections.remove(targetNick);
        }
    }

    // ═══════════ Server Mode ═══════════

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

        chatView.addSystemMessage("Connected to server. Select a user to chat.");
    }

    private void handleServerMessage(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String type = obj.optString("type", "");

            switch (type) {
                case Constants.TYPE_USER_LIST:
                    JSONArray usersArr = obj.getJSONArray("users");
                    List<String> users = new ArrayList<>();
                    for (int i = 0; i < usersArr.length(); i++) {
                        users.add(usersArr.getString(i));
                    }
                    chatView.updateUserList(users);
                    break;

                case Constants.TYPE_PRIVATE:
                case Constants.TYPE_TEXT:
                case Constants.TYPE_FILE:
                    Message msg = JSONUtils.jsonToMessage(json);
                    Message historyMsg = prepareIncomingMessage(msg);
                    saveToChatHistory(historyMsg);
                    if (msg.getSender().equals(selectedUser)) {
                        chatView.addMessage(
                                historyMsg.getSender(),
                                getDisplayContent(historyMsg),
                                historyMsg.getTimestamp(),
                                false,
                                Constants.TYPE_FILE.equals(historyMsg.getType()));
                    }
                    break;

                default:
                    // ignore other types in chat controller
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ChatController] Error: " + e.getMessage());
        }
    }

    // ═══════════ Chat History ═══════════

    private void saveToChatHistory(Message msg) {
        String key = getHistoryKey(msg.getSender(), msg.getReceiver());
        lanChatHistory.computeIfAbsent(key, k -> new ArrayList<>()).add(msg);
    }

    private void loadChatHistory(String otherUser) {
        chatView.clearChat();
        String key = getHistoryKey(chatView.getCurrentUser(), otherUser);
        List<Message> history = lanChatHistory.get(key);
        if (history != null) {
            for (Message msg : history) {
                boolean isSent = msg.getSender().equals(chatView.getCurrentUser());
                chatView.addMessage(
                        msg.getSender(),
                        getDisplayContent(msg),
                        msg.getTimestamp(),
                        isSent,
                        Constants.TYPE_FILE.equals(msg.getType()));
            }
        }
    }

    private Message prepareIncomingMessage(Message msg) {
        if (!Constants.TYPE_FILE.equals(msg.getType())) {
            return sanitizeForHistory(msg);
        }

        Message historyMsg = sanitizeForHistory(msg);
        String savedPath = saveIncomingFile(msg);
        if (savedPath != null) {
            chatView.addSystemMessage("Received file saved to: " + savedPath);
        } else {
            chatView.addSystemMessage("Received file metadata, but the file content could not be saved.");
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

        String fileName = msg.getFileName() != null && !msg.getFileName().isEmpty()
                ? msg.getFileName()
                : "attachment.bin";

        try {
            byte[] data = Base64.getDecoder().decode(msg.getFileData());
            Path userDownloadDir = Path.of(Constants.DOWNLOADS_DIR, chatView.getCurrentUser());
            Files.createDirectories(userDownloadDir);

            Path target = userDownloadDir.resolve(fileName);
            if (Files.exists(target)) {
                String uniqueName = System.currentTimeMillis() + "_" + fileName;
                target = userDownloadDir.resolve(uniqueName);
            }

            Files.write(target, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return target.toAbsolutePath().toString();
        } catch (Exception e) {
            System.err.println("[ChatController] Cannot save incoming file: " + e.getMessage());
            return null;
        }
    }

    private String getHistoryKey(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    // ═══════════ Shutdown ═══════════

    private void shutdown() {
        if (isLanMode) {
            if (broadcaster != null)
                broadcaster.stop();
            if (udpListener != null)
                udpListener.stop();
            try {
                if (lanServerSocket != null)
                    lanServerSocket.close();
            } catch (IOException ignored) {
            }
            for (PrintWriter pw : lanConnections.values()) {
                try {
                    pw.close();
                } catch (Exception ignored) {
                }
            }
        } else {
            if (chatClient != null)
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
}
