package com.chatapp.view.admin;

import com.chatapp.util.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class StatisticsPanel extends JPanel {

    private JLabel onlineCountLabel;
    private JLabel totalUsersLabel;
    private JLabel totalMessagesLabel;
    private JButton refreshButton;

    public StatisticsPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 18));
        setBackground(Constants.BG_DARK);

        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);

        onlineCountLabel = createStatCard(cards, "Online Users", "0", Constants.CARD_GREEN, Constants.ONLINE_GREEN);
        totalUsersLabel = createStatCard(cards, "Registered Users", "0", Constants.CARD_BLUE, Constants.ACCENT);
        totalMessagesLabel = createStatCard(cards, "Total Messages", "0", Constants.CARD_PINK, Constants.ERROR_RED);

        add(cards, BorderLayout.NORTH);

        RoundedPanel footer = new RoundedPanel(Constants.BG_SECONDARY, 28);
        footer.setLayout(new BorderLayout());
        footer.setBorder(new EmptyBorder(22, 24, 22, 24));

        JLabel hint = new JLabel("Refresh to sync current server statistics.");
        hint.setFont(Constants.FONT_BODY);
        hint.setForeground(Constants.TEXT_SECONDARY);
        footer.add(hint, BorderLayout.WEST);

        refreshButton = new JButton("Refresh Stats");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        refreshButton.setBackground(Constants.CARD_BLUE);
        refreshButton.setForeground(Constants.ACCENT);
        refreshButton.setBorder(new EmptyBorder(10, 16, 10, 16));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        footer.add(refreshButton, BorderLayout.EAST);

        add(footer, BorderLayout.CENTER);
    }

    private JLabel createStatCard(JPanel parent, String title, String value, Color bg, Color accent) {
        RoundedPanel card = new RoundedPanel(bg, 26);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Constants.TEXT_SECONDARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(16));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        valueLabel.setForeground(accent);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(valueLabel);

        parent.add(card);
        return valueLabel;
    }

    public void updateStats(int online, int total, long messages) {
        onlineCountLabel.setText(String.valueOf(online));
        totalUsersLabel.setText(String.valueOf(total));
        totalMessagesLabel.setText(String.valueOf(messages));
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
