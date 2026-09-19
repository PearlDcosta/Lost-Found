package com.lostfound.distributed;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class TokenRingMutex extends UnicastRemoteObject implements TokenRingNode {

    private final String nodeId;
    private final Lock lock = new ReentrantLock();
    private final Condition tokenArrived = lock.newCondition();
    private volatile boolean hasToken;
    private volatile boolean claimedLocally; 
    private volatile TokenRingNode nextNode;

    public TokenRingMutex(String nodeId, boolean startsWithToken) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.hasToken = startsWithToken;
    }

    
    public void setNextNode(TokenRingNode nextNode) {
        this.nextNode = nextNode;
    }

    @Override
    public void receiveToken() throws RemoteException {
        lock.lock();
        try {
            hasToken = true;
            tokenArrived.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void acquire(long timeoutMs) throws InterruptedException, TimeoutException {
        lock.lock();
        try {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (!hasToken || claimedLocally) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new TimeoutException("[TokenRing:" + nodeId + "] timed out waiting for the token");
                }
                tokenArrived.await(remaining, TimeUnit.MILLISECONDS);
            }
            claimedLocally = true;
        } finally {
            lock.unlock();
        }
    }
    public void release() {
        lock.lock();
        try {
            hasToken = false;
            claimedLocally = false;
        } finally {
            lock.unlock();
        }
        try {
            if (nextNode != null) {
                nextNode.receiveToken();
            }
        } catch (RemoteException e) {
            System.err.println("[TokenRing:" + nodeId + "] failed to pass token to next node: " + e.getMessage());
        }
    }

    public boolean hasToken() {
        return hasToken;
    }

    public String getNodeId() {
        return nodeId;
    }
}
