package com.chatapp.view;

import com.chatapp.model.Group;
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
    private final DefaultListModel<GroupListItem> groupListModel = new DefaultListModel<>();
    private JList<String> userList;
    private JList<GroupListItem> groupList;
    private JPanel chatPanel;
    private JScrollPane chatScrollPane;
    private JTextField messageField;
    private JButton sendButton;
    private JButton emojiButton;
    private JButton attachButton;
    private JButton logoutButton;
    private JButton roomButton;
    private JButton createGroupButton;
    private JButton joinGroupButton;
    private JButton leaveGroupButton;
    private JLabel chatHeaderLabel;
    private JLabel chatSubHeaderLabel;
    private final String currentUser;
    private final boolean isLanMode;
    private GroupListItem selectedGroupItem;
    
    // Reply functionality UI
    private JPanel replyPreviewWrapper;
    private JPanel replyPreviewPanel;
    private JLabel replyHeaderLabel;
    private JLabel replyContentLabel;
    private JButton cancelReplyButton;
    private com.chatapp.model.Message replyingToMessage = null;

    public ChatView(String currentUser, boolean isLanMode) {
        this.currentUser = currentUser;
        this.isLanMode = isLanMode;
        initUI();
    }

    private void initUI() {
        setTitle("ChatApp - " + currentUser + (isLanMode ? " [LAN]" : " [Server]"));
        setSize(Constants.WINDOW_WIDTH + 60, Constants.WINDOW_HEIGHT);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(980, 640));
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
        sidebar.setLayout(new BorderLayout(0, 18));
        sidebar.setPreferredSize(new Dimension(320, 0));
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

        JPanel listsPanel = new JPanel();
        listsPanel.setOpaque(false);
        listsPanel.setLayout(new BoxLayout(listsPanel, BoxLayout.Y_AXIS));

        listsPanel.add(buildPrivateSection());
        listsPanel.add(Box.createVerticalStrut(16));
        listsPanel.add(buildGroupSection());

        sidebar.add(listsPanel, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildPrivateSection() {
        JPanel privateSection = new JPanel(new BorderLayout(0, 10));
        privateSection.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Private chats");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(Constants.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JLabel hint = new JLabel("1-to-1");
        hint.setFont(Constants.FONT_SMALL);
        hint.setForeground(Constants.TEXT_MUTED);
        header.add(hint, BorderLayout.EAST);
        privateSection.add(header, BorderLayout.NORTH);

        userList = new JList<>(userListModel);
        userList.setBackground(Constants.BG_SECONDARY);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFixedCellHeight(60);
        userList.setCellRenderer(new UserCellRenderer());

        JScrollPane scroll = new JScrollPane(userList);
        scroll.setBorder(BorderFactory.createLineBorder(Constants.BORDER_COLOR));
        scroll.getViewport().setBackground(Constants.BG_SECONDARY);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(0, 180));
        privateSection.add(scroll, BorderLayout.CENTER);

        return privateSection;
    }

    private JPanel buildGroupSection() {
        JPanel groupSection = new JPanel(new BorderLayout(0, 10));
        groupSection.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Groups");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(Constants.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        createGroupButton = createMiniButton("+");
        createGroupButton.setToolTipText("Create group");
        joinGroupButton = createMiniButton("↗");
        joinGroupButton.setToolTipText("Join group");
        leaveGroupButton = createMiniButton("−");
        leaveGroupButton.setToolTipText("Leave group");
        actions.add(createGroupButton);
        actions.add(joinGroupButton);
        actions.add(leaveGroupButton);
        header.add(actions, BorderLayout.EAST);
        groupSection.add(header, BorderLayout.NORTH);

        groupList = new JList<>(groupListModel);
        groupList.setBackground(Constants.BG_SECONDARY);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setFixedCellHeight(66);
        groupList.setCellRenderer(new GroupCellRenderer());

        JScrollPane scroll = new JScrollPane(groupList);
        scroll.setBorder(BorderFactory.createLineBorder(Constants.BORDER_COLOR));
        scroll.getViewport().setBackground(Constants.BG_SECONDARY);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(0, 220));
        groupSection.add(scroll, BorderLayout.CENTER);

        if (isLanMode) {
            createGroupButton.setEnabled(false);
            joinGroupButton.setEnabled(false);
            leaveGroupButton.setEnabled(false);
            JLabel lanHint = new JLabel("Groups are available in server mode.");
            lanHint.setFont(Constants.FONT_SMALL);
            lanHint.setForeground(Constants.TEXT_MUTED);
            groupSection.add(lanHint, BorderLayout.SOUTH);
        }

        return groupSection;
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

        chatHeaderLabel = new JLabel("Public Room");
        chatHeaderLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        chatHeaderLabel.setForeground(Constants.TEXT_PRIMARY);
        headerText.add(chatHeaderLabel);

        chatSubHeaderLabel = new JLabel("Everyone is here. Click a user for private chat.");
        chatSubHeaderLabel.setFont(Constants.FONT_BODY);
        chatSubHeaderLabel.setForeground(Constants.TEXT_SECONDARY);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(chatSubHeaderLabel);

        header.add(headerText, BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);

        JLabel modePill = createPillLabel(isLanMode ? "LAN" : "SERVER", Constants.CARD_GREEN, Constants.ONLINE_GREEN);
        headerActions.add(modePill);

        roomButton = createSoftButton("Room", Constants.CARD_GREEN, Constants.ONLINE_GREEN, 14);
        roomButton.setPreferredSize(new Dimension(80, 40));
        roomButton.setToolTipText("Back to Public Room");
        headerActions.add(roomButton);

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

        JPanel composerOuter = new JPanel(new BorderLayout(0, 8));
        composerOuter.setOpaque(false);
        composerOuter.setBorder(new EmptyBorder(12, 20, 20, 20));

        // Reply preview wrapper
        replyPreviewWrapper = new JPanel(new BorderLayout());
        replyPreviewWrapper.setOpaque(false);
        replyPreviewWrapper.setVisible(false);
        
        replyPreviewPanel = new RoundedPanel(Constants.BG_INPUT, 12);
        replyPreviewPanel.setLayout(new BorderLayout());
        replyPreviewPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, Constants.ACCENT),
            new EmptyBorder(8, 12, 8, 12)
        ));
        
        JPanel replyHeader = new JPanel(new BorderLayout());
        replyHeader.setOpaque(false);
        replyHeaderLabel = new JLabel("Phản hồi:");
        replyHeaderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        replyHeaderLabel.setForeground(Constants.ACCENT);
        
        cancelReplyButton = new JButton("<html><b>✕</b></html>");
        cancelReplyButton.setBorderPainted(false);
        cancelReplyButton.setContentAreaFilled(false);
        cancelReplyButton.setForeground(Constants.TEXT_MUTED);
        cancelReplyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelReplyButton.addActionListener(e -> cancelReply());
        
        replyHeader.add(replyHeaderLabel, BorderLayout.WEST);
        replyHeader.add(cancelReplyButton, BorderLayout.EAST);
        
        replyContentLabel = new JLabel("");
        replyContentLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        replyContentLabel.setForeground(Constants.TEXT_SECONDARY);
        
        replyPreviewPanel.add(replyHeader, BorderLayout.NORTH);
        replyPreviewPanel.add(replyContentLabel, BorderLayout.CENTER);
        
        replyPreviewWrapper.add(replyPreviewPanel, BorderLayout.CENTER);
        composerOuter.add(replyPreviewWrapper, BorderLayout.NORTH);

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
        setComposerEnabled(true);

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

    private JButton createMiniButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
        button.setForeground(Constants.ACCENT);
        button.setBackground(Constants.CARD_BLUE);
        button.setPreferredSize(new Dimension(34, 30));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
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

    public void addBubbleMessage(com.chatapp.model.Message msg, boolean isSent) {
        boolean showSender = msg.getGroupId() != null && !msg.getGroupId().isEmpty();
        
        JPanel row = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 12, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        RoundedPanel bubble = new RoundedPanel(isSent ? Constants.BUBBLE_SENT : Constants.BUBBLE_RECV, 24);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(12, 16, 12, 16));
        bubble.setMaximumSize(new Dimension(460, Integer.MAX_VALUE));

        if (showSender || (!isSent && msg.getSender() != null && !msg.getSender().isEmpty())) {
            JLabel senderLabel = new JLabel("[" + msg.getSender() + "]");
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            senderLabel.setForeground(isSent ? new Color(225, 236, 255) : Constants.ACCENT);
            senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(senderLabel);
            bubble.add(Box.createVerticalStrut(6));
        }

        // Render Reply Snippet if exists
        if (msg.getReplySnippet() != null && !msg.getReplySnippet().isEmpty()) {
            JPanel replyPanel = new JPanel(new BorderLayout());
            replyPanel.setOpaque(false);
            replyPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(255, 255, 255, 100)),
                new EmptyBorder(0, 10, 4, 0)
            ));
            
            JLabel replyHeader = new JLabel("Phản hồi " + msg.getReplySender() + ":");
            replyHeader.setFont(new Font("Segoe UI", Font.BOLD, 10));
            replyHeader.setForeground(isSent ? new Color(255, 255, 255, 180) : Constants.TEXT_MUTED);
            
            JLabel replyContent = new JLabel("<html><i>" + msg.getReplySnippet() + "</i></html>");
            replyContent.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            replyContent.setForeground(isSent ? new Color(255, 255, 255, 180) : Constants.TEXT_MUTED);
            
            replyPanel.add(replyHeader, BorderLayout.NORTH);
            replyPanel.add(replyContent, BorderLayout.CENTER);
            
            bubble.add(replyPanel);
            bubble.add(Box.createVerticalStrut(6));
        }

        boolean isFile = Constants.TYPE_FILE.equals(msg.getType());
        String content = isFile && msg.getFileName() != null ? msg.getFileName() : msg.getContent();

        if (msg.isUnsent()) {
            JLabel contentLabel = new JLabel("<html><i>Tin nhắn đã bị thu hồi</i></html>");
            contentLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            contentLabel.setForeground(isSent ? new Color(225, 236, 255, 180) : Constants.TEXT_MUTED);
            contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(contentLabel);
        } else {
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
        }
        bubble.add(Box.createVerticalStrut(8));

        JPanel metaRow = new JPanel(new BorderLayout());
        metaRow.setOpaque(false);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        JLabel timeLabel = new JLabel(sdf.format(new Date(msg.getTimestamp())));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setForeground(isSent ? new Color(225, 236, 255) : Constants.TEXT_MUTED);
        metaRow.add(timeLabel, isSent ? BorderLayout.EAST : BorderLayout.WEST);
        bubble.add(metaRow);

        bubble.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = new JPopupMenu();
                    
                    JMenuItem replyItem = new JMenuItem("Phản hồi");
                    replyItem.addActionListener(ev -> {
                        if (messageActionCallback != null) messageActionCallback.onReply(msg);
                    });
                    menu.add(replyItem);
                    
                    if (isSent && !msg.isUnsent()) {
                        JMenuItem unsendItem = new JMenuItem("Thu hồi tin nhắn");
                        unsendItem.addActionListener(ev -> {
                            if (messageActionCallback != null) messageActionCallback.onUnsend(msg);
                        });
                        menu.add(unsendItem);
                    }
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        row.add(bubble);
        chatPanel.add(row);
        refreshChatArea();
    }

    public void addMessage(String sender, String content, long timestamp, boolean isSent, boolean isFile) {
        addMessage(sender, content, timestamp, isSent, isFile, false);
    }

    public void addMessage(String sender, String content, long timestamp, boolean isSent, boolean isFile, boolean showSender) {
        JPanel row = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 12, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        RoundedPanel bubble = new RoundedPanel(isSent ? Constants.BUBBLE_SENT : Constants.BUBBLE_RECV, 24);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(12, 16, 12, 16));
        bubble.setMaximumSize(new Dimension(460, Integer.MAX_VALUE));

        if (showSender || (!isSent && sender != null && !sender.isEmpty())) {
            JLabel senderLabel = new JLabel("[" + sender + "]");
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            senderLabel.setForeground(isSent ? new Color(225, 236, 255) : Constants.ACCENT);
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

    public void updateGroupList(List<Group> groups) {
        SwingUtilities.invokeLater(() -> {
            String selectedGroupId = getSelectedGroupId();
            groupListModel.clear();
            for (Group group : groups) {
                groupListModel.addElement(new GroupListItem(group.getGroupId(), group.getGroupName(), group.getMembers().size()));
            }
            if (selectedGroupId != null) {
                for (int i = 0; i < groupListModel.size(); i++) {
                    if (selectedGroupId.equals(groupListModel.get(i).groupId)) {
                        groupList.setSelectedIndex(i);
                        break;
                    }
                }
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
        String safe = content == null ? "" : content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
        String prefix = isFile ? "<b>[File]</b> " : "";
        return "<html><div style='width:280px; line-height:1.4;'>" + prefix + safe + "</div></html>";
    }

    private void refreshChatArea() {
        chatPanel.revalidate();
        chatPanel.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = chatScrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }

    // ───── Public Control API ─────

    public interface MessageActionCallback {
        void onReply(com.chatapp.model.Message msg);
        void onUnsend(com.chatapp.model.Message msg);
    }
    
    private MessageActionCallback messageActionCallback;
    
    public void setMessageActionCallback(MessageActionCallback callback) {
        this.messageActionCallback = callback;
    }
    
    public void startReply(com.chatapp.model.Message msg) {
        this.replyingToMessage = msg;
        replyHeaderLabel.setText("Phản hồi " + msg.getSender() + ":");
        replyContentLabel.setText((Constants.TYPE_FILE.equals(msg.getType()) ? "[Đính kèm] " + msg.getFileName() : msg.getContent()));
        replyPreviewWrapper.setVisible(true);
        messageField.requestFocusInWindow();
    }
    
    public void cancelReply() {
        this.replyingToMessage = null;
        replyPreviewWrapper.setVisible(false);
    }
    
    public com.chatapp.model.Message getReplyingToMessage() {
        return this.replyingToMessage;
    }

    public String getMessageText() {
        String text = messageField.getText().trim();
        messageField.setText("");
        return text;
    }

    public String getSelectedUser() {
        return userList.getSelectedValue();
    }

    public String getSelectedGroupId() {
        GroupListItem item = groupList.getSelectedValue();
        selectedGroupItem = item;
        return item == null ? null : item.groupId;
    }

    public String getSelectedGroupName() {
        GroupListItem item = groupList.getSelectedValue();
        return item == null ? null : item.groupName;
    }

    public int getSelectedGroupMemberCount() {
        GroupListItem item = groupList.getSelectedValue();
        return item == null ? 0 : item.memberCount;
    }

    public void clearUserSelection() {
        userList.clearSelection();
    }

    public void clearGroupSelection() {
        groupList.clearSelection();
        selectedGroupItem = null;
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

    public void setPrivateChatHeader(String userName) {
        chatHeaderLabel.setText(userName);
        chatSubHeaderLabel.setText("Private conversation");
        setComposerEnabled(true);
    }

    public void setGroupChatHeader(String groupName, int memberCount) {
        chatHeaderLabel.setText(groupName);
        chatSubHeaderLabel.setText(memberCount + " members in this group");
        setComposerEnabled(true);
    }

    public void resetConversationState() {
        chatHeaderLabel.setText("Public Room");
        chatSubHeaderLabel.setText("Everyone is here. Click a user for private chat.");
        setComposerEnabled(false);
    }

    public void addSendListener(ActionListener listener) {
        sendButton.addActionListener(listener);
        messageField.addActionListener(listener);
    }

    public void addUserSelectionListener(javax.swing.event.ListSelectionListener listener) {
        userList.addListSelectionListener(listener);
    }

    public void addGroupSelectionListener(javax.swing.event.ListSelectionListener listener) {
        groupList.addListSelectionListener(listener);
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

    public void addRoomButtonListener(ActionListener listener) {
        roomButton.addActionListener(listener);
    }

    public void addCreateGroupListener(ActionListener listener) {
        createGroupButton.addActionListener(listener);
    }

    public void addJoinGroupListener(ActionListener listener) {
        joinGroupButton.addActionListener(listener);
    }

    public void addLeaveGroupListener(ActionListener listener) {
        leaveGroupButton.addActionListener(listener);
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
        if (content == null) {
            return null;
        }
        String normalized = content.trim();
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

    private static class GroupCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            GroupListItem item = (GroupListItem) value;
            JPanel container = new JPanel(new BorderLayout(10, 0));
            container.setBorder(new EmptyBorder(8, 10, 8, 10));
            container.setBackground(isSelected ? Constants.CARD_GREEN : Constants.BG_SECONDARY);

            JLabel icon = new JLabel("G", SwingConstants.CENTER);
            icon.setOpaque(true);
            icon.setBackground(isSelected ? Constants.ONLINE_GREEN : Constants.BG_TERTIARY);
            icon.setForeground(isSelected ? Color.WHITE : Constants.TEXT_PRIMARY);
            icon.setFont(new Font("Segoe UI", Font.BOLD, 14));
            icon.setPreferredSize(new Dimension(38, 38));
            container.add(icon, BorderLayout.WEST);

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

            JLabel nameLabel = new JLabel(item.groupName);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLabel.setForeground(Constants.TEXT_PRIMARY);
            textPanel.add(nameLabel);

            JLabel metaLabel = new JLabel(item.groupId + " • " + item.memberCount + " members");
            metaLabel.setFont(Constants.FONT_SMALL);
            metaLabel.setForeground(Constants.TEXT_SECONDARY);
            textPanel.add(metaLabel);

            container.add(textPanel, BorderLayout.CENTER);
            return container;
        }
    }

    private static class GroupListItem {
        private final String groupId;
        private final String groupName;
        private final int memberCount;

        private GroupListItem(String groupId, String groupName, int memberCount) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.memberCount = memberCount;
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
                    g2.fillPolygon(new int[]{width / 2 - 8, width / 2, width / 2 + 8},
                            new int[]{height / 2 - 2, height / 2 + 5, height / 2 - 2}, 3);
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
