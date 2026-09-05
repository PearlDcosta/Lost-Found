package com.lostfound.util;

import com.lostfound.remote.AuthServiceRemote;
import com.lostfound.remote.ClaimServiceRemote;
import com.lostfound.remote.ItemServiceRemote;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Client-side helper that looks up the RMI registry (host/port from
 * config.properties) and hands back typed remote stubs. Every Swing
 * GUI frame that needs to talk to the server goes through here
 * instead of calling LocateRegistry directly, so the lookup logic
 * lives in exactly one place.
 *
 * In Phase 20, only rmi.host in config.properties needs to change
 * (from "localhost" to the cloud VM's public IP) for every client to
 * start talking to the deployed server — no code changes required.
 */
public final class RMIConnector {

    private static Registry registry;

    private RMIConnector() {
        // static utility class, no instances
    }

    private static Registry getRegistry() throws RemoteException {
        if (registry == null) {
            String host = ConfigLoader.get("rmi.host", "localhost");
            int port = ConfigLoader.getInt("rmi.port", 1099);
            registry = LocateRegistry.getRegistry(host, port);
        }
        return registry;
    }

    public static AuthServiceRemote getAuthService() throws RemoteException, NotBoundException {
        return (AuthServiceRemote) getRegistry().lookup("AuthService");
    }

    public static ItemServiceRemote getItemService() throws RemoteException, NotBoundException {
        return (ItemServiceRemote) getRegistry().lookup("ItemService");
    }

    public static ClaimServiceRemote getClaimService() throws RemoteException, NotBoundException {
        return (ClaimServiceRemote) getRegistry().lookup("ClaimService");
    }
}
