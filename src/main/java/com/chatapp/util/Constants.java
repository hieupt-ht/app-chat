package com.chatapp.util;

import java.awt.*;

public class Constants {
    // ───── Network ─────
    public static final int TCP_PORT = 9000;
    public static final int UDP_PORT = 9001;
    public static final String BROADCAST_ADDRESS = "255.255.255.255";
    public static final int UDP_BROADCAST_INTERVAL = 3000; // ms
    public static final int UDP_USER_TTL = 10000; // ms – remove stale users after this

    // ───── Paths ─────
    public static final String DATA_DIR = "data";
    public static final String USERS_FILE = DATA_DIR + "/users.json";
    public static final String MESSAGES_DIR = DATA_DIR + "/messages";
    public static final String DOWNLOADS_DIR = DATA_DIR + "/downloads";

    // ───── UI Colors (Light theme) ─────
    public static final Color BG_DARK       = new Color(245, 247, 251);
    public static final Color BG_SECONDARY  = new Color(255, 255, 255);
    public static final Color BG_TERTIARY   = new Color(236, 242, 250);
    public static final Color BG_INPUT      = new Color(247, 249, 253);
    public static final Color ACCENT        = new Color(76, 140, 255);
    public static final Color ACCENT_HOVER  = new Color(59, 121, 233);
    public static final Color TEXT_PRIMARY   = new Color(31, 41, 55);
    public static final Color TEXT_SECONDARY = new Color(91, 108, 132);
    public static final Color TEXT_MUTED     = new Color(130, 145, 165);
    public static final Color ONLINE_GREEN   = new Color(31, 170, 112);
    public static final Color BUBBLE_SENT    = new Color(76, 140, 255);
    public static final Color BUBBLE_RECV    = new Color(233, 238, 245);
    public static final Color BORDER_COLOR   = new Color(218, 225, 236);
    public static final Color ERROR_RED      = new Color(220, 76, 100);
    public static final Color SUCCESS_GREEN  = new Color(31, 170, 112);
    public static final Color OFFLINE_GRAY   = new Color(156, 163, 175);
    public static final Color WARNING_ORANGE = new Color(239, 146, 48);
    public static final Color CARD_BLUE      = new Color(228, 240, 255);
    public static final Color CARD_GREEN     = new Color(229, 246, 239);
    public static final Color CARD_PINK      = new Color(255, 239, 242);

    // ───── UI Fonts ─────
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_INPUT   = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BUTTON  = new Font("Segoe UI", Font.BOLD, 14);

    // ───── UI Sizes ─────
    public static final int WINDOW_WIDTH  = 950;
    public static final int WINDOW_HEIGHT = 650;
    public static final int USER_PANEL_WIDTH = 280;
    public static final int BORDER_RADIUS = 20;
    public static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    // ───── Message Types ─────
    public static final String TYPE_TEXT     = "TEXT";
    public static final String TYPE_FILE     = "FILE";
    public static final String TYPE_EMOJI    = "EMOJI";
    public static final String TYPE_SYSTEM   = "SYSTEM";
    public static final String TYPE_LOGIN    = "LOGIN";
    public static final String TYPE_REGISTER = "REGISTER";
    public static final String TYPE_LOGOUT   = "LOGOUT";
    public static final String TYPE_USER_LIST = "USER_LIST";
    public static final String TYPE_AUTH_RESULT = "AUTH_RESULT";
    public static final String TYPE_PRIVATE  = "PRIVATE";
    public static final String TYPE_BAN_USER = "BAN_USER";
    public static final String TYPE_DELETE_USER = "DELETE_USER";
    public static final String TYPE_FORCE_LOGOUT = "FORCE_LOGOUT";
    public static final String TYPE_DELETE_MESSAGE = "DELETE_MESSAGE";
    public static final String TYPE_GET_STATS = "GET_STATS";
    public static final String TYPE_STATS_RESULT = "STATS_RESULT";

    // ───── Roles ─────
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    // ───── UDP Prefixes ─────
    public static final String UDP_HELLO = "HELLO";
    public static final String UDP_BYE   = "BYE";

    // ───── Emojis ─────
    public static final String[] EMOJIS = {
        "😀", "😂", "😍", "🤔", "😎", "😢", "😡", "👍",
        "👎", "❤️", "🔥", "🎉", "👋", "🙏", "💪", "✨"
    };
}
