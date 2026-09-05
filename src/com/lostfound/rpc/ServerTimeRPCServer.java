package com.lostfound.rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Minimal RPC-style server built directly on java.net.DatagramSocket
 * (no RMI, no HTTP framework) — this is the lab's "server calculator /
 * date-time server using RPC, make use of Datagram" requirement,
 * repurposed as a genuinely useful piece of the application: the
 * authoritative server clock used to validate report dates (see
 * ItemServiceImpl.reportItem / ServerTimeRPCClient).
 *
 * Protocol (deliberately simple for an academic demo):
 *   client -> server : UTF-8 bytes "GET_TIME"
 *   server -> client : UTF-8 bytes of LocalDateTime.now().toString()
 *                       (ISO-8601, e.g. "2026-08-23T09:15:30.123")
 * Any other request is silently ignored.
 *
 * Runs as a background daemon thread, started alongside the RMI
 * services by LostFoundServer.main() — both processes live on the
 * same server machine (see Phase 1 architecture diagram).
 */
public class ServerTimeRPCServer implements Runnable {

    public static final String REQUEST_COMMAND = "GET_TIME";
    private static final int BUFFER_SIZE = 256;

    private final int port;
    private volatile boolean running = true;
    private DatagramSocket socket;

    public ServerTimeRPCServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
            System.out.println("[ServerTimeRPCServer] listening on UDP port " + port);

            byte[] buffer = new byte[BUFFER_SIZE];
            while (running) {
                try {
                    DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(requestPacket); // blocks until a request arrives

                    String request = new String(requestPacket.getData(), 0,
                            requestPacket.getLength(), StandardCharsets.UTF_8).trim();

                    if (REQUEST_COMMAND.equals(request)) {
                        String response = LocalDateTime.now().toString();
                        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                        DatagramPacket responsePacket = new DatagramPacket(
                                responseBytes, responseBytes.length,
                                requestPacket.getAddress(), requestPacket.getPort());
                        socket.send(responsePacket);
                    }
                    // unrecognized requests are silently ignored — a production
                    // protocol would reply with an error code, but this stays
                    // minimal on purpose for an academic demo.

                } catch (IOException e) {
                    if (running) {
                        System.err.println("[ServerTimeRPCServer] error handling request: " + e.getMessage());
                    }
                    // if running == false, this IOException is just the socket
                    // closing from stop() unblocking receive() — expected, not an error
                }
            }
        } catch (SocketException e) {
            System.err.println("[ServerTimeRPCServer] failed to bind UDP port " + port + ": " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    /** Stops the server by closing the socket, which unblocks the pending receive(). */
    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}
