package com.lostfound.remote;

import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Role;
import com.lostfound.model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI remote interface for identity/authentication. Every method here
 * executes on the server (LostFoundServer) and is invoked remotely by
 * Swing GUI clients via RMI — this is the Remote Method Invocation
 * (Module 2) and Identity Management (Module 6) lab requirement made
 * real inside the Lost & Found application.
 *
 * Every method must declare RemoteException (network/RMI failure),
 * in addition to whatever application-level exceptions it can raise.
 */
public interface AuthServiceRemote extends Remote {

    /**
     * Registers a new user. Validates input, checks for a duplicate
     * email, hashes the password server-side, and persists the user.
     * @return the created user, WITHOUT the password hash populated
     */
    User register(String name, String email, String password, Role role)
            throws RemoteException, ValidationException, AuthenticationException;

    /**
     * Authenticates a user by email/password.
     * @return the authenticated user, WITHOUT the password hash populated
     */
    User login(String email, String password)
            throws RemoteException, AuthenticationException;

    /** Admin-only: requires callerUserId to belong to an ADMIN account (verified server-side). */
    List<User> getAllUsers(int callerUserId) throws RemoteException, DatabaseException, AuthenticationException;

    /** Admin-only: requires callerUserId to belong to an ADMIN account. Refuses to delete the last remaining ADMIN. */
    void deleteUser(int callerUserId, int targetUserId) throws RemoteException, DatabaseException, ValidationException, AuthenticationException;
}
