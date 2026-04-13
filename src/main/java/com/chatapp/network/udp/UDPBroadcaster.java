package com.chatapp.network.udp;

import com.chatapp.util.Constants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Periodically broadcasts the user's presence on the LAN
 * using UDP broadcast packets.
 * Packet format: HELLO|nickname|tcpPort
 */
public class UDPBroadcaster implements Runnable {

    private final String nickname;
    private final int tcpPort;
    private volatile boolean running = true;
    private DatagramSocket socket;

    public UDPBroadcaster(String nickname, int tcpPort) {
        this.nickname = nickname;
        this.tcpPort = tcpPort;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);

            while (running) {
                String msg = Constants.UDP_HELLO + "|" + nickname + "|" + tcpPort;
                byte[] data = msg.getBytes("UTF-8");

                DatagramPacket packet = new DatagramPacket(
                        data, data.length,
                        InetAddress.getByName(Constants.BROADCAST_ADDRESS),
                        Constants.UDP_PORT);
                socket.send(packet);

                Thread.sleep(Constants.UDP_BROADCAST_INTERVAL);
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[UDPBroadcaster] Error: " + e.getMessage());
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    /**
     * Sends a BYE packet before shutting down so peers remove us immediately.
     */
    public void stop() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                String msg = Constants.UDP_BYE + "|" + nickname + "|" + tcpPort;
                byte[] data = msg.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(
                        data, data.length,
                        InetAddress.getByName(Constants.BROADCAST_ADDRESS),
                        Constants.UDP_PORT);
                socket.send(packet);
                socket.close();
            }
        } catch (Exception e) {
            // ignore on shutdown
        }
    }
}
