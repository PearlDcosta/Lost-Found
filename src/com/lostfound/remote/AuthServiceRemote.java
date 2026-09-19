package com.lostfound.remote;

import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Role;
import com.lostfound.model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;











public interface AuthServiceRemote extends Remote {

    




    User register(String name, String email, String password, Role role)
            throws RemoteException, ValidationException, AuthenticationException;

    



    User login(String email, String password)
            throws RemoteException, AuthenticationException;

    
    List<User> getAllUsers(int callerUserId) throws RemoteException, DatabaseException, AuthenticationException;

    
    void deleteUser(int callerUserId, int targetUserId) throws RemoteException, DatabaseException, ValidationException, AuthenticationException;
}
