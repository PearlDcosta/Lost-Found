package com.lostfound.rpc;

import com.lostfound.exception.RPCException;
import com.lostfound.util.ConfigLoader;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;








public final class ServerTimeRPCClient {

    private static final int TIMEOUT_MS = 3000;
    private static final int BUFFER_SIZE = 256;

    private ServerTimeRPCClient() {
        
    }

    
    public static LocalDateTime getServerDateTime() throws RPCException {
        String host = ConfigLoader.get("rpc.host", "localhost");
        int port = ConfigLoader.getInt("rpc.port", 9877);
        return getServerDateTime(host, port);
    }

    public static LocalDateTime getServerDateTime(String host, int port) throws RPCException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            byte[] requestBytes = ServerTimeRPCServer.REQUEST_COMMAND.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length, address, port);
            socket.send(requestPacket);

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0,
                    responsePacket.getLength(), StandardCharsets.UTF_8);
            return LocalDateTime.parse(response);

        } catch (SocketTimeoutException e) {
            throw new RPCException("Server-time RPC request timed out after " + TIMEOUT_MS +
                    "ms. Is ServerTimeRPCServer running on " + host + ":" + port + "?", e);
        } catch (IOException e) {
            throw new RPCException("Server-time RPC request failed: " + e.getMessage(), e);
        } catch (DateTimeParseException e) {
            throw new RPCException("Server returned an unparseable timestamp: " + e.getMessage(), e);
        }
    }
}
