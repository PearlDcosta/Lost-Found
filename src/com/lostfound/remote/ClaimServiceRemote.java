package com.lostfound.remote;

import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Claim;
import com.lostfound.model.ClaimStatus;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI remote interface for the claim workflow: submit, list, approve,
 * reject. Approving/rejecting a claim also drives the related item's
 * ItemStatus (see ClaimServiceImpl) — this is where the status
 * lifecycle (Phase 17) is enforced server-side, so no client can
 * bypass the rules by calling things in the wrong order.
 */
public interface ClaimServiceRemote extends Remote {

    /**
     * Submits a claim against a FOUND item. Also advances the item's
     * status from FOUND to CLAIM_REQUESTED.
     */
    Claim submitClaim(int itemId, int userId, String description)
            throws RemoteException, DatabaseException, ValidationException;

    List<Claim> getClaimsForItem(int itemId) throws RemoteException, DatabaseException;

    List<Claim> getClaimsByUser(int userId) throws RemoteException, DatabaseException;

    List<Claim> getAllClaims() throws RemoteException, DatabaseException;

    /** Approves a PENDING claim; also advances the related item to VERIFIED. */
    void approveClaim(int claimId, int adminUserId) throws RemoteException, DatabaseException, ValidationException;

    /** Rejects a PENDING claim; reverts the related item back to FOUND. */
    void rejectClaim(int claimId, int adminUserId) throws RemoteException, DatabaseException, ValidationException;

    int countByStatus(ClaimStatus status) throws RemoteException, DatabaseException;
}
