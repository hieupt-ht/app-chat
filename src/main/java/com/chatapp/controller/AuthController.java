package com.chatapp.controller;

import com.chatapp.network.tcp.ChatClient;
import com.chatapp.util.Constants;
import com.chatapp.view.ChatView;
import com.chatapp.view.LoginView;
import com.chatapp.view.RegisterView;
import org.json.JSONObject;

import javax.swing.*;

/**
 * Controller for authentication flow.
 * Wires LoginView and RegisterView to ChatClient.
 */
public class AuthController {

    private final LoginView loginView;
    private RegisterView registerView;
    private final ChatClient chatClient;
    private String serverHost;

    public AuthController(LoginView loginView, ChatClient chatClient, String serverHost) {
        this.loginView = loginView;
        this.chatClient = chatClient;
        this.serverHost = serverHost;
        initListeners();
    }

    private void initListeners() {
        // Login button
        loginView.addLoginListener(e -> handleLogin());

        // Register button → open register view
        loginView.addRegisterListener(e -> showRegisterView());

        // LAN mode button
        loginView.addLanModeListener(e -> handleLanMode());

        // Listen for server responses
        chatClient.setServerEventListener(new ChatClient.ServerEventListener() {
            @Override
            public void onMessageReceived(String json) {
                SwingUtilities.invokeLater(() -> handleServerResponse(json));
            }

            @Override
            public void onDisconnected() {
                SwingUtilities.invokeLater(() -> loginView.setStatus("Disconnected from server.", true));
            }
        });
    }

    private void handleLogin() {
        String username = loginView.getUsername();
        String password = loginView.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            loginView.setStatus("Please fill in all fields.", true);
            return;
        }

        loginView.setStatus("Connecting...", false);

        // Connect to server if not already connected
        if (!chatClient.isConnected()) {
            if (!chatClient.connect(serverHost, Constants.TCP_PORT)) {
                loginView.setStatus("Cannot connect to server. Is it running?", true);
                return;
            }
            // Re-set the listener after connecting
            chatClient.setServerEventListener(new ChatClient.ServerEventListener() {
                @Override
                public void onMessageReceived(String json) {
                    SwingUtilities.invokeLater(() -> handleServerResponse(json));
                }

                @Override
                public void onDisconnected() {
                    SwingUtilities.invokeLater(() -> loginView.setStatus("Disconnected from server.", true));
                }
            });
        }

        chatClient.login(username, password);
    }

    private void handleServerResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String type = obj.optString("type", "");

            if (Constants.TYPE_AUTH_RESULT.equals(type)) {
                String result = obj.optString("content", "");

                switch (result) {
                    case "LOGIN_SUCCESS":
                        loginView.setStatus("Login successful!", false);
                        String role = obj.optString("role", Constants.ROLE_USER);
                        if (Constants.ROLE_ADMIN.equals(role)) {
                            openAdminDashboard(loginView.getUsername());
                        } else {
                            openChatView(loginView.getUsername(), false);
                        }
                        break;
                    case "LOGIN_FAILED":
                        String reason = obj.optString("reason", "");
                        if ("BANNED".equals(reason)) {
                            loginView.setStatus("Account banned. Contact admin.", true);
                        } else {
                            loginView.setStatus("Invalid username or password.", true);
                        }
                        break;
                    case "REGISTER_SUCCESS":
                        if (registerView != null) {
                            registerView.setStatus("Account created! You can now login.", false);
                        }
                        break;
                    case "REGISTER_FAILED":
                        if (registerView != null) {
                            registerView.setStatus("Username already exists.", true);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("[AuthController] Error: " + e.getMessage());
        }
    }

    private void showRegisterView() {
        if (registerView == null) {
            registerView = new RegisterView();
        }
        registerView.clearFields();

        registerView.addRegisterListener(e -> handleRegister());
        registerView.addBackListener(e -> {
            registerView.setVisible(false);
            loginView.setVisible(true);
        });

        loginView.setVisible(false);
        registerView.setVisible(true);
    }

    private void handleRegister() {
        String username = registerView.getUsername();
        String password = registerView.getPassword();
        String confirm = registerView.getConfirmPassword();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            registerView.setStatus("Please fill in all fields.", true);
            return;
        }

        if (username.length() < 3) {
            registerView.setStatus("Username must be at least 3 characters.", true);
            return;
        }

        if (password.length() < 4) {
            registerView.setStatus("Password must be at least 4 characters.", true);
            return;
        }

        if (!password.equals(confirm)) {
            registerView.setStatus("Passwords do not match.", true);
            return;
        }

        // Connect to server if not connected
        if (!chatClient.isConnected()) {
            if (!chatClient.connect(serverHost, Constants.TCP_PORT)) {
                registerView.setStatus("Cannot connect to server.", true);
                return;
            }
            chatClient.setServerEventListener(new ChatClient.ServerEventListener() {
                @Override
                public void onMessageReceived(String json) {
                    SwingUtilities.invokeLater(() -> handleServerResponse(json));
                }

                @Override
                public void onDisconnected() {
                }
            });
        }

        chatClient.register(username, password);
    }

    private void handleLanMode() {
        String nickname = loginView.getUsername();
        if (nickname.isEmpty()) {
            nickname = "User_" + (int) (Math.random() * 1000);
        }
        openChatView(nickname, true);
    }

    private void openChatView(String username, boolean isLanMode) {
        loginView.setVisible(false);
        if (registerView != null)
            registerView.setVisible(false);

        ChatView chatView = new ChatView(username, isLanMode);
        ChatController chatController = new ChatController(chatView, chatClient, isLanMode, this::showLoginView);
        chatView.setVisible(true);
    }

    private void openAdminDashboard(String username) {
        loginView.setVisible(false);
        if (registerView != null)
            registerView.setVisible(false);

        com.chatapp.view.admin.AdminDashboard adminDashboard = new com.chatapp.view.admin.AdminDashboard(username);
        com.chatapp.controller.AdminController adminController =
                new com.chatapp.controller.AdminController(adminDashboard, chatClient, this::showLoginView);
        adminDashboard.setVisible(true);
    }

    private void showLoginView() {
        if (registerView != null) {
            registerView.setVisible(false);
        }
        loginView.clearFields();
        loginView.setStatus("Logged out.", false);
        loginView.setVisible(true);
        loginView.toFront();
    }
}
