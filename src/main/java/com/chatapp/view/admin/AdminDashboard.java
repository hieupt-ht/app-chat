package com.chatapp.view.admin;

import com.chatapp.util.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminDashboard extends JFrame {

    private final String adminName;
    private UserManagementPanel userPanel;
    private ChatManagementPanel chatPanel;
    private StatisticsPanel statsPanel;
    private CardLayout contentLayout;
    private JPanel contentPanel;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JButton userButton;
    private JButton chatButton;
    private JButton statsButton;
    private JButton logoutButton;

    public AdminDashboard(String adminName) {
        this.adminName = adminName;
        initUI();
    }

    private void initUI() {
        setTitle("ChatApp - Admin Dashboard [" + adminName + "]");
        setSize(1180, 760);
        setMinimumSize(new Dimension(1040, 680));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Constants.BG_DARK);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBackground(Constants.BG_DARK);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContentArea(), BorderLayout.CENTER);

        add(root, BorderLayout.CENTER);
        showSection("users");
    }

    private JPanel buildSidebar() {
        RoundedPanel sidebar = new RoundedPanel(Constants.BG_SECONDARY, 30);
        sidebar.setLayout(new BorderLayout(0, 18));
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(new EmptyBorder(24, 18, 18, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Admin Console");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(Constants.TEXT_PRIMARY);
        header.add(title);
        header.add(Box.createVerticalStrut(6));

        JLabel subtitle = new JLabel("Manage users, logs and stats");
        subtitle.setFont(Constants.FONT_BODY);
        subtitle.setForeground(Constants.TEXT_SECONDARY);
        header.add(subtitle);
        header.add(Box.createVerticalStrut(20));

        RoundedPanel adminCard = new RoundedPanel(Constants.CARD_BLUE, 22);
        adminCard.setLayout(new BorderLayout());
        adminCard.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel adminLabel = new JLabel("<html><b>" + adminName + "</b><br/>Administrator</html>");
        adminLabel.setFont(Constants.FONT_BODY);
        adminLabel.setForeground(Constants.TEXT_PRIMARY);
        adminCard.add(adminLabel, BorderLayout.CENTER);

        header.add(adminCard);
        sidebar.add(header, BorderLayout.NORTH);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));

        userButton = createSidebarButton("User Management");
        chatButton = createSidebarButton("Chat Logs");
        statsButton = createSidebarButton("Statistics");

        userButton.addActionListener(e -> showSection("users"));
        chatButton.addActionListener(e -> showSection("chat"));
        statsButton.addActionListener(e -> showSection("stats"));

        nav.add(userButton);
        nav.add(Box.createVerticalStrut(10));
        nav.add(chatButton);
        nav.add(Box.createVerticalStrut(10));
        nav.add(statsButton);
        nav.add(Box.createVerticalGlue());

        sidebar.add(nav, BorderLayout.CENTER);

        logoutButton = createSidebarButton("Logout");
        logoutButton.setBackground(Constants.CARD_PINK);
        logoutButton.setForeground(Constants.ERROR_RED);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(logoutButton, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel buildContentArea() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 18));
        wrapper.setOpaque(false);

        RoundedPanel header = new RoundedPanel(Constants.BG_SECONDARY, 28);
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(22, 24, 22, 24));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        titleLabel = new JLabel();
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Constants.TEXT_PRIMARY);
        text.add(titleLabel);

        subtitleLabel = new JLabel();
        subtitleLabel.setFont(Constants.FONT_BODY);
        subtitleLabel.setForeground(Constants.TEXT_SECONDARY);
        text.add(Box.createVerticalStrut(6));
        text.add(subtitleLabel);
        header.add(text, BorderLayout.WEST);

        wrapper.add(header, BorderLayout.NORTH);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setOpaque(false);

        userPanel = new UserManagementPanel();
        chatPanel = new ChatManagementPanel();
        statsPanel = new StatisticsPanel();

        contentPanel.add(userPanel, "users");
        contentPanel.add(chatPanel, "chat");
        contentPanel.add(statsPanel, "stats");

        wrapper.add(contentPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBackground(Constants.BG_INPUT);
        button.setForeground(Constants.TEXT_PRIMARY);
        button.setBorder(new EmptyBorder(14, 16, 14, 16));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void showSection(String key) {
        contentLayout.show(contentPanel, key);

        userButton.setBackground("users".equals(key) ? Constants.CARD_BLUE : Constants.BG_INPUT);
        chatButton.setBackground("chat".equals(key) ? Constants.CARD_BLUE : Constants.BG_INPUT);
        statsButton.setBackground("stats".equals(key) ? Constants.CARD_BLUE : Constants.BG_INPUT);

        userButton.setForeground("users".equals(key) ? Constants.ACCENT : Constants.TEXT_PRIMARY);
        chatButton.setForeground("chat".equals(key) ? Constants.ACCENT : Constants.TEXT_PRIMARY);
        statsButton.setForeground("stats".equals(key) ? Constants.ACCENT : Constants.TEXT_PRIMARY);

        if ("users".equals(key)) {
            titleLabel.setText("User Management");
            subtitleLabel.setText("Review account roles, availability and moderation actions.");
        } else if ("chat".equals(key)) {
            titleLabel.setText("Chat Logs");
            subtitleLabel.setText("Browse stored conversations in a readable message format.");
        } else {
            titleLabel.setText("Statistics");
            subtitleLabel.setText("High-level system activity metrics for the current server state.");
        }
    }

    public void addLogoutListener(java.awt.event.ActionListener listener) {
        logoutButton.addActionListener(listener);
    }

    public UserManagementPanel getUserPanel() {
        return userPanel;
    }

    public ChatManagementPanel getChatPanel() {
        return chatPanel;
    }

    public StatisticsPanel getStatsPanel() {
        return statsPanel;
    }

    public String getAdminName() {
        return adminName;
    }

    private static class RoundedPanel extends JPanel {
        private final Color backgroundColor;
        private final int arc;

        private RoundedPanel(Color backgroundColor, int arc) {
            this.backgroundColor = backgroundColor;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
