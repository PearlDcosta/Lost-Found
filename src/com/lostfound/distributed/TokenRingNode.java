package com.lostfound.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI remote interface for a single participant in a token ring
 * (lab Module 4 / Practical 9: mutual exclusion via Token Ring
 * algorithm). Each node in the ring exposes this so its predecessor
 * can hand it the token remotely — the token itself never travels as
 * data, only this notification does; "holding the token" is local
 * state on each node (see TokenRingMutex).
 */
public interface TokenRingNode extends Remote {

    /** Called by the previous node in the ring to pass the token to this node. */
    void receiveToken() throws RemoteException;
}
