package com.chatapp.network.tcp;

import com.chatapp.model.Message;
import com.chatapp.util.Constants;
import com.chatapp.util.JSONUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;

/**
 * TCP client that connects to the ChatServer.
 * Provides methods for login, register, send message, and disconnect.
 * Runs a background thread to receive incoming messages.
 */
public class ChatClient {

    /**
     * Callback for events received from the server.
     */
    public interface ServerEventListener {
        void onMessageReceived(String json);

        void onDisconnected();
    }

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private ServerEventListener listener;
    private volatile boolean running = false;
    private Thread receiveThread;

    public void setServerEventListener(ServerEventListener listener) {
        this.listener = listener;
    }

    /**
     * Connects to the chat server.
     */
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            running = true;

            // Start receiving thread
            receiveThread = new Thread(this::receiveLoop, "ChatClient-Receive");
            receiveThread.setDaemon(true);
            receiveThread.start();

            System.out.println("[Client] Connected to server " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("[Client] Connection failed: " + e.getMessage());
            return false;
        }
    }

    private void receiveLoop() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (listener != null) {
                    listener.onMessageReceived(line);
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[Client] Connection lost: " + e.getMessage());
            }
        } finally {
            running = false;
            if (listener != null) {
                listener.onDisconnected();
            }
        }
    }

    /**
     * Sends a login request to the server.
     */
    public void login(String username, String password) {
        JSONObject obj = new JSONObject();
        obj.put("type", Constants.TYPE_LOGIN);
        obj.put("sender", username);
        obj.put("content", password);
        send(obj.toString());
    }

    /**
     * Sends a register request to the server.
     */
    public void register(String username, String password) {
        JSONObject obj = new JSONObject();
        obj.put("type", Constants.TYPE_REGISTER);
        obj.put("sender", username);
        obj.put("content", password);
        send(obj.toString());
    }

    /**
     * Sends a private message to another user.
     */
    public void sendMessage(Message msg) {
        send(JSONUtils.messageToJSON(msg));
    }

    /**
     * Sends a raw JSON string to the server.
     */
    public void send(String json) {
        if (writer != null) {
            writer.println(json);
        }
    }

    /**
     * Disconnects from the server.
     */
    public void disconnect() {
        running = false;
        try {
            // Send logout notification
            if (writer != null) {
                JSONObject obj = new JSONObject();
                obj.put("type", Constants.TYPE_LOGOUT);
                writer.println(obj.toString());
            }
        } catch (Exception ignored) {
        }

        try {
            if (reader != null)
                reader.close();
        } catch (IOException ignored) {
        }
        try {
            if (writer != null)
                writer.close();
        } catch (Exception ignored) {
        }
        try {
            if (socket != null)
                socket.close();
        } catch (IOException ignored) {
        }
    }

    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }
}
