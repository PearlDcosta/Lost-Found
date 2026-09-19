package com.lostfound.distributed;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
                
                
            } else {
                
                
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
            
            
        } finally {
            lock.unlock();
        }

        if (shouldForward) {
            forwardCoordinator(announcedCoordinatorId);
        }
    }

    
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
