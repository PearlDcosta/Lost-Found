package com.lostfound.remote;

import com.lostfound.dao.ClaimDAO;
import com.lostfound.dao.ItemDAO;
import com.lostfound.dao.StatusHistoryDAO;
import com.lostfound.dao.UserDAO;
import com.lostfound.distributed.TokenRingMutex;
import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Claim;
import com.lostfound.model.ClaimStatus;
import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.StatusHistory;
import com.lostfound.util.AuthorizationUtil;
import com.lostfound.util.ValidationUtil;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ClaimServiceImpl extends UnicastRemoteObject implements ClaimServiceRemote {

    private final ClaimDAO claimDAO = new ClaimDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final StatusHistoryDAO statusHistoryDAO = new StatusHistoryDAO();
    private final UserDAO userDAO = new UserDAO();

    // A ring of size 1 for this single-server deployment: the node's
    // "next" is itself, so releasing immediately re-arms the token for
    // the next acquirer. This still correctly serializes the
    // approve/reject critical section across the concurrent threads
    // RMI spins up per incoming client call — see class Javadoc and
    // TokenRingMutex's Javadoc for the multi-server-node case.
    private final TokenRingMutex claimMutex;
    private static final long TOKEN_TIMEOUT_MS = 5000;

    public ClaimServiceImpl() throws RemoteException {
        super();
        this.claimMutex = new TokenRingMutex("ClaimServiceImpl-node", true);
        this.claimMutex.setNextNode(this.claimMutex);
    }

    @Override
    public Claim submitClaim(int itemId, int userId, String description)
            throws RemoteException, DatabaseException, ValidationException {

        ValidationUtil.requireNonBlank(description, "Claim description");

        Item item = itemDAO.findById(itemId)
                .orElseThrow(() -> new DatabaseException("No item found with id " + itemId));

        if (item.getStatus() != ItemStatus.FOUND) {
            throw new ValidationException("Claims can only be submitted for items currently marked FOUND " +
                    "(current status: " + item.getStatus() + ").");
        }

        Claim claim = new Claim(itemId, userId, description.trim());
        Claim insertedClaim = claimDAO.insert(claim);

        // FOUND -> CLAIM_REQUESTED is a valid transition (see ItemStatus)
        itemDAO.updateStatus(itemId, ItemStatus.CLAIM_REQUESTED);
        statusHistoryDAO.insert(new StatusHistory(itemId, ItemStatus.FOUND, ItemStatus.CLAIM_REQUESTED, userId));

        return insertedClaim;
    }

    @Override
    public List<Claim> getClaimsForItem(int itemId) throws RemoteException, DatabaseException {
        return claimDAO.findByItemId(itemId);
    }

    @Override
    public List<Claim> getClaimsByUser(int userId) throws RemoteException, DatabaseException {
        return claimDAO.findByUserId(userId);
    }

    @Override
    public List<Claim> getAllClaims() throws RemoteException, DatabaseException {
        return claimDAO.findAll();
    }

    @Override
    public void approveClaim(int claimId, int adminUserId) throws RemoteException, DatabaseException, ValidationException {
        try {
            AuthorizationUtil.requireAdmin(userDAO, adminUserId);
        } catch (AuthenticationException e) {
            throw new ValidationException(e.getMessage());
        }
        try {
            claimMutex.acquire(TOKEN_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Interrupted while waiting for the claim mutex token.", e);
        } catch (TimeoutException e) {
            throw new DatabaseException("Timed out waiting for exclusive access to approve this claim " +
                    "(another admin action is in progress). Please try again.", e);
        }
        try {
            Claim claim = claimDAO.findById(claimId)
                    .orElseThrow(() -> new DatabaseException("No claim found with id " + claimId));

            if (claim.getStatus() != ClaimStatus.PENDING) {
                throw new ValidationException("Only PENDING claims can be approved (current status: " +
                        claim.getStatus() + ").");
            }

            claimDAO.updateStatus(claimId, ClaimStatus.APPROVED);
            // CLAIM_REQUESTED -> VERIFIED is a valid transition (see ItemStatus)
            itemDAO.updateStatus(claim.getItemId(), ItemStatus.VERIFIED);
            statusHistoryDAO.insert(new StatusHistory(claim.getItemId(),
                    ItemStatus.CLAIM_REQUESTED, ItemStatus.VERIFIED, adminUserId));
        } finally {
            claimMutex.release();
        }
    }

    @Override
    public void rejectClaim(int claimId, int adminUserId) throws RemoteException, DatabaseException, ValidationException {
        try {
            AuthorizationUtil.requireAdmin(userDAO, adminUserId);
        } catch (AuthenticationException e) {
            throw new ValidationException(e.getMessage());
        }
        try {
            claimMutex.acquire(TOKEN_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Interrupted while waiting for the claim mutex token.", e);
        } catch (TimeoutException e) {
            throw new DatabaseException("Timed out waiting for exclusive access to reject this claim " +
                    "(another admin action is in progress). Please try again.", e);
        }
        try {
            Claim claim = claimDAO.findById(claimId)
                    .orElseThrow(() -> new DatabaseException("No claim found with id " + claimId));

            if (claim.getStatus() != ClaimStatus.PENDING) {
                throw new ValidationException("Only PENDING claims can be rejected (current status: " +
                        claim.getStatus() + ").");
            }

            claimDAO.updateStatus(claimId, ClaimStatus.REJECTED);
            // CLAIM_REQUESTED -> FOUND (reject reverts the item so someone
            // else can still claim it) is a valid transition (see ItemStatus)
            itemDAO.updateStatus(claim.getItemId(), ItemStatus.FOUND);
            statusHistoryDAO.insert(new StatusHistory(claim.getItemId(),
                    ItemStatus.CLAIM_REQUESTED, ItemStatus.FOUND, adminUserId));
        } finally {
            claimMutex.release();
        }
    }

    @Override
    public int countByStatus(ClaimStatus status) throws RemoteException, DatabaseException {
        return claimDAO.countByStatus(status);
    }
}
