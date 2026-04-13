package com.chatapp;

import com.chatapp.controller.AuthController;
import com.chatapp.network.tcp.ChatClient;
import com.chatapp.view.LoginView;

import javax.swing.*;
import java.awt.*;

/**
 * Main entry point for the ChatApp client.
 * Initializes the Swing Look-and-Feel and launches the Login screen.
 */
public class App {

    public static void main(String[] args) {
        // Set look-and-feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // Improve text rendering
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
        } catch (Exception e) {
            // fallback to default
        }

        SwingUtilities.invokeLater(() -> {
            // Server host (default: localhost for testing)
            String serverHost = "localhost";
            if (args.length > 0) {
                serverHost = args[0];
            }

            // Create MVC components
            ChatClient chatClient = new ChatClient();
            LoginView loginView = new LoginView();
            AuthController authController = new AuthController(loginView, chatClient, serverHost);

            loginView.setVisible(true);

            System.out.println("╔══════════════════════════════════╗");
            System.out.println("║       ChatApp Client v1.0        ║");
            System.out.println("║  Server: " + String.format("%-23s", serverHost) + " ║");
            System.out.println("╚══════════════════════════════════╝");
        });
    }
}
