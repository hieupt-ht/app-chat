package com.chatapp.view.admin;

import com.chatapp.util.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatManagementPanel extends JPanel {

    private final DefaultListModel<GroupEntry> groupListModel = new DefaultListModel<>();
    private JList<GroupEntry> groupList;
    private JComboBox<String> memberComboBox;
    private JPanel messagesContainer;
    private JButton refreshButton;
    private JButton removeMemberButton;
    private JButton deleteGroupButton;
    private JSONObject selectedMessage;
    private ActionListener deleteListener;
    private final List<JSONObject> cachedMessages = new ArrayList<>();

    public ChatManagementPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Constants.BG_DARK);

        RoundedPanel card = new RoundedPanel(Constants.BG_SECONDARY, 28);
        card.setLayout(new BorderLayout(16, 16));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Chat logs and groups");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Constants.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        refreshButton = createButton("Refresh", Constants.CARD_BLUE, Constants.ACCENT);
        header.add(refreshButton, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildGroupPanel(), buildMessagesPanel());
        splitPane.setBorder(null);
        splitPane.setDividerSize(10);
        splitPane.setOpaque(false);
        splitPane.setResizeWeight(0.34);
        card.add(splitPane, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    private JPanel buildGroupPanel() {
        RoundedPanel panel = new RoundedPanel(Constants.BG_INPUT, 24);
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Groups");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Constants.TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setFixedCellHeight(70);
        groupList.setCellRenderer(new GroupCellRenderer());
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateMembersForSelection();
                renderMessages();
            }
        });

        JScrollPane groupScroll = new JScrollPane(groupList);
        groupScroll.setBorder(BorderFactory.createLineBorder(Constants.BORDER_COLOR));
        groupScroll.getViewport().setBackground(Constants.BG_SECONDARY);
        panel.add(groupScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        JLabel membersLabel = new JLabel("Members");
        membersLabel.setFont(Constants.FONT_SMALL);
        membersLabel.setForeground(Constants.TEXT_SECONDARY);
        footer.add(membersLabel);
        footer.add(Box.createVerticalStrut(6));

        memberComboBox = new JComboBox<>();
        memberComboBox.setFont(Constants.FONT_BODY);
        footer.add(memberComboBox);
        footer.add(Box.createVerticalStrut(10));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        removeMemberButton = createButton("Remove Member", Constants.CARD_PINK, Constants.ERROR_RED);
        deleteGroupButton = createButton("Delete Group", Constants.CARD_PINK, Constants.ERROR_RED);
        actions.add(removeMemberButton);
        actions.add(deleteGroupButton);
        footer.add(actions);

        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildMessagesPanel() {
        RoundedPanel panel = new RoundedPanel(Constants.BG_DARK, 24);
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(Constants.BG_DARK);
        messagesContainer.setBorder(new EmptyBorder(6, 6, 6, 6));

        JScrollPane scrollPane = new JScrollPane(messagesContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(Constants.BORDER_COLOR));
        scrollPane.getViewport().setBackground(Constants.BG_DARK);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JButton createButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    public void updateGroups(JSONArray groupsArr) {
        String selectedGroupId = getSelectedGroupId();
        groupListModel.clear();

        if (groupsArr != null) {
            for (int i = 0; i < groupsArr.length(); i++) {
                JSONObject group = groupsArr.optJSONObject(i);
                if (group == null) {
                    continue;
                }
                JSONArray members = group.optJSONArray("members");
                List<String> memberNames = new ArrayList<>();
                if (members != null) {
                    for (int j = 0; j < members.length(); j++) {
                        memberNames.add(members.optString(j));
                    }
                }
                groupListModel.addElement(new GroupEntry(
                        group.optString("groupId"),
                        group.optString("groupName"),
                        memberNames));
            }
        }

        if (selectedGroupId != null) {
            for (int i = 0; i < groupListModel.size(); i++) {
                if (selectedGroupId.equals(groupListModel.getElementAt(i).groupId)) {
                    groupList.setSelectedIndex(i);
                    break;
                }
            }
        }

        updateMembersForSelection();
        renderMessages();
    }

    public void updateMessages(JSONArray messagesArr) {
        cachedMessages.clear();
        if (messagesArr != null) {
            for (int i = 0; i < messagesArr.length(); i++) {
                JSONObject message = messagesArr.optJSONObject(i);
                if (message != null) {
                    cachedMessages.add(message);
                }
            }
        }
        renderMessages();
    }

    private void renderMessages() {
        messagesContainer.removeAll();
        selectedMessage = null;

        String selectedGroupId = getSelectedGroupId();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (JSONObject message : cachedMessages) {
            String messageGroupId = message.optString("groupId", "");
            if (selectedGroupId != null && !selectedGroupId.equals(messageGroupId)) {
                continue;
            }
            messagesContainer.add(createMessageBubble(message, sdf));
            messagesContainer.add(Box.createVerticalStrut(12));
        }

        if (messagesContainer.getComponentCount() == 0) {
            messagesContainer.add(createEmptyState(selectedGroupId));
        }

        messagesContainer.revalidate();
        messagesContainer.repaint();
    }

    private Component createEmptyState(String selectedGroupId) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        wrapper.setOpaque(false);

        RoundedPanel pill = new RoundedPanel(Constants.BG_TERTIARY, 18);
        pill.setLayout(new BorderLayout());
        pill.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel label = new JLabel(selectedGroupId == null
                ? "Select a group to browse group-specific messages."
                : "No messages for this group.");
        label.setFont(Constants.FONT_BODY);
        label.setForeground(Constants.TEXT_SECONDARY);
        pill.add(label, BorderLayout.CENTER);
        wrapper.add(pill);
        return wrapper;
    }

    private JPanel createMessageBubble(JSONObject message, SimpleDateFormat sdf) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        boolean isFile = Constants.TYPE_FILE.equals(message.optString("type"));
        boolean isGroup = message.has("groupId") && !message.optString("groupId").isEmpty();

        RoundedPanel bubble = new RoundedPanel(isGroup ? Constants.CARD_GREEN : Constants.BG_SECONDARY, 24);
        bubble.setLayout(new BorderLayout(0, 12));
        bubble.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        String route = isGroup
                ? message.optString("sender") + " -> " + resolveGroupName(message.optString("groupId"))
                : message.optString("sender") + " -> " + message.optString("receiver");
        JLabel routeLabel = new JLabel(route);
        routeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        routeLabel.setForeground(Constants.TEXT_PRIMARY);
        top.add(routeLabel, BorderLayout.WEST);

        JButton deleteButton = createButton("Delete", Constants.CARD_PINK, Constants.ERROR_RED);
        deleteButton.addActionListener(e -> triggerDelete(message));
        top.add(deleteButton, BorderLayout.EAST);
        bubble.add(top, BorderLayout.NORTH);

        JLabel body = new JLabel(toHtml(isFile ? "[File] " + message.optString("content") : message.optString("content")));
        body.setFont(Constants.FONT_BODY);
        body.setForeground(Constants.TEXT_PRIMARY);
        bubble.add(body, BorderLayout.CENTER);

        JLabel meta = new JLabel(sdf.format(new Date(message.optLong("timestamp"))));
        meta.setFont(Constants.FONT_SMALL);
        meta.setForeground(Constants.TEXT_MUTED);
        bubble.add(meta, BorderLayout.SOUTH);

        row.add(bubble, BorderLayout.CENTER);
        return row;
    }

    private void triggerDelete(JSONObject message) {
        selectedMessage = new JSONObject();
        selectedMessage.put("timestamp", message.optLong("timestamp"));
        selectedMessage.put("sender", message.optString("sender"));
        selectedMessage.put("receiver", message.optString("receiver"));
        selectedMessage.put("groupId", message.optString("groupId", ""));
        if (deleteListener != null) {
            deleteListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "delete-message"));
        }
    }

    private void updateMembersForSelection() {
        memberComboBox.removeAllItems();
        GroupEntry selected = groupList.getSelectedValue();
        if (selected != null) {
            for (String member : selected.members) {
                memberComboBox.addItem(member);
            }
        }
    }

    private String resolveGroupName(String groupId) {
        for (int i = 0; i < groupListModel.size(); i++) {
            GroupEntry entry = groupListModel.getElementAt(i);
            if (entry.groupId.equals(groupId)) {
                return entry.groupName;
            }
        }
        return groupId;
    }

    private String toHtml(String content) {
        return "<html><div style='width:520px; line-height:1.45;'>"
                + content.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>")
                + "</div></html>";
    }

    public String getSelectedGroupId() {
        GroupEntry selected = groupList.getSelectedValue();
        return selected == null ? null : selected.groupId;
    }

    public String getSelectedGroupMember() {
        Object selected = memberComboBox.getSelectedItem();
        return selected == null ? null : selected.toString();
    }

    public JSONObject getSelectedMessage() {
        return selectedMessage;
    }

    public void addDeleteListener(ActionListener listener) {
        this.deleteListener = listener;
    }

    public void addRefreshListener(ActionListener listener) {
        refreshButton.addActionListener(listener);
    }

    public void addRemoveMemberListener(ActionListener listener) {
        removeMemberButton.addActionListener(listener);
    }

    public void addDeleteGroupListener(ActionListener listener) {
        deleteGroupButton.addActionListener(listener);
    }

    private static class GroupEntry {
        private final String groupId;
        private final String groupName;
        private final List<String> members;

        private GroupEntry(String groupId, String groupName, List<String> members) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.members = members;
        }
    }

    private static class GroupCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            GroupEntry entry = (GroupEntry) value;
            JPanel container = new JPanel(new BorderLayout(10, 0));
            container.setBorder(new EmptyBorder(8, 10, 8, 10));
            container.setBackground(isSelected ? Constants.CARD_GREEN : Constants.BG_SECONDARY);

            JLabel icon = new JLabel("G", SwingConstants.CENTER);
            icon.setOpaque(true);
            icon.setBackground(isSelected ? Constants.ONLINE_GREEN : Constants.BG_TERTIARY);
            icon.setForeground(isSelected ? Color.WHITE : Constants.TEXT_PRIMARY);
            icon.setPreferredSize(new Dimension(36, 36));
            icon.setFont(new Font("Segoe UI", Font.BOLD, 14));
            container.add(icon, BorderLayout.WEST);

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

            JLabel nameLabel = new JLabel(entry.groupName);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLabel.setForeground(Constants.TEXT_PRIMARY);
            textPanel.add(nameLabel);

            JLabel metaLabel = new JLabel(entry.groupId + " • " + entry.members.size() + " members");
            metaLabel.setFont(Constants.FONT_SMALL);
            metaLabel.setForeground(Constants.TEXT_SECONDARY);
            textPanel.add(metaLabel);

            container.add(textPanel, BorderLayout.CENTER);
            return container;
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color color;
        private final int arc;

        private RoundedPanel(Color color, int arc) {
            this.color = color;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
