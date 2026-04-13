package com.chatapp.view.admin;

import com.chatapp.util.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionListener;

public class UserManagementPanel extends JPanel {

    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton banButton;
    private JButton unbanButton;
    private JButton deleteButton;
    private JButton forceLogoutButton;
    private JButton refreshButton;

    public UserManagementPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Constants.BG_DARK);

        RoundedPanel contentCard = new RoundedPanel(Constants.BG_SECONDARY, 28);
        contentCard.setLayout(new BorderLayout(0, 16));
        contentCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel title = new JLabel("Accounts");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Constants.TEXT_PRIMARY);
        topBar.add(title, BorderLayout.WEST);

        refreshButton = createButton("Refresh", Constants.CARD_BLUE, Constants.ACCENT);
        topBar.add(refreshButton, BorderLayout.EAST);
        contentCard.add(topBar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Username", "Role", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userTable.setForeground(Constants.TEXT_PRIMARY);
        userTable.setBackground(Constants.BG_SECONDARY);
        userTable.setGridColor(Constants.BORDER_COLOR);
        userTable.setRowHeight(38);
        userTable.setSelectionBackground(Constants.CARD_BLUE);
        userTable.setSelectionForeground(Constants.TEXT_PRIMARY);
        userTable.setShowVerticalLines(false);
        userTable.setIntercellSpacing(new Dimension(0, 0));
        userTable.setDefaultRenderer(Object.class, new UserTableRenderer());

        JTableHeader tableHeader = userTable.getTableHeader();
        tableHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tableHeader.setBackground(Constants.BG_INPUT);
        tableHeader.setForeground(Constants.TEXT_SECONDARY);
        tableHeader.setBorder(BorderFactory.createLineBorder(Constants.BORDER_COLOR));
        tableHeader.setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Constants.BORDER_COLOR));
        scrollPane.getViewport().setBackground(Constants.BG_SECONDARY);
        contentCard.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonBar.setOpaque(false);

        banButton = createButton("Ban", Constants.CARD_PINK, Constants.ERROR_RED);
        unbanButton = createButton("Unban", Constants.CARD_GREEN, Constants.ONLINE_GREEN);
        forceLogoutButton = createButton("Force Logout", Constants.CARD_BLUE, Constants.ACCENT);
        deleteButton = createButton("Delete", Constants.CARD_PINK, Constants.ERROR_RED);

        buttonBar.add(banButton);
        buttonBar.add(unbanButton);
        buttonBar.add(forceLogoutButton);
        buttonBar.add(deleteButton);
        contentCard.add(buttonBar, BorderLayout.SOUTH);

        add(contentCard, BorderLayout.CENTER);
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

    public void updateUsers(JSONArray usersArr) {
        tableModel.setRowCount(0);
        for (int i = 0; i < usersArr.length(); i++) {
            JSONObject user = usersArr.optJSONObject(i);
            if (user == null) {
                continue;
            }
            boolean banned = user.optBoolean("isBanned");
            boolean online = user.optBoolean("online");
            String status = banned ? "Banned" : online ? "Online" : "Offline";
            tableModel.addRow(new Object[]{
                    user.optString("username"),
                    user.optString("role"),
                    status
            });
        }
    }

    public String getSelectedUser() {
        int row = userTable.getSelectedRow();
        return row >= 0 ? String.valueOf(tableModel.getValueAt(row, 0)) : null;
    }

    public void addBanListener(ActionListener listener) {
        banButton.addActionListener(listener);
    }

    public void addUnbanListener(ActionListener listener) {
        unbanButton.addActionListener(listener);
    }

    public void addDeleteListener(ActionListener listener) {
        deleteButton.addActionListener(listener);
    }

    public void addForceLogoutListener(ActionListener listener) {
        forceLogoutButton.addActionListener(listener);
    }

    public void addRefreshListener(ActionListener listener) {
        refreshButton.addActionListener(listener);
    }

    private static class UserTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBorder(new EmptyBorder(0, 12, 0, 12));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            if (isSelected) {
                label.setBackground(Constants.CARD_BLUE);
                label.setForeground(Constants.TEXT_PRIMARY);
                return label;
            }

            label.setBackground(row % 2 == 0 ? Constants.BG_SECONDARY : Constants.BG_INPUT);
            label.setForeground(Constants.TEXT_PRIMARY);

            if (column == 2) {
                String status = String.valueOf(value);
                if ("Online".equals(status)) {
                    label.setForeground(Constants.ONLINE_GREEN);
                } else if ("Banned".equals(status)) {
                    label.setForeground(Constants.ERROR_RED);
                } else {
                    label.setForeground(Constants.OFFLINE_GRAY);
                }
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
            }

            return label;
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
