package com.lostfound.rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;



















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
                    socket.receive(requestPacket); 

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
                    
                    
                    

                } catch (IOException e) {
                    if (running) {
                        System.err.println("[ServerTimeRPCServer] error handling request: " + e.getMessage());
                    }
                    
                    
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

    
    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}
