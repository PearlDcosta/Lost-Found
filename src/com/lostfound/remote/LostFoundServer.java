package com.lostfound.remote;

import com.lostfound.distributed.RingElection;
import com.lostfound.rpc.ServerTimeRPCServer;
import com.lostfound.util.ConfigLoader;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Server-side entry point. Starts:
 *   1. An RMI registry, with AuthService/ItemService/ClaimService bound —
 *      Swing clients look these up via Registry.lookup(...) (Phase 6-9).
 *   2. A background ServerTimeRPCServer thread over UDP — the
 *      authoritative server-time source used by ItemServiceImpl
 *      (Phase 10).
 *   3. A RingElection node that elects itself coordinator on startup
 *      (Phase 15) — a ring of size 1 in this default single-server
 *      deployment, but a real run of the same election algorithm that
 *      would decide leadership among multiple LostFoundServer
 *      instances in a redundant, multi-node deployment.
 *
 * Run this BEFORE starting any Swing client. In production (Phase 20)
 * this single process runs on the cloud VM; clients then point at that
 * VM's public IP via rmi.host/rpc.host in config.properties instead of
 * localhost — no code changes required.
 */
public class LostFoundServer {

    public static void main(String[] args) {
        try {
            int rmiPort = ConfigLoader.getInt("rmi.port", 1099);

            Registry registry = LocateRegistry.createRegistry(rmiPort);

            AuthServiceRemote authService = new AuthServiceImpl();
            ItemServiceRemote itemService = new ItemServiceImpl();
            ClaimServiceRemote claimService = new ClaimServiceImpl();

            registry.rebind("AuthService", authService);
            registry.rebind("ItemService", itemService);
            registry.rebind("ClaimService", claimService);

            int rpcPort = ConfigLoader.getInt("rpc.port", 9877);
            ServerTimeRPCServer rpcServer = new ServerTimeRPCServer(rpcPort);
            Thread rpcThread = new Thread(rpcServer, "ServerTimeRPCServer-Thread");
            rpcThread.setDaemon(true);
            rpcThread.start();

            int nodeId = ConfigLoader.getInt("server.node.id", 1);
            RingElection electionNode = new RingElection(nodeId);
            electionNode.setNextNode(electionNode); // ring of size 1 for this single-server deployment
            electionNode.startElection();
            int coordinatorId = electionNode.awaitCoordinator(2000);

            System.out.println("=================================================");
            System.out.println(" LostFoundServer started successfully");
            System.out.println(" RMI registry listening on port " + rmiPort);
            System.out.println(" Bound services: AuthService, ItemService, ClaimService");
            System.out.println(" RPC server-time service listening on UDP port " + rpcPort);
            System.out.println(" Ring election complete: this node (id=" + nodeId +
                    ") is coordinator (id=" + coordinatorId + ")");
            System.out.println(" Press Ctrl+C to stop the server.");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("Failed to start LostFoundServer: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
