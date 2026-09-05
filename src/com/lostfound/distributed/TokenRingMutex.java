package com.lostfound.distributed;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Mutual exclusion via the Token Ring algorithm: a single token
 * circulates around a logical ring of nodes (node0 -> node1 -> ... ->
 * nodeN -> node0). A node may enter its critical section only while
 * it holds the token; releasing the critical section passes the
 * token to the next node over a real RMI call — this is genuinely
 * distributed coordination, not a local lock pretending to be one.
 *
 * Used to guard claim approval/rejection (ClaimServiceImpl) against
 * two admin server nodes racing to act on the same claim at once. In
 * a deployment with multiple admin server instances across machines,
 * each instance runs one TokenRingMutex node and the ring has one
 * entry per instance. This project's default single-server
 * deployment (Phase 6) runs a ring of size 1 (the node's "next" is
 * itself) — the exact same mutual-exclusion guarantee still applies
 * to the concurrent RMI request-handling threads within that one
 * process, which is where the real race condition would otherwise
 * occur (see the standalone 3-node test in this phase's write-up for
 * the genuinely multi-node case).
 */
public class TokenRingMutex extends UnicastRemoteObject implements TokenRingNode {

    private final String nodeId;
    private final Lock lock = new ReentrantLock();
    private final Condition tokenArrived = lock.newCondition();
    private volatile boolean hasToken;
    private volatile boolean claimedLocally; // true while some local thread currently owns the token
    private volatile TokenRingNode nextNode;

    public TokenRingMutex(String nodeId, boolean startsWithToken) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.hasToken = startsWithToken;
    }

    /** Must be set before acquire()/release() are used — who this node hands the token to. */
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

    /**
     * Blocks the calling thread until this node holds the token AND no
     * other local thread currently owns it. The second condition is
     * essential: RMI dispatches concurrent client calls onto separate
     * threads within the same node, so "this node has the token" alone
     * is not enough to guarantee only one thread proceeds — without
     * tracking per-acquisition local ownership, every thread already
     * waiting when the token arrives would pass through at once.
     */
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

    /**
     * Leaves the critical section and passes the token to the next
     * node over RMI. Any other local threads still waiting go back to
     * waiting for the token to circle the ring around to this node
     * again — this node only admits one local thread per possession
     * of the token, matching the classic Token Ring discipline rather
     * than draining an entire local queue before passing it on.
     */
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
