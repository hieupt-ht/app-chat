package com.chatapp.view;

import com.chatapp.util.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChatView extends JFrame {
    private static final EmojiSpec[] EMOJI_SPECS = {
            new EmojiSpec(":smile:", EmojiKind.SMILE),
            new EmojiSpec(":laugh:", EmojiKind.LAUGH),
            new EmojiSpec(":love:", EmojiKind.LOVE),
            new EmojiSpec(":cool:", EmojiKind.COOL),
            new EmojiSpec(":wow:", EmojiKind.WOW),
            new EmojiSpec(":sad:", EmojiKind.SAD),
            new EmojiSpec(":angry:", EmojiKind.ANGRY),
            new EmojiSpec(":like:", EmojiKind.LIKE)
    };

    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList;
    private JPanel chatPanel;
    private JScrollPane chatScrollPane;
    private JTextField messageField;
    private JButton sendButton;
    private JButton emojiButton;
    private JButton attachButton;
    private JButton logoutButton;
    private JLabel chatHeaderLabel;
    private JLabel chatSubHeaderLabel;
    private final String currentUser;
    private String selectedUser;
    private final boolean isLanMode;

    public ChatView(String currentUser, boolean isLanMode) {
        this.currentUser = currentUser;
        this.isLanMode = isLanMode;
        initUI();
    }

    private void initUI() {
        setTitle("ChatApp - " + currentUser + (isLanMode ? " [LAN]" : " [Server]"));
        setSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 620));
        getContentPane().setBackground(Constants.BG_DARK);
        setLayout(new BorderLayout(16, 16));

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(Constants.BG_DARK);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildConversationArea(), BorderLayout.CENTER);

        add(root, BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        RoundedPanel sidebar = new RoundedPanel(Constants.BG_SECONDARY, 28);
        sidebar.setLayout(new BorderLayout(0, 16));
        sidebar.setPreferredSize(new Dimension(Constants.USER_PANEL_WIDTH, 0));
        sidebar.setBorder(new EmptyBorder(22, 18, 18, 18));

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel appLabel = new JLabel("ChatApp");
        appLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        appLabel.setForeground(Constants.TEXT_PRIMARY);
        headerPanel.add(appLabel);
        headerPanel.add(Box.createVerticalStrut(4));

        JLabel modeLabel = new JLabel(isLanMode ? "LAN chat mode" : "Server chat mode");
        modeLabel.setFont(Constants.FONT_BODY);
        modeLabel.setForeground(Constants.TEXT_SECONDARY);
        headerPanel.add(modeLabel);
        headerPanel.add(Box.createVerticalStrut(18));

        RoundedPanel currentUserCard = new RoundedPanel(Constants.CARD_BLUE, 22);
        currentUserCard.setLayout(new BorderLayout(12, 0));
        currentUserCard.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel avatar = new JLabel(currentUser.substring(0, 1).toUpperCase(), SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(Constants.ACCENT);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 18));
        avatar.setPreferredSize(new Dimension(42, 42));
        avatar.setBorder(BorderFactory.createEmptyBorder());
        currentUserCard.add(avatar, BorderLayout.WEST);

        JPanel currentUserText = new JPanel();
        currentUserText.setOpaque(false);
        currentUserText.setLayout(new BoxLayout(currentUserText, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(currentUser);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLabel.setForeground(Constants.TEXT_PRIMARY);
        currentUserText.add(nameLabel);

        JLabel statusLabel = new JLabel(isLanMode ? "Discoverable on LAN" : "Connected account");
        statusLabel.setFont(Constants.FONT_SMALL);
        statusLabel.setForeground(Constants.TEXT_SECONDARY);
        currentUserText.add(statusLabel);

        currentUserCard.add(currentUserText, BorderLayout.CENTER);
        headerPanel.add(currentUserCard);

        sidebar.add(headerPanel, BorderLayout.NORTH);

        JPanel listWrapper = new JPanel(new BorderLayout(0, 12));
        listWrapper.setOpaque(false);

        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setOpaque(false);
        JLabel listTitle = new JLabel("Online users");
        listTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        listTitle.setForeground(Constants.TEXT_PRIMARY);
        listHeader.add(listTitle, BorderLayout.WEST);

        JLabel hint = new JLabel("Select one to start");
        hint.setFont(Constants.FONT_SMALL);
        hint.setForeground(Constants.TEXT_MUTED);
        listHeader.add(hint, BorderLayout.EAST);
        listWrapper.add(listHeader, BorderLayout.NORTH);

        userList = new JList<>(userListModel);
        userList.setBackground(Constants.BG_SECONDARY);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFixedCellHeight(62);
        userList.setCellRenderer(new UserCellRenderer());
        userList.setBorder(new EmptyBorder(0, 0, 0, 0));

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(null);
        userScroll.getViewport().setBackground(Constants.BG_SECONDARY);
        userScroll.getVerticalScrollBar().setUnitIncrement(16);
        userScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listWrapper.add(userScroll, BorderLayout.CENTER);

        sidebar.add(listWrapper, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildConversationArea() {
        RoundedPanel conversationPanel = new RoundedPanel(Constants.BG_SECONDARY, 28);
        conversationPanel.setLayout(new BorderLayout(0, 0));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(20, 24, 16, 24));

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));

        chatHeaderLabel = new JLabel("Select a conversation");
        chatHeaderLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        chatHeaderLabel.setForeground(Constants.TEXT_PRIMARY);
        headerText.add(chatHeaderLabel);

        chatSubHeaderLabel = new JLabel("Your messages will appear as clean chat bubbles");
        chatSubHeaderLabel.setFont(Constants.FONT_BODY);
        chatSubHeaderLabel.setForeground(Constants.TEXT_SECONDARY);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(chatSubHeaderLabel);

        header.add(headerText, BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);

        JLabel modePill = createPillLabel(isLanMode ? "LAN" : "SERVER", Constants.CARD_GREEN, Constants.ONLINE_GREEN);
        headerActions.add(modePill);

        logoutButton = createSoftButton("Logout", Constants.CARD_PINK, Constants.ERROR_RED, 16);
        logoutButton.setPreferredSize(new Dimension(108, 40));
        headerActions.add(logoutButton);

        header.add(headerActions, BorderLayout.EAST);
        conversationPanel.add(header, BorderLayout.NORTH);

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Constants.BG_DARK);
        chatPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        chatScrollPane = new JScrollPane(chatPanel);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getViewport().setBackground(Constants.BG_DARK);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        conversationPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel composerOuter = new JPanel(new BorderLayout());
        composerOuter.setOpaque(false);
        composerOuter.setBorder(new EmptyBorder(12, 20, 20, 20));

        RoundedPanel composer = new RoundedPanel(Constants.BG_INPUT, 24);
        composer.setLayout(new BorderLayout(10, 0));
        composer.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);
        attachButton = createIconButton(ActionIcon.ATTACH);
        attachButton.setToolTipText("Attach file");
        emojiButton = createIconButton(ActionIcon.EMOJI);
        emojiButton.setToolTipText("Insert emoji");
        leftActions.add(attachButton);
        leftActions.add(emojiButton);
        composer.add(leftActions, BorderLayout.WEST);

        messageField = createMessageField();
        composer.add(messageField, BorderLayout.CENTER);

        sendButton = createSoftButton("Send", Constants.ACCENT, Color.WHITE, 15);
        sendButton.setPreferredSize(new Dimension(88, 42));
        composer.add(sendButton, BorderLayout.EAST);
        setComposerEnabled(false);

        composerOuter.add(composer, BorderLayout.CENTER);
        conversationPanel.add(composerOuter, BorderLayout.SOUTH);

        return conversationPanel;
    }

    private JTextField createMessageField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setBackground(Constants.BG_INPUT);
        field.setForeground(Constants.TEXT_PRIMARY);
        field.setCaretColor(Constants.TEXT_PRIMARY);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return field;
    }

    private JLabel createPillLabel(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setBorder(new EmptyBorder(8, 12, 8, 12));
        return label;
    }

    private JButton createSoftButton(String text, Color background, Color foreground, int fontSize) {
        JButton button = new RoundedButton(background, foreground);
        button.setText(text);
        button.setFont(new Font("Dialog", Font.BOLD, fontSize));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createIconButton(ActionIcon iconType) {
        JButton button = new IconButton(Constants.BG_SECONDARY, Constants.TEXT_PRIMARY, iconType);
        button.setPreferredSize(new Dimension(42, 42));
        button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    public void addMessage(String sender, String content, long timestamp, boolean isSent) {
        addMessage(sender, content, timestamp, isSent, false);
    }

    public void addMessage(String sender, String content, long timestamp, boolean isSent, boolean isFile) {
        JPanel row = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 12, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        RoundedPanel bubble = new RoundedPanel(isSent ? Constants.BUBBLE_SENT : Constants.BUBBLE_RECV, 24);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(12, 16, 12, 16));
        bubble.setMaximumSize(new Dimension(430, Integer.MAX_VALUE));

        if (!isSent) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            senderLabel.setForeground(Constants.ACCENT);
            senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(senderLabel);
            bubble.add(Box.createVerticalStrut(6));
        }

        EmojiSpec emojiSpec = !isFile ? findEmojiSpec(content) : null;
        if (emojiSpec != null) {
            EmojiView emojiView = new EmojiView(emojiSpec.kind, isSent ? new Dimension(42, 42) : new Dimension(38, 38));
            emojiView.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(emojiView);
        } else {
            JLabel contentLabel = new JLabel(toBubbleHtml(content, isFile));
            contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            contentLabel.setForeground(isSent ? Color.WHITE : Constants.TEXT_PRIMARY);
            contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(contentLabel);
        }
        bubble.add(Box.createVerticalStrut(8));

        JPanel metaRow = new JPanel(new BorderLayout());
        metaRow.setOpaque(false);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        JLabel timeLabel = new JLabel(sdf.format(new Date(timestamp)));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setForeground(isSent ? new Color(225, 236, 255) : Constants.TEXT_MUTED);
        metaRow.add(timeLabel, isSent ? BorderLayout.EAST : BorderLayout.WEST);
        bubble.add(metaRow);

        row.add(bubble);
        chatPanel.add(row);
        refreshChatArea();
    }

    public void addSystemMessage(String message) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(4, 0, 14, 0));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        RoundedPanel pill = new RoundedPanel(Constants.BG_TERTIARY, 18);
        pill.setLayout(new BorderLayout());
        pill.setBorder(new EmptyBorder(8, 14, 8, 14));

        JLabel label = new JLabel(message);
        label.setFont(Constants.FONT_SMALL);
        label.setForeground(Constants.TEXT_SECONDARY);
        pill.add(label, BorderLayout.CENTER);
        wrapper.add(pill);

        chatPanel.add(wrapper);
        refreshChatArea();
    }

    public void clearChat() {
        chatPanel.removeAll();
        refreshChatArea();
    }

    public void updateUserList(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            String selected = userList.getSelectedValue();
            userListModel.clear();
            for (String user : users) {
                if (!currentUser.equals(user)) {
                    userListModel.addElement(user);
                }
            }
            if (selected != null && userListModel.contains(selected)) {
                userList.setSelectedValue(selected, true);
            }
        });
    }

    private void showEmojiPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Constants.BORDER_COLOR));
        popup.setBackground(Constants.BG_SECONDARY);

        JPanel emojiGrid = new JPanel(new GridLayout(2, 4, 8, 8));
        emojiGrid.setBackground(Constants.BG_SECONDARY);
        emojiGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        for (EmojiSpec emojiSpec : EMOJI_SPECS) {
            JButton emojiItem = new EmojiButton(emojiSpec.kind);
            emojiItem.setPreferredSize(new Dimension(48, 48));
            emojiItem.addActionListener(e -> {
                messageField.setText(messageField.getText() + emojiSpec.code);
                popup.setVisible(false);
                messageField.requestFocusInWindow();
            });
            emojiGrid.add(emojiItem);
        }

        popup.add(emojiGrid);
        popup.show(emojiButton, 0, -popup.getPreferredSize().height);
    }

    private String toBubbleHtml(String content, boolean isFile) {
        String safe = content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
        String prefix = isFile ? "<b>[File]</b> " : "";
        return "<html><div style='width:260px; line-height:1.4;'>" + prefix + safe + "</div></html>";
    }

    private void refreshChatArea() {
        chatPanel.revalidate();
        chatPanel.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = chatScrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }

    public String getMessageText() {
        String text = messageField.getText().trim();
        messageField.setText("");
        return text;
    }

    public String getSelectedUser() {
        return userList.getSelectedValue();
    }

    public File chooseAttachment() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select file to send");
        int result = chooser.showOpenDialog(this);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    public void focusMessageField() {
        messageField.requestFocusInWindow();
    }

    public void setChatHeader(String userName) {
        selectedUser = userName;
        chatHeaderLabel.setText(userName);
        chatSubHeaderLabel.setText("Private conversation with " + userName);
        setComposerEnabled(true);
    }

    public void addSendListener(ActionListener listener) {
        sendButton.addActionListener(listener);
        messageField.addActionListener(listener);
    }

    public void addUserSelectionListener(javax.swing.event.ListSelectionListener listener) {
        userList.addListSelectionListener(listener);
    }

    public void addEmojiListener() {
        emojiButton.addActionListener(e -> showEmojiPopup());
    }

    public void addAttachListener(ActionListener listener) {
        attachButton.addActionListener(listener);
    }

    public void addLogoutListener(ActionListener listener) {
        logoutButton.addActionListener(listener);
    }

    public void addWindowCloseListener(java.awt.event.WindowListener listener) {
        addWindowListener(listener);
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public boolean isLanMode() {
        return isLanMode;
    }

    private void setComposerEnabled(boolean enabled) {
        messageField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        attachButton.setEnabled(enabled);
        emojiButton.setEnabled(enabled);
        if (!enabled) {
            messageField.setText("");
        }
    }

    private EmojiSpec findEmojiSpec(String content) {
        String normalized = content == null ? "" : content.trim();
        for (EmojiSpec emojiSpec : EMOJI_SPECS) {
            if (emojiSpec.code.equals(normalized)) {
                return emojiSpec;
            }
        }
        return null;
    }

    private static class UserCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JPanel container = new JPanel(new BorderLayout(10, 0));
            container.setBorder(new EmptyBorder(8, 10, 8, 10));
            container.setBackground(isSelected ? Constants.CARD_BLUE : Constants.BG_SECONDARY);

            JLabel avatar = new JLabel(value.toString().substring(0, 1).toUpperCase(), SwingConstants.CENTER);
            avatar.setOpaque(true);
            avatar.setBackground(isSelected ? Constants.ACCENT : Constants.BG_TERTIARY);
            avatar.setForeground(isSelected ? Color.WHITE : Constants.TEXT_PRIMARY);
            avatar.setFont(new Font("Segoe UI", Font.BOLD, 14));
            avatar.setPreferredSize(new Dimension(38, 38));
            container.add(avatar, BorderLayout.WEST);

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

            JLabel nameLabel = new JLabel(value.toString());
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLabel.setForeground(Constants.TEXT_PRIMARY);
            textPanel.add(nameLabel);

            JLabel statusLabel = new JLabel("Online now");
            statusLabel.setFont(Constants.FONT_SMALL);
            statusLabel.setForeground(Constants.ONLINE_GREEN);
            textPanel.add(statusLabel);

            container.add(textPanel, BorderLayout.CENTER);
            return container;
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color fillColor;
        private final int arc;

        private RoundedPanel(Color fillColor, int arc) {
            this.fillColor = fillColor;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedButton extends JButton {
        private final Color baseColor;
        private final Color textColor;

        private RoundedButton(Color baseColor, Color textColor) {
            this.baseColor = baseColor;
            this.textColor = textColor;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = isEnabled() ? (getModel().isRollover() ? baseColor.darker() : baseColor) : Constants.BORDER_COLOR;
            if (getModel().isPressed()) {
                fill = fill.darker();
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.dispose();

            setForeground(textColor);
            super.paintComponent(g);
        }
    }

    private static class IconButton extends RoundedButton {
        private final ActionIcon iconType;

        private IconButton(Color baseColor, Color textColor, ActionIcon iconType) {
            super(baseColor, textColor);
            this.iconType = iconType;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isEnabled() ? getForeground() : Constants.TEXT_MUTED);
            int w = getWidth();
            int h = getHeight();

            if (iconType == ActionIcon.ATTACH) {
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(w / 2 - 8, h / 2 - 10, 14, 18, 300, 250);
                g2.drawArc(w / 2 - 4, h / 2 - 8, 10, 14, 300, 250);
            } else {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(w / 2 - 9, h / 2 - 9, 18, 18);
                g2.fillOval(w / 2 - 4, h / 2 - 2, 2, 2);
                g2.fillOval(w / 2 + 2, h / 2 - 2, 2, 2);
                g2.drawArc(w / 2 - 5, h / 2, 10, 6, 200, 140);
            }
            g2.dispose();
        }
    }

    private static class EmojiButton extends JButton {
        private final EmojiKind kind;

        private EmojiButton(EmojiKind kind) {
            this.kind = kind;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? Constants.CARD_BLUE : Constants.BG_INPUT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);

            EmojiView.paintEmoji((Graphics2D) g.create(), kind, getWidth(), getHeight(), 28);
        }
    }

    private static class EmojiView extends JComponent {
        private final EmojiKind kind;
        private final Dimension size;

        private EmojiView(EmojiKind kind, Dimension size) {
            this.kind = kind;
            this.size = size;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return size;
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintEmoji((Graphics2D) g.create(), kind, getWidth(), getHeight(), Math.min(getWidth(), getHeight()) - 8);
        }

        private static void paintEmoji(Graphics2D g2, EmojiKind kind, int width, int height, int diameter) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = (width - diameter) / 2;
            int y = (height - diameter) / 2;
            Color face = switch (kind) {
                case LOVE -> new Color(255, 209, 102);
                case ANGRY -> new Color(255, 168, 107);
                case SAD -> new Color(137, 197, 255);
                default -> new Color(255, 212, 84);
            };
            g2.setColor(face);
            g2.fillOval(x, y, diameter, diameter);
            g2.setColor(new Color(92, 62, 34));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if (kind == EmojiKind.LIKE) {
                int cx = width / 2;
                int cy = height / 2;
                Polygon thumb = new Polygon(
                        new int[]{cx - 6, cx - 1, cx + 2, cx + 6, cx + 6, cx - 2},
                        new int[]{cy + 4, cy + 4, cy - 2, cy - 2, cy + 7, cy + 7},
                        6);
                g2.setColor(new Color(76, 140, 255));
                g2.fillPolygon(thumb);
                g2.setColor(new Color(76, 140, 255));
                g2.fillRoundRect(cx - 9, cy - 1, 5, 10, 3, 3);
                g2.dispose();
                return;
            }

            g2.fillOval(width / 2 - 6, height / 2 - 4, 3, 3);
            g2.fillOval(width / 2 + 3, height / 2 - 4, 3, 3);

            switch (kind) {
                case SMILE -> g2.drawArc(width / 2 - 7, height / 2 - 1, 14, 9, 200, 140);
                case LAUGH -> {
                    g2.drawLine(width / 2 - 8, height / 2 - 5, width / 2 - 3, height / 2 - 2);
                    g2.drawLine(width / 2 - 8, height / 2 - 2, width / 2 - 3, height / 2 - 5);
                    g2.drawLine(width / 2 + 3, height / 2 - 5, width / 2 + 8, height / 2 - 2);
                    g2.drawLine(width / 2 + 3, height / 2 - 2, width / 2 + 8, height / 2 - 5);
                    g2.drawArc(width / 2 - 8, height / 2 - 1, 16, 10, 190, 160);
                }
                case LOVE -> {
                    g2.setColor(new Color(255, 84, 112));
                    g2.fillOval(width / 2 - 7, height / 2 - 5, 5, 5);
                    g2.fillOval(width / 2 + 2, height / 2 - 5, 5, 5);
                    g2.fillPolygon(
                            new int[]{width / 2 - 8, width / 2, width / 2 + 8},
                            new int[]{height / 2 - 2, height / 2 + 5, height / 2 - 2},
                            3);
                    g2.setColor(new Color(92, 62, 34));
                    g2.drawArc(width / 2 - 7, height / 2 + 1, 14, 7, 200, 140);
                }
                case COOL -> {
                    g2.setColor(new Color(52, 73, 94));
                    g2.fillRoundRect(width / 2 - 10, height / 2 - 6, 8, 5, 3, 3);
                    g2.fillRoundRect(width / 2 + 2, height / 2 - 6, 8, 5, 3, 3);
                    g2.fillRect(width / 2 - 2, height / 2 - 5, 4, 2);
                    g2.setColor(new Color(92, 62, 34));
                    g2.drawArc(width / 2 - 7, height / 2 + 1, 14, 7, 200, 140);
                }
                case WOW -> g2.drawOval(width / 2 - 3, height / 2 + 1, 6, 7);
                case SAD -> g2.drawArc(width / 2 - 7, height / 2 + 4, 14, 7, 20, 140);
                case ANGRY -> {
                    g2.drawLine(width / 2 - 8, height / 2 - 6, width / 2 - 3, height / 2 - 4);
                    g2.drawLine(width / 2 + 3, height / 2 - 4, width / 2 + 8, height / 2 - 6);
                    g2.drawArc(width / 2 - 7, height / 2 + 4, 14, 7, 20, 140);
                }
                default -> g2.drawArc(width / 2 - 7, height / 2 - 1, 14, 9, 200, 140);
            }
            g2.dispose();
        }
    }

    private enum ActionIcon {
        ATTACH,
        EMOJI
    }

    private enum EmojiKind {
        SMILE,
        LAUGH,
        LOVE,
        COOL,
        WOW,
        SAD,
        ANGRY,
        LIKE
    }

    private static class EmojiSpec {
        private final String code;
        private final EmojiKind kind;

        private EmojiSpec(String code, EmojiKind kind) {
            this.code = code;
            this.kind = kind;
        }
    }
}
