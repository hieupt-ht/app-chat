package com.chatapp.network.udp;

import com.chatapp.util.Constants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for UDP broadcast packets from other peers on the LAN.
 * Maintains a map of discovered users with TTL-based expiration.
 */
public class UDPListener implements Runnable {

    /**
     * Info about a discovered LAN peer.
     */
    public static class PeerInfo {
        public final String nickname;
        public final String ipAddress;
        public final int tcpPort;
        public long lastSeen;

        public PeerInfo(String nickname, String ipAddress, int tcpPort) {
            this.nickname = nickname;
            this.ipAddress = ipAddress;
            this.tcpPort = tcpPort;
            this.lastSeen = System.currentTimeMillis();
        }
    }

    /**
     * Callback interface to notify when users join/leave.
     */
    public interface PeerDiscoveryListener {
        void onPeersUpdated(Map<String, PeerInfo> peers);
    }

    private final String myNickname;
    private volatile boolean running = true;
    private DatagramSocket socket;
    private final ConcurrentHashMap<String, PeerInfo> peers = new ConcurrentHashMap<>();
    private PeerDiscoveryListener listener;

    public UDPListener(String myNickname) {
        this.myNickname = myNickname;
    }

    public void setPeerDiscoveryListener(PeerDiscoveryListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(Constants.UDP_PORT));
            socket.setBroadcast(true);

            // Start a cleanup thread that removes stale peers
            Thread cleanupThread = new Thread(this::cleanupLoop, "UDP-Cleanup");
            cleanupThread.setDaemon(true);
            cleanupThread.start();

            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                String senderIP = packet.getAddress().getHostAddress();

                processPacket(message, senderIP);
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[UDPListener] Error: " + e.getMessage());
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void processPacket(String message, String senderIP) {
        String[] parts = message.split("\\|");
        if (parts.length < 3)
            return;

        String type = parts[0];
        String nickname = parts[1];
        int tcpPort;
        try {
            tcpPort = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return;
        }

        // Ignore our own broadcasts
        if (nickname.equals(myNickname))
            return;

        if (Constants.UDP_HELLO.equals(type)) {
            PeerInfo info = new PeerInfo(nickname, senderIP, tcpPort);
            peers.put(nickname, info);
            notifyListener();
        } else if (Constants.UDP_BYE.equals(type)) {
            peers.remove(nickname);
            notifyListener();
        }
    }

    private void cleanupLoop() {
        while (running) {
            try {
                Thread.sleep(Constants.UDP_USER_TTL / 2);
                long now = System.currentTimeMillis();
                boolean changed = false;
                for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
                    if (now - entry.getValue().lastSeen > Constants.UDP_USER_TTL) {
                        peers.remove(entry.getKey());
                        changed = true;
                    }
                }
                if (changed) {
                    notifyListener();
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onPeersUpdated(new ConcurrentHashMap<>(peers));
        }
    }

    public Map<String, PeerInfo> getPeers() {
        return new ConcurrentHashMap<>(peers);
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
