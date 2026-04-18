package com.chatapp.network.udp;

import com.chatapp.util.Constants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                sendPresence(Constants.UDP_HELLO);

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
                sendPresence(Constants.UDP_BYE);
                socket.close();
            }
        } catch (Exception e) {
            // ignore on shutdown
        }
    }

    private void sendPresence(String type) throws Exception {
        String msg = type + "|" + nickname + "|" + tcpPort;
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);

        for (InetAddress broadcastAddress : resolveBroadcastAddresses()) {
            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    broadcastAddress,
                    Constants.UDP_PORT);
            socket.send(packet);
        }
    }

    private List<InetAddress> resolveBroadcastAddresses() throws Exception {
        Set<String> seen = new HashSet<>();
        List<InetAddress> addresses = new ArrayList<>();

        for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (networkInterface == null || !networkInterface.isUp() || networkInterface.isLoopback()) {
                continue;
            }

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast != null && seen.add(broadcast.getHostAddress())) {
                    addresses.add(broadcast);
                }
            }
        }

        InetAddress globalBroadcast = InetAddress.getByName(Constants.BROADCAST_ADDRESS);
        if (seen.add(globalBroadcast.getHostAddress())) {
            addresses.add(globalBroadcast);
        }

        return addresses;
    }
}
