package com.lostfound.distributed;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Chang-Roberts ring election algorithm (lab Module 4 / Practical 10).
 * Nodes sit in a unidirectional logical ring, each with a unique
 * integer id. Any node may call startElection() (e.g. because it
 * noticed the current coordinator is unreachable). The highest-id
 * node in the ring always wins — even when multiple nodes start an
 * election at the same moment — and every node converges on the same
 * coordinator id. The "participant" flag is the key optimization
 * that lets this terminate correctly and efficiently under concurrent
 * elections: a node that has already forwarded a stronger candidate
 * discards further weaker candidacies instead of re-forwarding them.
 *
 * Used to decide which admin server node is authoritative if multiple
 * LostFoundServer instances ever run for redundancy. This project's
 * default single-server deployment runs a ring of size 1, which
 * trivially and correctly "elects" itself on startup — see
 * LostFoundServer.main() for that real (if minimal) usage.
 */
public class RingElection extends UnicastRemoteObject implements RingElectionNode {

    private final int nodeId;
    private final Lock lock = new ReentrantLock();
    private final Condition coordinatorKnown = lock.newCondition();

    private boolean participant = false;
    private volatile int coordinatorId = -1;
    private volatile RingElectionNode nextNode;

    public RingElection(int nodeId) throws RemoteException {
        super();
        this.nodeId = nodeId;
    }

    public void setNextNode(RingElectionNode nextNode) {
        this.nextNode = nextNode;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    /** Kicks off an election, e.g. because this node believes the coordinator is down. */
    public void startElection() {
        lock.lock();
        try {
            participant = true;
        } finally {
            lock.unlock();
        }
        forwardElection(nodeId);
    }

    @Override
    public void receiveElection(int candidateId) throws RemoteException {
        boolean shouldForwardElection = false;
        int forwardCandidateId = -1;
        boolean becameCoordinator = false;

        lock.lock();
        try {
            if (candidateId > nodeId) {
                participant = true;
                shouldForwardElection = true;
                forwardCandidateId = candidateId;
            } else if (candidateId < nodeId) {
                if (!participant) {
                    participant = true;
                    shouldForwardElection = true;
                    forwardCandidateId = nodeId;
                }
                // else: already participating with a candidate at least this
                // good — discard the weaker one instead of forwarding it
            } else {
                // candidateId == nodeId: this node's own candidacy has
                // travelled the whole ring untouched by a higher id — it wins.
                becameCoordinator = true;
                coordinatorId = nodeId;
                participant = false;
                coordinatorKnown.signalAll();
            }
        } finally {
            lock.unlock();
        }

        if (shouldForwardElection) {
            forwardElection(forwardCandidateId);
        } else if (becameCoordinator) {
            forwardCoordinator(nodeId);
        }
    }

    @Override
    public void receiveCoordinator(int announcedCoordinatorId) throws RemoteException {
        boolean shouldForward = false;

        lock.lock();
        try {
            if (announcedCoordinatorId != nodeId) {
                coordinatorId = announcedCoordinatorId;
                participant = false;
                coordinatorKnown.signalAll();
                shouldForward = true;
            }
            // if announcedCoordinatorId == nodeId, the announcement has
            // travelled the full ring back to where it started — stop here
        } finally {
            lock.unlock();
        }

        if (shouldForward) {
            forwardCoordinator(announcedCoordinatorId);
        }
    }

    /** Blocks until this node has learned who the coordinator is. */
    public int awaitCoordinator(long timeoutMs) throws InterruptedException, TimeoutException {
        lock.lock();
        try {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (coordinatorId == -1) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new TimeoutException("[RingElection:" + nodeId +
                            "] timed out waiting for a coordinator to be elected");
                }
                coordinatorKnown.await(remaining, TimeUnit.MILLISECONDS);
            }
            return coordinatorId;
        } finally {
            lock.unlock();
        }
    }

    private void forwardElection(int candidateId) {
        try {
            if (nextNode != null) {
                nextNode.receiveElection(candidateId);
            }
        } catch (RemoteException e) {
            System.err.println("[RingElection:" + nodeId + "] failed to forward election message: " + e.getMessage());
        }
    }

    private void forwardCoordinator(int announcedCoordinatorId) {
        try {
            if (nextNode != null) {
                nextNode.receiveCoordinator(announcedCoordinatorId);
            }
        } catch (RemoteException e) {
            System.err.println("[RingElection:" + nodeId + "] failed to forward coordinator announcement: " + e.getMessage());
        }
    }
}
