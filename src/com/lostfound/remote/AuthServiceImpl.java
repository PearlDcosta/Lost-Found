package com.lostfound.remote;

import com.lostfound.dao.UserDAO;
import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Role;
import com.lostfound.model.User;
import com.lostfound.util.AuthorizationUtil;
import com.lostfound.util.PasswordUtil;
import com.lostfound.util.ValidationUtil;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Optional;







public class AuthServiceImpl extends UnicastRemoteObject implements AuthServiceRemote {

    private final UserDAO userDAO = new UserDAO();

    public AuthServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public User register(String name, String email, String password, Role role)
            throws RemoteException, ValidationException, AuthenticationException {

        ValidationUtil.requireNonBlank(name, "Name");
        ValidationUtil.requireValidEmail(email);
        ValidationUtil.requireMinLength(password, 6, "Password");
        if (role == null) {
            throw new ValidationException("Role is required.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        try {
            if (userDAO.emailExists(normalizedEmail)) {
                throw new AuthenticationException("An account with this email already exists.");
            }

            String hashedPassword = PasswordUtil.hash(password);
            User newUser = new User(name.trim(), normalizedEmail, hashedPassword, role);
            User inserted = userDAO.insert(newUser);
            return inserted.withoutPassword();

        } catch (DatabaseException e) {
            throw new AuthenticationException("Registration failed: " + e.getMessage(), e);
        }
    }

    @Override
    public User login(String email, String password) throws RemoteException, AuthenticationException {
        if (email == null || password == null) {
            throw new AuthenticationException("Invalid email or password.");
        }

        try {
            Optional<User> userOpt = userDAO.findByEmail(email.trim().toLowerCase());

            if (userOpt.isEmpty() || !PasswordUtil.verify(password, userOpt.get().getPassword())) {
                
                
                throw new AuthenticationException("Invalid email or password.");
            }

            return userOpt.get().withoutPassword();

        } catch (DatabaseException e) {
            throw new AuthenticationException("Login failed due to a system error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> getAllUsers(int callerUserId) throws RemoteException, DatabaseException, AuthenticationException {
        AuthorizationUtil.requireAdmin(userDAO, callerUserId);
        List<User> users = userDAO.findAll();
        
        return users.stream().map(User::withoutPassword).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void deleteUser(int callerUserId, int targetUserId)
            throws RemoteException, DatabaseException, ValidationException, AuthenticationException {
        AuthorizationUtil.requireAdmin(userDAO, callerUserId);

        User target = userDAO.findById(targetUserId)
                .orElseThrow(() -> new DatabaseException("No user found with id " + targetUserId));

        if (target.getRole() == Role.ADMIN) {
            long adminCount = userDAO.findAll().stream().filter(u -> u.getRole() == Role.ADMIN).count();
            if (adminCount <= 1) {
                throw new ValidationException("Cannot delete the last remaining admin account.");
            }
        }
        userDAO.delete(targetUserId);
    }
}
