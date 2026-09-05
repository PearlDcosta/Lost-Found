package com.lostfound.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI remote interface for a participant in a ring-based coordinator
 * election (lab Module 4 / Practical 10: Election Algorithm). Nodes
 * are arranged in a logical ring exactly like TokenRingMutex; this
 * interface carries election/coordinator-announcement messages around
 * that same ring instead of a mutual-exclusion token.
 */
public interface RingElectionNode extends Remote {

    /** Forwarded around the ring while candidates for coordinator are being compared. */
    void receiveElection(int candidateId) throws RemoteException;

    /** Forwarded around the ring once to announce the winning coordinator to everyone. */
    void receiveCoordinator(int coordinatorId) throws RemoteException;
}
