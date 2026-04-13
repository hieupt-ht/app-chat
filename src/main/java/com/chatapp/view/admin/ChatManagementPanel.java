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
import java.util.Date;

public class ChatManagementPanel extends JPanel {

    private JPanel messagesContainer;
    private JButton refreshButton;
    private JSONObject selectedMessage;
    private ActionListener deleteListener;

    public ChatManagementPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Constants.BG_DARK);

        RoundedPanel card = new RoundedPanel(Constants.BG_SECONDARY, 28);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Message archive");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Constants.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        refreshButton = createButton("Refresh", Constants.CARD_BLUE, Constants.ACCENT);
        header.add(refreshButton, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(Constants.BG_DARK);
        messagesContainer.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(messagesContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(Constants.BORDER_COLOR));
        scrollPane.getViewport().setBackground(Constants.BG_DARK);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        card.add(scrollPane, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
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

    public void updateMessages(JSONArray messagesArr) {
        messagesContainer.removeAll();
        selectedMessage = null;

        if (messagesArr == null || messagesArr.length() == 0) {
            messagesContainer.add(createEmptyState());
            refreshContainer();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        for (int i = 0; i < messagesArr.length(); i++) {
            JSONObject message = messagesArr.optJSONObject(i);
            if (message == null) {
                continue;
            }

            String type = message.optString("type");
            if (!Constants.TYPE_PRIVATE.equals(type) && !Constants.TYPE_FILE.equals(type)) {
                continue;
            }

            messagesContainer.add(createMessageBubble(message, sdf));
            messagesContainer.add(Box.createVerticalStrut(12));
        }

        if (messagesContainer.getComponentCount() == 0) {
            messagesContainer.add(createEmptyState());
        }

        refreshContainer();
    }

    private Component createEmptyState() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        wrapper.setOpaque(false);

        RoundedPanel pill = new RoundedPanel(Constants.BG_TERTIARY, 18);
        pill.setLayout(new BorderLayout());
        pill.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel label = new JLabel("No chat logs available yet.");
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

        RoundedPanel bubble = new RoundedPanel(
                Constants.TYPE_FILE.equals(message.optString("type")) ? Constants.CARD_BLUE : Constants.BG_SECONDARY,
                24);
        bubble.setLayout(new BorderLayout(0, 12));
        bubble.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel route = new JLabel(message.optString("sender") + " -> " + message.optString("receiver"));
        route.setFont(new Font("Segoe UI", Font.BOLD, 13));
        route.setForeground(Constants.TEXT_PRIMARY);
        top.add(route, BorderLayout.WEST);

        JButton deleteButton = createButton("Delete", Constants.CARD_PINK, Constants.ERROR_RED);
        deleteButton.setMargin(new Insets(8, 12, 8, 12));
        deleteButton.addActionListener(e -> triggerDelete(message));
        top.add(deleteButton, BorderLayout.EAST);
        bubble.add(top, BorderLayout.NORTH);

        String content = message.optString("content");
        boolean isFile = Constants.TYPE_FILE.equals(message.optString("type"));
        JLabel body = new JLabel(toHtml(isFile ? "[File] " + content : content));
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
        if (deleteListener != null) {
            deleteListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "delete-message"));
        }
    }

    private String toHtml(String content) {
        return "<html><div style='width:520px; line-height:1.45;'>"
                + content.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>")
                + "</div></html>";
    }

    private void refreshContainer() {
        messagesContainer.revalidate();
        messagesContainer.repaint();
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
