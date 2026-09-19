package com.lostfound.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface RingElectionNode extends Remote {

    void receiveElection(int candidateId) throws RemoteException;

    void receiveCoordinator(int coordinatorId) throws RemoteException;
}
