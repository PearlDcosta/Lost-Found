package com.lostfound.remote;

import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Claim;
import com.lostfound.model.ClaimStatus;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;








public interface ClaimServiceRemote extends Remote {

    



    Claim submitClaim(int itemId, int userId, String description)
            throws RemoteException, DatabaseException, ValidationException;

    List<Claim> getClaimsForItem(int itemId) throws RemoteException, DatabaseException;

    List<Claim> getClaimsByUser(int userId) throws RemoteException, DatabaseException;

    List<Claim> getAllClaims() throws RemoteException, DatabaseException;

    
    void approveClaim(int claimId, int adminUserId) throws RemoteException, DatabaseException, ValidationException;

    
    void rejectClaim(int claimId, int adminUserId) throws RemoteException, DatabaseException, ValidationException;

    int countByStatus(ClaimStatus status) throws RemoteException, DatabaseException;
}
