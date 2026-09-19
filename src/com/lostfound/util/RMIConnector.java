package com.lostfound.util;

import com.lostfound.remote.AuthServiceRemote;
import com.lostfound.remote.ClaimServiceRemote;
import com.lostfound.remote.ItemServiceRemote;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;












public final class RMIConnector {

    private static Registry registry;

    private RMIConnector() {
        
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
